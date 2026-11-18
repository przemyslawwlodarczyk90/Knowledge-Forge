import { useEffect, useId, useRef, useState } from 'react'

/**
 * Subtelna ikonka „?” z krótką instrukcją w dymku. Otwiera się kliknięciem (działa też na
 * dotyku), zamyka kliknięciem obok albo Escape. `children` — 2–4 krótkie punkty, nie elaborat.
 */
export default function HelpTip({ title, children, align = 'right' }) {
  const [open, setOpen] = useState(false)
  const root = useRef(null)
  const id = useId()

  useEffect(() => {
    if (!open) return
    const onDown = e => { if (!root.current?.contains(e.target)) setOpen(false) }
    const onKey = e => {
      // preventDefault: Escape zamyka tylko dymek, nie okno <dialog>, w którym siedzi.
      if (e.key === 'Escape') { e.preventDefault(); e.stopPropagation(); setOpen(false) }
    }
    document.addEventListener('pointerdown', onDown)
    document.addEventListener('keydown', onKey, true)
    return () => {
      document.removeEventListener('pointerdown', onDown)
      document.removeEventListener('keydown', onKey, true)
    }
  }, [open])

  return (
    <span className="help-tip" ref={root}>
      <button type="button" className="help-tip-btn" aria-label={`Jak to działa: ${title}`}
        aria-expanded={open} aria-controls={id} onClick={() => setOpen(o => !o)}>
        <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <circle cx="8" cy="8" r="6.6" stroke="currentColor" strokeWidth="1.3" />
          <path d="M6.3 6.2a1.8 1.8 0 1 1 2.5 1.7c-.5.2-.8.6-.8 1.1v.4" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
          <circle cx="8" cy="11.4" r=".8" fill="currentColor" />
        </svg>
      </button>
      {open && (
        <span className={`help-tip-pop help-tip-pop--${align}`} id={id} role="note">
          <strong>{title}</strong>
          {children}
        </span>
      )}
      <style>{`
        .help-tip { position:relative; display:inline-flex; vertical-align:middle; }
        .help-tip-btn {
          display:inline-flex; align-items:center; justify-content:center; width:22px; height:22px;
          padding:0; border:none; border-radius:50%; background:transparent; color:var(--text-3);
          cursor:pointer; transition:color var(--t-fast), background var(--t-fast);
        }
        .help-tip-btn:hover, .help-tip-btn[aria-expanded="true"] { color:var(--accent); background:var(--accent-light); }
        .help-tip-btn:focus-visible { outline:2px solid var(--accent); outline-offset:1px; }
        .help-tip-pop {
          position:absolute; top:calc(100% + 6px); z-index:60; width:min(260px, 80vw);
          display:flex; flex-direction:column; gap:6px; padding:12px 14px;
          border:1px solid var(--border-1); border-radius:var(--radius-lg);
          background:var(--bg-1); color:var(--text-1); box-shadow:var(--shadow-lg);
          font-size:.8rem; font-weight:400; line-height:1.45; text-align:left; white-space:normal;
          animation:help-tip-in 120ms ease;
        }
        .help-tip-pop--right { left:-8px; }
        .help-tip-pop--left { right:-8px; }
        .help-tip-pop strong { color:var(--text-0); font-size:.82rem; }
        .help-tip-pop ul { margin:0; padding-left:16px; display:flex; flex-direction:column; gap:3px; }
        .help-tip-pop p { margin:0; }
        @keyframes help-tip-in { from { opacity:0; transform:translateY(-3px) } to { opacity:1; transform:none } }
      `}</style>
    </span>
  )
}
