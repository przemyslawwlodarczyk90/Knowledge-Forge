import { useEffect, useState } from 'react'
import { quizApi } from '@/api/services'
import { toast } from '@/store'
import HelpTip from '@/components/ui/HelpTip'
import GenerateQuizModal from './GenerateQuizModal'
import QuizRunner from './QuizRunner'

/**
 * Quiz w pasku akcji notatki: skromny licznik pytań w puli, „Wygeneruj quiz z AI” (kolejne
 * kliknięcia dorzucają po 100 pytań) i „Rozwiąż quiz”. Widoczny tylko dla zapisanej notatki.
 * Renderowany z key={topicId} — zmiana tematu montuje go od nowa (świeży stan).
 */
export default function QuizBar({ topicId, topic, disabled }) {
  const [stats, setStats] = useState(null)
  const [modal, setModal] = useState(null)

  useEffect(() => {
    let cancelled = false
    quizApi.stats(topicId)
      .then(res => { if (!cancelled) setStats(res.data) })
      .catch(() => { if (!cancelled) setStats(null) })
    return () => { cancelled = true }
  }, [topicId])

  const total = stats?.total ?? 0

  function onGenerated(result) {
    setStats(result.stats)
    setModal(null)
    const partial = result.failedChunks > 0 ? ' (część porcji AI się nie udała — możesz dogenerować)' : ''
    toast.success(`Dodano ${result.added} pytań — w puli jest ich ${result.stats.total}${partial}`)
  }

  return (
    <div className="quiz-bar">
      <span className="quiz-bar-count" title={stats ? `Łatwe ${stats.easy} · średnie ${stats.medium} · trudne ${stats.hard}` : undefined}
        aria-label={`Pytań w puli quizu: ${total}`}>
        <svg width="13" height="13" viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M3 2.5h10v11H3z" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round" />
          <path d="M5.5 6l1.2 1.2L9 5M5.5 10.5h5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        {total}
      </span>
      <button type="button" className="act-btn act-btn--edit" disabled={disabled} onClick={() => setModal('generate')}>
        {total > 0 ? '+100 pytań z AI' : 'Wygeneruj quiz z AI'}
      </button>
      <button type="button" className="act-btn act-btn--quiz" disabled={disabled || total === 0} onClick={() => setModal('run')}>
        Rozwiąż quiz
      </button>
      <HelpTip title="Quiz z AI" align="right">
        <ul>
          <li>AI tworzy 100 pytań A–D z tej notatki (łatwe, średnie, trudne).</li>
          <li>Każdy quiz losuje pytania z całej puli — liczba obok to jej rozmiar.</li>
          <li>Zaliczenie od 75%. Znudzone pytania? Dodaj kolejne 100.</li>
        </ul>
      </HelpTip>

      {modal === 'generate' && (
        <GenerateQuizModal topic={topic} poolSize={total} onClose={() => setModal(null)} onGenerated={onGenerated} />
      )}
      {modal === 'run' && (
        <QuizRunner topic={topic} stats={stats} onClose={() => setModal(null)} onStatsChange={setStats} />
      )}

      <style>{`
        .quiz-bar { display:flex; align-items:center; gap:6px; flex-wrap:wrap; }
        .quiz-bar-count {
          display:inline-flex; align-items:center; gap:4px; height:22px; padding:0 8px;
          border-radius:99px; background:var(--bg-3); color:var(--text-2);
          font-family:var(--font-mono); font-size:.72rem; font-weight:700;
        }
        .act-btn--quiz { color:var(--accent); border-color:var(--border-0); background:var(--accent-light); }
        .act-btn--quiz:hover:not(:disabled) { background:var(--accent-glow-strong); }
      `}</style>
    </div>
  )
}
