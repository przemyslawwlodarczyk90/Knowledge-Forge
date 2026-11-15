const STAGE_LABEL = {
  fetching: 'Pobieram notatki',
  toc: 'Przygotowuję spis treści',
  rendering: 'Przygotowuję PDF',
  finalizing: 'Zapisuję plik',
}

/**
 * Blokujący overlay pokazywany podczas generowania zbiorczego PDF — pobieranie aktualnych
 * wersji wybranych notatek (z możliwością anulowania) i samo renderowanie (bez cofania —
 * w tym miejscu operacja jest już krótka i w pełni lokalna, zob. DashboardPage).
 */
export default function BulkPdfProgressModal({ progress, onCancel }) {
  if (!progress) return null
  const { stage, index = 0, total } = progress
  const label = STAGE_LABEL[stage] ?? 'Przygotowuję PDF'
  const canCancel = stage === 'fetching' && !!onCancel

  return (
    <div className="bpp-backdrop">
      <div className="bpp-box">
        <span className="bpp-spinner" />
        <p className="bpp-label">
          {label}{total ? ` ${index}/${total}` : '…'}
        </p>
        {total > 0 && (
          <div className="bpp-track">
            <div className="bpp-bar" style={{ width: `${Math.min(100, (index / total) * 100)}%` }} />
          </div>
        )}
        {canCancel && (
          <button type="button" className="bpp-cancel" onClick={onCancel}>Anuluj</button>
        )}
      </div>

      <style>{`
        .bpp-backdrop {
          position: fixed; inset: 0; z-index: 400;
          background: rgba(15,23,42,0.5); backdrop-filter: blur(2px);
          display: flex; align-items: center; justify-content: center; padding: 16px;
        }
        .bpp-box {
          background: #fff; border-radius: var(--radius-xl); border: 1px solid var(--border-1);
          box-shadow: var(--shadow-lg); width: 100%; max-width: 340px;
          padding: 26px 24px; text-align: center;
        }
        .bpp-spinner {
          display: inline-block; width: 26px; height: 26px; border-radius: 50%;
          border: 3px solid var(--border-1); border-top-color: var(--accent);
          animation: spin .7s linear infinite; margin-bottom: 14px;
        }
        .bpp-label { font-size: 0.88rem; font-weight: 700; color: var(--text-0); margin-bottom: 12px; }
        .bpp-track { height: 6px; border-radius: 100px; background: var(--bg-2); overflow: hidden; margin-bottom: 14px; }
        .bpp-bar { height: 100%; background: var(--accent); transition: width .25s ease; }
        .bpp-cancel {
          padding: 7px 16px; background: transparent; border: 1px solid var(--border-1);
          border-radius: var(--radius-md); font-size: 0.8rem; color: var(--text-2); cursor: pointer;
          transition: background .15s;
        }
        .bpp-cancel:hover { background: var(--bg-2); }
      `}</style>
    </div>
  )
}
