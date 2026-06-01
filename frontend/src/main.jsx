import React from 'react'
import ReactDOM from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { router } from '@/router'
import { useAuthStore } from '@/store'

import '@/styles/globals.css'

useAuthStore.getState().rehydrate()

// Token wygasł — wyloguj i przekieruj na login
window.addEventListener('auth:expired', () => {
  useAuthStore.getState().logout()
  router.navigate('/login', { replace: true })
})

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>
)
