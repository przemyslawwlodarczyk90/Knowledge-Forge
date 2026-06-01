import { useState } from 'react'

/* ── Ikony SVG ── */
const ChevronRight = () => (
  <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
    <path d="M4 2.5L7.5 6 4 9.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const ChevronDown = () => (
  <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
    <path d="M2 4L6 8l4-4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const FolderIcon = ({ open }) => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
    {open
      ? <path d="M1 4.5a1 1 0 011-1h3l1 1h6a1 1 0 011 1v5a1 1 0 01-1 1H2a1 1 0 01-1-1v-6z" fill="var(--accent)" opacity="0.7" stroke="none"/>
      : <path d="M1 4.5a1 1 0 011-1h3l1 1h6a1 1 0 011 1v5a1 1 0 01-1 1H2a1 1 0 01-1-1v-6z" stroke="currentColor" strokeWidth="1.3" fill="none"/>
    }
  </svg>
)
const NoteIcon = () => (
  <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
    <rect x="1.5" y="1" width="9" height="10" rx="1.5" stroke="currentColor" strokeWidth="1.3"/>
    <path d="M3.5 4h5M3.5 6.5h5M3.5 9h3" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
  </svg>
)
const PlusIcon = () => (
  <svg width="11" height="11" viewBox="0 0 11 11" fill="none">
    <path d="M5.5 1.5v8M1.5 5.5h8" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
  </svg>
)

const DIFF_COLOR = { BASIC: '#16a34a', MEDIUM: '#d97706', HARD: '#dc2626' }
const DIFF_DOT   = { BASIC: '●', MEDIUM: '●', HARD: '●' }

/* ── Pojedynczy węzeł drzewa ── */
function TreeNode({ node, depth = 0, expanded, topics, loadingTopics, selectedTopicId,
  onToggle, onSelectTopic, onAddSubcategory, onAddTopic }) {

  const [hovered, setHovered] = useState(false)
  const isExpanded = expanded.has(node.id)
  const hasChildren = (node.children?.length ?? 0) > 0
  const nodeTopics  = topics[node.id] ?? []
  const isLoading   = loadingTopics.has(node.id)
  const indent      = depth * 16

  return (
    <div>
      {/* ── Wiersz kategorii ── */}
      <div
        className={`tree-row${isExpanded ? ' tree-row--open' : ''}`}
        style={{ paddingLeft: 12 + indent }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      >
        {/* Chevron */}
        <button
          className="tree-chevron"
          onClick={() => onToggle(node.id)}
          aria-label={isExpanded ? 'Zwiń' : 'Rozwiń'}
        >
          {isExpanded ? <ChevronDown /> : <ChevronRight />}
        </button>

        {/* Folder + nazwa */}
        <span className="tree-folder-icon"><FolderIcon open={isExpanded} /></span>
        <span
          className="tree-node-name"
          onClick={() => onToggle(node.id)}
          title={node.name}
        >
          {node.name}
        </span>

        {/* Akcje (widoczne na hover) */}
        {hovered && (
          <div className="tree-actions">
            <button className="tree-action-btn" title="Dodaj podkategorię"
              onClick={e => { e.stopPropagation(); onAddSubcategory(node.id, node.name) }}>
              <span className="tree-action-icon"><PlusIcon /></span>
              <span className="tree-action-text">Kat.</span>
            </button>
            <button className="tree-action-btn tree-action-btn--topic" title="Dodaj temat"
              onClick={e => { e.stopPropagation(); onAddTopic(node.id, node.name) }}>
              <span className="tree-action-icon"><PlusIcon /></span>
              <span className="tree-action-text">Temat</span>
            </button>
          </div>
        )}
      </div>

      {/* ── Zawartość (jeśli rozwinięty) ── */}
      {isExpanded && (
        <div>
          {/* Podkategorie */}
          {(node.children ?? []).map(child => (
            <TreeNode
              key={child.id}
              node={child}
              depth={depth + 1}
              expanded={expanded}
              topics={topics}
              loadingTopics={loadingTopics}
              selectedTopicId={selectedTopicId}
              onToggle={onToggle}
              onSelectTopic={onSelectTopic}
              onAddSubcategory={onAddSubcategory}
              onAddTopic={onAddTopic}
            />
          ))}

          {/* Tematy */}
          {isLoading && (
            <div className="tree-loading" style={{ paddingLeft: 28 + indent + 16 }}>
              <span className="tree-spinner" /> Ładowanie...
            </div>
          )}
          {nodeTopics.map(topic => (
            <button
              key={topic.id}
              className={`tree-topic${selectedTopicId === topic.id ? ' tree-topic--active' : ''}`}
              style={{ paddingLeft: 28 + indent + 16 }}
              onClick={() => onSelectTopic(topic)}
            >
              <span className="tree-topic-icon"><NoteIcon /></span>
              <span className="tree-topic-name" title={topic.title}>{topic.title}</span>
              <span className="tree-topic-dot" title={topic.difficulty}
                style={{ color: DIFF_COLOR[topic.difficulty] }}>
                {DIFF_DOT[topic.difficulty]}
              </span>
            </button>
          ))}
          {!isLoading && nodeTopics.length === 0 && !hasChildren && (
            <p className="tree-empty" style={{ paddingLeft: 28 + indent + 16 }}>
              Brak tematów
            </p>
          )}
        </div>
      )}
    </div>
  )
}

/* ── Panel drzewa ── */
export default function CategoryTree({
  tree, expanded, topics, loadingTopics, selectedTopicId,
  onToggle, onSelectTopic, onAddRootCategory, onAddSubcategory, onAddTopic,
  loading,
}) {
  return (
    <aside className="tree-panel">
      {/* Nagłówek */}
      <div className="tree-header">
        <span className="tree-header-title">Baza wiedzy</span>
        <button className="tree-add-root" onClick={onAddRootCategory} title="Nowa kategoria">
          <PlusIcon />
        </button>
      </div>

      {/* Drzewo */}
      <div className="tree-content">
        {loading ? (
          <div className="tree-loading-full">
            <span className="tree-spinner" /> Ładowanie...
          </div>
        ) : tree.length === 0 ? (
          <div className="tree-empty-full">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none" style={{ color: 'var(--text-3)', margin: '0 auto 8px' }}>
              <path d="M4 10a2 2 0 012-2h7l2 2h11a2 2 0 012 2v12a2 2 0 01-2 2H6a2 2 0 01-2-2V10z" stroke="currentColor" strokeWidth="1.5" fill="none"/>
            </svg>
            <p>Brak kategorii</p>
            <button className="tree-create-first" onClick={onAddRootCategory}>
              Utwórz pierwszą →
            </button>
          </div>
        ) : (
          tree.map(node => (
            <TreeNode
              key={node.id}
              node={node}
              depth={0}
              expanded={expanded}
              topics={topics}
              loadingTopics={loadingTopics}
              selectedTopicId={selectedTopicId}
              onToggle={onToggle}
              onSelectTopic={onSelectTopic}
              onAddSubcategory={onAddSubcategory}
              onAddTopic={onAddTopic}
            />
          ))
        )}
      </div>

      <style>{`
        .tree-panel {
          width: 272px; flex-shrink: 0;
          border-right: 1px solid var(--border-1);
          background: #fff;
          display: flex; flex-direction: column;
          height: 100%;
        }
        .tree-header {
          display: flex; align-items: center; justify-content: space-between;
          padding: 14px 14px 10px;
          border-bottom: 1px solid var(--border-1);
          flex-shrink: 0;
        }
        .tree-header-title {
          font-size: 0.72rem; font-weight: 700;
          letter-spacing: 0.1em; text-transform: uppercase;
          color: var(--text-3);
        }
        .tree-add-root {
          width: 26px; height: 26px; border-radius: 6px;
          background: var(--accent-light); border: 1px solid var(--border-0);
          color: var(--accent); display: flex; align-items: center; justify-content: center;
          cursor: pointer; transition: background var(--t-fast);
        }
        .tree-add-root:hover { background: var(--accent-glow-strong); }

        .tree-content {
          flex: 1; overflow-y: auto; padding: 6px 0;
        }

        /* Wiersz kategorii */
        .tree-row {
          display: flex; align-items: center; gap: 4px;
          padding: 5px 8px 5px 12px;
          cursor: default; min-height: 30px;
          transition: background var(--t-fast);
          position: relative;
        }
        .tree-row:hover { background: var(--bg-2); }
        .tree-row--open > .tree-node-name { color: var(--text-0); }

        .tree-chevron {
          width: 18px; height: 18px; flex-shrink: 0;
          background: none; border: none; cursor: pointer;
          color: var(--text-3); display: flex; align-items: center; justify-content: center;
          border-radius: 4px; transition: color var(--t-fast), background var(--t-fast);
        }
        .tree-chevron:hover { color: var(--accent); background: var(--accent-light); }

        .tree-folder-icon { flex-shrink: 0; display: flex; align-items: center; color: var(--text-2); }

        .tree-node-name {
          flex: 1; font-size: 0.82rem; font-weight: 500;
          color: var(--text-1); cursor: pointer; overflow: hidden;
          text-overflow: ellipsis; white-space: nowrap;
          transition: color var(--t-fast);
        }
        .tree-node-name:hover { color: var(--text-0); }

        /* Akcje hover */
        .tree-actions {
          display: flex; gap: 3px; flex-shrink: 0;
        }
        .tree-action-btn {
          display: flex; align-items: center; gap: 2px;
          padding: 3px 6px; border-radius: 5px; border: 1px solid var(--border-1);
          background: #fff; color: var(--text-2); font-size: 0.65rem; font-weight: 600;
          cursor: pointer; white-space: nowrap;
          transition: all var(--t-fast);
        }
        .tree-action-btn:hover { background: var(--accent-light); color: var(--accent); border-color: var(--border-0); }
        .tree-action-btn--topic:hover { background: #f0fdf4; color: #16a34a; border-color: rgba(22,163,74,.25); }
        .tree-action-icon { display: flex; align-items: center; }
        .tree-action-text { font-size: 0.62rem; }

        /* Temat (liść) */
        .tree-topic {
          display: flex; align-items: center; gap: 7px;
          width: 100%; padding: 5px 10px 5px 12px;
          background: none; border: none; text-align: left;
          cursor: pointer; min-height: 28px;
          transition: background var(--t-fast);
        }
        .tree-topic:hover { background: var(--bg-2); }
        .tree-topic--active { background: var(--accent-light) !important; }
        .tree-topic--active .tree-topic-name { color: var(--accent); font-weight: 600; }
        .tree-topic-icon { color: var(--text-3); flex-shrink: 0; display: flex; }
        .tree-topic-name {
          flex: 1; font-size: 0.8rem; color: var(--text-1); overflow: hidden;
          text-overflow: ellipsis; white-space: nowrap;
        }
        .tree-topic-dot { font-size: 0.55rem; flex-shrink: 0; }

        /* Stany */
        .tree-loading {
          display: flex; align-items: center; gap: 6px;
          font-size: 0.75rem; color: var(--text-3); padding: 6px 12px;
        }
        .tree-empty { font-size: 0.73rem; color: var(--text-3); padding: 4px 12px; font-style: italic; }
        .tree-spinner {
          width: 10px; height: 10px; border-radius: 50%;
          border: 1.5px solid var(--border-1); border-top-color: var(--accent);
          animation: spin .6s linear infinite; flex-shrink: 0;
        }
        .tree-loading-full {
          display: flex; align-items: center; gap: 8px;
          padding: 24px 16px; font-size: 0.8rem; color: var(--text-2);
        }
        .tree-empty-full {
          padding: 32px 16px; text-align: center;
          font-size: 0.8rem; color: var(--text-3);
        }
        .tree-create-first {
          display: inline-block; margin-top: 8px;
          font-size: 0.78rem; color: var(--accent); font-weight: 600;
          background: none; border: none; cursor: pointer;
          transition: opacity .15s;
        }
        .tree-create-first:hover { opacity: .75; }
      `}</style>
    </aside>
  )
}
