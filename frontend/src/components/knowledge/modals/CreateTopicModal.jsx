import { useState } from 'react'
import { topicsApi } from '@/api/services'
import { toast } from '@/store'
import ConfirmCodeModal from './ConfirmCodeModal'

const DIFFICULTY_OPTS = [
  { value: 'BASIC',  label: 'Podstawowy', color: '#16a34a', bg: '#f0fdf4', border: 'rgba(22,163,74,.25)' },
  { value: 'MEDIUM', label: 'Średni',     color: '#d97706', bg: '#fffbeb', border: 'rgba(217,119,6,.25)'  },
  { value: 'HARD',   label: 'Zaawansowany', color: '#dc2626', bg: '#fef2f2', border: 'rgba(220,38,38,.25)' },
]

export default function CreateTopicModal({ categoryId, categoryName, onClose, onCreated }) {
  const [title, setTitle]             = useState('')
  const [shortPrompt, setShortPrompt] = useState('')
  const [difficulty, setDifficulty]   = useState('BASIC')
  const [isCode, setIsCode]             = useState(false)
  const [showCodeConfirm, setShowCodeConfirm] = useState(false)
  const [loading, setLoading]           = useState(false)
  const [errors, setErrors]             = useState({})

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
      const res = await topicsApi.create({
        categoryId,
        title: title.trim(),
        shortPrompt: shortPrompt.trim(),
        difficulty,
        code: isCode,
      })
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
    <div className="tm-backdrop" onClick={onClose}>
      <div className="tm-box" onClick={e => e.stopPropagation()}>

        <header className="tm-head">
          <div>
            <p className="tm-eyebrow">Nowy temat</p>
            <h2 className="tm-title">w "{categoryName}"</h2>
          </div>
          <button className="tm-close" onClick={onClose}>✕</button>
        </header>

        <form onSubmit={handleSubmit} className="tm-body">

          {/* Tytuł */}
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

          {/* Kontekst AI */}
          <div className="mf-field">
            <label className="mf-label">Kontekst dla AI *</label>
            <textarea
              className={`mf-input mf-textarea${errors.shortPrompt ? ' mf-input--err' : ''}`}
              value={shortPrompt}
              onChange={e => { setShortPrompt(e.target.value); setErrors(p => ({...p, shortPrompt: ''})) }}
              placeholder="Co chcesz wiedzieć o tym temacie? Np. wyjaśnij z przykładami kodu, skupiony na praktycznym użyciu..."
              rows={3}
            />
            {errors.shortPrompt && <span className="mf-error">{errors.shortPrompt}</span>}
          </div>

          {/* Poziom trudności */}
          <div className="mf-field">
            <label className="mf-label">Poziom trudności</label>
            <div className="diff-group">
              {DIFFICULTY_OPTS.map(opt => (
                <button
                  key={opt.value}
                  type="button"
                  className={`diff-btn${difficulty === opt.value ? ' diff-btn--on' : ''}`}
                  style={difficulty === opt.value ? { background: opt.bg, borderColor: opt.border, color: opt.color } : {}}
                  onClick={() => setDifficulty(opt.value)}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* Czy to KOD — nieodwracalny boolean */}
          <div className="mf-field">
            <label className="mf-label">Typ notatki</label>
            <label className="code-toggle" onClick={e => { if (!isCode) { e.preventDefault(); setShowCodeConfirm(true) } }}>
              <input
                type="checkbox"
                checked={isCode}
                onChange={e => { if (!e.target.checked) setIsCode(false) }}
                className="code-toggle-input"
              />
              <span className={`code-toggle-track${isCode ? ' code-toggle-track--on' : ''}`}>
                <span className="code-toggle-thumb" />
              </span>
              <div className="code-toggle-labels">
                <span className="code-toggle-main">
                  {isCode ? (
                    <>
                      <svg width="13" height="13" viewBox="0 0 13 13" fill="none" style={{ display:'inline-block', verticalAlign:'-2px', marginRight:'5px' }}>
                        <path d="M9 2.5l2.5 4L9 11M4 2.5L1.5 6.5 4 11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                      </svg>
                      Tak — to jest KOD
                    </>
                  ) : 'Czy to KOD?'}
                </span>
                <span className="code-toggle-sub">
                  {isCode
                    ? 'Odblokuje przycisk "Wygeneruj zadania" · nieodwracalne'
                    : 'Zaznacz jeśli temat dotyczy programowania / kodu'}
                </span>
              </div>
            </label>
          </div>

          <footer className="tm-foot">
            <button type="button" className="btn-ghost" onClick={onClose}>Anuluj</button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? <span className="btn-spin" /> : 'Utwórz temat'}
            </button>
          </footer>
        </form>
      </div>

      {showCodeConfirm && (
        <ConfirmCodeModal
          onConfirm={() => setIsCode(true)}
          onClose={() => setShowCodeConfirm(false)}
        />
      )}

      <style>{`
        .tm-backdrop {
          position:fixed; inset:0; z-index:200;
          background:rgba(15,23,42,0.45); backdrop-filter:blur(3px);
          display:flex; align-items:center; justify-content:center; padding:16px;
        }
        .tm-box {
          background:#fff; border-radius:var(--radius-xl); border:1px solid var(--border-1);
          box-shadow:var(--shadow-lg); width:100%; max-width:480px;
          max-height:90vh; overflow-y:auto;
          animation:fadeIn .2s ease both;
        }
        .tm-head { display:flex; align-items:flex-start; justify-content:space-between; padding:20px 20px 0; gap:12px; }
        .tm-eyebrow { font-family:var(--font-mono); font-size:0.6rem; font-weight:700; letter-spacing:0.12em; text-transform:uppercase; color:var(--accent); margin-bottom:2px; }
        .tm-title { font-size:1rem; font-weight:700; color:var(--text-0); }
        .tm-close { background:none; border:none; color:var(--text-3); cursor:pointer; font-size:0.9rem; padding:4px 6px; border-radius:6px; flex-shrink:0; transition:background var(--t-fast); }
        .tm-close:hover { background:var(--bg-2); }
        .tm-body { padding:16px 20px 20px; display:flex; flex-direction:column; gap:14px; }
        .tm-foot { display:flex; justify-content:flex-end; gap:8px; margin-top:4px; }

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

        .diff-group { display:flex; gap:6px; flex-wrap:wrap; }
        .diff-btn {
          flex:1; min-width:80px; padding:8px 6px; text-align:center;
          border-radius:var(--radius-md); border:1.5px solid var(--border-1);
          background:#fff; font-size:0.79rem; font-weight:600; color:var(--text-2);
          cursor:pointer; transition:all var(--t-fast);
        }
        .diff-btn:hover { border-color:var(--border-0); }
        .diff-btn--on { font-weight:700; }

        /* Toggle "Czy to KOD" */
        .code-toggle {
          display:flex; align-items:flex-start; gap:12px;
          padding:12px 14px; border-radius:var(--radius-md);
          border:1.5px solid var(--border-1); background:var(--bg-2);
          cursor:pointer; transition:all var(--t-fast);
          user-select:none;
        }
        .code-toggle:hover { border-color:var(--border-0); }
        .code-toggle-input { display:none; }
        .code-toggle-track {
          width:36px; height:20px; border-radius:100px; flex-shrink:0;
          background:var(--bg-4); transition:background var(--t-mid);
          position:relative; margin-top:2px;
        }
        .code-toggle-track--on { background:var(--accent); }
        .code-toggle-thumb {
          position:absolute; top:3px; left:3px;
          width:14px; height:14px; border-radius:50%;
          background:#fff; transition:transform var(--t-mid);
          box-shadow:0 1px 3px rgba(0,0,0,0.2);
        }
        .code-toggle-track--on .code-toggle-thumb { transform:translateX(16px); }
        .code-toggle-labels { flex:1; }
        .code-toggle-main {
          display:block; font-size:0.84rem; font-weight:700;
          color:var(--text-0); margin-bottom:2px;
        }
        .code-toggle-sub { font-size:0.72rem; color:var(--text-2); line-height:1.4; }

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
