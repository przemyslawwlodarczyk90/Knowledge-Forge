/** Wspólne style okien quizu — wyłącznie zmienne motywu, więc działają w trybie jasnym i ciemnym. */
export function QuizDialogStyles() {
  return (
    <style>{`
      .quiz-dialog {
        width:min(100% - 24px, 720px); max-height:calc(100% - 24px); margin:auto; padding:0;
        border:1px solid var(--border-1); border-radius:var(--radius-xl);
        color:var(--text-0); background:var(--bg-1); box-shadow:var(--shadow-lg); overflow:auto;
      }
      .quiz-dialog--narrow { width:min(100% - 24px, 500px); }
      .quiz-dialog::backdrop { background:rgba(15,23,42,.5); backdrop-filter:blur(4px); }
      .quiz-dialog[open] { animation:quiz-pop 160ms ease; }
      @keyframes quiz-pop { from { opacity:0; transform:translateY(6px) scale(.985) } to { opacity:1; transform:none } }

      .quiz-form, .quiz-body { display:flex; flex-direction:column; gap:14px; padding:24px; }
      .quiz-dialog-head { display:flex; align-items:center; gap:6px; }
      .quiz-dialog-head h2 { font-size:1.12rem; font-weight:700; margin:0; }
      .quiz-dialog-head .quiz-close { margin-left:auto; }
      .quiz-sub { margin:-8px 0 0; color:var(--text-2); font-size:.86rem; overflow-wrap:anywhere; }
      .quiz-form label { font-size:.84rem; font-weight:700; color:var(--text-1); }
      .quiz-optional { font-weight:400; color:var(--text-3); }
      .quiz-form textarea {
        padding:10px 12px; border:1px solid var(--border-1); border-radius:var(--radius-md);
        background:var(--bg-2); color:inherit; font:inherit; resize:vertical; margin-top:-6px;
      }
      .quiz-form textarea:focus { outline:none; border-color:var(--accent); box-shadow:0 0 0 3px var(--accent-glow); }
      .quiz-pool-note {
        margin:0; padding:10px 12px; border-radius:var(--radius-md); font-size:.84rem;
        background:var(--accent-light); color:var(--text-1);
      }
      .quiz-error { margin:0; color:var(--red); font-size:.86rem; }

      .quiz-progress { display:flex; flex-direction:column; gap:6px; font-size:.84rem; color:var(--text-1); }
      .quiz-progress small { color:var(--text-3); }
      .quiz-progress-bar { height:6px; border-radius:99px; background:var(--bg-3); overflow:hidden; }
      .quiz-progress-bar span {
        display:block; height:100%; border-radius:inherit; transition:width 1s linear;
        background:linear-gradient(90deg, var(--accent), var(--cyan));
      }
      .quiz-progress-row { display:flex; justify-content:space-between; gap:8px; }
      .quiz-progress-time { font-family:var(--font-mono); color:var(--text-3); }

      .quiz-footer { display:flex; flex-wrap:wrap; justify-content:flex-end; gap:8px; margin-top:4px; }
      .quiz-btn {
        display:inline-flex; align-items:center; justify-content:center; gap:7px; min-height:36px;
        padding:0 16px; border:1px solid var(--border-1); border-radius:var(--radius-md);
        background:var(--bg-2); color:var(--text-1); font:inherit; font-size:.86rem; font-weight:600;
        cursor:pointer; transition:background var(--t-fast), border-color var(--t-fast), transform var(--t-fast);
      }
      .quiz-btn:hover:not(:disabled) { background:var(--bg-3); }
      .quiz-btn:active:not(:disabled) { transform:translateY(1px); }
      .quiz-btn:disabled { opacity:.55; cursor:not-allowed; }
      .quiz-btn:focus-visible { outline:2px solid var(--accent); outline-offset:2px; }
      .quiz-btn--primary { background:var(--accent); border-color:var(--accent); color:#fff; }
      .quiz-btn--primary:hover:not(:disabled) { background:var(--accent-dim); }
      .quiz-btn--ghost { background:transparent; }
      .quiz-btn--danger { color:var(--red); background:transparent; border-color:transparent; padding:0 8px; }
      .quiz-btn--danger:hover:not(:disabled) { background:var(--red-light); }
      .quiz-close {
        width:32px; height:32px; padding:0; border:none; border-radius:var(--radius-md);
        background:transparent; color:var(--text-2); font-size:1.1rem; cursor:pointer;
      }
      .quiz-close:hover { background:var(--bg-3); color:var(--text-0); }
      .quiz-spin {
        width:13px; height:13px; border-radius:50%; border:2px solid rgba(255,255,255,.4);
        border-top-color:#fff; animation:quiz-rot .7s linear infinite;
      }
      @keyframes quiz-rot { to { transform:rotate(360deg) } }

      .quiz-chip {
        display:inline-flex; align-items:center; gap:5px; padding:2px 9px; border-radius:99px;
        font-size:.72rem; font-weight:700; letter-spacing:.02em; text-transform:uppercase;
      }
      .quiz-chip[data-level="EASY"]   { background:var(--green-light);  color:var(--green); }
      .quiz-chip[data-level="MEDIUM"] { background:var(--yellow-light); color:var(--yellow); }
      .quiz-chip[data-level="HARD"]   { background:var(--red-light);    color:var(--red); }
      .quiz-inline-code {
        padding:1px 5px; border-radius:4px; background:var(--bg-3);
        font-family:var(--font-mono); font-size:.88em;
      }
      @media (max-width:560px) {
        .quiz-form, .quiz-body { padding:18px 16px; }
        .quiz-footer .quiz-btn { flex:1 1 auto; }
      }
      @media (prefers-reduced-motion: reduce) {
        .quiz-dialog[open], .quiz-progress-bar span { animation:none; transition:none; }
      }
    `}</style>
  )
}

/** Tekst z `kodem w backtickach` — fragmenty kodu w treści pytań i odpowiedzi. */
export function RichText({ text }) {
  if (!text) return null
  const parts = String(text).split(/(`[^`\n]+`)/g)
  return parts.map((part, i) =>
    part.length > 2 && part.startsWith('`') && part.endsWith('`')
      ? <code key={i} className="quiz-inline-code">{part.slice(1, -1)}</code>
      : <span key={i}>{part}</span>)
}
