import { useState, useRef, useCallback, useEffect } from 'react'

/** Klasa na <body> nadawana podczas przeciągania — kursor + blokada zaznaczania (globals.css). */
const DRAG_CLASS = { horizontal: 'resizing-col', vertical: 'resizing-row' }

/**
 * Hook dla przeciąganego separatora, który zmienia rozmiar sąsiedniego panelu
 * (Pointer Events: pointerdown/pointermove/pointerup, ze sprzątaniem listenerów).
 *
 * `orientation` decyduje o osi przeciągania:
 *  - 'horizontal' (domyślnie) — separator pionowy, przeciąganie po osi X, zmienia SZEROKOŚĆ
 *    (np. sidebar ↔ treść, drzewo kategorii ↔ notatka);
 *  - 'vertical' — separator poziomy, przeciąganie po osi Y, zmienia WYSOKOŚĆ
 *    (np. nagłówek wpisu ↔ treść notatki).
 * Poza wyborem osi (clientX/clientY, width/height, nazwa klasy na <body>) logika jest identyczna.
 *
 * Rozmiar jest ograniczany do [min, max], gdzie górna granica może zostać dodatkowo
 * zawężona przez `maxRatio` (ułamek szerokości/wysokości `containerRef`, np. 0.5 = połowa) oraz
 * `extraMin` (ile miejsca zostawić sąsiadowi po drugiej stronie separatora).
 *
 * Rozmiar żyje WYŁĄCZNIE w pamięci komponentu — nie jest zapisywany między odświeżeniami strony
 * (świadomie: każde odświeżenie ma przywracać domyślny układ, zob. `defaultSize`). W ramach tej
 * samej sesji (bez przeładowania strony) rozmiar oczywiście przetrwa np. przełączanie się między
 * tematami, dopóki komponent trzymający ten hook się nie odmontuje. Przy zmianie rozmiaru okna
 * (albo dowolnej innej zmianie min/max/maxRatio/extraMin) rozmiar jest na bieżąco przycinany do
 * aktualnie dostępnej przestrzeni.
 *
 * @param {object} opts
 * @param {number} opts.defaultSize domyślny rozmiar (px) — startowy i przywracany dwuklikiem separatora
 * @param {number} opts.min         dolne ograniczenie (px)
 * @param {number} [opts.max]       górne ograniczenie (px), domyślnie bez limitu
 * @param {number} [opts.maxRatio]  górne ograniczenie jako ułamek wymiaru containerRef (0–1)
 * @param {number} [opts.extraMin]  miejsce (px) rezerwowane dla panelu po drugiej stronie separatora
 * @param {import('react').RefObject<HTMLElement>} [opts.containerRef] kontener, względem którego liczone są maxRatio/extraMin
 * @param {'horizontal'|'vertical'} [opts.orientation] oś przeciągania — patrz opis wyżej
 */
export function useResizablePanel({ defaultSize, min, max, maxRatio, extraMin, containerRef, orientation = 'horizontal' }) {
  const isVertical = orientation === 'vertical'

  const resolveMax = useCallback(() => {
    let hi = max ?? Infinity
    const rect = containerRef?.current?.getBoundingClientRect()
    const containerSize = rect ? (isVertical ? rect.height : rect.width) : undefined
    if (containerSize) {
      if (maxRatio != null) hi = Math.min(hi, containerSize * maxRatio)
      if (extraMin != null) hi = Math.min(hi, containerSize - extraMin)
    }
    return Math.max(hi, min) // dolna granica ma zawsze pierwszeństwo, nawet gdy kontener jest bardzo ciasny
  }, [max, maxRatio, extraMin, containerRef, min, isVertical])

  const clamp = useCallback(value => Math.min(Math.max(value, min), resolveMax()), [min, resolveMax])

  // Stan startowy przycinamy TYLKO do statycznych min/max — resolveMax()/clamp() czytałyby
  // containerRef.current, a refy nie wolno czytać w trakcie renderu (i tak jest to bez znaczenia:
  // przed zamontowaniem ref jest pusty, więc maxRatio/extraMin i tak nie miałyby czego zmierzyć).
  // Pełne przycięcie robi efekt tuż poniżej, zaraz po zamontowaniu.
  const [size, setSize] = useState(() => Math.min(Math.max(defaultSize, min), max ?? Infinity))
  const [dragging, setDragging] = useState(false)
  const dragStartRef = useRef(null)
  const stopDragRef = useRef(null)

  // Dociśnij rozmiar do aktualnych granic za każdym razem, gdy `clamp` faktycznie się zmienia —
  // czyli i od razu po zamontowaniu (kontener mierzy się dopiero wtedy), i za każdym razem, gdy
  // min/max/maxRatio/extraMin się zmieniają. Dla konsumentów ze STAŁYMI granicami (sidebar, drzewo
  // kategorii, wysokość nagłówka notatki) `clamp` po starcie już się nie zmienia, więc efekt po
  // prostu nie odpala się ponownie — zachowanie identyczne jak wcześniej. Dla konsumentów z
  // granicami wyliczanymi DYNAMICZNIE z zewnętrznego pomiaru DOM (np. separator notatki
  // nachodzący na nagłówek — jego min/max zależą od aktualnie zmierzonej wysokości elementu nad
  // nim) to dodatkowe re-clampowanie jest konieczne, żeby przeciągnięty rozmiar nie "wystawał"
  // poza granice, gdy ten zewnętrzny pomiar się zmieni z innego powodu niż resize okna.
  useEffect(() => {
    // Świadomie: to synchronizacja z zewnętrznym pomiarem DOM (rozmiar kontenera), nie coś
    // liczalne w trakcie renderu — clamp() sam jest no-opem (Object.is-równy poprzedniej wartości),
    // gdy granice się nie zmieniły, więc to nie powoduje kaskady renderów w praktyce.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setSize(prev => clamp(prev))
  }, [clamp])

  // Zmiana rozmiaru okna → przytnij aktualny rozmiar do aktualnie dostępnej przestrzeni.
  useEffect(() => {
    const onResize = () => setSize(prev => clamp(prev))
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [clamp])

  // Gdyby komponent odmontował się w trakcie przeciągania — posprzątaj globalne listenery.
  useEffect(() => () => stopDragRef.current?.(), [])

  const handlePointerDown = useCallback((e) => {
    if (e.pointerType === 'touch') return // na dotyku separator jest wyłączony
    e.preventDefault()
    const dragClass = DRAG_CLASS[orientation] ?? DRAG_CLASS.horizontal
    dragStartRef.current = { pos: isVertical ? e.clientY : e.clientX, size }
    setDragging(true)
    document.body.classList.add(dragClass)

    const onMove = (ev) => {
      if (!dragStartRef.current) return
      const pos = isVertical ? ev.clientY : ev.clientX
      const delta = pos - dragStartRef.current.pos
      setSize(clamp(dragStartRef.current.size + delta))
    }
    const onUp = () => {
      dragStartRef.current = null
      setDragging(false)
      document.body.classList.remove(dragClass)
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
      window.removeEventListener('pointercancel', onUp)
      stopDragRef.current = null
    }
    stopDragRef.current = onUp

    // Nasłuch na window (nie na samym uchwycie), żeby przeciąganie działało
    // również wtedy, gdy kursor wyjedzie poza separator.
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
    window.addEventListener('pointercancel', onUp)
  }, [size, clamp, isVertical, orientation])

  const reset = useCallback(() => setSize(clamp(defaultSize)), [clamp, defaultSize])

  return { size, dragging, handlePointerDown, reset }
}
