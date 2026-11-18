import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { authApi } from '@/api/services'
import { useTitle } from '@/hooks'
import { AuthStyles } from './authStyles'

export default function PasswordResetPage({ reset = false }) {
  const { hash } = useLocation()
  const token = new URLSearchParams(hash.slice(1)).get('token')
  const validLink = /^[A-Za-z0-9_-]{43}$/.test(token ?? '')
  const title = reset ? 'Ustaw nowe hasło' : 'Zresetuj hasło'
  useTitle(title)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(false)
  const [error, setError] = useState('')

  async function submit(event) {
    event.preventDefault()
    if (reset && password !== confirmation) {
      setError('Hasła muszą być takie same.')
      return
    }
    if (reset && new TextEncoder().encode(password).length > 72) {
      setError('Hasło może mieć maksymalnie 72 bajty w UTF-8.')
      return
    }
    setError('')
    setBusy(true)
    try {
      if (reset) await authApi.resetPassword(token, password)
      else await authApi.requestPasswordReset(username)
      setPassword('')
      setConfirmation('')
      setDone(true)
    } catch (err) {
      setError(reset && err?.status === 400
        ? 'Nie udało się użyć linku. Poproś o nowy link do resetowania hasła.'
        : 'Nie udało się ukończyć operacji. Spróbuj ponownie.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-card auth-card--in">
      <div className="auth-card-header">
        <h2 className="auth-card-title">{title}</h2>
      </div>
      {done ? (
        <p role="status" className="auth-card-sub">
          {reset ? 'Hasło zostało zmienione. Zaloguj się nowym hasłem.'
            : 'Jeśli konto istnieje i może korzystać z resetu, otrzymasz link na przypisany do niego adres e-mail.'}
        </p>
      ) : reset && !validLink ? (
        <p role="alert" className="auth-err">Brakuje prawidłowego linku do resetowania hasła.</p>
      ) : (
        <form onSubmit={submit} className="auth-form">
          {reset ? (
            <>
              <div className="auth-field">
                <label className="auth-label" htmlFor="reset-password">Nowe hasło</label>
                <input id="reset-password" className="auth-input" type="password" autoComplete="new-password"
                  required minLength={6} maxLength={72} value={password} onChange={e => setPassword(e.target.value)} />
              </div>
              <div className="auth-field">
                <label className="auth-label" htmlFor="reset-confirmation">Powtórz nowe hasło</label>
                <input id="reset-confirmation" className="auth-input" type="password" autoComplete="new-password"
                  required maxLength={72} value={confirmation} onChange={e => setConfirmation(e.target.value)} />
              </div>
            </>
          ) : (
            <div className="auth-field">
              <label className="auth-label" htmlFor="reset-username">Nazwa użytkownika</label>
              <input id="reset-username" className="auth-input" autoComplete="username" required
                value={username} onChange={e => setUsername(e.target.value)} />
            </div>
          )}
          {error && <p role="alert" className="auth-err">{error}</p>}
          <button type="submit" className="auth-btn" disabled={busy || (!reset && !username.trim())}>
            {busy ? (reset ? 'Zapisywanie…' : 'Wysyłanie…') : reset ? 'Zapisz nowe hasło' : 'Wyślij link do resetu'}
          </button>
        </form>
      )}
      {reset && !done && <p className="auth-switch"><Link className="auth-link" to="/forgot-password">Poproś o nowy link</Link></p>}
      <p className="auth-switch"><Link className="auth-link" to="/login">Wróć do logowania</Link></p>
      <AuthStyles />
    </div>
  )
}
