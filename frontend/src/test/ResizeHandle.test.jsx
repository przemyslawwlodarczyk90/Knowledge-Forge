import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import ResizeHandle from '../components/layout/ResizeHandle'

describe('ResizeHandle', () => {
  it('orientation="horizontal" (domyślna) — kursor col-resize, aria-orientation vertical (linia pionowa)', () => {
    render(<ResizeHandle label="test" onPointerDown={vi.fn()} onDoubleClick={vi.fn()} />)
    const handle = screen.getByRole('separator')
    expect(handle).toHaveAttribute('aria-orientation', 'vertical')
    expect(handle.className).toContain('resize-handle--x')
  })

  it('orientation="vertical" — kursor row-resize, aria-orientation horizontal (linia pozioma)', () => {
    render(<ResizeHandle orientation="vertical" label="test" onPointerDown={vi.fn()} onDoubleClick={vi.fn()} />)
    const handle = screen.getByRole('separator')
    expect(handle).toHaveAttribute('aria-orientation', 'horizontal')
    expect(handle.className).toContain('resize-handle--y')
  })

  it('dragging=true dodaje klasę resize-handle--active', () => {
    const { rerender } = render(<ResizeHandle dragging={false} onPointerDown={vi.fn()} onDoubleClick={vi.fn()} />)
    expect(screen.getByRole('separator').className).not.toContain('resize-handle--active')
    rerender(<ResizeHandle dragging={true} onPointerDown={vi.fn()} onDoubleClick={vi.fn()} />)
    expect(screen.getByRole('separator').className).toContain('resize-handle--active')
  })

  it('label trafia do aria-label i title (z podpowiedzią o dwukliku)', () => {
    render(<ResizeHandle label="Szerokość drzewa" onPointerDown={vi.fn()} onDoubleClick={vi.fn()} />)
    const handle = screen.getByRole('separator')
    expect(handle).toHaveAttribute('aria-label', 'Szerokość drzewa')
    expect(handle.getAttribute('title')).toMatch(/Szerokość drzewa.*dwuklik/)
  })

  it('className jest doklejany obok wbudowanych klas', () => {
    render(<ResizeHandle className="my-custom-handle" onPointerDown={vi.fn()} onDoubleClick={vi.fn()} />)
    expect(screen.getByRole('separator').className).toContain('my-custom-handle')
  })

  it('onPointerDown i onDoubleClick są podpięte pod właściwe zdarzenia', () => {
    const onPointerDown = vi.fn()
    const onDoubleClick = vi.fn()
    render(<ResizeHandle onPointerDown={onPointerDown} onDoubleClick={onDoubleClick} />)
    const handle = screen.getByRole('separator')

    handle.dispatchEvent(new Event('pointerdown', { bubbles: true }))
    expect(onPointerDown).toHaveBeenCalledTimes(1)

    handle.dispatchEvent(new Event('dblclick', { bubbles: true }))
    expect(onDoubleClick).toHaveBeenCalledTimes(1)
  })
})
