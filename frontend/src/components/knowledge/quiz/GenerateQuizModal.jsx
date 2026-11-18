import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { quizApi } from '@/api/services'
import HelpTip from '@/components/ui/HelpTip'
import { QuizDialogStyles } from './quizStyles'
import { errorMessage } from './quizLogic'

const STEPS = [
  'Czytam notatkę…',
  'Układam pytania łatwe…',
  'Układam pytania średnie…',
  'Układam pytania trudne…',
  'Sprawdzam jednoznaczność odpowiedzi…',
  'Usuwam duplikaty i tasuję odpowiedzi…',
]

/** „Wygeneruj quiz z AI” — opcjonalna prośba użytkownika, potem 100 nowych pytań do puli. */
export default function GenerateQuizModal({ topic, poolSize, onClose, onGenerated }) {
  const dialog = useRef(null)
  const request = useRef(null)
  const [instruction, setInstruction] = useState('')
  const [loading, setLoading] = useState(false)
  const [elapsed, setElapsed] = useState(0)
  const [error, setError] = useState('')

  useLayoutEffect(() => {
    const element = dialog.current
    element.showModal()
    return () => {
      request.current?.abort()
      element.close()
    }
  }, [])

  useEffect(() => {
    if (!loading) return
    setElapsed(0)
    const timer = setInterval(() => setElapsed(s => s + 1), 1000)
    return () => clearInterval(timer)
  }, [loading])

  function cancel() {
    request.current?.abort()
    onClose()
  }

  async function submit(event) {
    event.preventDefault()
    if (request.current) return
    const controller = new AbortController()
    request.current = controller
    setLoading(true)
    setError('')
    try {
      const guidance = instruction.trim()
      const { data } = await quizApi.generate(topic.id, guidance ? { instruction: guidance } : {}, controller.signal)
      if (controller.signal.aborted) return
      onGenerated(data)
    } catch (err) {
      if (controller.signal.aborted) return
      setError(errorMessage(err))
    } finally {
      request.current = null
      if (!controller.signal.aborted) setLoading(false)
    }
  }

  const step = STEPS[Math.min(STEPS.length - 1, Math.floor(elapsed / 12))]
  const progress = Math.min(95, Math.round((elapsed / 90) * 100))

  return createPortal(
    <dialog ref={dialog} className="quiz-dialog quiz-dialog--narrow" aria-labelledby="quiz-gen-title"
      onCancel={event => { event.preventDefault(); cancel() }}>
      <form onSubmit={submit} className="quiz-form">
        <div className="quiz-dialog-head">
          <h2 id="quiz-gen-title">{poolSize > 0 ? 'Dodaj pytania z AI' : 'Wygeneruj quiz z AI'}</h2>
          <HelpTip title="Jak powstaje quiz">
            <ul>
              <li>AI czyta zapisaną notatkę i układa 100 pytań A–D.</li>
              <li>30 łatwych, 40 średnich, 30 trudnych; przy kodzie część pytań zawiera kod.</li>
              <li>Każde kolejne generowanie dorzuca 100 nowych pytań do puli.</li>
            </ul>
          </HelpTip>
        </div>
        <p className="quiz-sub">{topic.title}</p>

        {poolSize > 0 && (
          <p className="quiz-pool-note">
            W puli jest już <b>{poolSize}</b> pytań — po generowaniu będzie ich około <b>{poolSize + 100}</b>.
          </p>
        )}

        <label htmlFor="quiz-instruction">Na czym się skupić? <span className="quiz-optional">(opcjonalnie)</span></label>
        <textarea id="quiz-instruction" value={instruction} onChange={e => setInstruction(e.target.value)}
          placeholder="np. więcej pytań z analizą kodu, mniej definicji" maxLength={1000} rows={3}
          disabled={loading} autoFocus />

        {loading && (
          <div className="quiz-progress" role="status">
            <div className="quiz-progress-bar"><span style={{ width: `${progress}%` }} /></div>
            <div className="quiz-progress-row">
              <span>{step}</span>
              <span className="quiz-progress-time">{elapsed}s</span>
            </div>
            <small>Zwykle trwa to 1–2 minuty. Możesz zostawić to okno otwarte.</small>
          </div>
        )}
        {error && <p className="quiz-error" role="alert">{error}</p>}

        <footer className="quiz-footer">
          <button type="button" className="quiz-btn quiz-btn--ghost" onClick={cancel}>Anuluj</button>
          <button type="submit" className="quiz-btn quiz-btn--primary" disabled={loading}>
            {loading ? <><span className="quiz-spin" /> Generuję…</> : 'Wygeneruj 100 pytań'}
          </button>
        </footer>
      </form>
      <QuizDialogStyles />
    </dialog>,
    document.body,
  )
}
