import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { applyTheme, readPreference, resolveTheme, savePreference, THEME_KEY } from '../theme/theme'
import ThemeToggle from '../components/ui/ThemeToggle'

function mockSystem(dark) {
  const listeners = new Set()
  const media = {
    matches: dark,
    addEventListener: (_, fn) => listeners.add(fn),
    removeEventListener: (_, fn) => listeners.delete(fn),
  }
  window.matchMedia = vi.fn(() => media)
  return {
    flip(next) { media.matches = next; listeners.forEach(fn => fn()) },
    listeners,
  }
}

beforeEach(() => {
  localStorage.clear()
  delete document.documentElement.dataset.theme
})
afterEach(() => {
  delete window.matchMedia
  vi.useRealTimers()
})

describe('theme', () => {
  it('reads, saves and resolves the preference', () => {
    expect(readPreference()).toBe('system')
    savePreference('dark')
    expect(localStorage.getItem(THEME_KEY)).toBe('dark')
    expect(readPreference()).toBe('dark')
    localStorage.setItem(THEME_KEY, 'neon')
    expect(readPreference()).toBe('system')
    savePreference('system')
    expect(localStorage.getItem(THEME_KEY)).toBeNull()

    expect(resolveTheme('light')).toBe('light')
    expect(resolveTheme('system')).toBe('light') // jsdom: brak matchMedia
    mockSystem(true)
    expect(resolveTheme('system')).toBe('dark')
  })

  it('survives blocked storage', () => {
    const get = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('blocked') })
    const set = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked') })
    expect(readPreference()).toBe('system')
    expect(() => savePreference('dark')).not.toThrow()
    get.mockRestore(); set.mockRestore()
  })

  it('applies data-theme and a short transition class', () => {
    vi.useFakeTimers()
    expect(applyTheme('dark', { animate: true })).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(document.documentElement.classList.contains('theme-switching')).toBe(true)
    vi.advanceTimersByTime(300)
    expect(document.documentElement.classList.contains('theme-switching')).toBe(false)
  })

  it('toggle switches, persists and follows the system while on "system"', async () => {
    const system = mockSystem(false)
    const user = userEvent.setup()
    render(<ThemeToggle />)

    const toDark = screen.getByRole('button', { name: 'Włącz tryb ciemny' })
    await user.click(toDark)
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(localStorage.getItem(THEME_KEY)).toBe('dark')
    await user.click(screen.getByRole('button', { name: 'Włącz tryb jasny' }))
    expect(document.documentElement.dataset.theme).toBe('light')
    expect(localStorage.getItem(THEME_KEY)).toBe('light')

    // Jawny wybór -> zmiana systemu nic nie robi.
    expect(system.listeners.size).toBe(0)
  })

  it('reacts to OS theme changes when no explicit choice was made', () => {
    const system = mockSystem(false)
    render(<ThemeToggle />)
    expect(screen.getByRole('button', { name: 'Włącz tryb ciemny' })).toBeInTheDocument()
    act(() => system.flip(true))
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(screen.getByRole('button', { name: 'Włącz tryb jasny' })).toBeInTheDocument()
  })
})
