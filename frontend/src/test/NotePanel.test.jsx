import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, act, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { clearNoteCache, getNoteCacheSize } from '../cache/noteCache'

// ── Mock WS transport — controllable from the test body via wsMock ──────────────────────────
const wsMock = vi.hoisted(() => ({
  eventListeners: [],
  statusListeners: [],
  status: 'connected',
}))

vi.mock('@/ws/socket', () => ({
  openApplicationSocket: vi.fn(),
  closeApplicationSocket: vi.fn(),
  subscribeToEvents: (fn) => {
    wsMock.eventListeners.push(fn)
    return () => { wsMock.eventListeners = wsMock.eventListeners.filter(f => f !== fn) }
  },
  subscribeToStatus: (fn) => {
    fn(wsMock.status)
    wsMock.statusListeners.push(fn)
    return () => { wsMock.statusListeners = wsMock.statusListeners.filter(f => f !== fn) }
  },
  getConnectionStatus: () => wsMock.status,
}))

vi.mock('@/ws/clientId', () => ({ getClientId: () => 'this-client' }))

// ── Mock API — controllable via getByTopicMock / saveMock ───────────────────────────────────
const getByTopicMock = vi.fn()
const saveMock = vi.fn()
const generateMock = vi.fn()
const refineMock = vi.fn()

vi.mock('@/api/services', () => ({
  notesApi: {
    getByTopic: (...args) => getByTopicMock(...args),
    save: (...args) => saveMock(...args),
    generate: (...args) => generateMock(...args),
    refine: (...args) => refineMock(...args),
  },
  topicsApi: {
    update: vi.fn(() => Promise.resolve({ data: {} })),
  },
  attachmentsApi: {
    listByTopic: vi.fn(() => Promise.resolve({ data: [] })),
    upload: vi.fn(),
    remove: vi.fn(),
    viewUrl: (id) => `/api/attachments/${id}/view`,
    downloadUrl: (id) => `/api/attachments/${id}/download`,
  },
}))

const NotePanel = (await import('../components/knowledge/NotePanel')).default

function emitWsEvent(event) {
  act(() => { wsMock.eventListeners.forEach(fn => fn(event)) })
}

function setWsStatus(status) {
  act(() => {
    wsMock.status = status
    wsMock.statusListeners.forEach(fn => fn(status))
  })
}

function makeTopic(overrides = {}) {
  return {
    id: 't1',
    title: 'Temat testowy',
    type: 'NOTE',
    status: 'NOTE_ADDED',
    detailLevel: 'MEDIUM',
    author: 'tester',
    categoryId: 'cat-1',
    version: 1,
    createdAt: '2026-08-01T10:00:00Z',
    ...overrides,
  }
}

function makeNote(overrides = {}) {
  return {
    topicId: 't1',
    version: 1,
    contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Treść notatki' }] }] },
    createdAt: '2026-08-01T10:05:00Z',
    updatedAt: '2026-08-01T10:05:00Z',
    ...overrides,
  }
}

function notFoundError() {
  return { status: 404, message: 'Not found' }
}

function renderPanel(topic, props = {}) {
  return render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} {...props} />)
}

/** Klika przycisk potwierdzenia w aktualnie otwartym ConfirmModal (nie myli go z przyciskiem
 * spustowym o tej samej etykiecie — np. "Edytuj" istnieje jednocześnie jako trigger i jako
 * potwierdzenie w modalu). */
async function confirmModal(user) {
  const btn = document.querySelector('.cfm-btn-confirm')
  if (!btn) throw new Error('ConfirmModal nie jest otwarty')
  await user.click(btn)
}

beforeEach(() => {
  clearNoteCache()
  wsMock.eventListeners = []
  wsMock.statusListeners = []
  wsMock.status = 'connected'
  getByTopicMock.mockReset()
  saveMock.mockReset()
  generateMock.mockReset()
  refineMock.mockReset()
})

describe('NotePanel — generowanie z AI', () => {
  beforeEach(() => {
    HTMLDialogElement.prototype.showModal ??= function () { this.open = true }
    HTMLDialogElement.prototype.close ??= function () { this.open = false }
  })

  it('przekazuje wytyczne i otwiera szkic do edycji, a anulowanie przywraca zapisaną treść', async () => {
    getByTopicMock.mockResolvedValue({ data: makeNote() })
    generateMock.mockResolvedValue({ data: {
      contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Szkic z AI' }] }] },
    } })
    const user = userEvent.setup()
    renderPanel(makeTopic({ shortPrompt: 'Opis tematu', detailLevel: 'HIGH' }))
    await screen.findByText('Treść notatki')

    await user.click(screen.getByRole('button', { name: 'Generuj z AI' }))
    const modal = within(screen.getByRole('dialog', { name: 'Wygeneruj notatkę z AI' }))
    const instruction = modal.getByLabelText('Wytyczne dla AI (opcjonalnie)')
    const difficulty = modal.getByLabelText('Poziom szczegółowości szkicu')
    expect(instruction.value).toBe('Opis tematu')
    expect(difficulty.value).toBe('HARD')
    await user.clear(instruction)
    await user.type(instruction, 'Dodaj przykłady')
    await user.selectOptions(difficulty, 'BASIC')
    await user.click(modal.getByRole('button', { name: 'Wygeneruj szkic' }))

    await screen.findByText('Szkic z AI')
    expect(generateMock).toHaveBeenCalledWith('t1', {
      instruction: 'Dodaj przykłady', difficulty: 'BASIC',
    }, expect.any(AbortSignal))
    expect(saveMock).not.toHaveBeenCalled()
    await user.click(screen.getByRole('button', { name: 'Anuluj' }))
    await screen.findByText('Treść notatki')
  })

  it('dopracowuje bieżący szkic bez zapisywania go ani przesyłania obrazów', async () => {
    const original = makeNote()
    original.contentJson.content.push({ type: 'image', attrs: { src: '/api/test.png', alt: 'Obraz notatki' } })
    getByTopicMock.mockResolvedValue({ data: original })
    const revised = makeNote()
    revised.contentJson.content[0].content[0].text = 'Dopracowana notatka'
    refineMock.mockResolvedValue({ data: revised })
    const user = userEvent.setup()
    const { container } = renderPanel(makeTopic())
    await screen.findByText('Treść notatki')

    await user.click(screen.getByRole('button', { name: 'Edytuj' }))
    await confirmModal(user)
    const editor = container.querySelector('.tiptap').editor
    act(() => editor.commands.insertContentAt(editor.state.doc.firstChild.nodeSize - 1, ' Dopisek'))
    await user.click(screen.getByRole('button', { name: 'Dopracuj z AI' }))
    const modal = within(screen.getByRole('dialog', { name: 'Dopracuj notatkę z AI' }))
    const submit = modal.getByRole('button', { name: 'Dopracuj szkic' })
    expect(submit).toBeDisabled()
    await user.type(modal.getByLabelText('Co zmienić w notatce?'), 'Dodaj diagram')
    await user.click(submit)

    await screen.findByText('Dopracowana notatka')
    expect(refineMock).toHaveBeenCalledWith('t1', {
      instruction: 'Dodaj diagram', currentNote: 'Treść notatki Dopisek',
    }, expect.any(AbortSignal))
    expect(generateMock).not.toHaveBeenCalled()
    expect(screen.getByRole('img', { name: 'Obraz notatki' })).toBeInTheDocument()
    expect(saveMock).not.toHaveBeenCalled()
  })
})

afterEach(() => {
  vi.useRealTimers()
  clearNoteCache()
})

describe('NotePanel — cache notatek', () => {
  it('pierwsze otwarcie tematu wykonuje request i wypełnia cache', async () => {
    getByTopicMock.mockResolvedValue({ data: makeNote() })
    renderPanel(makeTopic())

    await screen.findByText('Treść notatki')
    expect(getByTopicMock).toHaveBeenCalledTimes(1)
    expect(getNoteCacheSize()).toBe(1)
  })

  it('ponowne wejście w tę samą notatkę w czasie 30 minut NIE wykonuje kolejnego requestu', async () => {
    getByTopicMock.mockResolvedValue({ data: makeNote() })
    const topic = makeTopic()
    const { rerender } = renderPanel(topic, { key: 'a' })
    await screen.findByText('Treść notatki')
    expect(getByTopicMock).toHaveBeenCalledTimes(1)

    // Przełącz na inny temat, potem wróć do pierwszego.
    const otherTopic = makeTopic({ id: 't2' })
    getByTopicMock.mockResolvedValueOnce({ data: makeNote({ topicId: 't2', contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Inna notatka' }] }] } }) })
    rerender(<NotePanel topic={otherTopic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await screen.findByText('Inna notatka')

    rerender(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await screen.findByText('Treść notatki')

    // Łącznie dwa unikalne tematy -> dwa requesty, ale drugie wejście w t1 NIE wywołało trzeciego.
    expect(getByTopicMock).toHaveBeenCalledTimes(2)
  })

  it('po wygaśnięciu (30 minut) ponownie pobiera notatkę z backendu', async () => {
    vi.useFakeTimers()
    getByTopicMock.mockResolvedValue({ data: makeNote() })
    const topic = makeTopic()
    const { rerender } = render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await vi.waitFor(() => expect(getByTopicMock).toHaveBeenCalledTimes(1))

    const otherTopic = makeTopic({ id: 't2' })
    rerender(<NotePanel topic={otherTopic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await vi.waitFor(() => expect(getByTopicMock).toHaveBeenCalledTimes(2))

    await act(async () => { await vi.advanceTimersByTimeAsync(31 * 60 * 1000) })

    rerender(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await vi.waitFor(() => expect(getByTopicMock).toHaveBeenCalledTimes(3))
  })

  it('zapis notatki aktualizuje cache bez dodatkowego pobrania', async () => {
    getByTopicMock.mockResolvedValueOnce({ data: makeNote() })
    const savedNote = makeNote({ version: 2, contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Nowa treść' }] }] } })
    saveMock.mockResolvedValue({ data: savedNote })
    const topic = makeTopic()
    const user = userEvent.setup()
    renderPanel(topic)
    await screen.findByText('Treść notatki')

    await user.click(screen.getByRole('button', { name: 'Edytuj' }))
    await confirmModal(user)
    await user.click(screen.getByRole('button', { name: 'Zapisz' }))
    await confirmModal(user)

    await waitFor(() => expect(saveMock).toHaveBeenCalledTimes(1))
    await screen.findByText('Nowa treść')
    // Zapis nie powinien wywoływać dodatkowego GET-a poza początkowym ładowaniem — cache
    // dostaje dokładnie to, co zwrócił notesApi.save.
    expect(getByTopicMock).toHaveBeenCalledTimes(1)
    expect(getNoteCacheSize()).toBe(1)
  })

  it('NOTE_SAVED innego klienta dla NIEOTWARTEGO tematu unieważnia jego cache, bez dodatkowego pobrania', async () => {
    getByTopicMock.mockResolvedValue({ data: makeNote() })
    const topic = makeTopic({ id: 't1' })
    renderPanel(topic)
    await screen.findByText('Treść notatki')
    expect(getNoteCacheSize()).toBe(1)

    emitWsEvent({
      eventId: 'e1', type: 'NOTE_SAVED', occurredAt: new Date().toISOString(),
      sourceClientId: 'other-client',
      payload: { topicId: 'OTHER-TOPIC-NOT-OPEN', topicStatus: 'NOTE_ADDED' },
    })

    // Nie powinno nastąpić żadne dodatkowe pobranie (event dotyczy tematu, którego nie widzimy).
    expect(getByTopicMock).toHaveBeenCalledTimes(1)
    expect(getNoteCacheSize()).toBe(1) // cache dla t1 (otwartego) pozostaje nietknięty
  })

  it('NOTE_SAVED innego klienta dla AKTUALNIE otwartej notatki (tryb podglądu) odświeża widok i cache', async () => {
    getByTopicMock.mockResolvedValueOnce({ data: makeNote() })
    const topic = makeTopic()
    renderPanel(topic)
    await screen.findByText('Treść notatki')

    const refreshed = makeNote({ version: 2, contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Zaktualizowana z innej karty' }] }] } })
    getByTopicMock.mockResolvedValueOnce({ data: refreshed })

    emitWsEvent({
      eventId: 'e2', type: 'NOTE_SAVED', occurredAt: new Date().toISOString(),
      sourceClientId: 'other-client',
      payload: { topicId: 't1', topicStatus: 'NOTE_ADDED' },
    })

    await screen.findByText('Zaktualizowana z innej karty')
    expect(getByTopicMock).toHaveBeenCalledTimes(2)
  })

  it('NOTE_SAVED innego klienta w trakcie EDYCJI nie nadpisuje niezapisanej treści w edytorze', async () => {
    getByTopicMock.mockResolvedValueOnce({ data: makeNote() })
    const topic = makeTopic()
    const user = userEvent.setup()
    renderPanel(topic)
    await screen.findByText('Treść notatki')

    await user.click(screen.getByRole('button', { name: 'Edytuj' }))
    await confirmModal(user)
    await screen.findByRole('button', { name: 'Anuluj' }) // jesteśmy w trybie edycji

    emitWsEvent({
      eventId: 'e3', type: 'NOTE_SAVED', occurredAt: new Date().toISOString(),
      sourceClientId: 'other-client',
      payload: { topicId: 't1', topicStatus: 'NOTE_ADDED' },
    })

    // Treść w edytorze pozostaje ta sama (widoczny tekst niezmieniony), pojawia się komunikat konfliktu.
    expect(screen.getByText('Treść notatki')).toBeInTheDocument()
    expect(await screen.findByText(/zapisał w tle nowszą wersję/)).toBeInTheDocument()
    // Nie wykonano dodatkowego GET-a (nie odświeżamy po cichu w trybie edycji).
    expect(getByTopicMock).toHaveBeenCalledTimes(1)
  })

  it('własne zdarzenie NOTE_SAVED (ten sam klient) jest ignorowane', async () => {
    getByTopicMock.mockResolvedValueOnce({ data: makeNote() })
    const topic = makeTopic()
    renderPanel(topic)
    await screen.findByText('Treść notatki')

    emitWsEvent({
      eventId: 'e4', type: 'NOTE_SAVED', occurredAt: new Date().toISOString(),
      sourceClientId: 'this-client', // == getClientId() zmockowany wyżej
      payload: { topicId: 't1', topicStatus: 'NOTE_ADDED' },
    })

    expect(getByTopicMock).toHaveBeenCalledTimes(1) // brak reakcji na własne zdarzenie
  })

  it('reconnect (odzyskanie połączenia WS) czyści cały cache', async () => {
    getByTopicMock.mockResolvedValueOnce({ data: makeNote() })
    const topic = makeTopic()
    renderPanel(topic)
    await screen.findByText('Treść notatki')
    expect(getNoteCacheSize()).toBe(1)

    getByTopicMock.mockResolvedValueOnce({ data: makeNote({ version: 3 }) })

    // Symuluj utratę i odzyskanie połączenia (useReconnectResync reaguje TYLKO na powrót PO
    // wcześniejszym rozłączeniu, nie na pierwsze połączenie po starcie).
    setWsStatus('disconnected')
    setWsStatus('connected')

    await waitFor(() => expect(getByTopicMock).toHaveBeenCalledTimes(2))
  })

  it('pierwsze połączenie WS po starcie (bez wcześniejszego rozłączenia) NIE czyści cache', async () => {
    getByTopicMock.mockResolvedValueOnce({ data: makeNote() })
    const topic = makeTopic()
    renderPanel(topic)
    await screen.findByText('Treść notatki')
    expect(getNoteCacheSize()).toBe(1)

    // subscribeToStatus już wywołał fn('connected') od razu przy montowaniu — to jest to
    // "pierwsze połączenie", useReconnectResync go pomija.
    expect(getByTopicMock).toHaveBeenCalledTimes(1)
  })

  it('reloadAfterConflict zawsze omija cache i zastępuje jego wpis świeżą odpowiedzią', async () => {
    getByTopicMock.mockResolvedValueOnce({ data: makeNote() })
    const topic = makeTopic()
    const user = userEvent.setup()
    renderPanel(topic)
    await screen.findByText('Treść notatki')
    expect(getNoteCacheSize()).toBe(1) // wpis w cache z pierwszego ładowania — wciąż ważny (nie wygasł)

    // Wejdź w tryb edycji, potem odbierz NOTE_SAVED innego klienta — tryb edycji NIE odświeża
    // się po cichu, tylko pokazuje komunikat konfliktu i unieważnia stary wpis cache.
    await user.click(screen.getByRole('button', { name: 'Edytuj' }))
    await confirmModal(user)

    emitWsEvent({
      eventId: 'e6', type: 'NOTE_SAVED', occurredAt: new Date().toISOString(),
      sourceClientId: 'other-client',
      payload: { topicId: 't1', topicStatus: 'NOTE_ADDED' },
    })
    const refreshBtn = await screen.findByText('Odśwież aktualną wersję')
    expect(getByTopicMock).toHaveBeenCalledTimes(1) // konflikt w trybie edycji NIE pobiera po cichu
    expect(getNoteCacheSize()).toBe(0) // unieważniony przez zdarzenie WS

    const freshVersion = makeNote({ version: 5, contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Po konflikcie' }] }] } })
    getByTopicMock.mockResolvedValueOnce({ data: freshVersion })

    await user.click(refreshBtn)

    await screen.findByText('Po konflikcie')
    expect(getByTopicMock).toHaveBeenCalledTimes(2) // reloadAfterConflict ZAWSZE pyta backend, nigdy cache
    expect(getNoteCacheSize()).toBe(1) // ...i zastępuje wpis w cache świeżą odpowiedzią
  })

  it('szybkie przełączanie tematów nie pokazuje odpowiedzi dotyczącej poprzedniego tematu (AbortController)', async () => {
    let resolveFirst
    const firstPromise = new Promise(resolve => { resolveFirst = resolve })
    getByTopicMock.mockImplementationOnce(() => firstPromise)
    getByTopicMock.mockResolvedValueOnce({ data: makeNote({ topicId: 't2', contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Temat drugi' }] }] } }) })

    const topicA = makeTopic({ id: 't1' })
    const topicB = makeTopic({ id: 't2' })
    const { rerender } = renderPanel(topicA)

    // Zanim odpowiedź dla t1 wróci, przełącz na t2.
    rerender(<NotePanel topic={topicB} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await screen.findByText('Temat drugi')

    // Teraz spóźniona odpowiedź dla t1 wraca — nie może nadpisać tego, co widzimy (t2).
    resolveFirst({ data: makeNote({ topicId: 't1', contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Temat pierwszy (spóźniony)' }] }] } }) })
    await act(async () => { await Promise.resolve() })

    expect(screen.getByText('Temat drugi')).toBeInTheDocument()
    expect(screen.queryByText('Temat pierwszy (spóźniony)')).not.toBeInTheDocument()
  })

  it('cache odpowiedzi 404 — ponowne wejście w ciągu 2 minut nie wykonuje kolejnego requestu', async () => {
    vi.useFakeTimers()
    getByTopicMock.mockRejectedValue(notFoundError())
    const topic = makeTopic({ id: 't1' })
    const { rerender } = render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await vi.waitFor(() => expect(getByTopicMock).toHaveBeenCalledTimes(1))
    await vi.waitFor(() => expect(screen.getByText('Brak treści — kliknij „Edytuj”, aby zacząć pisać.')).toBeInTheDocument())

    const other = makeTopic({ id: 't2' })
    rerender(<NotePanel topic={other} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await vi.waitFor(() => expect(getByTopicMock).toHaveBeenCalledTimes(2))

    rerender(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await vi.waitFor(() => expect(screen.getByText('Brak treści — kliknij „Edytuj”, aby zacząć pisać.')).toBeInTheDocument())
    expect(getByTopicMock).toHaveBeenCalledTimes(2) // bez trzeciego requestu — 404 wciąż w cache (< 2 min)
  })
})
