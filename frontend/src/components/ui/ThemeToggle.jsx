import { useTheme } from '@/theme/useTheme'

/** Przełącznik jasny/ciemny — ikona słońca albo księżyca, zależnie od tego, na co przełączy. */
export default function ThemeToggle({ className = '' }) {
  const { theme, toggle } = useTheme()
  const dark = theme === 'dark'
  const label = dark ? 'Włącz tryb jasny' : 'Włącz tryb ciemny'

  return (
    <button type="button" className={`theme-toggle ${className}`} onClick={toggle} aria-label={label} title={label}>
      {dark ? (
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <circle cx="8" cy="8" r="3.2" stroke="currentColor" strokeWidth="1.5" />
          <path d="M8 1.2v1.6M8 13.2v1.6M1.2 8h1.6M13.2 8h1.6M3.2 3.2l1.1 1.1M11.7 11.7l1.1 1.1M3.2 12.8l1.1-1.1M11.7 4.3l1.1-1.1"
            stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
        </svg>
      ) : (
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M13.5 9.6A5.8 5.8 0 0 1 6.4 2.5a5.8 5.8 0 1 0 7.1 7.1z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
        </svg>
      )}
      <style>{`
        .theme-toggle {
          display:inline-flex; align-items:center; justify-content:center; width:34px; height:34px; flex:none;
          padding:0; border:1px solid var(--border-1); border-radius:var(--radius-md);
          background:var(--bg-1); color:var(--text-2); cursor:pointer;
          transition:color var(--t-fast), background var(--t-fast), border-color var(--t-fast), transform var(--t-fast);
        }
        .theme-toggle:hover { color:var(--accent); border-color:var(--border-0); background:var(--accent-light); }
        .theme-toggle:active { transform:scale(.94); }
        .theme-toggle:focus-visible { outline:2px solid var(--accent); outline-offset:2px; }
      `}</style>
    </button>
  )
}
