import { describe, it, expect, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import CategoryTree from '../components/knowledge/CategoryTree'

const TREE = [
  {
    id: 'cat-a', name: 'Kategoria A', topicCount: 2,
    children: [
      { id: 'sub-a1', name: 'Podkategoria A1', topicCount: 2, children: [] },
    ],
  },
]

const TOPICS = {
  'sub-a1': [
    { id: 't1', title: 'Instrukcja uruchamiania aplikacji A', status: 'NOTE_ADDED', detailLevel: 'MEDIUM' },
    { id: 't2', title: 'Temat bez notatki', status: 'NEW', detailLevel: 'MEDIUM' },
  ],
}

function baseProps(overrides = {}) {
  return {
    tree: TREE,
    expanded: new Set(['cat-a', 'sub-a1']),
    topics: TOPICS,
    loadingTopics: new Set(),
    selectedTopicId: null,
    loading: false,
    onToggle: vi.fn(),
    onSelectTopic: vi.fn(),
    onAddRootCategory: vi.fn(),
    onAddSubcategory: vi.fn(),
    onAddTopic: vi.fn(),
    bulkMode: false,
    onToggleBulkMode: vi.fn(),
    selectedIds: new Set(),
    onToggleSelect: vi.fn(),
    onClearSelection: vi.fn(),
    onSelectVisible: vi.fn(),
    onRequestBulkPdf: vi.fn(),
    bulkPdfBusy: false,
    ...overrides,
  }
}

describe('CategoryTree — tryb "Druk masowy"', () => {
  it('domyślnie (tryb wyłączony) nie pokazuje checkboxów przy tematach ani przycisku PDF', () => {
    render(<CategoryTree {...baseProps()} />)
    expect(document.querySelectorAll('.tn-topic-checkbox')).toHaveLength(0)
    expect(screen.queryByText(/^PDF \(/)).not.toBeInTheDocument()
  })

  it('kliknięcie nazwy tematu otwiera notatkę, gdy tryb jest wyłączony', async () => {
    const props = baseProps()
    render(<CategoryTree {...props} />)
    await userEvent.click(screen.getByText('Instrukcja uruchamiania aplikacji A'))
    expect(props.onSelectTopic).toHaveBeenCalledTimes(1)
    expect(props.onSelectTopic).toHaveBeenCalledWith(expect.objectContaining({ id: 't1' }))
  })

  it('włączenie trybu pokazuje checkboxy przy tematach i pasek akcji z przyciskiem PDF', () => {
    render(<CategoryTree {...baseProps({ bulkMode: true })} />)
    expect(document.querySelectorAll('.tn-topic-checkbox')).toHaveLength(2)
    expect(screen.getByText('PDF (0)')).toBeInTheDocument()
  })

  it('przycisk PDF jest wyłączony przy zerowym wyborze i włączony, gdy coś jest zaznaczone', () => {
    const { rerender } = render(<CategoryTree {...baseProps({ bulkMode: true, selectedIds: new Set() })} />)
    expect(screen.getByText('PDF (0)').closest('button')).toBeDisabled()

    rerender(<CategoryTree {...baseProps({ bulkMode: true, selectedIds: new Set(['t1']) })} />)
    expect(screen.getByText('PDF (1)').closest('button')).toBeEnabled()
  })

  it('licznik wybranych tematów w przycisku PDF jest poprawny dla wielu zaznaczeń', () => {
    render(<CategoryTree {...baseProps({ bulkMode: true, selectedIds: new Set(['t1', 't2']) })} />)
    expect(screen.getByText('PDF (2)')).toBeInTheDocument()
  })

  it('kliknięcie checkboxa zaznacza temat, ale NIE otwiera notatki', async () => {
    const props = baseProps({ bulkMode: true })
    render(<CategoryTree {...props} />)
    const checkbox = screen.getByRole('checkbox', { name: /Zaznacz „Instrukcja uruchamiania aplikacji A”/ })
    await userEvent.click(checkbox)
    expect(props.onToggleSelect).toHaveBeenCalledTimes(1)
    expect(props.onToggleSelect).toHaveBeenCalledWith(expect.objectContaining({ id: 't1' }))
    expect(props.onSelectTopic).not.toHaveBeenCalled()
  })

  it('kliknięcie nazwy tematu w trybie masowym nadal otwiera notatkę (nie zaznacza)', async () => {
    const props = baseProps({ bulkMode: true })
    render(<CategoryTree {...props} />)
    await userEvent.click(screen.getByText('Instrukcja uruchamiania aplikacji A'))
    expect(props.onSelectTopic).toHaveBeenCalledTimes(1)
    expect(props.onToggleSelect).not.toHaveBeenCalled()
  })

  it('temat bez zapisanej notatki ma wyłączony checkbox z opisem "Brak zapisanej notatki"', () => {
    render(<CategoryTree {...baseProps({ bulkMode: true })} />)
    const checkbox = screen.getByRole('checkbox', { name: /Temat bez notatki.*brak zapisanej notatki/i })
    expect(checkbox).toBeDisabled()
  })

  it('checkbox zaznaczonego tematu jest widoczny również gdy ten temat jest aktualnie otwarty (selectedTopicId)', () => {
    render(<CategoryTree {...baseProps({ bulkMode: true, selectedTopicId: 't1', selectedIds: new Set(['t1']) })} />)
    const checkbox = screen.getByRole('checkbox', { name: /Zaznacz „Instrukcja uruchamiania aplikacji A”/ })
    expect(checkbox).toBeInTheDocument()
    expect(checkbox).toBeChecked()
  })

  it('wyłączenie trybu (checkbox "Druk masowy") woła onToggleBulkMode(false)', async () => {
    const props = baseProps({ bulkMode: true })
    render(<CategoryTree {...props} />)
    const toggle = screen.getByRole('checkbox', { name: 'Druk masowy' })
    await userEvent.click(toggle)
    expect(props.onToggleBulkMode).toHaveBeenCalledWith(false)
  })

  it('pasek akcji ma przyciski Wyczyść i Zaznacz widoczne, poprawnie podpięte', async () => {
    const props = baseProps({ bulkMode: true, selectedIds: new Set(['t1']) })
    render(<CategoryTree {...props} />)
    const bar = screen.getByText('Wybrano: 1').closest('.tree-bulk-bar')
    await userEvent.click(within(bar).getByText('Wyczyść'))
    expect(props.onClearSelection).toHaveBeenCalledTimes(1)
    await userEvent.click(within(bar).getByText('Zaznacz widoczne'))
    expect(props.onSelectVisible).toHaveBeenCalledTimes(1)
  })
})
