/**
 * Awaryjny, lokalny odczyt pojedynczych plików `.kfdoc` i zbiorczych `.kfbundle` — CAŁKOWICIE
 * niezależny od backendu/bazy. Wszystko dzieje się w przeglądarce: żaden plik wybrany przez
 * użytkownika nigdy nie jest wysyłany na serwer (zob. dokumentacja/BACKUP_STRATEGY.txt, poziom 4).
 * Cel: nawet przy pustej/uszkodzonej bazie danych można otworzyć zapisany wcześniej .kfdoc albo
 * .kfbundle i wygenerować z niego PDF do wydruku/zapisu.
 *
 * Oba formaty to kontenery ZIP z jawnym manifestem — rozpoznawane po ZAWARTOŚCI (pole "format"
 * w manifest.json), nie po rozszerzeniu pliku. Rozpakowywanie przez `fflate` (mała, utrzymywana
 * biblioteka, zero zależności) — świadomie NIE własny, ręcznie pisany parser ZIP.
 */
import { unzipSync } from 'fflate'

// ── Limity bezpieczeństwa ───────────────────────────────────────────────────────────────────
// Chronią przed: przypadkowym wczytaniem ogromnego pliku, ZIP bombami, zawieszeniem przeglądarki,
// uszkodzonym archiwum, path traversalem. Zob. dokumentacja/BACKUP_STRATEGY.txt, "Limity
// bezpieczeństwa panelu ratunkowego".
export const MAX_KFDOC_INPUT_BYTES = 100 * 1024 * 1024        // 100 MB — pojedynczy .kfdoc
export const MAX_KFBUNDLE_INPUT_BYTES = 600 * 1024 * 1024     // 600 MB — zbiorczy .kfbundle
export const MAX_DOCUMENTS_IN_BUNDLE = 5000
export const MAX_ASSETS_PER_DOCUMENT = 200
export const MAX_SINGLE_ASSET_BYTES = 25 * 1024 * 1024        // 25 MB — pojedynczy obraz po rozpakowaniu
export const MAX_TOTAL_UNCOMPRESSED_BYTES = 1.5 * 1024 * 1024 * 1024 // 1.5 GB — suma po rozpakowaniu
export const SUPPORTED_FORMAT_VERSION = 1

export class RecoveryError extends Error {
  constructor(message) {
    super(message)
    this.name = 'RecoveryError'
  }
}

export function formatBytes(n) {
  if (n < 1024) return `${n} B`
  const units = ['KB', 'MB', 'GB']
  let v = n / 1024
  let i = 0
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++ }
  return `${v.toFixed(1)} ${units[i]}`
}

function assertSafeEntryName(name) {
  if (!name || name.includes('..') || name.startsWith('/') || name.startsWith('\\') || /^[a-zA-Z]:/.test(name)) {
    throw new RecoveryError(`Niedozwolona ścieżka wewnątrz archiwum: "${name}".`)
  }
}

function assertZipSignature(bytes) {
  // Lokalny nagłówek pliku ZIP zaczyna się od "PK\x03\x04" ("PK\x05\x06" dla pustego archiwum).
  if (bytes.length < 4 || bytes[0] !== 0x50 || bytes[1] !== 0x4b) {
    throw new RecoveryError('To nie jest plik ZIP (.kfdoc/.kfbundle) — nieprawidłowa sygnatura pliku.')
  }
}

function parseJsonEntry(entries, name, label) {
  const raw = entries[name]
  if (!raw) throw new RecoveryError(`Brak pliku "${name}" w archiwum — to nie jest poprawny ${label}.`)
  try {
    return JSON.parse(new TextDecoder('utf-8').decode(raw))
  } catch {
    throw new RecoveryError(`Plik "${name}" jest uszkodzony (nieprawidłowy JSON).`)
  }
}

function unzipSafely(bytes, label) {
  try {
    return unzipSync(bytes)
  } catch {
    throw new RecoveryError(`Nie udało się odczytać archiwum ZIP (${label}) — plik jest uszkodzony.`)
  }
}

const EXT_MIME = { png: 'image/png', jpg: 'image/jpeg', jpeg: 'image/jpeg', gif: 'image/gif', webp: 'image/webp', svg: 'image/svg+xml' }
function guessMime(fileName) {
  const ext = fileName.split('.').pop()?.toLowerCase()
  return EXT_MIME[ext] ?? 'application/octet-stream'
}

/** Analogiczne do backendowego `DocumentContainer.rewriteImageSrc` — podmienia "asset:xyz" na lokalny Blob URL. */
function rewriteAssetRefs(node, assetUrlOf) {
  if (Array.isArray(node)) {
    node.forEach(child => rewriteAssetRefs(child, assetUrlOf))
    return
  }
  if (node == null || typeof node !== 'object') return
  if (node.type === 'image' && typeof node.attrs?.src === 'string' && node.attrs.src.startsWith('asset:')) {
    const assetName = node.attrs.src.slice('asset:'.length)
    const url = assetUrlOf(assetName)
    if (url) node.attrs.src = url
  }
  for (const value of Object.values(node)) rewriteAssetRefs(value, assetUrlOf)
}

/**
 * Rozpakowuje WEWNĘTRZNE wpisy jednego .kfdoc (już odczytane przez unzipSync) na kształt danych
 * oczekiwany przez `generateSingleNotePdf`/`generateBulkNotesPdf` (zob. pdfGenerator.js).
 * Zwraca też listę utworzonych Blob URL — wywołujący jest odpowiedzialny za ich zwolnienie
 * (`revokeBlobUrls`) PO wygenerowaniu PDF-a.
 */
function buildNoteDataFromKfdocEntries(entries, manifest) {
  if (manifest.format !== 'kfdoc') {
    throw new RecoveryError(`Nieoczekiwany format wewnątrz .kfdoc: "${manifest.format}".`)
  }
  if (manifest.version !== SUPPORTED_FORMAT_VERSION) {
    throw new RecoveryError(`Nieobsługiwana wersja formatu .kfdoc: ${manifest.version} (ta aplikacja obsługuje wersję ${SUPPORTED_FORMAT_VERSION}).`)
  }
  const contentJson = parseJsonEntry(entries, 'content.json', 'plik .kfdoc')

  const meta = manifest.metadata && typeof manifest.metadata === 'object' ? manifest.metadata : {}
  const blobUrls = []
  let assetCount = 0
  let totalAssetBytes = 0

  const assetUrlOf = (assetName) => {
    assetCount++
    if (assetCount > MAX_ASSETS_PER_DOCUMENT) {
      throw new RecoveryError(`Zbyt wiele obrazów w jednej notatce (limit: ${MAX_ASSETS_PER_DOCUMENT}).`)
    }
    const entryName = `assets/${assetName}`
    assertSafeEntryName(entryName)
    const bytes = entries[entryName]
    if (!bytes) return null // odwołanie do brakującego assetu — generator PDF sam zgłosi to jako "issue"
    if (bytes.length > MAX_SINGLE_ASSET_BYTES) {
      throw new RecoveryError(`Obraz "${assetName}" jest zbyt duży (${formatBytes(bytes.length)}, limit: ${formatBytes(MAX_SINGLE_ASSET_BYTES)}).`)
    }
    totalAssetBytes += bytes.length
    if (totalAssetBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
      throw new RecoveryError('Przekroczono łączny dopuszczalny rozmiar rozpakowanych danych.')
    }
    const blob = new Blob([bytes], { type: guessMime(assetName) })
    const url = URL.createObjectURL(blob)
    blobUrls.push(url)
    return url
  }

  // Kopia głęboka — nie modyfikujemy oryginalnych wpisów ZIP w miejscu (mogłyby być reużyte, np.
  // przy weryfikacji checksumy w kfbundle).
  const contentCopy = JSON.parse(JSON.stringify(contentJson))
  rewriteAssetRefs(contentCopy, assetUrlOf)

  const categoryPath = Array.isArray(meta.categoryPath) && meta.categoryPath.length > 0
    ? meta.categoryPath
    : (meta.categoryName ? [meta.categoryName] : [])

  const noteData = {
    title: meta.title || '(bez tytułu)',
    categoryPath,
    author: meta.author ?? null,
    type: meta.type ?? null,
    createdAt: meta.topicCreatedAt ?? meta.noteCreatedAt ?? null,
    updatedAt: meta.noteCreatedAt ?? meta.savedAt ?? null,
    lastVerificationOfActualityDate: meta.lastVerificationOfActualityDate ?? null,
    contentJson: contentCopy,
  }
  return { noteData, blobUrls }
}

/** Zwalnia Blob URL utworzone przy odczycie — wołać PO wygenerowaniu/otwarciu PDF-a. */
export function revokeBlobUrls(urls) {
  for (const url of urls ?? []) {
    try { URL.revokeObjectURL(url) } catch { /* nic — url mógł już zostać zwolniony */ }
  }
}

async function sha256Hex(bytes) {
  const digest = await crypto.subtle.digest('SHA-256', bytes)
  return [...new Uint8Array(digest)].map(b => b.toString(16).padStart(2, '0')).join('')
}

/**
 * Odczytuje wybrany przez użytkownika plik (`File`, np. z <input type="file"> albo drag&drop) —
 * WYŁĄCZNIE lokalnie, zero requestów sieciowych. Rozpoznaje .kfdoc vs .kfbundle po zawartości
 * manifestu, nie po rozszerzeniu.
 *
 * Zwraca:
 *   { kind: 'kfdoc',    noteData, blobUrls }
 *   { kind: 'kfbundle', notesData: [...], issues: [{fileName,title,reason}], blobUrls, manifest }
 */
export async function readEmergencyFile(file) {
  if (file.size > MAX_KFBUNDLE_INPUT_BYTES) {
    throw new RecoveryError(`Plik jest zbyt duży (${formatBytes(file.size)}) — maksymalny obsługiwany rozmiar to ${formatBytes(MAX_KFBUNDLE_INPUT_BYTES)}.`)
  }
  const bytes = new Uint8Array(await file.arrayBuffer())
  assertZipSignature(bytes)
  const entries = unzipSafely(bytes, file.name);
  const manifest = parseJsonEntry(entries, 'manifest.json', 'plik .kfdoc/.kfbundle')

  if (manifest.format === 'kfdoc') {
    if (file.size > MAX_KFDOC_INPUT_BYTES) {
      throw new RecoveryError(`Plik .kfdoc jest zbyt duży (${formatBytes(file.size)}) — limit to ${formatBytes(MAX_KFDOC_INPUT_BYTES)}.`)
    }
    const { noteData, blobUrls } = buildNoteDataFromKfdocEntries(entries, manifest);
    return { kind: 'kfdoc', noteData, blobUrls }
  }

  if (manifest.format === 'kfbundle') {
    return await readKfbundle(entries, manifest)
  }

  throw new RecoveryError(`Nierozpoznany format archiwum: "${manifest.format ?? '(brak)'}".`)
}

async function readKfbundle(entries, manifest) {
  if (manifest.version !== SUPPORTED_FORMAT_VERSION) {
    throw new RecoveryError(`Nieobsługiwana wersja formatu .kfbundle: ${manifest.version} (ta aplikacja obsługuje wersję ${SUPPORTED_FORMAT_VERSION}).`)
  }
  const index = parseJsonEntry(entries, 'index.json', 'plik .kfbundle')
  if (!Array.isArray(index)) {
    throw new RecoveryError('index.json ma nieprawidłowy format (oczekiwano tablicy dokumentów).')
  }
  if (index.length > MAX_DOCUMENTS_IN_BUNDLE) {
    throw new RecoveryError(`Zbyt wiele dokumentów w archiwum (${index.length}) — limit to ${MAX_DOCUMENTS_IN_BUNDLE}.`)
  }

  const notesData = []
  const issues = []
  const blobUrls = []

  for (const entry of index) {
    const fileName = entry?.fileName
    try {
      assertSafeEntryName(fileName)
      const raw = entries[fileName]
      if (!raw) {
        issues.push({ fileName, title: entry?.title, reason: 'Plik wskazany w indeksie nie istnieje w archiwum.' })
        continue
      }
      if (entry?.corrupted) {
        issues.push({ fileName, title: entry?.title, reason: entry?.error || 'Oznaczony jako uszkodzony podczas tworzenia backupu.' })
        continue
      }
      if (entry?.sha256) {
        const actual = await sha256Hex(raw)
        if (actual !== entry.sha256) {
          issues.push({ fileName, title: entry?.title, reason: 'Suma kontrolna SHA-256 się nie zgadza — plik mógł zostać uszkodzony po zapisaniu backupu.' })
          continue
        }
      }
      const docEntries = unzipSafely(raw, fileName)
      const docManifest = parseJsonEntry(docEntries, 'manifest.json', 'wewnętrzny plik .kfdoc')
      const { noteData, blobUrls: docBlobUrls } = buildNoteDataFromKfdocEntries(docEntries, docManifest)
      notesData.push(noteData)
      blobUrls.push(...docBlobUrls)
    } catch (e) {
      issues.push({ fileName, title: entry?.title, reason: e instanceof Error ? e.message : String(e) })
    }
  }

  // Celowo BEZ rzucania, nawet gdy notesData jest puste — spójnie z resztą mechanizmu ("nie
  // pomijaj po cichu, pokaż listę problemów"): to UI decyduje, jak zaprezentować "0 z N czytelnych",
  // dokładnie tak samo jak przypadek "część z N". Odróżnia to też PUSTY, ale poprawny backup
  // (notesData:[], issues:[]) od backupu, w którym WSZYSTKO jest uszkodzone (notesData:[], issues:[N]).
  return { kind: 'kfbundle', notesData, issues, blobUrls, manifest }
}
