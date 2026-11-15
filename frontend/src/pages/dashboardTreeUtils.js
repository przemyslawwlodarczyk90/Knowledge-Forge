/**
 * Czysta funkcja (bez mutacji poprzedniego stanu) — aktualizuje topicCount węzła categoryId
 * ORAZ wszystkich jego przodków, bo topicCount w tym drzewie to suma REKURENCYJNA razem
 * z podkategoriami (zob. backend: CategoryTreeDto.fromRoot). Gdy targetValue jest podane
 * (autorytatywna wartość z bazy, zwrócona przez backend w tej samej transakcji co zmiana),
 * węzeł categoryId dostaje DOKŁADNIE tę wartość zamiast ślepego +delta; przodkowie zawsze
 * dostają +delta (backend nie liczy ich dokładnej wartości przy tej operacji — wyrówna się
 * przy najbliższym pełnym odświeżeniu drzewa/reconnect).
 */
export function updateCategoryTopicCount(tree, categoryId, delta, targetValue) {
  let found = false

  function walk(nodes) {
    let changedAny = false
    const next = nodes.map(node => {
      const children = node.children ? walk(node.children) : node.children
      const childrenChanged = children !== node.children

      if (String(node.id) === String(categoryId)) {
        found = true
        changedAny = true
        const newCount = targetValue != null ? targetValue : (node.topicCount ?? 0) + delta
        return { ...node, topicCount: Math.max(0, newCount), children }
      }
      if (childrenChanged) {
        changedAny = true
        return { ...node, topicCount: Math.max(0, (node.topicCount ?? 0) + delta), children }
      }
      return node
    })
    return changedAny ? next : nodes
  }

  const nextTree = walk(tree)
  return found ? nextTree : tree
}

/**
 * Zwraca wybrane tematy (`selectedIds`, Set) w kolejności ZGODNEJ z drzewem — kolejność kategorii,
 * potem podkategorii, potem tematów w obrębie każdej — a NIE w kolejności klikania checkboxów
 * (do trybu "Druk masowy", zob. CategoryTree/DashboardPage). Operuje wyłącznie na tym, co już
 * jest w stanie (`topics`, wypełniane przy rozwijaniu węzła) — węzły nigdy nierozwinięte po
 * prostu nie mają tu żadnych tematów do zebrania, więc nie mogą zawierać zaznaczenia w pierwszej
 * kolejności (checkbox nie istnieje, dopóki temat nie jest wyrenderowany).
 */
export function collectSelectedTopicsInTreeOrder(tree, topics, selectedIds) {
  const result = []
  function walk(nodes) {
    for (const node of nodes) {
      // Ta sama kolejność co w CategoryTree/TreeNode: podkategorie (dzieci) renderują się NAD
      // własnymi tematami danego węzła, więc odwiedzamy je w tej samej kolejności.
      if (node.children?.length) walk(node.children)
      for (const topic of topics[node.id] ?? []) {
        if (selectedIds.has(topic.id)) result.push(topic)
      }
    }
  }
  walk(tree)
  return result
}

/**
 * Ścieżka węzłów od korzenia do `categoryId` (włącznie), jako `[{id, name}, ...]` — do breadcrumb
 * w NotePanel i nagłówka notatki w PDF. Zwraca `[]`, gdy kategoria nie istnieje w drzewie.
 */
export function findPath(tree, categoryId, path = []) {
  for (const node of tree) {
    const current = [...path, { id: node.id, name: node.name }]
    if (node.id === categoryId) return current
    if (node.children?.length) {
      const found = findPath(node.children, categoryId, current)
      // Pusta tablica (sygnał "nie znaleziono" z rekurencji) jest w JS TRUTHY — `if (found)` tutaj
      // przerywałoby całe przeszukiwanie na pierwszej gałęzi, która NIE zawierała szukanej
      // kategorii, więc trzeba jawnie sprawdzić długość, żeby przejść do kolejnych węzłów.
      if (found.length) return found
    }
  }
  return []
}
