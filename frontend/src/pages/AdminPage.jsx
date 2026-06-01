import { useTitle } from '@/hooks'
import { PageHeader } from '@/components/ui'

export default function AdminPage() {
  useTitle('Admin')

  return (
    <>
      <PageHeader
        title="Panel Administratora"
        subtitle="Knowledge-Forge — zarządzanie platformą"
      />

      <div className="adm-placeholder">
        <span className="adm-placeholder-icon">⚙</span>
        <h2 className="adm-placeholder-title">Panel w budowie</h2>
        <p className="adm-placeholder-desc">
          Funkcje administracyjne zostaną dodane wraz z rozwojem platformy.
        </p>
      </div>

      <style>{`
        .adm-placeholder {
          background: var(--bg-1);
          border: 1px solid var(--border-1);
          border-radius: var(--radius-lg);
          padding: 80px 40px;
          text-align: center;
          display: flex; flex-direction: column; align-items: center; gap: 12px;
          margin-top: 8px;
        }
        .adm-placeholder-icon {
          font-size: 2.5rem; color: var(--cyan); opacity: 0.5; margin-bottom: 8px;
        }
        .adm-placeholder-title {
          font-size: 1.1rem; font-weight: 700; color: var(--text-1);
        }
        .adm-placeholder-desc {
          font-size: 0.83rem; color: var(--text-2);
          max-width: 420px; line-height: 1.7;
        }
      `}</style>
    </>
  )
}
