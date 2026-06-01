import { useState, useEffect, useCallback } from 'react'
import { useTitle } from '@/hooks'
import { categoriesApi, topicsApi } from '@/api/services'
import { toast } from '@/store'
import CategoryTree from '@/components/knowledge/CategoryTree'
import NotePanel from '@/components/knowledge/NotePanel'
import CreateCategoryModal from '@/components/knowledge/modals/CreateCategoryModal'
import CreateTopicModal from '@/components/knowledge/modals/CreateTopicModal'

export default function DashboardPage() {
  useTitle('Dashboard')

  /* ── Stan drzewa ── */
  const [rootTree, setRootTree]             = useState([])      // children root node
  const [treeLoading, setTreeLoading]       = useState(true)
  const [expanded, setExpanded]             = useState(new Set())
  const [topics, setTopics]                 = useState({})      // { catId: Topic[] }
  const [loadingTopics, setLoadingTopics]   = useState(new Set())
  const [selectedTopic, setSelectedTopic]   = useState(null)
  const [categoryPath, setCategoryPath]     = useState([])

  /* ── Modale ── */
  const [catModal, setCatModal]     = useState(null)   // { parentId, parentName } | null
  const [topicModal, setTopicModal] = useState(null)   // { categoryId, categoryName } | null

  /* ── Załaduj drzewo kategorii ── */
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

  /* ── Rozwiń / zwiń węzeł ── */
  const handleToggle = useCallback(async (nodeId) => {
    setExpanded(prev => {
      const next = new Set(prev)
      if (next.has(nodeId)) { next.delete(nodeId); return next }
      next.add(nodeId)
      return next
    })
    // Załaduj tematy jeśli jeszcze nie ma
    if (!topics[nodeId]) {
      setLoadingTopics(p => new Set(p).add(nodeId))
      try {
        const res = await topicsApi.listByCategory(nodeId)
        setTopics(p => ({ ...p, [nodeId]: res.data }))
      } catch {
        /* 404 = brak tematów, to OK */
      } finally {
        setLoadingTopics(p => { const n = new Set(p); n.delete(nodeId); return n })
      }
    }
  }, [topics])

  /* ── Wybierz temat ── */
  const handleSelectTopic = useCallback((topic) => {
    setSelectedTopic(topic)
    setCategoryPath(findPath(rootTree, topic.categoryId))
  }, [rootTree])

  /* ── Dodaj kategorię ── */
  const handleCategoryCreated = useCallback((newCat) => {
    loadTree()
    // Jeśli nowa kategoria ma parentId, rozwiń rodzica
    if (newCat.parentId) {
      setExpanded(prev => new Set(prev).add(newCat.parentId))
    }
  }, [loadTree])

  /* ── Dodaj temat ── */
  const handleTopicCreated = useCallback((newTopic) => {
    setTopics(prev => ({
      ...prev,
      [newTopic.categoryId]: [...(prev[newTopic.categoryId] ?? []), newTopic],
    }))
    setExpanded(prev => new Set(prev).add(newTopic.categoryId))
    setSelectedTopic(newTopic)
    setCategoryPath(findPath(rootTree, newTopic.categoryId))
  }, [rootTree])

  /* ── Aktualizacja tematu (np. zmiana difficulty) ── */
  const handleTopicUpdated = useCallback((updated) => {
    setTopics(prev => ({
      ...prev,
      [updated.categoryId]: (prev[updated.categoryId] ?? []).map(t =>
        t.id === updated.id ? updated : t
      ),
    }))
    setSelectedTopic(prev => prev?.id === updated.id ? updated : prev)
  }, [])

  return (
    <div className="db-root">
      {/* ── Drzewo ── */}
      <CategoryTree
        tree={rootTree}
        loading={treeLoading}
        expanded={expanded}
        topics={topics}
        loadingTopics={loadingTopics}
        selectedTopicId={selectedTopic?.id}
        onToggle={handleToggle}
        onSelectTopic={handleSelectTopic}
        onAddRootCategory={() => setCatModal({ parentId: null, parentName: null })}
        onAddSubcategory={(id, name) => setCatModal({ parentId: id, parentName: name })}
        onAddTopic={(id, name) => setTopicModal({ categoryId: id, categoryName: name })}
      />

      {/* ── Notatka ── */}
      <NotePanel
        topic={selectedTopic}
        categoryPath={categoryPath}
        onTopicUpdated={handleTopicUpdated}
      />

      {/* ── Modele ── */}
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

      <style>{`
        .db-root {
          display: flex;
          height: calc(100vh - 56px); /* topbar height */
          margin: -28px -32px;        /* negate main-content padding */
          overflow: hidden;
        }
      `}</style>
    </div>
  )
}

/* Znajdź ścieżkę do kategorii w drzewie */
function findPath(tree, categoryId, path = []) {
  for (const node of tree) {
    const current = [...path, { id: node.id, name: node.name }]
    if (node.id === categoryId) return current
    if (node.children?.length) {
      const found = findPath(node.children, categoryId, current)
      if (found) return found
    }
  }
  return []
}
