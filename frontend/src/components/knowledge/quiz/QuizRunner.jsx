import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { quizApi } from '@/api/services'
import HelpTip from '@/components/ui/HelpTip'
import { QuizDialogStyles, RichText } from './quizStyles'
import { DEFAULT_PASS_RATIO, LETTERS, LEVEL_LABEL, lengthOptions, scoreOf } from './quizLogic'

/**
 * Rozwiązywanie quizu: ustawienia -> pytania A–D z natychmiastowym wyjaśnieniem -> wynik
 * (zaliczenie od 75%) i przegląd błędów. „Nowy quiz” zawsze losuje od nowa z całej puli.
 */
export default function QuizRunner({ topic, stats, onClose, onStatsChange }) {
  const dialog = useRef(null)
  const pool = stats?.total ?? 0
  const options = lengthOptions(pool)
  const [length, setLength] = useState(options.includes(20) ? 20 : options[options.length - 1] ?? 0)
  const [phase, setPhase] = useState('setup')
  const [quiz, setQuiz] = useState(null)
  const [index, setIndex] = useState(0)
  const [answers, setAnswers] = useState([])
  const [error, setError] = useState('')
  const [confirmClear, setConfirmClear] = useState(false)

  useLayoutEffect(() => {
    const element = dialog.current
    element.showModal()
    return () => element.close()
  }, [])

  const start = useCallback(async () => {
    setPhase('loading')
    setError('')
    try {
      const { data } = await quizApi.draw(topic.id, length)
      if (!data?.questions?.length) {
        setError('Pula pytań jest pusta — najpierw wygeneruj quiz z AI.')
        setPhase('setup')
        return
      }
      setQuiz(data)
      setIndex(0)
      setAnswers([])
      setPhase('question')
    } catch (err) {
      setError(err?.message ?? 'Nie udało się wylosować pytań.')
      setPhase('setup')
    }
  }, [topic.id, length])

  async function clearPool() {
    try {
      const { data } = await quizApi.clear(topic.id)
      onStatsChange?.(data)
      onClose()
    } catch (err) {
      setError(err?.message ?? 'Nie udało się wyczyścić puli.')
    }
  }

  const question = quiz?.questions[index]
  const chosen = answers[index]
  const revealed = chosen !== undefined

  const choose = useCallback(optionIndex => {
    if (phase !== 'question' || answers[index] !== undefined) return
    setAnswers(prev => { const next = [...prev]; next[index] = optionIndex; return next })
  }, [phase, answers, index])

  const next = useCallback(() => {
    if (!revealed) return
    if (index + 1 < quiz.questions.length) setIndex(i => i + 1)
    else setPhase('result')
  }, [revealed, index, quiz])

  useEffect(() => {
    if (phase !== 'question') return
    const onKey = e => {
      if (e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLInputElement) return
      const key = e.key.toLowerCase()
      const pick = ['1', '2', '3', '4'].indexOf(key) >= 0 ? Number(key) - 1 : ['a', 'b', 'c', 'd'].indexOf(key)
      if (pick >= 0) { e.preventDefault(); choose(pick) }
      else if (e.key === 'Enter' && revealed) { e.preventDefault(); next() }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [phase, choose, next, revealed])

  const passRatio = quiz?.passRatio ?? stats?.passRatio ?? DEFAULT_PASS_RATIO
  const passPercent = Math.round(passRatio * 100)

  return createPortal(
    <dialog ref={dialog} className="quiz-dialog" aria-labelledby="quiz-run-title"
      onCancel={event => { event.preventDefault(); onClose() }}>
      <div className="quiz-body">
        <div className="quiz-dialog-head">
          <h2 id="quiz-run-title">Quiz: {topic.title}</h2>
          <HelpTip title="Jak działa quiz">
            <ul>
              <li>Każde podejście losuje nowe pytania z całej puli.</li>
              <li>Wybierz odpowiedź (klik albo klawisze 1–4), zobacz wyjaśnienie, przejdź dalej (Enter).</li>
              <li>Zaliczenie: co najmniej {passPercent}% poprawnych odpowiedzi.</li>
            </ul>
          </HelpTip>
          <button type="button" className="quiz-close" aria-label="Zamknij quiz" onClick={onClose}>✕</button>
        </div>

        {(phase === 'setup' || phase === 'loading') && (
          <Setup pool={pool} stats={stats} options={options} length={length} setLength={setLength}
            passPercent={passPercent} loading={phase === 'loading'} error={error} onStart={start}
            confirmClear={confirmClear} setConfirmClear={setConfirmClear} onClear={clearPool} />
        )}

        {phase === 'question' && question && (
          <Question question={question} index={index} total={quiz.questions.length} chosen={chosen}
            answers={answers} questions={quiz.questions} onChoose={choose} onNext={next} />
        )}

        {phase === 'result' && (
          <Result quiz={quiz} answers={answers} passRatio={passRatio} onRetry={start}
            onSetup={() => setPhase('setup')} onClose={onClose} />
        )}
      </div>
      <QuizDialogStyles />
      <RunnerStyles />
    </dialog>,
    document.body,
  )
}

function Setup({ pool, stats, options, length, setLength, passPercent, loading, error, onStart,
  confirmClear, setConfirmClear, onClear }) {
  return (
    <>
      <div className="qr-pool">
        <div className="qr-pool-total"><b>{pool}</b><span>pytań w puli</span></div>
        <div className="qr-pool-levels">
          <span className="quiz-chip" data-level="EASY">{stats?.easy ?? 0} łatwych</span>
          <span className="quiz-chip" data-level="MEDIUM">{stats?.medium ?? 0} średnich</span>
          <span className="quiz-chip" data-level="HARD">{stats?.hard ?? 0} trudnych</span>
        </div>
      </div>

      <fieldset className="qr-length" disabled={loading || pool === 0}>
        <legend>Liczba pytań</legend>
        <div className="qr-length-opts">
          {options.map(n => (
            <label key={n} className={`qr-length-opt${n === length ? ' qr-length-opt--on' : ''}`}>
              <input type="radio" name="quiz-length" value={n} checked={n === length} onChange={() => setLength(n)} />
              {n}
            </label>
          ))}
        </div>
      </fieldset>
      <p className="qr-hint">Zaliczenie od <b>{passPercent}%</b> poprawnych odpowiedzi.</p>
      {error && <p className="quiz-error" role="alert">{error}</p>}

      <footer className="quiz-footer qr-setup-footer">
        {pool > 0 && (confirmClear ? (
          <span className="qr-confirm">
            Usunąć wszystkie {pool} pytań?
            <button type="button" className="quiz-btn quiz-btn--danger" onClick={onClear}>Tak, usuń</button>
            <button type="button" className="quiz-btn quiz-btn--ghost" onClick={() => setConfirmClear(false)}>Nie</button>
          </span>
        ) : (
          <button type="button" className="quiz-btn quiz-btn--danger" onClick={() => setConfirmClear(true)}>
            Wyczyść pulę
          </button>
        ))}
        <button type="button" className="quiz-btn quiz-btn--primary" onClick={onStart} disabled={loading || pool === 0}>
          {loading ? <><span className="quiz-spin" /> Losuję…</> : 'Rozpocznij quiz'}
        </button>
      </footer>
    </>
  )
}

function Question({ question, index, total, chosen, answers, questions, onChoose, onNext }) {
  const revealed = chosen !== undefined
  const correctSoFar = questions.filter((q, i) => answers[i] === q.correctIndex).length
  const isLast = index + 1 === total
  const nextRef = useRef(null)
  useEffect(() => { if (revealed) nextRef.current?.focus() }, [revealed])

  return (
    <>
      <div className="qr-progress">
        <div className="qr-progress-meta">
          <span>Pytanie <b>{index + 1}</b> / {total}</span>
          <span className="quiz-chip" data-level={question.difficulty}>{LEVEL_LABEL[question.difficulty]}</span>
          <span className="qr-progress-score" aria-label="Poprawne odpowiedzi">✓ {correctSoFar}</span>
        </div>
        <div className="qr-progress-bar" role="progressbar" aria-valuemin={0} aria-valuemax={total} aria-valuenow={index + (revealed ? 1 : 0)}>
          <span style={{ width: `${((index + (revealed ? 1 : 0)) / total) * 100}%` }} />
        </div>
      </div>

      <h3 className="qr-question"><RichText text={question.question} /></h3>
      {question.code && <pre className="qr-code"><code>{question.code}</code></pre>}

      <div className="qr-options" role="group" aria-label="Odpowiedzi">
        {question.options.map((option, i) => {
          let state = ''
          if (revealed && i === question.correctIndex) state = ' qr-option--correct'
          else if (revealed && i === chosen) state = ' qr-option--wrong'
          else if (revealed) state = ' qr-option--dim'
          return (
            <button key={i} type="button" className={`qr-option${state}`} onClick={() => onChoose(i)}
              disabled={revealed} aria-pressed={chosen === i}>
              <span className="qr-letter">{LETTERS[i]}</span>
              <span className="qr-option-text"><RichText text={option} /></span>
              {revealed && i === question.correctIndex && <span className="qr-mark" aria-label="poprawna">✓</span>}
              {revealed && i === chosen && i !== question.correctIndex && <span className="qr-mark" aria-label="twoja odpowiedź">✕</span>}
            </button>
          )
        })}
      </div>

      {revealed && (
        <div className={`qr-feedback${chosen === question.correctIndex ? ' qr-feedback--ok' : ' qr-feedback--bad'}`} role="status">
          <strong>{chosen === question.correctIndex ? 'Dobrze!' : `Niestety — poprawna odpowiedź to ${LETTERS[question.correctIndex]}.`}</strong>
          <p><RichText text={question.explanation} /></p>
        </div>
      )}

      <footer className="quiz-footer">
        <span className="qr-keys">{revealed ? 'Enter — dalej' : 'Klawisze 1–4 lub A–D'}</span>
        <button ref={nextRef} type="button" className="quiz-btn quiz-btn--primary" onClick={onNext} disabled={!revealed}>
          {isLast ? 'Zobacz wynik' : 'Następne pytanie →'}
        </button>
      </footer>
    </>
  )
}

function Result({ quiz, answers, passRatio, onRetry, onSetup, onClose }) {
  const score = scoreOf(quiz.questions, answers, passRatio)
  const percent = Math.round(score.ratio * 100)
  const mistakes = quiz.questions.map((q, i) => ({ q, chosen: answers[i] })).filter(m => m.chosen !== m.q.correctIndex)
  const ring = 2 * Math.PI * 52

  return (
    <>
      <div className={`qr-result${score.passed ? ' qr-result--pass' : ' qr-result--fail'}`}>
        <svg className="qr-ring" width="128" height="128" viewBox="0 0 128 128" aria-hidden="true">
          <circle cx="64" cy="64" r="52" className="qr-ring-track" />
          <circle cx="64" cy="64" r="52" className="qr-ring-value"
            strokeDasharray={ring} strokeDashoffset={ring * (1 - score.ratio)} />
          <text x="64" y="70" textAnchor="middle" className="qr-ring-text">{percent}%</text>
        </svg>
        <div className="qr-result-text">
          <span className="qr-verdict">{score.passed ? 'Zaliczony' : 'Niezaliczony'}</span>
          <p><b>{score.correct}</b> z {score.total} poprawnych · próg {Math.round(passRatio * 100)}%</p>
          <div className="qr-pool-levels">
            {['EASY', 'MEDIUM', 'HARD'].filter(l => score.byLevel[l]).map(l => (
              <span key={l} className="quiz-chip" data-level={l}>
                {LEVEL_LABEL[l]} {score.byLevel[l].correct}/{score.byLevel[l].total}
              </span>
            ))}
          </div>
        </div>
      </div>

      {mistakes.length > 0 && (
        <details className="qr-mistakes">
          <summary>Przejrzyj błędy ({mistakes.length})</summary>
          <ol>
            {mistakes.map(({ q, chosen }) => (
              <li key={q.id}>
                <p className="qr-mistake-q"><RichText text={q.question} /></p>
                {q.code && <pre className="qr-code qr-code--small"><code>{q.code}</code></pre>}
                <p className="qr-mistake-yours">Twoja: {chosen === undefined ? '—' : <RichText text={q.options[chosen]} />}</p>
                <p className="qr-mistake-right">Poprawna: <RichText text={q.options[q.correctIndex]} /></p>
                <p className="qr-mistake-why"><RichText text={q.explanation} /></p>
              </li>
            ))}
          </ol>
        </details>
      )}

      <footer className="quiz-footer">
        <button type="button" className="quiz-btn quiz-btn--ghost" onClick={onClose}>Zamknij</button>
        <button type="button" className="quiz-btn" onClick={onSetup}>Zmień ustawienia</button>
        <button type="button" className="quiz-btn quiz-btn--primary" onClick={onRetry}>Nowy quiz — losuj ponownie</button>
      </footer>
    </>
  )
}

function RunnerStyles() {
  return (
    <style>{`
      .qr-pool {
        display:flex; flex-wrap:wrap; align-items:center; justify-content:space-between; gap:12px;
        padding:16px; border-radius:var(--radius-lg); background:var(--bg-2); border:1px solid var(--border-2);
      }
      .qr-pool-total { display:flex; align-items:baseline; gap:8px; }
      .qr-pool-total b { font-size:2rem; line-height:1; font-family:var(--font-display); color:var(--accent); }
      .qr-pool-total span { color:var(--text-2); font-size:.86rem; }
      .qr-pool-levels { display:flex; flex-wrap:wrap; gap:6px; }
      .qr-length { border:none; padding:0; margin:0; }
      .qr-length legend { font-size:.84rem; font-weight:700; color:var(--text-1); margin-bottom:8px; }
      .qr-length-opts { display:flex; flex-wrap:wrap; gap:8px; }
      .qr-length-opt {
        position:relative; min-width:56px; padding:8px 14px; text-align:center; cursor:pointer;
        border:1px solid var(--border-1); border-radius:var(--radius-md); background:var(--bg-1);
        font-weight:600; font-size:.9rem; transition:all var(--t-fast);
      }
      .qr-length-opt input { position:absolute; opacity:0; pointer-events:none; }
      .qr-length-opt:has(input:focus-visible) { outline:2px solid var(--accent); outline-offset:2px; }
      .qr-length-opt--on { border-color:var(--accent); background:var(--accent-light); color:var(--accent); }
      .qr-hint { margin:0; color:var(--text-2); font-size:.84rem; }
      .qr-setup-footer { justify-content:space-between; align-items:center; }
      .qr-confirm { display:inline-flex; align-items:center; gap:4px; font-size:.84rem; color:var(--text-1); flex-wrap:wrap; }

      .qr-progress { display:flex; flex-direction:column; gap:8px; }
      .qr-progress-meta { display:flex; align-items:center; gap:10px; font-size:.84rem; color:var(--text-2); }
      .qr-progress-score { margin-left:auto; font-weight:700; color:var(--green); }
      .qr-progress-bar { height:5px; border-radius:99px; background:var(--bg-3); overflow:hidden; }
      .qr-progress-bar span { display:block; height:100%; background:linear-gradient(90deg, var(--accent), var(--cyan)); transition:width var(--t-mid); }

      .qr-question { margin:4px 0 0; font-size:1.05rem; line-height:1.5; font-weight:650; color:var(--text-0); overflow-wrap:anywhere; }
      .qr-code {
        margin:0; padding:14px 16px; border-radius:var(--radius-md); overflow:auto; max-height:320px;
        background:#0f172a; color:#e2e8f0; border:1px solid rgba(148,163,184,.18);
        font-family:var(--font-mono); font-size:.82rem; line-height:1.55; tab-size:4;
      }
      .qr-code--small { font-size:.76rem; max-height:200px; padding:10px 12px; }

      .qr-options { display:flex; flex-direction:column; gap:8px; }
      .qr-option {
        display:flex; align-items:flex-start; gap:12px; width:100%; padding:12px 14px; text-align:left;
        border:1.5px solid var(--border-1); border-radius:var(--radius-lg); background:var(--bg-1);
        color:var(--text-0); font:inherit; font-size:.92rem; line-height:1.45; cursor:pointer;
        transition:border-color var(--t-fast), background var(--t-fast), transform var(--t-fast), opacity var(--t-fast);
      }
      .qr-option:hover:not(:disabled) { border-color:var(--accent); background:var(--accent-light); transform:translateX(2px); }
      .qr-option:focus-visible { outline:2px solid var(--accent); outline-offset:2px; }
      .qr-option:disabled { cursor:default; }
      .qr-letter {
        flex:none; display:inline-flex; align-items:center; justify-content:center; width:26px; height:26px;
        border-radius:7px; background:var(--bg-3); color:var(--text-1); font-weight:700; font-size:.8rem;
      }
      .qr-option-text { flex:1; white-space:pre-wrap; overflow-wrap:anywhere; padding-top:2px; }
      .qr-mark { flex:none; font-weight:800; padding-top:2px; }
      .qr-option--correct { border-color:var(--green); background:var(--green-light); }
      .qr-option--correct .qr-letter { background:var(--green); color:#fff; }
      .qr-option--correct .qr-mark { color:var(--green); }
      .qr-option--wrong { border-color:var(--red); background:var(--red-light); }
      .qr-option--wrong .qr-letter { background:var(--red); color:#fff; }
      .qr-option--wrong .qr-mark { color:var(--red); }
      .qr-option--dim { opacity:.55; }

      .qr-feedback {
        padding:12px 14px; border-radius:var(--radius-md); border-left:3px solid; font-size:.88rem;
        animation:qr-fade 160ms ease;
      }
      .qr-feedback p { margin:4px 0 0; color:var(--text-1); line-height:1.5; }
      .qr-feedback--ok { border-color:var(--green); background:var(--green-light); }
      .qr-feedback--ok strong { color:var(--green); }
      .qr-feedback--bad { border-color:var(--red); background:var(--red-light); }
      .qr-feedback--bad strong { color:var(--red); }
      @keyframes qr-fade { from { opacity:0; transform:translateY(-3px) } to { opacity:1; transform:none } }
      .qr-keys { margin-right:auto; align-self:center; color:var(--text-3); font-size:.78rem; }

      .qr-result { display:flex; align-items:center; gap:20px; flex-wrap:wrap; padding:8px 0; }
      .qr-ring-track { fill:none; stroke:var(--bg-3); stroke-width:10; }
      .qr-ring-value {
        fill:none; stroke-width:10; stroke-linecap:round; transform:rotate(-90deg); transform-origin:64px 64px;
        transition:stroke-dashoffset 700ms ease;
      }
      .qr-result--pass .qr-ring-value { stroke:var(--green); }
      .qr-result--fail .qr-ring-value { stroke:var(--red); }
      .qr-ring-text { font-size:22px; font-weight:700; fill:var(--text-0); font-family:var(--font-mono); letter-spacing:-.02em; }
      .qr-result-text { display:flex; flex-direction:column; gap:8px; }
      .qr-result-text p { margin:0; color:var(--text-1); }
      .qr-verdict { font-size:1.5rem; font-weight:800; font-family:var(--font-display); }
      .qr-result--pass .qr-verdict { color:var(--green); }
      .qr-result--fail .qr-verdict { color:var(--red); }

      .qr-mistakes { border:1px solid var(--border-1); border-radius:var(--radius-lg); background:var(--bg-2); }
      .qr-mistakes summary { padding:12px 14px; cursor:pointer; font-weight:650; font-size:.9rem; }
      .qr-mistakes ol { margin:0; padding:0 14px 14px 34px; display:flex; flex-direction:column; gap:14px; max-height:360px; overflow:auto; }
      .qr-mistakes li { font-size:.86rem; display:flex; flex-direction:column; gap:4px; }
      .qr-mistakes li p { margin:0; overflow-wrap:anywhere; }
      .qr-mistake-q { font-weight:650; color:var(--text-0); }
      .qr-mistake-yours { color:var(--red); }
      .qr-mistake-right { color:var(--green); font-weight:600; }
      .qr-mistake-why { color:var(--text-2); }

      @media (max-width:560px) {
        .qr-result { justify-content:center; text-align:center; }
        .qr-result .qr-pool-levels { justify-content:center; }
        .qr-keys { display:none; }
      }
      @media (prefers-reduced-motion: reduce) {
        .qr-option, .qr-feedback, .qr-ring-value, .qr-progress-bar span { transition:none; animation:none; }
      }
    `}</style>
  )
}
