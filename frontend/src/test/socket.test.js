import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

class FakeWebSocket {
  static OPEN = 1
  static CONNECTING = 0
  static instances = []
  constructor(url) {
    this.url = url
    this.readyState = FakeWebSocket.CONNECTING
    FakeWebSocket.instances.push(this)
  }
  close() { this.readyState = 3; this.onclose?.() }
  open() { this.readyState = FakeWebSocket.OPEN; this.onopen?.() }
  message(data) { this.onmessage?.({ data }) }
}

let socket
beforeEach(async () => {
  vi.useFakeTimers()
  FakeWebSocket.instances = []
  vi.stubGlobal('WebSocket', FakeWebSocket)
  vi.resetModules()
  socket = await import('@/ws/socket')
})
afterEach(() => {
  socket.closeApplicationSocket()
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

const event = (id, type = 'TOPIC_UPDATED') => JSON.stringify({ eventId: id, type, occurredAt: '2030-01-01T00:00:00Z' })

describe('application socket', () => {
  it('opens a single connection to /ws/updates and reports status', () => {
    const statuses = []
    socket.subscribeToStatus(s => statuses.push(s))
    socket.openApplicationSocket()
    socket.openApplicationSocket()
    expect(FakeWebSocket.instances).toHaveLength(1)
    expect(FakeWebSocket.instances[0].url).toMatch(/\/ws\/updates$/)
    FakeWebSocket.instances[0].open()
    expect(socket.getConnectionStatus()).toBe('connected')
    expect(statuses).toEqual(['disconnected', 'connecting', 'connected'])
  })

  it('delivers valid events once and ignores duplicates and garbage', () => {
    const got = []
    const unsubscribe = socket.subscribeToEvents(e => got.push(e.eventId))
    socket.openApplicationSocket()
    const ws = FakeWebSocket.instances[0]
    ws.open()
    ws.message(event('a'))
    ws.message(event('a'))
    ws.message('not json')
    ws.message(JSON.stringify({ type: 'X' }))
    ws.message(event('b'))
    expect(got).toEqual(['a', 'b'])
    unsubscribe()
    ws.message(event('c'))
    expect(got).toEqual(['a', 'b'])
  })

  it('a throwing listener does not break the others', () => {
    const got = []
    const err = vi.spyOn(console, 'error').mockImplementation(() => {})
    socket.subscribeToEvents(() => { throw new Error('boom') })
    socket.subscribeToEvents(e => got.push(e.eventId))
    socket.openApplicationSocket()
    FakeWebSocket.instances[0].open()
    FakeWebSocket.instances[0].message(event('z'))
    expect(got).toEqual(['z'])
    err.mockRestore()
  })

  it('reconnects with backoff after an unexpected close, but not after manual close', () => {
    socket.openApplicationSocket()
    FakeWebSocket.instances[0].open()
    FakeWebSocket.instances[0].onclose()
    expect(socket.getConnectionStatus()).toBe('reconnecting')
    vi.advanceTimersByTime(2000)
    expect(FakeWebSocket.instances).toHaveLength(2)

    socket.closeApplicationSocket()
    expect(socket.getConnectionStatus()).toBe('disconnected')
    vi.advanceTimersByTime(60000)
    expect(FakeWebSocket.instances).toHaveLength(2)
  })
})
