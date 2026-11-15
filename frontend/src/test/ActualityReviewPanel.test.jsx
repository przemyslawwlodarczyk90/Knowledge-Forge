import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, act, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'

// ── Mock WS transport — sterowalny z ciała testu przez wsMock.eventListeners ────────────────
const wsMock = vi.hoisted(() => ({ eventListeners: [] }))

vi.mock('@/ws/socket', () => ({
  openApplicationSocket: vi.fn(),
  closeApplicationSocket: vi.fn(),
  subscribeToEvents: (fn) => {
    wsMock.eventListeners.push(fn)
    return () => { wsMock.eventListeners = wsMock.eventListeners.filter(f => f !== fn) }
  },
  subscribeToStatus: () => () => {},
  getConnectionStatus: () => 'connected',
}))
vi.mock('@/ws/clientId', () => ({ getClientId: () => 'this-client' }))

const navigateMock = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return { ...actual, useNavigate: () => navigateMock }
})

vi.mock('@/api/services', () => ({
  categoriesApi: {
    getTree: vi.fn(() => Promise.resolve({ data: { children: [
      { id: 'cat-1', name: 'Kategoria A', children: [] },
    ] } })),
  },
  topicsApi: {
    listAuthors: vi.fn(() => Promise.resolve({ data: ['Ala', 'Bob'] })),
    actualityReview: vi.fn(() => Promise.resolve({ data: [] })),
    verifyActuality: vi.fn(() => Promise.resolve({ data: {} })),
  },
}))

const ActualityReviewPanel = (await import('../components/knowledge/ActualityReviewPanel')).default

function emitWsEvent(event) {
  act(() => { wsMock.eventListeners.forEach(fn => fn(event)) })
}

function renderPanel() {
  return render(
    <MemoryRouter>
      <ActualityReviewPanel />
    </MemoryRouter>,
  )
}

const RECORD_1 = {
  id: 'topic-1', title: 'Stara notatka', author: 'Ala', type: 'NOTE', categoryId: 'cat-1',
  createdAt: '2024-01-01T10:00:00Z', lastVerificationOfActualityDate: null,
  actualityVerified: false, version: 3,
}
const RECORD_2 = {
  id: 'topic-2', title: 'Stara instrukcja', author: 'Bob', type: 'INSTRUCTION', categoryId: 'cat-1',
  createdAt: '2023-05-01T10:00:00Z', lastVerificationOfActualityDate: '2024-06-01T10:00:00Z',
  actualityVerified: false, version: 1,
}

beforeEach(() => {
  vi.clearAllMocks()
  wsMock.eventListeners = []
})

async function openPanel(user) {
  await user.click(screen.getByRole('button', { name: /weryfikacja aktualności/i }))
  await screen.findByRole('dialog', { name: 'Weryfikacja aktualności' })
}

describe('ActualityReviewPanel', () => {
  it('otwarcie panelu automatycznie pobiera listę bez wybierania filtra', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)

    await screen.findByText('Stara notatka')
    expect(topicsApi.actualityReview).toHaveBeenCalledWith(undefined)
  })

  it('filtr autora zawęża listę — "Dowolny" pokazuje wszystkie, wybór autora woła API z tym autorem', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await screen.findByText('Stara notatka')

    const authorSelect = document.body.querySelector('.sop-select')
    await user.selectOptions(authorSelect, 'Ala')

    expect(topicsApi.actualityReview).toHaveBeenLastCalledWith('Ala')
  })

  it('pusta lista pokazuje komunikat "Wszystkie notatki są aktualne"', async () => {
    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)

    await screen.findByText('Wszystkie notatki są aktualne')
  })

  it('błąd pobierania listy pokazuje komunikat z możliwością ponowienia', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.actualityReview.mockRejectedValueOnce(new Error('boom'))
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)

    await screen.findByText('Nie udało się pobrać listy')
    await user.click(screen.getByRole('button', { name: 'Spróbuj ponownie' }))
    await screen.findByText('Stara notatka')
  })

  it('rekord bez wcześniejszej weryfikacji pokazuje "Nigdy — termin liczony od utworzenia"', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)

    await screen.findByText('Nigdy — termin liczony od utworzenia')
  })

  it('kliknięcie "Potwierdź aktualność" NIE woła od razu API — najpierw pokazuje modal', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await screen.findByText('Stara notatka')

    await user.click(screen.getByRole('button', { name: 'Potwierdź aktualność' }))

    expect(topicsApi.verifyActuality).not.toHaveBeenCalled()
    expect(screen.getByText('Potwierdzić aktualność?')).toBeInTheDocument()
    expect(screen.getByText(/bez edycji treści/)).toBeInTheDocument()
  })

  it('modal potwierdzenia: Anuluj zamyka modal bez wywołania API', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await screen.findByText('Stara notatka')
    await user.click(screen.getByRole('button', { name: 'Potwierdź aktualność' }))

    await user.click(screen.getByRole('button', { name: 'Anuluj' }))

    expect(topicsApi.verifyActuality).not.toHaveBeenCalled()
    expect(screen.queryByText('Potwierdzić aktualność?')).not.toBeInTheDocument()
    // Rekord zostaje na liście, bo nic nie zostało potwierdzone.
    expect(screen.getByText('Stara notatka')).toBeInTheDocument()
  })

  it('potwierdzenie w modalu woła verifyActuality z id i version, usuwa rekord z listy po sukcesie', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })
    topicsApi.verifyActuality.mockResolvedValueOnce({ data: { ...RECORD_1, actualityVerified: true, version: 4 } })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await screen.findByText('Stara notatka')
    await user.click(screen.getByRole('button', { name: 'Potwierdź aktualność' }))

    const dialogButtons = screen.getAllByRole('button', { name: 'Potwierdź aktualność' })
    await user.click(dialogButtons[dialogButtons.length - 1])

    expect(topicsApi.verifyActuality).toHaveBeenCalledWith('topic-1', 3)
    await screen.findByText('Wszystkie notatki są aktualne')
    expect(screen.queryByText('Stara notatka')).not.toBeInTheDocument()
    expect(screen.queryByText('Potwierdzić aktualność?')).not.toBeInTheDocument()
  })

  it('konflikt wersji (409) zostawia listę bez rekordu i odświeża ją, pokazując komunikat', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })
    const conflictErr = Object.assign(new Error('conflict'), { status: 409 })
    topicsApi.verifyActuality.mockRejectedValueOnce(conflictErr)
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_2] })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await screen.findByText('Stara notatka')
    await user.click(screen.getByRole('button', { name: 'Potwierdź aktualność' }))
    const dialogButtons = screen.getAllByRole('button', { name: 'Potwierdź aktualność' })
    await user.click(dialogButtons[dialogButtons.length - 1])

    await screen.findByText('Stara instrukcja')
    expect(screen.queryByText('Potwierdzić aktualność?')).not.toBeInTheDocument()
    expect(topicsApi.actualityReview).toHaveBeenCalledTimes(2)
  })

  it('błąd inny niż 409 zostawia rekord na liście i modal otwarty', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })
    topicsApi.verifyActuality.mockRejectedValueOnce(new Error('server error'))

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await screen.findByText('Stara notatka')
    await user.click(screen.getByRole('button', { name: 'Potwierdź aktualność' }))
    const dialogButtons = screen.getAllByRole('button', { name: 'Potwierdź aktualność' })
    await user.click(dialogButtons[dialogButtons.length - 1])

    // Modal i rekord ZOSTAJĄ — błąd inny niż 409 nie usuwa wpisu z listy ani nie zamyka modala
    // (użytkownik może spróbować ponownie), inaczej niż przy sukcesie czy konflikcie wersji.
    await act(async () => { await Promise.resolve() })
    expect(screen.getByText('Potwierdzić aktualność?')).toBeInTheDocument()
    expect(screen.getByText('Stara notatka')).toBeInTheDocument()
    expect(topicsApi.verifyActuality).toHaveBeenCalledTimes(1)
  })

  it('"Edytuj" zamyka panel i nawiguje do tematu w trybie edycji, bez potwierdzania aktualności', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await screen.findByText('Stara notatka')

    await user.click(screen.getByRole('button', { name: 'Edytuj' }))

    expect(navigateMock).toHaveBeenCalledWith('/dashboard?topic=topic-1&edit=1')
    expect(topicsApi.verifyActuality).not.toHaveBeenCalled()
    expect(screen.queryByRole('dialog', { name: 'Weryfikacja aktualności' })).not.toBeInTheDocument()
  })

  it('reaguje na zdarzenie WebSocket ACTUALITY_REVIEW_CHANGED odświeżeniem listy', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_1] })
    topicsApi.actualityReview.mockResolvedValueOnce({ data: [RECORD_2] })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await screen.findByText('Stara notatka')

    emitWsEvent({ type: 'ACTUALITY_REVIEW_CHANGED' })

    await screen.findByText('Stara instrukcja')
    expect(topicsApi.actualityReview).toHaveBeenCalledTimes(2)
  })

  it('nie odświeża listy przez WS, gdy panel jest zamknięty', async () => {
    const { topicsApi } = await import('@/api/services')
    renderPanel()

    emitWsEvent({ type: 'ACTUALITY_REVIEW_CHANGED' })

    expect(topicsApi.actualityReview).not.toHaveBeenCalled()
  })

  it('panel zamyka się klawiszem Escape (po animacji wysunięcia)', async () => {
    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await waitFor(() => expect(document.body.querySelector('.sop-panel')).toHaveClass('sop-open'))

    act(() => { window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' })) })

    // Animacja wyjścia: klasa "--open" znika od razu (cofa transform), sam dialog usuwany z DOM
    // dopiero po zakończeniu animacji (zob. FiltersPanel.test.jsx — ten sam wzorzec, wspólny SlideOverPanel).
    await waitFor(() => expect(document.body.querySelector('.sop-panel')).not.toHaveClass('sop-open'))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Weryfikacja aktualności' })).not.toBeInTheDocument(), { timeout: 1000 })
  })
})
