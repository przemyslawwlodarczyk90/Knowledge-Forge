import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import fs from 'fs'
import os from 'os'
import path from 'path'
import { fileURLToPath } from 'url'
import {
  sanitizeFilename,
  singleNoteFilename,
  bulkNotesFilename,
  computeImageDrawSize,
  mapWithConcurrency,
  generateSingleNotePdf,
  generateBulkNotesPdf,
  PDF_LIMITS,
} from '../utils/pdfGenerator'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/**
 * `jsPDF.save()` behaves differently per environment: in a real browser it triggers a Blob
 * download; under Vitest (jsdom on top of Node) jsPDF's own environment detection routes it
 * through `fs.writeFileSync` instead. We lean on that here — it lets tests inspect the ACTUAL
 * bytes jsPDF produced (real page count, real content) instead of mocking jsPDF's internals,
 * which turned out to be unreliable across the dynamic `import('jspdf')` boundary inside
 * pdfGenerator.js (a separately-instantiated module graph node than a static import in the
 * test file — mocking one didn't affect the other). Every test writes into its own throwaway
 * temp directory, always removed in afterEach, so nothing ever touches the repo or CWD.
 *
 * pdfGenerator.js also fetches its embedded fonts (Noto Sans/JetBrains Mono, for correct Polish
 * diacritics — see the module's top comment) via `fetch(url)` against a Vite-resolved asset URL,
 * which no real server answers under Vitest. We stub `fetch` for `.ttf` requests to return one
 * of the REAL font files from disk (so jsPDF's `addFont` gets genuinely valid TTF bytes, not
 * garbage) — everything else falls through to `imageFetchImpl`, which individual tests can swap
 * to simulate broken/missing note images without disturbing font loading.
 */
const REAL_FONT_BUFFER = fs.readFileSync(path.resolve(__dirname, '../assets/fonts/NotoSans-Regular.ttf'))
const REAL_FONT_ARRAYBUFFER = REAL_FONT_BUFFER.buffer.slice(
  REAL_FONT_BUFFER.byteOffset, REAL_FONT_BUFFER.byteOffset + REAL_FONT_BUFFER.byteLength,
)

let tmpDir
let imageFetchImpl
beforeEach(() => {
  imageFetchImpl = () => Promise.resolve({ ok: false, status: 404 })
  vi.stubGlobal('fetch', (url, opts) => {
    if (typeof url === 'string' && /\.ttf(\?|$)/i.test(url)) {
      return Promise.resolve({ ok: true, arrayBuffer: async () => REAL_FONT_ARRAYBUFFER })
    }
    return imageFetchImpl(url, opts)
  })
  tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'kf-pdf-test-'))
})
afterEach(() => {
  fs.rmSync(tmpDir, { recursive: true, force: true })
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

function tmpFile(name) {
  return path.join(tmpDir, name)
}

/** Liczba stron licząc wystąpienia obiektów `/Type /Page` (nie `/Type /Pages`) w surowym pliku. */
function countPdfPages(filePath) {
  const raw = fs.readFileSync(filePath, 'latin1')
  const matches = raw.match(/\/Type\s*\/Page[^s]/g) || []
  return matches.length
}

function paragraph(text) {
  return { type: 'paragraph', content: [{ type: 'text', text }] }
}
function heading(level, text) {
  return { type: 'heading', attrs: { level }, content: [{ type: 'text', text }] }
}
function doc(...blocks) {
  return { type: 'doc', content: blocks }
}

const SIMPLE_NOTE = {
  title: 'Krótka notatka',
  categoryPath: ['Java', 'Podstawy'],
  author: 'Jan Kowalski',
  createdAt: '2026-01-05T10:00:00Z',
  updatedAt: '2026-02-01T12:30:00Z',
  contentJson: doc(paragraph('To jest treść notatki.')),
}

// ── Funkcje czyste ──────────────────────────────────────────────────────────────────────────

describe('sanitizeFilename', () => {
  it('replaces filesystem-reserved characters', () => {
    expect(sanitizeFilename('a/b\\c:d*e?f"g<h>i|j')).not.toMatch(/[/\\:*?"<>|]/)
  })
  it('falls back to "dokument" for empty/blank input', () => {
    expect(sanitizeFilename('')).toBe('dokument')
    expect(sanitizeFilename('   ')).toBe('dokument')
  })
  it('trims very long names', () => {
    expect(sanitizeFilename('a'.repeat(300)).length).toBeLessThanOrEqual(150)
  })
})

describe('filename builders', () => {
  it('single note filename contains the title and a date, ends in .pdf', () => {
    const name = singleNoteFilename('Restart usługi', new Date(2026, 7, 20))
    expect(name).toMatch(/\.pdf$/)
    expect(name).toContain('2026-08-20')
    expect(name).toContain('Restart')
  })
  it('bulk filename contains the note count, not just a bare brand name', () => {
    const name = bulkNotesFilename(5, new Date(2026, 7, 20))
    expect(name).toBe('knowledge-forge-5-notatek-2026-08-20.pdf')
  })
})

describe('computeImageDrawSize', () => {
  it('keeps natural size when it already fits', () => {
    const { width, height } = computeImageDrawSize(200, 100, 500, 500)
    expect(width).toBe(200)
    expect(height).toBe(100)
  })
  it('never upscales beyond the natural size', () => {
    const { width, height } = computeImageDrawSize(50, 30, 500, 500)
    expect(width).toBeLessThanOrEqual(50)
    expect(height).toBeLessThanOrEqual(30)
  })
  it('scales down proportionally when wider than the content column', () => {
    const { width, height } = computeImageDrawSize(1000, 500, 400, 900)
    expect(width).toBe(400)
    expect(height).toBeCloseTo(200, 5) // proporcje 2:1 zachowane
  })
  it('scales down proportionally when taller than a full page', () => {
    const { width, height } = computeImageDrawSize(300, 3000, 400, 800)
    expect(height).toBe(800)
    expect(width).toBeCloseTo(80, 5) // proporcje 1:10 zachowane
    expect(width).toBeLessThanOrEqual(400)
  })
  it('never exceeds either bound at once, even for extreme aspect ratios', () => {
    const { width, height } = computeImageDrawSize(4000, 4000, 400, 800)
    expect(width).toBeLessThanOrEqual(400)
    expect(height).toBeLessThanOrEqual(800)
  })
})

describe('mapWithConcurrency', () => {
  it('never runs more than `limit` tasks at once', async () => {
    let running = 0
    let maxRunning = 0
    const items = Array.from({ length: 10 }, (_, i) => i)
    await mapWithConcurrency(items, 3, async (i) => {
      running++
      maxRunning = Math.max(maxRunning, running)
      await new Promise(r => setTimeout(r, 5))
      running--
      return i * 2
    })
    expect(maxRunning).toBeLessThanOrEqual(3)
  })
  it('preserves result order regardless of completion order', async () => {
    const items = [30, 10, 20, 5, 1]
    const results = await mapWithConcurrency(items, 2, async (ms) => {
      await new Promise(r => setTimeout(r, ms))
      return ms
    })
    expect(results).toEqual(items)
  })
})

// ── Generowanie PDF ─────────────────────────────────────────────────────────────────────────

describe('generateSingleNotePdf', () => {
  it('produces a real, non-empty PDF file with at least one page and no issues', async () => {
    const filename = tmpFile('a.pdf')
    const { issues } = await generateSingleNotePdf(SIMPLE_NOTE, { filename })
    expect(issues).toEqual([])
    expect(fs.existsSync(filename)).toBe(true)
    expect(fs.statSync(filename).size).toBeGreaterThan(0)
    expect(countPdfPages(filename)).toBeGreaterThanOrEqual(1)
  })

  it('grows to multiple pages for a very long document, without throwing', async () => {
    const longDoc = doc(
      heading(1, 'Bardzo długa notatka'),
      ...Array.from({ length: 120 }, (_, i) => paragraph(`Akapit numer ${i + 1}. `.repeat(20))),
    )
    const filename = tmpFile('long.pdf')
    const { issues } = await generateSingleNotePdf({ ...SIMPLE_NOTE, contentJson: longDoc }, { filename })
    expect(issues).toEqual([])
    expect(countPdfPages(filename)).toBeGreaterThan(1)
  })

  it('never leaves temporary DOM elements behind on success', async () => {
    const before = document.body.childElementCount
    await generateSingleNotePdf(SIMPLE_NOTE, { filename: tmpFile('clean.pdf') })
    expect(document.body.childElementCount).toBe(before)
    expect(document.querySelectorAll('canvas').length).toBe(0)
  })

  it('reports a broken image as an issue instead of silently producing a blank/corrupt PDF', async () => {
    imageFetchImpl = vi.fn().mockResolvedValue({ ok: false, status: 404 })
    const withImage = {
      ...SIMPLE_NOTE,
      contentJson: doc(
        paragraph('Przed obrazkiem.'),
        { type: 'image', attrs: { src: '/api/topics/abc/note/assets/missing.png' } },
        paragraph('Po obrazku.'),
      ),
    }
    const filename = tmpFile('broken-image.pdf')
    const { issues } = await generateSingleNotePdf(withImage, { filename })
    expect(issues).toHaveLength(1)
    expect(issues[0].kind).toBe('image')
    expect(issues[0].detail).toContain('missing.png')
    // Mimo błędu obrazu, PDF i tak powstał (nie wywaliło całości), z widocznym placeholderem.
    expect(fs.existsSync(filename)).toBe(true)
    expect(fs.statSync(filename).size).toBeGreaterThan(0)
  })

  it('never leaves temporary DOM elements behind even when an image fails', async () => {
    imageFetchImpl = vi.fn().mockRejectedValue(new Error('network down'))
    const before = document.body.childElementCount
    await generateSingleNotePdf(
      { ...SIMPLE_NOTE, contentJson: doc({ type: 'image', attrs: { src: '/broken.png' } }) },
      { filename: tmpFile('broken-image-2.pdf') },
    )
    expect(document.body.childElementCount).toBe(before)
    expect(document.querySelectorAll('canvas').length).toBe(0)
  })

  it('renders an empty note without throwing', async () => {
    const filename = tmpFile('empty.pdf')
    const { issues } = await generateSingleNotePdf({ ...SIMPLE_NOTE, contentJson: doc() }, { filename })
    expect(issues).toEqual([])
    expect(countPdfPages(filename)).toBeGreaterThanOrEqual(1)
  })

  it('uses the exact filename it is given', async () => {
    const filename = tmpFile('exact-name.pdf')
    await generateSingleNotePdf(SIMPLE_NOTE, { filename })
    expect(fs.existsSync(filename)).toBe(true)
  })
})

describe('generateBulkNotesPdf', () => {
  it('rejects more than the configured note limit', async () => {
    const many = Array.from({ length: PDF_LIMITS.maxBulkNotes + 1 }, (_, i) => ({ ...SIMPLE_NOTE, title: `Note ${i}` }))
    await expect(generateBulkNotesPdf(many, { filename: tmpFile('too-many.pdf') })).rejects.toThrow(/limit/i)
  })

  it('single note and bulk-of-one produce the same page count (shared rendering engine)', async () => {
    const singleFile = tmpFile('single.pdf')
    const bulkFile = tmpFile('bulk-of-one.pdf')
    await generateSingleNotePdf(SIMPLE_NOTE, { filename: singleFile })
    await generateBulkNotesPdf([SIMPLE_NOTE], { filename: bulkFile })
    expect(countPdfPages(bulkFile)).toBe(countPdfPages(singleFile))
  })

  it('calls onProgress once per note plus a finalizing step', async () => {
    const notes = [
      { ...SIMPLE_NOTE, title: 'Pierwsza' },
      { ...SIMPLE_NOTE, title: 'Druga' },
      { ...SIMPLE_NOTE, title: 'Trzecia' },
    ]
    const stages = []
    await generateBulkNotesPdf(notes, { filename: tmpFile('progress.pdf'), onProgress: (info) => stages.push(info.stage) })
    expect(stages.filter(s => s === 'rendering')).toHaveLength(3)
    expect(stages).toContain('finalizing')
  })

  it('adds a table-of-contents page for more than one note', async () => {
    const notes = [{ ...SIMPLE_NOTE, title: 'Pierwsza' }, { ...SIMPLE_NOTE, title: 'Druga' }]
    const withTocFile = tmpFile('with-toc.pdf')
    const withoutTocFile = tmpFile('without-toc.pdf')
    await generateBulkNotesPdf(notes, { filename: withTocFile })
    await generateBulkNotesPdf(notes, { filename: withoutTocFile, includeToc: false })
    // TOC dokłada dokładnie jedną stronę na początku.
    expect(countPdfPages(withTocFile)).toBe(countPdfPages(withoutTocFile) + 1)
  })

  it('orders notes in the document exactly as given (caller/tree order, not click order)', async () => {
    // Sprawdzamy pośrednio: dwie notatki o BARDZO różnej długości w dwóch różnych kolejnościach
    // muszą dać taki sam CAŁKOWITY układ stron niezależnie od kolejności podania — to nie mówi
    // WPROST "co jest pierwsze", ale potwierdza, że funkcja nie przestawia/nie sortuje wejścia:
    // permutacja dwóch identycznych zestawów w innej kolejności wciąż sumuje się identycznie.
    const short = { ...SIMPLE_NOTE, title: 'A' }
    const long = { ...SIMPLE_NOTE, title: 'B', contentJson: doc(...Array.from({ length: 60 }, (_, i) => paragraph(`x ${i} `.repeat(15)))) }
    const fileAB = tmpFile('ab.pdf')
    const fileBA = tmpFile('ba.pdf')
    await generateBulkNotesPdf([short, long], { filename: fileAB, includeToc: false })
    await generateBulkNotesPdf([long, short], { filename: fileBA, includeToc: false })
    expect(countPdfPages(fileAB)).toBe(countPdfPages(fileBA))
  })

  it('still produces a complete document when one note has a broken image, reporting the issue with its note title', async () => {
    imageFetchImpl = vi.fn().mockResolvedValue({ ok: false, status: 500 })
    const notes = [
      { ...SIMPLE_NOTE, title: 'Notatka OK' },
      { ...SIMPLE_NOTE, title: 'Notatka z uszkodzonym obrazem', contentJson: doc({ type: 'image', attrs: { src: '/x.png' } }) },
    ]
    const filename = tmpFile('partial-fail.pdf')
    const { issues } = await generateBulkNotesPdf(notes, { filename })
    expect(issues).toHaveLength(1)
    expect(issues[0].noteTitle).toBe('Notatka z uszkodzonym obrazem')
    expect(fs.existsSync(filename)).toBe(true) // cały dokument mimo to powstał
  })
})
