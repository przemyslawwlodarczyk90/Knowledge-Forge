import React from 'react'
import ReactDOM from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { router } from '@/router'
import { useAuthStore } from '@/store'
import { authApi } from '@/api/services'

import '@/styles/globals.css'
import { applyTheme, readPreference } from '@/theme/theme'

// Zwykle motyw ustawił już skrypt w index.html — to tylko zabezpieczenie, gdyby go zablokowano.
applyTheme(readPreference())

useAuthStore.getState().rehydrate()

// Token wygasł — wyloguj (także ciasteczko po stronie serwera) i przekieruj na login
window.addEventListener('auth:expired', () => {
  if (!useAuthStore.getState().isAuthenticated) return
  useAuthStore.getState().logout()
  authApi.logout().catch(() => {})
  router.navigate('/login', { replace: true })
})

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
)
