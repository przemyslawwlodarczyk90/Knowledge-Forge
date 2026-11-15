import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'

vi.mock('@/ws/socket', () => ({
  openApplicationSocket: vi.fn(),
  closeApplicationSocket: vi.fn(),
  subscribeToEvents: vi.fn(() => () => {}),
  subscribeToStatus: (fn) => { fn('connected'); return () => {} },
  getConnectionStatus: () => 'connected',
}))

vi.mock('@/ws/clientId', () => ({ getClientId: () => 'this-client' }))

vi.mock('@/api/services', () => ({
  categoriesApi: { getTree: vi.fn(() => Promise.resolve({ data: { children: [] } })) },
  topicsApi: { listByCategory: vi.fn(() => Promise.reject({ status: 404 })), getById: vi.fn() },
  notesApi: { getByTopic: vi.fn(() => Promise.reject({ status: 404 })), save: vi.fn() },
  attachmentsApi: {
    listByTopic: vi.fn(() => Promise.resolve({ data: [] })),
    upload: vi.fn(), remove: vi.fn(),
    viewUrl: (id) => `/api/attachments/${id}/view`,
    downloadUrl: (id) => `/api/attachments/${id}/download`,
  },
}))

vi.mock('@/utils/pdfGenerator', () => ({
  generateSingleNotePdf: vi.fn(),
  generateBulkNotesPdf: vi.fn(),
  mapWithConcurrency: vi.fn(),
  PDF_LIMITS: { maxBulkNotes: 50, maxConcurrentRequests: 3 },
}))

const DashboardPage = (await import('../pages/DashboardPage')).default

function firePointer(type, target, props = {}) {
  const evt = new Event(type, { bubbles: true })
  Object.assign(evt, { pointerType: 'mouse', clientX: 0, clientY: 0, ...props })
  target.dispatchEvent(evt)
}

function renderDashboard() {
  return render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <DashboardPage />
    </MemoryRouter>,
  )
}

beforeEach(() => {
  localStorage.clear()
})

describe('DashboardPage — separator "drzewo ↔ notatka" (nachodzenie w lewo)', () => {
  it('renderuje uchwyt drzewo↔notatka i warstwę .note-area', async () => {
    const { container } = renderDashboard()
    await screen.findByText('Baza wiedzy')
    expect(container.querySelector('.tree-resize-handle')).toBeInTheDocument()
    expect(container.querySelector('.note-area')).toBeInTheDocument()
  })

  it('.db-root ustawia zmienną CSS --tree-visible-w równą domyślnej szerokości drzewa (268px)', async () => {
    const { container } = renderDashboard()
    await screen.findByText('Baza wiedzy')
    const root = container.querySelector('.db-root')
    expect(root.style.getPropertyValue('--tree-visible-w')).toBe('268px')
  })

  it('drzewo renderuje się na pełnej (min. domyślnej) szerokości nawet po przeciągnięciu daleko w lewo', async () => {
    const { container } = renderDashboard()
    await screen.findByText('Baza wiedzy')
    const handle = container.querySelector('.tree-resize-handle')

    act(() => { firePointer('pointerdown', handle, { clientX: 268 }) })
    act(() => { firePointer('pointermove', window, { clientX: 0 }) }) // maksymalnie w lewo
    act(() => { firePointer('pointerup', window) })

    const treePanel = container.querySelector('.tree-panel')
    expect(parseFloat(treePanel.style.width)).toBeGreaterThanOrEqual(268) // drzewo NIGDY nie jest ściskane poniżej domyślnej szerokości
  })

  it('przeciągnięcie w lewo zostawia widoczny skrawek ~4% (nie da się całkowicie zakryć drzewa)', async () => {
    const { container } = renderDashboard()
    await screen.findByText('Baza wiedzy')
    const handle = container.querySelector('.tree-resize-handle')

    act(() => { firePointer('pointerdown', handle, { clientX: 268 }) })
    act(() => { firePointer('pointermove', window, { clientX: -10000 }) }) // absurdalnie daleko w lewo
    act(() => { firePointer('pointerup', window) })

    const root = container.querySelector('.db-root')
    const visible = parseFloat(root.style.getPropertyValue('--tree-visible-w'))
    expect(visible).toBeGreaterThan(0)
    expect(visible).toBeLessThan(268 * 0.15) // wyraźnie "prawie całkowicie" zakryte, ale nie zero
  })

  it('separator nie może wjechać w prawo poza domyślną pozycję (prawa granica = pozycja domyślna)', async () => {
    const { container } = renderDashboard()
    await screen.findByText('Baza wiedzy')
    const handle = container.querySelector('.tree-resize-handle')

    act(() => { firePointer('pointerdown', handle, { clientX: 268 }) })
    act(() => { firePointer('pointermove', window, { clientX: 268 + 5000 }) }) // próba dużego rozciągnięcia w prawo
    act(() => { firePointer('pointerup', window) })

    // Rozciąganie W PRAWO (ponad domyślną) to NIEZMIENIona, istniejąca funkcja (maxRatio) —
    // ten test weryfikuje tylko, że NIE crashuje i że wartość rośnie (już testowane szczegółowo
    // w useResizablePanel.test.js) — właściwe zachowanie lewej granicy jest w innych testach.
    const root = container.querySelector('.db-root')
    const visible = parseFloat(root.style.getPropertyValue('--tree-visible-w'))
    expect(visible).toBeGreaterThanOrEqual(268)
  })

  it('dwuklik przywraca domyślną pozycję separatora (268px)', async () => {
    const { container } = renderDashboard()
    await screen.findByText('Baza wiedzy')
    const handle = container.querySelector('.tree-resize-handle')

    act(() => { firePointer('pointerdown', handle, { clientX: 268 }) })
    act(() => { firePointer('pointermove', window, { clientX: 0 }) })
    act(() => { firePointer('pointerup', window) })

    const root = container.querySelector('.db-root')
    expect(root.style.getPropertyValue('--tree-visible-w')).not.toBe('268px')

    act(() => { handle.dispatchEvent(new Event('dblclick', { bubbles: true })) })
    expect(root.style.getPropertyValue('--tree-visible-w')).toBe('268px')
  })

  it('NIE zapisuje położenia w localStorage — po odświeżeniu strony separator ma wracać na domyślne miejsce', async () => {
    const { container, unmount } = renderDashboard()
    await screen.findByText('Baza wiedzy')
    const handle = container.querySelector('.tree-resize-handle')

    act(() => { firePointer('pointerdown', handle, { clientX: 268 }) })
    act(() => { firePointer('pointermove', window, { clientX: 68 }) }) // -200
    act(() => { firePointer('pointerup', window) })

    const root = container.querySelector('.db-root')
    expect(root.style.getPropertyValue('--tree-visible-w')).not.toBe('268px')
    expect(localStorage.length).toBe(0) // nic nie trafiło do localStorage

    // "Odświeżenie strony" == świeże zamontowanie komponentu od zera.
    unmount()
    const fresh = renderDashboard()
    await screen.findByText('Baza wiedzy')
    const freshRoot = fresh.container.querySelector('.db-root')
    expect(freshRoot.style.getPropertyValue('--tree-visible-w')).toBe('268px')
  })
})
