import { useCallback, useEffect, useRef, useState } from 'react'
import SlideOverPanel, { EmptyState, IconAlert } from './SlideOverPanel'
import { appConfigApi } from '@/api/services'
import {
  readEmergencyFile, revokeBlobUrls, formatBytes, RecoveryError,
  MAX_KFBUNDLE_INPUT_BYTES,
} from '@/utils/emergencyRecovery'

const IconUpload = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
    <path d="M7 9.5V1.5M7 1.5L4 4.5M7 1.5l3 3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M1.5 9.5v2a1 1 0 0 0 1 1h9a1 1 0 0 0 1-1v-2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
)

const IconFile = () => (
  <svg width="34" height="34" viewBox="0 0 24 24" fill="none">
    <path d="M6 2.5h8l4 4V20a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 4 20V4A1.5 1.5 0 0 1 5.5 2.5H6Z" stroke="currentColor" strokeWidth="1.6"/>
    <path d="M14 2.5V7a1 1 0 0 0 1 1h4.5" stroke="currentColor" strokeWidth="1.6"/>
  </svg>
)

function buildIssuesReportNoteData(issues, totalCount) {
  const listItems = issues.map(i => ({
    type: 'listItem',
    content: [{ type: 'paragraph', content: [{ type: 'text', text: `${i.title || i.fileName || '(nieznany dokument)'} — ${i.reason}` }] }],
  }))
  return {
    title: 'Raport importu — pominięte dokumenty',
    categoryPath: [],
    author: null,
    createdAt: null,
    updatedAt: null,
    lastVerificationOfActualityDate: null,
    contentJson: {
      type: 'doc',
      content: [
        {
          type: 'paragraph',
          content: [{
            type: 'text',
            text: `Nie udało się odczytać ${issues.length} z ${totalCount} dokumentów znalezionych w tym archiwum. `
              + 'Poniżej lista pominiętych wpisów wraz z przyczyną — reszta dokumentu zawiera wyłącznie poprawnie odczytane wpisy.',
          }],
        },
        { type: 'bulletList', content: listItems },
      ],
    },
  }
}

/**
 * Panel "Wczytaj dane binarne" — awaryjny, LOKALNY odczyt pojedynczego `.kfdoc` albo zbiorczego
 * `.kfbundle`, niezależny od backendu/bazy (zob. dokumentacja/BACKUP_STRATEGY.txt, poziom 4).
 * Żaden wybrany plik nie opuszcza przeglądarki — cała praca (rozpakowanie ZIP, odczyt manifestu,
 * odtworzenie treści i obrazów, wygenerowanie PDF) dzieje się w emergencyRecovery.js i
 * pdfGenerator.js, bez jednego wywołania do API.
 */
export default function EmergencyRecoveryPanel() {
  // Widoczność przycisku sterowana z backendu (config.properties: ui.visible-binary-loader,
  // domyślnie true) — zob. AppConfigServlet. Startujemy optymistycznie jako widoczny (zgodnie
  // z domyślną wartością konfiguracji), żeby nie migać przyciskiem w typowym przypadku; jeśli
  // zapytanie się nie powiedzie, zostajemy przy tym samym, bezpiecznym domyślnym zachowaniu.
  const [visible, setVisible] = useState(true)
  useEffect(() => {
    let cancelled = false
    appConfigApi.get()
      .then(res => { if (!cancelled) setVisible(res.data?.visibleBinaryLoader !== false) })
      .catch(() => {}) // brak połączenia z API -> zostajemy przy domyślnym `true`
    return () => { cancelled = true }
  }, [])

  const [open, setOpen] = useState(false)
  const [stage, setStage] = useState('idle') // idle | parsing | ready | generating | done | error
  const [fileName, setFileName] = useState(null)
  const [parsed, setParsed] = useState(null) // { kind, noteData?, notesData?, issues?, blobUrls }
  const [error, setError] = useState(null)
  const [resultMessage, setResultMessage] = useState(null)
  const [fallbackUrl, setFallbackUrl] = useState(null)
  const [fallbackName, setFallbackName] = useState(null)
  const [dragOver, setDragOver] = useState(false)
  const inputRef = useRef(null)

  const reset = () => {
    if (parsed?.blobUrls) revokeBlobUrls(parsed.blobUrls)
    setStage('idle'); setFileName(null); setParsed(null); setError(null)
    setResultMessage(null); setFallbackUrl(null); setFallbackName(null)
  }

  const handleFile = useCallback(async (file) => {
    if (!file) return
    reset()
    setFileName(file.name)
    setStage('parsing')
    try {
      const result = await readEmergencyFile(file)
      setParsed(result)
      setStage('ready')
    } catch (e) {
      setError(e instanceof RecoveryError ? e.message : 'Nie udało się odczytać pliku — sprawdź, czy to poprawny plik .kfdoc albo .kfbundle.')
      setStage('error')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const onInputChange = (e) => {
    const file = e.target.files?.[0]
    handleFile(file)
    e.target.value = '' // pozwala wybrać ten sam plik ponownie
  }

  const onDrop = (e) => {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files?.[0]
    handleFile(file)
  }

  const generateAndOpen = async () => {
    if (!parsed) return
    setStage('generating')
    setError(null)
    try {
      const { generateSingleNotePdf, generateBulkNotesPdf, singleNoteFilename, bulkNotesFilename } = await import('@/utils/pdfGenerator')

      let blob, filename
      if (parsed.kind === 'kfdoc') {
        const res = await generateSingleNotePdf(parsed.noteData, { output: 'blob' })
        blob = res.blob
        filename = singleNoteFilename(parsed.noteData.title)
      } else {
        const notes = parsed.issues?.length
          ? [buildIssuesReportNoteData(parsed.issues, parsed.notesData.length + parsed.issues.length), ...parsed.notesData]
          : parsed.notesData
        if (notes.length === 0) {
          setError('Ta kopia zapasowa nie zawiera żadnego odczytanego dokumentu.')
          setStage('error')
          return
        }
        const res = await generateBulkNotesPdf(notes, { output: 'blob' })
        blob = res.blob
        filename = bulkNotesFilename(parsed.notesData.length)
      }

      // Assety (obrazy) nie są już potrzebne — PDF ma je "wypieczone" jako bajty; zwalniamy Blob
      // URL wejściowe, ale NIE blob samego PDF-a (nowa karta go jeszcze potrzebuje).
      revokeBlobUrls(parsed.blobUrls)
      const pdfUrl = URL.createObjectURL(blob)
      const newTab = window.open(pdfUrl, '_blank')
      if (!newTab) {
        // Przeglądarka zablokowała nową kartę — fallback: przycisk pobrania zamiast automatycznego podglądu.
        setFallbackUrl(pdfUrl)
        setFallbackName(filename)
        setResultMessage('Przeglądarka zablokowała otwarcie nowej karty. Pobierz wygenerowany PDF poniżej.')
      } else {
        setResultMessage('PDF otwarty w nowej karcie — możesz go tam przejrzeć, wydrukować albo zapisać.')
      }
      setStage('done')
    } catch (e) {
      setError(e?.message || 'Nie udało się wygenerować PDF.')
      setStage('error')
    }
  }

  const handleClose = () => {
    reset()
    setOpen(false)
  }

  // Po WSZYSTKICH hookach (zasady hooków) — ukryty przycisk to po prostu brak renderowania
  // czegokolwiek, nie samo `display:none`, żeby nie zostawiać w DOM nieużywanego triggera.
  if (!visible) return null

  return (
    <>
      <button className="erp-trigger" onClick={() => setOpen(true)}>
        <IconUpload /> <span>Wczytaj dane binarne</span>
      </button>

      {open && (
        <SlideOverPanel title="Wczytaj dane binarne" onClose={handleClose}>
          <div className="erp-body">
            <p className="erp-intro">
              Awaryjny podgląd wcześniej zserializowanych danych. Działa całkowicie lokalnie w
              przeglądarce — plik NIE jest wysyłany na serwer ani zapisywany w bazie, więc działa
              nawet przy pustej lub uszkodzonej bazie danych.
            </p>

            {stage !== 'done' && (
              <div
                className={`erp-drop${dragOver ? ' erp-drop--over' : ''}`}
                onDragOver={e => { e.preventDefault(); setDragOver(true) }}
                onDragLeave={() => setDragOver(false)}
                onDrop={onDrop}
              >
                <IconFile />
                <p className="erp-drop-title">Przeciągnij plik tutaj</p>
                <p className="erp-drop-sub">albo</p>
                <button type="button" className="sop-btn sop-btn-secondary" onClick={() => inputRef.current?.click()}>
                  Wybierz plik
                </button>
                <input
                  ref={inputRef} type="file" accept=".kfdoc,.kfbundle" className="erp-input"
                  onChange={onInputChange}
                />
                <p className="erp-drop-formats">
                  Obsługiwane formaty: .kfdoc, .kfbundle — maksymalny rozmiar {formatBytes(MAX_KFBUNDLE_INPUT_BYTES)}.
                </p>
              </div>
            )}

            {fileName && stage !== 'idle' && (
              <div className="erp-file-row">
                <IconFile />
                <span className="erp-file-name">{fileName}</span>
              </div>
            )}

            {stage === 'parsing' && (
              <div className="sop-loading"><span className="sop-loading-spin" /> Odczytywanie pliku…</div>
            )}

            {stage === 'error' && (
              <EmptyState icon={<IconAlert />} tone="error" title="Nie udało się odczytać pliku" text={error} />
            )}

            {stage === 'ready' && parsed?.kind === 'kfdoc' && (
              <div className="erp-summary">
                <p><b>Rozpoznano pojedynczą notatkę:</b> „{parsed.noteData.title}”</p>
                <button type="button" className="sop-btn sop-btn-primary" onClick={generateAndOpen}>
                  Otwórz podgląd PDF
                </button>
              </div>
            )}

            {stage === 'ready' && parsed?.kind === 'kfbundle' && (
              <div className="erp-summary">
                <p>
                  <b>Rozpoznano zbiorczy backup:</b> {parsed.notesData.length} poprawny(ch) dokument(ów)
                  {parsed.issues?.length > 0 && <>, <span className="erp-issue-count">{parsed.issues.length} uszkodzony(ch)/pominiętych</span></>}.
                </p>
                {parsed.issues?.length > 0 && (
                  <div className="erp-issues">
                    <p className="erp-issues-title">Problemy z częścią dokumentów:</p>
                    <ul className="erp-issues-list">
                      {parsed.issues.map((i, idx) => (
                        <li key={idx}><b>{i.title || i.fileName || '(nieznany)'}</b> — {i.reason}</li>
                      ))}
                    </ul>
                    <p className="erp-issues-note">
                      Poprawne dokumenty nadal można wygenerować jako PDF — lista powyższa trafi do
                      niego jako osobna strona raportu.
                    </p>
                  </div>
                )}
                {parsed.notesData.length === 0 ? (
                  <p className="erp-issues-note">
                    Żadnego dokumentu z tego archiwum nie udało się odczytać — nie ma z czego wygenerować PDF.
                  </p>
                ) : (
                  <div className="erp-actions-row">
                    <button type="button" className="sop-btn sop-btn-secondary" onClick={handleClose}>Anuluj</button>
                    <button type="button" className="sop-btn sop-btn-primary" onClick={generateAndOpen}>
                      {parsed.issues?.length > 0 ? 'Wygeneruj mimo to' : 'Otwórz podgląd PDF'}
                    </button>
                  </div>
                )}
              </div>
            )}

            {stage === 'generating' && (
              <div className="sop-loading"><span className="sop-loading-spin" /> Generowanie PDF…</div>
            )}

            {stage === 'done' && (
              <div className="erp-summary">
                <EmptyState icon={<IconFile />} tone="success" title="Gotowe" text={resultMessage} />
                {fallbackUrl && (
                  <a href={fallbackUrl} download={fallbackName} className="sop-btn sop-btn-primary erp-download-link">
                    Pobierz PDF
                  </a>
                )}
                <button type="button" className="sop-btn sop-btn-secondary" onClick={reset}>Wczytaj kolejny plik</button>
              </div>
            )}
          </div>

          <style>{`
            .erp-body { padding: 20px 26px 26px; overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 18px; }
            .erp-intro { font-size: 0.85rem; color: var(--text-2); line-height: 1.6; max-width: 640px; }

            .erp-drop {
              border: 2px dashed var(--border-1); border-radius: var(--radius-lg);
              padding: 36px 24px; display: flex; flex-direction: column; align-items: center; gap: 8px;
              color: var(--text-2); transition: border-color var(--t-fast), background var(--t-fast);
            }
            .erp-drop--over { border-color: var(--accent); background: var(--accent-light); }
            .erp-drop-title { font-size: 0.92rem; font-weight: 700; color: var(--text-0); margin-top: 4px; }
            .erp-drop-sub { font-size: 0.76rem; color: var(--text-3); }
            .erp-drop-formats { font-size: 0.72rem; color: var(--text-3); margin-top: 10px; }
            .erp-input { display: none; }

            .erp-file-row { display: flex; align-items: center; gap: 10px; color: var(--text-1); font-size: 0.85rem; font-weight: 600; }

            .erp-summary { display: flex; flex-direction: column; gap: 14px; max-width: 640px; }
            .erp-summary p { font-size: 0.86rem; color: var(--text-1); line-height: 1.6; }
            .erp-issue-count { color: #d97706; font-weight: 700; }

            .erp-issues { background: #fffbeb; border: 1px solid rgba(217,119,6,.25); border-radius: var(--radius-md); padding: 14px 16px; }
            .erp-issues-title { font-weight: 700; color: #92400e; margin-bottom: 6px; }
            .erp-issues-list { margin: 0 0 8px; padding-left: 18px; font-size: 0.8rem; color: #92400e; line-height: 1.7; max-height: 220px; overflow-y: auto; }
            .erp-issues-note { font-size: 0.76rem; color: #92400e; opacity: .85; }

            .erp-actions-row { display: flex; gap: 10px; }
            .erp-download-link { text-decoration: none; text-align: center; }
          `}</style>
        </SlideOverPanel>
      )}

      <style>{`
        .erp-trigger {
          display:flex; align-items:center; gap:6px; width:calc(100% - 4px);
          margin:6px 2px 0; padding:7px 9px;
          border:1.5px solid var(--border-1); border-radius:var(--radius-md);
          background:var(--bg-2); color:var(--text-2);
          font-size:0.78rem; font-weight:600; cursor:pointer;
          transition:background var(--t-fast), border-color var(--t-fast), color var(--t-fast);
        }
        .erp-trigger:hover { background:#fff; border-color:var(--border-0); color:var(--accent); }
      `}</style>
    </>
  )
}
