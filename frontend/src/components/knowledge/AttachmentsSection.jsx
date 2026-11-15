import { useState, useEffect, useCallback, useRef } from 'react'
import { attachmentsApi } from '@/api/services'
import { toast } from '@/store'
import { useApplicationEvents, useReconnectResync } from '@/hooks'
import { getClientId } from '@/ws/clientId'
import ConfirmModal from './modals/ConfirmModal'

const LOG = '[AttachmentsSection]'

/* ── Ikony ── */
const IconChevron = ({ open }) => (
  <svg width="11" height="11" viewBox="0 0 11 11" fill="none" style={{ transform: open ? 'rotate(90deg)' : 'none', transition: 'transform var(--t-fast)' }}>
    <path d="M3.5 2L7 5.5 3.5 9" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const IconVideo = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
    <rect x="1.5" y="3" width="9.5" height="10" rx="1.5" stroke="currentColor" strokeWidth="1.4"/>
    <path d="M11 6.5l3.2-2v7l-3.2-2z" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round"/>
  </svg>
)
const IconSheet = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
    <rect x="2" y="1.5" width="12" height="13" rx="1.5" stroke="currentColor" strokeWidth="1.4"/>
    <path d="M2 6h12M6 6v8.5" stroke="currentColor" strokeWidth="1.2"/>
    <path d="M4 8.5h1.5M4 11h1.5" stroke="currentColor" strokeWidth="1.1" strokeLinecap="round"/>
  </svg>
)
const IconDoc = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
    <path d="M3 1.5h6.5L13 5v9.5a1 1 0 01-1 1H3a1 1 0 01-1-1v-12a1 1 0 011-1z" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round"/>
    <path d="M9.5 1.5V5H13" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round"/>
    <path d="M4.5 8h6M4.5 10.5h6M4.5 13h3.5" stroke="currentColor" strokeWidth="1.1" strokeLinecap="round"/>
  </svg>
)
const IconArchive = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
    <rect x="1.5" y="3" width="13" height="11" rx="1.3" stroke="currentColor" strokeWidth="1.4"/>
    <path d="M1.5 6.2h13" stroke="currentColor" strokeWidth="1.3"/>
    <path d="M7 8.2h2" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
  </svg>
)
const IconClip = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
    <path d="M11 4.5L5.3 10.2a2.3 2.3 0 003.3 3.3l6-6a3.6 3.6 0 00-5.1-5.1l-6 6a5 5 0 007 7l5.2-5.2"
      stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const IconUpload = () => (
  <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
    <path d="M6.5 9.5V2M3.5 5l3-3 3 3M2 11h9" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const IconDownload = () => (
  <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
    <path d="M6.5 1.5v7M3.5 6l3 3 3-3M2 11h9" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const IconTrash = () => (
  <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
    <path d="M2.5 3.5h8M5 3.5V2h3v1.5M3.5 3.5l.6 8a1 1 0 001 .9h3.8a1 1 0 001-.9l.6-8" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const IconPlay = () => (
  <svg width="13" height="13" viewBox="0 0 13 13" fill="none">
    <path d="M4 2.5l7 4-7 4v-8z" fill="currentColor"/>
  </svg>
)

const SHEET_EXT = new Set(['xlsx', 'xls', 'csv'])
const DOC_EXT   = new Set(['docx', 'doc', 'pdf', 'txt'])
const ARCHIVE_EXT = new Set(['zip', 'rar', '7z', 'tar', 'gz'])

function extensionOf(name) {
  const dot = (name || '').lastIndexOf('.')
  return dot >= 0 ? name.slice(dot + 1).toLowerCase() : ''
}

function iconAndLabelFor(attachment) {
  if (attachment.attachmentType === 'VIDEO') return { Icon: IconVideo, label: 'Nagranie spotkania' }
  const ext = extensionOf(attachment.originalName)
  if (SHEET_EXT.has(ext)) return { Icon: IconSheet, label: 'Arkusz' }
  if (DOC_EXT.has(ext)) return { Icon: IconDoc, label: 'Dokument' }
  if (ARCHIVE_EXT.has(ext)) return { Icon: IconArchive, label: 'Archiwum' }
  return { Icon: IconClip, label: ext ? ext.toUpperCase() : 'Plik' }
}

/** np. 1536000 -> "1,5 MB" (separator dziesiętny wg pl-PL). */
function formatBytes(bytes) {
  if (bytes == null) return '—'
  if (bytes < 1024) return `${bytes} B`
  const units = ['KB', 'MB', 'GB']
  let value = bytes / 1024
  let unitIndex = 0
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex++
  }
  const formatted = value.toLocaleString('pl-PL', { maximumFractionDigits: 1, minimumFractionDigits: value < 10 ? 1 : 0 })
  return `${formatted} ${units[unitIndex]}`
}

function formatDate(iso) {
  if (!iso) return null
  try {
    return new Date(iso).toLocaleString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return null
  }
}

/* ── Jeden wiersz listy załączników ── */
function AttachmentRow({ attachment, onDeleteRequest }) {
  const { Icon } = iconAndLabelFor(attachment)
  const isVideo = attachment.attachmentType === 'VIDEO'

  return (
    <div className="att-row">
      <span className="att-row-icon"><Icon /></span>

      <div className="att-row-main">
        {isVideo ? (
          <a
            className="att-row-name att-row-name--link"
            href={attachmentsApi.viewUrl(attachment.id)}
            target="_blank"
            rel="noopener noreferrer"
            title={attachment.originalName}
          >
            <IconPlay /> <span>{attachment.originalName}</span>
          </a>
        ) : (
          <a
            className="att-row-name"
            href={attachmentsApi.downloadUrl(attachment.id)}
            title={attachment.originalName}
          >
            {attachment.originalName}
          </a>
        )}
        {attachment.description && <span className="att-row-desc">{attachment.description}</span>}
        <div className="att-row-meta">
          <span>{formatBytes(attachment.sizeBytes)}</span>
          <span>{formatDate(attachment.createdAt)}</span>
        </div>
      </div>

      <div className="att-row-actions">
        <a
          className="att-row-btn"
          href={attachmentsApi.downloadUrl(attachment.id)}
          title="Pobierz"
        >
          <IconDownload />
        </a>
        <button
          type="button"
          className="att-row-btn att-row-btn--danger"
          title="Usuń"
          onClick={() => onDeleteRequest(attachment)}
        >
          <IconTrash />
        </button>
      </div>
    </div>
  )
}

/**
 * Sekcja załączników w panelu notatki — widoczna dla wybranego tematu, zwijalna.
 * Nagranie Teams (MP4) otwiera się w nowej karcie (natywny odtwaracz wideo przeglądarki,
 * Content-Type: video/mp4 + Content-Disposition: inline z backendu). Każdy inny plik —
 * wyłącznie pobieranie, bez podglądu.
 */
export default function AttachmentsSection({ topicId }) {
  const [expanded, setExpanded] = useState(false)
  const [attachments, setAttachments] = useState([])
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState(null)
  const [description, setDescription] = useState('')
  const [pendingDelete, setPendingDelete] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const fileInputRef = useRef(null)

  const load = useCallback(() => {
    if (!topicId) return
    setLoading(true)
    attachmentsApi.listByTopic(topicId)
      .then(res => setAttachments(res.data))
      .catch(err => {
        console.error(LOG, 'failed to load attachments', err)
        toast.error('Błąd ładowania załączników')
      })
      .finally(() => setLoading(false))
  }, [topicId])

  useEffect(() => {
    setAttachments([])
    setUploadError(null)
    // Sekcja ma zawsze startować zwinięta dla nowo otwartego rekordu — komponent nie odmontowuje
    // się przy zmianie topicId (żyje w NotePanel), więc trzeba resetować stan jawnie, a nie tylko
    // w useState().
    setExpanded(false)
    load()
  }, [topicId, load])

  /* ── WebSocket — odśwież listę TYLKO gdy zdarzenie dotyczy aktualnie otwartego tematu ──
     Własne zdarzenia (ta karta) pomijamy — upload/delete i tak od razu wołają load() lokalnie. */
  useApplicationEvents(useCallback((event) => {
    if (!topicId) return
    const isOwn = event.sourceClientId != null && event.sourceClientId === getClientId()
    if (isOwn) return
    if (event.type === 'ATTACHMENT_CREATED' && event.payload?.topicId === topicId) load()
    if (event.type === 'ATTACHMENT_DELETED' && event.payload?.topicId === topicId) load()
  }, [topicId, load]))

  /* Po odzyskaniu połączenia mogliśmy przegapić zdarzenia — pobierz listę na nowo. */
  useReconnectResync(load)

  const handleUpload = async () => {
    const file = fileInputRef.current?.files?.[0]
    if (!file) {
      setUploadError('Wybierz plik przed dodaniem załącznika')
      return
    }
    setUploadError(null)
    setUploading(true)
    const formData = new FormData()
    formData.append('file', file)
    if (description.trim()) formData.append('description', description.trim())

    console.debug(LOG, 'upload: starting', { name: file.name, size: file.size })
    try {
      await attachmentsApi.upload(topicId, formData)
      toast.success('Załącznik dodany')
      if (fileInputRef.current) fileInputRef.current.value = ''
      setDescription('')
      load()
    } catch (err) {
      console.error(LOG, 'upload failed', err)
      setUploadError(err.message ?? 'Błąd przesyłania pliku')
    } finally {
      setUploading(false)
    }
  }

  const confirmDelete = async () => {
    if (!pendingDelete) return
    setDeleting(true)
    try {
      await attachmentsApi.remove(pendingDelete.id)
      toast.success('Załącznik usunięty')
      setPendingDelete(null)
      load()
    } catch (err) {
      console.error(LOG, 'delete failed', err)
      toast.error(err.message ?? 'Błąd usuwania załącznika')
    } finally {
      setDeleting(false)
    }
  }

  if (!topicId) return null

  return (
    <div className="att-section">
      <button type="button" className="att-head" onClick={() => setExpanded(e => !e)}>
        <IconChevron open={expanded} />
        <span className="att-head-title">Załączniki ({attachments.length})</span>
      </button>

      {expanded && (
        <div className="att-body">
          <div className="att-upload-row">
            <input ref={fileInputRef} type="file" className="att-file-input" disabled={uploading} />
            <input
              type="text"
              className="att-desc-input"
              placeholder="Opis (opcjonalnie)"
              value={description}
              onChange={e => setDescription(e.target.value)}
              disabled={uploading}
              maxLength={500}
            />
            <button type="button" className="att-upload-btn" onClick={handleUpload} disabled={uploading}>
              {uploading ? <span className="att-spin" /> : <IconUpload />}
              {uploading ? 'Wysyłanie…' : 'Dodaj załącznik'}
            </button>
          </div>
          {uploadError && <p className="att-error">{uploadError}</p>}

          <div className="att-list">
            {loading && <p className="att-status">Ładowanie…</p>}
            {!loading && attachments.length === 0 && (
              <p className="att-status att-status--empty">Brak załączników dla tego tematu.</p>
            )}
            {!loading && attachments.map(a => (
              <AttachmentRow key={a.id} attachment={a} onDeleteRequest={setPendingDelete} />
            ))}
          </div>
        </div>
      )}

      {pendingDelete && (
        <ConfirmModal
          title="Usunąć załącznik?"
          message={`„${pendingDelete.originalName}” zostanie trwale usunięty — razem z plikiem na dysku.`}
          confirmLabel="Usuń"
          loading={deleting}
          onConfirm={confirmDelete}
          onClose={() => !deleting && setPendingDelete(null)}
        />
      )}

      <style>{`
        .att-section {
          border: 1px solid var(--border-1); border-radius: var(--radius-md);
          margin: 0 24px 14px; background: var(--bg-2); overflow: hidden;
          flex-shrink: 0;
        }
        .att-head {
          display: flex; align-items: center; gap: 7px; width: 100%;
          padding: 10px 14px; background: none; border: none; cursor: pointer;
          color: var(--text-1); text-align: left;
        }
        .att-head-title { font-size: 0.79rem; font-weight: 700; }
        .att-head:hover { color: var(--accent); }

        .att-body { padding: 0 14px 14px; border-top: 1px solid var(--border-1); }

        .att-upload-row {
          display: flex; gap: 8px; flex-wrap: wrap; align-items: center;
          margin-top: 12px;
        }
        .att-file-input { flex: 1 1 200px; min-width: 0; font-size: 0.76rem; color: var(--text-2); }
        .att-desc-input {
          flex: 1 1 200px; min-width: 0; padding: 7px 10px;
          border: 1.5px solid var(--border-1); border-radius: var(--radius-md);
          background: #fff; font-size: 0.78rem; color: var(--text-0); outline: none;
          transition: border-color var(--t-fast);
        }
        .att-desc-input:focus { border-color: var(--accent); }
        .att-desc-input:disabled, .att-file-input:disabled { opacity: .6; cursor: not-allowed; }

        .att-upload-btn {
          display: flex; align-items: center; gap: 6px; flex-shrink: 0;
          padding: 8px 14px; background: var(--accent); color: #fff; border: none;
          border-radius: var(--radius-md); font-size: 0.78rem; font-weight: 700;
          cursor: pointer; transition: background var(--t-fast); white-space: nowrap;
        }
        .att-upload-btn:hover:not(:disabled) { background: var(--accent-dim); }
        .att-upload-btn:disabled { opacity: .6; cursor: not-allowed; }
        .att-spin { width: 12px; height: 12px; border-radius: 50%; border: 2px solid rgba(255,255,255,.4); border-top-color: #fff; animation: spin .6s linear infinite; }

        .att-error {
          margin-top: 8px; font-size: 0.75rem; color: var(--red);
          background: var(--red-light); border: 1px solid rgba(220,38,38,.2);
          border-radius: var(--radius-sm); padding: 6px 10px;
        }

        .att-list {
          margin-top: 12px; max-height: 320px; overflow-y: auto;
          display: flex; flex-direction: column; gap: 6px;
        }
        .att-status { font-size: 0.78rem; color: var(--text-3); padding: 8px 2px; }
        .att-status--empty { font-style: italic; }

        .att-row {
          display: flex; align-items: flex-start; gap: 10px;
          padding: 9px 10px; background: #fff; border: 1px solid var(--border-1);
          border-radius: var(--radius-md); min-width: 0;
        }
        .att-row-icon { color: var(--text-3); flex-shrink: 0; margin-top: 1px; }
        .att-row-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
        .att-row-name {
          font-size: 0.81rem; font-weight: 600; color: var(--text-0);
          overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%;
          text-decoration: none; cursor: pointer;
        }
        .att-row-name:hover { color: var(--accent); text-decoration: underline; }
        .att-row-name--link { display: inline-flex; align-items: center; gap: 5px; color: var(--accent); }
        .att-row-name--link span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .att-row-desc {
          font-size: 0.72rem; color: var(--text-2); font-style: italic;
          overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
        }
        .att-row-meta {
          display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
          font-size: 0.68rem; color: var(--text-3); margin-top: 1px;
        }
        .att-row-actions { display: flex; gap: 4px; flex-shrink: 0; }
        .att-row-btn {
          width: 26px; height: 26px; display: flex; align-items: center; justify-content: center;
          border-radius: 6px; border: 1px solid var(--border-1); background: #fff;
          color: var(--text-2); cursor: pointer; text-decoration: none;
          transition: background var(--t-fast), color var(--t-fast);
        }
        .att-row-btn:hover { background: var(--bg-2); color: var(--text-0); }
        .att-row-btn--danger:hover { background: var(--red-light); color: var(--red); border-color: rgba(220,38,38,.25); }

        @media (max-width: 860px) {
          .att-section { margin: 0 16px 12px; }
        }
      `}</style>
    </div>
  )
}
