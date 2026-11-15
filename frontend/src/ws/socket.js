/**
 * Jeden współdzielony klient WebSocket dla całej aplikacji — moduł-singleton (nie hook, nie
 * komponent), więc niezależnie od tego, ile miejsc w drzewie komponentów go używa, otwarte jest
 * zawsze dokładnie jedno połączenie. openApplicationSocket()/closeApplicationSocket() wołane są
 * raz, z korzenia aplikacji (zob. AppLayout) — pojedyncze mount/unmount całej appki.
 */
const BASE_BACKOFF_MS = 1000
const MAX_BACKOFF_MS = 30000
const RECENT_EVENT_IDS_LIMIT = 200

let socket = null
let manuallyOpened = false
let reconnectAttempts = 0
let reconnectTimer = null
let status = 'disconnected' // 'connecting' | 'connected' | 'reconnecting' | 'disconnected'

const eventListeners = new Set()
const statusListeners = new Set()
const recentEventIds = []

function isValidEvent(obj) {
  return !!obj && typeof obj === 'object'
    && typeof obj.eventId === 'string'
    && typeof obj.type === 'string'
    && typeof obj.occurredAt === 'string'
}

/** Ograniczony zbiór (FIFO) niedawno obsłużonych eventId — chroni przed dwukrotnym przetworzeniem tego samego zdarzenia. */
function alreadySeen(eventId) {
  if (recentEventIds.includes(eventId)) return true
  recentEventIds.push(eventId)
  if (recentEventIds.length > RECENT_EVENT_IDS_LIMIT) recentEventIds.shift()
  return false
}

function setStatus(next) {
  if (status === next) return
  status = next
  statusListeners.forEach(fn => { try { fn(status) } catch { /* nie wywracaj innych listenerów */ } })
}

function wsUrl() {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${window.location.host}/ws/updates`
}

/** Exponential backoff 1s,2s,4s,8s,16s,30s(cap) + niewielki jitter — i TYLKO JEDEN aktywny timer naraz. */
function scheduleReconnect() {
  if (reconnectTimer || !manuallyOpened) return
  const backoff = Math.min(BASE_BACKOFF_MS * 2 ** reconnectAttempts, MAX_BACKOFF_MS)
  const jitter = Math.random() * Math.min(500, backoff * 0.2)
  reconnectAttempts += 1
  setStatus('reconnecting')
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    if (manuallyOpened) connect()
  }, backoff + jitter)
}

function connect() {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return
  setStatus(reconnectAttempts > 0 ? 'reconnecting' : 'connecting')

  let s
  try {
    s = new WebSocket(wsUrl())
  } catch {
    scheduleReconnect()
    return
  }
  socket = s

  s.onopen = () => {
    reconnectAttempts = 0 // zeruj licznik prób dopiero po UDANYM połączeniu
    setStatus('connected')
  }
  s.onclose = () => {
    if (socket === s) socket = null
    setStatus('disconnected')
    scheduleReconnect()
  }
  s.onerror = () => {
    // onclose i tak nastąpi zaraz po onerror w przeglądarkach — reconnect planujemy tam, nie tu (unikamy podwójnego harmonogramu)
  }
  s.onmessage = (evt) => {
    let parsed
    try {
      parsed = JSON.parse(evt.data)
    } catch {
      return // nie-JSON -> ignoruj po cichu
    }
    if (!isValidEvent(parsed)) return // nieznany/niepoprawny kształt -> ignoruj po cichu
    if (alreadySeen(parsed.eventId)) return
    eventListeners.forEach(fn => {
      try { fn(parsed) } catch (e) { console.error('[ws] listener threw', e) }
    })
  }
}

export function openApplicationSocket() {
  if (manuallyOpened) return
  manuallyOpened = true
  connect()
}

export function closeApplicationSocket() {
  manuallyOpened = false
  if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
  if (socket) { try { socket.close(1000, 'client unmounted') } catch { /* noop */ } socket = null }
  setStatus('disconnected')
}

/** fn(event: WsEvent) — wywoływany dla każdego poprawnego, nie-zduplikowanego zdarzenia. */
export function subscribeToEvents(fn) {
  eventListeners.add(fn)
  return () => eventListeners.delete(fn)
}

/** fn(status: 'connecting'|'connected'|'reconnecting'|'disconnected') — wywoływany od razu z aktualnym stanem, potem przy każdej zmianie. */
export function subscribeToStatus(fn) {
  statusListeners.add(fn)
  fn(status)
  return () => statusListeners.delete(fn)
}

export function getConnectionStatus() {
  return status
}
