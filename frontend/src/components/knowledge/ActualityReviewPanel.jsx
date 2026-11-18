import { useState, useCallback, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { categoriesApi, topicsApi } from '@/api/services'
import { toast } from '@/store'
import { useApplicationEvents } from '@/hooks'
import { findPath } from '@/pages/dashboardTreeUtils'
import SlideOverPanel, { EmptyState, IconAlert, IconCheckCircle } from './SlideOverPanel'
import ConfirmModal from './modals/ConfirmModal'

const TYPE_LABEL = { INSTRUCTION: 'Instrukcja', NOTE: 'Notatka' }

const IconClock = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
    <circle cx="7" cy="7" r="5.5" stroke="currentColor" strokeWidth="1.4"/>
    <path d="M7 4v3.2l2.2 1.3" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)

function formatDate(iso) {
  if (!iso) return null
  try {
    return new Date(iso).toLocaleString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return null
  }
}

/**
 * Guzik "Weryfikacja aktualności" (w menu bocznym, bezpośrednio pod "Filtry") + panel-drawer
 * z listą tematów wymagających sprawdzenia (actualityVerified=false — zob.
 * dokumentacja/ACTUALITY_VERIFICATION.txt). Układ/animacja (backdrop, panel-drawer, nagłówek,
 * Escape) to SlideOverPanel, współdzielony z FiltersPanel razem ze wspólnymi klasami CSS pól/
 * przycisków/pustych stanów (`sop-*`); logika (pobieranie, potwierdzanie, WebSocket) zostaje
 * tutaj, osobno.
 */
export default function ActualityReviewPanel() {
  const [open, setOpen] = useState(false)
  const [authorsLoaded, setAuthorsLoaded] = useState(false)
  const [authors, setAuthors] = useState([])
  const [tree, setTree] = useState([])

  const [authorFilter, setAuthorFilter] = useState('')
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)

  const [pendingConfirm, setPendingConfirm] = useState(null) // record | null
  const [confirming, setConfirming] = useState(false)

  const navigate = useNavigate()

  const ensureAuthorsLoaded = useCallback(async () => {
    if (authorsLoaded) return
    try {
      const [authorsRes, treeRes] = await Promise.all([
        topicsApi.listAuthors(),
        categoriesApi.getTree(),
      ])
      setAuthors(authorsRes.data ?? [])
      setTree(treeRes.data.children ?? [])
      setAuthorsLoaded(true)
    } catch {
      // Niekrytyczne — filtr autora i ścieżka kategorii po prostu zostaną puste; lista rekordów
      // (główna treść panelu) ładuje się niezależnie, patrz fetchList.
    }
  }, [authorsLoaded])

  const fetchList = useCallback(async (author) => {
    setLoading(true)
    setError(false)
    try {
      const res = await topicsApi.actualityReview(author || undefined)
      setRecords(res.data ?? [])
    } catch {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [])

  const handleOpen = () => {
    setOpen(true)
    ensureAuthorsLoaded()
    fetchList(authorFilter)
  }

  // Zmiana filtra autora, gdy panel jest już otwarty, od razu odświeża listę (jeden, prosty
  // filtr — bez osobnego przycisku "Zastosuj", w przeciwieństwie do wieloczęściowego FiltersPanel).
  useEffect(() => {
    if (!open) return
    fetchList(authorFilter)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authorFilter])

  // Otwarte panele reagują na ręczne potwierdzenie, zapis notatki I zbiorcze oznaczenie przez
  // scheduler — wszystkie trzy publikują to samo, lekkie zdarzenie (zob. ApplicationEventHub#
  // actualityReviewChanged), więc jeden nasłuch tutaj wystarcza na wszystkie trzy przypadki.
  useApplicationEvents(useCallback((event) => {
    if (!open) return
    if (event.type === 'ACTUALITY_REVIEW_CHANGED') {
      fetchList(authorFilter)
    }
  }, [open, authorFilter, fetchList]))

  const requestConfirm = (record) => setPendingConfirm(record)

  const doConfirm = async () => {
    if (!pendingConfirm) return
    setConfirming(true)
    try {
      await topicsApi.verifyActuality(pendingConfirm.id, pendingConfirm.version)
      setRecords(prev => prev.filter(r => r.id !== pendingConfirm.id))
      toast.success(`„${pendingConfirm.title}” — aktualność potwierdzona`)
      setPendingConfirm(null)
    } catch (err) {
      if (err.status === 409) {
        toast.error('Ten wpis został w międzyczasie zmieniony przez innego użytkownika — lista została odświeżona')
        setPendingConfirm(null)
        fetchList(authorFilter)
      } else {
        toast.error(err.message ?? 'Błąd potwierdzania aktualności')
        // Rekord ZOSTAJE na liście, modal ZOSTAJE otwarty — użytkownik może spróbować ponownie.
      }
    } finally {
      setConfirming(false)
    }
  }

  const handleEdit = (record) => {
    setOpen(false)
    navigate(`/dashboard?topic=${record.id}&edit=1`)
  }

  return (
    <>
      <button className="arp-trigger" onClick={handleOpen}>
        <IconClock /> <span>Weryfikacja aktualności</span>
      </button>

      {open && (
        <SlideOverPanel title="Weryfikacja aktualności" onClose={() => setOpen(false)}>
          {/* ── Kompaktowy filtr autora ── */}
          <div className="sop-criteria">
            <div className="sop-field" style={{ maxWidth: 260 }}>
              <label className="sop-label">Autor</label>
              <select className="sop-select" value={authorFilter} onChange={e => setAuthorFilter(e.target.value)}>
                <option value="">Dowolny</option>
                {authors.map(a => <option key={a} value={a}>{a}</option>)}
              </select>
            </div>
          </div>

          {/* ── Lista rekordów wymagających sprawdzenia — reszta wysokości, własne przewijanie ── */}
          <div className="sop-list">
            {loading && (
              <div className="sop-loading">
                <span className="sop-loading-spin" />
                Ładowanie…
              </div>
            )}
            {!loading && error && (
              <EmptyState
                icon={<IconAlert />}
                tone="error"
                title="Nie udało się pobrać listy"
                text="Spróbuj ponownie za chwilę."
                action={<button type="button" className="sop-empty-retry" onClick={() => fetchList(authorFilter)}>Spróbuj ponownie</button>}
              />
            )}
            {!loading && !error && records.length === 0 && (
              <EmptyState
                icon={<IconCheckCircle />}
                tone="success"
                title="Wszystkie notatki są aktualne"
                text="Nic tu obecnie nie wymaga ponownego sprawdzenia."
              />
            )}
            {!loading && !error && records.map(r => {
              const baseDate = r.lastVerificationOfActualityDate ?? r.createdAt
              const categoryPath = findPath(tree, r.categoryId).map(c => c.name).join(' › ')
              return (
                <div key={r.id} className="arp-row">
                  <div className="arp-row-top">
                    <span className="arp-row-title">{r.title}</span>
                    <span className="arp-row-badge" data-type={r.type}>{TYPE_LABEL[r.type] ?? r.type}</span>
                  </div>
                  {categoryPath && <div className="arp-row-path">{categoryPath}</div>}
                  <div className="arp-row-dates">
                    {r.author && <span>Autor: <b>{r.author}</b></span>}
                    <span>Utworzono: <b>{formatDate(r.createdAt)}</b></span>
                    <span>
                      Ostatnio potwierdzono:{' '}
                      <b>{formatDate(r.lastVerificationOfActualityDate) ?? 'Nigdy — termin liczony od utworzenia'}</b>
                    </span>
                    <span className="arp-row-base" title="Data, od której liczony jest skonfigurowany okres ważności">
                      Data bazowa: <b>{formatDate(baseDate)}</b>
                    </span>
                  </div>
                  <div className="arp-row-actions">
                    <button type="button" className="arp-btn arp-btn--confirm" onClick={() => requestConfirm(r)}>
                      Potwierdź aktualność
                    </button>
                    <button type="button" className="arp-btn arp-btn--edit" onClick={() => handleEdit(r)}>
                      Edytuj
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        </SlideOverPanel>
      )}

      {pendingConfirm && (
        <ConfirmModal
          title="Potwierdzić aktualność?"
          message={`„${pendingConfirm.title}” zostanie oznaczone jako aktualne, bez edycji treści. Datę ostatniej weryfikacji ustawimy na teraz.`}
          confirmLabel="Potwierdź aktualność"
          loading={confirming}
          onConfirm={doConfirm}
          onClose={() => !confirming && setPendingConfirm(null)}
        />
      )}

      <style>{`
        .arp-trigger {
          display:flex; align-items:center; gap:6px; width:calc(100% - 4px);
          margin:6px 2px 0; padding:7px 9px;
          border:1.5px solid var(--border-1); border-radius:var(--radius-md);
          background:var(--bg-2); color:var(--text-2);
          font-size:0.78rem; font-weight:600; cursor:pointer;
          transition:background var(--t-fast), border-color var(--t-fast), color var(--t-fast);
        }
        .arp-trigger:hover { background:var(--bg-1); border-color:var(--border-0); color:var(--accent); }

        .arp-row {
          display:flex; flex-direction:column; gap:6px;
          padding:14px 16px; background:var(--bg-2); border:1px solid var(--border-1); border-radius:var(--radius-md);
          transition:border-color var(--t-fast), background var(--t-fast);
        }
        .arp-row:hover { background:var(--bg-1); border-color:var(--border-0); }
        .arp-row-top { display:flex; align-items:center; gap:8px; }
        .arp-row-title { font-size:0.9rem; font-weight:700; color:var(--text-0); flex:1; min-width:0; }
        .arp-row-badge {
          font-family:var(--font-mono); font-size:0.6rem; font-weight:700;
          letter-spacing:0.06em; text-transform:uppercase; flex-shrink:0;
          padding:2px 8px; border-radius:100px;
          background:var(--accent-light); border:1px solid var(--border-0); color:var(--accent);
        }
        .arp-row-badge[data-type="INSTRUCTION"] { background:var(--violet-light); border-color:var(--violet-border); color:var(--violet); }
        .arp-row-path { font-size:0.72rem; color:var(--text-3); }
        .arp-row-dates { display:flex; flex-direction:column; gap:3px; font-size:0.74rem; color:var(--text-2); margin-top:2px; }
        .arp-row-dates b { color:var(--text-1); font-weight:600; }
        .arp-row-base { color:var(--text-3); }

        .arp-row-actions { display:flex; gap:8px; margin-top:6px; }
        .arp-btn {
          flex:1; height:36px; border-radius:var(--radius-md); font-size:0.78rem; font-weight:700;
          cursor:pointer; white-space:nowrap; transition:all var(--t-fast); border:1.5px solid transparent;
        }
        .arp-btn--confirm { background:var(--green-light); color:var(--green); border-color:rgba(22,163,74,.3); }
        .arp-btn--confirm:hover { background:var(--green-soft); }
        .arp-btn--edit { background:var(--accent-light); color:var(--accent); border-color:var(--border-0); }
        .arp-btn--edit:hover { background:var(--accent-glow-strong); }
      `}</style>
    </>
  )
}
