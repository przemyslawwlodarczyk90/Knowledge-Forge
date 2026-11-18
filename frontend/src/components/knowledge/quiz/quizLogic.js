/** Czysta logika quizu (bez Reacta) — długości podejścia, wynik, komunikaty błędów. */

export const LETTERS = ['A', 'B', 'C', 'D']
export const LENGTHS = [10, 20, 30, 50]
export const DEFAULT_PASS_RATIO = 0.75
export const LEVEL_LABEL = { EASY: 'Łatwe', MEDIUM: 'Średnie', HARD: 'Trudne' }

/** Dostępne długości quizu dla danej puli (np. pula 14 -> [10, 14]). */
export function lengthOptions(pool) {
  const options = LENGTHS.filter(n => n <= pool)
  if (pool > 0 && pool < 50 && !options.includes(pool)) options.push(pool)
  return options
}

export function scoreOf(questions, answers, passRatio = DEFAULT_PASS_RATIO) {
  const correct = questions.filter((q, i) => answers[i] === q.correctIndex).length
  const total = questions.length
  const byLevel = {}
  questions.forEach((q, i) => {
    const level = (byLevel[q.difficulty] ??= { correct: 0, total: 0 })
    level.total += 1
    if (answers[i] === q.correctIndex) level.correct += 1
  })
  return { correct, total, ratio: total ? correct / total : 0, passed: total > 0 && correct / total >= passRatio, byLevel }
}

export function errorMessage(err) {
  if (err?.status === 503) return err.message
  if (err?.status === 429) return 'Osiągnięto dzienny limit generowania quizów. Spróbuj jutro.'
  if (err?.status === 400 || err?.status === 409) return err.message
  return 'Nie udało się wygenerować pytań. Spróbuj ponownie.'
}
