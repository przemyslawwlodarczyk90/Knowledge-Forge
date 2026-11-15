/**
 * Cienki, przeciągany separator między dwoma panelami — pionowy (domyślnie, zmienia
 * szerokość) albo poziomy (`orientation="vertical"`, zmienia wysokość).
 * Logikę przeciągania/ograniczeń dostarcza hook `useResizablePanel` — ten komponent
 * jest wyłącznie "uchwytem": renderuje pasek, przekazuje pointerdown/dblclick dalej
 * i podświetla się w trakcie przeciągania. `className` pozwala nadpisać pozycjonowanie
 * (np. `position: fixed` dla separatora sidebaru) bez kopiowania logiki.
 *
 * `orientation` opisuje OŚ PRZECIĄGANIA (to samo pojęcie co w `useResizablePanel`), nie
 * kierunek linii separatora — stąd separator o orientation="horizontal" (przeciąganie
 * w poziomie, zmiana szerokości) rysuje PIONOWĄ linię i ma aria-orientation="vertical",
 * a orientation="vertical" (przeciąganie w pionie, zmiana wysokości) rysuje POZIOMĄ linię
 * i ma aria-orientation="horizontal" — zgodnie z ARIA (opisuje orientację samej linii).
 */
const CONFIG = {
  horizontal: { cursor: 'col-resize', ariaOrientation: 'vertical',   axisClass: 'resize-handle--x' },
  vertical:   { cursor: 'row-resize', ariaOrientation: 'horizontal', axisClass: 'resize-handle--y' },
}

export default function ResizeHandle({ onPointerDown, onDoubleClick, dragging, label, orientation = 'horizontal', className = '', style }) {
  const cfg = CONFIG[orientation] ?? CONFIG.horizontal
  return (
    <div
      className={`resize-handle ${cfg.axisClass}${dragging ? ' resize-handle--active' : ''}${className ? ` ${className}` : ''}`}
      style={style}
      role="separator"
      aria-orientation={cfg.ariaOrientation}
      aria-label={label}
      title={label ? `${label} — dwuklik: domyślny rozmiar` : undefined}
      onPointerDown={onPointerDown}
      onDoubleClick={onDoubleClick}
    >
      <style>{`
        .resize-handle {
          flex-shrink: 0;
          position: relative; z-index: 170;
          touch-action: none;
          -webkit-tap-highlight-color: transparent;
        }

        /* ── Oś pozioma (przeciąganie X) — pionowa linia, zmiana szerokości ── */
        .resize-handle--x { width: 9px; margin: 0 -4px; cursor: col-resize; }
        .resize-handle--x::after {
          content: '';
          position: absolute; top: 0; bottom: 0; left: 4px; width: 1px;
          background: var(--border-1);
          transition: background var(--t-fast), width var(--t-fast), left var(--t-fast), box-shadow var(--t-fast);
        }
        .resize-handle--x:hover::after { background: var(--accent); }
        .resize-handle--x.resize-handle--active::after {
          left: 2px; width: 5px; border-radius: 3px;
          background: var(--accent);
          box-shadow: 0 0 0 3px var(--accent-glow);
        }

        /* ── Oś pionowa (przeciąganie Y) — pozioma linia, zmiana wysokości ── */
        .resize-handle--y { height: 9px; margin: -4px 0; cursor: row-resize; }
        .resize-handle--y::after {
          content: '';
          position: absolute; left: 0; right: 0; top: 4px; height: 1px;
          background: var(--border-1);
          transition: background var(--t-fast), height var(--t-fast), top var(--t-fast), box-shadow var(--t-fast);
        }
        .resize-handle--y:hover::after { background: var(--accent); }
        .resize-handle--y.resize-handle--active::after {
          top: 2px; height: 5px; border-radius: 3px;
          background: var(--accent);
          box-shadow: 0 0 0 3px var(--accent-glow);
        }
      `}</style>
    </div>
  )
}
