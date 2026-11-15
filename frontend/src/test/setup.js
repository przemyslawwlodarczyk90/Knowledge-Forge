import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// `globals: false` w vitest.config.js (świadomie — testy importują describe/it/expect jawnie,
// bez niejawnych globali) oznacza, że automatyczne sprzątanie @testing-library/react (które
// normalnie samo podpina się pod globalny `afterEach`) się nie uruchamia — robimy to jawnie tu,
// raz, dla wszystkich plików testowych, żeby kolejne render() w tym samym pliku nie kumulowały
// się w document.body.
afterEach(() => {
  cleanup()
})
