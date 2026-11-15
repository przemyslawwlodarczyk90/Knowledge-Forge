import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

const mockAppConfigGet = vi.fn()
vi.mock('@/api/services', () => ({
  appConfigApi: { get: (...args) => mockAppConfigGet(...args) },
}))

const mockReadEmergencyFile = vi.fn()
vi.mock('@/utils/emergencyRecovery', () => ({
  readEmergencyFile: (...args) => mockReadEmergencyFile(...args),
  revokeBlobUrls: vi.fn(),
  formatBytes: (n) => `${n} B`,
  RecoveryError: class RecoveryError extends Error {},
  MAX_KFBUNDLE_INPUT_BYTES: 600 * 1024 * 1024,
}))

const mockGenerateSingleNotePdf = vi.fn()
const mockGenerateBulkNotesPdf = vi.fn()
vi.mock('@/utils/pdfGenerator', () => ({
  generateSingleNotePdf: (...args) => mockGenerateSingleNotePdf(...args),
  generateBulkNotesPdf: (...args) => mockGenerateBulkNotesPdf(...args),
  singleNoteFilename: (title) => `${title}.pdf`,
  bulkNotesFilename: (count) => `bulk-${count}.pdf`,
}))

const EmergencyRecoveryPanel = (await import('../components/knowledge/EmergencyRecoveryPanel')).default

function renderPanel() {
  return render(<EmergencyRecoveryPanel />)
}

function makeFile(name) {
  return new File(['x'], name, { type: 'application/octet-stream' })
}

async function openPanel(user) {
  await user.click(screen.getByRole('button', { name: /wczytaj dane binarne/i }))
  await screen.findByRole('dialog', { name: 'Wczytaj dane binarne' })
}

async function selectFile(user, file) {
  const input = document.body.querySelector('.erp-input')
  await user.upload(input, file)
}

beforeEach(() => {
  vi.clearAllMocks()
  mockAppConfigGet.mockResolvedValue({ data: { visibleBinaryLoader: true } })
  window.open = vi.fn(() => ({})) // domyślnie: nowa karta się otwiera
  globalThis.URL.createObjectURL = vi.fn(() => 'blob:mock-pdf-url')
})

describe('EmergencyRecoveryPanel', () => {
  it('panel się otwiera i pokazuje informację o obsługiwanych formatach', async () => {
    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)

    expect(screen.getAllByText(/\.kfdoc/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/\.kfbundle/).length).toBeGreaterThan(0)
    expect(screen.getByText(/nie jest wysyłany na serwer/i)).toBeInTheDocument()
  })

  it('wybór pojedynczego .kfdoc pokazuje tytuł i przycisk "Otwórz podgląd PDF"', async () => {
    mockReadEmergencyFile.mockResolvedValueOnce({
      kind: 'kfdoc', noteData: { title: 'Moja notatka' }, blobUrls: [],
    })
    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)

    await selectFile(user, makeFile('note.kfdoc'))

    await screen.findByText(/Moja notatka/)
    expect(screen.getByRole('button', { name: 'Otwórz podgląd PDF' })).toBeInTheDocument()
    expect(mockGenerateSingleNotePdf).not.toHaveBeenCalled()
  })

  it('kliknięcie "Otwórz podgląd PDF" generuje blob i otwiera nową kartę', async () => {
    mockReadEmergencyFile.mockResolvedValueOnce({
      kind: 'kfdoc', noteData: { title: 'Moja notatka' }, blobUrls: [],
    })
    mockGenerateSingleNotePdf.mockResolvedValueOnce({ issues: [], blob: new Blob(['pdf']) })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await selectFile(user, makeFile('note.kfdoc'))
    await screen.findByRole('button', { name: 'Otwórz podgląd PDF' })

    await user.click(screen.getByRole('button', { name: 'Otwórz podgląd PDF' }))

    await screen.findByText(/otwarty w nowej karcie/i)
    expect(mockGenerateSingleNotePdf).toHaveBeenCalledWith({ title: 'Moja notatka' }, { output: 'blob' })
    expect(window.open).toHaveBeenCalledWith('blob:mock-pdf-url', '_blank')
  })

  it('zablokowana nowa karta pokazuje przycisk pobrania jako fallback', async () => {
    window.open = vi.fn(() => null) // symulacja zablokowanego popupu
    mockReadEmergencyFile.mockResolvedValueOnce({
      kind: 'kfdoc', noteData: { title: 'Moja notatka' }, blobUrls: [],
    })
    mockGenerateSingleNotePdf.mockResolvedValueOnce({ issues: [], blob: new Blob(['pdf']) })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await selectFile(user, makeFile('note.kfdoc'))
    await screen.findByRole('button', { name: 'Otwórz podgląd PDF' })
    await user.click(screen.getByRole('button', { name: 'Otwórz podgląd PDF' }))

    await screen.findByText(/zablokowała otwarcie nowej karty/i)
    expect(screen.getByRole('link', { name: 'Pobierz PDF' })).toHaveAttribute('href', 'blob:mock-pdf-url')
  })

  it('.kfbundle z uszkodzonymi wpisami pokazuje listę problemów i wymaga potwierdzenia przed generowaniem', async () => {
    mockReadEmergencyFile.mockResolvedValueOnce({
      kind: 'kfbundle',
      notesData: [{ title: 'Dobra' }],
      issues: [{ fileName: 'documents/bad.kfdoc', title: 'Zła notatka', reason: 'Uszkodzony kontener ZIP' }],
      blobUrls: [],
    })
    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)

    await selectFile(user, makeFile('backup.kfbundle'))

    await screen.findByText(/1 poprawny/)
    expect(screen.getByText(/Zła notatka/)).toBeInTheDocument()
    expect(screen.getByText(/Uszkodzony kontener ZIP/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Wygeneruj mimo to' })).toBeInTheDocument()
    expect(mockGenerateBulkNotesPdf).not.toHaveBeenCalled()
  })

  it('"Wygeneruj mimo to" dołącza stronę raportu z pominiętymi dokumentami do PDF-u', async () => {
    mockReadEmergencyFile.mockResolvedValueOnce({
      kind: 'kfbundle',
      notesData: [{ title: 'Dobra' }],
      issues: [{ fileName: 'documents/bad.kfdoc', title: 'Zła notatka', reason: 'Uszkodzony kontener ZIP' }],
      blobUrls: [],
    })
    mockGenerateBulkNotesPdf.mockResolvedValueOnce({ issues: [], blob: new Blob(['pdf']) })

    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)
    await selectFile(user, makeFile('backup.kfbundle'))
    await screen.findByRole('button', { name: 'Wygeneruj mimo to' })

    await user.click(screen.getByRole('button', { name: 'Wygeneruj mimo to' }))

    await waitFor(() => expect(mockGenerateBulkNotesPdf).toHaveBeenCalled())
    const [notesArg] = mockGenerateBulkNotesPdf.mock.calls[0]
    expect(notesArg).toHaveLength(2) // strona raportu + 1 dobra notatka
    expect(notesArg[0].title).toMatch(/Raport importu/)
    expect(notesArg[1].title).toBe('Dobra')
  })

  it('backup bez żadnego czytelnego dokumentu nie pozwala wygenerować PDF-u', async () => {
    mockReadEmergencyFile.mockResolvedValueOnce({
      kind: 'kfbundle',
      notesData: [],
      issues: [{ fileName: 'documents/bad.kfdoc', title: 'Zła notatka', reason: 'Uszkodzony kontener ZIP' }],
      blobUrls: [],
    })
    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)

    await selectFile(user, makeFile('backup.kfbundle'))

    await screen.findByText(/nie ma z czego wygenerować/i)
    expect(screen.queryByRole('button', { name: /wygeneruj/i })).not.toBeInTheDocument()
  })

  it('błąd odczytu pliku pokazuje czytelny komunikat', async () => {
    mockReadEmergencyFile.mockRejectedValueOnce(new Error('To nie jest poprawny plik ZIP.'))
    const user = userEvent.setup()
    renderPanel()
    await openPanel(user)

    await selectFile(user, makeFile('broken.kfdoc'))

    await screen.findByText('Nie udało się odczytać pliku')
  })

  describe('widoczność sterowana ui.visible-binary-loader (GET /api/app-config)', () => {
    it('przycisk jest widoczny, gdy backend zwraca visibleBinaryLoader:true', async () => {
      mockAppConfigGet.mockResolvedValue({ data: { visibleBinaryLoader: true } })
      renderPanel()

      await waitFor(() => expect(mockAppConfigGet).toHaveBeenCalled())
      expect(screen.getByRole('button', { name: /wczytaj dane binarne/i })).toBeInTheDocument()
    })

    it('przycisk jest ukryty, gdy backend zwraca visibleBinaryLoader:false', async () => {
      mockAppConfigGet.mockResolvedValue({ data: { visibleBinaryLoader: false } })
      renderPanel()

      await waitFor(() => expect(mockAppConfigGet).toHaveBeenCalled())
      await waitFor(() => expect(screen.queryByRole('button', { name: /wczytaj dane binarne/i })).not.toBeInTheDocument())
    })

    it('przycisk zostaje widoczny (fail-open), gdy zapytanie o konfigurację się nie powiedzie', async () => {
      mockAppConfigGet.mockRejectedValue(new Error('network error'))
      renderPanel()

      await waitFor(() => expect(mockAppConfigGet).toHaveBeenCalled())
      expect(screen.getByRole('button', { name: /wczytaj dane binarne/i })).toBeInTheDocument()
    })
  })
})
