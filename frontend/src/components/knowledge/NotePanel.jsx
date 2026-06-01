import { useState, useEffect } from 'react'
import { notesApi, topicsApi } from '@/api/services'
import { toast } from '@/store'
import AskMoreModal from './modals/AskMoreModal'

const DIFFICULTY_OPTS = [
  { value: 'BASIC',  label: 'Podstawowy', color: '#16a34a', bg: '#f0fdf4', border: 'rgba(22,163,74,.25)' },
  { value: 'MEDIUM', label: 'Średni',     color: '#d97706', bg: '#fffbeb', border: 'rgba(217,119,6,.25)' },
  { value: 'HARD',   label: 'Zaawansowany', color: '#dc2626', bg: '#fef2f2', border: 'rgba(220,38,38,.25)' },
]

export default function NotePanel({ topic, categoryPath, onTopicUpdated }) {
  const [note, setNote]               = useState(null)
  const [noteLoading, setNoteLoading] = useState(false)
  const [difficulty, setDifficulty]   = useState(topic?.difficulty ?? 'BASIC')
  const [isCode, setIsCode]           = useState(false)
  const [savingDiff, setSavingDiff]   = useState(false)
  const [showAskMore, setShowAskMore] = useState(false)

  /* Załaduj notatkę gdy topic się zmienia */
  useEffect(() => {
    if (!topic) { setNote(null); return }
    setDifficulty(topic.difficulty)
    setNoteLoading(true)
    notesApi.getByTopic(topic.id)
      .then(res => setNote(res.data))
      .catch(err => {
        if (err.status === 404) setNote(null)
        else toast.error('Błąd ładowania notatki')
      })
      .finally(() => setNoteLoading(false))
  }, [topic?.id])

  /* Zmiana poziomu trudności → aktualizacja tematu */
  const handleDifficultyChange = async (val) => {
    if (val === difficulty || savingDiff) return
    setDifficulty(val)
    setSavingDiff(true)
    try {
      const res = await topicsApi.update(topic.id, { difficulty: val })
      onTopicUpdated?.(res.data)
    } catch {
      toast.error('Błąd zapisu poziomu trudności')
      setDifficulty(topic.difficulty)
    } finally {
      setSavingDiff(false)
    }
  }

  /* ── Pusty ekran gdy brak wybranego tematu ── */
  if (!topic) {
    return (
      <div className="note-empty-screen">
        <div className="note-empty-inner">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
            <rect x="6" y="4" width="36" height="40" rx="4" stroke="var(--border-1)" strokeWidth="2"/>
            <path d="M14 16h20M14 22h20M14 28h14" stroke="var(--border-1)" strokeWidth="2" strokeLinecap="round"/>
          </svg>
          <h2 className="note-empty-title">Wybierz temat</h2>
          <p className="note-empty-sub">
            Kliknij temat z drzewa kategorii po lewej,<br/>aby zobaczyć lub stworzyć notatkę.
          </p>
        </div>
        <NoteStyles />
      </div>
    )
  }

  const currentDiff = DIFFICULTY_OPTS.find(d => d.value === difficulty)

  return (
    <div className="note-panel">

      {/* ── Nagłówek ── */}
      <div className="note-header">
        <div className="note-breadcrumb">
          {categoryPath?.map((cat, i) => (
            <span key={cat.id}>
              {i > 0 && <span className="note-bc-sep">›</span>}
              <span className="note-bc-item">{cat.name}</span>
            </span>
          ))}
        </div>
        <h1 className="note-title">{topic.title}</h1>
        <div className="note-meta">
          <span className="note-status-badge" data-status={topic.status}>
            {STATUS_LABEL[topic.status] ?? topic.status}
          </span>
        </div>
      </div>

      {/* ── Konfiguracja notatki ── */}
      <div className="note-config">

        {/* Poziom trudności */}
        <div className="note-config-row">
          <span className="note-config-label">Poziom trudności</span>
          <div className="diff-group">
            {DIFFICULTY_OPTS.map(opt => (
              <button
                key={opt.value}
                className={`diff-btn${difficulty === opt.value ? ' diff-btn--on' : ''}`}
                style={difficulty === opt.value
                  ? { background: opt.bg, borderColor: opt.border, color: opt.color }
                  : {}}
                onClick={() => handleDifficultyChange(opt.value)}
                disabled={savingDiff}
              >
                {opt.label}
                {difficulty === opt.value && savingDiff && <span className="diff-saving" />}
              </button>
            ))}
          </div>
        </div>

        {/* Czy to kod */}
        <div className="note-config-row">
          <label className="code-check">
            <input
              type="checkbox"
              checked={isCode}
              onChange={e => setIsCode(e.target.checked)}
              className="code-check-input"
            />
            <span className="code-check-box">
              {isCode && (
                <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
                  <path d="M1.5 5.5L4 8l4.5-5.5" stroke="white" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              )}
            </span>
            <span className="code-check-label">
              <span className="code-check-text">Czy to KOD?</span>
              <span className="code-check-sub">Zaznacz jeśli notatka dotyczy kodu / programowania</span>
            </span>
            {isCode && (
              <span className="code-check-badge">
                <svg width="11" height="11" viewBox="0 0 11 11" fill="none">
                  <path d="M7 2l2.5 3.5L7 9M4 2L1.5 5.5 4 9" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                KOD
              </span>
            )}
          </label>
        </div>
      </div>

      {/* ── Treść notatki ── */}
      <div className="note-content-area">
        {noteLoading ? (
          <div className="note-loading">
            <span className="note-spinner" />
            <span>Ładowanie notatki...</span>
          </div>
        ) : note ? (
          <NoteContentView note={note} />
        ) : (
          <div className="note-content-empty">
            <div className="nce-icon">
              <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
                <circle cx="20" cy="20" r="17" stroke="var(--border-1)" strokeWidth="1.5"/>
                <path d="M13 20h14M20 13v14" stroke="var(--border-1)" strokeWidth="2" strokeLinecap="round"/>
              </svg>
            </div>
            <p className="nce-title">Brak notatki</p>
            <p className="nce-sub">
              Treść notatki pojawi się tutaj po wygenerowaniu przez AI.<br/>
              Integracja z AI w przygotowaniu.
            </p>
            <div className="nce-placeholder">
              <span className="nce-placeholder-line" style={{ width: '90%' }} />
              <span className="nce-placeholder-line" style={{ width: '75%' }} />
              <span className="nce-placeholder-line" style={{ width: '82%' }} />
              <span className="nce-placeholder-line" style={{ width: '60%' }} />
              <span className="nce-placeholder-line" style={{ width: '88%' }} />
              <span className="nce-placeholder-line" style={{ width: '70%' }} />
            </div>
          </div>
        )}
      </div>

      {/* ── Pasek akcji ── */}
      <div className="note-actions">
        {isCode && (
          <button className="act-btn act-btn--code" title="Wygeneruj zadania programistyczne" disabled>
            <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
              <path d="M10 3l3.5 4.5L10 12M5 3L1.5 7.5 5 12" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            Wygeneruj zadania
          </button>
        )}

        <div className="act-right">
          <button
            className="act-btn act-btn--ask"
            onClick={() => setShowAskMore(true)}
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <circle cx="7" cy="7" r="6" stroke="currentColor" strokeWidth="1.4"/>
              <path d="M5.5 5.5C5.5 4.67 6.17 4 7 4s1.5.67 1.5 1.5c0 .8-.8 1.3-1.5 1.5v1" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
              <circle cx="7" cy="10.5" r="0.6" fill="currentColor"/>
            </svg>
            Dopytaj o...
          </button>

          <button className="act-btn act-btn--quiz" disabled title="Wkrótce dostępne">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M2 2h10v10H2z" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round"/>
              <path d="M5 5h4M5 7h4M5 9h2" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
            </svg>
            Rozpocznij quiz
          </button>
        </div>
      </div>

      {/* ── Modal dopytaj ── */}
      {showAskMore && (
        <AskMoreModal
          topicTitle={topic.title}
          onClose={() => setShowAskMore(false)}
        />
      )}

      <NoteStyles />
    </div>
  )
}

/* Widok treści notatki (gdy nota istnieje) */
function NoteContentView({ note }) {
  const c = note.content
  if (!c) return null
  return (
    <article className="note-article">
      {c.title && <h2 className="na-title">{c.title}</h2>}
      {c.summary && <p className="na-summary">{c.summary}</p>}
      {c.simpleExplanation && (
        <div className="na-simple">
          <span className="na-simple-label">Jak dla dziecka</span>
          <p>{c.simpleExplanation}</p>
        </div>
      )}
      {(c.sections ?? []).map((s, i) => (
        <section key={i} className="na-section">
          <h3 className="na-section-h">{s.heading}</h3>
          <p className="na-section-p">{s.content}</p>
        </section>
      ))}
      {(c.examples ?? []).length > 0 && (
        <div className="na-examples">
          <h3 className="na-sub-h">Przykłady</h3>
          {c.examples.map((ex, i) => (
            <div key={i} className="na-example">
              {ex.title && <p className="na-example-title">{ex.title}</p>}
              {ex.code  && <pre className="na-code"><code>{ex.code}</code></pre>}
              {ex.explanation && <p className="na-example-exp">{ex.explanation}</p>}
            </div>
          ))}
        </div>
      )}
      {(c.memoryPoints ?? []).length > 0 && (
        <div className="na-list-section">
          <h3 className="na-sub-h">Do zapamiętania</h3>
          <ul className="na-list na-list--green">
            {c.memoryPoints.map((p, i) => <li key={i}>{p}</li>)}
          </ul>
        </div>
      )}
      {(c.commonMistakes ?? []).length > 0 && (
        <div className="na-list-section">
          <h3 className="na-sub-h">Częste błędy</h3>
          <ul className="na-list na-list--red">
            {c.commonMistakes.map((m, i) => <li key={i}>{m}</li>)}
          </ul>
        </div>
      )}
    </article>
  )
}

const STATUS_LABEL = {
  NEW:            'Nowy',
  NOTE_GENERATED: 'Notatka gotowa',
  QUIZ_READY:     'Quiz dostępny',
  PASSED:         'Zaliczony',
  MASTERED:       'Opanowany',
}

function NoteStyles() {
  return (
    <style>{`
      /* ── Pusty ── */
      .note-empty-screen {
        flex:1; display:flex; align-items:center; justify-content:center;
        background:var(--bg-0);
      }
      .note-empty-inner { text-align:center; }
      .note-empty-inner svg { margin:0 auto 16px; }
      .note-empty-title { font-size:1.1rem; font-weight:700; color:var(--text-1); margin-bottom:8px; }
      .note-empty-sub   { font-size:0.82rem; color:var(--text-3); line-height:1.7; }

      /* ── Panel główny ── */
      .note-panel {
        flex:1; display:flex; flex-direction:column; overflow:hidden;
        background:#fff;
      }

      /* Nagłówek */
      .note-header {
        padding:20px 28px 16px; border-bottom:1px solid var(--border-1); flex-shrink:0;
      }
      .note-breadcrumb { display:flex; align-items:center; gap:4px; margin-bottom:8px; }
      .note-bc-sep  { color:var(--text-3); font-size:0.75rem; margin:0 2px; }
      .note-bc-item { font-size:0.72rem; color:var(--text-3); }
      .note-title   { font-size:1.4rem; font-weight:800; color:var(--text-0); line-height:1.25; margin-bottom:10px; letter-spacing:-0.01em; }
      .note-meta    { display:flex; gap:8px; }
      .note-status-badge {
        font-family:var(--font-mono); font-size:0.62rem; font-weight:700;
        letter-spacing:0.07em; text-transform:uppercase;
        padding:3px 9px; border-radius:100px;
        border:1px solid var(--border-1); color:var(--text-3); background:var(--bg-2);
      }
      [data-status="NOTE_GENERATED"] { background:#eff6ff; border-color:rgba(37,99,235,.2); color:var(--accent); }
      [data-status="PASSED"]  { background:#f0fdf4; border-color:rgba(22,163,74,.25); color:#16a34a; }
      [data-status="MASTERED"]{ background:#fdf4ff; border-color:rgba(147,51,234,.25); color:#9333ea; }

      /* Konfiguracja */
      .note-config {
        padding:14px 28px; border-bottom:1px solid var(--border-1); flex-shrink:0;
        display:flex; flex-direction:column; gap:12px; background:var(--bg-2);
      }
      .note-config-row { display:flex; align-items:center; gap:16px; }
      .note-config-label { font-size:0.72rem; font-weight:700; color:var(--text-2); letter-spacing:0.04em; white-space:nowrap; min-width:130px; }

      /* Difficulty */
      .diff-group { display:flex; gap:6px; }
      .diff-btn {
        padding:6px 14px; border-radius:var(--radius-md);
        border:1.5px solid var(--border-1); background:#fff;
        font-size:0.78rem; font-weight:600; color:var(--text-2);
        cursor:pointer; transition:all var(--t-fast); white-space:nowrap;
        display:flex; align-items:center; gap:5px;
      }
      .diff-btn:hover { border-color:var(--border-0); background:var(--bg-2); }
      .diff-btn--on   { font-weight:700; }
      .diff-btn:disabled { opacity:.6; cursor:wait; }
      .diff-saving {
        width:10px; height:10px; border-radius:50%;
        border:1.5px solid transparent; border-top-color:currentColor;
        animation:spin .6s linear infinite;
      }

      /* Checkbox kod */
      .code-check { display:flex; align-items:center; gap:10px; cursor:pointer; }
      .code-check-input { display:none; }
      .code-check-box {
        width:18px; height:18px; border-radius:5px; flex-shrink:0;
        border:1.5px solid var(--border-1); background:#fff;
        display:flex; align-items:center; justify-content:center;
        transition:all var(--t-fast);
      }
      .code-check-input:checked + .code-check-box {
        background:var(--accent); border-color:var(--accent);
      }
      .code-check-label { display:flex; flex-direction:column; gap:1px; }
      .code-check-text  { font-size:0.82rem; font-weight:600; color:var(--text-1); }
      .code-check-sub   { font-size:0.7rem; color:var(--text-3); }
      .code-check-badge {
        display:inline-flex; align-items:center; gap:4px;
        font-family:var(--font-mono); font-size:0.6rem; font-weight:700;
        letter-spacing:0.1em; padding:3px 7px; border-radius:100px;
        background:#eff6ff; border:1px solid rgba(37,99,235,.2); color:var(--accent);
      }

      /* Treść */
      .note-content-area {
        flex:1; overflow-y:auto; padding:20px 28px;
      }
      .note-loading {
        display:flex; align-items:center; gap:10px;
        font-size:0.82rem; color:var(--text-2); padding:40px 0;
      }
      .note-spinner {
        width:16px; height:16px; border-radius:50%;
        border:2px solid var(--border-1); border-top-color:var(--accent);
        animation:spin .7s linear infinite;
      }

      /* Empty note placeholder */
      .note-content-empty {
        display:flex; flex-direction:column; align-items:center;
        padding:32px 0; gap:8px;
      }
      .nce-icon { color:var(--text-3); margin-bottom:4px; }
      .nce-title { font-size:0.95rem; font-weight:700; color:var(--text-2); }
      .nce-sub   { font-size:0.78rem; color:var(--text-3); text-align:center; line-height:1.7; }
      .nce-placeholder {
        width:100%; max-width:440px; margin-top:20px;
        display:flex; flex-direction:column; gap:10px;
      }
      .nce-placeholder-line {
        height:12px; border-radius:6px; display:block;
        background:linear-gradient(90deg, var(--bg-3) 25%, var(--bg-4) 50%, var(--bg-3) 75%);
        background-size:200% 100%;
        animation:shimmer 2s ease infinite;
      }

      /* Artikel notatki */
      .note-article { max-width:700px; }
      .na-title   { font-size:1.2rem; font-weight:800; color:var(--text-0); margin-bottom:10px; }
      .na-summary { font-size:0.9rem; color:var(--text-1); line-height:1.7; margin-bottom:16px; padding-bottom:16px; border-bottom:1px solid var(--border-1); }
      .na-simple  { background:var(--accent-light); border:1px solid var(--border-0); border-radius:var(--radius-md); padding:12px 16px; margin-bottom:20px; }
      .na-simple-label { font-size:0.68rem; font-weight:700; text-transform:uppercase; letter-spacing:.07em; color:var(--accent); display:block; margin-bottom:6px; }
      .na-simple p { font-size:0.85rem; color:var(--text-1); line-height:1.6; }
      .na-section { margin-bottom:18px; }
      .na-section-h { font-size:0.95rem; font-weight:700; color:var(--text-0); margin-bottom:6px; }
      .na-section-p { font-size:0.85rem; color:var(--text-1); line-height:1.7; }
      .na-sub-h { font-size:0.88rem; font-weight:700; color:var(--text-0); margin:20px 0 10px; }
      .na-example { background:var(--bg-2); border-radius:var(--radius-md); padding:12px 14px; margin-bottom:10px; }
      .na-example-title { font-size:0.8rem; font-weight:700; color:var(--text-1); margin-bottom:8px; }
      .na-code { background:var(--bg-3); border-radius:var(--radius-sm); padding:10px 12px; overflow-x:auto; font-family:var(--font-mono); font-size:0.8rem; color:var(--text-0); margin:8px 0; }
      .na-example-exp { font-size:0.8rem; color:var(--text-2); margin-top:6px; line-height:1.6; }
      .na-list-section { margin-bottom:12px; }
      .na-list { padding-left:18px; display:flex; flex-direction:column; gap:5px; }
      .na-list li { font-size:0.84rem; color:var(--text-1); line-height:1.6; }
      .na-list--green li::marker { color:#16a34a; }
      .na-list--red   li::marker { color:var(--red); }

      /* Pasek akcji */
      .note-actions {
        padding:12px 20px; border-top:1px solid var(--border-1); flex-shrink:0;
        display:flex; align-items:center; gap:8px; background:#fff;
      }
      .act-right { margin-left:auto; display:flex; gap:8px; }
      .act-btn {
        display:inline-flex; align-items:center; gap:7px;
        padding:9px 16px; border-radius:var(--radius-md); border:1.5px solid var(--border-1);
        font-size:0.8rem; font-weight:600; cursor:pointer;
        transition:all var(--t-fast); white-space:nowrap;
      }
      .act-btn:disabled { opacity:.45; cursor:not-allowed; }
      .act-btn--code  { background:#f0fdf4; color:#16a34a; border-color:rgba(22,163,74,.3); }
      .act-btn--code:hover:not(:disabled)  { background:#dcfce7; }
      .act-btn--ask   { background:var(--accent-light); color:var(--accent); border-color:var(--border-0); }
      .act-btn--ask:hover   { background:var(--accent-glow-strong); }
      .act-btn--quiz  { background:#f5f3ff; color:#7c3aed; border-color:rgba(124,58,237,.25); }
      .act-btn--quiz:hover:not(:disabled)  { background:#ede9fe; }
    `}</style>
  )
}
