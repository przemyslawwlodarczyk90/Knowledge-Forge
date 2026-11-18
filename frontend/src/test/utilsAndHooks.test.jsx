import { describe, it, expect, vi } from 'vitest'
import { renderHook, act, waitFor, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { formatDate, formatDateTime, timeAgo, truncate, normalizeLevel, formatSalary, sortByDate } from '@/utils'
import { useApi, useDebounce, useTitle } from '@/hooks'
import { Badge, PageHeader, EmptyState, ConfirmModal, Button } from '@/components/ui'

describe('utils', () => {
  it('format dates, text and lists', () => {
    expect(formatDate('2030-01-02T10:00:00Z')).toBe('02.01.2030')
    expect(formatDateTime(null)).toBe('—')
    expect(formatDateTime('2030-01-02T10:05:00')).toBe('02.01.2030 10:05')
    expect(formatDate('zła data')).toBe('zła data')
    expect(timeAgo('2020-01-01T00:00:00Z')).toMatch(/temu/)
    expect(timeAgo(null)).toBe('—')
    expect(truncate('abcdef', 3)).toBe('abc…')
    expect(truncate(null)).toBe('')
    expect(truncate('ab')).toBe('ab')
    expect(normalizeLevel('SENIOR')).toBe('Senior')
    expect(normalizeLevel(null)).toBe('—')
    expect(formatSalary('Brak wynagrodzenia')).toBeNull()
    expect(formatSalary('10k')).toBe('10k')
    const list = [{ createdAt: '2030-01-01' }, { createdAt: '2031-01-01' }, {}]
    expect(sortByDate(list)[0].createdAt).toBe('2031-01-01')
    expect(sortByDate(list, 'createdAt', 'asc')[2].createdAt).toBe('2031-01-01')
  })
})

describe('hooks', () => {
  it('useApi loads data, exposes errors and resets', async () => {
    const ok = vi.fn(() => Promise.resolve({ data: 42 }))
    const { result } = renderHook(() => useApi(ok, { immediate: true }))
    await waitFor(() => expect(result.current.data).toBe(42))
    act(() => result.current.reset())
    expect(result.current.data).toBeNull()

    const fail = vi.fn(() => Promise.reject(new Error('x')))
    const { result: r2 } = renderHook(() => useApi(fail))
    await act(async () => { await expect(r2.current.execute()).rejects.toThrow('x') })
    expect(r2.current.error.message).toBe('x')
  })

  it('useDebounce delays value and useTitle sets document title', () => {
    vi.useFakeTimers()
    const { result, rerender } = renderHook(({ v }) => useDebounce(v, 100), { initialProps: { v: 'a' } })
    rerender({ v: 'b' })
    expect(result.current).toBe('a')
    act(() => vi.advanceTimersByTime(150))
    expect(result.current).toBe('b')
    vi.useRealTimers()

    const { unmount } = renderHook(() => useTitle('Logowanie'))
    expect(document.title).toBe('Logowanie — Knowledge Forge')
    unmount()
  })
})

describe('ui components', () => {
  it('render basic building blocks', async () => {
    const onClick = vi.fn()
    render(<>
      <Badge level="senior" />
      <PageHeader title="Panel" subtitle="opis" actions={<span>akcja</span>} />
      <EmptyState title="Pusto" description="nic" action={<span>dodaj</span>} />
      <Button onClick={onClick}>Klik</Button>
      <Button loading variant="ghost" size="sm">Czekaj</Button>
    </>)
    expect(screen.getByText('Panel')).toBeInTheDocument()
    expect(screen.getByText('Pusto')).toBeInTheDocument()
    await userEvent.click(screen.getByText('Klik'))
    expect(onClick).toHaveBeenCalled()
  })

  it('ConfirmModal requires the confirmation phrase', async () => {
    const onConfirm = vi.fn(); const onClose = vi.fn()
    render(<ConfirmModal title="Usuń" description="na pewno?" onConfirm={onConfirm} onClose={onClose} />)
    const input = screen.getByRole('textbox')
    await userEvent.type(input, 'POTWIERDŹ')
    await userEvent.click(screen.getByText(/Usuń/, { selector: 'button' }))
    expect(onConfirm).toHaveBeenCalled()
    expect(onClose).toHaveBeenCalled()
  })
})
