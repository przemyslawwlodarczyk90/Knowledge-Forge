import { useCallback, useEffect, useState } from 'react'
import { applyTheme, readPreference, resolveTheme, savePreference } from './theme'

/** Bieżący motyw + przełączanie; przy 'system' reaguje na zmianę ustawienia systemu na żywo. */
export function useTheme() {
  const [preference, setPreference] = useState(readPreference)
  const [theme, setTheme] = useState(() => resolveTheme(readPreference()))

  useEffect(() => {
    if (preference !== 'system' || typeof window.matchMedia !== 'function') return
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = () => setTheme(applyTheme('system', { animate: true }))
    media.addEventListener?.('change', onChange)
    return () => media.removeEventListener?.('change', onChange)
  }, [preference])

  const choose = useCallback(next => {
    savePreference(next)
    setPreference(next)
    setTheme(applyTheme(next, { animate: true }))
  }, [])

  /** Jedno kliknięcie: jasny <-> ciemny (jawny wybór zastępuje 'system'). */
  const toggle = useCallback(() => choose(theme === 'dark' ? 'light' : 'dark'), [choose, theme])

  return { preference, theme, choose, toggle }
}
