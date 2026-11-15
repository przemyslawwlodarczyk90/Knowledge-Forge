import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

const attachmentsApi = {
  listByTopic: vi.fn(),
  upload: vi.fn(() => Promise.resolve({ data: {} })),
  remove: vi.fn(() => Promise.resolve()),
  viewUrl: (id) => `/api/attachments/${id}/view`,
  downloadUrl: (id) => `/api/attachments/${id}/download`,
}
vi.mock('@/api/services', () => ({ attachmentsApi }))

let wsHandler = null
vi.mock('@/hooks', () => ({
  useApplicationEvents: (fn) => { wsHandler = fn },
  useReconnectResync: () => {},
}))

const AttachmentsSection = (await import('@/components/knowledge/AttachmentsSection')).default

const VIDEO = { id: 1, topicId: 't1', originalName: 'film.mp4', attachmentType: 'VIDEO', sizeBytes: 5 * 1024 * 1024, description: 'nagranie', createdAt: '2030-01-01T10:00:00Z' }
const FILE = { id: 2, topicId: 't1', originalName: 'raport.xlsx', attachmentType: 'FILE', sizeBytes: 2048, createdAt: '2030-01-01T10:00:00Z' }

async function expand() {
  render(<AttachmentsSection topicId="t1" />)
  const head = document.querySelector('.att-head')
  if (head && !document.querySelector('.att-file-input')) await userEvent.click(head)
}

beforeEach(() => {
  vi.clearAllMocks()
  attachmentsApi.listByTopic.mockResolvedValue({ data: [VIDEO, FILE] })
})

describe('AttachmentsSection', () => {
  it('lists video and file attachments with view/download links', async () => {
    await expand()
    await waitFor(() => expect(screen.getAllByTitle('film.mp4').length).toBeGreaterThan(0))
    expect(document.querySelector('a[href="/api/attachments/1/view"]')).toBeTruthy()
    expect(document.querySelector('a[href="/api/attachments/2/download"]')).toBeTruthy()
    expect(attachmentsApi.listByTopic).toHaveBeenCalledWith('t1')
  })

  it('uploads the chosen file with description', async () => {
    await expand()
    const file = new File(['abc'], 'notatka.txt', { type: 'text/plain' })
    await userEvent.upload(document.querySelector('.att-file-input'), file)
    await userEvent.type(screen.getByPlaceholderText('Opis (opcjonalnie)'), ' opis ')
    await userEvent.click(document.querySelector('.att-upload-btn'))
    await waitFor(() => expect(attachmentsApi.upload).toHaveBeenCalledTimes(1))
    const fd = attachmentsApi.upload.mock.calls[0][1]
    expect(fd.get('file').name).toBe('notatka.txt')
    expect(fd.get('description')).toBe('opis')
  })

  it('deletes after confirmation and reloads on matching websocket events only', async () => {
    await expand()
    await waitFor(() => expect(screen.getAllByTitle('Usuń').length).toBe(2))
    await userEvent.click(screen.getAllByTitle('Usuń')[1])
    const dialog = await screen.findByText('Usunąć załącznik?')
    const confirmBtn = within(dialog.closest('[role="dialog"]') ?? document.body).getAllByText('Usuń').pop()
    await userEvent.click(confirmBtn)
    await waitFor(() => expect(attachmentsApi.remove).toHaveBeenCalledWith(2))

    const calls = attachmentsApi.listByTopic.mock.calls.length
    wsHandler({ type: 'ATTACHMENT_CREATED', payload: { topicId: 'other' } })
    expect(attachmentsApi.listByTopic.mock.calls.length).toBe(calls)
    wsHandler({ type: 'ATTACHMENT_DELETED', payload: { topicId: 't1' } })
    await waitFor(() => expect(attachmentsApi.listByTopic.mock.calls.length).toBe(calls + 1))
  })

  it('shows empty state and load errors', async () => {
    attachmentsApi.listByTopic.mockResolvedValueOnce({ data: [] })
    await expand()
    await waitFor(() => expect(screen.getByText('Brak załączników dla tego tematu.')).toBeInTheDocument())
  })
})
