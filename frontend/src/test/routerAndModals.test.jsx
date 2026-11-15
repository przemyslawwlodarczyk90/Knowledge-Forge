import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Routes, Route } from 'react-router-dom'

const categoriesApi = { create: vi.fn() }
const topicsApi = { create: vi.fn() }
vi.mock('@/api/services', () => ({ categoriesApi, topicsApi, authApi: {}, adminApi: {} }))

const { useAuthStore } = await import('@/store')
const { PrivateRoute, GuestRoute, AdminRoute } = await import('@/router')
const CreateCategoryModal = (await import('@/components/knowledge/modals/CreateCategoryModal')).default
const CreateTopicModal = (await import('@/components/knowledge/modals/CreateTopicModal')).default
const AdminPage = (await import('@/pages/AdminPage')).default

function guarded(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<GuestRoute><p>LOGIN</p></GuestRoute>} />
        <Route path="/dashboard" element={<PrivateRoute><p>DASH</p></PrivateRoute>} />
        <Route path="/admin" element={<AdminRoute><AdminPage /></AdminRoute>} />
      </Routes>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  vi.clearAllMocks()
  useAuthStore.setState({ token: null, user: null, isAuthenticated: false })
})

describe('route guards', () => {
  it('anonymous user is sent to login', () => {
    guarded('/dashboard')
    expect(screen.getByText('LOGIN')).toBeInTheDocument()
  })

  it('logged-in user skips login and cannot open admin without the role', () => {
    useAuthStore.setState({ isAuthenticated: true, user: { role: 'USER' } })
    guarded('/login')
    expect(screen.getByText('DASH')).toBeInTheDocument()
  })

  it('non-admin on /admin goes to dashboard, admin sees the panel, anonymous goes to login', () => {
    useAuthStore.setState({ isAuthenticated: true, user: { role: 'USER' } })
    const { unmount } = guarded('/admin')
    expect(screen.getByText('DASH')).toBeInTheDocument()
    unmount()
    useAuthStore.setState({ isAuthenticated: true, user: { role: 'ADMIN' } })
    const second = guarded('/admin')
    expect(screen.getByText('Panel Administratora')).toBeInTheDocument()
    second.unmount()
    useAuthStore.setState({ isAuthenticated: false, user: null })
    guarded('/admin')
    expect(screen.getByText('LOGIN')).toBeInTheDocument()
  })
})

describe('CreateCategoryModal', () => {
  it('requires a name, creates a sub-category and reports errors', async () => {
    const onClose = vi.fn(); const onCreated = vi.fn()
    render(<CreateCategoryModal parentId="p1" parentName="Java" onClose={onClose} onCreated={onCreated} />)
    expect(screen.getByText('w "Java"')).toBeInTheDocument()
    await userEvent.click(screen.getByText('Utwórz'))
    expect(screen.getByText('Nazwa jest wymagana')).toBeInTheDocument()

    categoriesApi.create.mockRejectedValueOnce({ message: 'Duplikat' })
    await userEvent.type(screen.getByPlaceholderText('Nazwa podkategorii'), '  Streams ')
    await userEvent.click(screen.getByText('Utwórz'))
    await waitFor(() => expect(screen.getByText('Duplikat')).toBeInTheDocument())

    categoriesApi.create.mockResolvedValueOnce({ data: { id: 'c1', name: 'Streams' } })
    await userEvent.click(screen.getByText('Utwórz'))
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith({ id: 'c1', name: 'Streams' }))
    expect(categoriesApi.create).toHaveBeenLastCalledWith({ name: 'Streams', parentId: 'p1' })
    expect(onClose).toHaveBeenCalled()
  })
})

describe('CreateTopicModal', () => {
  it('validates title, asks for confirmation and creates the topic', async () => {
    const onClose = vi.fn(); const onCreated = vi.fn()
    render(<CreateTopicModal categoryId="cat" categoryName="Java" onClose={onClose} onCreated={onCreated} />)
    await userEvent.click(screen.getByText('Utwórz'))
    expect(screen.getByText('Tytuł jest wymagany')).toBeInTheDocument()

    await userEvent.type(screen.getByPlaceholderText('np. Konfiguracja środowiska'), 'Lambdy')
    await userEvent.type(screen.getByPlaceholderText(/Krótki opis/), 'opis')
    await userEvent.click(screen.getByText('Wysoki'))
    await userEvent.click(screen.getByText('Instrukcja'))

    topicsApi.create.mockRejectedValueOnce({ message: 'Błąd serwera' })
    await userEvent.click(screen.getByText('Utwórz'))
    await userEvent.click(await screen.findByText('Dodaj'))
    await waitFor(() => expect(topicsApi.create).toHaveBeenCalledTimes(1))

    topicsApi.create.mockResolvedValueOnce({ data: { id: 't1', title: 'Lambdy' } })
    await userEvent.click(screen.getByText('Utwórz'))
    await userEvent.click(await screen.findByText('Dodaj'))
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith({ id: 't1', title: 'Lambdy' }))
    expect(topicsApi.create).toHaveBeenLastCalledWith({
      categoryId: 'cat', title: 'Lambdy', shortPrompt: 'opis', detailLevel: 'HIGH', type: 'INSTRUCTION',
    })
  })
})
