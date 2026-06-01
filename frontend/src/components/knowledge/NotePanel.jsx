import { useState, useEffect } from 'react'
import { notesApi, topicsApi } from '@/api/services'
import { toast } from '@/store'
import AskMoreModal from './modals/AskMoreModal'

const DIFFICULTY_OPTS = [
  { value: 'BASIC',  label: 'Podstawowy',   color: '#16a34a', bg: '#f0fdf4', border: 'rgba(22,163,74,.25)' },
  { value: 'MEDIUM', label: 'Średni',        color: '#d97706', bg: '#fffbeb', border: 'rgba(217,119,6,.25)'  },
  { value: 'HARD',   label: 'Zaawansowany',  color: '#dc2626', bg: '#fef2f2', border: 'rgba(220,38,38,.25)'  },
]

const STATUS_LABEL = {
  NEW:            'Nowy',
  NOTE_GENERATED: 'Notatka gotowa',
  QUIZ_READY:     'Quiz dostępny',
  PASSED:         'Zaliczony',
  MASTERED:       'Opanowany',
}

export default function NotePanel({ topic, categoryPath, onTopicUpdated, onOpenTree }) {
  const [note, setNote]               = useState(null)
  const [noteLoading, setNoteLoading] = useState(false)
  const [difficulty, setDifficulty]   = useState(topic?.difficulty ?? 'BASIC')
  const [savingDiff, setSavingDiff]   = useState(false)
  const [showAskMore, setShowAskMore] = useState(false)

  useEffect(() => {
    if (!topic) { setNote(null); return }
    setDifficulty(topic.difficulty)
    setNoteLoading(true)
    notesApi.getByTopic(topic.id)
      .then(res => setNote(res.data))
      .catch(err => { if (err.status === 404) setNote(null); else toast.error('Błąd ładowania notatki') })
      .finally(() => setNoteLoading(false))
  }, [topic?.id])

  const handleDifficultyChange = async (val) => {
    if (val === difficulty || savingDiff) return
    setDifficulty(val)
    setSavingDiff(true)
    try {
      const res = await topicsApi.update(topic.id, { difficulty: val })
      onTopicUpdated?.(res.data)
    } catch {
      toast.error('Błąd zapisu trudności')
      setDifficulty(topic.difficulty)
    } finally {
      setSavingDiff(false)
    }
  }

  /* ── Brak wybranego tematu ── */
  if (!topic) {
    return (
      <div className="np-empty">
        {/* Przycisk otwierający drzewo na małych ekranach */}
        <button className="np-tree-toggle" onClick={onOpenTree} title="Otwórz drzewo kategorii">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <path d="M3 5h12M3 9h8M3 13h10" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
          </svg>
          Kategorie
        </button>
        <div className="np-empty-inner">
          <svg width="52" height="52" viewBox="0 0 52 52" fill="none">
            <rect x="8" y="6" width="36" height="40" rx="4" stroke="var(--border-1)" strokeWidth="1.5"/>
            <path d="M16 18h20M16 25h20M16 32h12" stroke="var(--border-1)" strokeWidth="1.8" strokeLinecap="round"/>
          </svg>
          <h2 className="np-empty-title">Wybierz temat</h2>
          <p className="np-empty-sub">Kliknij temat z drzewa kategorii,<br/>aby wyświetlić lub stworzyć notatkę.</p>
        </div>
        <NoteStyles />
      </div>
    )
  }

  const isCode = topic.code ?? false

  return (
    <div className="note-panel">

      {/* ── Nagłówek ── */}
      <div className="note-header">
        <div className="note-header-top">
          {/* Hamburger na małych ekranach */}
          <button className="np-tree-toggle np-tree-toggle--inline" onClick={onOpenTree} title="Otwórz drzewo">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M2 4.5h12M2 8.5h8M2 12.5h10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
            </svg>
          </button>
          <div className="note-breadcrumb">
            {(categoryPath ?? []).map((cat, i) => (
              <span key={cat.id}>
                {i > 0 && <span className="bc-sep">›</span>}
                <span className="bc-item">{cat.name}</span>
              </span>
            ))}
          </div>
          <div className="note-badges">
            <span className="note-status" data-status={topic.status}>
              {STATUS_LABEL[topic.status] ?? topic.status}
            </span>
            {isCode && (
              <span className="note-code-badge">
                <svg width="11" height="11" viewBox="0 0 11 11" fill="none">
                  <path d="M7.5 2l3 3.5-3 3.5M3.5 2l-3 3.5 3 3.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                KOD
              </span>
            )}
          </div>
        </div>
        <h1 className="note-title">{topic.title}</h1>
      </div>

      {/* ── Konfiguracja ── */}
      <div className="note-config">

        {/* Baner KOD — widoczny przy dopisywaniu gdy temat jest kodem */}
        {isCode && (
          <div className="note-code-banner">
            <span className="note-code-banner-icon">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M9.5 2.5l3 4-3 4M4.5 2.5l-3 4 3 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </span>
            <div>
              <span className="note-code-banner-title">Notatka oznaczona jako KOD</span>
              <span className="note-code-banner-sub">
                AI wygeneruje treść z przykładami kodu · dostępny przycisk "Wygeneruj zadania"
              </span>
            </div>
          </div>
        )}

        <div className="note-config-row">
          <span className="note-config-label">Trudność</span>
          <div className="diff-group">
            {DIFFICULTY_OPTS.map(opt => (
              <button
                key={opt.value}
                className={`diff-btn${difficulty === opt.value ? ' diff-btn--on' : ''}`}
                style={difficulty === opt.value ? { background: opt.bg, borderColor: opt.border, color: opt.color } : {}}
                onClick={() => handleDifficultyChange(opt.value)}
                disabled={savingDiff}
              >
                {opt.label}
                {difficulty === opt.value && savingDiff && <span className="diff-saving" />}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* ── Treść notatki ── */}
      <div className="note-content-area">
        {noteLoading ? (
          <div className="note-loading"><span className="note-spinner" /> Ładowanie notatki...</div>
        ) : note ? (
          <NoteContentView note={note} />
        ) : (
          <EmptyNote />
        )}
      </div>

      {/* ── Pasek akcji ── */}
      <div className="note-actions">
        {isCode && (
          <button className="act-btn act-btn--code" disabled title="Wkrótce — wymaga AI">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M9.5 2.5l3 4-3 4M4.5 2.5l-3 4 3 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            Wygeneruj zadania
          </button>
        )}
        <div className="act-spacer" />
        <button className="act-btn act-btn--ask" onClick={() => setShowAskMore(true)}>
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <circle cx="7" cy="7" r="6" stroke="currentColor" strokeWidth="1.4"/>
            <path d="M5.3 5.5C5.5 4.6 6.1 4 7 4c.9 0 1.7.7 1.7 1.5 0 .9-.9 1.4-1.7 1.6v.9" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
            <circle cx="7" cy="10.5" r="0.6" fill="currentColor"/>
          </svg>
          Dopytaj o...
        </button>
        <button className="act-btn act-btn--quiz" disabled title="Wkrótce dostępne">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <rect x="1.5" y="1.5" width="11" height="11" rx="2" stroke="currentColor" strokeWidth="1.4"/>
            <path d="M4 4.5h6M4 7h6M4 9.5h4" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
          </svg>
          Rozpocznij quiz
        </button>
      </div>

      {showAskMore && (
        <AskMoreModal topicTitle={topic.title} onClose={() => setShowAskMore(false)} />
      )}

      <NoteStyles />
    </div>
  )
}

function EmptyNote() {
  return (
    <div className="note-empty-content">
      <div className="nec-top">
        <svg width="44" height="44" viewBox="0 0 44 44" fill="none">
          <circle cx="22" cy="22" r="19" stroke="var(--border-1)" strokeWidth="1.5"/>
          <path d="M14 22h16M22 14v16" stroke="var(--border-1)" strokeWidth="2" strokeLinecap="round"/>
        </svg>
        <p className="nec-title">Brak notatki</p>
        <p className="nec-sub">Treść zostanie wygenerowana przez AI.<br/>Integracja w przygotowaniu.</p>
      </div>
      <div className="nec-skeleton">
        {[90, 75, 83, 60, 88, 70, 55, 78].map((w, i) => (
          <span key={i} className="nec-line" style={{ width: `${w}%`, animationDelay: `${i * 0.12}s` }} />
        ))}
      </div>
    </div>
  )
}

function NoteContentView({ note }) {
  const c = note.content
  if (!c) return null
  return (
    <article className="note-article">
      {c.title            && <h2 className="na-title">{c.title}</h2>}
      {c.summary          && <p  className="na-summary">{c.summary}</p>}
      {c.simpleExplanation && (
        <div className="na-simple">
          <span className="na-simple-label">Jak dla dziecka</span>
          <p>{c.simpleExplanation}</p>
        </div>
      )}
      {(c.sections ?? []).map((s, i) => (
        <section key={i} className="na-section">
          <h3 className="na-section-h">{s.heading}</h3>
          <p  className="na-section-p">{s.content}</p>
        </section>
      ))}
      {(c.examples ?? []).length > 0 && <>
        <h3 className="na-sub-h">Przykłady</h3>
        {c.examples.map((ex, i) => (
          <div key={i} className="na-example">
            {ex.title       && <p className="na-example-title">{ex.title}</p>}
            {ex.code        && <pre className="na-code"><code>{ex.code}</code></pre>}
            {ex.explanation && <p className="na-example-exp">{ex.explanation}</p>}
          </div>
        ))}
      </>}
      {(c.memoryPoints ?? []).length > 0 && <>
        <h3 className="na-sub-h">Do zapamiętania</h3>
        <ul className="na-list na-list--green">
          {c.memoryPoints.map((p, i) => <li key={i}>{p}</li>)}
        </ul>
      </>}
      {(c.commonMistakes ?? []).length > 0 && <>
        <h3 className="na-sub-h">Częste błędy</h3>
        <ul className="na-list na-list--red">
          {c.commonMistakes.map((m, i) => <li key={i}>{m}</li>)}
        </ul>
      </>}
    </article>
  )
}

function NoteStyles() {
  return (
    <style>{`
      /* ── Puste ── */
      .np-empty {
        flex:1; display:flex; flex-direction:column;
        align-items:center; justify-content:center;
        background:var(--bg-0); position:relative; padding:20px;
      }
      .np-empty-inner { text-align:center; }
      .np-empty-inner svg { margin:0 auto 16px; }
      .np-empty-title { font-size:1.1rem; font-weight:700; color:var(--text-1); margin-bottom:8px; }
      .np-empty-sub   { font-size:0.82rem; color:var(--text-3); line-height:1.7; }

      /* Hamburger przycisk (tablet/mobile) */
      .np-tree-toggle {
        display:none;
        position:absolute; top:16px; left:16px;
        align-items:center; gap:6px;
        padding:7px 12px; background:#fff;
        border:1px solid var(--border-1); border-radius:var(--radius-md);
        font-size:0.78rem; font-weight:600; color:var(--text-1);
        cursor:pointer; box-shadow:var(--shadow-sm);
        transition:background var(--t-fast);
      }
      .np-tree-toggle:hover { background:var(--bg-2); }
      .np-tree-toggle--inline {
        position:static; flex-shrink:0;
      }

      /* ── Panel ── */
      .note-panel {
        flex:1; display:flex; flex-direction:column;
        overflow:hidden; background:#fff;
        min-width:0;
      }

      /* Header */
      .note-header { padding:18px 24px 14px; border-bottom:1px solid var(--border-1); flex-shrink:0; }
      .note-header-top {
        display:flex; align-items:center; gap:8px; margin-bottom:8px; flex-wrap:wrap;
      }
      .note-breadcrumb {
        flex:1; display:flex; align-items:center; gap:3px; min-width:0; flex-wrap:wrap;
      }
      .bc-sep  { color:var(--text-3); font-size:0.72rem; margin:0 2px; }
      .bc-item { font-size:0.71rem; color:var(--text-3); white-space:nowrap; }
      .note-badges { display:flex; gap:5px; align-items:center; flex-shrink:0; }
      .note-status {
        font-family:var(--font-mono); font-size:0.6rem; font-weight:700;
        letter-spacing:0.07em; text-transform:uppercase;
        padding:3px 8px; border-radius:100px;
        background:var(--bg-2); border:1px solid var(--border-1); color:var(--text-3);
      }
      [data-status="NOTE_GENERATED"] { background:#eff6ff; border-color:rgba(37,99,235,.2); color:var(--accent); }
      [data-status="PASSED"]         { background:#f0fdf4; border-color:rgba(22,163,74,.25); color:#16a34a; }
      [data-status="MASTERED"]       { background:#fdf4ff; border-color:rgba(147,51,234,.25); color:#9333ea; }
      .note-code-badge {
        display:inline-flex; align-items:center; gap:4px;
        font-family:var(--font-mono); font-size:0.6rem; font-weight:700;
        letter-spacing:0.1em; padding:3px 8px; border-radius:100px;
        background:#eff6ff; border:1px solid rgba(37,99,235,.2); color:var(--accent);
      }
      .note-title { font-size:1.3rem; font-weight:800; color:var(--text-0); line-height:1.25; letter-spacing:-0.01em; }

      /* Config */
      .note-config {
        padding:12px 24px; border-bottom:1px solid var(--border-1);
        flex-shrink:0; background:var(--bg-2);
        display:flex; flex-direction:column; gap:10px;
      }

      /* Baner KOD */
      .note-code-banner {
        display:flex; align-items:flex-start; gap:10px;
        padding:10px 12px; border-radius:var(--radius-md);
        background:#eff6ff; border:1px solid rgba(37,99,235,.25);
      }
      .note-code-banner-icon {
        color:var(--accent); flex-shrink:0; display:flex; margin-top:1px;
      }
      .note-code-banner-title {
        display:block; font-size:0.78rem; font-weight:700; color:var(--accent);
        margin-bottom:2px;
      }
      .note-code-banner-sub {
        display:block; font-size:0.71rem; color:var(--text-2); line-height:1.5;
      }

      .note-config-row { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
      .note-config-label { font-size:0.71rem; font-weight:700; color:var(--text-2); letter-spacing:0.04em; white-space:nowrap; }
      .diff-group { display:flex; gap:5px; flex-wrap:wrap; }
      .diff-btn {
        padding:5px 12px; border-radius:var(--radius-md);
        border:1.5px solid var(--border-1); background:#fff;
        font-size:0.77rem; font-weight:600; color:var(--text-2);
        cursor:pointer; transition:all var(--t-fast); display:flex; align-items:center; gap:4px;
      }
      .diff-btn:hover { border-color:var(--border-0); }
      .diff-btn--on   { font-weight:700; }
      .diff-btn:disabled { opacity:.6; cursor:wait; }
      .diff-saving { width:9px; height:9px; border-radius:50%; border:1.5px solid transparent; border-top-color:currentColor; animation:spin .6s linear infinite; }

      /* Content */
      .note-content-area { flex:1; overflow-y:auto; padding:20px 24px; }
      .note-loading { display:flex; align-items:center; gap:10px; font-size:0.82rem; color:var(--text-2); padding:40px 0; }
      .note-spinner { width:16px; height:16px; border-radius:50%; border:2px solid var(--border-1); border-top-color:var(--accent); animation:spin .7s linear infinite; }

      /* Empty content */
      .note-empty-content { display:flex; flex-direction:column; align-items:center; gap:20px; padding:24px 0; }
      .nec-top { text-align:center; }
      .nec-top svg { margin:0 auto 12px; }
      .nec-title { font-size:0.95rem; font-weight:700; color:var(--text-2); margin-bottom:5px; }
      .nec-sub   { font-size:0.78rem; color:var(--text-3); line-height:1.6; }
      .nec-skeleton { width:100%; max-width:500px; display:flex; flex-direction:column; gap:10px; }
      .nec-line {
        height:11px; border-radius:6px; display:block;
        background:linear-gradient(90deg, var(--bg-3) 25%, var(--bg-4) 50%, var(--bg-3) 75%);
        background-size:200% 100%; animation:shimmer 2s ease infinite;
      }

      /* Artikel */
      .note-article { max-width:680px; }
      .na-title   { font-size:1.15rem; font-weight:800; color:var(--text-0); margin-bottom:10px; }
      .na-summary { font-size:0.88rem; color:var(--text-1); line-height:1.7; margin-bottom:16px; padding-bottom:16px; border-bottom:1px solid var(--border-1); }
      .na-simple  { background:var(--accent-light); border:1px solid var(--border-0); border-radius:var(--radius-md); padding:12px 16px; margin-bottom:20px; }
      .na-simple-label { font-size:0.65rem; font-weight:700; text-transform:uppercase; letter-spacing:.08em; color:var(--accent); display:block; margin-bottom:5px; }
      .na-simple p { font-size:0.84rem; color:var(--text-1); line-height:1.6; }
      .na-section { margin-bottom:16px; }
      .na-section-h { font-size:0.93rem; font-weight:700; color:var(--text-0); margin-bottom:5px; }
      .na-section-p { font-size:0.84rem; color:var(--text-1); line-height:1.7; }
      .na-sub-h { font-size:0.88rem; font-weight:700; color:var(--text-0); margin:18px 0 10px; }
      .na-example { background:var(--bg-2); border-radius:var(--radius-md); padding:12px 14px; margin-bottom:10px; }
      .na-example-title { font-size:0.79rem; font-weight:700; color:var(--text-1); margin-bottom:7px; }
      .na-code { background:var(--bg-3); border-radius:var(--radius-sm); padding:10px 12px; overflow-x:auto; font-family:var(--font-mono); font-size:0.79rem; color:var(--text-0); margin:7px 0; }
      .na-example-exp { font-size:0.79rem; color:var(--text-2); margin-top:5px; line-height:1.6; }
      .na-list { padding-left:18px; display:flex; flex-direction:column; gap:5px; }
      .na-list li { font-size:0.84rem; color:var(--text-1); line-height:1.6; }
      .na-list--green li::marker { color:#16a34a; }
      .na-list--red   li::marker { color:var(--red); }

      /* Actions */
      .note-actions {
        padding:10px 16px; border-top:1px solid var(--border-1); flex-shrink:0;
        display:flex; align-items:center; gap:6px; background:#fff;
        flex-wrap:wrap;
      }
      .act-spacer { flex:1; }
      .act-btn {
        display:inline-flex; align-items:center; gap:6px;
        padding:8px 14px; border-radius:var(--radius-md); border:1.5px solid var(--border-1);
        font-size:0.78rem; font-weight:600; cursor:pointer;
        transition:all var(--t-fast); white-space:nowrap; background:#fff;
      }
      .act-btn:disabled { opacity:.45; cursor:not-allowed; }
      .act-btn--code  { background:#f0fdf4; color:#16a34a; border-color:rgba(22,163,74,.3); }
      .act-btn--code:hover:not(:disabled)  { background:#dcfce7; }
      .act-btn--ask   { background:var(--accent-light); color:var(--accent); border-color:var(--border-0); }
      .act-btn--ask:hover   { background:var(--accent-glow-strong); }
      .act-btn--quiz  { background:#f5f3ff; color:#7c3aed; border-color:rgba(124,58,237,.25); }
      .act-btn--quiz:hover:not(:disabled) { background:#ede9fe; }

      /* ── Responsywność ── */
      @media (max-width: 860px) {
        .np-tree-toggle { display:flex; }
        .np-tree-toggle--inline { display:flex; }
        .note-title { font-size:1.1rem; }
        .note-header { padding:14px 16px 12px; }
        .note-config { padding:10px 16px; }
        .note-content-area { padding:16px; }
        .note-actions { padding:10px 12px; }
        .act-btn { padding:8px 10px; font-size:0.74rem; }
        .diff-btn { padding:5px 8px; font-size:0.72rem; }
      }

      @media (max-width: 540px) {
        .act-btn span:not(.act-icon) { display:none; }
        .note-title { font-size:1rem; }
        .bc-item { font-size:0.68rem; }
      }
    `}</style>
  )
}
