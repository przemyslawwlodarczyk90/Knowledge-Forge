import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import { clearNoteCache } from '../cache/noteCache'

vi.mock('@/ws/socket', () => ({
  openApplicationSocket: vi.fn(),
  closeApplicationSocket: vi.fn(),
  subscribeToEvents: vi.fn(() => () => {}),
  subscribeToStatus: (fn) => { fn('connected'); return () => {} },
  getConnectionStatus: () => 'connected',
}))

vi.mock('@/ws/clientId', () => ({ getClientId: () => 'this-client' }))

vi.mock('@/api/services', () => ({
  notesApi: {
    getByTopic: vi.fn(() => Promise.reject({ status: 404 })),
    save: vi.fn(),
  },
  topicsApi: { update: vi.fn(() => Promise.resolve({ data: {} })) },
  attachmentsApi: {
    listByTopic: vi.fn(() => Promise.resolve({ data: [] })),
    upload: vi.fn(),
    remove: vi.fn(),
    viewUrl: (id) => `/api/attachments/${id}/view`,
    downloadUrl: (id) => `/api/attachments/${id}/download`,
  },
}))

const NotePanel = (await import('../components/knowledge/NotePanel')).default

const topic = {
  id: 't1', title: 'Temat testowy', type: 'NOTE', status: 'NEW', detailLevel: 'MEDIUM',
  author: 'tester', categoryId: 'cat-1', version: 1, createdAt: '2026-08-01T10:00:00Z',
}

function firePointer(type, target, props = {}) {
  const evt = new Event(type, { bubbles: true })
  Object.assign(evt, { pointerType: 'mouse', clientX: 0, clientY: 0, ...props })
  target.dispatchEvent(evt)
}

beforeEach(() => {
  localStorage.clear()
  clearNoteCache()
})

describe('NotePanel — separator "załączniki ↔ nagłówek" (nachodzenie w górę)', () => {
  it('renderuje uchwyt z aria-orientation="horizontal" i kursorem row-resize', async () => {
    render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    const handle = await screen.findByRole('separator', { name: /rozmiar obszaru notatki/i })
    expect(handle).toHaveAttribute('aria-orientation', 'horizontal')
    expect(getComputedStyle(handle).cursor).toBe('row-resize')
  })

  it('istnieje NIEZALEŻNIE od istniejącego separatora wysokości nagłówka (oba obecne jednocześnie)', async () => {
    render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await screen.findByRole('separator', { name: /rozmiar obszaru notatki/i })
    const headerHandle = screen.getByRole('separator', { name: /wysokość nagłówka wpisu/i })
    expect(headerHandle).toBeInTheDocument()
    expect(headerHandle).toHaveAttribute('aria-orientation', 'horizontal')
  })

  it('note-panel ustawia zmienną CSS --note-lower-top w inline style', async () => {
    const { container } = render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await screen.findByRole('separator', { name: /rozmiar obszaru notatki/i })
    const panel = container.querySelector('.note-panel')
    expect(panel.style.getPropertyValue('--note-lower-top')).toMatch(/^\d+(\.\d+)?px$/)
  })

  it('przeciągnięcie w górę (ujemna delta Y) zmniejsza --note-lower-top', async () => {
    const { container } = render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    const handle = await screen.findByRole('separator', { name: /rozmiar obszaru notatki/i })
    const panel = container.querySelector('.note-panel')
    const before = parseFloat(panel.style.getPropertyValue('--note-lower-top'))

    act(() => { firePointer('pointerdown', handle, { clientY: 300 }) })
    act(() => { firePointer('pointermove', window, { clientY: 250 }) }) // 50px w górę
    act(() => { firePointer('pointerup', window) })

    const after = parseFloat(panel.style.getPropertyValue('--note-lower-top'))
    expect(after).toBeLessThan(before)
  })

  it('przeciąganie w dół z pozycji domyślnej NIE powoduje zmiany (nie da się zejść poniżej domyślnej)', async () => {
    const { container } = render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    const handle = await screen.findByRole('separator', { name: /rozmiar obszaru notatki/i })
    const panel = container.querySelector('.note-panel')
    const before = parseFloat(panel.style.getPropertyValue('--note-lower-top'))

    act(() => { firePointer('pointerdown', handle, { clientY: 300 }) })
    act(() => { firePointer('pointermove', window, { clientY: 400 }) }) // próba w dół, 100px
    act(() => { firePointer('pointerup', window) })

    const after = parseFloat(panel.style.getPropertyValue('--note-lower-top'))
    expect(after).toBe(before)
  })

  it('dwuklik przywraca domyślną pozycję po przeciągnięciu w górę', async () => {
    const { container } = render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    const handle = await screen.findByRole('separator', { name: /rozmiar obszaru notatki/i })
    const panel = container.querySelector('.note-panel')
    const original = parseFloat(panel.style.getPropertyValue('--note-lower-top'))

    act(() => { firePointer('pointerdown', handle, { clientY: 300 }) })
    act(() => { firePointer('pointermove', window, { clientY: 200 }) })
    act(() => { firePointer('pointerup', window) })
    expect(parseFloat(panel.style.getPropertyValue('--note-lower-top'))).toBeLessThan(original)

    act(() => { handle.dispatchEvent(new Event('dblclick', { bubbles: true })) })
    expect(parseFloat(panel.style.getPropertyValue('--note-lower-top'))).toBeCloseTo(original, 0)
  })

  it('przeciąganie blokuje zaznaczanie tekstu (klasa resizing-row na <body>) i sprząta ją po puszczeniu', async () => {
    render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    const handle = await screen.findByRole('separator', { name: /rozmiar obszaru notatki/i })
    act(() => { firePointer('pointerdown', handle, { clientY: 300 }) })
    expect(document.body.classList.contains('resizing-row')).toBe(true)
    act(() => { firePointer('pointerup', window) })
    expect(document.body.classList.contains('resizing-row')).toBe(false)
  })

  it('NIE zapisuje położenia w localStorage — po odświeżeniu strony (świeże zamontowanie) wraca na domyślne miejsce', async () => {
    const { container, unmount } = render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    const handle = await screen.findByRole('separator', { name: /rozmiar obszaru notatki/i })
    const panel = container.querySelector('.note-panel')
    const original = parseFloat(panel.style.getPropertyValue('--note-lower-top'))

    act(() => { firePointer('pointerdown', handle, { clientY: 300 }) })
    act(() => { firePointer('pointermove', window, { clientY: 200 }) })
    act(() => { firePointer('pointerup', window) })
    expect(parseFloat(panel.style.getPropertyValue('--note-lower-top'))).toBeLessThan(original)
    expect(localStorage.length).toBe(0)

    unmount()
    const fresh = render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)
    await screen.findByRole('separator', { name: /rozmiar obszaru notatki/i })
    const freshPanel = fresh.container.querySelector('.note-panel')
    expect(parseFloat(freshPanel.style.getPropertyValue('--note-lower-top'))).toBeCloseTo(original, 0)
  })
})
