import { useState } from 'react'

const CONFIRM_WORD = 'POTWIERDŹ'

export default function ConfirmCodeModal({ onConfirm, onClose }) {
  const [input, setInput] = useState('')
  const valid = input === CONFIRM_WORD

  const handleSubmit = (e) => {
    e.preventDefault()
    if (valid) { onConfirm(); onClose() }
  }

  return (
    <div className="ccm-backdrop" onClick={onClose}>
      <div className="ccm-box" onClick={e => e.stopPropagation()}>

        {/* Ikona ostrzeżenia */}
        <div className="ccm-icon-wrap">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <path d="M8 5l6 10 6-10" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
            <path d="M3 21l9-16a2.3 2.3 0 014 0l9 16a2 2 0 01-1.7 3H4.7A2 2 0 013 21z"
              stroke="white" strokeWidth="1.8" strokeLinejoin="round" fill="none"/>
            <circle cx="14" cy="20" r="1" fill="white"/>
            <path d="M14 13v4" stroke="white" strokeWidth="1.8" strokeLinecap="round"/>
          </svg>
        </div>

        <h2 className="ccm-title">Oznaczenie notatki jako KOD</h2>

        <div className="ccm-info">
          <p className="ccm-info-text">
            Oznaczenie tematu jako <strong>KOD</strong> jest <strong>nieodwracalne</strong>.
            Po zapisaniu nie będzie możliwości zmiany tej opcji.
          </p>
          <ul className="ccm-info-list">
            <li>Notatka zostanie sklasyfikowana jako materiał programistyczny</li>
            <li>Odblokujesz przycisk <strong>"Wygeneruj zadania"</strong></li>
            <li>AI będzie generować treści zorientowane na kod</li>
          </ul>
        </div>

        <form onSubmit={handleSubmit} className="ccm-form">
          <label className="ccm-field-label">
            Wpisz <code className="ccm-code">{CONFIRM_WORD}</code> aby potwierdzić:
          </label>
          <input
            className={`ccm-input${input.length > 0 && !valid ? ' ccm-input--wrong' : ''}${valid ? ' ccm-input--ok' : ''}`}
            value={input}
            onChange={e => setInput(e.target.value.toUpperCase())}
            placeholder={CONFIRM_WORD}
            autoFocus
            autoComplete="off"
            spellCheck={false}
          />
          {input.length > 0 && !valid && (
            <span className="ccm-hint">Wpisz dokładnie: {CONFIRM_WORD}</span>
          )}

          <div className="ccm-foot">
            <button type="button" className="ccm-btn-cancel" onClick={onClose}>
              Anuluj
            </button>
            <button
              type="submit"
              className="ccm-btn-confirm"
              disabled={!valid}
            >
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M2 7l4 4 6-7" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              Potwierdzam — to jest KOD
            </button>
          </div>
        </form>
      </div>

      <style>{`
        .ccm-backdrop {
          position:fixed; inset:0; z-index:300;
          background:rgba(15,23,42,0.55); backdrop-filter:blur(4px);
          display:flex; align-items:center; justify-content:center; padding:16px;
        }
        .ccm-box {
          background:#fff; border-radius:var(--radius-xl);
          border:1px solid rgba(220,38,38,.2);
          box-shadow:0 0 0 4px rgba(220,38,38,.06), var(--shadow-lg);
          width:100%; max-width:420px;
          animation:fadeIn .2s ease both;
          overflow:hidden;
        }

        .ccm-icon-wrap {
          background:linear-gradient(135deg, #dc2626, #b91c1c);
          padding:20px; display:flex; align-items:center; justify-content:center;
        }

        .ccm-title {
          font-size:1rem; font-weight:800; color:var(--text-0);
          padding:16px 20px 0; margin:0;
        }

        .ccm-info {
          padding:12px 20px 0;
        }
        .ccm-info-text {
          font-size:0.82rem; color:var(--text-1); line-height:1.6; margin-bottom:10px;
        }
        .ccm-info-text strong { color:var(--text-0); }
        .ccm-info-list {
          margin:0; padding-left:18px;
          display:flex; flex-direction:column; gap:4px;
        }
        .ccm-info-list li {
          font-size:0.78rem; color:var(--text-2); line-height:1.5;
        }
        .ccm-info-list li strong { color:var(--text-1); }

        .ccm-form {
          padding:16px 20px 20px;
          display:flex; flex-direction:column; gap:8px;
        }
        .ccm-field-label {
          font-size:0.76rem; color:var(--text-2); line-height:1.5;
        }
        .ccm-code {
          font-family:var(--font-mono); font-size:0.78rem; font-weight:700;
          color:var(--red); background:var(--red-light);
          padding:1px 5px; border-radius:4px; border:1px solid rgba(220,38,38,.15);
        }
        .ccm-input {
          width:100%; padding:10px 13px;
          border-radius:var(--radius-md);
          border:2px solid var(--border-1); background:var(--bg-2);
          font-family:var(--font-mono); font-size:0.9rem; font-weight:700;
          color:var(--text-0); outline:none; letter-spacing:0.05em;
          transition:border-color .15s, background .15s;
          text-transform:uppercase;
        }
        .ccm-input:focus { border-color:var(--accent); background:#fff; }
        .ccm-input--wrong { border-color:var(--red) !important; background:var(--red-light) !important; }
        .ccm-input--ok    { border-color:#16a34a !important; background:#f0fdf4 !important; }
        .ccm-hint { font-size:0.7rem; color:var(--red); }

        .ccm-foot {
          display:flex; gap:8px; margin-top:4px;
        }
        .ccm-btn-cancel {
          flex:1; padding:10px; background:transparent; border:1px solid var(--border-1);
          border-radius:var(--radius-md); font-size:0.83rem; color:var(--text-2);
          cursor:pointer; transition:background .15s;
        }
        .ccm-btn-cancel:hover { background:var(--bg-2); }
        .ccm-btn-confirm {
          flex:2; padding:10px 14px;
          background:linear-gradient(135deg, #dc2626, #b91c1c);
          color:#fff; border:none; border-radius:var(--radius-md);
          font-size:0.83rem; font-weight:700;
          cursor:pointer; display:flex; align-items:center; justify-content:center; gap:6px;
          transition:filter .15s, opacity .15s;
        }
        .ccm-btn-confirm:hover:not(:disabled) { filter:brightness(1.08); }
        .ccm-btn-confirm:disabled { opacity:.35; cursor:not-allowed; }
      `}</style>
    </div>
  )
}
