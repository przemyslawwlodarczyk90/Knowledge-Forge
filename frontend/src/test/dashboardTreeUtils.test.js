import { describe, it, expect } from 'vitest'
import { updateCategoryTopicCount, collectSelectedTopicsInTreeOrder, findPath } from '../pages/dashboardTreeUtils'

const TREE = [
  {
    id: 'cat-a', name: 'Kategoria A', topicCount: 0,
    children: [
      { id: 'sub-a1', name: 'Podkategoria A1', topicCount: 0, children: [] },
      { id: 'sub-a2', name: 'Podkategoria A2', topicCount: 0, children: [] },
    ],
  },
  {
    id: 'cat-b', name: 'Kategoria B', topicCount: 0,
    children: [
      { id: 'sub-b1', name: 'Podkategoria B1', topicCount: 0, children: [] },
    ],
  },
]

function topic(id, status = 'NOTE_ADDED') {
  return { id, title: `Topic ${id}`, status }
}

describe('collectSelectedTopicsInTreeOrder', () => {
  it('returns selected topics in tree order, not selection/click order', () => {
    const topics = {
      'sub-a1': [topic('t1'), topic('t2')],
      'sub-a2': [topic('t3')],
      'sub-b1': [topic('t4')],
    }
    // Zaznaczone w odwrotnej kolejności niż w drzewie — wynik MUSI mimo to iść po drzewie.
    const selected = new Set(['t4', 't1', 't3'])
    const result = collectSelectedTopicsInTreeOrder(TREE, topics, selected)
    expect(result.map(t => t.id)).toEqual(['t1', 't3', 't4'])
  })

  it('never includes a topic from a category that was never expanded/loaded', () => {
    const topics = { 'sub-a1': [topic('t1')] } // sub-a2/sub-b1 nigdy nie rozwinięte -> brak w `topics`
    const selected = new Set(['t1', 'ghost-id']) // 'ghost-id' teoretycznie mogło zostać zaznaczone gdzieś indziej
    const result = collectSelectedTopicsInTreeOrder(TREE, topics, selected)
    expect(result.map(t => t.id)).toEqual(['t1'])
  })

  it('returns an empty array when nothing is selected', () => {
    const topics = { 'sub-a1': [topic('t1')] }
    expect(collectSelectedTopicsInTreeOrder(TREE, topics, new Set())).toEqual([])
  })

  it('a Set cannot contain the same topic id twice, so double-toggling is naturally idempotent', () => {
    const ids = new Set()
    ids.add('t1'); ids.add('t1'); ids.add('t1')
    expect(ids.size).toBe(1)
    const topics = { 'sub-a1': [topic('t1')] }
    expect(collectSelectedTopicsInTreeOrder(TREE, topics, ids)).toHaveLength(1)
  })
})

describe('findPath', () => {
  it('finds a category under the FIRST root even when later roots also have children', () => {
    expect(findPath(TREE, 'cat-a').map(n => n.id)).toEqual(['cat-a'])
  })

  it('REGRESSION: finds a category nested under a LATER root, when an earlier root also has children '
    + '(empty array is truthy in JS — a naive `if (recursiveResult)` check stops at the first root '
    + "that doesn't contain the target instead of checking the rest, so this used to always return "
    + '[] whenever the match wasn\'t in the very first branch — broke the breadcrumb/PDF header for '
    + 'almost any real multi-category tree)', () => {
    const result = findPath(TREE, 'sub-b1')
    expect(result.map(n => n.id)).toEqual(['cat-b', 'sub-b1'])
  })

  it('finds a deeply nested category and returns the full ancestor chain in order', () => {
    expect(findPath(TREE, 'sub-a2').map(n => n.id)).toEqual(['cat-a', 'sub-a2'])
  })

  it('returns an empty array for an id that does not exist in the tree', () => {
    expect(findPath(TREE, 'does-not-exist')).toEqual([])
  })
})

describe('updateCategoryTopicCount (regression — unrelated to bulk mode, still exercised here)', () => {
  it('sets the exact target value on the touched node and bumps ancestors by delta', () => {
    const tree = [{ id: 'root', topicCount: 3, children: [{ id: 'child', topicCount: 3, children: [] }] }]
    const next = updateCategoryTopicCount(tree, 'child', 1, 4)
    expect(next[0].children[0].topicCount).toBe(4)
    expect(next[0].topicCount).toBe(4)
  })
})
