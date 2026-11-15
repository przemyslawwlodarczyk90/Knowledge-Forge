import { createPortal } from 'react-dom'

// Portalowany do document.body — dokładnie z tego samego powodu co SlideOverPanel (zob. komentarz
// tam): wywoływany m.in. z ActualityReviewPanel, zagnieżdżonego w <aside className="sidebar">
// (własny stacking context, z-index:100), więc bez portalu jego z-index poniżej nie chroniłby go
// przed elementami spoza sidebara o wyższym z-index w ROOT stacking context (np. .resize-handle,
// z-index:170). z-index:1100 — jeden poziom nad pełnoekranowym panelem (1000, zob. SlideOverPanel),
// żeby modal potwierdzenia zawsze był nad nim, niezależnie skąd jest wywołany.
export default function ConfirmModal({ title, message, confirmLabel = 'Potwierdź', cancelLabel = 'Anuluj', loading, onConfirm, onClose }) {
  return createPortal(
    <div className="cfm-backdrop" onClick={() => !loading && onClose()}>
      <div className="cfm-box" onClick={e => e.stopPropagation()}>
        <h2 className="cfm-title">{title}</h2>
        {message && <p className="cfm-message">{message}</p>}
        <footer className="cfm-foot">
          <button type="button" className="cfm-btn-cancel" onClick={onClose} disabled={loading}>
            {cancelLabel}
          </button>
          <button type="button" className="cfm-btn-confirm" onClick={onConfirm} disabled={loading}>
            {loading ? <span className="cfm-spin" /> : confirmLabel}
          </button>
        </footer>
      </div>

      <style>{`
        .cfm-backdrop {
          position:fixed; inset:0; z-index:1100;
          background:rgba(15,23,42,0.45); backdrop-filter:blur(3px);
          display:flex; align-items:center; justify-content:center; padding:16px;
        }
        .cfm-box {
          background:#fff; border-radius:var(--radius-xl); border:1px solid var(--border-1);
          box-shadow:var(--shadow-lg); width:100%; max-width:380px;
          padding:22px 22px 18px; animation:fadeIn .2s ease both;
        }
        .cfm-title { font-size:0.98rem; font-weight:800; color:var(--text-0); margin-bottom:6px; }
        .cfm-message { font-size:0.82rem; color:var(--text-2); line-height:1.6; margin-bottom:16px; }
        .cfm-foot { display:flex; justify-content:flex-end; gap:8px; }
        .cfm-btn-cancel {
          padding:9px 16px; background:transparent; border:1px solid var(--border-1);
          border-radius:var(--radius-md); font-size:0.83rem; color:var(--text-2);
          cursor:pointer; transition:background .15s;
        }
        .cfm-btn-cancel:hover:not(:disabled) { background:var(--bg-2); }
        .cfm-btn-confirm {
          padding:9px 18px; background:var(--accent); color:#fff; border:none;
          border-radius:var(--radius-md); font-size:0.83rem; font-weight:700;
          cursor:pointer; transition:background .15s; min-width:76px;
          display:flex; align-items:center; justify-content:center;
        }
        .cfm-btn-confirm:hover:not(:disabled) { background:var(--accent-dim); }
        .cfm-btn-cancel:disabled, .cfm-btn-confirm:disabled { opacity:.5; cursor:not-allowed; }
        .cfm-spin { display:inline-block; width:13px; height:13px; border:2px solid rgba(255,255,255,.35); border-top-color:#fff; border-radius:50%; animation:spin .6s linear infinite; }
      `}</style>
    </div>,
    document.body,
  )
}
