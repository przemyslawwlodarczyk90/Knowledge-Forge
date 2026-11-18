import { useState } from 'react'
import { topicsApi } from '@/api/services'
import { toast } from '@/store'
import ConfirmModal from './ConfirmModal'

const DETAIL_LEVEL_OPTS = [
  { value: 'LOW',    label: 'Niski',   color: '#64748b', bg: 'var(--bg-3)', border: 'rgba(100,116,139,.25)',
    hint: 'Szkic — temat zanotowany, do uzupełnienia później.' },
  { value: 'MEDIUM', label: 'Średni',  color: '#d97706', bg: 'var(--yellow-light)', border: 'rgba(217,119,6,.25)',
    hint: 'Notatka robocza — spisana szybko, czytelna głównie dla Ciebie.' },
  { value: 'HIGH',   label: 'Wysoki',  color: '#16a34a', bg: 'var(--green-light)', border: 'rgba(22,163,74,.25)',
    hint: 'Notatka dopracowana — napisana tak, by zrozumiał ją każdy.' },
]

const TYPE_OPTS = [
  { value: 'INSTRUCTION', label: 'Instrukcja', desc: 'Krok po kroku — jak coś zrobić' },
  { value: 'NOTE',         label: 'Notatka',    desc: 'Wiedza do zapamiętania lub wyjaśnienia' },
]

export default function CreateTopicModal({ categoryId, categoryName, onClose, onCreated }) {
  const [title, setTitle]             = useState('')
  const [shortPrompt, setShortPrompt] = useState('')
  const [detailLevel, setDetailLevel] = useState('MEDIUM')
  const [type, setType]               = useState('NOTE')
  const [loading, setLoading]         = useState(false)
  const [errors, setErrors]           = useState({})
  const [confirming, setConfirming]   = useState(false)

  const activeLevel = DETAIL_LEVEL_OPTS.find(o => o.value === detailLevel)

  const validate = () => {
    const e = {}
    if (!title.trim()) e.title = 'Tytuł jest wymagany'
    return e
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setConfirming(true)
  }

  const handleConfirmCreate = async () => {
    setLoading(true)
    try {
      const res = await topicsApi.create({
        categoryId,
        title: title.trim(),
        shortPrompt: shortPrompt.trim() || null,
        detailLevel,
        type,
      })
      toast.success(`"${res.data.title}" utworzone`)
      onCreated(res.data)
      onClose()
    } catch (err) {
      toast.error(err.message ?? 'Błąd tworzenia wpisu')
      setConfirming(false)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="tm-backdrop" onClick={onClose}>
      <div className="tm-box" onClick={e => e.stopPropagation()}>

        <header className="tm-head">
          <div>
            <p className="tm-eyebrow">Nowy wpis</p>
            <h2 className="tm-title">w "{categoryName}"</h2>
          </div>
          <button className="tm-close" onClick={onClose}>✕</button>
        </header>

        <form onSubmit={handleSubmit} className="tm-body">

          {/* Tytuł */}
          <div className="mf-field">
            <label className="mf-label">Tytuł *</label>
            <input
              className={`mf-input${errors.title ? ' mf-input--err' : ''}`}
              value={title}
              onChange={e => { setTitle(e.target.value); setErrors(p => ({...p, title: ''})) }}
              placeholder="np. Konfiguracja środowiska"
              autoFocus
            />
            {errors.title && <span className="mf-error">{errors.title}</span>}
          </div>

          {/* Opis */}
          <div className="mf-field">
            <label className="mf-label">Opis (opcjonalnie)</label>
            <textarea
              className="mf-input mf-textarea"
              value={shortPrompt}
              onChange={e => setShortPrompt(e.target.value)}
              placeholder="Krótki opis tego, co obejmuje ten wpis..."
              rows={2}
            />
          </div>

          {/* Typ — instrukcja albo notatka */}
          <div className="mf-field">
            <label className="mf-label">Typ</label>
            <div className="type-group">
              {TYPE_OPTS.map(opt => (
                <label key={opt.value} className={`type-opt${type === opt.value ? ' type-opt--on' : ''}`}>
                  <input
                    type="radio"
                    name="topic-type"
                    value={opt.value}
                    checked={type === opt.value}
                    onChange={() => setType(opt.value)}
                  />
                  <span className="type-opt-label">{opt.label}</span>
                  <span className="type-opt-desc">{opt.desc}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Poziom szczegółowości */}
          <div className="mf-field">
            <label className="mf-label">Poziom szczegółowości</label>
            <div className="diff-group">
              {DETAIL_LEVEL_OPTS.map(opt => (
                <button
                  key={opt.value}
                  type="button"
                  className={`diff-btn${detailLevel === opt.value ? ' diff-btn--on' : ''}`}
                  style={detailLevel === opt.value ? { background: opt.bg, borderColor: opt.border, color: opt.color } : {}}
                  onClick={() => setDetailLevel(opt.value)}
                >
                  {opt.label}
                </button>
              ))}
            </div>
            {activeLevel && <p className="diff-hint">{activeLevel.hint}</p>}
          </div>

          <footer className="tm-foot">
            <button type="button" className="btn-ghost" onClick={onClose}>Anuluj</button>
            <button type="submit" className="btn-primary">Utwórz</button>
          </footer>
        </form>
      </div>

      {confirming && (
        <ConfirmModal
          title="Dodać ten wpis?"
          message={`"${title.trim()}" zostanie utworzone w "${categoryName}".`}
          confirmLabel="Dodaj"
          loading={loading}
          onConfirm={handleConfirmCreate}
          onClose={() => !loading && setConfirming(false)}
        />
      )}

      <style>{`
        .tm-backdrop {
          position:fixed; inset:0; z-index:200;
          background:rgba(15,23,42,0.45); backdrop-filter:blur(3px);
          display:flex; align-items:center; justify-content:center; padding:16px;
        }
        .tm-box {
          background:var(--bg-1); border-radius:var(--radius-xl); border:1px solid var(--border-1);
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
        .mf-input:focus { border-color:var(--accent); background:var(--bg-1); box-shadow:0 0 0 3px var(--accent-glow); }
        .mf-input--err { border-color:var(--red) !important; }
        .mf-textarea { font-family:inherit; min-height:56px; }
        .mf-error { font-size:0.72rem; color:var(--red); }

        /* Typ: instrukcja / notatka */
        .type-group { display:flex; gap:8px; }
        .type-opt {
          flex:1; display:flex; flex-direction:column; gap:2px;
          padding:10px 12px; border-radius:var(--radius-md);
          border:1.5px solid var(--border-1); background:var(--bg-1);
          cursor:pointer; transition:all var(--t-fast); position:relative;
        }
        .type-opt input { position:absolute; top:10px; right:10px; margin:0; accent-color:var(--accent); }
        .type-opt:hover { border-color:var(--border-0); }
        .type-opt--on { border-color:var(--accent); background:var(--accent-light); }
        .type-opt-label { font-size:0.84rem; font-weight:700; color:var(--text-0); padding-right:20px; }
        .type-opt-desc { font-size:0.71rem; color:var(--text-2); line-height:1.4; }

        .diff-group { display:flex; gap:6px; flex-wrap:wrap; }
        .diff-btn {
          flex:1; min-width:80px; padding:8px 6px; text-align:center;
          border-radius:var(--radius-md); border:1.5px solid var(--border-1);
          background:var(--bg-1); font-size:0.79rem; font-weight:600; color:var(--text-2);
          cursor:pointer; transition:all var(--t-fast);
        }
        .diff-btn:hover { border-color:var(--border-0); }
        .diff-btn--on { font-weight:700; }
        .diff-hint { font-size:0.71rem; color:var(--text-2); line-height:1.4; margin-top:2px; }

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
