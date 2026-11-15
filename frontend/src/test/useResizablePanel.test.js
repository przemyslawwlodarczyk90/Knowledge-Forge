import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { useResizablePanel } from '../hooks/useResizablePanel'

/** Fałszywy ref kontenera — zwraca kontrolowany przez test rozmiar zamiast prawdziwego layoutu DOM. */
function makeContainerRef(width, height = width) {
  return { current: { getBoundingClientRect: () => ({ width, height }) } }
}

function firePointer(type, target, props = {}) {
  const evt = new Event(type, { bubbles: true })
  Object.assign(evt, { pointerType: 'mouse', clientX: 0, clientY: 0, ...props })
  target.dispatchEvent(evt)
  return evt
}

beforeEach(() => {
  localStorage.clear()
})

afterEach(() => {
  document.body.className = ''
})

describe('useResizablePanel', () => {
  it('startuje zawsze z defaultSize', () => {
    const { result } = renderHook(() => useResizablePanel({ defaultSize: 200, min: 100, max: 300 }))
    expect(result.current.size).toBe(200)
  })

  it('NIE odtwarza rozmiaru z localStorage nawet, gdy pod tym samym kluczem coś tam leży — każde świeże zamontowanie ma wracać na domyślne miejsce', () => {
    localStorage.setItem('kf.jakisResizable', '250')
    const { result } = renderHook(() => useResizablePanel({ defaultSize: 200, min: 100, max: 300 }))
    expect(result.current.size).toBe(200)
  })

  it('NIE zapisuje rozmiaru w localStorage po zmianie (żadnego klucza) — po odświeżeniu strony każdy resizable ma wrócić na swoje miejsce', () => {
    const { result } = renderHook(() => useResizablePanel({ defaultSize: 200, min: 100, max: 300 }))
    act(() => { result.current.handlePointerDown({ pointerType: 'mouse', clientX: 0, clientY: 0, preventDefault: () => {} }) })
    act(() => { firePointer('pointermove', window, { clientX: 40, clientY: 0 }) })
    act(() => { firePointer('pointerup', window) })
    expect(result.current.size).toBe(240)
    expect(localStorage.length).toBe(0)
  })

  it('przycina defaultSize do [min, max], gdyby wykraczał poza granice', () => {
    const { result } = renderHook(() => useResizablePanel({ defaultSize: 9999, min: 100, max: 300 }))
    expect(result.current.size).toBe(300)
  })

  it('reset() przywraca defaultSize (w granicach)', () => {
    const { result } = renderHook(() => useResizablePanel({ defaultSize: 200, min: 100, max: 300 }))
    act(() => { result.current.handlePointerDown({ pointerType: 'mouse', clientX: 0, clientY: 0, preventDefault: () => {} }) })
    act(() => { firePointer('pointermove', window, { clientX: 50, clientY: 0 }) })
    act(() => { firePointer('pointerup', window) })
    expect(result.current.size).toBe(250)
    act(() => { result.current.reset() })
    expect(result.current.size).toBe(200)
  })

  it('orientation="horizontal" (domyślna) — przeciąganie po osi X, kursor col-resize semantyka delta X', () => {
    const { result } = renderHook(() => useResizablePanel({ defaultSize: 200, min: 50, max: 400, orientation: 'horizontal' }))
    const el = document.createElement('div')
    document.body.appendChild(el)

    act(() => { result.current.handlePointerDown({ pointerType: 'mouse', clientX: 100, clientY: 999, preventDefault: () => {} }) })
    expect(result.current.dragging).toBe(true)
    act(() => { firePointer('pointermove', window, { clientX: 150, clientY: 5000 }) }) // clientY zignorowany dla orientation horizontal
    expect(result.current.size).toBe(250) // 200 + (150-100)
    act(() => { firePointer('pointerup', window) })
    expect(result.current.dragging).toBe(false)
  })

  it('orientation="vertical" — przeciąganie po osi Y (clientY), nie X', () => {
    const { result } = renderHook(() => useResizablePanel({ defaultSize: 200, min: 50, max: 400, orientation: 'vertical' }))

    act(() => { result.current.handlePointerDown({ pointerType: 'mouse', clientX: 999, clientY: 100, preventDefault: () => {} }) })
    act(() => { firePointer('pointermove', window, { clientX: 5000, clientY: 130 }) }) // clientX zignorowany
    expect(result.current.size).toBe(230) // 200 + (130-100)
  })

  it('ignoruje pointerType "touch" (separator wyłączony na dotyku)', () => {
    const { result } = renderHook(() => useResizablePanel({ defaultSize: 200, min: 50, max: 400 }))
    act(() => { result.current.handlePointerDown({ pointerType: 'touch', clientX: 100, clientY: 100, preventDefault: () => {} }) })
    expect(result.current.dragging).toBe(false)
  })

  it('dodaje klasę resizing-col na <body> podczas przeciągania (horizontal) i usuwa po puszczeniu', () => {
    const { result } = renderHook(() => useResizablePanel({ defaultSize: 200, min: 50, max: 400, orientation: 'horizontal' }))
    act(() => { result.current.handlePointerDown({ pointerType: 'mouse', clientX: 0, clientY: 0, preventDefault: () => {} }) })
    expect(document.body.classList.contains('resizing-col')).toBe(true)
    act(() => { firePointer('pointerup', window) })
    expect(document.body.classList.contains('resizing-col')).toBe(false)
  })

  it('dodaje klasę resizing-row na <body> podczas przeciągania (vertical)', () => {
    const { result } = renderHook(() => useResizablePanel({ defaultSize: 200, min: 50, max: 400, orientation: 'vertical' }))
    act(() => { result.current.handlePointerDown({ pointerType: 'mouse', clientX: 0, clientY: 0, preventDefault: () => {} }) })
    expect(document.body.classList.contains('resizing-row')).toBe(true)
    act(() => { firePointer('pointercancel', window) })
    expect(document.body.classList.contains('resizing-row')).toBe(false)
  })

  it('przeciąganie działa również po wyjechaniu kursorem poza uchwyt (listenery na window, nie na elemencie)', () => {
    const { result } = renderHook(() => useResizablePanel({ defaultSize: 200, min: 50, max: 400 }))
    act(() => { result.current.handlePointerDown({ pointerType: 'mouse', clientX: 0, clientY: 0, preventDefault: () => {} }) })
    // pointermove wystrzelony z dowolnego miejsca w drzewie (symulacja kursora poza uchwytem) —
    // nasłuch jest na window, więc i tak zadziała.
    act(() => { firePointer('pointermove', document.body, { clientX: 40, clientY: 0 }) })
    expect(result.current.size).toBe(240)
  })

  it('przycina rozmiar do maxRatio*containerWidth (poziomo)', () => {
    const containerRef = makeContainerRef(1000)
    const { result } = renderHook(() => useResizablePanel({
      defaultSize: 200, min: 50, maxRatio: 0.3, containerRef, orientation: 'horizontal',
    }))
    act(() => { result.current.handlePointerDown({ pointerType: 'mouse', clientX: 0, clientY: 0, preventDefault: () => {} }) })
    act(() => { firePointer('pointermove', window, { clientX: 5000, clientY: 0 }) }) // próba przeciągnięcia daleko poza granicę
    expect(result.current.size).toBe(300) // 0.3 * 1000
  })

  it('extraMin rezerwuje miejsce sąsiadowi po drugiej stronie separatora', () => {
    const containerRef = makeContainerRef(1000)
    const { result } = renderHook(() => useResizablePanel({
      defaultSize: 200, min: 50, extraMin: 700, containerRef, orientation: 'horizontal',
    }))
    act(() => { result.current.handlePointerDown({ pointerType: 'mouse', clientX: 0, clientY: 0, preventDefault: () => {} }) })
    act(() => { firePointer('pointermove', window, { clientX: 5000, clientY: 0 }) })
    expect(result.current.size).toBe(300) // 1000 - 700
  })

  it('re-clampuje reaktywnie, gdy min/max się zmieniają MIĘDZY renderami (nie tylko przy resize okna)', () => {
    const { result, rerender } = renderHook(
      ({ max }) => useResizablePanel({ defaultSize: 200, min: 50, max, orientation: 'vertical' }),
      { initialProps: { max: 400 } },
    )
    act(() => { result.current.handlePointerDown({ pointerType: 'mouse', clientX: 0, clientY: 0, preventDefault: () => {} }) })
    act(() => { firePointer('pointermove', window, { clientX: 0, clientY: 5000 }) }) // rozciągnij do maxa (400)
    act(() => { firePointer('pointerup', window) })
    expect(result.current.size).toBe(400)

    // Granica max nagle maleje (np. zmierzony na żywo element się skurczył) — size powinien się
    // sam przyciąć do nowego max, bez żadnej dodatkowej interakcji użytkownika.
    rerender({ max: 150 })
    expect(result.current.size).toBe(150)
  })

  it('dolna granica (min) zawsze ma pierwszeństwo, nawet gdy kontener jest bardzo ciasny', () => {
    const containerRef = makeContainerRef(10) // absurdalnie mały kontener
    const { result } = renderHook(() => useResizablePanel({
      defaultSize: 200, min: 100, maxRatio: 0.1, containerRef, orientation: 'horizontal',
    }))
    expect(result.current.size).toBeGreaterThanOrEqual(100)
  })
})
