import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, within, act, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

const quiz = vi.hoisted(() => ({ stats: vi.fn(), draw: vi.fn(), generate: vi.fn(), clear: vi.fn() }))
const toasts = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }))

vi.mock('@/api/services', () => ({ quizApi: quiz }))
vi.mock('@/store', () => ({ toast: toasts }))

const { default: QuizRunner } = await import('../components/knowledge/quiz/QuizRunner')
const { default: GenerateQuizModal } = await import('../components/knowledge/quiz/GenerateQuizModal')
const { lengthOptions, scoreOf, errorMessage } = await import('../components/knowledge/quiz/quizLogic')
const { default: QuizBar } = await import('../components/knowledge/quiz/QuizBar')
const { default: HelpTip } = await import('../components/ui/HelpTip')
const { RichText } = await import('../components/knowledge/quiz/quizStyles')

HTMLDialogElement.prototype.showModal ??= function () { this.open = true }
HTMLDialogElement.prototype.close ??= function () { this.open = false }

const topic = { id: 't1', title: 'Rekurencja' }
const stats = (total, extra = {}) => ({ total, easy: Math.round(total * 0.3), medium: Math.round(total * 0.4),
  hard: total - Math.round(total * 0.3) - Math.round(total * 0.4), maxPool: 1000, questionsPerGeneration: 100,
  passRatio: 0.75, ...extra })

function question(i, difficulty = 'MEDIUM', extra = {}) {
  return { id: `q${i}`, difficulty, question: `Pytanie ${i} o \`fib(n)\``, code: null,
    options: [`zła ${i}`, `dobra ${i}`, `zła2 ${i}`, `zła3 ${i}`], correctIndex: 1,
    explanation: `Wyjaśnienie ${i}`, ...extra }
}

function drawn(n) {
  return { data: { questions: Array.from({ length: n }, (_, i) =>
    question(i, ['EASY', 'MEDIUM', 'HARD'][i % 3], i === 0 ? { code: 'int fib(int n) {\n    return n;\n}' } : {})),
  poolSize: 100, passRatio: 0.75 } }
}

beforeEach(() => {
  Object.values(quiz).forEach(fn => fn.mockReset())
  Object.values(toasts).forEach(fn => fn.mockReset())
})

describe('quiz helpers', () => {
  it('offers only lengths the pool can fill', () => {
    expect(lengthOptions(0)).toEqual([])
    expect(lengthOptions(14)).toEqual([10, 14])
    expect(lengthOptions(30)).toEqual([10, 20, 30])
    expect(lengthOptions(200)).toEqual([10, 20, 30, 50])
    expect(lengthOptions(5)).toEqual([5])
  })

  it('scores the attempt with the 75% pass threshold and per-level breakdown', () => {
    const qs = [question(1, 'EASY'), question(2, 'EASY'), question(3, 'HARD'), question(4, 'HARD')]
    expect(scoreOf(qs, [1, 1, 1, 0])).toMatchObject({ correct: 3, total: 4, passed: true,
      byLevel: { EASY: { correct: 2, total: 2 }, HARD: { correct: 1, total: 2 } } })
    expect(scoreOf(qs, [1, 1, 0, 0]).passed).toBe(false)
    expect(scoreOf([], []).passed).toBe(false)
    expect(scoreOf(qs, [1, 1, 1, 1], 1).passed).toBe(true)
  })

  it('renders backtick code inline', () => {
    const { container } = render(<p><RichText text="Użyj `map()` zamiast pętli" /></p>)
    expect(container.querySelector('code')).toHaveTextContent('map()')
    expect(container).toHaveTextContent('Użyj map() zamiast pętli')
    const { container: empty } = render(<p><RichText text="" /></p>)
    expect(empty.textContent).toBe('')
  })

  it('maps API errors to friendly messages', () => {
    expect(errorMessage({ status: 503, message: 'brak klucza' })).toBe('brak klucza')
    expect(errorMessage({ status: 429 })).toMatch(/dzienny limit/)
    expect(errorMessage({ status: 400, message: 'Najpierw zapisz' })).toBe('Najpierw zapisz')
    expect(errorMessage({ status: 409, message: 'Pula pełna' })).toBe('Pula pełna')
    expect(errorMessage({ status: 502 })).toMatch(/Spróbuj ponownie/)
  })
})

describe('HelpTip', () => {
  it('opens on click and closes on outside click or Escape', async () => {
    const user = userEvent.setup()
    render(<div><HelpTip title="Pomoc"><p>Krótka instrukcja</p></HelpTip><button>obok</button></div>)
    const trigger = screen.getByRole('button', { name: 'Jak to działa: Pomoc' })
    expect(screen.queryByRole('note')).not.toBeInTheDocument()

    await user.click(trigger)
    expect(screen.getByRole('note')).toHaveTextContent('Krótka instrukcja')
    expect(trigger).toHaveAttribute('aria-expanded', 'true')
    await user.keyboard('{Escape}')
    expect(screen.queryByRole('note')).not.toBeInTheDocument()

    await user.click(trigger)
    await user.click(screen.getByRole('button', { name: 'obok' }))
    expect(screen.queryByRole('note')).not.toBeInTheDocument()
    await user.click(trigger)
    await user.click(trigger)
    expect(screen.queryByRole('note')).not.toBeInTheDocument()
  })
})

describe('QuizRunner', () => {
  it('plays a full quiz: answers, explanations, keyboard, pass result and a fresh redraw', async () => {
    const user = userEvent.setup()
    quiz.draw.mockResolvedValueOnce(drawn(4)).mockResolvedValueOnce(drawn(4))
    const onClose = vi.fn()
    render(<QuizRunner topic={topic} stats={stats(100)} onClose={onClose} />)

    const dialog = within(screen.getByRole('dialog', { name: 'Quiz: Rekurencja' }))
    expect(dialog.getByText('100')).toBeInTheDocument()
    expect(dialog.getByText('30 łatwych')).toBeInTheDocument()
    expect(dialog.getByRole('radio', { name: '20' })).toBeChecked()
    await user.click(dialog.getByRole('radio', { name: '10' }).closest('label'))
    await user.click(dialog.getByRole('button', { name: 'Rozpocznij quiz' }))
    expect(quiz.draw).toHaveBeenCalledWith('t1', 10)

    // 1: poprawna myszką, kod widoczny jako blok
    await screen.findByRole('progressbar')
    expect(dialog.getByText(/int fib\(int n\)/)).toBeInTheDocument()
    expect(dialog.getByRole('button', { name: 'Następne pytanie →' })).toBeDisabled()
    await user.click(dialog.getByRole('button', { name: /dobra 0/ }))
    expect(dialog.getByRole('status')).toHaveTextContent('Dobrze!')
    expect(dialog.getByRole('status')).toHaveTextContent('Wyjaśnienie 0')
    // ponowny wybór po odsłonięciu nic nie zmienia
    expect(dialog.getByRole('button', { name: /zła 0/ })).toBeDisabled()
    await user.click(dialog.getByRole('button', { name: 'Następne pytanie →' }))

    // 2: błędna klawiszem "1", przejście Enterem
    await user.keyboard('1')
    expect(dialog.getByRole('status')).toHaveTextContent('poprawna odpowiedź to B')
    await user.keyboard('{Enter}')

    // 3 i 4: poprawne klawiszem "b"
    await user.keyboard('b')
    await user.keyboard('{Enter}')
    await user.keyboard('b')
    expect(dialog.getByRole('button', { name: 'Zobacz wynik' })).toBeEnabled()
    await user.click(dialog.getByRole('button', { name: 'Zobacz wynik' }))

    expect(dialog.getByText('Zaliczony')).toBeInTheDocument()
    expect(dialog.getByText('75%')).toBeInTheDocument()
    await user.click(dialog.getByText('Przejrzyj błędy (1)'))
    expect(dialog.getByText('Wyjaśnienie 1')).toBeInTheDocument()

    await user.click(dialog.getByRole('button', { name: 'Nowy quiz — losuj ponownie' }))
    expect(quiz.draw).toHaveBeenCalledTimes(2)
    await screen.findByRole('progressbar')

    await user.click(dialog.getByRole('button', { name: 'Zamknij quiz' }))
    expect(onClose).toHaveBeenCalled()
  })

  it('fails below 75% and lets the user change settings', async () => {
    const user = userEvent.setup()
    quiz.draw.mockResolvedValueOnce(drawn(2))
    render(<QuizRunner topic={topic} stats={stats(14)} onClose={() => {}} />)
    const dialog = within(screen.getByRole('dialog'))
    expect(dialog.getByRole('radio', { name: '14' })).toBeChecked()
    await user.click(dialog.getByRole('button', { name: 'Rozpocznij quiz' }))
    await screen.findByRole('progressbar')
    await user.keyboard('a')
    await user.keyboard('{Enter}')
    await user.keyboard('4')
    await user.keyboard('{Enter}')
    expect(dialog.getByText('Niezaliczony')).toBeInTheDocument()
    expect(dialog.getByText('0%')).toBeInTheDocument()
    await user.click(dialog.getByRole('button', { name: 'Zmień ustawienia' }))
    expect(dialog.getByRole('button', { name: 'Rozpocznij quiz' })).toBeInTheDocument()
  })

  it('reports an empty pool or a failed draw, and blocks start for an empty pool', async () => {
    const user = userEvent.setup()
    quiz.draw.mockResolvedValueOnce({ data: { questions: [] } }).mockRejectedValueOnce({ message: 'Błąd sieci' })
    const { unmount } = render(<QuizRunner topic={topic} stats={stats(20)} onClose={() => {}} />)
    const dialog = within(screen.getByRole('dialog'))
    await user.click(dialog.getByRole('button', { name: 'Rozpocznij quiz' }))
    expect(await dialog.findByRole('alert')).toHaveTextContent('Pula pytań jest pusta')
    await user.click(dialog.getByRole('button', { name: 'Rozpocznij quiz' }))
    expect(await dialog.findByText('Błąd sieci')).toBeInTheDocument()
    unmount()

    render(<QuizRunner topic={topic} stats={stats(0)} onClose={() => {}} />)
    expect(screen.getByRole('button', { name: 'Rozpocznij quiz' })).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Wyczyść pulę' })).not.toBeInTheDocument()
  })

  it('clears the pool after confirmation', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    const onStatsChange = vi.fn()
    quiz.clear.mockRejectedValueOnce({ message: 'Nie można' }).mockResolvedValueOnce({ data: stats(0) })
    render(<QuizRunner topic={topic} stats={stats(100)} onClose={onClose} onStatsChange={onStatsChange} />)
    const dialog = within(screen.getByRole('dialog'))

    await user.click(dialog.getByRole('button', { name: 'Wyczyść pulę' }))
    await user.click(dialog.getByRole('button', { name: 'Nie' }))
    await user.click(dialog.getByRole('button', { name: 'Wyczyść pulę' }))
    expect(dialog.getByText('Usunąć wszystkie 100 pytań?')).toBeInTheDocument()
    await user.click(dialog.getByRole('button', { name: 'Tak, usuń' }))
    expect(await dialog.findByText('Nie można')).toBeInTheDocument()
    await user.click(dialog.getByRole('button', { name: 'Tak, usuń' }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(onStatsChange).toHaveBeenCalledWith(expect.objectContaining({ total: 0 }))
  })

  it('closes on Escape (dialog cancel)', () => {
    const onClose = vi.fn()
    render(<QuizRunner topic={topic} stats={stats(10)} onClose={onClose} />)
    fireEvent(screen.getByRole('dialog'), new Event('cancel', { cancelable: true }))
    expect(onClose).toHaveBeenCalled()
  })
})

describe('GenerateQuizModal', () => {
  afterEach(() => vi.useRealTimers())

  it('sends the optional instruction, shows progress and hands back the result', async () => {
    const user = userEvent.setup()
    let resolve
    quiz.generate.mockReturnValueOnce(new Promise(r => { resolve = r }))
    const onGenerated = vi.fn()
    render(<GenerateQuizModal topic={topic} poolSize={100} onClose={() => {}} onGenerated={onGenerated} />)
    const dialog = within(screen.getByRole('dialog', { name: 'Dodaj pytania z AI' }))
    expect(dialog.getByText(/po generowaniu będzie ich około/)).toHaveTextContent('200')

    await user.type(dialog.getByLabelText(/Na czym się skupić/), '  więcej kodu  ')
    await user.click(dialog.getByRole('button', { name: 'Wygeneruj 100 pytań' }))
    expect(quiz.generate).toHaveBeenCalledWith('t1', { instruction: 'więcej kodu' }, expect.any(AbortSignal))
    expect(dialog.getByRole('status')).toHaveTextContent('Czytam notatkę…')
    expect(dialog.getByRole('button', { name: /Generuję/ })).toBeDisabled()

    await act(async () => resolve({ data: { added: 100, stats: stats(200) } }))
    expect(onGenerated).toHaveBeenCalledWith({ added: 100, stats: stats(200) })
  })

  it('advances progress text over time', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    quiz.generate.mockReturnValueOnce(new Promise(() => {}))
    render(<GenerateQuizModal topic={topic} poolSize={0} onClose={() => {}} onGenerated={() => {}} />)
    fireEvent.click(screen.getByRole('button', { name: 'Wygeneruj 100 pytań' }))
    expect(quiz.generate).toHaveBeenCalledWith('t1', {}, expect.any(AbortSignal))
    await act(async () => { vi.advanceTimersByTime(25_000) })
    expect(screen.getByRole('status')).toHaveTextContent('Układam pytania średnie…')
    expect(screen.getByText(/^2\ds$/)).toBeInTheDocument()
  })

  it('shows the backend reason on failure and aborts on cancel', async () => {
    const user = userEvent.setup()
    quiz.generate.mockRejectedValueOnce({ status: 503, message: 'AI nie jest skonfigurowane' })
    const onClose = vi.fn()
    render(<GenerateQuizModal topic={topic} poolSize={0} onClose={onClose} onGenerated={() => {}} />)
    expect(screen.getByRole('dialog', { name: 'Wygeneruj quiz z AI' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Wygeneruj 100 pytań' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('AI nie jest skonfigurowane')

    let signal
    quiz.generate.mockImplementationOnce((id, body, s) => { signal = s; return new Promise(() => {}) })
    await user.click(screen.getByRole('button', { name: 'Wygeneruj 100 pytań' }))
    await user.click(screen.getByRole('button', { name: 'Anuluj' }))
    expect(signal.aborted).toBe(true)
    expect(onClose).toHaveBeenCalled()
  })
})

describe('QuizBar', () => {
  it('shows the pool size, generates more questions and opens the quiz', async () => {
    const user = userEvent.setup()
    quiz.stats.mockResolvedValueOnce({ data: stats(0) })
    quiz.generate.mockResolvedValueOnce({ data: { added: 100, failedChunks: 0, stats: stats(100) } })
    render(<QuizBar topicId="t1" topic={topic} />)

    expect(await screen.findByLabelText('Pytań w puli quizu: 0')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Rozwiąż quiz' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Wygeneruj quiz z AI' }))
    await user.click(screen.getByRole('button', { name: 'Wygeneruj 100 pytań' }))
    expect(await screen.findByLabelText('Pytań w puli quizu: 100')).toBeInTheDocument()
    expect(toasts.success).toHaveBeenCalledWith('Dodano 100 pytań — w puli jest ich 100')
    expect(screen.getByRole('button', { name: '+100 pytań z AI' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Rozwiąż quiz' }))
    expect(screen.getByRole('dialog', { name: 'Quiz: Rekurencja' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Zamknij quiz' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Jak to działa: Quiz z AI' }))
    expect(screen.getByRole('note')).toHaveTextContent('Zaliczenie od 75%')
  })

  it('mentions partial failures and survives a stats error', async () => {
    const user = userEvent.setup()
    quiz.stats.mockRejectedValueOnce(new Error('x'))
    quiz.generate.mockResolvedValueOnce({ data: { added: 70, failedChunks: 2, stats: stats(70) } })
    render(<QuizBar topicId="t2" topic={topic} />)
    await waitFor(() => expect(quiz.stats).toHaveBeenCalledWith('t2'))
    expect(screen.getByLabelText('Pytań w puli quizu: 0')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Wygeneruj quiz z AI' }))
    await user.click(screen.getByRole('button', { name: 'Wygeneruj 100 pytań' }))
    await waitFor(() => expect(toasts.success).toHaveBeenCalledWith(expect.stringContaining('część porcji AI się nie udała')))
  })
})
