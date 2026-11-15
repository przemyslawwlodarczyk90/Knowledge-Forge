import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// Osobny plik od vite.config.js (który steruje buildem produkcyjnym i dev-serwerem) — testy
// mają inne potrzeby (środowisko jsdom, setupFiles), a `vitest/config`'s defineConfig scala
// pola "test" z resztą configu w sposób, którego zwykłe `vite`'owe defineConfig nie rozumie.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.js'],
    globals: false,
    css: false,
  },
})
