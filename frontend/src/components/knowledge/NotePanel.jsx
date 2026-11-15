import { useState, useEffect, useCallback, useRef } from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Placeholder from '@tiptap/extension-placeholder'
import { notesApi, topicsApi } from '@/api/services'
import { toast } from '@/store'
import { useApplicationEvents, useReconnectResync, useResizablePanel } from '@/hooks'
import { getClientId } from '@/ws/clientId'
import { generateSingleNotePdf } from '@/utils/pdfGenerator'
import {
  getCachedNote,
  setCachedNote,
  invalidateCachedNote,
  clearNoteCache,
  getCachedNoteMissing,
  setCachedNoteMissing,
} from '@/cache/noteCache'
import ConfirmModal from './modals/ConfirmModal'
import AttachmentsSection from './AttachmentsSection'
import ResizeHandle from '@/components/layout/ResizeHandle'

const LOG = '[NotePanel]'

/* Przeciągana wysokość nagłówka wpisu (separator nagłówek ↔ „Poziom szczegółowości”) */
const NOTE_HEADER_DEFAULT = 176
const NOTE_HEADER_MIN = 110
const NOTE_HEADER_MAX = 480
const NOTE_HEADER_CONTENT_RESERVE = 220 // miejsce zostawiane pod nagłówkiem (config, załączniki, pasek akcji, minimum treści)

/* ── Treść notatki nachodząca w górę na nagłówek rekordu ──
   Osobny mechanizm od powyższego (ten zmienia wysokość SAMEGO nagłówka w normalnym układzie;
   poniższy przesuwa warstwę załączniki+treść+akcje NAD nagłówkiem, position:absolute).
   `.note-upper-stack` (nagłówek + jego uchwyt + ewentualny baner konfliktu + poziom
   szczegółowości) jest mierzony na żywo (ResizeObserver) — jego wysokość to zarówno domyślna
   (= "brak nachodzenia") pozycja NOWEGO separatora, jak i 100% odniesienia dla limitu 96%/4%. */
const NOTE_LOWER_OVERLAP_RATIO = 0.96 // dolna warstwa może przykryć maks. tyle % wysokości górnego stosu
const NOTE_LOWER_OVERLAP_SLIVER_MIN = 24 // techniczne minimum widocznego skrawka nagłówka + uchwytu (px)
const NOTE_UPPER_STACK_ESTIMATE = 176 + 9 + 96 // rozsądne przybliżenie przed pierwszym pomiarem ResizeObserver (nagłówek + uchwyt + poziom szczegółowości)

const DETAIL_LEVEL_OPTS = [
  { value: 'LOW',    label: 'Niski',  color: '#64748b', bg: '#f1f5f9', border: 'rgba(100,116,139,.25)',
    hint: 'Szkic — zanotowany, do uzupełnienia później.' },
  { value: 'MEDIUM', label: 'Średni', color: '#d97706', bg: '#fffbeb', border: 'rgba(217,119,6,.25)',
    hint: 'Notatka robocza — spisana szybko, czytelna głównie dla Ciebie.' },
  { value: 'HIGH',   label: 'Wysoki', color: '#16a34a', bg: '#f0fdf4', border: 'rgba(22,163,74,.25)',
    hint: 'Notatka dopracowana — napisana tak, by zrozumiał ją każdy.' },
]

const TYPE_LABEL = { INSTRUCTION: 'Instrukcja', NOTE: 'Notatka' }

const MIME_EXT = { 'image/png': 'png', 'image/jpeg': 'jpg', 'image/gif': 'gif', 'image/webp': 'webp' }

function formatDate(iso) {
  if (!iso) return null
  try {
    return new Date(iso).toLocaleString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return null
  }
}

function blobToBase64(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onloadend = () => resolve(String(reader.result).split(',')[1] ?? '')
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
}

/** Podmienia w miejscu "src" obrazków (blob: świeżo wklejone, /api/... już zapisane) na "asset:<id>" i zbiera bajty do wysłania. */
async function resolveImageAssets(node, assets) {
  if (!node) return
  if (node.type === 'image' && node.attrs?.src) {
    const src = node.attrs.src
    if (src.startsWith('blob:') || src.startsWith('/api/')) {
      console.debug(LOG, 'resolveImageAssets: fetching image', src)
      try {
        const resp = await fetch(src)
        if (!resp.ok) throw new Error(`HTTP ${resp.status} while fetching ${src}`)
        const blob = await resp.blob()
        const id = src.startsWith('/api/')
          ? src.substring(src.lastIndexOf('/') + 1)
          : `${crypto.randomUUID()}.${MIME_EXT[blob.type] ?? 'png'}`
        assets.push({ id, mimeType: blob.type, dataBase64: await blobToBase64(blob) })
        node.attrs.src = `asset:${id}`
        console.debug(LOG, 'resolveImageAssets: resolved', { id, mimeType: blob.type, bytes: blob.size })
      } catch (err) {
        console.error(LOG, 'resolveImageAssets: failed for', src, err)
        throw err
      }
    }
  }
  if (Array.isArray(node.content)) {
    for (const child of node.content) await resolveImageAssets(child, assets)
  }
}

/* ── Pasek narzędzi edytora ── */
const Btn = ({ onClick, active, disabled, label, children }) => (
  <button type="button" className={`tb-btn${active ? ' tb-btn--on' : ''}`}
    onMouseDown={e => e.preventDefault()} onClick={onClick} disabled={disabled} title={label}>
    {children}
  </button>
)

function Toolbar({ editor }) {
  if (!editor) return null
  return (
    <div className="editor-toolbar">
      <Btn label="Pogrubienie" active={editor.isActive('bold')} onClick={() => editor.chain().focus().toggleBold().run()}><b>B</b></Btn>
      <Btn label="Kursywa" active={editor.isActive('italic')} onClick={() => editor.chain().focus().toggleItalic().run()}><i>I</i></Btn>
      <Btn label="Przekreślenie" active={editor.isActive('strike')} onClick={() => editor.chain().focus().toggleStrike().run()}><s>S</s></Btn>
      <span className="tb-sep" />
      <Btn label="Nagłówek 1" active={editor.isActive('heading', { level: 1 })} onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}>H1</Btn>
      <Btn label="Nagłówek 2" active={editor.isActive('heading', { level: 2 })} onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}>H2</Btn>
      <Btn label="Nagłówek 3" active={editor.isActive('heading', { level: 3 })} onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}>H3</Btn>
      <span className="tb-sep" />
      <Btn label="Lista punktowana" active={editor.isActive('bulletList')} onClick={() => editor.chain().focus().toggleBulletList().run()}>•</Btn>
      <Btn label="Lista numerowana" active={editor.isActive('orderedList')} onClick={() => editor.chain().focus().toggleOrderedList().run()}>1.</Btn>
      <Btn label="Cytat" active={editor.isActive('blockquote')} onClick={() => editor.chain().focus().toggleBlockquote().run()}>”</Btn>
      <Btn label="Blok kodu" active={editor.isActive('codeBlock')} onClick={() => editor.chain().focus().toggleCodeBlock().run()}>{'</>'}</Btn>
      <span className="tb-sep" />
      <Btn label="Cofnij" onClick={() => editor.chain().focus().undo().run()} disabled={!editor.can().undo()}>↶</Btn>
      <Btn label="Ponów" onClick={() => editor.chain().focus().redo().run()} disabled={!editor.can().redo()}>↷</Btn>
      <span className="tb-hint">Wklej obrazek ze schowka (Ctrl+V) — np. wycinek z Narzędzia wycinania.</span>
    </div>
  )
}

export default function NotePanel({ topic, categoryPath, onTopicUpdated, onOpenTree, editRequest }) {
  const [note, setNote]               = useState(null)
  const [noteLoading, setNoteLoading] = useState(false)
  const [mode, setMode]               = useState('view') // 'view' | 'edit'
  const [pendingAction, setPendingAction] = useState(null) // null | 'edit' | 'save'
  const [actionLoading, setActionLoading] = useState(false)
  const [detailLevel, setDetailLevel] = useState(topic?.detailLevel ?? 'MEDIUM')
  const [savingLevel, setSavingLevel] = useState(false)
  const [downloadingPdf, setDownloadingPdf] = useState(false)
  const [conflict, setConflict] = useState(null) // null | { message: string }
  const originalContentRef = useRef(null)
  const notePanelRef = useRef(null)

  /* Separator nagłówek wpisu ↔ „Poziom szczegółowości” — przeciąganie w pionie zmienia
     wysokość nagłówka, a tym samym (nagłówek jest jedynym elementem o zmiennej wysokości
     nad flex:1 obszarem treści) ilość miejsca dostępnego dla edytora notatki. */
  const headerResize = useResizablePanel({
    defaultSize: NOTE_HEADER_DEFAULT,
    min: NOTE_HEADER_MIN,
    max: NOTE_HEADER_MAX,
    extraMin: NOTE_HEADER_CONTENT_RESERVE,
    containerRef: notePanelRef,
    orientation: 'vertical',
  })

  /* Separator załączniki+treść ↔ nagłówek — przeciąganie w górę nachodzi na cały górny stos
     (nagłówek, ewentualny baner konfliktu, poziom szczegółowości), position:absolute, zamiast go
     ściskać. `upperStackHeight` to jego domyślna (= brak nachodzenia) pozycja I JEDNOCZEŚNIE 100%
     odniesienia dla limitu 96%/4% — mierzona na żywo (ResizeObserver), bo zależy m.in. od wysokości
     nagłówka (osobno przeciąganej powyżej) i od zawijania się przycisków w .note-config.
     `null` dopóki nie mieliśmy jeszcze ANI JEDNEGO realnego pomiaru — do tego czasu używamy
     rozsądnego przybliżenia (NOTE_UPPER_STACK_ESTIMATE), żeby separator miał gdzie "stać" od
     pierwszej klatki, zamiast migać. */
  const upperStackRef = useRef(null)
  const [upperStackHeight, setUpperStackHeight] = useState(null)
  // Czy użytkownik w BIEŻĄCEJ sesji faktycznie przeciągnął ten separator — dopóki nie, jego
  // pozycja ma ściśle podążać za KAŻDĄ zmianą upperStackHeight (np. przeciągnięciem osobnego
  // separatora wysokości nagłówka), a nie zamrażać się na wartości sprzed takiej zmiany. Zawsze
  // startuje jako `false` — rozmiary nie są zapamiętywane między odświeżeniami strony (żaden
  // resizable w aplikacji tego nie robi, zob. useResizablePanel.js), więc nie ma tu żadnej
  // wcześniej zapisanej wartości do rozpoznania.
  const lowerOverlapCustomizedRef = useRef(false)

  useEffect(() => {
    const el = upperStackRef.current
    if (!el || typeof ResizeObserver === 'undefined') return
    const ro = new ResizeObserver(entries => {
      const h = entries[0]?.contentRect?.height
      if (h == null || h <= 0) return
      setUpperStackHeight(h)
    })
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  const upperStackHeightOrEstimate = upperStackHeight ?? NOTE_UPPER_STACK_ESTIMATE
  const lowerOverlapMin = Math.max(upperStackHeightOrEstimate * (1 - NOTE_LOWER_OVERLAP_RATIO), NOTE_LOWER_OVERLAP_SLIVER_MIN)
  const lowerOverlap = useResizablePanel({
    defaultSize: upperStackHeightOrEstimate,
    min: lowerOverlapMin,
    max: Math.max(upperStackHeightOrEstimate, lowerOverlapMin), // strażnik przed odwróconymi granicami w przejściowych stanach pomiaru
    orientation: 'vertical',
  })

  // Dopóki separator nie został świadomie przeciągnięty, jego pozycja ma ŚCIŚLE podążać za każdą
  // zmianą zmierzonej wysokości górnego stosu (np. przeciągnięciem separatora wysokości nagłówka)
  // — inaczej zostałaby "w tyle" i .note-lower-area nie zaczynałaby się dokładnie tam, gdzie
  // .note-upper-stack faktycznie się kończy.
  useEffect(() => {
    if (upperStackHeight == null) return
    if (!lowerOverlapCustomizedRef.current) lowerOverlap.reset()
    // `lowerOverlap.reset` (nie cały `lowerOverlap`) jest tu celowo jedyną zależnością z tego
    // obiektu — to memoizowany callback z samego hooka, stabilny dopóki nie zmienią się jego
    // własne min/max/defaultSize; `lowerOverlap` jako całość to nowy literał obiektu przy każdym
    // renderze, więc zależność od niego uruchamiałaby ten efekt na każdej zmianie stanu.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [upperStackHeight, lowerOverlap.reset])

  const handleLowerOverlapPointerDown = useCallback((e) => {
    lowerOverlapCustomizedRef.current = true
    lowerOverlap.handlePointerDown(e)
  }, [lowerOverlap])

  const handleLowerOverlapReset = useCallback(() => {
    lowerOverlapCustomizedRef.current = false
    lowerOverlap.reset()
  }, [lowerOverlap])

  // Czy .note-lower-area faktycznie nachodzi w tej chwili na górny stos (nagłówek + konfigurację)
  // — 0.5px tolerancji na zaokrąglenia pomiaru ResizeObserver. Sterowana tym klasa
  // `note-panel--expanded-up` neutralizuje wyłącznie to, co w normalnym stanie leży NAD
  // `.note-lower-area` mimo bycia "pod" nią wizualnie — w praktyce `.note-header-resize-handle`,
  // który (jak każdy ResizeHandle) ma własny z-index:170, czyli WYŻEJ niż z-index:20
  // `.note-lower-area` — bez tego jego pozioma linia (::after) zostaje "zawieszona" nad białym
  // tłem rozszerzonej notatki, dokładnie w swoim starym, nieruchomym miejscu.
  const isExpandedUp = lowerOverlap.size < upperStackHeightOrEstimate - 0.5

  const editor = useEditor({
    extensions: [
      StarterKit.configure({ heading: { levels: [1, 2, 3] } }),
      Image,
      Placeholder.configure({ placeholder: 'Zacznij pisać instrukcję lub notatkę…' }),
    ],
    content: '',
    editable: false,
    editorProps: {
      handlePaste(view, event) {
        const items = event.clipboardData?.items
        if (!items) return false
        for (const item of items) {
          if (item.type.startsWith('image/')) {
            const file = item.getAsFile()
            if (!file) continue
            event.preventDefault()
            const url = URL.createObjectURL(file)
            console.debug(LOG, 'paste: image inserted as blob URL', url, { size: file.size, type: file.type })
            const node = view.state.schema.nodes.image.create({ src: url })
            view.dispatch(view.state.tr.replaceSelectionWith(node))
            return true
          }
        }
        return false
      },
    },
  })

  useEffect(() => { editor?.setEditable(mode === 'edit') }, [mode, editor])

  useEffect(() => {
    if (!topic || !editor) { setNote(null); return }
    const topicId = topic.id
    console.debug(LOG, 'loading note for topic', topicId)
    setDetailLevel(topic.detailLevel)
    setMode('view')
    setConflict(null)

    // ── Cache w pamięci karty (zob. src/cache/noteCache.js, dokumentacja/NOTE_CACHE.txt) ──
    // Trafienie w pozytywny wpis — pomija sieć całkowicie, odnawia jego czas ostatniego użycia.
    const cached = getCachedNote(topicId)
    if (cached) {
      console.debug(LOG, 'note cache hit', { topicId, version: cached.version })
      setNote(cached)
      originalContentRef.current = cached.contentJson ?? ''
      editor.commands.setContent(cached.contentJson ?? '')
      setNoteLoading(false)
      return
    }
    // Trafienie w świeży (< 2 min) negatywny wpis — "dla tego tematu nie ma jeszcze notatki".
    if (getCachedNoteMissing(topicId)) {
      console.debug(LOG, 'note cache hit (missing marker)', { topicId })
      setNote(null)
      originalContentRef.current = ''
      editor.commands.setContent('')
      setNoteLoading(false)
      return
    }

    // Brak trafienia (albo wygasło) — pobierz z backendu. AbortController chroni przed wyścigiem:
    // jeśli użytkownik zdąży przełączyć temat, zanim ta odpowiedź wróci, cleanup poniżej ją
    // anuluje i spóźniona odpowiedź nigdy nie nadpisze stanu już wyświetlanego innego tematu.
    const controller = new AbortController()
    setNoteLoading(true)
    notesApi.getByTopic(topicId, { signal: controller.signal })
      .then(res => {
        // Obrona w głąb — normalnie aborted request odrzuca (przechodzi do .catch), ale jawne
        // sprawdzenie tu nie zależy od tego, JAK dokładnie dana implementacja/transport
        // sygnalizuje anulowanie: spóźniona odpowiedź dla już opuszczonego tematu nigdy nie
        // nadpisze stanu aktualnie wyświetlanego (innego) tematu.
        if (controller.signal.aborted) return
        console.debug(LOG, 'note loaded', { topicId, version: res.data.version })
        setCachedNote(topicId, res.data)
        setNote(res.data)
        originalContentRef.current = res.data.contentJson ?? ''
        editor.commands.setContent(res.data.contentJson ?? '')
      })
      .catch(err => {
        if (controller.signal.aborted) return // przełączono temat w międzyczasie — nic nie rób
        if (err.status === 404) {
          console.debug(LOG, 'no note yet for topic', topicId)
          setCachedNoteMissing(topicId)
          setNote(null)
          originalContentRef.current = ''
          editor.commands.setContent('')
        } else {
          console.error(LOG, 'failed to load note', err)
          toast.error('Błąd ładowania notatki')
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setNoteLoading(false)
      })

    return () => controller.abort()
  }, [topic?.id, editor])

  /* ── "Edytuj" z panelu "Weryfikacja aktualności" (ActualityReviewPanel) — przełącza od razu
     w tryb edycji, BEZ ConfirmModal (samo kliknięcie "Edytuj" w tamtym panelu to już świadoma
     decyzja użytkownika). Celowo OSOBNY efekt od powyższego: gdyby `editRequest` był w jego
     zależnościach, konsumowanie żądania (setMode('edit') -> zmiana stanu -> re-render) NIE
     wywołałoby ponownie CAŁEGO ładowania notatki, ale za to wygaszenie `editRequest` z powrotem
     do "skonsumowany" cofnęłoby tryb z powrotem do 'view' (bo ten efekt zależy tylko od
     topic?.id, który by się nie zmienił). Rozdzielenie na dwa efekty tego unika. `nonce` (zamiast
     samego topicId) pozwala na DRUGIE żądanie edycji TEGO SAMEGO tematu po tym, jak poprzednie
     zostało już skonsumowane (np. użytkownik anulował edycję i ponownie kliknął "Edytuj") — sam
     temat nie musi się zmienić, tylko `nonce` musi być nowy. Czeka na `!noteLoading`, żeby nie
     przełączać w edycję nad jeszcze pustym/ładującym się edytorem. */
  const consumedEditNonceRef = useRef(null)
  useEffect(() => {
    if (!editRequest || !topic || editRequest.topicId !== topic.id) return
    if (consumedEditNonceRef.current === editRequest.nonce) return
    if (noteLoading) return
    consumedEditNonceRef.current = editRequest.nonce
    console.debug(LOG, 'editRequest consumed — switching to edit mode', editRequest)
    setMode('edit')
  }, [editRequest, topic, noteLoading])

  /* ── WebSocket — zdarzenia dotyczące notatek (WSZYSTKICH tematów, nie tylko aktualnie
     otwartego — zob. gałąź "NOTE_SAVED dla tematu, który nie jest otwarty" poniżej) ──
     Własne zdarzenia (ta karta) są ignorowane — stan już zaktualizowała odpowiedź HTTP
     notesApi.save, która sama aktualizuje cache (zob. confirmSave). */
  useApplicationEvents(useCallback((event) => {
    const isOwn = event.sourceClientId != null && event.sourceClientId === getClientId()
    if (isOwn) return

    if (event.type === 'NOTE_SAVED') {
      const eventTopicId = event.payload?.topicId
      if (!eventTopicId) return
      const isCurrentlyOpen = !!topic && eventTopicId === topic.id

      if (!isCurrentlyOpen) {
        // Notatka NIE jest aktualnie otwarta — tylko unieważnij jej wpis w cache (pozytywny
        // i negatywny). Nie pobieramy jej od razu — aktualna wersja trafi do cache dopiero
        // przy kolejnym otwarciu tego tematu (zob. efekt ładowania notatki wyżej).
        invalidateCachedNote(eventTopicId)
        return
      }

      invalidateCachedNote(eventTopicId) // stary wpis (pozytywny lub negatywny) już nieaktualny

      if (mode === 'edit') {
        // Ktoś inny zapisał w tle — NIE nadpisujemy niezapisanej treści w edytorze po cichu,
        // tylko informujemy; własny zapis i tak dostanie 409, jeśli spróbuje nadpisać tę wersję.
        // Niezapisanej lokalnej treści świadomie NIE zapisujemy do cache jako "aktualną" wersję.
        setConflict({
          message: 'Inny użytkownik zapisał w tle nowszą wersję tej notatki. '
            + 'Twoje zmiany nie zostały utracone, ale przy zapisie może pojawić się konflikt wersji.',
        })
      } else {
        // Tryb podglądu — nie ma czego chronić, można bezpiecznie po cichu odświeżyć i zapisać
        // świeżo pobraną wersję w cache.
        notesApi.getByTopic(topic.id)
          .then(res => {
            setCachedNote(topic.id, res.data)
            setNote(res.data)
            originalContentRef.current = res.data.contentJson ?? ''
            editor?.commands.setContent(res.data.contentJson ?? '')
          })
          .catch(() => { /* najlepszy wysiłek — użytkownik i tak zobaczy aktualne dane przy następnym wejściu */ })
      }
      onTopicUpdated?.({ ...topic, status: event.payload.topicStatus })
    }

    if (event.type === 'TOPIC_UPDATED' && topic && event.payload?.topic?.id === topic.id) {
      onTopicUpdated?.(event.payload.topic)
      if (mode !== 'edit') setDetailLevel(event.payload.topic.detailLevel)
    }
  }, [topic, mode, editor, onTopicUpdated]))

  /* ── Utrata i odzyskanie połączenia WebSocket ──
     Cache jest bezpieczny tylko przy nieprzerwanej ciągłości zdarzeń NOTE_SAVED. Podczas
     rozłączenia inny użytkownik mógł zapisać notatkę bez naszej wiedzy — po odzyskaniu
     połączenia (NIE przy pierwszym połączeniu po starcie — o to dba useReconnectResync) cały
     cache jest więc czyszczony. Automatycznie pobieramy na nowo TYLKO aktualnie widoczną
     notatkę w trybie podglądu — nie hurtowo wszystko, co było wcześniej w cache. */
  useReconnectResync(useCallback(() => {
    console.debug(LOG, 'WS reconnect — clearing note cache')
    clearNoteCache()
    if (!topic || !editor) return

    if (mode === 'edit') {
      // Nie nadpisujemy lokalnej, niezapisanej treści — tylko ostrzegamy, że wersja na serwerze
      // mogła się zmienić podczas przerwy w połączeniu.
      setConflict({
        message: 'Połączenie z serwerem zostało na chwilę utracone. Podczas przerwy ktoś inny mógł zapisać nowszą wersję tej notatki. '
          + 'Twoje zmiany pozostają w edytorze — możesz je skopiować, a potem odświeżyć, żeby zobaczyć aktualną wersję.',
      })
      return
    }

    // Tryb podglądu — bezpiecznie pobierz aktualną wersję i od razu zapisz świeży wpis w cache.
    notesApi.getByTopic(topic.id)
      .then(res => {
        setCachedNote(topic.id, res.data)
        setNote(res.data)
        originalContentRef.current = res.data.contentJson ?? ''
        editor.commands.setContent(res.data.contentJson ?? '')
      })
      .catch(err => {
        if (err.status === 404) {
          setCachedNoteMissing(topic.id)
          setNote(null)
          originalContentRef.current = ''
          editor.commands.setContent('')
        }
        // inne błędy — najlepszy wysiłek, użytkownik i tak zobaczy aktualne dane przy następnym wejściu
      })
  }, [topic, mode, editor]))

  const handleDetailLevelChange = async (val) => {
    if (val === detailLevel || savingLevel) return
    setDetailLevel(val)
    setSavingLevel(true)
    try {
      const res = await topicsApi.update(topic.id, { detailLevel: val, expectedVersion: topic.version })
      onTopicUpdated?.(res.data)
    } catch (err) {
      console.error(LOG, 'failed to save detail level', err)
      if (err.status === 409) {
        toast.error('Ten temat został w międzyczasie zmieniony przez innego użytkownika — odśwież, aby zobaczyć aktualną wersję')
      } else {
        toast.error('Błąd zapisu poziomu szczegółowości')
      }
      setDetailLevel(topic.detailLevel)
    } finally {
      setSavingLevel(false)
    }
  }

  const requestEdit = () => setPendingAction('edit')

  const confirmEdit = () => {
    console.debug(LOG, 'edit confirmed, entering edit mode')
    setMode('edit')
    setPendingAction(null)
  }

  const cancelEdit = () => {
    console.debug(LOG, 'edit discarded, reverting content')
    editor?.commands.setContent(originalContentRef.current ?? '')
    setMode('view')
  }

  const requestSave = () => setPendingAction('save')

  const confirmSave = useCallback(async () => {
    if (!editor || !topic) return
    setActionLoading(true)
    console.debug(LOG, 'save: starting', { topicId: topic.id, baseVersion: note?.version ?? null })
    try {
      // getJSON() dzieli obiekty attrs z żywym stanem edytora — bez głębokiej kopii,
      // podmiana "src" na "asset:..." poniżej nadpisałaby to, co widać na ekranie.
      const json = JSON.parse(JSON.stringify(editor.getJSON()))
      const assets = []
      await resolveImageAssets(json, assets)
      console.debug(LOG, 'save: assets resolved', { count: assets.length })

      // Optimistic locking — wysyłamy wersję, na której zaczęliśmy edycję (note?.version, null
      // gdy to pierwszy zapis). Backend porówna ją z aktualną wersją w bazie (WHERE version = ?).
      const res = await notesApi.save(topic.id, { contentJson: json, assets, baseVersion: note?.version ?? null })
      console.debug(LOG, 'save: server response', { version: res.data.version })
      // Cache dostaje DOKŁADNIE to, co zwrócił backend — żadnego dodatkowego pobrania tylko
      // po to, żeby go wypełnić.
      setCachedNote(topic.id, res.data)
      setNote(res.data)
      originalContentRef.current = res.data.contentJson ?? ''
      editor.commands.setContent(res.data.contentJson ?? '')
      onTopicUpdated?.({ ...topic, status: 'NOTE_ADDED' })
      setConflict(null)
      toast.success('Zapisano')
      setMode('view')
      setPendingAction(null)
    } catch (err) {
      console.error(LOG, 'save: failed', err)
      if (err.status === 409) {
        // Nie nadpisujemy ani nie czyścimy treści w edytorze — użytkownik może ją ręcznie
        // skopiować. Zostajemy w trybie edycji, żeby nic nie zniknęło "po cichu".
        setConflict({
          message: 'Ten wpis został w międzyczasie zmieniony przez innego użytkownika. '
            + 'Twoja treść pozostaje w edytorze — możesz ją skopiować, a potem odświeżyć, żeby zobaczyć aktualną wersję.',
        })
        setPendingAction(null)
      } else {
        toast.error(err.message ?? 'Błąd zapisu')
      }
    } finally {
      setActionLoading(false)
    }
  }, [editor, topic, note, onTopicUpdated])

  /**
   * Porzuca lokalne (niezapisane) zmiany i wczytuje aktualną wersję notatki z serwera.
   * Zawsze OMIJA cache przy odczycie (użytkownik świadomie prosi o "prawdziwie aktualną"
   * wersję po konflikcie) — ale poprawną odpowiedź od razu zapisuje w cache jako nowy wpis,
   * żeby kolejne otwarcie tego tematu znów mogło z niego skorzystać.
   */
  const reloadAfterConflict = useCallback(async () => {
    if (!topic || !editor) return
    setConflict(null)
    setNoteLoading(true)
    try {
      const res = await notesApi.getByTopic(topic.id)
      setCachedNote(topic.id, res.data)
      setNote(res.data)
      originalContentRef.current = res.data.contentJson ?? ''
      editor.commands.setContent(res.data.contentJson ?? '')
      setMode('view')
    } catch (err) {
      toast.error(err.message ?? 'Błąd ładowania aktualnej wersji')
    } finally {
      setNoteLoading(false)
    }
  }, [topic, editor])

  const handleDownloadPdf = useCallback(async () => {
    if (!editor || !topic || downloadingPdf) return
    setDownloadingPdf(true)
    try {
      // Treść bieżąca z edytora (nie note.contentJson) — tak jak poprzednio, PDF odzwierciedla
      // to, co faktycznie widać na ekranie, łącznie z niezapisanymi jeszcze zmianami w edycji.
      const { issues } = await generateSingleNotePdf({
        title: topic.title,
        categoryPath: (categoryPath ?? []).map(c => c.name),
        author: topic.author,
        createdAt: topic.createdAt,
        updatedAt: note?.updatedAt ?? topic.updatedAt,
        lastVerificationOfActualityDate: topic.lastVerificationOfActualityDate,
        contentJson: editor.getJSON(),
      })
      if (issues.length) {
        const names = issues.map(i => i.detail?.split('/').pop() ?? i.detail).join(', ')
        toast.error(`PDF zapisany, ale ${issues.length} obraz(y) nie udało się załadować: ${names}`)
      } else {
        toast.success('PDF zapisany')
      }
    } catch (err) {
      console.error(LOG, 'PDF generation failed', err)
      toast.error(err.message ?? 'Błąd generowania PDF')
    } finally {
      setDownloadingPdf(false)
    }
  }, [editor, topic, categoryPath, note, downloadingPdf])

  /* ── Brak wybranego tematu ── */
  if (!topic) {
    return (
      <div className="np-empty">
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
          <h2 className="np-empty-title">Wybierz wpis</h2>
          <p className="np-empty-sub">Kliknij wpis z drzewa kategorii,<br/>aby otworzyć edytor.</p>
        </div>
        <NoteStyles />
      </div>
    )
  }

  return (
    <div
      className={`note-panel${isExpandedUp ? ' note-panel--expanded-up' : ''}`}
      ref={notePanelRef}
      style={{ '--note-header-h': `${headerResize.size}px`, '--note-lower-top': `${lowerOverlap.size}px` }}
    >

      {/* ── Górny stos: nagłówek rekordu + poziom szczegółowości (+ ewentualny baner konfliktu) ──
          Mierzony na żywo (ResizeObserver, zob. upperStackRef) — to jego wysokość jest zarówno
          domyślną pozycją NOWEGO separatora poniżej, jak i odniesieniem dla limitu 96%/4%. Zostaje
          w normalnym układzie, nieściskany — to warstwa .note-lower-area (dalej) na niego nachodzi. */}
      <div className="note-upper-stack" ref={upperStackRef}>

        {/* ── Nagłówek ── */}
        <div className="note-header">
          <div className="note-header-top">
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
              <span className="note-type-badge" data-type={topic.type}>{TYPE_LABEL[topic.type] ?? topic.type}</span>
            </div>
          </div>
          <h1 className="note-title">{topic.title}</h1>
          {topic.shortPrompt && <p className="note-desc">{topic.shortPrompt}</p>}
          <div className="note-meta-row">
            <div className="note-author-row" title="Autor będzie uzupełniany automatycznie po zalogowaniu">
              <span className="note-author-label">Autor</span>
              <span className="note-author-value">{topic.author || 'automatycznie z konta (wkrótce)'}</span>
            </div>
            {formatDate(topic.createdAt) && (
              <div className="note-date-row" title="Data utworzenia wpisu — ustawiana raz, przy dodaniu">
                <span className="note-author-label">Utworzono</span>
                <span className="note-author-value">{formatDate(topic.createdAt)}</span>
              </div>
            )}
            {formatDate(note?.createdAt) && (
              <div className="note-date-row" title="Data pierwszego zapisu treści — ustawiana raz, nie zmienia się przy edycji">
                <span className="note-author-label">Treść od</span>
                <span className="note-author-value">{formatDate(note.createdAt)}</span>
              </div>
            )}
            {/* Trzecia data obok "Utworzono"/"Treść od" — zob. dokumentacja/ACTUALITY_VERIFICATION.txt. Zawsze
                widoczna (nie tylko gdy ustawiona), z jawnym "Nigdy", tak samo jak w PDF (zob.
                pdfGenerator.js renderNoteHeader) i w panelu "Weryfikacja aktualności". */}
            <div className="note-date-row" title="Ostatnie świadome potwierdzenie aktualności — ręczny przycisk albo zapis treści notatki">
              <span className="note-author-label">Zweryfikowano</span>
              <span className="note-author-value">
                {formatDate(topic.lastVerificationOfActualityDate) ?? 'Nigdy — termin liczony od utworzenia'}
              </span>
            </div>
          </div>
        </div>

        {/* Separator nagłówek ↔ „Poziom szczegółowości” — przeciąganie w pionie zmienia
            wysokość nagłówka (a więc i ilość miejsca dla treści notatki poniżej). */}
        <ResizeHandle
          className="note-header-resize-handle"
          orientation="vertical"
          label="Wysokość nagłówka wpisu"
          dragging={headerResize.dragging}
          onPointerDown={headerResize.handlePointerDown}
          onDoubleClick={headerResize.reset}
        />

        {/* ── Konflikt wersji (409 przy zapisie, albo zapis innego użytkownika w tle) ── */}
        {conflict && (
          <div className="note-conflict">
            <span className="note-conflict-icon">⚠</span>
            <p className="note-conflict-text">{conflict.message}</p>
            <button type="button" className="note-conflict-btn" onClick={reloadAfterConflict}>
              Odśwież aktualną wersję
            </button>
            <button type="button" className="note-conflict-dismiss" onClick={() => setConflict(null)}>✕</button>
          </div>
        )}

        {/* ── Konfiguracja ── */}
        <div className="note-config">
          <div className="note-config-row">
            <span className="note-config-label">
              Poziom szczegółowości
              {mode !== 'edit' && <span className="note-config-lock" title="Zablokowane — kliknij „Edytuj”, aby zmienić">🔒</span>}
            </span>
            <div className="diff-group">
              {DETAIL_LEVEL_OPTS.map(opt => (
                <button
                  key={opt.value}
                  className={`diff-btn${detailLevel === opt.value ? ' diff-btn--on' : ''}`}
                  style={detailLevel === opt.value ? { background: opt.bg, borderColor: opt.border, color: opt.color } : {}}
                  onClick={() => handleDetailLevelChange(opt.value)}
                  disabled={savingLevel || mode !== 'edit'}
                >
                  {opt.label}
                  {detailLevel === opt.value && savingLevel && <span className="diff-saving" />}
                </button>
              ))}
            </div>
          </div>
          <p className="note-config-hint">{DETAIL_LEVEL_OPTS.find(o => o.value === detailLevel)?.hint}</p>
        </div>
      </div>

      {/* Separator załączniki ↔ poziom szczegółowości — przeciąganie w górę przesuwa i powiększa
          warstwę poniżej (position:absolute), nachodząc na cały górny stos zamiast go ściskać. */}
      <ResizeHandle
        className="note-lower-resize-handle"
        orientation="vertical"
        label="Rozmiar obszaru notatki (załączniki, treść, akcje)"
        dragging={lowerOverlap.dragging}
        onPointerDown={handleLowerOverlapPointerDown}
        onDoubleClick={handleLowerOverlapReset}
      />

      {/* ── Warstwa: załączniki + treść + pasek akcji — position:absolute, nachodzi na .note-upper-stack ── */}
      <div className="note-lower-area">
        {/* ── Załączniki ── */}
        <AttachmentsSection topicId={topic.id} />

        {/* ── Edytor ── */}
        {mode === 'edit' && <Toolbar editor={editor} />}
        <div className="note-content-area">
          {noteLoading ? (
            <div className="note-loading"><span className="note-spinner" /> Ładowanie…</div>
          ) : (
            <>
              {mode === 'view' && !note && (
                <p className="note-empty-hint">Brak treści — kliknij „Edytuj”, aby zacząć pisać.</p>
              )}
              <EditorContent editor={editor} className="note-editor" />
            </>
          )}
        </div>

        {/* ── Pasek akcji ── */}
        <div className="note-actions">
          <div className="act-spacer" />
          {note && (
            <button className="act-btn act-btn--pdf" onClick={handleDownloadPdf} disabled={downloadingPdf}>
              {downloadingPdf ? <span className="btn-spin" /> : (
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                  <path d="M7 1.5v8M4 6.5l3 3 3-3M2.5 11.5h9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              )}
              {downloadingPdf ? 'Generuję…' : 'Pobierz PDF'}
            </button>
          )}
          {mode === 'view' ? (
            <button className="act-btn act-btn--edit" onClick={requestEdit}>
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M9.5 1.5l3 3-8 8-3.5 1 1-3.5 8-8.5z" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round"/>
              </svg>
              Edytuj
            </button>
          ) : (
            <>
              <button className="act-btn act-btn--cancel" onClick={cancelEdit}>Anuluj</button>
              <button className="act-btn act-btn--save" onClick={requestSave}>
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                  <path d="M2 2h8l2 2v8H2V2z" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round"/>
                  <path d="M4 2v3h5V2M4 8h6" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
                </svg>
                Zapisz
              </button>
            </>
          )}
        </div>
      </div>

      {pendingAction === 'edit' && (
        <ConfirmModal
          title="Edytować tę notatkę?"
          message="Przełączysz się w tryb edycji. Możesz w każdej chwili anulować zmiany."
          confirmLabel="Edytuj"
          onConfirm={confirmEdit}
          onClose={() => setPendingAction(null)}
        />
      )}
      {pendingAction === 'save' && (
        <ConfirmModal
          title="Zapisać zmiany?"
          message="Notatka i wklejone obrazki zostaną zapisane. PDF wygenerujesz później, na żądanie."
          confirmLabel="Zapisz"
          loading={actionLoading}
          onConfirm={confirmSave}
          onClose={() => !actionLoading && setPendingAction(null)}
        />
      )}

      <NoteStyles />
    </div>
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
      .np-tree-toggle--inline { position:static; flex-shrink:0; }

      /* ── Panel ── */
      .note-panel { flex:1; display:flex; flex-direction:column; overflow:hidden; background:#fff; min-width:0; position:relative; }

      /* Górny stos — nagłówek + jego uchwyt + baner konfliktu + poziom szczegółowości, w
         normalnym układzie, mierzony na żywo (ResizeObserver, zob. upperStackRef). To NAD nim
         nachodzi .note-lower-area, nie on sam się ściska. */
      .note-upper-stack { display:flex; flex-direction:column; flex-shrink:0; }

      /* Header — wysokość sterowana przeciąganiem separatora (headerResize / --note-header-h);
         border-bottom celowo usunięty — linię separatora rysuje teraz ResizeHandle poniżej. */
      .note-header { height:var(--note-header-h, 176px); padding:18px 24px 14px; overflow-y:auto; flex-shrink:0; }
      .note-header-top { display:flex; align-items:center; gap:8px; margin-bottom:8px; flex-wrap:wrap; }
      .note-breadcrumb { flex:1; display:flex; align-items:center; gap:3px; min-width:0; flex-wrap:wrap; }
      .bc-sep  { color:var(--text-3); font-size:0.72rem; margin:0 2px; }
      .bc-item { font-size:0.71rem; color:var(--text-3); white-space:nowrap; }
      .note-badges { display:flex; gap:5px; align-items:center; flex-shrink:0; }
      .note-type-badge {
        font-family:var(--font-mono); font-size:0.6rem; font-weight:700;
        letter-spacing:0.07em; text-transform:uppercase;
        padding:3px 8px; border-radius:100px;
        background:var(--accent-light); border:1px solid var(--border-0); color:var(--accent);
      }
      .note-type-badge[data-type="INSTRUCTION"] { background:#f5f3ff; border-color:rgba(124,58,237,.25); color:#7c3aed; }
      .note-title { font-size:1.3rem; font-weight:800; color:var(--text-0); line-height:1.25; letter-spacing:-0.01em; margin-bottom:4px; }
      .note-desc { font-size:0.78rem; color:var(--text-3); font-style:italic; line-height:1.5; margin-bottom:8px; }
      .note-meta-row { display:flex; align-items:center; gap:16px; flex-wrap:wrap; }
      .note-author-row, .note-date-row { display:flex; align-items:center; gap:6px; cursor:help; }
      .note-author-label { font-size:0.68rem; font-weight:700; color:var(--text-3); letter-spacing:0.05em; text-transform:uppercase; }
      .note-author-value { font-size:0.78rem; color:var(--text-3); font-style:italic; }

      /* Konflikt wersji — optimistic locking (409) */
      .note-conflict {
        display:flex; align-items:flex-start; gap:10px;
        padding:11px 24px; background:#fffbeb; border-bottom:1px solid rgba(217,119,6,.25);
        flex-shrink:0;
      }
      .note-conflict-icon { font-size:0.95rem; color:#d97706; flex-shrink:0; line-height:1.4; }
      .note-conflict-text { flex:1; font-size:0.79rem; color:#92400e; line-height:1.5; }
      .note-conflict-btn {
        flex-shrink:0; padding:6px 12px; background:#d97706; color:#fff; border:none;
        border-radius:var(--radius-md); font-size:0.76rem; font-weight:700; cursor:pointer;
        white-space:nowrap; transition:background var(--t-fast);
      }
      .note-conflict-btn:hover { background:#b45309; }
      .note-conflict-dismiss {
        flex-shrink:0; background:none; border:none; color:#92400e; cursor:pointer;
        font-size:0.8rem; padding:4px 6px; border-radius:6px; opacity:.7;
      }
      .note-conflict-dismiss:hover { opacity:1; background:rgba(217,119,6,.12); }

      /* Config */
      .note-config {
        padding:12px 24px; border-bottom:1px solid var(--border-1);
        flex-shrink:0; background:var(--bg-2);
        display:flex; flex-direction:column; gap:6px;
      }
      .note-config-row { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
      .note-config-label { font-size:0.71rem; font-weight:700; color:var(--text-2); letter-spacing:0.04em; white-space:nowrap; }
      .note-config-hint { font-size:0.71rem; color:var(--text-3); line-height:1.4; }
      .diff-group { display:flex; gap:5px; flex-wrap:wrap; }
      .diff-btn {
        padding:5px 12px; border-radius:var(--radius-md);
        border:1.5px solid var(--border-1); background:#fff;
        font-size:0.77rem; font-weight:600; color:var(--text-2);
        cursor:pointer; transition:all var(--t-fast); display:flex; align-items:center; gap:4px;
      }
      .diff-btn:hover { border-color:var(--border-0); }
      .diff-btn--on   { font-weight:700; }
      .diff-btn:disabled { opacity:.55; cursor:not-allowed; }
      .note-config-lock { margin-left:5px; font-size:0.7rem; opacity:.6; cursor:help; }
      .diff-saving { width:9px; height:9px; border-radius:50%; border:1.5px solid transparent; border-top-color:currentColor; animation:spin .6s linear infinite; }

      /* Toolbar edytora — "jak w Wordzie" */
      .editor-toolbar {
        display:flex; align-items:center; gap:3px; flex-wrap:wrap;
        padding:8px 24px; border-bottom:1px solid var(--border-1);
        background:#fff; flex-shrink:0; position:relative; z-index:2;
      }
      .tb-btn {
        min-width:28px; height:28px; padding:0 7px; border-radius:6px;
        border:1px solid transparent; background:none; color:var(--text-1);
        font-size:0.82rem; cursor:pointer; display:flex; align-items:center; justify-content:center;
        transition:background var(--t-fast), border-color var(--t-fast);
      }
      .tb-btn:hover:not(:disabled) { background:var(--bg-2); }
      .tb-btn--on { background:var(--accent-light); border-color:var(--border-0); color:var(--accent); }
      .tb-btn:disabled { opacity:.35; cursor:not-allowed; }
      .tb-sep { width:1px; height:18px; background:var(--border-1); margin:0 4px; }
      .tb-hint { margin-left:auto; font-size:0.68rem; color:var(--text-3); white-space:nowrap; }

      /* Separator załączniki ↔ poziom szczegółowości — position:absolute, pozycjonowany na
         --note-lower-top, żeby siedział dokładnie na granicy .note-lower-area również wtedy, gdy
         ta głęboko nachodzi na .note-upper-stack (w normalnym układzie zostałby "pod" nią). */
      .note-lower-resize-handle {
        position: absolute; left: 0; right: 0; margin: 0;
        top: calc(var(--note-lower-top, 0px) - 4px);
      }

      /* Warstwa: załączniki + treść + pasek akcji — nachodzi na .note-upper-stack, gdy
         --note-lower-top < jego naturalnej wysokości (zob. JSX / lowerOverlap). z-index:20 —
         wyraźnie nad .note-upper-stack (bez własnego z-index), wyraźnie pod uchwytem
         (z-index:170, zob. ResizeHandle.jsx) i pod topbarem aplikacji (z-index:50, leży wyżej w
         drzewie DOM, więc go to w ogóle nie dotyczy — --note-lower-top nigdy nie schodzi poniżej
         0, czyli własnego górnego brzegu .note-panel, który i tak leży pod topbarem). */
      .note-lower-area {
        position: absolute; left: 0; right: 0; bottom: 0;
        top: var(--note-lower-top, 0px);
        z-index: 20;
        background: #fff;
        display: flex; flex-direction: column; overflow: hidden;
      }

      /* Gdy .note-lower-area faktycznie nachodzi na górny stos (note-panel--expanded-up, zob.
         isExpandedUp w JSX): .note-header-resize-handle ma WŁASNY z-index:170 (każdy ResizeHandle
         — zob. ResizeHandle.jsx, potrzebne, żeby uchwyty były zawsze chwytalne w normalnym stanie),
         czyli WYŻEJ niż z-index:20 tej warstwy — bez tej reguły jego pozioma linia (::after)
         zostaje "zawieszona" w powietrzu nad białym tłem rozszerzonej notatki, w swoim starym,
         nieruchomym miejscu, mimo że wszystko inne pod spodem jest już poprawnie zakryte. Jedyny
         separator, który MA pozostać widoczny i chwytalny w tym stanie, to .note-lower-resize-handle
         (ten sam ruchomy uchwyt nad "Załącznikami") — jego z-index:170 zostaje nietknięty.
         visibility (nie display:none) — .note-header-resize-handle ma zachować swój layout box
         (jest position:relative w normalnym przepływie .note-upper-stack, a jego zniknięcie z
         przepływu przesunęłoby wszystko poniżej o jego wysokość). pointer-events:none dodatkowo
         gwarantuje, że nie przechwytuje kliknięć/najechania (kursor row-resize) nad obszarem,
         który wizualnie należy już do notatki. */
      .note-panel--expanded-up .note-header-resize-handle {
        visibility: hidden;
        pointer-events: none;
      }
      /* Obronnie (nie jest to obserwowany root cause, ale spójne z resztą): reszta górnego stosu
         jest w normalnym przepływie (position:static), więc jej border-bottom/box-shadow są już
         poprawnie zakrywane przez samo malowanie warstw (.note-lower-area ma jawny z-index > auto)
         — ta reguła tylko dopina ewentualny box-shadow/pseudoelement, gdyby się kiedyś pojawił. */
      .note-panel--expanded-up .note-config,
      .note-panel--expanded-up .note-conflict {
        border-bottom-color: transparent;
        box-shadow: none;
      }
      .note-panel--expanded-up .note-config::before,
      .note-panel--expanded-up .note-config::after,
      .note-panel--expanded-up .note-conflict::before,
      .note-panel--expanded-up .note-conflict::after {
        display: none;
      }

      /* Content / edytor */
      .note-content-area { flex:1; overflow-y:auto; padding:20px 24px; position:relative; }
      .note-empty-hint { font-size:0.83rem; color:var(--text-3); font-style:italic; margin-bottom:12px; }
      .note-loading { display:flex; align-items:center; gap:10px; font-size:0.82rem; color:var(--text-2); padding:40px 0; }
      .note-spinner { width:16px; height:16px; border-radius:50%; border:2px solid var(--border-1); border-top-color:var(--accent); animation:spin .7s linear infinite; }

      /* Edytor wypełnia całą szerokość panelu notatki — panel sam zmienia rozmiar
         wraz z przeciąganiem separatorów układu (ResizeHandle / useResizablePanel),
         więc nie ma tu już natywnego "resize" ani sztywnych max/min-width. */
      .note-editor {
        width: 100%; min-height:220px;
        padding:2px 10px 10px 2px;
      }
      .note-editor .ProseMirror { outline:none; font-size:0.9rem; line-height:1.75; color:var(--text-0); min-height:200px; }
      .note-editor .ProseMirror p.is-editor-empty:first-child::before {
        content: attr(data-placeholder); float:left; color:var(--text-3); pointer-events:none; height:0;
      }
      .note-editor .ProseMirror h1 { font-size:1.4rem; font-weight:800; margin:0.6em 0 0.3em; }
      .note-editor .ProseMirror h2 { font-size:1.15rem; font-weight:700; margin:0.6em 0 0.3em; }
      .note-editor .ProseMirror h3 { font-size:1rem; font-weight:700; margin:0.6em 0 0.3em; }
      .note-editor .ProseMirror p { margin:0.5em 0; }
      .note-editor .ProseMirror ul, .note-editor .ProseMirror ol { padding-left:1.4em; margin:0.5em 0; }
      .note-editor .ProseMirror blockquote { border-left:3px solid var(--border-0); padding-left:12px; margin:0.7em 0; color:var(--text-2); }
      .note-editor .ProseMirror pre { background:var(--bg-3); border-radius:var(--radius-sm); padding:10px 12px; overflow-x:auto; font-family:var(--font-mono); font-size:0.82rem; }
      .note-editor .ProseMirror img { max-width:100%; border-radius:var(--radius-sm); margin:0.6em 0; display:block; }

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
        text-decoration:none;
      }
      .act-btn:disabled { opacity:.45; cursor:not-allowed; }
      .act-btn--pdf { background:#fef2f2; color:#dc2626; border-color:rgba(220,38,38,.25); }
      .act-btn--pdf:hover { background:#fee2e2; }
      .act-btn--edit { background:var(--accent-light); color:var(--accent); border-color:var(--border-0); }
      .act-btn--edit:hover { background:var(--accent-glow-strong); }
      .act-btn--cancel { color:var(--text-2); }
      .act-btn--cancel:hover { background:var(--bg-2); }
      .act-btn--save { background:var(--accent); color:#fff; border-color:var(--accent); }
      .act-btn--save:hover:not(:disabled) { background:var(--accent-dim); }
      .btn-spin { display:inline-block; width:12px; height:12px; border-radius:50%; border:1.5px solid transparent; border-top-color:currentColor; animation:spin .6s linear infinite; }

      /* ── Responsywność ── */
      @media (max-width: 860px) {
        .np-tree-toggle { display:flex; }
        .np-tree-toggle--inline { display:flex; }
        .note-title { font-size:1.1rem; }
        /* Na wąskim ekranie separator wysokości nagłówka jest wyłączony (dotyk pogarsza
           responsywność) — nagłówek wraca do naturalnej, automatycznej wysokości. */
        .note-header { height:auto; overflow-y:visible; padding:14px 16px 12px; border-bottom:1px solid var(--border-1); }
        .note-header-resize-handle { display:none; }
        .note-config, .editor-toolbar { padding:10px 16px; }
        .note-content-area { padding:16px; }
        .note-actions { padding:10px 12px; }
        .act-btn { padding:8px 10px; font-size:0.74rem; }
        .diff-btn { padding:5px 8px; font-size:0.72rem; }
        .tb-hint { display:none; }
        /* Nachodzenie treści notatki na nagłówek (jak i nachodzenie notatki na drzewo w
           DashboardPage) jest funkcją wyłącznie desktopową — na dotyku/wąskim ekranie pogarsza
           responsywność. Wracamy do zwykłego układu flex, na pełną domyślną wysokość. */
        .note-lower-resize-handle { display:none; }
        .note-lower-area { position:static; top:auto; flex:1; }
      }

      @media (max-width: 540px) {
        .act-btn span:not(.act-icon) { display:none; }
        .note-title { font-size:1rem; }
        .bc-item { font-size:0.68rem; }
      }
    `}</style>
  )
}
