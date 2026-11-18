/**
 * Motyw aplikacji: 'light' | 'dark' | 'system'. Wybór użytkownika leży w localStorage
 * (kf-theme), a efektywny motyw ląduje jako data-theme na <html>. Ten sam algorytm, w wersji
 * inline, uruchamia index.html przed załadowaniem Reacta, żeby ekran nie mignął jasnym tłem.
 */
export const THEME_KEY = 'kf-theme'
export const THEMES = ['light', 'dark', 'system']

export function readPreference() {
  try {
    const value = localStorage.getItem(THEME_KEY)
    return THEMES.includes(value) ? value : 'system'
  } catch {
    return 'system'
  }
}

export function systemPrefersDark() {
  return typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches
}

export function resolveTheme(preference) {
  if (preference === 'light' || preference === 'dark') return preference
  return systemPrefersDark() ? 'dark' : 'light'
}

export function applyTheme(preference, { animate = false } = {}) {
  const root = document.documentElement
  if (animate) {
    root.classList.add('theme-switching')
    setTimeout(() => root.classList.remove('theme-switching'), 250)
  }
  const theme = resolveTheme(preference)
  root.dataset.theme = theme
  return theme
}

export function savePreference(preference) {
  try {
    if (preference === 'system') localStorage.removeItem(THEME_KEY)
    else localStorage.setItem(THEME_KEY, preference)
  } catch {
    /* tryb prywatny / zablokowany storage — motyw działa do końca sesji */
  }
}
