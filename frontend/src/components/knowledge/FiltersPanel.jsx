import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { categoriesApi, topicsApi } from '@/api/services'
import { toast } from '@/store'
import { findPath } from '@/pages/dashboardTreeUtils'
import SlideOverPanel, { EmptyState, IconSearch, IconAlert } from './SlideOverPanel'

const DETAIL_LEVEL_OPTS = [
  { value: 'LOW',    label: 'Niski',  color: '#64748b' },
  { value: 'MEDIUM', label: 'Średni', color: '#d97706' },
  { value: 'HIGH',   label: 'Wysoki', color: '#16a34a' },
]
const DETAIL_LEVEL_LABEL = Object.fromEntries(DETAIL_LEVEL_OPTS.map(o => [o.value, o.label]))
const DETAIL_LEVEL_COLOR = Object.fromEntries(DETAIL_LEVEL_OPTS.map(o => [o.value, o.color]))
const TYPE_LABEL = { INSTRUCTION: 'Instrukcja', NOTE: 'Notatka' }

const IconFilter = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
    <path d="M1.5 2.5h11L8.5 7.5v4L5.5 13v-5.5L1.5 2.5z" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round"/>
  </svg>
)

/**
 * Guzik "Filtry" + panel-drawer filtrów (autor / poziom szczegółowości / kategoria+podkategoria)
 * z listą wyników. Układ/animacja (backdrop, panel-drawer, nagłówek, Escape) to SlideOverPanel —
 * współdzielony z ActualityReviewPanel, razem ze wspólnymi klasami CSS pól/przycisków/pustych
 * stanów (`sop-*`); logika filtrowania i sama lista wyników (inny kształt danych, inne API,
 * inny wygląd wiersza) zostają tutaj, osobno.
 */
export default function FiltersPanel() {
  const [open, setOpen]       = useState(false)
  const [loaded, setLoaded]   = useState(false)
  const [treeLoading, setTreeLoading] = useState(false)
  const [tree, setTree]       = useState([])
  const [authors, setAuthors] = useState([])

  const [author, setAuthor]             = useState('')
  const [detailLevel, setDetailLevel]   = useState('')
  const [categoryId, setCategoryId]     = useState('')
  const [subcategoryId, setSubcategoryId] = useState('')

  const [results, setResults]   = useState(null)
  const [searched, setSearched] = useState(false)
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState(false)

  const navigate = useNavigate()

  const ensureLoaded = useCallback(async () => {
    if (loaded) return
    setTreeLoading(true)
    try {
      const [treeRes, authorsRes] = await Promise.all([
        categoriesApi.getTree(),
        topicsApi.listAuthors(),
      ])
      setTree(treeRes.data.children ?? [])
      setAuthors(authorsRes.data ?? [])
      setLoaded(true)
    } catch {
      toast.error('Błąd ładowania danych filtra')
    } finally {
      setTreeLoading(false)
    }
  }, [loaded])

  const handleOpen = () => { setOpen(true); ensureLoaded() }

  const activeCategory = tree.find(c => c.id === categoryId)
  const subcategoryOpts = activeCategory?.children ?? []

  const hasAnyFilter = Boolean(author || detailLevel || categoryId)

  const runSearch = async () => {
    if (!hasAnyFilter) { toast.error('Wybierz co najmniej jeden filtr'); return }
    setSearching(true)
    setSearched(true)
    setSearchError(false)
    try {
      const res = await topicsApi.filter({
        author: author || undefined,
        detailLevel: detailLevel || undefined,
        categoryId: subcategoryId || categoryId || undefined,
      })
      setResults(res.data)
    } catch {
      setResults(null)
      setSearchError(true)
      toast.error('Błąd filtrowania')
    } finally {
      setSearching(false)
    }
  }

  const handleClear = () => {
    setAuthor(''); setDetailLevel(''); setCategoryId(''); setSubcategoryId('')
    setResults(null); setSearched(false); setSearchError(false)
  }

  const handlePick = (t) => {
    navigate(`/dashboard?topic=${t.id}`)
    setOpen(false)
  }

  return (
    <>
      <button className="fp-trigger" onClick={handleOpen}>
        <IconFilter /> <span>Filtry</span>
      </button>

      {open && (
        <SlideOverPanel title="Filtry" onClose={() => setOpen(false)}>
          {treeLoading ? (
            <div className="sop-loading">
              <span className="sop-loading-spin" />
              Ładowanie…
            </div>
          ) : (
            <>
              {/* ── Kryteria — jedna uporządkowana sekcja na górze ── */}
              <div className="sop-criteria">
                <div className="sop-criteria-grid">
                  <div className="sop-field">
                    <label className="sop-label">Autor</label>
                    <select className="sop-select" value={author} onChange={e => setAuthor(e.target.value)}>
                      <option value="">Dowolny</option>
                      {authors.map(a => <option key={a} value={a}>{a}</option>)}
                    </select>
                  </div>

                  <div className="sop-field">
                    <label className="sop-label">Kategoria</label>
                    <select
                      className="sop-select"
                      value={categoryId}
                      onChange={e => { setCategoryId(e.target.value); setSubcategoryId('') }}
                    >
                      <option value="">Dowolna</option>
                      {tree.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  </div>

                  <div className="sop-field">
                    <label className="sop-label">Podkategoria</label>
                    <select
                      className="sop-select"
                      value={subcategoryId}
                      onChange={e => setSubcategoryId(e.target.value)}
                      disabled={!categoryId || subcategoryOpts.length === 0}
                    >
                      <option value="">{categoryId ? 'Dowolna' : '—'}</option>
                      {subcategoryOpts.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                    </select>
                  </div>

                  <div className="sop-field">
                    <label className="sop-label">Poziom szczegółowości</label>
                    <div className="fp-chip-group">
                      {DETAIL_LEVEL_OPTS.map(opt => (
                        <button
                          key={opt.value}
                          type="button"
                          className={`fp-chip${detailLevel === opt.value ? ' fp-chip--on' : ''}`}
                          style={detailLevel === opt.value ? { borderColor: opt.color, color: opt.color, background: `${opt.color}14` } : {}}
                          onClick={() => setDetailLevel(detailLevel === opt.value ? '' : opt.value)}
                        >
                          {opt.label}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                {authors.length === 0 && (
                  <p className="fp-hint">Brak jeszcze autorów — pole uzupełni się, gdy trafi tu wpis z autora z konta.</p>
                )}

                <div className="sop-actions">
                  <button className="sop-btn sop-btn-secondary" type="button" onClick={handleClear}>Wyczyść</button>
                  <button className="sop-btn sop-btn-primary" type="button" onClick={runSearch} disabled={searching}>
                    {searching && <span className="sop-spin" />}
                    Zastosuj filtry
                  </button>
                </div>
              </div>

              {/* ── Wyniki — reszta dostępnej wysokości, własne przewijanie ── */}
              <div className="sop-list">
                {!searched && (
                  <EmptyState
                    icon={<IconSearch />}
                    title="Wybierz kryteria"
                    text="Ustaw co najmniej jeden filtr powyżej i kliknij „Zastosuj filtry”, żeby zobaczyć wyniki."
                  />
                )}
                {searched && searching && (
                  <div className="sop-loading">
                    <span className="sop-loading-spin" />
                    Szukam…
                  </div>
                )}
                {searched && !searching && searchError && (
                  <EmptyState
                    icon={<IconAlert />}
                    tone="error"
                    title="Nie udało się pobrać wyników"
                    text="Spróbuj ponownie za chwilę."
                    action={<button type="button" className="sop-empty-retry" onClick={runSearch}>Spróbuj ponownie</button>}
                  />
                )}
                {searched && !searching && !searchError && results?.length === 0 && (
                  <EmptyState
                    icon={<IconSearch />}
                    title="Brak wyników"
                    text="Żaden wpis nie spełnia wybranych filtrów — spróbuj je poluzować."
                  />
                )}
                {searched && !searching && !searchError && results?.map(t => (
                  <button key={t.id} className="fp-result" onClick={() => handlePick(t)}>
                    <div className="fp-result-top">
                      <span className="fp-result-title">{t.title}</span>
                      <span
                        className="fp-result-level"
                        style={{ color: DETAIL_LEVEL_COLOR[t.detailLevel], borderColor: `${DETAIL_LEVEL_COLOR[t.detailLevel]}55`, background: `${DETAIL_LEVEL_COLOR[t.detailLevel]}14` }}
                      >
                        {DETAIL_LEVEL_LABEL[t.detailLevel] ?? t.detailLevel}
                      </span>
                    </div>
                    <div className="fp-result-meta">
                      <span className="fp-result-badge">{TYPE_LABEL[t.type] ?? t.type}</span>
                      {t.author && <span className="fp-result-badge">{t.author}</span>}
                      <span className="fp-result-path">{findPath(tree, t.categoryId).map(c => c.name).join(' › ')}</span>
                    </div>
                  </button>
                ))}
              </div>
            </>
          )}
        </SlideOverPanel>
      )}

      <style>{`
        .fp-trigger {
          display:flex; align-items:center; gap:6px; width:calc(100% - 4px);
          margin:8px 2px 0; padding:7px 9px;
          border:1.5px solid var(--border-1); border-radius:var(--radius-md);
          background:var(--bg-2); color:var(--text-2);
          font-size:0.78rem; font-weight:600; cursor:pointer;
          transition:background var(--t-fast), border-color var(--t-fast), color var(--t-fast);
        }
        .fp-trigger:hover { background:var(--bg-1); border-color:var(--border-0); color:var(--accent); }

        .fp-hint { font-size:0.71rem; color:var(--text-3); line-height:1.4; }

        .fp-chip-group { display:flex; gap:5px; flex-wrap:wrap; align-items:center; min-height:40px; }
        .fp-chip {
          padding:7px 11px; border-radius:100px; border:1.5px solid var(--border-1);
          background:var(--bg-1); font-size:0.76rem; font-weight:600; color:var(--text-2);
          cursor:pointer; transition:all var(--t-fast); white-space:nowrap;
        }
        .fp-chip:hover { border-color:var(--border-0); }
        .fp-chip--on { font-weight:700; }

        .fp-result {
          display:flex; flex-direction:column; gap:5px; width:100%;
          padding:12px 14px; background:var(--bg-2); border:1px solid var(--border-1); border-radius:var(--radius-md);
          text-align:left; cursor:pointer; transition:background var(--t-fast), border-color var(--t-fast);
        }
        .fp-result:hover { background:var(--bg-1); border-color:var(--border-0); }
        .fp-result-top { display:flex; align-items:center; justify-content:space-between; gap:10px; }
        .fp-result-title { font-size:0.87rem; font-weight:700; color:var(--text-0); }
        .fp-result-level {
          flex-shrink:0; font-size:0.66rem; font-weight:700; letter-spacing:0.03em;
          padding:3px 9px; border-radius:100px; border:1px solid; white-space:nowrap;
        }
        .fp-result-meta { font-size:0.72rem; color:var(--text-3); display:flex; gap:6px; align-items:center; flex-wrap:wrap; }
        .fp-result-badge {
          font-family:var(--font-mono); font-size:0.61rem; font-weight:700;
          letter-spacing:0.05em; text-transform:uppercase;
          padding:2px 7px; border-radius:100px; background:var(--bg-1); border:1px solid var(--border-1); color:var(--text-2);
        }
        .fp-result-path { color:var(--text-3); }
      `}</style>
    </>
  )
}
