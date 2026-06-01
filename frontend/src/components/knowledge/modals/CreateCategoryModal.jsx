import { useState } from 'react'
import { categoriesApi } from '@/api/services'
import { toast } from '@/store'

export default function CreateCategoryModal({ parentId, parentName, onClose, onCreated }) {
  const [name, setName]         = useState('')
  const [desc, setDesc]         = useState('')
  const [loading, setLoading]   = useState(false)
  const [error, setError]       = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!name.trim()) { setError('Nazwa jest wymagana'); return }
    setLoading(true)
    setError('')
    try {
      const res = await categoriesApi.create({ name: name.trim(), description: desc.trim() || null, parentId: parentId ?? null })
      toast.success(`Kategoria "${res.data.name}" utworzona`)
      onCreated(res.data)
      onClose()
    } catch (err) {
      setError(err.message ?? 'Błąd tworzenia kategorii')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-box" onClick={e => e.stopPropagation()}>
        <header className="modal-head">
          <div>
            <p className="modal-eyebrow">{parentId ? 'Podkategoria' : 'Kategoria główna'}</p>
            <h2 className="modal-title">
              {parentId ? `w "${parentName}"` : 'Nowa kategoria'}
            </h2>
          </div>
          <button className="modal-close" onClick={onClose}>✕</button>
        </header>

        <form onSubmit={handleSubmit} className="modal-body">
          <div className="mf-field">
            <label className="mf-label">Nazwa *</label>
            <input
              className="mf-input"
              value={name}
              onChange={e => { setName(e.target.value); setError('') }}
              placeholder={parentId ? 'np. JAVA, SIECI, RENESANS' : 'np. IT, HISTORIA, MEDYCYNA'}
              autoFocus
            />
            {error && <span className="mf-error">{error}</span>}
          </div>

          <div className="mf-field">
            <label className="mf-label">Opis (opcjonalny)</label>
            <input
              className="mf-input"
              value={desc}
              onChange={e => setDesc(e.target.value)}
              placeholder="Krótki opis kategorii"
            />
          </div>

          <footer className="modal-foot">
            <button type="button" className="btn-ghost" onClick={onClose}>Anuluj</button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? <span className="btn-spin" /> : 'Utwórz'}
            </button>
          </footer>
        </form>
      </div>

      <ModalStyles />
    </div>
  )
}

function ModalStyles() {
  return (
    <style>{`
      .modal-eyebrow { font-family:var(--font-mono); font-size:0.6rem; font-weight:700; letter-spacing:0.12em; text-transform:uppercase; color:var(--accent); margin-bottom:2px; }
      .modal-backdrop {
        position:fixed; inset:0; z-index:200;
        background:rgba(15,23,42,0.45); backdrop-filter:blur(3px);
        display:flex; align-items:center; justify-content:center; padding:16px;
      }
      .modal-box {
        background:#fff; border-radius:var(--radius-xl);
        border:1px solid var(--border-1);
        box-shadow:var(--shadow-lg);
        width:100%; max-width:420px;
        animation:fadeIn .2s ease both;
      }
      .modal-head {
        display:flex; align-items:center; justify-content:space-between;
        padding:18px 20px 0;
      }
      .modal-title { font-size:1rem; font-weight:700; color:var(--text-0); }
      .modal-close {
        background:none; border:none; color:var(--text-3);
        cursor:pointer; font-size:0.9rem; padding:4px 6px;
        border-radius:6px; transition:background var(--t-fast);
      }
      .modal-close:hover { background:var(--bg-2); color:var(--text-1); }
      .modal-body { padding:16px 20px 20px; display:flex; flex-direction:column; gap:14px; }
      .modal-foot { display:flex; justify-content:flex-end; gap:8px; margin-top:4px; }
      .mf-field { display:flex; flex-direction:column; gap:5px; }
      .mf-label { font-size:0.72rem; font-weight:700; color:var(--text-1); letter-spacing:0.04em; }
      .mf-input {
        padding:10px 12px; border-radius:var(--radius-md);
        border:1.5px solid var(--border-1); background:var(--bg-2);
        font-size:0.88rem; color:var(--text-0); outline:none;
        transition:border-color .15s;
      }
      .mf-input:focus { border-color:var(--accent); background:#fff; box-shadow:0 0 0 3px var(--accent-glow); }
      .mf-error { font-size:0.72rem; color:var(--red); }
      .btn-primary {
        padding:9px 20px; background:var(--accent); color:#fff;
        border:none; border-radius:var(--radius-md); font-size:0.84rem; font-weight:700;
        cursor:pointer; transition:background .15s;
      }
      .btn-primary:hover:not(:disabled) { background:var(--accent-dim); }
      .btn-primary:disabled { opacity:.5; cursor:not-allowed; }
      .btn-ghost {
        padding:9px 16px; background:transparent; color:var(--text-2);
        border:1px solid var(--border-1); border-radius:var(--radius-md);
        font-size:0.84rem; cursor:pointer; transition:background .15s;
      }
      .btn-ghost:hover { background:var(--bg-2); }
      .btn-spin {
        display:inline-block; width:13px; height:13px;
        border:2px solid rgba(255,255,255,.3); border-top-color:#fff;
        border-radius:50%; animation:spin .6s linear infinite; vertical-align:middle;
      }
    `}</style>
  )
}
