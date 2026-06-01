import { lazy, Suspense } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store'
import AppLayout    from '@/components/layout/AppLayout'
import AuthLayout   from '@/components/layout/AuthLayout'
import PageLoader   from '@/components/ui/PageLoader'

// Auth
const LoginPage    = lazy(() => import('@/pages/auth/LoginPage'))
const RegisterPage = lazy(() => import('@/pages/auth/RegisterPage'))
const ConfirmPage  = lazy(() => import('@/pages/auth/ConfirmPage'))

// App
const DashboardPage = lazy(() => import('@/pages/DashboardPage'))
const AdminPage     = lazy(() => import('@/pages/AdminPage'))

// ── Guards ────────────────────────────────────────────────────────
export function PrivateRoute({ children }) {
  const ok = useAuthStore((s) => s.isAuthenticated)
  if (!ok) return <Navigate to="/login" replace />
  return children
}

export function GuestRoute({ children }) {
  const ok = useAuthStore((s) => s.isAuthenticated)
  if (ok) return <Navigate to="/dashboard" replace />
  return children
}

export function AdminRoute({ children }) {
  const { isAuthenticated, user } = useAuthStore()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (user?.role !== 'ADMIN') return <Navigate to="/dashboard" replace />
  return children
}

const S = ({ children }) => <Suspense fallback={<PageLoader />}>{children}</Suspense>

export const router = createBrowserRouter([
  // ── Publiczne (auth)
  {
    element: <AuthLayout />,
    children: [
      { path: '/login',    element: <GuestRoute><S><LoginPage /></S></GuestRoute> },
      { path: '/register', element: <GuestRoute><S><RegisterPage /></S></GuestRoute> },
      { path: '/confirm',  element: <S><ConfirmPage /></S> },
    ],
  },
  // ── Chronione (app)
  {
    element: <PrivateRoute><AppLayout /></PrivateRoute>,
    children: [
      { index: true,        element: <Navigate to="/dashboard" replace /> },
      { path: '/dashboard', element: <S><DashboardPage /></S> },
      { path: '/admin',     element: <AdminRoute><S><AdminPage /></S></AdminRoute> },
    ],
  },
  { path: '*', element: <Navigate to="/dashboard" replace /> },
])
