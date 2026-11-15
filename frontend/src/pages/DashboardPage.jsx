import { useState, useEffect, useCallback, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useTitle, useResizablePanel, useApplicationEvents, useReconnectResync } from '@/hooks'
import { categoriesApi, topicsApi, notesApi } from '@/api/services'
import { toast } from '@/store'
import { getClientId } from '@/ws/clientId'
import { generateBulkNotesPdf, mapWithConcurrency, PDF_LIMITS } from '@/utils/pdfGenerator'
import CategoryTree from '@/components/knowledge/CategoryTree'
import NotePanel from '@/components/knowledge/NotePanel'
import CreateCategoryModal from '@/components/knowledge/modals/CreateCategoryModal'
import CreateTopicModal from '@/components/knowledge/modals/CreateTopicModal'
import ConfirmModal from '@/components/knowledge/modals/ConfirmModal'
import BulkPdfProgressModal from '@/components/knowledge/modals/BulkPdfProgressModal'
import BulkPdfIssuesModal from '@/components/knowledge/modals/BulkPdfIssuesModal'
import ResizeHandle from '@/components/layout/ResizeHandle'
import { updateCategoryTopicCount, collectSelectedTopicsInTreeOrder, findPath } from './dashboardTreeUtils'

const TREE_DEFAULT = 268
const NOTE_MIN = 360
const HANDLE_SIZE = 9 // szerokość ResizeHandle — wliczana do rezerwacji miejsca dla panelu notatki
const CLEAR_CONFIRM_THRESHOLD = 5 // powyżej tylu zaznaczonych tematów pytamy przed wyczyszczeniem

/* ── Notatka nachodząca na drzewo kategorii (separator drzewo ↔ notatka) ──
   `treeResize.size` już nie jest tylko "szerokością panelu drzewa" — to pozycja separatora,
   liczona od LEWEJ krawędzi drzewa. Dla size >= TREE_DEFAULT zachowuje się DOKŁADNIE jak dawny
   mechanizm (drzewo faktycznie renderuje się szersze, bez nachodzenia). Dla size < TREE_DEFAULT
   drzewo NIE jest już ściskane w normalnym układzie — renderuje się nadal na pełnej szerokości
   TREE_DEFAULT, a notatka (osobna, position:absolute warstwa) wjeżdża na nie od lewej, zostawiając
   widoczny tylko `size` pikseli drzewa. Patrz też `.note-area` / `.tree-resize-handle` w stylach
   poniżej i sekcja 1 zadania (95cache/resize) opisującego to zachowanie. */
const TREE_OVERLAP_RATIO = 0.96 // notatka może przykryć maks. tyle % szerokości drzewa w lewo
const TREE_OVERLAP_SLIVER_MIN = 24 // techniczne minimum widocznego skrawka drzewa + uchwytu (px), gdy 4% byłoby za wąskie do złapania
const TREE_MIN = Math.max(TREE_DEFAULT * (1 - TREE_OVERLAP_RATIO), TREE_OVERLAP_SLIVER_MIN)

export default function DashboardPage() {
  useTitle('Drzewo wiedzy')

  const [searchParams, setSearchParams]   = useSearchParams()
  const [rootTree, setRootTree]           = useState([])
  const [treeLoading, setTreeLoading]     = useState(true)
  const [expanded, setExpanded]           = useState(new Set())
  const [topics, setTopics]               = useState({})
  const [loadingTopics, setLoadingTopics] = useState(new Set())
  const [selectedTopic, setSelectedTopic] = useState(null)
  const [categoryPath, setCategoryPath]   = useState([])
  /* Żądanie "od razu wejdź w tryb edycji" z panelu "Weryfikacja aktualności" — zob. efekt niżej
     i NotePanel#editRequest. { topicId, nonce } | null. */
  const [editRequest, setEditRequest]     = useState(null)

  /* Widoczność drzewa na tablecie */
  const [treeVisible, setTreeVisible]     = useState(true)

  /* Modale */
  const [catModal, setCatModal]       = useState(null)
  const [topicModal, setTopicModal]   = useState(null)

  /* ── Druk masowy — zaznaczenie jako Set (id-y), nigdy tablica z możliwymi duplikatami ── */
  const [bulkMode, setBulkMode]             = useState(false)
  const [selectedTopicIds, setSelectedTopicIds] = useState(() => new Set())
  const [pendingClear, setPendingClear]     = useState(null) // null | { alsoExitBulkMode: boolean }
  const [bulkProgress, setBulkProgress]     = useState(null) // null | { stage, index, total }
  const [bulkIssues, setBulkIssues]         = useState(null) // null | { failed: [...], okCount, notesData: [...] }
  const [bulkPdfBusy, setBulkPdfBusy]       = useState(false)
  const bulkAbortRef = useRef(null)
  // Śledzi, czy podczas TRWAJĄCEGO generowania zbiorczego PDF przyszło NOTE_SAVED dla któregoś
  // z wybranych tematów — żeby po zakończeniu uczciwie poinformować, że dokument mógł już nie
  // zawierać najnowszej wersji (snapshot logiczny jest pobierany raz, na starcie operacji).
  const bulkTrackingRef = useRef({ active: false, changedTopicIds: new Set() })

  /* Przeciągana szerokość drzewa kategorii (separator drzewo ↔ notatka) */
  const dbRootRef = useRef(null)
  const treeResize = useResizablePanel({
    defaultSize: TREE_DEFAULT,
    min: TREE_MIN,
    maxRatio: 0.5,
    extraMin: NOTE_MIN + HANDLE_SIZE,
    containerRef: dbRootRef,
  })
  // Szerokość, na jakiej drzewo FAKTYCZNIE się renderuje — nigdy mniejsza niż TREE_DEFAULT, nawet
  // gdy separator jest przeciągnięty głęboko w lewo (wtedy drzewo stoi w miejscu, a notatka wjeżdża
  // na nie z wierzchu — zob. komentarz przy TREE_OVERLAP_RATIO wyżej).
  const treeDomWidth = Math.max(treeResize.size, TREE_DEFAULT)

  const loadTree = useCallback(async () => {
    setTreeLoading(true)
    try {
      const res = await categoriesApi.getTree()
      setRootTree(res.data.children ?? [])
    } catch (err) {
      if (err.status !== 401) toast.error('Błąd ładowania kategorii')
    } finally {
      setTreeLoading(false)
    }
  }, [])

  useEffect(() => { loadTree() }, [loadTree])

  /* Otwarcie wpisu z wyników wyszukiwarki ALBO z panelu "Weryfikacja aktualności"
     (nawigacja na /dashboard?topic=<id>, opcjonalnie z &edit=1 — zob. editRequest niżej). */
  useEffect(() => {
    const topicId = searchParams.get('topic')
    if (!topicId || treeLoading || rootTree.length === 0) return
    const wantsEdit = searchParams.get('edit') === '1'

    let cancelled = false
    ;(async () => {
      try {
        const res = await topicsApi.getById(topicId)
        if (cancelled) return
        const topic = res.data
        const path = findPath(rootTree, topic.categoryId)
        setExpanded(prev => {
          const next = new Set(prev)
          path.forEach(p => next.add(p.id))
          return next
        })
        setTopics(prev => ({
          ...prev,
          [topic.categoryId]: prev[topic.categoryId]?.some(t => t.id === topic.id)
            ? prev[topic.categoryId]
            : [...(prev[topic.categoryId] ?? []), topic],
        }))
        setSelectedTopic(topic)
        setCategoryPath(path)
        // "Edytuj" z panelu "Weryfikacja aktualności" (ActualityReviewPanel) ma od razu przełączyć
        // notatkę w tryb edycji — parametr nawigacji, nie globalna zmienna (zob. NotePanel#editRequest).
        // `nonce` odróżnia kolejne żądania edycji TEGO SAMEGO tematu (np. dwa kolejne kliknięcia
        // "Edytuj", z anulowaniem edycji między nimi) od jednego, wciąż nieskonsumowanego żądania.
        if (wantsEdit) setEditRequest({ topicId: topic.id, nonce: Date.now() })
        if (window.innerWidth <= 860) setTreeVisible(false)
      } catch {
        toast.error('Nie znaleziono wpisu z wyszukiwarki')
      } finally {
        setSearchParams(params => { params.delete('topic'); params.delete('edit'); return params }, { replace: true })
      }
    })()

    return () => { cancelled = true }
  }, [searchParams, rootTree, treeLoading, setSearchParams])

  const handleToggle = useCallback(async (nodeId) => {
    setExpanded(prev => {
      const next = new Set(prev)
      next.has(nodeId) ? next.delete(nodeId) : next.add(nodeId)
      return next
    })
    if (!topics[nodeId]) {
      setLoadingTopics(p => new Set(p).add(nodeId))
      try {
        const res = await topicsApi.listByCategory(nodeId)
        setTopics(p => ({ ...p, [nodeId]: res.data }))
      } catch { /* 404 = brak tematów */ } finally {
        setLoadingTopics(p => { const n = new Set(p); n.delete(nodeId); return n })
      }
    }
  }, [topics])

  const handleSelectTopic = useCallback((topic) => {
    setSelectedTopic(topic)
    setCategoryPath(findPath(rootTree, topic.categoryId))
    /* Na tablecie ukryj drzewo po wyborze tematu */
    if (window.innerWidth <= 860) setTreeVisible(false)
  }, [rootTree])

  const handleCategoryCreated = useCallback((newCat) => {
    loadTree()
    if (newCat.parentId) setExpanded(prev => new Set(prev).add(newCat.parentId))
  }, [loadTree])

  /* Karta, która sama utworzyła temat: aktualizuje stan natychmiast z odpowiedzi HTTP —
     licznik idzie na wartość zwróconą przez backend (categoryTopicCount), nie ślepe +1. */
  const handleTopicCreated = useCallback((newTopic) => {
    setTopics(prev => ({
      ...prev,
      [newTopic.categoryId]: [...(prev[newTopic.categoryId] ?? []), newTopic],
    }))
    setRootTree(prev => updateCategoryTopicCount(prev, newTopic.categoryId, 1, newTopic.categoryTopicCount))
    setExpanded(prev => new Set(prev).add(newTopic.categoryId))
    setSelectedTopic(newTopic)
    setCategoryPath(findPath(rootTree, newTopic.categoryId))
    if (window.innerWidth <= 860) setTreeVisible(false)
  }, [rootTree])

  const handleTopicUpdated = useCallback((updated) => {
    setTopics(prev => ({
      ...prev,
      [updated.categoryId]: (prev[updated.categoryId] ?? []).map(t =>
        t.id === updated.id ? updated : t
      ),
    }))
    setSelectedTopic(prev => prev?.id === updated.id ? updated : prev)
  }, [])

  // ── Druk masowy ──────────────────────────────────────────────────────────────────────────

  const doClearSelection = useCallback((alsoExitBulkMode) => {
    setSelectedTopicIds(new Set())
    if (alsoExitBulkMode) setBulkMode(false)
  }, [])

  /** Duże zaznaczenie -> pyta przed wyczyszczeniem; małe -> czyści od razu. */
  const requestClearSelection = useCallback((alsoExitBulkMode) => {
    if (selectedTopicIds.size > CLEAR_CONFIRM_THRESHOLD) {
      setPendingClear({ alsoExitBulkMode })
    } else {
      doClearSelection(alsoExitBulkMode)
    }
  }, [selectedTopicIds, doClearSelection])

  const handleToggleBulkMode = useCallback((on) => {
    if (on) { setBulkMode(true); return }
    // Wyłączenie trybu ZAWSZE czyści zaznaczenie i ukrywa checkboxy/pasek akcji.
    requestClearSelection(true)
  }, [requestClearSelection])

  const handleToggleSelect = useCallback((topic) => {
    setSelectedTopicIds(prev => {
      const next = new Set(prev)
      if (next.has(topic.id)) next.delete(topic.id)
      else next.add(topic.id)
      return next
    })
  }, [])

  /**
   * "Zaznacz widoczne" — wyłącznie tematy już POBRANE do stanu (rozwinięte kategorie) i mające
   * zapisaną notatkę. Świadomie NIE jest to "zaznacz wszystko" — nie sięga po tematy z kategorii,
   * których nikt jeszcze nie rozwinął (zob. komentarz w dashboardTreeUtils).
   */
  const handleSelectVisible = useCallback(() => {
    setSelectedTopicIds(prev => {
      const next = new Set(prev)
      for (const list of Object.values(topics)) {
        for (const t of list) {
          if (t.status === 'NOTE_ADDED') next.add(t.id)
        }
      }
      return next
    })
  }, [topics])

  const resetBulkPdfState = useCallback(() => {
    setBulkPdfBusy(false)
    setBulkProgress(null)
    setBulkIssues(null)
    bulkAbortRef.current = null
    bulkTrackingRef.current = { active: false, changedTopicIds: new Set() }
  }, [])

  const handleCancelBulkFetch = useCallback(() => {
    bulkAbortRef.current?.abort()
  }, [])

  /** Ostatni krok — renderuje i zapisuje PDF z już pobranych, poprawnych notatek. */
  const finishBulkPdf = useCallback(async (notesData) => {
    setBulkIssues(null)
    setBulkProgress({ stage: 'toc', index: 0, total: notesData.length })
    try {
      const { issues } = await generateBulkNotesPdf(notesData, {
        onProgress: (info) => setBulkProgress(info),
      })
      const changedIds = bulkTrackingRef.current.changedTopicIds
      if (changedIds.size > 0) {
        const titles = notesData.filter(n => changedIds.has(n.topicId)).map(n => n.title)
        toast.warn(`Uwaga: podczas generowania ktoś zapisał nowszą wersję: ${titles.join(', ')} — PDF zawiera wersję sprzed tej zmiany.`)
      }
      if (issues.length > 0) {
        const names = issues.map(i => `${i.noteTitle} (${i.detail?.split('/').pop() ?? i.detail})`).join(', ')
        toast.error(`PDF zapisany, ale ${issues.length} obraz(y) nie udało się załadować: ${names}`)
      } else {
        toast.success(`PDF zapisany (${notesData.length} notatek)`)
      }
    } catch (err) {
      console.error('[DashboardPage] bulk PDF generation failed', err)
      toast.error(err.message ?? 'Błąd generowania zbiorczego PDF')
    } finally {
      resetBulkPdfState()
    }
  }, [resetBulkPdfState])

  const handleRequestBulkPdf = useCallback(async () => {
    if (selectedTopicIds.size === 0 || bulkPdfBusy) return
    const orderedTopics = collectSelectedTopicsInTreeOrder(rootTree, topics, selectedTopicIds)
    if (orderedTopics.length === 0) return
    if (orderedTopics.length > PDF_LIMITS.maxBulkNotes) {
      toast.error(`Można wygenerować maksymalnie ${PDF_LIMITS.maxBulkNotes} notatek naraz (wybrano ${orderedTopics.length}).`)
      return
    }
    if (orderedTopics.length > 15) {
      toast.info('Duża liczba notatek — generowanie może chwilę potrwać.')
    }

    setBulkPdfBusy(true)
    const controller = new AbortController()
    bulkAbortRef.current = controller
    bulkTrackingRef.current = { active: true, changedTopicIds: new Set() }
    setBulkProgress({ stage: 'fetching', index: 0, total: orderedTopics.length })

    let completed = 0
    const results = await mapWithConcurrency(orderedTopics, 3, async (topic) => {
      try {
        const res = await notesApi.getByTopic(topic.id, { signal: controller.signal })
        completed += 1
        setBulkProgress(p => (p ? { ...p, index: completed } : p))
        return { topic, note: res.data, ok: true }
      } catch (err) {
        completed += 1
        setBulkProgress(p => (p ? { ...p, index: completed } : p))
        return { topic, error: err, ok: false }
      }
    })

    if (controller.signal.aborted) {
      resetBulkPdfState()
      toast.info('Generowanie PDF anulowane')
      return
    }

    const succeeded = results.filter(r => r.ok)
    const failed = results.filter(r => !r.ok)
    const notesData = succeeded.map(({ topic, note }) => ({
      topicId: topic.id,
      title: topic.title,
      categoryPath: findPath(rootTree, topic.categoryId).map(c => c.name),
      author: topic.author,
      createdAt: topic.createdAt,
      updatedAt: note.updatedAt,
      lastVerificationOfActualityDate: topic.lastVerificationOfActualityDate,
      contentJson: note.contentJson,
    }))

    if (failed.length > 0) {
      setBulkProgress(null)
      setBulkIssues({ failed, okCount: succeeded.length, notesData })
      return
    }

    await finishBulkPdf(notesData)
  }, [selectedTopicIds, bulkPdfBusy, rootTree, topics, resetBulkPdfState, finishBulkPdf])

  /* ── WebSocket — reakcja na zdarzenia innych kart/użytkowników ──
     Własne zdarzenia (sourceClientId === nasz clientId) już zostały zastosowane przez odpowiedź
     HTTP tego samego żądania — tu tylko cicho synchronizujemy licznik do wartości autorytatywnej
     (SET, nie kolejny +1), żeby nigdy nie policzyć własnej operacji podwójnie. */
  useApplicationEvents(useCallback((event) => {
    const isOwn = event.sourceClientId != null && event.sourceClientId === getClientId()

    switch (event.type) {
      case 'TOPIC_CREATED': {
        const { topic, currentTopicCount } = event.payload ?? {}
        if (!topic) return
        if (isOwn) {
          setRootTree(prev => updateCategoryTopicCount(prev, topic.categoryId, 0, currentTopicCount))
          return
        }
        setTopics(prev => {
          if (!(topic.categoryId in prev)) return prev // kategoria nigdy nie była rozwinięta — nic do dopisania
          if (prev[topic.categoryId].some(t => t.id === topic.id)) return prev // już jest -> bez duplikatu
          return { ...prev, [topic.categoryId]: [...prev[topic.categoryId], topic] }
        })
        setRootTree(prev => updateCategoryTopicCount(prev, topic.categoryId, 1, currentTopicCount))
        // Celowo NIE zmieniamy selectedTopic i nie otwieramy żadnego modala/panelu za kogoś innego.
        break
      }
      case 'TOPIC_UPDATED': {
        if (isOwn) return // już zastosowane przez odpowiedź HTTP tego żądania
        const { topic } = event.payload ?? {}
        if (!topic) return
        setTopics(prev => ({
          ...prev,
          [topic.categoryId]: (prev[topic.categoryId] ?? []).map(t => t.id === topic.id ? topic : t),
        }))
        setSelectedTopic(prev => prev?.id === topic.id ? topic : prev)
        break
      }
      case 'TOPIC_DELETED': {
        const { topicId, currentTopicCount } = event.payload ?? {}
        if (!topicId) return
        setTopics(prev => {
          const next = { ...prev }
          for (const catId of Object.keys(next)) {
            if (next[catId].some(t => t.id === topicId)) next[catId] = next[catId].filter(t => t.id !== topicId)
          }
          return next
        })
        if (event.categoryId) {
          setRootTree(prev => updateCategoryTopicCount(prev, event.categoryId, isOwn ? 0 : -1, currentTopicCount))
        }
        if (!isOwn) {
          setSelectedTopic(prev => {
            if (prev?.id === topicId) {
              toast.warn('Ten temat został usunięty przez innego użytkownika')
              return null
            }
            return prev
          })
        }
        // Druk masowy — usunięty temat nie może zostać w zaznaczeniu (ani w cudzym, ani we
        // własnym: gdyby to BYŁO nasze usunięcie, temat i tak zniknął z drzewa, checkbox przestał
        // istnieć razem z nim).
        setSelectedTopicIds(prev => {
          if (!prev.has(topicId)) return prev
          if (!isOwn) toast.warn('Jeden z zaznaczonych tematów został usunięty przez innego użytkownika i zniknął z zaznaczenia')
          const next = new Set(prev)
          next.delete(topicId)
          return next
        })
        break
      }
      case 'NOTE_SAVED': {
        if (isOwn) return
        const { topicId, topicStatus } = event.payload ?? {}
        if (!topicId) return
        setTopics(prev => {
          const next = { ...prev }
          for (const catId of Object.keys(next)) {
            next[catId] = next[catId].map(t => t.id === topicId ? { ...t, status: topicStatus } : t)
          }
          return next
        })
        break
      }
      case 'CATEGORY_CREATED':
      case 'CATEGORY_UPDATED':
      case 'CATEGORY_DELETED':
        if (!isOwn) loadTree()
        break
      default:
        break
    }
  }, [loadTree]))

  /* Po odzyskaniu połączenia WS zdarzenia z okresu przerwy mogły zostać bezpowrotnie przegapione —
     baza pozostaje źródłem prawdy, więc realnie pobieramy drzewo/liczniki i aktualnie rozwinięte
     listy tematów na nowo, zamiast zakładać, że lokalny stan jest wciąż aktualny. */
  useReconnectResync(useCallback(() => {
    loadTree()
    expanded.forEach(catId => {
      topicsApi.listByCategory(catId)
        .then(res => setTopics(p => ({ ...p, [catId]: res.data })))
        .catch(() => { /* 404 = brak tematów, nic do zrobienia */ })
    })
  }, [loadTree, expanded]))

  /* Osobna subskrypcja WS — aktywna TYLKO podczas trwającego generowania zbiorczego PDF (zob.
     bulkTrackingRef/handleRequestBulkPdf). Notatki są pobierane raz, na starcie operacji — jeśli
     ktoś zapisze nowszą wersję w międzyczasie, PDF i tak użyje tego, co pobrano na starcie
     (spójny snapshot logiczny), ale użytkownik dostaje o tym uczciwą informację po zakończeniu. */
  useApplicationEvents(useCallback((event) => {
    if (!bulkTrackingRef.current.active) return
    if (event.type !== 'NOTE_SAVED') return
    const topicId = event.payload?.topicId
    if (topicId && selectedTopicIds.has(topicId)) {
      bulkTrackingRef.current.changedTopicIds.add(topicId)
    }
  }, [selectedTopicIds]))

  return (
    <div className="db-root" ref={dbRootRef} style={{ '--tree-visible-w': `${treeResize.size}px` }}>

      {/* Overlay tła na tablecie gdy drzewo otwarte */}
      {treeVisible && (
        <div
          className="db-overlay"
          onClick={() => setTreeVisible(false)}
        />
      )}

      {/* Drzewo — stałe na desktop, slide-in na tablecie. Renderuje się zawsze na pełnej
          (co najmniej TREE_DEFAULT) szerokości — patrz treeDomWidth — nawet gdy notatka
          wjeżdża na nie z wierzchu; drzewo nigdy nie jest ściskane w locie. */}
      <div className={`db-tree-wrap${treeVisible ? ' db-tree-wrap--open' : ''}`}>
        <CategoryTree
          tree={rootTree}
          loading={treeLoading}
          expanded={expanded}
          topics={topics}
          loadingTopics={loadingTopics}
          selectedTopicId={selectedTopic?.id}
          width={treeDomWidth}
          onToggle={handleToggle}
          onSelectTopic={handleSelectTopic}
          onAddRootCategory={() => setCatModal({ parentId: null, parentName: null })}
          onAddSubcategory={(id, name) => setCatModal({ parentId: id, parentName: name })}
          onAddTopic={(id, name) => setTopicModal({ categoryId: id, categoryName: name })}
          bulkMode={bulkMode}
          onToggleBulkMode={handleToggleBulkMode}
          selectedIds={selectedTopicIds}
          onToggleSelect={handleToggleSelect}
          onClearSelection={() => requestClearSelection(false)}
          onSelectVisible={handleSelectVisible}
          onRequestBulkPdf={handleRequestBulkPdf}
          bulkPdfBusy={bulkPdfBusy}
        />
      </div>

      {/* Separator drzewo ↔ notatka (tylko na desktopie — na tablecie drzewo jest overlayem).
          Pozycjonowany absolutnie na `--tree-visible-w`, żeby podążał za notatką również wtedy,
          gdy ta nachodzi na drzewo (patrz .note-area poniżej) — w normalnym układzie flex
          zostałby "pod" notatką i nie dałoby się go złapać w rejonie nachodzenia. */}
      <ResizeHandle
        className="tree-resize-handle"
        label="Szerokość drzewa kategorii / nachodzenie notatki na drzewo"
        dragging={treeResize.dragging}
        onPointerDown={treeResize.handlePointerDown}
        onDoubleClick={treeResize.reset}
      />

      {/* Warstwa notatki — position:absolute, żeby mogła nachodzić na panel drzewa zamiast
          jedynie powiększać się w pozostałej wolnej przestrzeni (zob. sekcja 1 zadania resize).
          `left` podąża za `--tree-visible-w`; dla treeResize.size >= TREE_DEFAULT to DOKŁADNIE
          prawa krawędź w pełni renderowanego drzewa (zero nachodzenia, piksel w piksel jak dawny
          układ flex) — dla treeResize.size < TREE_DEFAULT to widoczny skrawek drzewa. */}
      <div className="note-area">
        <NotePanel
          topic={selectedTopic}
          categoryPath={categoryPath}
          onTopicUpdated={handleTopicUpdated}
          onOpenTree={() => setTreeVisible(true)}
          editRequest={editRequest}
        />
      </div>

      {/* Modale */}
      {catModal !== null && (
        <CreateCategoryModal
          parentId={catModal.parentId}
          parentName={catModal.parentName}
          onClose={() => setCatModal(null)}
          onCreated={handleCategoryCreated}
        />
      )}
      {topicModal !== null && (
        <CreateTopicModal
          categoryId={topicModal.categoryId}
          categoryName={topicModal.categoryName}
          onClose={() => setTopicModal(null)}
          onCreated={handleTopicCreated}
        />
      )}
      {pendingClear !== null && (
        <ConfirmModal
          title="Wyczyścić zaznaczenie?"
          message={`Odznaczysz ${selectedTopicIds.size} wybranych tematów. Tej operacji nie można cofnąć.`}
          confirmLabel="Wyczyść"
          onConfirm={() => { doClearSelection(pendingClear.alsoExitBulkMode); setPendingClear(null) }}
          onClose={() => setPendingClear(null)}
        />
      )}
      {bulkProgress && (
        <BulkPdfProgressModal progress={bulkProgress} onCancel={handleCancelBulkFetch} />
      )}
      {bulkIssues && (
        <BulkPdfIssuesModal
          failed={bulkIssues.failed}
          okCount={bulkIssues.okCount}
          onCancel={() => { resetBulkPdfState(); toast.info('Generowanie PDF anulowane') }}
          onContinueWithout={() => finishBulkPdf(bulkIssues.notesData)}
        />
      )}
      <style>{`
        .db-root {
          display: flex;
          height: calc(100vh - 56px);
          margin: -28px -32px;
          overflow: hidden;
          position: relative;
        }

        .db-tree-wrap {
          flex-shrink: 0;
          display: flex;
        }

        /* Separator — pozycjonowany absolutnie na --tree-visible-w (patrz inline style na
           .db-root), żeby zawsze siedział DOKŁADNIE na granicy widocznego drzewa/notatki,
           również gdy notatka głęboko nachodzi na drzewo (wtedy w normalnym układzie flex
           separator zostałby "pod" nią, nieuchwytny). */
        .tree-resize-handle {
          position: absolute; top: 0; bottom: 0; margin: 0;
          left: calc(var(--tree-visible-w) - 4px);
        }

        /* Warstwa notatki — nachodzi na drzewo, gdy --tree-visible-w < TREE_DEFAULT (zob. JSX).
           z-index:10 — wyraźnie powyżej .tree-panel (bez własnego z-index, więc efektywnie 0 w
           tym samym kontekście spozycjonowania), ale wyraźnie poniżej uchwytów (z-index:170,
           zob. ResizeHandle.jsx) i reszty chrome aplikacji (sidebar z-index:100 żyje POZA
           .content-wrap, topbar z-index:50 leży wyżej w drzewie DOM) — nachodzenie nigdy nie
           może więc wizualnie przykryć sidebaru ani topbara, co dodatkowo gwarantuje sama
           geometria (--tree-visible-w nigdy nie spada do 0, a .note-area nigdy nie wychodzi
           powyżej górnej krawędzi .db-root, która i tak leży pod topbarem). */
        .note-area {
          position: absolute; top: 0; bottom: 0; right: 0;
          left: var(--tree-visible-w);
          z-index: 10;
          display: flex; min-width: 0;
        }

        /* ── Responsywność ── */
        @media (max-width: 860px) {
          .db-tree-wrap {
            position: absolute;
            top: 0; left: 0; bottom: 0;
            z-index: 50;
            transform: translateX(-100%);
            transition: transform var(--t-slow);
          }
          .db-tree-wrap--open {
            transform: translateX(0);
            box-shadow: var(--shadow-lg);
          }
          .db-overlay {
            position: absolute; inset: 0; z-index: 40;
            background: rgba(15,23,42,0.25);
            backdrop-filter: blur(1px);
          }
          /* Drzewo jest overlayem poza przepływem flex — separator nie ma tu sensu, a nachodzenie
             notatki na drzewo (desktopowa funkcja) pogarszałoby tu responsywność — wyłączamy je i
             wracamy do zwykłego układu flex na pełną szerokość. */
          .tree-resize-handle { display: none; }
          .note-area { position: static; left: auto; flex: 1; }
        }

        @media (min-width: 861px) {
          .db-overlay { display: none; }
        }

        /* Ujemny margines .db-root musi znosić padding .main-content (AppLayout) —
           ten padding maleje na węższych ekranach, więc margines musi podążać za nim,
           inaczej strona przepełnia się poziomo (patrz breakpointy w AppLayout.jsx). */
        @media (max-width: 900px) {
          .db-root { margin: -16px; }
        }
        @media (max-width: 540px) {
          .db-root { margin: -12px; }
        }
      `}</style>
    </div>
  )
}

