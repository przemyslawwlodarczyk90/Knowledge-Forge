import { Outlet } from 'react-router-dom'
import ToastContainer from '@/components/ui/ToastContainer'

export default function AuthLayout() {
  return (
    <>
      <div className="auth-shell">

        {/* Dekoracyjny gradient góra */}
        <div className="auth-top-bar" aria-hidden="true" />

        {/* Logo w nagłówku */}
        <header className="auth-header">
          <div className="auth-header-logo">
            <div className="auth-logo-icon">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                <path d="M3 4h12M3 8h8M3 12h10M3 16h6" stroke="white" strokeWidth="2" strokeLinecap="round"/>
              </svg>
            </div>
            <div className="auth-logo-text">
              <span className="auth-logo-k">Knowledge</span>
              <span className="auth-logo-f">Forge</span>
            </div>
          </div>
        </header>

        {/* Formularz */}
        <main className="auth-main">
          <Outlet />
        </main>

        {/* Stopka */}
        <footer className="auth-footer">
          © 2025 Knowledge-Forge · Platforma wiedzy
        </footer>
      </div>

      <ToastContainer />

      <style>{`
        .auth-shell {
          min-height: 100vh;
          display: flex;
          flex-direction: column;
          align-items: center;
          background: var(--bg-0);
          position: relative;
        }

        /* Górny pasek kolorowy */
        .auth-top-bar {
          position: fixed;
          top: 0; left: 0; right: 0;
          height: 3px;
          background: linear-gradient(90deg, var(--accent) 0%, var(--cyan) 100%);
          z-index: 100;
        }

        /* Nagłówek */
        .auth-header {
          width: 100%;
          max-width: 1100px;
          padding: 24px 32px 0;
        }
        .auth-header-logo {
          display: inline-flex;
          align-items: center;
          gap: 10px;
        }
        .auth-logo-icon {
          width: 36px; height: 36px;
          border-radius: 9px;
          background: linear-gradient(135deg, var(--accent) 0%, var(--cyan) 100%);
          display: flex; align-items: center; justify-content: center;
          box-shadow: 0 2px 8px rgba(37,99,235,0.3);
          flex-shrink: 0;
        }
        .auth-logo-text {
          display: flex;
          align-items: baseline;
          gap: 4px;
        }
        .auth-logo-k {
          font-family: var(--font-display);
          font-size: 1rem; font-weight: 400;
          color: var(--text-1);
          letter-spacing: 0.01em;
        }
        .auth-logo-f {
          font-family: var(--font-display);
          font-size: 1.05rem; font-weight: 800;
          color: var(--accent);
          letter-spacing: -0.01em;
        }

        /* Main area */
        .auth-main {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          width: 100%;
          padding: 32px 20px 24px;
        }
        .auth-main > * {
          width: 100%;
          max-width: 420px;
        }

        /* Footer */
        .auth-footer {
          font-size: 0.72rem;
          color: var(--text-3);
          padding: 20px;
          text-align: center;
        }
      `}</style>
    </>
  )
}
