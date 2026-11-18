import { useCallback, useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'

const ANIM_MS = 320

// Klasa na <body> na czas otwarcia — dwie role: (1) blokuje przewijanie tła (`overflow:hidden`,
// zob. CSS niżej), (2) wyłącza `pointer-events` na WSZYSTKICH `.resize-handle` głównego układu
// (sidebar, drzewo↔notatka), żeby nie dało się ich chwycić/najechać (kursor col-resize/row-resize)
// spod pełnoekranowej nakładki, mimo że wizualnie są już całkowicie zakryte. Zob. też komentarz przy
// createPortal poniżej — bez portalu samo to i tak by nie wystarczyło.
const BODY_OPEN_CLASS = 'fullscreen-overlay-open'

/**
 * Wspólny układ paneli "drawer" wysuwanych od prawej krawędzi ekranu — półprzezroczyste,
 * przyciemnione tło, panel o stałej szerokości z cieniem i zaokrągloną lewą krawędzią, nagłówek
 * oddzielony od treści, animowane wjeżdżanie/wyjeżdżanie (CSS transition, łagodne
 * przyspieszenie+wyhamowanie). Współdzielone MIĘDZY FiltersPanel i ActualityReviewPanel — logika
 * biznesowa (filtrowanie, pobieranie danych, WebSocket) zostaje w każdym panelu osobno, tu żyje
 * WYŁĄCZNIE układ/animacja i wspólne klasy CSS (pola formularza, przyciski, pusty stan — zob.
 * EmptyState/ikony poniżej), żeby nie dublować stylu między dwoma panelami.
 *
 * Renderowany przez `createPortal` wprost do `document.body`, NIE w miejscu wywołania w drzewie
 * JSX. Oba panele (Filtry, Weryfikacja aktualności) są dziś wywoływane z wnętrza `<aside
 * className="sidebar">` (zob. AppLayout.jsx), które ma WŁASNY z-index (100) i tym samym WŁASNY
 * stacking context — z-index nadany dzieciom sidebara (jak dawne z-index:190 tego panelu) jest
 * porównywany TYLKO wewnątrz tego kontekstu, nie globalnie. Efekt: elementy spoza sidebara, które
 * escapują do stacking contextu ROOT z wyższym z-index niż sam sidebar (w tej appce dowolny
 * `.resize-handle` — z-index:170 na poziomie root, bo ani `.content-wrap`, ani `.main-content` nie
 * mają własnego z-index) potrafiły malować się NAD panelem, mimo pozornie wysokiego z-index samego
 * panelu. Portal do document.body eliminuje ten problem u źródła — panel żyje odtąd w ROOT stacking
 * context, na równi z resztą aplikacji, więc jego z-index (1000, zob. hierarchia w CSS niżej)
 * porównuje się bezpośrednio z KAŻDYM innym elementem strony.
 *
 * Animacja zamknięcia: klik w tło / Escape / krzyżyk NIE wołają `onClose` od razu — najpierw
 * przełączają lokalny stan `closing`, co cofa CSS-owe klasy "--open" (panel z powrotem
 * translateX(100%), tło z powrotem przezroczyste) i DOPIERO po czasie trwania tej animacji
 * (ANIM_MS) wołają `onClose`, które rodzic używa do faktycznego odmontowania panelu
 * (`{open && <SlideOverPanel>}`). Zamknięcia inicjowane samą logiką biznesową (np. wybranie
 * wyniku i przejście do tematu) nadal mogą wołać `onClose` rodzica bezpośrednio — nawigacja sama
 * w sobie jest już wystarczająco widoczną zmianą, więc nie odgrywamy tu dodatkowej animacji.
 */
export default function SlideOverPanel({ title, onClose, children }) {
  const [entered, setEntered] = useState(false)
  const [closing, setClosing] = useState(false)
  const closeTimerRef = useRef(null)

  // Jedna klatka po zamontowaniu włączamy klasę "--open" - zmiana z (translateX 100%/opacity 0)
  // na stan docelowy musi nastąpić PO pierwszym renderze ze stanem początkowym, inaczej
  // przeglądarka nigdy go nie zobaczy i po prostu "przeskoczy" bez animacji.
  useEffect(() => {
    const raf = requestAnimationFrame(() => setEntered(true))
    return () => cancelAnimationFrame(raf)
  }, [])

  useEffect(() => () => { if (closeTimerRef.current) clearTimeout(closeTimerRef.current) }, [])

  // Blokada przewijania tła + wyłączenie uchwytów głównego układu — WYŁĄCZNIE na czas życia tego
  // komponentu (czyli łącznie z trwaniem animacji zamknięcia, bo rodzic odmontowuje panel dopiero
  // PO niej — zob. requestClose). Cleanup usuwa klasę zawsze: po normalnym odmontowaniu (sukces
  // zamknięcia) ORAZ przy jakimkolwiek nietypowym, wcześniejszym odmontowaniu — nigdy nie zostaje
  // "zawieszona" na body.
  useEffect(() => {
    document.body.classList.add(BODY_OPEN_CLASS)
    return () => document.body.classList.remove(BODY_OPEN_CLASS)
  }, [])

  const requestClose = useCallback(() => {
    setClosing(prev => {
      if (prev) return prev
      closeTimerRef.current = setTimeout(onClose, ANIM_MS)
      return true
    })
  }, [onClose])

  useEffect(() => {
    const onKeyDown = (e) => { if (e.key === 'Escape') requestClose() }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [requestClose])

  const isOpen = entered && !closing

  return createPortal(
    <div className={`sop-backdrop${isOpen ? ' sop-open' : ''}`} onClick={requestClose}>
      <div
        className={`sop-panel${isOpen ? ' sop-open' : ''}`}
        onClick={e => e.stopPropagation()}
        role="dialog" aria-modal="true" aria-label={title}
      >
        <header className="sop-head">
          <h2 className="sop-title">{title}</h2>
          <button className="sop-close" onClick={requestClose} title="Zamknij (Esc)" aria-label="Zamknij">
            <IconClose />
          </button>
        </header>
        {children}
      </div>

      <style>{`
        /* Hierarchia z-index pełnoekranowych warstw aplikacji (poza główną gałęzią layoutu, która
           cała mieści się poniżej 500 — sidebar:100, resize-handle:170, topbar:50, tree/note
           overlap:10/20 — zob. odpowiednie pliki): 1000 pełnoekranowy panel (ten), 1100 modal
           potwierdzenia (zob. ConfirmModal.jsx — też portalowany, z tego samego powodu), 1200
           toast (zob. ToastContainer.jsx). Każda warstwa ma jedno miejsce, gdzie jej numer jest
           zdefiniowany — nie ma tu przypadkowych wartości typu 999999999 rozrzuconych po plikach. */
        .sop-backdrop {
          position:fixed; inset:0; z-index:1000;
          background:rgba(15,23,42,0); backdrop-filter:blur(0px);
          display:flex; justify-content:flex-end;
          transition:background 340ms cubic-bezier(0.4,0,0.2,1), backdrop-filter 340ms cubic-bezier(0.4,0,0.2,1);
        }
        .sop-backdrop.sop-open { background:rgba(15,23,42,0.45); backdrop-filter:blur(2px); }

        .sop-panel {
          width:100%; max-width:100vw; height:100%;
          background:var(--bg-1);
          box-shadow:-20px 0 44px rgba(15,23,42,0.16), -3px 0 10px rgba(15,23,42,0.07);
          display:flex; flex-direction:column; overflow:hidden;
          transform:translateX(100%);
          transition:transform 340ms cubic-bezier(0.4,0,0.2,1);
          will-change:transform;
        }
        .sop-panel.sop-open { transform:translateX(0); }

        .sop-head {
          display:flex; align-items:center; justify-content:space-between; gap:12px;
          padding:20px 26px; background:var(--bg-2); border-bottom:1px solid var(--border-1); flex-shrink:0;
        }
        .sop-title { font-size:1.16rem; font-weight:800; color:var(--text-0); letter-spacing:-0.01em; }
        .sop-close {
          flex-shrink:0; width:36px; height:36px; display:flex; align-items:center; justify-content:center;
          background:var(--bg-1); border:1.5px solid var(--border-1); border-radius:50%; color:var(--text-2); cursor:pointer;
          transition:background var(--t-fast), border-color var(--t-fast), color var(--t-fast);
        }
        .sop-close:hover { background:var(--red-light); border-color:rgba(220,38,38,0.28); color:var(--red); }
        .sop-close:active { transform:scale(0.94); }

        /* ── Kryteria — sekcja u góry, wyraźnie oddzielona od listy poniżej ── */
        .sop-criteria { padding:20px 26px; border-bottom:1px solid var(--border-1); flex-shrink:0; display:flex; flex-direction:column; gap:16px; }
        .sop-criteria-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px 16px; }

        .sop-field { display:flex; flex-direction:column; gap:6px; min-width:0; }
        .sop-label { font-size:0.7rem; font-weight:700; color:var(--text-1); letter-spacing:0.03em; }
        .sop-input, .sop-select {
          height:40px; padding:0 12px; border-radius:var(--radius-md);
          border:1.5px solid var(--border-1); background:var(--bg-2);
          font-size:0.85rem; color:var(--text-0); outline:none; min-width:0;
          transition:border-color var(--t-fast), box-shadow var(--t-fast), background var(--t-fast);
        }
        .sop-input:focus, .sop-select:focus { border-color:var(--accent); background:var(--bg-1); box-shadow:0 0 0 3px var(--accent-glow); }
        .sop-select:disabled { opacity:.55; cursor:not-allowed; }

        .sop-actions { display:flex; gap:10px; justify-content:flex-end; }
        .sop-btn {
          height:40px; padding:0 18px; border-radius:var(--radius-md);
          font-size:0.83rem; font-weight:700; cursor:pointer; border:1.5px solid transparent;
          display:inline-flex; align-items:center; justify-content:center; gap:7px; white-space:nowrap;
          transition:background var(--t-fast), border-color var(--t-fast), color var(--t-fast), box-shadow var(--t-fast);
        }
        .sop-btn-primary { background:var(--accent); color:#fff; box-shadow:0 3px 10px rgba(37,99,235,0.28); }
        .sop-btn-primary:hover:not(:disabled) { background:var(--accent-dim); }
        .sop-btn-primary:disabled { opacity:.6; cursor:not-allowed; box-shadow:none; }
        .sop-btn-secondary { background:var(--bg-2); color:var(--text-2); border-color:var(--border-1); }
        .sop-btn-secondary:hover:not(:disabled) { background:var(--bg-1); border-color:var(--border-0); color:var(--text-1); }
        .sop-btn-secondary:disabled { opacity:.55; cursor:not-allowed; }
        .sop-spin { width:14px; height:14px; border:2px solid rgba(255,255,255,.4); border-top-color:#fff; border-radius:50%; animation:sopSpin .65s linear infinite; flex-shrink:0; }
        @keyframes sopSpin { to { transform:rotate(360deg); } }

        /* ── Lista — reszta wysokości panelu, własne przewijanie ── */
        .sop-list { flex:1; min-height:0; overflow-y:auto; padding:18px 26px 26px; display:flex; flex-direction:column; gap:10px; }

        /* ── Pusty stan / błąd — karta z ikoną, wyśrodkowana na dostępnej wysokości ── */
        .sop-empty { margin:auto; max-width:320px; padding:36px 22px; text-align:center; display:flex; flex-direction:column; align-items:center; gap:8px; }
        .sop-empty-icon {
          width:54px; height:54px; border-radius:50%; display:flex; align-items:center; justify-content:center;
          background:var(--bg-3); color:var(--text-2); margin-bottom:6px;
        }
        .sop-empty--success .sop-empty-icon { background:var(--green-light); color:var(--green); }
        .sop-empty--error .sop-empty-icon { background:var(--red-light); color:var(--red); }
        .sop-empty-title { font-size:0.94rem; font-weight:700; color:var(--text-0); }
        .sop-empty-text { font-size:0.8rem; color:var(--text-3); line-height:1.55; }
        .sop-empty-retry {
          margin-top:6px; height:36px; padding:0 16px; background:var(--bg-1); border:1.5px solid var(--border-1);
          border-radius:var(--radius-md); font-size:0.78rem; font-weight:700; color:var(--text-1); cursor:pointer;
          transition:background var(--t-fast), border-color var(--t-fast);
        }
        .sop-empty-retry:hover { background:var(--bg-2); border-color:var(--border-0); }

        /* ── Stan ładowania ── */
        .sop-loading { margin:auto; display:flex; flex-direction:column; align-items:center; gap:12px; color:var(--text-3); font-size:0.85rem; }
        .sop-loading-spin { width:28px; height:28px; border:3px solid var(--border-1); border-top-color:var(--accent); border-radius:50%; animation:sopSpin .8s linear infinite; }

        @media (max-width: 640px) {
          .sop-panel { width:100vw; max-width:100vw; border-radius:0; }
          .sop-head { padding:16px 18px; }
          .sop-criteria { padding:16px 18px; }
          .sop-criteria-grid { grid-template-columns:1fr; }
          .sop-list { padding:14px 18px 20px; }
        }

        /* ── Skutki uboczne otwartego pełnoekranowego panelu na resztę strony (BODY_OPEN_CLASS) ──
           Ten <style> jest wprawdzie renderowany WEWNĄTRZ portalu (a więc fizycznie w document.body),
           ale zwykłe <style> nie ma żadnego "scope" ograniczonego do miejsca w drzewie DOM — jego
           selektory (tu celowo celujące w body i .resize-handle GDZIEKOLWIEK na stronie)
           działają globalnie, dokładnie tak samo jak gdyby ten blok żył w globalnym arkuszu stylów. */
        body.fullscreen-overlay-open { overflow: hidden; }
        /* Uchwyty głównego układu (sidebar ↔ treść, drzewo ↔ notatka) nie mogą być chwytalne ani
           pokazywać kursora col-resize/row-resize spod pełnoekranowej nakładki — mimo że portal już
           samodzielnie rozwiązuje malowanie (który element widać), same uchwyty wciąż fizycznie
           zajmują swoje miejsce w layoucie POD nakładką i bez tego nadal reagowałyby na hover/drag. */
        body.fullscreen-overlay-open .resize-handle { pointer-events: none; }
      `}</style>
    </div>,
    document.body,
  )
}

/** Karta pustego stanu / błędu / sukcesu — ikona w kółku + tytuł + krótki opis + opcjonalna akcja. */
export function EmptyState({ icon, title, text, tone = 'neutral', action }) {
  return (
    <div className={`sop-empty${tone !== 'neutral' ? ` sop-empty--${tone}` : ''}`}>
      <div className="sop-empty-icon">{icon}</div>
      {title && <p className="sop-empty-title">{title}</p>}
      {text && <p className="sop-empty-text">{text}</p>}
      {action}
    </div>
  )
}

export function IconClose() {
  return (
    <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
      <path d="M2 2l11 11M13 2L2 13" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  )
}

export function IconSearch() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
      <circle cx="10.5" cy="10.5" r="6.5" stroke="currentColor" strokeWidth="1.8" />
      <path d="M15.5 15.5L20 20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

export function IconAlert() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
      <path d="M12 3.5L21.5 20h-19L12 3.5z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
      <path d="M12 9.5v5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      <circle cx="12" cy="17.2" r="1" fill="currentColor" />
    </svg>
  )
}

export function IconCheckCircle() {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="12" r="9.5" stroke="currentColor" strokeWidth="1.8" />
      <path d="M7.5 12.5l2.8 2.8L16.5 9" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
