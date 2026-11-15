const STORAGE_KEY = 'kf.clientId'

/**
 * Jeden UUID na kartę przeglądarki, trwały przez czas życia karty (sessionStorage, nie
 * localStorage — świadomie: każda NOWA karta ma dostać własny clientId, karta odświeżona
 * (F5) zachowuje ten sam). Wysyłany w nagłówku X-Client-Id przy każdej operacji modyfikującej
 * i porównywany z WsEvent.sourceClientId, żeby karta rozpoznała własne zdarzenie zwrotne.
 */
export function getClientId() {
  if (typeof window === 'undefined') return 'server'
  try {
    let id = window.sessionStorage.getItem(STORAGE_KEY)
    if (!id) {
      id = crypto.randomUUID()
      window.sessionStorage.setItem(STORAGE_KEY, id)
    }
    return id
  } catch {
    // sessionStorage niedostępny (np. tryb prywatny z restrykcjami) — generuj świeży id na czas życia modułu
    return crypto.randomUUID()
  }
}
