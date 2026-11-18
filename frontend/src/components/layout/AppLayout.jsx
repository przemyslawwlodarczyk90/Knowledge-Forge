import { useState, useEffect, useRef, useCallback } from 'react'
import { Outlet, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore, useUIStore } from '@/store'
import { useResizablePanel } from '@/hooks'
import { authApi, searchApi } from '@/api/services'
import { openApplicationSocket, closeApplicationSocket } from '@/ws/socket'
import ToastContainer from '@/components/ui/ToastContainer'
import FiltersPanel from '@/components/knowledge/FiltersPanel'
import ActualityReviewPanel from '@/components/knowledge/ActualityReviewPanel'
import EmergencyRecoveryPanel from '@/components/knowledge/EmergencyRecoveryPanel'
import ResizeHandle from './ResizeHandle'

const SIDEBAR_DEFAULT = 240
const SIDEBAR_MIN = 180
const SIDEBAR_MAX = 420
const SIDEBAR_COLLAPSED = 60

/* ── Ikony SVG ── */
const IconDashboard = () => (
  <svg width="17" height="17" viewBox="0 0 17 17" fill="none">
    <rect x="1.5" y="1.5" width="6" height="6" rx="1.5" stroke="currentColor" strokeWidth="1.6"/>
    <rect x="9.5" y="1.5" width="6" height="6" rx="1.5" stroke="currentColor" strokeWidth="1.6"/>
    <rect x="1.5" y="9.5" width="6" height="6" rx="1.5" stroke="currentColor" strokeWidth="1.6"/>
    <rect x="9.5" y="9.5" width="6" height="6" rx="1.5" stroke="currentColor" strokeWidth="1.6"/>
  </svg>
)
const IconAdmin = () => (
  <svg width="17" height="17" viewBox="0 0 17 17" fill="none">
    <circle cx="8.5" cy="8.5" r="5.5" stroke="currentColor" strokeWidth="1.6"/>
    <circle cx="8.5" cy="8.5" r="2" fill="currentColor"/>
    <path d="M8.5 2v2M8.5 13v2M2 8.5h2M13 8.5h2" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
  </svg>
)
const IconLogout = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
    <path d="M6 2.5H3.5a1 1 0 00-1 1v9a1 1 0 001 1H6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
    <path d="M10.5 11L13.5 8l-3-3M13.5 8H6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const IconChevronLeft = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
    <path d="M8.5 3L5 7l3.5 4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const IconChevronRight = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
    <path d="M5.5 3L9 7l-3.5 4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const IconSearch = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
    <circle cx="6" cy="6" r="4.5" stroke="currentColor" strokeWidth="1.5"/>
    <path d="M9.5 9.5L13 13" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
)

const NAV = [{ to: '/dashboard', label: 'Drzewo wiedzy', Icon: IconDashboard }]
const ADMIN_NAV = [{ to: '/admin', label: 'Admin', Icon: IconAdmin }]
const PAGE_LABELS = { '/dashboard': 'Drzewo wiedzy', '/admin': 'Panel admina' }

const MATCH_LABEL = { TITLE: 'tytuł', DESCRIPTION: 'opis', CONTENT: 'treść' }

/* ── Klocek wyszukiwarki — przeszukuje tytuł, opis i treść wszystkich wpisów ── */
function SearchBlock() {
  const [query, setQuery]     = useState('')
  const [results, setResults] = useState(null)
  const [loading, setLoading] = useState(false)
  const [open, setOpen]       = useState(false)
  const debounceRef = useRef(null)
  const navigate = useNavigate()

  useEffect(() => {
    clearTimeout(debounceRef.current)
    const q = query.trim()
    if (!q) { setResults(null); setOpen(false); return }
    debounceRef.current = setTimeout(async () => {
      setLoading(true)
      try {
        const res = await searchApi.search(q)
        setResults(res.data)
        setOpen(true)
      } catch {
        setResults([])
        setOpen(true)
      } finally {
        setLoading(false)
      }
    }, 300)
    return () => clearTimeout(debounceRef.current)
  }, [query])

  const handlePick = useCallback((r) => {
    navigate(`/dashboard?topic=${r.topicId}`)
    setOpen(false)
    setQuery('')
    setResults(null)
  }, [navigate])

  return (
    <div className="sb-search" onBlur={() => setTimeout(() => setOpen(false), 150)}>
      <div className="sb-search-label">Wyszukiwarka</div>
      <div className="sb-search-input-wrap">
        <span className="sb-search-icon"><IconSearch /></span>
        <input
          className="sb-search-input"
          value={query}
          onChange={e => setQuery(e.target.value)}
          onFocus={() => results && setOpen(true)}
          placeholder="Szukaj w notatkach…"
        />
      </div>

      {open && (
        <div className="sb-search-results">
          {loading && <div className="sb-search-status">Szukam…</div>}
          {!loading && results?.length === 0 && <div className="sb-search-status">Brak wyników</div>}
          {!loading && results?.map(r => (
            <button key={r.topicId} className="sb-search-result" onMouseDown={() => handlePick(r)}>
              <span className="sb-sr-title">{r.title}</span>
              {r.snippet && (
                <span className="sb-sr-snippet">
                  <em>{MATCH_LABEL[r.matchedIn] ?? r.matchedIn}:</em> {r.snippet}
                </span>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

export default function AppLayout() {
  const { sidebarOpen, toggleSidebar } = useUIStore()
  const { user, logout } = useAuthStore()
  const navigate  = useNavigate()
  const isAdmin   = user?.role === 'ADMIN'
  const location  = useLocation()

  // Czyści też ciasteczko KF_TOKEN po stronie serwera (obrazki/wideo/WebSocket).
  const handleLogout = () => {
    authApi.logout().catch(() => {})
    logout()
    navigate('/login')
  }
  const pageLabel = PAGE_LABELS[location.pathname] ?? 'Knowledge Forge'

  // Jedno współdzielone połączenie WebSocket dla całej aplikacji — otwarte raz, gdy layout
  // (czyli cała appka — to jedyny stały korzeń tras) się montuje, zamykane przy odmontowaniu.
  useEffect(() => {
    openApplicationSocket()
    return () => closeApplicationSocket()
  }, [])

  const sidebarResize = useResizablePanel({
    defaultSize: SIDEBAR_DEFAULT,
    min: SIDEBAR_MIN,
    max: SIDEBAR_MAX,
  })
  const sidebarWidth = sidebarOpen ? sidebarResize.size : SIDEBAR_COLLAPSED

  return (
    <>
      <div
        className={`shell ${sidebarOpen ? 'sb-open' : 'sb-closed'}`}
        style={{ '--sb': `${sidebarWidth}px` }}
      >

        {/* ── Sidebar ── */}
        <aside className="sidebar">

          {/* Nagłówek / brand */}
          <div className="sb-head">
            <div className="sb-brand">
              <div className="sb-icon">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path d="M2 3h12M2 7h8M2 11h10M2 15h6" stroke="white" strokeWidth="2" strokeLinecap="round"/>
                </svg>
              </div>
              {sidebarOpen && (
                <div className="sb-wordmark">
                  <span className="sb-wm-a">Knowledge</span>
                  <span className="sb-wm-b">Forge</span>
                </div>
              )}
            </div>
            <button className="sb-toggle-btn" onClick={toggleSidebar} title="Zwijaj/rozwijaj">
              {sidebarOpen ? <IconChevronLeft /> : <IconChevronRight />}
            </button>
          </div>

          {/* Nawigacja */}
          <nav className="sb-nav">
            {sidebarOpen && <div className="sb-nav-label">Menu</div>}

            {NAV.map(({ to, label, Icon }) => (
              <NavLink key={to} to={to}
                className={({ isActive }) => `ni${isActive ? ' ni--on' : ''}`}>
                <span className="ni-icon"><Icon /></span>
                {sidebarOpen && <span className="ni-label">{label}</span>}
              </NavLink>
            ))}

            {sidebarOpen && <SearchBlock />}
            {sidebarOpen && <FiltersPanel />}
            {sidebarOpen && <ActualityReviewPanel />}
            {sidebarOpen && <EmergencyRecoveryPanel />}

            {isAdmin && (
              <>
                {sidebarOpen && <div className="sb-nav-label sb-nav-label--gap">Administracja</div>}
                {ADMIN_NAV.map(({ to, label, Icon }) => (
                  <NavLink key={to} to={to}
                    className={({ isActive }) => `ni ni--admin${isActive ? ' ni--on' : ''}`}>
                    <span className="ni-icon"><Icon /></span>
                    {sidebarOpen && <span className="ni-label">{label}</span>}
                  </NavLink>
                ))}
              </>
            )}
          </nav>

          {/* Użytkownik */}
          <div className="sb-user">
            <div className="sb-uavatar">{(user?.username?.[0] ?? 'U').toUpperCase()}</div>
            {sidebarOpen && (
              <>
                <div className="sb-utext">
                  <span className="sb-uname">{user?.username}</span>
                  <span className="sb-urole">{user?.role === 'ADMIN' ? 'Administrator' : 'Użytkownik'}</span>
                </div>
                <button className="sb-logout-btn" onClick={handleLogout} title="Wyloguj się">
                  <IconLogout />
                </button>
              </>
            )}
          </div>
        </aside>

        {/* Separator sidebar ↔ treść (tylko gdy sidebar jest rozwinięty) */}
        {sidebarOpen && (
          <ResizeHandle
            className="sidebar-resize-handle"
            label="Szerokość paska bocznego"
            dragging={sidebarResize.dragging}
            onPointerDown={sidebarResize.handlePointerDown}
            onDoubleClick={sidebarResize.reset}
          />
        )}

        {/* ── Prawa strona ── */}
        <div className="content-wrap">

          {/* Topbar */}
          <header className="topbar">
            <div className="topbar-left">
              <div className="topbar-accent" aria-hidden="true" />
              <h1 className="topbar-title">{pageLabel}</h1>
            </div>
            <div className="topbar-right">
              <span className="topbar-chip">
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none" style={{marginRight:4}}>
                  <path d="M1.5 2.5h9M1.5 5.5h6M1.5 8.5h7.5M1.5 11.5h4.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
                </svg>
                Knowledge Forge
              </span>
            </div>
          </header>

          {/* Treść */}
          <main className="main-content">
            <Outlet />
          </main>
        </div>
      </div>

      <ToastContainer />

      <style>{`
        /* ── Shell ── */
        .shell { display:flex; min-height:100vh; --sb:240px; background:var(--bg-0); }
        .shell.sb-closed { --sb:60px; }

        /* ── Admin nav + użytkownik (przywrócone z wersji z logowaniem) ── */
        .sb-nav-label--gap { padding-top:20px; }
        .ni--admin    { color:var(--cyan); }
        .ni--admin:hover { background:var(--cyan-light); color:var(--cyan-dim); }
        .ni--admin.ni--on { color:var(--cyan-dim) !important; background:var(--cyan-light) !important; }
        .sb-user {
          display:flex; align-items:center; gap:10px;
          padding:12px 14px; border-top:1px solid var(--border-1); flex-shrink:0;
        }
        .sb-uavatar {
          width:30px; height:30px; border-radius:8px; flex-shrink:0;
          background:var(--accent-light); border:1px solid var(--border-0);
          color:var(--accent); font-family:var(--font-mono); font-size:0.72rem; font-weight:700;
          display:flex; align-items:center; justify-content:center;
        }
        .sb-utext { flex:1; overflow:hidden; }
        .sb-uname { display:block; font-size:0.8rem; font-weight:600; color:var(--text-0); white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
        .sb-urole { display:block; font-size:0.62rem; color:var(--text-3); margin-top:1px; font-family:var(--font-mono); letter-spacing:0.04em; }
        .sb-logout-btn {
          flex-shrink:0; background:none; border:none; color:var(--text-3);
          cursor:pointer; padding:5px; border-radius:6px;
          display:flex; align-items:center;
          transition:color var(--t-fast), background var(--t-fast);
        }
        .sb-logout-btn:hover { color:var(--red); background:var(--red-light); }

        /* ── Sidebar ── */
        .sidebar {
          position:fixed; top:0; left:0; bottom:0;
          width:var(--sb);
          background:#ffffff;
          border-right:1px solid var(--border-1);
          display:flex; flex-direction:column;
          transition:width var(--t-slow);
          overflow:hidden; z-index:100;
          box-shadow:var(--shadow-sm);
        }

        /* Brand / head */
        .sb-head {
          display:flex; align-items:center;
          min-height:60px; padding:0 14px;
          border-bottom:1px solid var(--border-1);
          gap:10px; flex-shrink:0;
        }
        .sb-brand { display:flex; align-items:center; gap:10px; flex:1; overflow:hidden; }
        .sb-icon {
          width:32px; height:32px; border-radius:8px; flex-shrink:0;
          background:linear-gradient(135deg, var(--accent) 0%, var(--cyan) 100%);
          display:flex; align-items:center; justify-content:center;
          box-shadow:0 2px 6px rgba(37,99,235,0.25);
        }
        .sb-wordmark { overflow:hidden; }
        .sb-wm-a { display:block; font-size:0.67rem; font-weight:400; color:var(--text-2); line-height:1.2; white-space:nowrap; }
        .sb-wm-b { display:block; font-size:0.9rem;  font-weight:800; color:var(--text-0); line-height:1.2; white-space:nowrap; letter-spacing:-0.01em; }

        .sb-toggle-btn {
          flex-shrink:0; background:none; border:none; cursor:pointer;
          color:var(--text-3); padding:6px; border-radius:6px;
          display:flex; align-items:center; justify-content:center;
          transition:color var(--t-fast), background var(--t-fast);
        }
        .sb-toggle-btn:hover { color:var(--accent); background:var(--accent-light); }

        /* Separator sidebar ↔ treść — pozycja liczona z tej samej zmiennej --sb,
           więc podąża za szerokością sidebaru bez dodatkowej logiki w JS. */
        .sidebar-resize-handle {
          position: fixed; top: 0; bottom: 0; margin: 0;
          left: calc(var(--sb) - 4px);
        }

        /* Nav */
        .sb-nav { flex:1; padding:8px; overflow-y:auto; overflow-x:hidden; }
        .sb-nav-label {
          font-family:var(--font-mono); font-size:0.59rem; font-weight:700;
          letter-spacing:0.12em; text-transform:uppercase;
          color:var(--text-3); padding:10px 8px 4px; white-space:nowrap;
        }

        .ni {
          display:flex; align-items:center; gap:9px;
          padding:8px 10px; border-radius:var(--radius-md);
          color:var(--text-2); font-size:0.82rem; font-weight:500;
          white-space:nowrap; text-decoration:none;
          transition:color var(--t-fast), background var(--t-fast);
          cursor:pointer; background:none; border:none; width:100%;
          margin-bottom:2px;
        }
        .ni:hover     { color:var(--text-0); background:var(--bg-2); }
        .ni--on       { color:var(--accent) !important; background:var(--accent-light) !important; font-weight:600; }
        .ni--on .ni-icon { color:var(--accent); }
        .ni-icon { flex-shrink:0; width:18px; display:flex; align-items:center; justify-content:center; }
        .ni-label { overflow:hidden; text-overflow:ellipsis; }

        /* ── Klocek wyszukiwarki ── */
        .sb-search { margin-top:18px; position:relative; }
        .sb-search-label {
          font-family:var(--font-mono); font-size:0.59rem; font-weight:700;
          letter-spacing:0.12em; text-transform:uppercase;
          color:var(--text-3); padding:2px 8px 6px; white-space:nowrap;
        }
        .sb-search-input-wrap {
          display:flex; align-items:center; gap:6px;
          padding:7px 9px; margin:0 2px;
          border:1.5px solid var(--border-1); border-radius:var(--radius-md);
          background:var(--bg-2); transition:border-color var(--t-fast), background var(--t-fast);
        }
        .sb-search-input-wrap:focus-within { border-color:var(--accent); background:#fff; }
        .sb-search-icon { color:var(--text-3); flex-shrink:0; display:flex; }
        .sb-search-input {
          border:none; background:none; outline:none; flex:1; min-width:0;
          font-size:0.78rem; color:var(--text-0);
        }
        .sb-search-input::placeholder { color:var(--text-3); }

        .sb-search-results {
          position:absolute; top:100%; left:2px; right:-2px; margin-top:4px;
          background:#fff; border:1px solid var(--border-1); border-radius:var(--radius-md);
          box-shadow:var(--shadow-lg); max-height:360px; overflow-y:auto; z-index:150;
        }
        .sb-search-status { padding:12px 12px; font-size:0.76rem; color:var(--text-3); }
        .sb-search-result {
          display:flex; flex-direction:column; gap:2px; width:100%;
          padding:9px 12px; background:none; border:none; border-bottom:1px solid var(--border-2);
          text-align:left; cursor:pointer; transition:background var(--t-fast);
        }
        .sb-search-result:last-child { border-bottom:none; }
        .sb-search-result:hover { background:var(--bg-2); }
        .sb-sr-title { font-size:0.8rem; font-weight:700; color:var(--text-0); }
        .sb-sr-snippet { font-size:0.71rem; color:var(--text-2); line-height:1.4; }
        .sb-sr-snippet em { font-style:normal; color:var(--accent); font-weight:600; }

        /* ── Right side ── */
        .content-wrap {
          margin-left:var(--sb); flex:1;
          display:flex; flex-direction:column; min-height:100vh;
          transition:margin-left var(--t-slow);
        }

        /* Topbar */
        .topbar {
          height:56px; flex-shrink:0;
          display:flex; align-items:center; justify-content:space-between;
          padding:0 28px;
          background:#ffffff;
          border-bottom:1px solid var(--border-1);
          position:sticky; top:0; z-index:50;
          box-shadow:var(--shadow-xs);
        }
        .topbar-left  { display:flex; align-items:center; gap:10px; }
        .topbar-accent {
          width:3px; height:18px; border-radius:2px;
          background:linear-gradient(180deg, var(--accent), var(--cyan));
        }
        .topbar-title  { font-size:0.95rem; font-weight:700; color:var(--text-0); }
        .topbar-right  { display:flex; align-items:center; gap:12px; }
        .topbar-chip {
          display:inline-flex; align-items:center;
          font-family:var(--font-mono); font-size:0.66rem; font-weight:600;
          letter-spacing:0.06em; text-transform:uppercase;
          color:var(--text-2);
          background:var(--bg-2); border:1px solid var(--border-1);
          padding:5px 10px; border-radius:100px;
        }

        .main-content { flex:1; padding:28px 32px; }

        /* ── Responsywność ── */
        @media (max-width: 900px) {
          /* !important — na wąskim ekranie sidebar ma być zawsze zwinięty,
             niezależnie od zapamiętanej/przeciąganej szerokości (inline style --sb). */
          .shell { --sb: 60px !important; }
          .sidebar-resize-handle { display: none; }
          .topbar { padding:0 16px; }
          .topbar-chip { display:none; }
          .main-content { padding:16px; }
        }
        @media (max-width: 540px) {
          .main-content { padding:12px; }
          .topbar-title { font-size:0.85rem; }
        }
      `}</style>
    </>
  )
}
