import { useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { notesApi } from '@/api/services'

const DEFAULT_DIFFICULTY = { LOW: 'BASIC', MEDIUM: 'MEDIUM', HIGH: 'HARD' }

export default function GenerateNoteModal({ topic, replacesNote, currentNote, preservesImages, onClose, onGenerated }) {
  const dialog = useRef(null)
  const request = useRef(null)
  const refining = currentNote != null
  const [instruction, setInstruction] = useState(refining ? '' : topic.shortPrompt?.slice(0, 1000) ?? '')
  const [difficulty, setDifficulty] = useState(DEFAULT_DIFFICULTY[topic.detailLevel] ?? 'MEDIUM')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useLayoutEffect(() => {
    const element = dialog.current
    element.showModal()
    return () => {
      request.current?.abort()
      element.close()
    }
  }, [])

  function cancel() {
    request.current?.abort()
    onClose()
  }

  async function generate(event) {
    event.preventDefault()
    const guidance = instruction.trim()
    if (request.current || (refining && !guidance)) return
    const controller = new AbortController()
    request.current = controller
    setLoading(true)
    setError('')
    try {
      const payload = { instruction: guidance, difficulty }
      if (refining) payload.currentNote = currentNote
      const { data } = await notesApi.generate(topic.id, payload, controller.signal)
      if (controller.signal.aborted) return
      if (data?.contentJson?.type !== 'doc') throw new Error('Invalid generated document')
      onGenerated(data.contentJson)
    } catch (err) {
      if (controller.signal.aborted) return
      if (err.status === 503) setError(err.message)
      else if (err.status === 429) setError('Osiągnięto dzienny limit generowania notatek.')
      else setError('Nie udało się wygenerować szkicu. Spróbuj ponownie.')
    } finally {
      request.current = null
      if (!controller.signal.aborted) setLoading(false)
    }
  }

  return createPortal(
    <dialog ref={dialog} className="ai-note-dialog" aria-labelledby="ai-note-title"
      onCancel={event => { event.preventDefault(); cancel() }}>
      <form onSubmit={generate}>
        <h2 id="ai-note-title">{refining ? 'Dopracuj notatkę z AI' : 'Wygeneruj notatkę z AI'}</h2>
        <p>{topic.title}</p>
        <label htmlFor="ai-note-instruction">{refining ? 'Co zmienić w notatce?' : 'Wytyczne dla AI (opcjonalnie)'}</label>
        <textarea id="ai-note-instruction" value={instruction} onChange={event => setInstruction(event.target.value)}
          placeholder={refining ? 'Opisz własnymi słowami, co zmienić' : undefined}
          maxLength={1000} rows={4} disabled={loading} required={refining} autoFocus />
        {refining
          ? <small>OpenAI otrzyma bieżący tekst notatki i Twoje wytyczne.</small>
          : <small>Puste pole użyje opisu tematu, jeśli został dodany.</small>}
        {!refining && <>
          <label htmlFor="ai-note-difficulty">Poziom szczegółowości szkicu</label>
          <select id="ai-note-difficulty" value={difficulty} onChange={event => setDifficulty(event.target.value)}
            disabled={loading}>
            <option value="BASIC">Podstawowy</option>
            <option value="MEDIUM">Średniozaawansowany</option>
            <option value="HARD">Zaawansowany</option>
          </select>
        </>}
        {replacesNote && <p>Zapisanie szkicu zastąpi bieżącą treść notatki.</p>}
        {preservesImages && <p>Wklejone obrazy znajdą się na końcu szkicu.</p>}
        {error && <p role="alert">{error}</p>}
        {loading && <p role="status">OpenAI przygotowuje szkic…</p>}
        <footer>
          <button type="button" className="act-btn act-btn--cancel" onClick={cancel}>Anuluj</button>
          <button type="submit" className="act-btn act-btn--save" disabled={loading || (refining && !instruction.trim())}>
            {refining ? 'Dopracuj szkic' : 'Wygeneruj szkic'}
          </button>
        </footer>
      </form>
      <style>{`
        .ai-note-dialog {
          width:min(100% - 32px, 480px); max-height:calc(100% - 32px); margin:auto;
          padding:24px; border:1px solid var(--border-1); border-radius:var(--radius-xl);
          color:var(--text-0); background:#fff; box-shadow:var(--shadow-lg);
        }
        .ai-note-dialog::backdrop { background:rgba(15,23,42,.45); backdrop-filter:blur(3px); }
        .ai-note-dialog form { display:flex; flex-direction:column; gap:12px; }
        .ai-note-dialog h2 { font-size:1.1rem; }
        .ai-note-dialog p, .ai-note-dialog small { color:var(--text-2); overflow-wrap:anywhere; }
        .ai-note-dialog label { font-size:.85rem; font-weight:700; }
        .ai-note-dialog textarea, .ai-note-dialog select {
          padding:8px 10px; border:1px solid var(--border-1); border-radius:var(--radius-md);
          background:#fff; color:inherit; font:inherit;
        }
        .ai-note-dialog textarea { resize:vertical; }
        .ai-note-dialog p[role="alert"] { color:#b91c1c; }
        .ai-note-dialog footer { display:flex; flex-wrap:wrap; justify-content:flex-end; gap:8px; }
      `}</style>
    </dialog>,
    document.body,
  )
}
