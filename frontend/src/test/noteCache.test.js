import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  getCachedNote,
  setCachedNote,
  invalidateCachedNote,
  clearNoteCache,
  getCachedNoteMissing,
  setCachedNoteMissing,
  getNoteCacheSize,
  getNoteCacheMissingSize,
} from '../cache/noteCache'

function makeNote(overrides = {}) {
  return {
    topicId: 't1',
    version: 1,
    contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'hello' }] }] },
    updatedAt: '2026-08-23T10:00:00Z',
    ...overrides,
  }
}

describe('noteCache', () => {
  beforeEach(() => {
    clearNoteCache()
  })

  afterEach(() => {
    vi.useRealTimers()
    clearNoteCache()
  })

  it('1. zapisuje i odczytuje notatkę', () => {
    setCachedNote('t1', makeNote())
    const cached = getCachedNote('t1')
    expect(cached).not.toBeNull()
    expect(cached.version).toBe(1)
    expect(cached.contentJson.content[0].content[0].text).toBe('hello')
  })

  it('2. zwraca null, gdy wpisu nie ma', () => {
    expect(getCachedNote('nieznany-temat')).toBeNull()
  })

  it('3. wpis wygasa po 30 minutach od ostatniego użycia', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'))
    setCachedNote('t1', makeNote())

    vi.setSystemTime(new Date('2026-01-01T00:29:59Z')) // 29:59 — jeszcze ważny
    expect(getCachedNote('t1')).not.toBeNull()

    vi.setSystemTime(new Date('2026-01-01T00:59:59Z')) // 30:00 od poprzedniego ODCZYTU (odnowionego) — jeszcze ważny
    expect(getCachedNote('t1')).not.toBeNull()

    vi.setSystemTime(new Date('2026-01-01T01:30:00Z')) // 30:01 od ostatniego użycia — wygasły
    expect(getCachedNote('t1')).toBeNull()
  })

  it('4. każde użycie odnawia czas ostatniego użycia (przesuwny TTL)', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'))
    setCachedNote('t1', makeNote())

    // Tuż przed wygaśnięciem — odczyt odnawia ważność o kolejne 30 minut.
    vi.setSystemTime(new Date('2026-01-01T00:29:00Z'))
    expect(getCachedNote('t1')).not.toBeNull()

    // Bez odnowienia wpis wygasłby o 00:30, ale odczyt w 00:29 przesunął ważność do ~00:59.
    vi.setSystemTime(new Date('2026-01-01T00:45:00Z'))
    expect(getCachedNote('t1')).not.toBeNull()
  })

  it('5. wygasły wpis jest usuwany z cache (nie tylko zwraca null)', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'))
    setCachedNote('t1', makeNote())
    expect(getNoteCacheSize()).toBe(1)

    vi.setSystemTime(new Date('2026-01-01T00:31:00Z'))
    expect(getCachedNote('t1')).toBeNull()
    expect(getNoteCacheSize()).toBe(0)
  })

  it('6. invalidateCachedNote usuwa konkretny wpis, nie ruszając innych', () => {
    setCachedNote('t1', makeNote({ topicId: 't1' }))
    setCachedNote('t2', makeNote({ topicId: 't2' }))
    invalidateCachedNote('t1')
    expect(getCachedNote('t1')).toBeNull()
    expect(getCachedNote('t2')).not.toBeNull()
  })

  it('7. clearNoteCache czyści cały cache (pozytywny i negatywny)', () => {
    setCachedNote('t1', makeNote())
    setCachedNoteMissing('t2')
    clearNoteCache()
    expect(getCachedNote('t1')).toBeNull()
    expect(getCachedNoteMissing('t2')).toBe(false)
    expect(getNoteCacheSize()).toBe(0)
    expect(getNoteCacheMissingSize()).toBe(0)
  })

  it('8. limit 50 wpisów — 51. wpis nie powiększa cache ponad limit', () => {
    for (let i = 0; i < 51; i++) {
      setCachedNote(`t${i}`, makeNote({ topicId: `t${i}` }))
    }
    expect(getNoteCacheSize()).toBe(50)
  })

  it('9. po przekroczeniu limitu usuwany jest najdawniej używany wpis (LRU)', () => {
    for (let i = 0; i < 50; i++) {
      setCachedNote(`t${i}`, makeNote({ topicId: `t${i}` }))
    }
    // Użyj t0 ponownie — teraz jest najświeższy, więc NIE powinien zostać wypchnięty.
    getCachedNote('t0')

    // 51. nowy wpis powinien wypchnąć t1 (najdawniej używany po odświeżeniu t0), nie t0.
    setCachedNote('t50', makeNote({ topicId: 't50' }))

    expect(getCachedNote('t0')).not.toBeNull()
    expect(getCachedNote('t1')).toBeNull()
    expect(getCachedNote('t50')).not.toBeNull()
    expect(getNoteCacheSize()).toBe(50)
  })

  it('10. cache odpowiedzi 404 ("brak notatki") jest ważny 2 minuty, krócej niż pozytywny', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'))
    setCachedNoteMissing('t1')
    expect(getCachedNoteMissing('t1')).toBe(true)

    vi.setSystemTime(new Date('2026-01-01T00:01:59Z'))
    expect(getCachedNoteMissing('t1')).toBe(true)

    vi.setSystemTime(new Date('2026-01-01T00:02:01Z'))
    expect(getCachedNoteMissing('t1')).toBe(false)
  })

  it('10b. zapisanie pozytywnego wpisu kasuje negatywny wpis dla tego samego tematu', () => {
    setCachedNoteMissing('t1')
    expect(getCachedNoteMissing('t1')).toBe(true)
    setCachedNote('t1', makeNote())
    expect(getCachedNoteMissing('t1')).toBe(false)
  })

  it('11a. mutacja obiektu PO setCachedNote nie wpływa na to, co jest w cache', () => {
    const note = makeNote()
    setCachedNote('t1', note)
    note.contentJson.content[0].content[0].text = 'ZMIENIONE-NA-ZEWNĄTRZ'
    note.version = 999

    const cached = getCachedNote('t1')
    expect(cached.contentJson.content[0].content[0].text).toBe('hello')
    expect(cached.version).toBe(1)
  })

  it('11b. mutacja obiektu zwróconego przez getCachedNote nie wpływa na kolejny odczyt z cache', () => {
    setCachedNote('t1', makeNote())
    const first = getCachedNote('t1')
    first.contentJson.content[0].content[0].text = 'ZMIENIONE-PRZEZ-KONSUMENTA'
    first.version = 999

    const second = getCachedNote('t1')
    expect(second.contentJson.content[0].content[0].text).toBe('hello')
    expect(second.version).toBe(1)
  })
})
