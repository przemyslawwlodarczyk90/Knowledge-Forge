import { useState } from 'react'
import { topicsApi } from '@/api/services'
import { toast } from '@/store'

const DIFFICULTY_OPTS = [
  { value: 'BASIC',  label: 'Podstawowy',     color: '#16a34a' },
  { value: 'MEDIUM', label: 'Średni',          color: '#d97706' },
  { value: 'HARD',   label: 'Zaawansowany',    color: '#dc2626' },
]

export default function CreateTopicModal({ categoryId, categoryName, onClose, onCreated }) {
  const [title, setTitle]           = useState('')
  const [shortPrompt, setShortPrompt] = useState('')
  const [difficulty, setDifficulty] = useState('BASIC')
  const [loading, setLoading]       = useState(false)
  const [errors, setErrors]         = useState({})

  const validate = () => {
    const e = {}
    if (!title.trim())       e.title = 'Tytuł jest wymagany'
    if (!shortPrompt.trim()) e.shortPrompt = 'Kontekst jest wymagany'
    return e
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setLoading(true)
    try {
      const res = await topicsApi.create({ categoryId, title: title.trim(), shortPrompt: shortPrompt.trim(), difficulty })
      toast.success(`Temat "${res.data.title}" utworzony`)
      onCreated(res.data)
      onClose()
    } catch (err) {
      toast.error(err.message ?? 'Błąd tworzenia tematu')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-box modal-box--md" onClick={e => e.stopPropagation()}>
        <header className="modal-head">
          <h2 className="modal-title">Nowy temat w "{categoryName}"</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </header>

        <form onSubmit={handleSubmit} className="modal-body">
          <div className="mf-field">
            <label className="mf-label">Tytuł tematu *</label>
            <input
              className={`mf-input${errors.title ? ' mf-input--err' : ''}`}
              value={title}
              onChange={e => { setTitle(e.target.value); setErrors(p => ({...p, title: ''})) }}
              placeholder="np. Polimorfizm w Javie"
              autoFocus
            />
            {errors.title && <span className="mf-error">{errors.title}</span>}
          </div>

          <div className="mf-field">
            <label className="mf-label">Kontekst dla AI *</label>
            <textarea
              className={`mf-input mf-textarea${errors.shortPrompt ? ' mf-input--err' : ''}`}
              value={shortPrompt}
              onChange={e => { setShortPrompt(e.target.value); setErrors(p => ({...p, shortPrompt: ''})) }}
              placeholder="Krótki opis co chcesz wiedzieć o tym temacie, np. wytłumacz z przykładami kodu, skupiony na praktycznym użyciu..."
              rows={3}
            />
            {errors.shortPrompt && <span className="mf-error">{errors.shortPrompt}</span>}
          </div>

          <div className="mf-field">
            <label className="mf-label">Poziom trudności</label>
            <div className="diff-group">
              {DIFFICULTY_OPTS.map(opt => (
                <label key={opt.value} className={`diff-opt${difficulty === opt.value ? ' diff-opt--on' : ''}`}
                  style={difficulty === opt.value ? { borderColor: opt.color, background: opt.color + '12', color: opt.color } : {}}>
                  <input type="radio" name="difficulty" value={opt.value}
                    checked={difficulty === opt.value}
                    onChange={() => setDifficulty(opt.value)}
                    style={{ display: 'none' }}
                  />
                  {opt.label}
                </label>
              ))}
            </div>
          </div>

          <footer className="modal-foot">
            <button type="button" className="btn-ghost" onClick={onClose}>Anuluj</button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? <span className="btn-spin" /> : 'Utwórz temat'}
            </button>
          </footer>
        </form>
      </div>

      <style>{`
        .modal-backdrop {
          position:fixed; inset:0; z-index:200;
          background:rgba(15,23,42,0.45); backdrop-filter:blur(3px);
          display:flex; align-items:center; justify-content:center; padding:16px;
        }
        .modal-box {
          background:#fff; border-radius:var(--radius-xl); border:1px solid var(--border-1);
          box-shadow:var(--shadow-lg); width:100%; max-width:420px;
          animation:fadeIn .2s ease both;
        }
        .modal-box--md { max-width:480px; }
        .modal-head { display:flex; align-items:center; justify-content:space-between; padding:18px 20px 0; }
        .modal-title { font-size:1rem; font-weight:700; color:var(--text-0); }
        .modal-close { background:none; border:none; color:var(--text-3); cursor:pointer; font-size:0.9rem; padding:4px 6px; border-radius:6px; transition:background var(--t-fast); }
        .modal-close:hover { background:var(--bg-2); }
        .modal-body { padding:16px 20px 20px; display:flex; flex-direction:column; gap:14px; }
        .modal-foot { display:flex; justify-content:flex-end; gap:8px; margin-top:4px; }
        .mf-field { display:flex; flex-direction:column; gap:5px; }
        .mf-label { font-size:0.72rem; font-weight:700; color:var(--text-1); letter-spacing:0.04em; }
        .mf-input {
          padding:10px 12px; border-radius:var(--radius-md);
          border:1.5px solid var(--border-1); background:var(--bg-2);
          font-size:0.88rem; color:var(--text-0); outline:none;
          transition:border-color .15s; resize:vertical;
        }
        .mf-input:focus { border-color:var(--accent); background:#fff; box-shadow:0 0 0 3px var(--accent-glow); }
        .mf-input--err { border-color:var(--red) !important; }
        .mf-textarea { font-family:inherit; min-height:72px; }
        .mf-error { font-size:0.72rem; color:var(--red); }
        .diff-group { display:flex; gap:8px; }
        .diff-opt {
          flex:1; text-align:center; padding:8px 4px;
          border:1.5px solid var(--border-1); border-radius:var(--radius-md);
          font-size:0.8rem; font-weight:600; color:var(--text-2);
          cursor:pointer; transition:all .15s; user-select:none;
        }
        .diff-opt:hover { border-color:var(--border-0); }
        .btn-primary { padding:9px 20px; background:var(--accent); color:#fff; border:none; border-radius:var(--radius-md); font-size:0.84rem; font-weight:700; cursor:pointer; transition:background .15s; }
        .btn-primary:hover:not(:disabled) { background:var(--accent-dim); }
        .btn-primary:disabled { opacity:.5; cursor:not-allowed; }
        .btn-ghost { padding:9px 16px; background:transparent; color:var(--text-2); border:1px solid var(--border-1); border-radius:var(--radius-md); font-size:0.84rem; cursor:pointer; transition:background .15s; }
        .btn-ghost:hover { background:var(--bg-2); }
        .btn-spin { display:inline-block; width:13px; height:13px; border:2px solid rgba(255,255,255,.3); border-top-color:#fff; border-radius:50%; animation:spin .6s linear infinite; vertical-align:middle; }
      `}</style>
    </div>
  )
}
