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

// ── Categories ────────────────────────────────────────────────────
export const categoriesApi = {
  getTree:  ()           => api.get('/categories/tree'),
  create:   (data)       => api.post('/categories', data),
  update:   (id, data)   => api.patch(`/categories/${id}`, data),
  remove:   (id)         => api.delete(`/categories/${id}`),
}

// ── Topics ────────────────────────────────────────────────────────
export const topicsApi = {
  listByCategory: (catId) => api.get(`/categories/${catId}/topics`),
  getById:        (id)    => api.get(`/topics/${id}`),
  create:         (data)  => api.post('/topics', data),
  update:         (id, d) => api.patch(`/topics/${id}`, d),
  remove:         (id)    => api.delete(`/topics/${id}`),
}

// ── Notes ─────────────────────────────────────────────────────────
export const notesApi = {
  getByTopic:  (topicId) => api.get(`/topics/${topicId}/note`),
  generate:    (topicId) => api.post(`/topics/${topicId}/note/generate`),
  regenerate:  (topicId) => api.post(`/topics/${topicId}/note/regenerate`),
}
