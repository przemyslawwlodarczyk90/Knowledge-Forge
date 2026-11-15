import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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

function makeTopic(overrides = {}) {
  return {
    id: 't1', title: 'Temat testowy', type: 'NOTE', status: 'NEW', detailLevel: 'MEDIUM',
    author: 'tester', categoryId: 'cat-1', version: 1, createdAt: '2026-08-01T10:00:00Z',
    lastVerificationOfActualityDate: null,
    ...overrides,
  }
}

beforeEach(() => {
  localStorage.clear()
  clearNoteCache()
})

describe('NotePanel — trzecia data "Zweryfikowano" (potwierdzenie aktualności)', () => {
  it('gdy nigdy nie potwierdzono, pokazuje jawnie "Nigdy — termin liczony od utworzenia"', async () => {
    const topic = makeTopic({ lastVerificationOfActualityDate: null })
    render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)

    await screen.findByText('Zweryfikowano')
    expect(screen.getByText('Nigdy — termin liczony od utworzenia')).toBeInTheDocument()
  })

  it('gdy jest data potwierdzenia, pokazuje ją sformatowaną obok "Utworzono"', async () => {
    const topic = makeTopic({ lastVerificationOfActualityDate: '2026-08-15T09:30:00Z' })
    render(<NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} />)

    await screen.findByText('Utworzono')
    expect(screen.getByText('Zweryfikowano')).toBeInTheDocument()
    // Wiersz "Zweryfikowano" jest osobny od "Utworzono" — trzecia data, nie podmiana istniejącej.
    const dateRows = document.querySelectorAll('.note-date-row')
    expect(dateRows.length).toBeGreaterThanOrEqual(2)
  })
})

describe('NotePanel — editRequest (wejście w tryb edycji z panelu "Weryfikacja aktualności")', () => {
  it('editRequest dla otwartego tematu przełącza od razu w tryb edycji (widoczny Toolbar)', async () => {
    const topic = makeTopic()
    const { rerender } = render(
      <NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()} editRequest={null} />,
    )
    await screen.findByText('Zweryfikowano')
    expect(screen.queryByRole('button', { name: 'Anuluj' })).not.toBeInTheDocument()

    rerender(
      <NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()}
        editRequest={{ topicId: 't1', nonce: 1 }} />,
    )

    await screen.findByRole('button', { name: 'Anuluj' })
  })

  it('editRequest dla innego tematu NIE przełącza w edycję', async () => {
    const topic = makeTopic()
    render(
      <NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()}
        editRequest={{ topicId: 'inny-temat', nonce: 1 }} />,
    )
    await screen.findByText('Zweryfikowano')
    expect(screen.queryByRole('button', { name: 'Anuluj' })).not.toBeInTheDocument()
  })

  it('anulowanie edycji po editRequest wraca do widoku i NIE woła zapisu (brak automatycznego potwierdzenia)', async () => {
    const { notesApi } = await import('@/api/services')
    const topic = makeTopic()
    const user = userEvent.setup()
    render(
      <NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()}
        editRequest={{ topicId: 't1', nonce: 1 }} />,
    )

    const cancelBtn = await screen.findByRole('button', { name: 'Anuluj' })
    await user.click(cancelBtn)

    expect(screen.queryByRole('button', { name: 'Anuluj' })).not.toBeInTheDocument()
    expect(notesApi.save).not.toHaveBeenCalled()
  })

  it('to samo nonce ponownie (bez zmiany) nie wymusza edycji drugi raz po ręcznym wyjściu', async () => {
    const topic = makeTopic()
    const user = userEvent.setup()
    const { rerender } = render(
      <NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()}
        editRequest={{ topicId: 't1', nonce: 5 }} />,
    )
    const cancelBtn = await screen.findByRole('button', { name: 'Anuluj' })
    await user.click(cancelBtn)
    expect(screen.queryByRole('button', { name: 'Anuluj' })).not.toBeInTheDocument()

    // Ten sam obiekt editRequest (ten sam nonce=5) przy re-renderze NIE powinien ponownie
    // wymusić trybu edycji — zostało już "skonsumowane".
    rerender(
      <NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()}
        editRequest={{ topicId: 't1', nonce: 5 }} />,
    )
    expect(screen.queryByRole('button', { name: 'Anuluj' })).not.toBeInTheDocument()
  })

  it('nowy nonce dla tego samego tematu może ponownie wymusić edycję', async () => {
    const topic = makeTopic()
    const user = userEvent.setup()
    const { rerender } = render(
      <NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()}
        editRequest={{ topicId: 't1', nonce: 1 }} />,
    )
    const cancelBtn = await screen.findByRole('button', { name: 'Anuluj' })
    await user.click(cancelBtn)

    rerender(
      <NotePanel topic={topic} categoryPath={[]} onTopicUpdated={vi.fn()} onOpenTree={vi.fn()}
        editRequest={{ topicId: 't1', nonce: 2 }} />,
    )

    await screen.findByRole('button', { name: 'Anuluj' })
  })
})
