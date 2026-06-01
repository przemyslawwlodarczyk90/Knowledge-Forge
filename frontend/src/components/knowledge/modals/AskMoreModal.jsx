export default function AskMoreModal({ topicTitle, onClose }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-box" onClick={e => e.stopPropagation()}>
        <header className="modal-head">
          <div>
            <p className="modal-eyebrow">AI · Dopytaj o temat</p>
            <h2 className="modal-title">{topicTitle}</h2>
          </div>
          <button className="modal-close" onClick={onClose}>✕</button>
        </header>

        <div className="modal-body">
          <p className="modal-info">
            Zadaj dodatkowe pytanie dotyczące tej notatki. AI uzupełni lub rozszerzy treść na podstawie Twojego pytania.
          </p>

          <div className="mf-field">
            <label className="mf-label">Twoje pytanie</label>
            <textarea
              className="mf-input mf-textarea"
              placeholder="np. Wyjaśnij prościej, dodaj przykład z życia codziennego, pokaż jak to działa w Spring Boot..."
              rows={4}
              disabled
            />
          </div>

          <div className="modal-placeholder">
            <span className="modal-placeholder-icon">🔧</span>
            <span>Integracja AI w przygotowaniu</span>
          </div>
        </div>

        <footer className="modal-foot">
          <button className="btn-ghost" onClick={onClose}>Zamknij</button>
          <button className="btn-primary" disabled title="Wkrótce dostępne">
            Wyślij pytanie →
          </button>
        </footer>
      </div>

      <style>{`
        .modal-backdrop {
          position:fixed; inset:0; z-index:200;
          background:rgba(15,23,42,0.45); backdrop-filter:blur(3px);
          display:flex; align-items:center; justify-content:center; padding:16px;
        }
        .modal-box {
          background:#fff; border-radius:var(--radius-xl); border:1px solid var(--border-1);
          box-shadow:var(--shadow-lg); width:100%; max-width:460px;
          animation:fadeIn .2s ease both;
        }
        .modal-head { display:flex; align-items:flex-start; justify-content:space-between; padding:20px 20px 0; gap:12px; }
        .modal-eyebrow { font-family:var(--font-mono); font-size:0.62rem; font-weight:700; letter-spacing:0.1em; text-transform:uppercase; color:var(--accent); margin-bottom:3px; }
        .modal-title { font-size:0.95rem; font-weight:700; color:var(--text-0); }
        .modal-close { background:none; border:none; color:var(--text-3); cursor:pointer; font-size:0.9rem; padding:4px 6px; border-radius:6px; flex-shrink:0; margin-top:-2px; transition:background var(--t-fast); }
        .modal-close:hover { background:var(--bg-2); }
        .modal-body { padding:16px 20px; display:flex; flex-direction:column; gap:12px; }
        .modal-info { font-size:0.8rem; color:var(--text-2); line-height:1.6; }
        .modal-placeholder { display:flex; align-items:center; gap:8px; padding:10px 14px; background:var(--yellow-light); border:1px solid rgba(217,119,6,.2); border-radius:var(--radius-md); font-size:0.77rem; color:var(--yellow); font-weight:600; }
        .modal-placeholder-icon { font-size:1rem; }
        .modal-foot { display:flex; justify-content:flex-end; gap:8px; padding:0 20px 20px; }
        .mf-field { display:flex; flex-direction:column; gap:5px; }
        .mf-label { font-size:0.72rem; font-weight:700; color:var(--text-1); letter-spacing:0.04em; }
        .mf-input { padding:10px 12px; border-radius:var(--radius-md); border:1.5px solid var(--border-1); background:var(--bg-2); font-size:0.88rem; color:var(--text-0); outline:none; transition:border-color .15s; resize:vertical; }
        .mf-input:disabled { opacity:.5; cursor:not-allowed; }
        .mf-textarea { font-family:inherit; min-height:80px; }
        .btn-primary { padding:9px 20px; background:var(--accent); color:#fff; border:none; border-radius:var(--radius-md); font-size:0.84rem; font-weight:700; cursor:pointer; transition:background .15s; }
        .btn-primary:disabled { opacity:.4; cursor:not-allowed; }
        .btn-ghost { padding:9px 16px; background:transparent; color:var(--text-2); border:1px solid var(--border-1); border-radius:var(--radius-md); font-size:0.84rem; cursor:pointer; transition:background .15s; }
        .btn-ghost:hover { background:var(--bg-2); }
      `}</style>
    </div>
  )
}
