import { describe, it, expect, vi, beforeEach } from 'vitest'

describe('axios instance interceptors', () => {
  beforeEach(() => localStorage.clear())

  it('adds client id and bearer token, normalizes errors and signals expiry on 401', async () => {
    const { default: api } = await import('@/api/axios')
    const onRequest = api.interceptors.request.handlers[0].fulfilled
    const onError = api.interceptors.response.handlers[0].rejected

    expect(onRequest({ headers: {} }).headers.Authorization).toBeUndefined()
    localStorage.setItem('jwt_token', 'tok')
    const cfg = onRequest({ headers: {} })
    expect(cfg.headers.Authorization).toBe('Bearer tok')
    expect(cfg.headers['X-Client-Id']).toBeTruthy()

    const expired = vi.fn()
    window.addEventListener('auth:expired', expired)
    await expect(onError({ response: { status: 401, data: { message: 'nope' } }, config: { url: '/topics' } }))
      .rejects.toMatchObject({ status: 401, message: 'nope' })
    expect(expired).toHaveBeenCalledTimes(1)
    await expect(onError({ response: { status: 401, data: {} }, config: { url: '/auth/login' }, message: 'x' }))
      .rejects.toMatchObject({ status: 401 })
    expect(expired).toHaveBeenCalledTimes(1)
    await expect(onError({ message: 'Network Error' })).rejects.toMatchObject({ message: 'Network Error' })
    expect(api.interceptors.response.handlers[0].fulfilled('res')).toBe('res')
  })
})

describe('api services', () => {
  it('map every call to the right endpoint', async () => {
    vi.resetModules()
    const api = { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() }
    vi.doMock('@/api/axios', () => ({ default: api }))
    const s = await import('@/api/services')
    s.authApi.register({}); s.authApi.login({}); s.authApi.logout(); s.authApi.updateProfile({})
    s.authApi.changePassword({}); s.authApi.confirmRegistration('t'); s.adminApi.getStatus()
    s.categoriesApi.getTree(); s.categoriesApi.create({}); s.categoriesApi.update('c', {}); s.categoriesApi.remove('c')
    s.topicsApi.listByCategory('c'); s.topicsApi.getById('t'); s.topicsApi.create({}); s.topicsApi.update('t', {})
    s.topicsApi.remove('t'); s.topicsApi.filter({}); s.topicsApi.listAuthors(); s.topicsApi.actualityReview('a')
    s.topicsApi.actualityReview(); s.topicsApi.verifyActuality('t', 1)
    s.notesApi.getByTopic('t'); s.notesApi.save('t', {}); s.appConfigApi.get(); s.searchApi.search('q')
    s.attachmentsApi.listByTopic('t'); s.attachmentsApi.upload('t', new FormData()); s.attachmentsApi.remove(5)
    expect(s.attachmentsApi.viewUrl(5)).toBe('/api/attachments/5/view')
    expect(s.attachmentsApi.downloadUrl(5)).toBe('/api/attachments/5/download')
    expect(api.post).toHaveBeenCalledWith('/auth/login', {})
    expect(api.get).toHaveBeenCalledWith('/v1/registration/confirm', { params: { token: 't' } })
    expect(api.post).toHaveBeenCalledWith('/topics/t/verify-actuality', { expectedVersion: 1 })
    expect(api.get).toHaveBeenCalledWith('/admin/status')
    expect(api.delete).toHaveBeenCalledWith('/attachments/5')
    vi.doUnmock('@/api/axios')
  })
})
