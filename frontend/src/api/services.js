import api from './axios'

// ── Auth ──────────────────────────────────────────────────────────
export const authApi = {
  register: (data)  => api.post('/auth/register', data),
  login:    (data)  => api.post('/auth/login', data),
  updateProfile:  (data)  => api.put('/auth/update', data),
  changePassword: (data)  => api.post('/auth/change-password', data),
  confirmRegistration: (token) =>
    api.get('/v1/registration/confirm', { params: { token } }),
}

// ── Admin ─────────────────────────────────────────────────────────
export const adminApi = {
  getStatus: () => api.get('/admin/status'),
}
