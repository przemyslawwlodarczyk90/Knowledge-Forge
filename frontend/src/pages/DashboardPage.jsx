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

  const [rootTree, setRootTree]           = useState([])
  const [treeLoading, setTreeLoading]     = useState(true)
  const [expanded, setExpanded]           = useState(new Set())
  const [topics, setTopics]               = useState({})
  const [loadingTopics, setLoadingTopics] = useState(new Set())
  const [selectedTopic, setSelectedTopic] = useState(null)
  const [categoryPath, setCategoryPath]   = useState([])

  /* Widoczność drzewa na tablecie */
  const [treeVisible, setTreeVisible]     = useState(true)

  /* Modale */
  const [catModal, setCatModal]     = useState(null)
  const [topicModal, setTopicModal] = useState(null)

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

  const handleTopicCreated = useCallback((newTopic) => {
    setTopics(prev => ({
      ...prev,
      [newTopic.categoryId]: [...(prev[newTopic.categoryId] ?? []), newTopic],
    }))
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

  return (
    <div className="db-root">

      {/* Overlay tła na tablecie gdy drzewo otwarte */}
      {treeVisible && (
        <div
          className="db-overlay"
          onClick={() => setTreeVisible(false)}
        />
      )}

      {/* Drzewo — stałe na desktop, slide-in na tablecie */}
      <div className={`db-tree-wrap${treeVisible ? ' db-tree-wrap--open' : ''}`}>
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
      </div>

      {/* Panel notatki */}
      <NotePanel
        topic={selectedTopic}
        categoryPath={categoryPath}
        onTopicUpdated={handleTopicUpdated}
        onOpenTree={() => setTreeVisible(true)}
      />

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
        }

        @media (min-width: 861px) {
          .db-overlay { display: none; }
        }
      `}</style>
    </div>
  )
}

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
