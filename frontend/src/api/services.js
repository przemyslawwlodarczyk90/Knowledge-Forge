import api from './axios'

// ── Auth ──────────────────────────────────────────────────────────
export const authApi = {
  register: (data)  => api.post('/auth/register', data),
  login:    (data)  => api.post('/auth/login', data),
  logout:   ()      => api.post('/auth/logout'),
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
  // Panel "Filtry" — filtrowanie po autorze / poziomie szczegółowości / (pod)kategorii.
  // Bez żadnego filtra backend świadomie zwraca pustą listę.
  filter:         (params) => api.get('/topics', { params }),
  listAuthors:    ()       => api.get('/topics/authors'),
  // Panel "Weryfikacja aktualności" (zob. dokumentacja/ACTUALITY_VERIFICATION.txt) — wyłącznie tematy
  // actualityVerified=false, opcjonalnie zawężone po autorze.
  actualityReview: (author) => api.get('/topics/actuality-review', { params: author ? { author } : undefined }),
  // Ręczne potwierdzenie aktualności — optimistic locking (expectedVersion), NIE ogólny PATCH.
  verifyActuality: (id, expectedVersion) => api.post(`/topics/${id}/verify-actuality`, { expectedVersion }),
}

// ── Notes / instrukcje ───────────────────────────────────────────────
// content_json + wklejone obrazki, spakowane po stronie backendu do jednego
// binarnego .kfdoc (zob. document.DocumentContainer w Javie). PDF nie jest
// tu w ogóle — generuje się on-demand w przeglądarce, zob. NotePanel.
export const notesApi = {
  // `config` (opcjonalny) — używany przez druk masowy do przekazania AbortSignal, żeby dało się
  // anulować pobieranie w toku (zob. DashboardPage / pdfGenerator).
  getByTopic: (topicId, config) => api.get(`/topics/${topicId}/note`, config),
  save:       (topicId, d)      => api.put(`/topics/${topicId}/note`, d),
  generate:   (topicId, data, signal) =>
    api.post(`/topics/${topicId}/note/generate`, data, { signal, timeout: 120_000 }),
}

// ── Konfiguracja widoczna dla frontendu ──────────────────────────────
// Wyłącznie przełączniki widoczności elementów UI z config.properties (zob. AppConfigServlet) —
// NIE sekrety, NIE ścieżki systemowe.
export const appConfigApi = {
  get: () => api.get('/app-config'),
}

// ── Wyszukiwarka ──────────────────────────────────────────────────
// Przeszukuje tytuł, opis i treść wszystkich wpisów (pełne przeszukanie po stronie serwera).
export const searchApi = {
  search: (q) => api.get('/search', { params: { q } }),
}

// ── Załączniki ────────────────────────────────────────────────────
// Baza trzyma tylko metadane — pliki leżą na dysku serwera. /view i /download budujemy
// wyłącznie z id (backend sam odnajduje plik po rekordzie z bazy, bez ścieżki w URL-u).
export const attachmentsApi = {
  listByTopic: (topicId) => api.get(`/topics/${topicId}/attachments`),
  upload:      (topicId, formData, onUploadProgress) =>
    api.post(`/topics/${topicId}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress,
    }),
  remove:      (id) => api.delete(`/attachments/${id}`),
  viewUrl:     (id) => `/api/attachments/${id}/view`,
  downloadUrl: (id) => `/api/attachments/${id}/download`,
}
