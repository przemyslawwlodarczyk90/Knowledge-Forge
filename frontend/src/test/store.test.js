import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAuthStore, useUIStore, toast, decodeJwtPayload, isTokenExpired, getTokenExpiresAt } from '@/store'

const b64 = (o) => btoa(JSON.stringify(o)).replace(/=+$/, '')
const tokenWithExp = (secondsFromNow) => `h.${b64({ sub: 'ala', exp: Math.floor(Date.now() / 1000) + secondsFromNow })}.s`

beforeEach(() => {
  localStorage.clear()
  useAuthStore.setState({ token: null, user: null, isAuthenticated: false })
  useUIStore.setState({ toasts: [] })
})

describe('jwt helpers', () => {
  it('decodes payload and evaluates expiry', () => {
    const t = tokenWithExp(3600)
    expect(decodeJwtPayload(t).sub).toBe('ala')
    expect(isTokenExpired(t)).toBe(false)
    expect(isTokenExpired(tokenWithExp(-10))).toBe(true)
    expect(getTokenExpiresAt(t)).toBeInstanceOf(Date)
    expect(decodeJwtPayload('garbage')).toBeNull()
    expect(isTokenExpired('garbage')).toBe(true)
    expect(getTokenExpiresAt('garbage')).toBeNull()
  })
})

describe('auth store', () => {
  it('setAuth persists, logout clears, updateUser merges', () => {
    const t = tokenWithExp(3600)
    useAuthStore.getState().setAuth(t, { id: 1, username: 'ala', role: 'USER' })
    expect(localStorage.getItem('jwt_token')).toBe(t)
    expect(useAuthStore.getState().isAuthenticated).toBe(true)
    useAuthStore.getState().updateUser({ email: 'a@x.pl' })
    expect(useAuthStore.getState().user.email).toBe('a@x.pl')
    expect(useAuthStore.getState().getExpiresAt()).toBeInstanceOf(Date)
    expect(useAuthStore.getState().checkExpiry()).toBe(true)
    useAuthStore.getState().logout()
    expect(localStorage.getItem('jwt_token')).toBeNull()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(useAuthStore.getState().getExpiresAt()).toBeNull()
  })

  it('rehydrate restores a valid session and drops expired or broken ones', () => {
    localStorage.setItem('jwt_token', tokenWithExp(3600))
    localStorage.setItem('user_data', JSON.stringify({ username: 'ala' }))
    useAuthStore.getState().rehydrate()
    expect(useAuthStore.getState().user.username).toBe('ala')

    localStorage.setItem('jwt_token', tokenWithExp(-100))
    useAuthStore.getState().rehydrate()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)

    localStorage.setItem('jwt_token', tokenWithExp(3600))
    localStorage.setItem('user_data', '{broken')
    useAuthStore.getState().rehydrate()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
  })

  it('checkExpiry logs out when the token expired', () => {
    useAuthStore.setState({ token: tokenWithExp(-100), user: { username: 'x' }, isAuthenticated: true })
    expect(useAuthStore.getState().checkExpiry()).toBe(false)
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
  })
})

describe('ui store', () => {
  it('toasts are added, auto-removed and sidebar toggles', () => {
    vi.useFakeTimers()
    toast.success('ok'); toast.error('err'); toast.info('i'); toast.warn('w', 0)
    expect(useUIStore.getState().toasts.map(t => t.type)).toEqual(['success', 'error', 'info', 'warn'])
    vi.advanceTimersByTime(5000)
    expect(useUIStore.getState().toasts).toHaveLength(1)
    const before = useUIStore.getState().sidebarOpen
    useUIStore.getState().toggleSidebar()
    expect(useUIStore.getState().sidebarOpen).toBe(!before)
    useUIStore.getState().setSidebarOpen(true)
    expect(useUIStore.getState().sidebarOpen).toBe(true)
    vi.useRealTimers()
  })
})
