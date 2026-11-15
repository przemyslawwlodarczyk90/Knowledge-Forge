import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Routes, Route } from 'react-router-dom'

const authApi = { login: vi.fn(), register: vi.fn(), confirmRegistration: vi.fn() }
vi.mock('@/api/services', () => ({ authApi }))

const { useAuthStore, useUIStore } = await import('@/store')
const LoginPage = (await import('@/pages/auth/LoginPage')).default
const RegisterPage = (await import('@/pages/auth/RegisterPage')).default
const ConfirmPage = (await import('@/pages/auth/ConfirmPage')).default
const AuthLayout = (await import('@/components/layout/AuthLayout')).default

function at(path, element) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={element ?? <LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/confirm" element={<ConfirmPage />} />
        </Route>
        <Route path="/dashboard" element={<p>DASHBOARD</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  useAuthStore.setState({ token: null, user: null, isAuthenticated: false })
  useUIStore.setState({ toasts: [] })
})

describe('LoginPage', () => {
  it('validates, logs in, stores session and goes to dashboard', async () => {
    at('/login')
    await userEvent.click(screen.getByRole('button', { name: /Zaloguj się/ }))
    expect(screen.getAllByText('Pole wymagane')).toHaveLength(2)

    authApi.login.mockResolvedValue({ data: { token: 'h.e30.s', userId: 7, username: 'ala', email: 'a@x.pl', role: 'ADMIN' } })
    await userEvent.type(screen.getByLabelText('Nazwa użytkownika'), 'ala')
    await userEvent.type(screen.getByLabelText('Hasło'), 'Secret123')
    await userEvent.click(screen.getByLabelText('Pokaż hasło'))
    await userEvent.click(screen.getByRole('button', { name: /Zaloguj się/ }))

    await waitFor(() => expect(screen.getByText('DASHBOARD')).toBeInTheDocument())
    expect(authApi.login).toHaveBeenCalledWith({ username: 'ala', password: 'Secret123' })
    expect(useAuthStore.getState().user).toMatchObject({ id: 7, username: 'ala', role: 'ADMIN' })
  })

  it('shows an error toast on failed login', async () => {
    authApi.login.mockRejectedValue({ message: 'Invalid username or password' })
    at('/login')
    await userEvent.type(screen.getByLabelText('Nazwa użytkownika'), 'ala')
    await userEvent.type(screen.getByLabelText('Hasło'), 'bad')
    await userEvent.click(screen.getByRole('button', { name: /Zaloguj się/ }))
    await waitFor(() => expect(useUIStore.getState().toasts[0]?.message).toBe('Invalid username or password'))
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
  })
})

describe('RegisterPage', () => {
  it('validates fields, shows password strength and registers', async () => {
    at('/register')
    await userEvent.click(screen.getByRole('button', { name: /Zarejestruj się/ }))
    expect(screen.getByText('Pole wymagane')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Nazwa użytkownika'), 'ala')
    await userEvent.type(screen.getByLabelText('Email'), 'ala@x.pl')
    await userEvent.type(screen.getByLabelText('Hasło'), 'Secret123!')
    expect(screen.getByText('Silne')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Powtórz hasło'), 'inne')
    await userEvent.click(screen.getByRole('button', { name: /Zarejestruj się/ }))
    expect(screen.getByText('Hasła nie są zgodne')).toBeInTheDocument()

    authApi.register.mockResolvedValue({ data: { success: true } })
    await userEvent.clear(screen.getByLabelText('Powtórz hasło'))
    await userEvent.type(screen.getByLabelText('Powtórz hasło'), 'Secret123!')
    await userEvent.click(screen.getByRole('button', { name: /Zarejestruj się/ }))
    await waitFor(() => expect(authApi.register).toHaveBeenCalledWith({ username: 'ala', email: 'ala@x.pl', password: 'Secret123!' }))
    await waitFor(() => expect(screen.getByRole('button', { name: /Zaloguj się/ })).toBeInTheDocument())
  })

  it('reports registration errors', async () => {
    authApi.register.mockRejectedValue({ message: 'Username already taken' })
    at('/register')
    await userEvent.type(screen.getByLabelText('Nazwa użytkownika'), 'ala')
    await userEvent.type(screen.getByLabelText('Email'), 'ala@x.pl')
    await userEvent.type(screen.getByLabelText('Hasło'), 'abcdef')
    await userEvent.type(screen.getByLabelText('Powtórz hasło'), 'abcdef')
    await userEvent.click(screen.getByRole('button', { name: /Zarejestruj się/ }))
    await waitFor(() => expect(useUIStore.getState().toasts[0]?.message).toBe('Username already taken'))
  })
})

describe('ConfirmPage', () => {
  it('confirms a valid token', async () => {
    authApi.confirmRegistration.mockResolvedValue({ data: {} })
    at('/confirm?token=abc')
    await waitFor(() => expect(screen.getByText(/zostało aktywowane/)).toBeInTheDocument())
    expect(authApi.confirmRegistration).toHaveBeenCalledWith('abc')
  })

  it('shows backend error and missing token message', async () => {
    authApi.confirmRegistration.mockRejectedValue({ message: 'Token has expired' })
    at('/confirm?token=old')
    await waitFor(() => expect(screen.getByText('Token has expired')).toBeInTheDocument())
  })

  it('without token', () => {
    at('/confirm')
    expect(screen.getByText(/Brak tokenu/)).toBeInTheDocument()
  })
})
