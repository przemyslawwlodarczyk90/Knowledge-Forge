import axios from 'axios'
import { getClientId } from '@/ws/clientId'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// Każde żądanie niesie clientId tej karty przeglądarki — backend wstawia go do zdarzenia
// WebSocket jako sourceClientId, żeby karta, która sama wykonała operację, mogła rozpoznać
// i zignorować (albo potraktować jako samo potwierdzenie) własne zdarzenie zwrotne.
api.interceptors.request.use((config) => {
  config.headers['X-Client-Id'] = getClientId()
  // Token JWT (zob. useAuthStore) — wymagany przez backend dla każdej operacji modyfikującej.
  // Zwykłe linki/obrazki/wideo (<img src="/api/...">) uwierzytelnia ciasteczko HttpOnly z logowania.
  const token = localStorage.getItem('jwt_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Obsługa odpowiedzi
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status

    // 401 poza samym logowaniem = token wygasł/nieważny -> wyloguj (zob. main.jsx)
    if (status === 401 && !err.config?.url?.startsWith('/auth/login')) {
      window.dispatchEvent(new Event('auth:expired'))
    }

    // Normalizuj błąd — wyciągnij message z backendu
    const message =
      err.response?.data?.message ??
      err.response?.data?.error ??
      err.message ??
      'Nieznany błąd'

    return Promise.reject({ ...err, message, status })
  }
)

export default api