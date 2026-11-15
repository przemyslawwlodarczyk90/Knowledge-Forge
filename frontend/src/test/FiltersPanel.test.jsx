import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, act, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'

vi.mock('@/api/services', () => ({
  categoriesApi: {
    getTree: vi.fn(() => Promise.resolve({ data: { children: [
      { id: 'cat-1', name: 'Kategoria A', children: [{ id: 'sub-1', name: 'Podkategoria A1', children: [] }] },
    ] } })),
  },
  topicsApi: {
    listAuthors: vi.fn(() => Promise.resolve({ data: ['Ala', 'Bob'] })),
    filter: vi.fn(() => Promise.resolve({ data: [] })),
  },
}))

const FiltersPanel = (await import('../components/knowledge/FiltersPanel')).default

function renderPanel() {
  return render(
    <MemoryRouter>
      <FiltersPanel />
    </MemoryRouter>,
  )
}

// Etykiety pól kryteriów nie są programowo powiązane (brak htmlFor/id) — to zwykłe wizualne
// <label> nad <select>, więc dostęp bierzemy po pozycji (Autor jest pierwszym polem select — zob.
// współdzielona klasa `.sop-select` z SlideOverPanel).
function authorSelectOf() {
  return document.body.querySelectorAll('.sop-select')[0]
}

// Panel ma dwa <style> (SlideOverPanel + FiltersPanel) — łączymy ich treść zamiast zakładać, który
// jest pierwszy w DOM.
function allStyleText() {
  return [...document.body.querySelectorAll('style')].map(s => s.textContent).join('\n')
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('FiltersPanel — panel typu drawer', () => {
  it('panel wysuwa się od prawej i zajmuje całą szerokość ekranu (bez dawnego max-width:440px)', async () => {
    const user = userEvent.setup()
    renderPanel()
    await user.click(screen.getByRole('button', { name: /filtry/i }))
    await screen.findByRole('dialog', { name: 'Filtry' })

    const panel = document.body.querySelector('.sop-panel')
    expect(panel).toBeInTheDocument()
    const styleText = allStyleText()
    // Pełnoekranowy — width:100%, żadnego max-width ograniczającego do stałej, wąskiej szerokości
    // (dawne max-width:440px zniknęło).
    expect(styleText).toMatch(/\.sop-panel\s*\{[^}]*width:\s*100%/)
    expect(styleText).not.toMatch(/max-width:\s*440px/)
    expect(styleText).toMatch(/\.sop-panel[^}]*box-shadow/)
    expect(styleText).toMatch(/\.sop-backdrop\.sop-open\s*\{[^}]*background:rgba\(15,23,42,0\.45\)/)
  })

  it('otwieranie i zamykanie animuje przesunięcie panelu (transform translateX) w ok. 300-400ms z łagodnym easingiem', async () => {
    const user = userEvent.setup()
    renderPanel()
    await user.click(screen.getByRole('button', { name: /filtry/i }))
    await screen.findByRole('dialog', { name: 'Filtry' })

    const styleText = allStyleText()
    expect(styleText).toMatch(/\.sop-panel\s*\{[^}]*transform:translateX\(100%\)/)
    expect(styleText).toMatch(/\.sop-panel\.sop-open\s*\{\s*transform:translateX\(0\)/)
    const transitionMatch = styleText.match(/\.sop-panel\s*\{[^}]*transition:transform (\d+)ms/)
    expect(transitionMatch).toBeTruthy()
    const durationMs = Number(transitionMatch[1])
    expect(durationMs).toBeGreaterThanOrEqual(280)
    expect(durationMs).toBeLessThanOrEqual(420)
    // "sop-open" pojawia się na panelu dopiero po zamontowaniu (klatka animacji wejścia) — na
    // starcie panel jest jeszcze w stanie zamkniętym (translateX 100%).
    const panel = document.body.querySelector('.sop-panel')
    await waitFor(() => expect(panel).toHaveClass('sop-open'))
  })

  it('kryteria są w jednej uporządkowanej sekcji na górze, nie w osobnych pełnowymiarowych blokach', async () => {
    const user = userEvent.setup()
    renderPanel()
    await user.click(screen.getByRole('button', { name: /filtry/i }))
    await screen.findByRole('dialog', { name: 'Filtry' })

    const criteria = document.body.querySelector('.sop-criteria')
    expect(criteria).toBeInTheDocument()
    // Wszystkie pola kryteriów (autor, kategoria, podkategoria, poziom) są DZIEĆMI jednej
    // uporządkowanej sekcji .sop-criteria, nie osobnymi, pełnowymiarowymi blokami.
    expect(criteria.querySelectorAll('.sop-field').length).toBe(4)
  })

  it('pola formularza mają spójną wysokość i widoczny stan focus', async () => {
    const user = userEvent.setup()
    renderPanel()
    await user.click(screen.getByRole('button', { name: /filtry/i }))
    await screen.findByRole('dialog', { name: 'Filtry' })

    const styleText = allStyleText()
    expect(styleText).toMatch(/\.sop-input,\s*\.sop-select\s*\{[^}]*height:40px/)
    expect(styleText).toMatch(/\.sop-input:focus,\s*\.sop-select:focus\s*\{[^}]*box-shadow:0 0 0 3px var\(--accent-glow\)/)
  })

  it('lista wyników ma własne przewijanie (overflow-y:auto), niezależne od panelu', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.filter.mockResolvedValueOnce({ data: Array.from({ length: 30 }, (_, i) => ({
      id: `t${i}`, title: `Wynik ${i}`, author: 'Ala', type: 'NOTE', detailLevel: 'MEDIUM', categoryId: 'cat-1',
    })) })

    const user = userEvent.setup()
    renderPanel()
    await user.click(screen.getByRole('button', { name: /filtry/i }))
    await screen.findByRole('dialog', { name: 'Filtry' })

    const authorSelect = authorSelectOf()
    await user.selectOptions(authorSelect, 'Ala')
    await user.click(screen.getByRole('button', { name: 'Zastosuj filtry' }))

    await screen.findByText('Wynik 0')
    const results = document.body.querySelector('.sop-list')
    const styleText = allStyleText()
    expect(styleText).toMatch(/\.sop-list\s*\{[^}]*overflow-y:\s*auto/)
    expect(results.querySelectorAll('.fp-result').length).toBe(30)
  })

  it('każdy wynik pokazuje tytuł, autora, typ, poziom szczegółowości i ścieżkę kategorii', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.filter.mockResolvedValueOnce({ data: [
      { id: 't1', title: 'Mój wynik', author: 'Ala', type: 'INSTRUCTION', detailLevel: 'HIGH', categoryId: 'sub-1' },
    ] })

    const user = userEvent.setup()
    renderPanel()
    await user.click(screen.getByRole('button', { name: /filtry/i }))
    await screen.findByRole('dialog', { name: 'Filtry' })
    const authorSelect = authorSelectOf()
    await user.selectOptions(authorSelect, 'Ala')
    await user.click(screen.getByRole('button', { name: 'Zastosuj filtry' }))

    await screen.findByText('Mój wynik')
    const meta = document.body.querySelector('.fp-result-meta')
    expect(meta).toHaveTextContent('Ala')
    expect(meta).toHaveTextContent('Instrukcja')
    // "Wysoki" pojawia się też jako etykieta chipa filtra poziomu — sprawdzamy konkretnie odznakę wyniku.
    expect(document.body.querySelector('.fp-result-level')).toHaveTextContent('Wysoki')
    expect(screen.getByText('Kategoria A › Podkategoria A1')).toBeInTheDocument()
  })

  it('kliknięcie wyniku nawiguje do tematu i zamyka panel', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.filter.mockResolvedValueOnce({ data: [
      { id: 'topic-xyz', title: 'Klikalny wynik', author: 'Ala', type: 'NOTE', detailLevel: 'LOW', categoryId: 'cat-1' },
    ] })

    const user = userEvent.setup()
    renderPanel()
    await user.click(screen.getByRole('button', { name: /filtry/i }))
    await screen.findByRole('dialog', { name: 'Filtry' })
    const authorSelect = authorSelectOf()
    await user.selectOptions(authorSelect, 'Ala')
    await user.click(screen.getByRole('button', { name: 'Zastosuj filtry' }))

    const result = await screen.findByText('Klikalny wynik')
    await user.click(result)

    expect(screen.queryByRole('dialog', { name: 'Filtry' })).not.toBeInTheDocument()
  })

  it('pusty stan przed wyszukiwaniem to estetyczna karta z ikoną, nie samotny napis', async () => {
    const user = userEvent.setup()
    renderPanel()
    await user.click(screen.getByRole('button', { name: /filtry/i }))
    await screen.findByRole('dialog', { name: 'Filtry' })

    const empty = document.body.querySelector('.sop-empty')
    expect(empty).toBeInTheDocument()
    expect(empty.querySelector('.sop-empty-icon')).toBeInTheDocument()
    expect(screen.getByText('Wybierz kryteria')).toBeInTheDocument()
  })

  it('brak wyników i stan błędu też pokazują estetyczną kartę z ikoną i pozwalają ponowić', async () => {
    const { topicsApi } = await import('@/api/services')
    topicsApi.filter.mockRejectedValueOnce(new Error('boom'))
    topicsApi.filter.mockResolvedValueOnce({ data: [] })

    const user = userEvent.setup()
    renderPanel()
    await user.click(screen.getByRole('button', { name: /filtry/i }))
    await screen.findByRole('dialog', { name: 'Filtry' })
    const authorSelect = authorSelectOf()
    await user.selectOptions(authorSelect, 'Ala')
    await user.click(screen.getByRole('button', { name: 'Zastosuj filtry' }))

    await screen.findByText('Nie udało się pobrać wyników')
    expect(document.body.querySelector('.sop-empty--error')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Spróbuj ponownie' }))
    await screen.findByText('Brak wyników')
    expect(document.body.querySelector('.sop-empty')).toBeInTheDocument()
  })

  it('panel zamyka się klawiszem Escape (po animacji wysunięcia)', async () => {
    const user = userEvent.setup()
    renderPanel()
    await user.click(screen.getByRole('button', { name: /filtry/i }))
    await screen.findByRole('dialog', { name: 'Filtry' })
    await waitFor(() => expect(document.body.querySelector('.sop-panel')).toHaveClass('sop-open'))

    act(() => { window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' })) })

    // Animacja wyjścia: klasa "--open" znika od razu (cofa transform), sam dialog usuwany z DOM
    // dopiero po zakończeniu animacji.
    await waitFor(() => expect(document.body.querySelector('.sop-panel')).not.toHaveClass('sop-open'))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Filtry' })).not.toBeInTheDocument(), { timeout: 1000 })
  })

  it('panel zamyka się kliknięciem tła (backdrop), po animacji wysunięcia', async () => {
    const user = userEvent.setup()
    renderPanel()
    await user.click(screen.getByRole('button', { name: /filtry/i }))
    await screen.findByRole('dialog', { name: 'Filtry' })
    await waitFor(() => expect(document.body.querySelector('.sop-panel')).toHaveClass('sop-open'))

    await user.click(document.body.querySelector('.sop-backdrop'))

    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Filtry' })).not.toBeInTheDocument(), { timeout: 1000 })
  })
})
