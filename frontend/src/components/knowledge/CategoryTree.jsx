import { useState } from 'react'

/* ── Ikony ── */
const ChevronRight = () => (
  <svg width="11" height="11" viewBox="0 0 11 11" fill="none">
    <path d="M3.5 2L7 5.5 3.5 9" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const ChevronDown = () => (
  <svg width="11" height="11" viewBox="0 0 11 11" fill="none">
    <path d="M2 3.5L5.5 7 9 3.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)
const NoteIcon = () => (
  <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
    <rect x="1.5" y="1" width="9" height="10" rx="1.5" stroke="currentColor" strokeWidth="1.3"/>
    <path d="M3.5 4h5M3.5 6.5h5M3.5 9h3" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
  </svg>
)
const PlusIcon = () => (
  <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
    <path d="M5 1.5v7M1.5 5h7" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
  </svg>
)

const DIFF_COLOR = { BASIC: '#16a34a', MEDIUM: '#d97706', HARD: '#dc2626' }

/* ── Węzeł drzewa ── */
function TreeNode({
  node, depth = 0,
  expanded, topics, loadingTopics, selectedTopicId,
  onToggle, onSelectTopic, onAddSubcategory, onAddTopic,
}) {
  const [hovered, setHovered] = useState(false)
  const isExpanded  = expanded.has(node.id)
  const nodeTopics  = topics[node.id] ?? []
  const isLoading   = loadingTopics.has(node.id)

  const isCategory    = depth === 0   // IT, HISTORIA
  const isSubcategory = depth === 1   // JAVA, SIECI, RENESANS

  return (
    <div className={`tn-block tn-block--d${depth}`}>

      {/* ── Wiersz węzła ── */}
      <div
        className={`tn-row tn-row--d${depth}${isExpanded ? ' tn-row--open' : ''}`}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      >
        <button className="tn-chevron" onClick={() => onToggle(node.id)}>
          {isExpanded ? <ChevronDown /> : <ChevronRight />}
        </button>

        <span
          className="tn-name"
          onClick={() => onToggle(node.id)}
          title={node.name}
        >
          {node.name}
        </span>

        {hovered && (
          <div className="tn-actions">
            {isCategory && (
              <button
                className="tn-act tn-act--sub"
                title="Dodaj podkategorię"
                onClick={e => { e.stopPropagation(); onAddSubcategory(node.id, node.name) }}
              >
                <PlusIcon /> <span>Podkat.</span>
              </button>
            )}
            {isSubcategory && (
              <button
                className="tn-act tn-act--topic"
                title="Dodaj temat / notatkę"
                onClick={e => { e.stopPropagation(); onAddTopic(node.id, node.name) }}
              >
                <PlusIcon /> <span>Temat</span>
              </button>
            )}
            {!isCategory && !isSubcategory && (
              /* głębiej niż 1 — daj oba */
              <>
                <button className="tn-act tn-act--sub" onClick={e => { e.stopPropagation(); onAddSubcategory(node.id, node.name) }}>
                  <PlusIcon /> <span>Podkat.</span>
                </button>
                <button className="tn-act tn-act--topic" onClick={e => { e.stopPropagation(); onAddTopic(node.id, node.name) }}>
                  <PlusIcon /> <span>Temat</span>
                </button>
              </>
            )}
          </div>
        )}
      </div>

      {/* ── Zawartość rozwinięta ── */}
      {isExpanded && (
        <div className="tn-children">
          {/* Podkategorie / dzieci */}
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

          {/* Tematy (liście) */}
          {isLoading && (
            <div className="tn-loading">
              <span className="tn-spinner" /> Ładowanie...
            </div>
          )}
          {!isLoading && nodeTopics.map(topic => (
            <button
              key={topic.id}
              className={`tn-topic${selectedTopicId === topic.id ? ' tn-topic--active' : ''}`}
              onClick={() => onSelectTopic(topic)}
            >
              <span className="tn-topic-icon"><NoteIcon /></span>
              <span className="tn-topic-name" title={topic.title}>{topic.title}</span>
              <span
                className="tn-topic-diff"
                title={topic.difficulty}
                style={{ background: DIFF_COLOR[topic.difficulty] }}
              />
            </button>
          ))}
          {!isLoading && nodeTopics.length === 0 && (node.children ?? []).length === 0 && (
            <p className="tn-empty-hint">
              {isCategory ? 'Brak podkategorii' : 'Brak tematów'}
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
      <div className="tree-head">
        <span className="tree-head-title">Baza wiedzy</span>
        <button className="tree-add-btn" onClick={onAddRootCategory} title="Nowa kategoria (np. IT, HISTORIA)">
          <PlusIcon />
        </button>
      </div>

      {/* Legenda głębokości */}
      <div className="tree-legend">
        <span className="tree-legend-item tree-legend-item--cat">Kategoria</span>
        <span className="tree-legend-sep">›</span>
        <span className="tree-legend-item tree-legend-item--sub">Podkategoria</span>
        <span className="tree-legend-sep">›</span>
        <span className="tree-legend-item tree-legend-item--topic">Temat</span>
      </div>

      {/* Drzewo */}
      <div className="tree-body">
        {loading ? (
          <div className="tree-status"><span className="tn-spinner" /> Ładowanie...</div>
        ) : tree.length === 0 ? (
          <div className="tree-empty">
            <svg width="36" height="36" viewBox="0 0 36 36" fill="none" style={{ color: 'var(--border-1)' }}>
              <path d="M4 10a2 2 0 012-2h8l2 2h14a2 2 0 012 2v14a2 2 0 01-2 2H6a2 2 0 01-2-2V10z" stroke="currentColor" strokeWidth="1.5"/>
            </svg>
            <p className="tree-empty-title">Brak kategorii</p>
            <p className="tree-empty-sub">Zacznij od dodania kategorii<br/>np. <em>IT</em> lub <em>HISTORIA</em></p>
            <button className="tree-empty-btn" onClick={onAddRootCategory}>
              Dodaj kategorię →
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
          width: 268px; flex-shrink: 0;
          border-right: 1px solid var(--border-1);
          background: var(--bg-2);
          display: flex; flex-direction: column;
          height: 100%;
        }

        /* Nagłówek */
        .tree-head {
          display: flex; align-items: center; justify-content: space-between;
          padding: 13px 14px 10px;
          border-bottom: 1px solid var(--border-1);
          background: #fff;
          flex-shrink: 0;
        }
        .tree-head-title {
          font-size: 0.68rem; font-weight: 800;
          letter-spacing: 0.12em; text-transform: uppercase;
          color: var(--text-2);
        }
        .tree-add-btn {
          width: 24px; height: 24px; border-radius: 6px;
          background: var(--accent); border: none;
          color: #fff; display: flex; align-items: center; justify-content: center;
          cursor: pointer; transition: background var(--t-fast);
        }
        .tree-add-btn:hover { background: var(--accent-dim); }

        /* Legenda */
        .tree-legend {
          display: flex; align-items: center; gap: 4px;
          padding: 6px 14px 8px;
          border-bottom: 1px solid var(--border-1);
          background: #fff;
          flex-shrink: 0;
        }
        .tree-legend-item {
          font-family: var(--font-mono); font-size: 0.58rem;
          font-weight: 700; letter-spacing: 0.07em; text-transform: uppercase;
        }
        .tree-legend-item--cat   { color: var(--accent); }
        .tree-legend-item--sub   { color: var(--text-2); }
        .tree-legend-item--topic { color: var(--text-3); }
        .tree-legend-sep { color: var(--text-3); font-size: 0.65rem; }

        /* Ciało */
        .tree-body {
          flex: 1; overflow-y: auto;
          padding: 8px 0 16px;
        }

        /* ── Blok węzła ── */
        .tn-block { }

        /* ── Wiersz ── */
        .tn-row {
          display: flex; align-items: center; gap: 5px;
          cursor: default; min-height: 32px;
          transition: background var(--t-fast);
          padding-right: 8px;
          position: relative;
        }
        .tn-row:hover { background: rgba(0,0,0,0.03); }

        /* Depth 0 — Kategoria (IT, HISTORIA) */
        .tn-row--d0 {
          padding-left: 10px;
          border-bottom: 1px solid var(--border-2);
          margin-top: 4px;
        }
        .tn-row--d0:first-child { margin-top: 0; }
        .tn-row--d0 .tn-name {
          font-size: 0.78rem; font-weight: 800;
          text-transform: uppercase; letter-spacing: 0.06em;
          color: var(--text-0);
        }

        /* Depth 1 — Podkategoria (JAVA, SIECI) */
        .tn-row--d1 { padding-left: 22px; }
        .tn-row--d1 .tn-name {
          font-size: 0.80rem; font-weight: 600;
          color: var(--text-1);
        }

        /* Depth 2+ */
        .tn-row--d2 { padding-left: 38px; }
        .tn-row--d2 .tn-name {
          font-size: 0.79rem; font-weight: 500;
          color: var(--text-2);
        }

        .tn-chevron {
          width: 18px; height: 18px; flex-shrink: 0;
          background: none; border: none; cursor: pointer;
          color: var(--text-3);
          display: flex; align-items: center; justify-content: center;
          border-radius: 4px; transition: color var(--t-fast), background var(--t-fast);
        }
        .tn-chevron:hover { color: var(--accent); background: var(--accent-light); }

        .tn-name {
          flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
          cursor: pointer; transition: color var(--t-fast);
        }
        .tn-name:hover { color: var(--accent) !important; }

        /* Akcje hover */
        .tn-actions {
          display: flex; gap: 3px; flex-shrink: 0;
        }
        .tn-act {
          display: flex; align-items: center; gap: 2px;
          padding: 3px 7px; border-radius: 5px;
          font-size: 0.64rem; font-weight: 700;
          cursor: pointer; white-space: nowrap;
          border: 1px solid; transition: all var(--t-fast);
        }
        .tn-act--sub {
          background: var(--accent-light); color: var(--accent);
          border-color: var(--border-0);
        }
        .tn-act--sub:hover { background: var(--accent-glow-strong); }
        .tn-act--topic {
          background: #f0fdf4; color: #16a34a;
          border-color: rgba(22,163,74,.25);
        }
        .tn-act--topic:hover { background: #dcfce7; }

        /* Dzieci */
        .tn-children { }

        /* Temat (liść) */
        .tn-topic {
          display: flex; align-items: center; gap: 8px;
          width: 100%; padding: 6px 10px 6px 46px;
          background: none; border: none; text-align: left;
          cursor: pointer; min-height: 28px;
          transition: background var(--t-fast);
          border-left: 2px solid transparent;
        }
        .tn-topic:hover { background: rgba(37,99,235,0.04); }
        .tn-topic--active {
          background: var(--accent-light) !important;
          border-left-color: var(--accent);
        }
        .tn-topic--active .tn-topic-name { color: var(--accent); font-weight: 600; }
        .tn-topic-icon { color: var(--text-3); flex-shrink: 0; display: flex; }
        .tn-topic-name {
          flex: 1; font-size: 0.78rem; color: var(--text-1);
          overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
          transition: color var(--t-fast);
        }
        .tn-topic-diff {
          width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0;
        }

        /* Stany */
        .tn-loading {
          display: flex; align-items: center; gap: 6px;
          font-size: 0.74rem; color: var(--text-3); padding: 6px 46px;
        }
        .tn-spinner {
          width: 10px; height: 10px; border-radius: 50%;
          border: 1.5px solid var(--border-1); border-top-color: var(--accent);
          animation: spin .6s linear infinite; flex-shrink: 0;
        }
        .tn-empty-hint {
          font-size: 0.71rem; color: var(--text-3); padding: 4px 46px;
          font-style: italic;
        }

        /* Puste drzewo */
        .tree-status {
          display: flex; align-items: center; gap: 8px;
          padding: 20px 14px; font-size: 0.8rem; color: var(--text-2);
        }
        .tree-empty {
          padding: 28px 16px; text-align: center;
        }
        .tree-empty svg { margin: 0 auto 12px; }
        .tree-empty-title { font-size: 0.88rem; font-weight: 700; color: var(--text-2); margin-bottom: 6px; }
        .tree-empty-sub   { font-size: 0.76rem; color: var(--text-3); line-height: 1.6; margin-bottom: 14px; }
        .tree-empty-sub em { font-style: normal; color: var(--accent); font-weight: 600; }
        .tree-empty-btn {
          font-size: 0.78rem; font-weight: 700; color: var(--accent);
          background: var(--accent-light); border: 1px solid var(--border-0);
          padding: 7px 14px; border-radius: var(--radius-md); cursor: pointer;
          transition: background var(--t-fast);
        }
        .tree-empty-btn:hover { background: var(--accent-glow-strong); }
      `}</style>
    </aside>
  )
}
