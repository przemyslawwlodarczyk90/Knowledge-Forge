import { useEffect, useRef, useState } from 'react'
import { subscribeToEvents, subscribeToStatus, getConnectionStatus } from '@/ws/socket'

/**
 * Subskrybuje współdzielony klient WebSocket (jedno połączenie na całą appkę — zob. ws/socket.js,
 * otwierane raz w AppLayout). handler jest wołany dla KAŻDEGO zdarzenia — komponent sam filtruje
 * po event.type, jeśli interesuje go tylko część typów. Subskrypcja jest zdejmowana automatycznie
 * przy odmontowaniu komponentu.
 */
export function useApplicationEvents(handler) {
  const handlerRef = useRef(handler)
  useEffect(() => { handlerRef.current = handler }, [handler])

  useEffect(() => {
    return subscribeToEvents(event => handlerRef.current(event))
  }, [])
}

/** Dyskretny stan połączenia: 'connecting' | 'connected' | 'reconnecting' | 'disconnected'. */
export function useConnectionStatus() {
  const [status, setStatus] = useState(getConnectionStatus)
  useEffect(() => subscribeToStatus(setStatus), [])
  return status
}

/**
 * Wywołuje onReconnect() gdy połączenie wraca PO wcześniejszym rozłączeniu — NIE przy pierwszym
 * połączeniu po starcie aplikacji (na starcie dane i tak są ładowane normalnym fetchem).
 * WebSocket jest tylko sygnałem "coś się zmieniło" — baza pozostaje źródłem prawdy, więc po
 * odzyskaniu połączenia trzeba realnie pobrać aktualny stan na nowo (zdarzenia z okresu przerwy
 * mogły zostać bezpowrotnie przegapione).
 */
export function useReconnectResync(onReconnect) {
  const callbackRef = useRef(onReconnect)
  useEffect(() => { callbackRef.current = onReconnect }, [onReconnect])
  const hasConnectedBefore = useRef(false)

  useEffect(() => {
    return subscribeToStatus(status => {
      if (status === 'connected') {
        if (hasConnectedBefore.current) {
          callbackRef.current()
        }
        hasConnectedBefore.current = true
      }
    })
  }, [])
}
