/**
 * Pokazywany, gdy część wybranych notatek nie dała się pobrać przed generowaniem zbiorczego PDF
 * (usunięta w międzyczasie, błąd sieci…) — użytkownik decyduje jawnie, NIGDY nie generujemy
 * po cichu niekompletnego dokumentu bez pytania.
 */
export default function BulkPdfIssuesModal({ failed, okCount, onCancel, onContinueWithout, loading }) {
  return (
    <div className="bpi-backdrop" onClick={() => !loading && onCancel()}>
      <div className="bpi-box" onClick={e => e.stopPropagation()}>
        <h2 className="bpi-title">Część notatek jest niedostępna</h2>
        <p className="bpi-intro">
          {okCount > 0
            ? `${okCount} notatek pobrano poprawnie, ale ${failed.length} nie udało się pobrać:`
            : `Nie udało się pobrać żadnej z ${failed.length} wybranych notatek:`}
        </p>
        <ul className="bpi-list">
          {failed.map(f => (
            <li key={f.topic.id} className="bpi-item">
              <span className="bpi-item-title">{f.topic.title}</span>
              <span className="bpi-item-reason">{f.error?.status === 404 ? 'Brak zapisanej notatki' : (f.error?.message ?? 'Błąd pobierania')}</span>
            </li>
          ))}
        </ul>
        <footer className="bpi-foot">
          <button type="button" className="bpi-btn-cancel" onClick={onCancel} disabled={loading}>Anuluj generowanie</button>
          <button
            type="button"
            className="bpi-btn-continue"
            onClick={onContinueWithout}
            disabled={loading || okCount === 0}
            title={okCount === 0 ? 'Żadna notatka nie jest dostępna' : undefined}
          >
            {loading ? <span className="bpi-spin" /> : `Generuj bez nich (${okCount})`}
          </button>
        </footer>
      </div>

      <style>{`
        .bpi-backdrop {
          position: fixed; inset: 0; z-index: 400;
          background: rgba(15,23,42,0.5); backdrop-filter: blur(2px);
          display: flex; align-items: center; justify-content: center; padding: 16px;
        }
        .bpi-box {
          background: #fff; border-radius: var(--radius-xl); border: 1px solid var(--border-1);
          box-shadow: var(--shadow-lg); width: 100%; max-width: 440px;
          padding: 22px 22px 18px; animation: fadeIn .2s ease both;
        }
        .bpi-title { font-size: 0.98rem; font-weight: 800; color: var(--text-0); margin-bottom: 6px; }
        .bpi-intro { font-size: 0.82rem; color: var(--text-2); line-height: 1.6; margin-bottom: 10px; }
        .bpi-list { list-style: none; max-height: 220px; overflow-y: auto; margin-bottom: 16px; display: flex; flex-direction: column; gap: 4px; }
        .bpi-item {
          display: flex; justify-content: space-between; gap: 10px;
          padding: 7px 10px; background: var(--red-light); border: 1px solid rgba(220,38,38,.2);
          border-radius: var(--radius-sm); font-size: 0.76rem;
        }
        .bpi-item-title { font-weight: 700; color: var(--text-0); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .bpi-item-reason { color: var(--red); flex-shrink: 0; white-space: nowrap; }
        .bpi-foot { display: flex; justify-content: flex-end; gap: 8px; }
        .bpi-btn-cancel {
          padding: 9px 16px; background: transparent; border: 1px solid var(--border-1);
          border-radius: var(--radius-md); font-size: 0.83rem; color: var(--text-2);
          cursor: pointer; transition: background .15s;
        }
        .bpi-btn-cancel:hover:not(:disabled) { background: var(--bg-2); }
        .bpi-btn-continue {
          padding: 9px 18px; background: var(--accent); color: #fff; border: none;
          border-radius: var(--radius-md); font-size: 0.83rem; font-weight: 700;
          cursor: pointer; transition: background .15s; min-width: 120px;
          display: flex; align-items: center; justify-content: center;
        }
        .bpi-btn-continue:hover:not(:disabled) { background: var(--accent-dim); }
        .bpi-btn-cancel:disabled, .bpi-btn-continue:disabled { opacity: .5; cursor: not-allowed; }
        .bpi-spin { display: inline-block; width: 13px; height: 13px; border: 2px solid rgba(255,255,255,.35); border-top-color: #fff; border-radius: 50%; animation: spin .6s linear infinite; }
      `}</style>
    </div>
  )
}
