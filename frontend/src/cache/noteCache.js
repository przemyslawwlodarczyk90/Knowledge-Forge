/**
 * Cache treści notatek — WYŁĄCZNIE w pamięci JS bieżącej karty przeglądarki (module-level
 * singleton, tak jak `ws/socket.js`). Celowo NIE używa localStorage/sessionStorage/IndexedDB/
 * Service Workera ani żadnego cache'a backendowego — po odświeżeniu strony albo zamknięciu
 * karty cache znika, i to jest zamierzone. Pełny opis mechanizmu: zob. dokumentacja/NOTE_CACHE.txt w
 * katalogu głównym repo.
 *
 * Ten moduł jest CELOWO "głupi" — trzyma tylko dane i zna reguły wygasania/LRU. Decyzję O TYM,
 * KIEDY sprawdzić/zapisać/unieważnić wpis (np. reakcja na zdarzenie WebSocket NOTE_SAVED, albo
 * to, czy dana notatka jest akurat otwarta w trybie podglądu czy edycji) podejmuje wywołujący
 * kod (NotePanel) — ten moduł o tym nic nie wie.
 *
 * Dwa niezależne magazyny:
 *   - `cache`        — pozytywne wpisy (pełna odpowiedź notatki), przesuwny TTL 30 min od
 *                       ostatniego użycia, limit 50 wpisów, wypieranie LRU.
 *   - `missingCache` — negatywne wpisy ("dla tego tematu nie ma jeszcze notatki", odpowiedź
 *                       404), stały TTL 2 minuty od zapisania (bez przesuwnego odnawiania).
 */

const TTL_MS = 30 * 60 * 1000        // 30 minut — ważność notatki od OSTATNIEGO użycia (przesuwne)
const MISSING_TTL_MS = 2 * 60 * 1000 // 2 minuty — ważność informacji "notatka nie istnieje" (stałe)
const MAX_ENTRIES = 50               // limit pozytywnych wpisów, wypieranie najdawniej używanego (LRU)

/** @type {Map<string, { note: object, lastUsedAt: number }>} */
const cache = new Map()

/** @type {Map<string, number>} topicId -> moment (Date.now()) ustawienia negatywnego wpisu */
const missingCache = new Map()

/**
 * Bezpieczna głęboka kopia — notatka trzyma `contentJson` (drzewo ProseMirror/TipTap), które
 * NIE MOŻE być tym samym obiektem co ten, którym operuje żywy edytor (edytor go mutuje w
 * miejscu). Kopiujemy zarówno przy zapisie do cache, jak i przy odczycie z niego, żeby ani
 * wywołujący kod, ani cache nie mogły przypadkiem zmutować się nawzajem.
 */
function cloneValue(value) {
  if (value == null) return value
  if (typeof structuredClone === 'function') {
    try {
      return structuredClone(value)
    } catch {
      // np. wartości niesklonowalne strukturalnie (nie powinno się zdarzyć dla zwykłego JSON-a
      // z backendu) — spadnij na JSON poniżej
    }
  }
  return JSON.parse(JSON.stringify(value))
}

/**
 * Zwraca głęboką kopię notatki z cache, jeśli wpis istnieje i nie wygasł — i odnawia jego czas
 * ostatniego użycia (przesuwny TTL) oraz pozycję LRU. Zwraca `null`, gdy wpisu nie ma albo
 * wygasł (wygasły wpis jest przy okazji usuwany).
 */
export function getCachedNote(topicId) {
  if (!topicId) return null
  const entry = cache.get(topicId)
  if (!entry) return null
  if (Date.now() - entry.lastUsedAt > TTL_MS) {
    cache.delete(topicId)
    return null
  }
  entry.lastUsedAt = Date.now()
  // Map zachowuje kolejność wstawiania — przenosimy wpis na koniec, żeby kolejność Mapy zawsze
  // odzwierciedlała "od najdawniej do najświeżej używanego" (LRU).
  cache.delete(topicId)
  cache.set(topicId, entry)
  return cloneValue(entry.note)
}

/**
 * Zapisuje PEŁNĄ odpowiedź notatki (musi zawierać co najmniej `contentJson` i `version`) w
 * cache, z odświeżonym czasem ostatniego użycia. Wymusza limit `MAX_ENTRIES` — po przekroczeniu
 * usuwa najdawniej używany wpis (LRU). Zapisanie pozytywnego wpisu kasuje ewentualny negatywny
 * ("brak notatki") wpis dla tego samego tematu — nie mogą być prawdziwe jednocześnie.
 */
export function setCachedNote(topicId, note) {
  if (!topicId || !note) return
  cache.delete(topicId) // jeśli już istniał — usuń, żeby poniższy set() wstawił go na koniec (świeże LRU)
  cache.set(topicId, { note: cloneValue(note), lastUsedAt: Date.now() })
  missingCache.delete(topicId)
  while (cache.size > MAX_ENTRIES) {
    const oldestKey = cache.keys().next().value
    cache.delete(oldestKey)
  }
}

/** Usuwa wpis (pozytywny ORAZ negatywny) dla jednego tematu — np. po NOTE_SAVED z innej karty. */
export function invalidateCachedNote(topicId) {
  if (!topicId) return
  cache.delete(topicId)
  missingCache.delete(topicId)
}

/** Czyści cały cache notatek (pozytywne i negatywne wpisy) — np. po odzyskaniu połączenia WS. */
export function clearNoteCache() {
  cache.clear()
  missingCache.clear()
}

/**
 * Czy dla tego tematu mamy wciąż ważny (nie starszy niż 2 minuty) negatywny wpis — "notatka
 * dla tego tematu obecnie nie istnieje" (odpowiednik świeżej odpowiedzi 404). Wygasły wpis jest
 * przy okazji usuwany.
 */
export function getCachedNoteMissing(topicId) {
  if (!topicId) return false
  const setAt = missingCache.get(topicId)
  if (setAt == null) return false
  if (Date.now() - setAt > MISSING_TTL_MS) {
    missingCache.delete(topicId)
    return false
  }
  return true
}

/** Zapisuje negatywny wpis ("notatka nie istnieje") dla tematu, ważny 2 minuty od teraz. */
export function setCachedNoteMissing(topicId) {
  if (!topicId) return
  missingCache.set(topicId, Date.now())
}

// ── Diagnostyka (testy / debugowanie w konsoli) ──────────────────────────────────────────────
/** Liczba pozytywnych wpisów aktualnie w cache (bez sprzątania wygasłych — czysto informacyjne). */
export function getNoteCacheSize() {
  return cache.size
}
/** Liczba negatywnych ("brak notatki") wpisów aktualnie w cache. */
export function getNoteCacheMissingSize() {
  return missingCache.size
}
