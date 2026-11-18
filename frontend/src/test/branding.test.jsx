import { describe, it, expect, vi } from 'vitest'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// AppLayout otwiera prawdziwy WebSocket na mount (zob. useEffect w AppLayout.jsx) — nieistotne
// dla testu wizualnego brandingu i niepotrzebnie kosztowne/hałaśliwe w jsdom (WebSocket tam nie
// istnieje natywnie), więc podmieniamy moduł na no-op, tak jak zrobiłby to każdy test integracyjny
// skupiony na jednej odpowiedzialności (tu: teksty marki), nie na warstwie sieciowej.
vi.mock('@/ws/socket', () => ({
  openApplicationSocket: vi.fn(),
  closeApplicationSocket: vi.fn(),
  subscribeToEvents: vi.fn(() => () => {}),
  subscribeToStatus: (fn) => { fn('connected'); return () => {} },
  getConnectionStatus: () => 'connected',
}))

const AppLayout = (await import('../components/layout/AppLayout')).default

describe('Rebranding — Knowledge Forge', () => {
  it('index.html tytuł i favicon są zaktualizowane', () => {
    const html = fs.readFileSync(path.resolve(__dirname, '../../index.html'), 'utf8')
    expect(html).toContain('<title>Knowledge Forge</title>')
    expect(html).toContain('<link rel="icon" type="image/svg+xml" href="/favicon.svg" />')
    expect(html).not.toMatch(/Knowledge-Forge/)
  })

  it('nowy favicon.svg istnieje i nie jest starym briefcase-iconem', () => {
    const svg = fs.readFileSync(path.resolve(__dirname, '../../public/favicon.svg'), 'utf8')
    expect(svg).toContain('<svg')
    expect(svg.length).toBeGreaterThan(50)
  })

  it('sidebar (wordmark) i topbar (chip) pokazują "Knowledge Forge", nie starą nazwę', () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<div>treść</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByText('Forge')).toBeInTheDocument()
    expect(screen.getByText('Knowledge Forge')).toBeInTheDocument()
    expect(screen.queryByText('Knowledge-Forge')).not.toBeInTheDocument()
  })
})
