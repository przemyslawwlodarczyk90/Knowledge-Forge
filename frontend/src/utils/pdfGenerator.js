/**
 * Generator PDF dla notatek Knowledge Forge — WSPÓLNY silnik dla pojedynczej notatki i druku
 * masowego (`generateSingleNotePdf` / `generateBulkNotesPdf`), zastępujący starą metodę
 * "html2canvas -> jeden gigantyczny JPEG -> przesuwaj go między stronami" (patrz git history /
 * NotePanel.jsx sprzed tej zmiany). Ten stary mechanizm renderował CAŁY dokument jako jeden
 * bardzo wysoki raster i dzielił go na strony wyłącznie przesuwając `position` obrazu — więc
 * ciął linie tekstu i obrazki w dowolnym miejscu, nakładał treść na marginesy i przy dłuższych
 * notatkach potrafił zjeść bardzo dużo pamięci (jeden canvas rzędu kilku tysięcy px wysokości).
 *
 * Nowe podejście: treść ProseMirror (`note.contentJson` — dokładnie to, co edytor TipTap
 * zapisuje/wczytuje, zob. NotePanel.jsx) jest odczytywana WPROST jako drzewo bloków (akapit,
 * nagłówek, lista, cytat, blok kodu, obraz…) i rysowana natywnym API tekstowym jsPDF
 * (`pdf.text()`, `pdf.rect()`, `pdf.addImage()`) z ręczną, świadomą paginacją blok-po-bloku:
 *   - tekst zostaje tekstem w PDF (przeszukiwalny, mały plik) — NIE jest rastrowany,
 *   - każdy blok jest łamany na linie SAMODZIELNIE (własny word-wrap, mierzony przez
 *     `pdf.getTextWidth()`) — nigdy nie przecinamy pojedynczej linii w połowie,
 *   - blok, który nie mieści się na bieżącej stronie, ale zmieściłby się na świeżej stronie,
 *     jest w CAŁOŚCI przenoszony (nie dzielony) — zob. `placeBlock()`,
 *   - blok wyższy niż jedna cała strona (bardzo długi akapit/blok kodu) jest jedynym
 *     wyjątkiem: dzielimy go linia-po-linii, nigdy w środku linii,
 *   - obrazy są rasteryzowane WYŁĄCZNIE pojedynczo (jeden mały canvas per obrazek, żeby
 *     przekonwertować pobrane bajty na format akceptowany przez `addImage`) — nie ma jednego
 *     canvasu na cały dokument.
 *
 * Biblioteka: jsPDF (już zależność projektu, MIT, ładowana dynamicznie dopiero z tej funkcji —
 * `await import('jspdf')` — więc nie powiększa głównego bundla). html2canvas NIE jest już
 * potrzebny i został usunięty z zależności (zob. package.json) — cały dokument to teraz
 * prawdziwy tekst + osadzone obrazy, żadnej rasteryzacji całości.
 *
 * Czcionki: jsPDF-owe wbudowane "standard 14" fonty (helvetica/courier) używają WinAnsiEncoding,
 * które NIE obejmuje polskich znaków diakrytycznych (ł, ń, ś, ż, ź, ć, ą, ę) — z nimi tekst
 * renderowałby się uszkodzony. Dlatego osadzamy własne czcionki TrueType z pełnym pokryciem
 * Unicode/Latin Extended-A: Noto Sans (tekst) + JetBrains Mono (kod), obie Google Fonts na
 * licencji SIL OFL 1.1 (wolno osadzać/dystrybuować), zob. `src/assets/fonts/README.md`.
 * Pliki .ttf importowane przez Vite (`?url`) i pobierane/rejestrowane w jsPDF WYŁĄCZNIE w tym
 * samym leniwym module co sam jsPDF — nie powiększają głównego bundla.
 */
import notoSansRegularUrl from '../assets/fonts/NotoSans-Regular.ttf?url'
import notoSansBoldUrl from '../assets/fonts/NotoSans-Bold.ttf?url'
import notoSansItalicUrl from '../assets/fonts/NotoSans-Italic.ttf?url'
import notoSansBoldItalicUrl from '../assets/fonts/NotoSans-BoldItalic.ttf?url'
import jetBrainsMonoRegularUrl from '../assets/fonts/JetBrainsMono-Regular.ttf?url'
import jetBrainsMonoBoldUrl from '../assets/fonts/JetBrainsMono-Bold.ttf?url'

const FONT_SANS = 'NotoSans'
const FONT_MONO = 'JetBrainsMono'

// ── Geometria strony (jednostki: pt, 1pt = 1/72 cala; A4 przez jsPDF format:'a4') ──────────
const MARGIN = { top: 64, bottom: 60, left: 54, right: 54 }
const FOOTER_TEXT = 'Knowledge Forge'
const MAX_CONCURRENT_REQUESTS = 4
const MAX_BULK_NOTES = 50

// ── Typografia / kolory (osobne od stylów UI — nie kopiujemy CSS aplikacji) ─────────────────
const STYLE = {
  body:      { size: 10.5, lineH: 15.5, color: [30, 41, 59] },
  h1:        { size: 18,   lineH: 23,   color: [15, 23, 42] },
  h2:        { size: 14.5, lineH: 19,   color: [15, 23, 42] },
  h3:        { size: 12.5, lineH: 17,   color: [15, 23, 42] },
  code:      { size: 9,    lineH: 12.5, color: [30, 41, 59] },
  meta:      { size: 9,    lineH: 13,   color: [100, 116, 139] },
  quote:     { size: 10.5, lineH: 15.5, color: [71, 85, 105] },
  footer:    { size: 8,    color: [148, 163, 184] },
  noteTitle: { size: 19,   lineH: 24,   color: [15, 23, 42] },
  tocTitle:  { size: 16,   lineH: 21,   color: [15, 23, 42] },
  tocItem:   { size: 10.5, lineH: 16,   color: [30, 41, 59] },
  tocMeta:   { size: 8.5,  lineH: 12,   color: [100, 116, 139] },
}
const CODE_BG = [241, 245, 249]
const QUOTE_BAR = [148, 163, 184]
const ACCENT = [37, 99, 235]
const HR_COLOR = [203, 213, 225]
const ERROR_COLOR = [185, 28, 28]
const ERROR_BG = [254, 242, 242]

const GAP = { paragraph: 8, heading: 6, headingTop: 14, list: 8, listItem: 3, quote: 10, code: 12, image: 10, hr: 12 }

// ═════════════════════════════════════════════════════════════════════════════════════════
// Pomocnicze — funkcje CZYSTE (bez zależności na jsPDF ani DOM), łatwe do przetestowania
// ═════════════════════════════════════════════════════════════════════════════════════════

/** Usuwa znaki niedozwolone/kłopotliwe w nazwach plików na Windows/macOS/Linux, przycina długość. */
export function sanitizeFilename(name) {
  const cleaned = String(name ?? '')
    .replace(/[/\\:*?"<>|]/g, '-')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/\.+$/, '')
  const safe = cleaned || 'dokument'
  return safe.length > 150 ? safe.slice(0, 150).trim() : safe
}

/** np. new Date(2026,7,20) -> "2026-08-20" (do nazw plików — sortowalne, bez separatorów kłopotliwych dla FS). */
function isoDateForFilename(date = new Date()) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/** Nazwa pliku dla pojedynczej notatki. */
export function singleNoteFilename(title, date = new Date()) {
  return sanitizeFilename(`${title || 'notatka'}-${isoDateForFilename(date)}.pdf`)
}

/** Nazwa pliku dla druku masowego — zawiera liczbę notatek, nie samą nazwę marki (spec pkt 5). */
export function bulkNotesFilename(count, date = new Date()) {
  return sanitizeFilename(`knowledge-forge-${count}-notatek-${isoDateForFilename(date)}.pdf`)
}

/**
 * Skaluje naturalny rozmiar obrazu tak, żeby zmieścił się w (maxWidth × maxPageHeight),
 * zachowując proporcje i NIGDY nie powiększając ponad naturalny rozmiar. `maxPageHeight`
 * to wysokość CAŁEJ dostępnej strony (nie bieżącego miejsca) — obraz jest skalowany
 * względem tego, czy w ogóle zmieściłby się na jakiejkolwiek (pustej) stronie; decyzję
 * "czy zmieści się TU, czy trzeba przenieść na nową stronę" podejmuje osobno `placeBlock`.
 */
export function computeImageDrawSize(naturalWidth, naturalHeight, maxWidth, maxPageHeight) {
  if (!naturalWidth || !naturalHeight) return { width: 0, height: 0 }
  let w = naturalWidth
  let h = naturalHeight
  if (w > maxWidth) {
    const s = maxWidth / w
    w *= s; h *= s
  }
  if (h > maxPageHeight) {
    const s = maxPageHeight / h
    w *= s; h *= s
  }
  return { width: w, height: h }
}

/** Prosty, ograniczony pool współbieżności — max `limit` promes naraz, kolejność wyników zachowana. */
export async function mapWithConcurrency(items, limit, fn) {
  const results = new Array(items.length)
  let next = 0
  async function worker() {
    while (next < items.length) {
      const i = next++
      results[i] = await fn(items[i], i)
    }
  }
  const workers = Array.from({ length: Math.min(limit, items.length) }, () => worker())
  await Promise.all(workers)
  return results
}

// ═════════════════════════════════════════════════════════════════════════════════════════
// Wrap tekstu (mierzone przez jsPDF — dokładnie to, co zostanie narysowane)
// ═════════════════════════════════════════════════════════════════════════════════════════

function fontFor(style) {
  if (style?.code) return { family: FONT_MONO, weight: style.bold ? 'bold' : 'normal' }
  let weight = 'normal'
  if (style?.bold && style?.italic) weight = 'bolditalic'
  else if (style?.bold) weight = 'bold'
  else if (style?.italic) weight = 'italic'
  return { family: FONT_SANS, weight }
}

function applyFont(pdf, style, size) {
  const { family, weight } = fontFor(style)
  pdf.setFont(family, weight)
  pdf.setFontSize(size)
}

/** Dzieli pojedynczy "niełamliwy" token (np. bardzo długi URL) znak po znaku, żeby nigdy nie wyjść poza margines. */
function splitLongToken(pdf, text, style, size, maxWidth) {
  applyFont(pdf, style, size)
  const chars = Array.from(text)
  const chunks = []
  let cur = ''
  for (const ch of chars) {
    const attempt = cur + ch
    if (cur && pdf.getTextWidth(attempt) > maxWidth) {
      chunks.push(cur)
      cur = ch
    } else {
      cur = attempt
    }
  }
  if (cur) chunks.push(cur)
  return chunks
}

/**
 * runs: [{ text, style: {bold,italic,strike,code} } | { break: true }] (hardBreak wymuszony podział linii)
 * Zwraca tablicę linii, każda linia to tablica segmentów { text, style, width } gotowych do narysowania.
 */
function wrapRuns(pdf, runs, size, maxWidth) {
  const tokens = []
  for (const run of runs) {
    if (run.break) { tokens.push({ break: true }); continue }
    if (!run.text) continue
    for (const part of run.text.split(/(\s+)/)) {
      if (part.length === 0) continue
      tokens.push({ text: part, isSpace: /^\s+$/.test(part), style: run.style })
    }
  }

  const lines = []
  let current = []
  let width = 0
  const flush = () => {
    while (current.length && current[current.length - 1].isSpace) {
      width -= current[current.length - 1].width
      current.pop()
    }
    lines.push(current)
    current = []
    width = 0
  }

  for (const tok of tokens) {
    if (tok.break) { flush(); continue }
    if (tok.isSpace && current.length === 0) continue // brak spacji na początku nowej linii
    applyFont(pdf, tok.style, size)
    const w = pdf.getTextWidth(tok.text)
    if (w > maxWidth) {
      for (const chunk of splitLongToken(pdf, tok.text, tok.style, size, maxWidth)) {
        applyFont(pdf, tok.style, size)
        const cw = pdf.getTextWidth(chunk)
        if (width + cw > maxWidth && current.length) flush()
        current.push({ text: chunk, style: tok.style, width: cw })
        width += cw
      }
      continue
    }
    if (width + w > maxWidth && current.length) {
      flush()
      if (tok.isSpace) continue
    }
    current.push({ text: tok.text, style: tok.style, width: w })
    width += w
  }
  if (current.length) flush()
  return lines.length ? lines : [[]]
}

/** Blok kodu: łamany po istniejących znakach nowej linii, potem (jeśli trzeba) znak-po-znaku — bez word-wrap. */
function wrapCodeLines(pdf, text, size, maxWidth) {
  applyFont(pdf, {}, size)
  const rawLines = String(text ?? '').split('\n')
  const out = []
  for (const raw of rawLines) {
    if (raw === '') { out.push(''); continue }
    if (pdf.getTextWidth(raw) <= maxWidth) { out.push(raw); continue }
    let cur = ''
    for (const ch of Array.from(raw)) {
      const attempt = cur + ch
      if (cur && pdf.getTextWidth(attempt) > maxWidth) {
        out.push(cur)
        cur = ch
      } else {
        cur = attempt
      }
    }
    if (cur) out.push(cur)
  }
  return out
}

// ═════════════════════════════════════════════════════════════════════════════════════════
// Osadzone czcionki (Noto Sans / JetBrains Mono) — zob. komentarz na górze pliku
// ═════════════════════════════════════════════════════════════════════════════════════════

/** Uint8Array -> base64, kawałkami (String.fromCharCode.apply na całym pliku naraz wysypałoby stos). */
function arrayBufferToBase64(buffer) {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  const chunkSize = 0x8000
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunkSize))
  }
  return btoa(binary)
}

async function fetchFontAsBase64(url) {
  const resp = await fetch(url)
  if (!resp.ok) throw new Error(`Nie udało się pobrać czcionki ${url}: HTTP ${resp.status}`)
  return arrayBufferToBase64(await resp.arrayBuffer())
}

// Pobranie i base64-owanie plików fontów jest kosztowne (6 requestów, ~700KB) — cache'ujemy
// WYNIK (nie same rejestracje w jsPDF, które są per-dokument) w module, więc drugi i kolejny
// wygenerowany PDF w tej samej karcie przeglądarki nie pobiera plików ponownie.
let fontDataPromise = null
function loadFontData() {
  if (!fontDataPromise) {
    fontDataPromise = Promise.all([
      fetchFontAsBase64(notoSansRegularUrl),
      fetchFontAsBase64(notoSansBoldUrl),
      fetchFontAsBase64(notoSansItalicUrl),
      fetchFontAsBase64(notoSansBoldItalicUrl),
      fetchFontAsBase64(jetBrainsMonoRegularUrl),
      fetchFontAsBase64(jetBrainsMonoBoldUrl),
    ]).then(([sansRegular, sansBold, sansItalic, sansBoldItalic, monoRegular, monoBold]) =>
      ({ sansRegular, sansBold, sansItalic, sansBoldItalic, monoRegular, monoBold }))
      .catch(err => { fontDataPromise = null; throw err }) // błąd -> nie cache'uj porażki, spróbuj ponownie następnym razem
  }
  return fontDataPromise
}

/** Rejestruje osadzone czcionki w TYM konkretnym dokumencie jsPDF (rejestracja jest per-instancja). */
async function registerFonts(pdf) {
  const f = await loadFontData()
  pdf.addFileToVFS('NotoSans-Regular.ttf', f.sansRegular)
  pdf.addFont('NotoSans-Regular.ttf', FONT_SANS, 'normal')
  pdf.addFileToVFS('NotoSans-Bold.ttf', f.sansBold)
  pdf.addFont('NotoSans-Bold.ttf', FONT_SANS, 'bold')
  pdf.addFileToVFS('NotoSans-Italic.ttf', f.sansItalic)
  pdf.addFont('NotoSans-Italic.ttf', FONT_SANS, 'italic')
  pdf.addFileToVFS('NotoSans-BoldItalic.ttf', f.sansBoldItalic)
  pdf.addFont('NotoSans-BoldItalic.ttf', FONT_SANS, 'bolditalic')
  pdf.addFileToVFS('JetBrainsMono-Regular.ttf', f.monoRegular)
  pdf.addFont('JetBrainsMono-Regular.ttf', FONT_MONO, 'normal')
  pdf.addFileToVFS('JetBrainsMono-Bold.ttf', f.monoBold)
  pdf.addFont('JetBrainsMono-Bold.ttf', FONT_MONO, 'bold')
}

// ═════════════════════════════════════════════════════════════════════════════════════════
// Silnik paginacji — jeden PdfBuilder na cały dokument (pojedyncza notatka LUB zbiorczy PDF)
// ═════════════════════════════════════════════════════════════════════════════════════════

class PdfBuilder {
  constructor(JsPdfCtor) {
    this.pdf = new JsPdfCtor({ unit: 'pt', format: 'a4', compress: true })
    this.pageWidth = this.pdf.internal.pageSize.getWidth()
    this.pageHeight = this.pdf.internal.pageSize.getHeight()
    this.margin = MARGIN
    this.contentWidth = this.pageWidth - MARGIN.left - MARGIN.right
    this.contentBottom = this.pageHeight - MARGIN.bottom
    this.fullPageHeight = this.contentBottom - MARGIN.top
    this.y = MARGIN.top
    this.startedAnyPage = false
    this.tocEntries = null // gdy nie-null: zbieramy {title, page} do wstawienia jako pierwsza strona
  }

  /** Musi być await-owane PRZED jakimkolwiek rysowaniem — rejestruje osadzone fonty w tym dokumencie. */
  async ready() {
    await registerFonts(this.pdf)
    return this
  }

  /** Zaczyna nową, "czystą" stronę — pierwsze wywołanie w dokumencie NIE dodaje pustej strony na start. */
  freshPage() {
    if (this.startedAnyPage) this.pdf.addPage()
    this.startedAnyPage = true
    this.y = this.margin.top
  }

  /** Miejsce zostające do końca strony (od bieżącego `y`). */
  remainingHeight() {
    return this.contentBottom - this.y
  }

  /**
   * Decyduje, GDZIE narysować blok o zmierzonej wysokości `height` (+ opcjonalny "lookahead"
   * doliczony tylko do decyzji, np. żeby nagłówek nie został osierocony na dole strony):
   *   'here'  — mieści się od bieżącego `y`,
   *   'moved' — nie mieści się tu, ale mieści się w całości na świeżej stronie -> przenosimy,
   *   'split' — wyższy niż cała strona -> wywołujący musi rysować linia-po-linii z ensureSpace.
   */
  placeBlock(height, lookahead = 0) {
    if (this.y + height + lookahead <= this.contentBottom) return 'here'
    if (height <= this.fullPageHeight) {
      this.freshPage()
      return 'moved'
    }
    return 'split'
  }

  /** Dla trybu 'split' — zapewnia miejsce na kolejną linię, w razie potrzeby łamiąc stronę. */
  ensureSpace(height) {
    if (this.y + height > this.contentBottom) this.freshPage()
  }

  save(filename) {
    this.pdf.save(filename)
  }

  /** Bajty gotowego PDF jako Blob — używane zamiast save() przez wywołujących, którzy sami
   *  decydują co zrobić z wynikiem (np. panel ratunkowy: otwiera go w nowej karcie zamiast
   *  automatycznie zapisywać na dysk, zob. emergencyRecovery.js). */
  blob() {
    return this.pdf.output('blob')
  }
}

/** Rysuje blok linii (akapit/nagłówek/cytat/kod…) z pełną logiką umieszczenia opisaną w placeBlock(). */
function drawLinesBlock(builder, lines, lineHeight, drawLine, lookahead = 0) {
  const totalHeight = lines.length * lineHeight
  const decision = builder.placeBlock(totalHeight, lookahead)
  if (decision === 'split') {
    for (const line of lines) {
      builder.ensureSpace(lineHeight)
      drawLine(line, builder.y)
      builder.y += lineHeight
    }
  } else {
    for (const line of lines) {
      drawLine(line, builder.y)
      builder.y += lineHeight
    }
  }
  return decision
}

function drawTextLine(pdf, line, x0, y, size, defaultColor) {
  let x = x0
  for (const seg of line) {
    applyFont(pdf, seg.style, size)
    pdf.setTextColor(...(seg.style?.code ? [190, 24, 93] : defaultColor))
    // baseline ≈ 78% w dół od górnej krawędzi linii, dobre przybliżenie dla Noto Sans/JetBrains Mono
    pdf.text(seg.text, x, y + size * 0.78)
    if (seg.style?.strike) {
      const midY = y + size * 0.45
      pdf.setDrawColor(...(seg.style?.code ? [190, 24, 93] : defaultColor))
      pdf.setLineWidth(0.6)
      pdf.line(x, midY, x + seg.width, midY)
    }
    x += seg.width
  }
}

// ── Konwersja węzłów ProseMirror -> "runs" tekstowe dla wrapRuns() ─────────────────────────

function styleFromMarks(marks) {
  const style = {}
  for (const m of marks ?? []) {
    if (m.type === 'bold') style.bold = true
    if (m.type === 'italic') style.italic = true
    if (m.type === 'strike') style.strike = true
    if (m.type === 'code') style.code = true
  }
  return style
}

/** Spłaszcza inline content (text/hardBreak) węzła blokowego do runs[] dla wrapRuns(). */
function inlineToRuns(content) {
  const runs = []
  for (const node of content ?? []) {
    if (node.type === 'text') runs.push({ text: node.text ?? '', style: styleFromMarks(node.marks) })
    else if (node.type === 'hardBreak') runs.push({ break: true })
  }
  return runs
}

/** Tekst "surowy" węzła (do TOC / meta) — bez formatowania, tylko konkatenacja text node'ów, rekurencyjnie. */
function plainText(node) {
  if (!node) return ''
  if (node.type === 'text') return node.text ?? ''
  return (node.content ?? []).map(plainText).join('')
}

// ═════════════════════════════════════════════════════════════════════════════════════════
// Preładowanie obrazów — jeden mały canvas PER obrazek (nie jeden na cały dokument)
// ═════════════════════════════════════════════════════════════════════════════════════════

function collectImageSrcs(node, out) {
  if (!node) return
  if (node.type === 'image' && node.attrs?.src) out.add(node.attrs.src)
  for (const child of node.content ?? []) collectImageSrcs(child, out)
}

/** Pobiera + dekoduje obraz do data-URL przez jednorazowy, mały <canvas> — zwalniany od razu po użyciu. */
async function loadImageAsDataUrl(src, signal) {
  const resp = await fetch(src, { signal })
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
  const blob = await resp.blob()
  const objectUrl = URL.createObjectURL(blob)
  try {
    const img = new Image()
    const loaded = new Promise((resolve, reject) => {
      img.onload = resolve
      img.onerror = () => reject(new Error('Nie udało się zdekodować obrazu'))
    })
    img.src = objectUrl
    await loaded
    const canvas = document.createElement('canvas')
    canvas.width = img.naturalWidth
    canvas.height = img.naturalHeight
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0)
    const dataUrl = canvas.toDataURL('image/png')
    // sprzątanie natychmiast po użyciu — nie trzymamy canvasu/Image ani chwili dłużej niż trzeba
    canvas.width = 0
    canvas.height = 0
    return { dataUrl, width: img.naturalWidth, height: img.naturalHeight }
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

/**
 * Preładowuje WSZYSTKIE obrazy jednej notatki (ograniczona współbieżność), zanim zacznie się
 * rysowanie — rysowanie w jsPDF jest synchroniczne, więc dane obrazów muszą być gotowe
 * wcześniej. Błędy pojedynczych obrazów NIE przerywają całości — trafiają do `issues`
 * i w miejscu obrazu w PDF pojawia się czytelny placeholder błędu (nigdy cicha pustka).
 */
async function preloadImages(contentJson, noteTitle, issues, signal) {
  const srcs = new Set()
  collectImageSrcs(contentJson, srcs)
  const map = new Map()
  await mapWithConcurrency(Array.from(srcs), MAX_CONCURRENT_REQUESTS, async (src) => {
    try {
      const data = await loadImageAsDataUrl(src, signal)
      map.set(src, { ok: true, ...data })
    } catch (err) {
      map.set(src, { ok: false, error: err })
      issues.push({ noteTitle, kind: 'image', detail: src, message: err.message ?? 'Błąd pobierania obrazu' })
    }
  })
  return map
}

// ═════════════════════════════════════════════════════════════════════════════════════════
// Renderowanie bloków
// ═════════════════════════════════════════════════════════════════════════════════════════

/** Przybliżona wysokość "pierwszego kawałka" następnego bloku — do heurystyki "nie osieracaj nagłówka". */
function estimateLookahead(node, imageMap) {
  if (!node) return 0
  switch (node.type) {
    case 'heading': return STYLE[`h${Math.min(node.attrs?.level ?? 1, 3)}`].lineH
    case 'paragraph': return STYLE.body.lineH
    case 'bulletList':
    case 'orderedList': return STYLE.body.lineH + GAP.listItem
    case 'blockquote': return STYLE.quote.lineH
    case 'codeBlock': return STYLE.code.lineH + GAP.code
    case 'image': {
      const entry = imageMap?.get(node.attrs?.src)
      if (entry?.ok) return Math.min(entry.height, 120) // proxy — pełne skalowanie policzone dopiero przy rysowaniu
      return 40
    }
    default: return STYLE.body.lineH
  }
}

function renderParagraphLike(builder, node, size, lineHeight, color, indent, lookahead) {
  const runs = inlineToRuns(node.content)
  const lines = wrapRuns(builder.pdf, runs, size, builder.contentWidth - indent)
  drawLinesBlock(builder, lines, lineHeight, (line, y) => {
    drawTextLine(builder.pdf, line, builder.margin.left + indent, y, size, color)
  }, lookahead)
}

function renderHeading(builder, node, nextNode, imageMap) {
  const level = Math.min(Math.max(node.attrs?.level ?? 1, 1), 3)
  const style = STYLE[`h${level}`]
  const runs = inlineToRuns(node.content).map(r => r.break ? r : { ...r, style: { ...r.style, bold: true } })
  const lines = wrapRuns(builder.pdf, runs, style.size, builder.contentWidth)
  builder.y += GAP.headingTop
  const lookahead = estimateLookahead(nextNode, imageMap)
  drawLinesBlock(builder, lines, style.lineH, (line, y) => {
    drawTextLine(builder.pdf, line, builder.margin.left, y, style.size, style.color)
  }, lookahead)
  builder.y += GAP.heading
}

function renderParagraph(builder, node, nextNode, imageMap) {
  renderParagraphLike(builder, node, STYLE.body.size, STYLE.body.lineH, STYLE.body.color, 0,
    estimateLookahead(nextNode, imageMap))
  builder.y += GAP.paragraph
}

function renderBlockquote(builder, node, nextNode, imageMap) {
  const indent = 16
  const runs = []
  for (const child of node.content ?? []) {
    if (runs.length) runs.push({ break: true }, { break: true })
    runs.push(...inlineToRuns(child.content).map(r => r.break ? r : { ...r, style: { ...r.style, italic: true } }))
  }
  const lines = wrapRuns(builder.pdf, runs, STYLE.quote.size, builder.contentWidth - indent)
  builder.y += 2
  drawLinesBlock(builder, lines, STYLE.quote.lineH, (line, y) => {
    builder.pdf.setFillColor(...QUOTE_BAR)
    builder.pdf.rect(builder.margin.left, y - 1, 2.5, STYLE.quote.lineH - 2, 'F')
    drawTextLine(builder.pdf, line, builder.margin.left + indent, y, STYLE.quote.size, STYLE.quote.color)
  }, estimateLookahead(nextNode, imageMap))
  builder.y += GAP.quote
}

function renderList(builder, node, depth, imageMap, ctx) {
  const ordered = node.type === 'orderedList'
  const items = node.content ?? []
  items.forEach((item, idx) => {
    const marker = ordered ? `${idx + 1}.` : '•'
    const indent = 14 + depth * 16
    const markerWidth = 16
    // Pierwszy blok akapitowy w listItem trzyma się markera; ewentualne dalsze bloki (np. zagnieżdżona lista)
    // renderowane są osobno pod nim.
    const [firstBlock, ...restBlocks] = item.content ?? []
    if (firstBlock?.type === 'paragraph') {
      const runs = inlineToRuns(firstBlock.content)
      const lines = wrapRuns(builder.pdf, runs, STYLE.body.size, builder.contentWidth - indent - markerWidth)
      const nextForLookahead = restBlocks[0] ?? (idx < items.length - 1 ? items[idx + 1]?.content?.[0] : null)
      drawLinesBlock(builder, lines, STYLE.body.lineH, (line, y) => {
        builder.pdf.setFont(FONT_SANS, 'normal')
        builder.pdf.setFontSize(STYLE.body.size)
        builder.pdf.setTextColor(...STYLE.body.color)
        drawTextLine(builder.pdf, line, builder.margin.left + indent + markerWidth, y, STYLE.body.size, STYLE.body.color)
      }, estimateLookahead(nextForLookahead, imageMap))
      // Marker rysowany osobno na wysokości PIERWSZEJ linii — dorysowujemy po fakcie, więc
      // musimy znać y pierwszej linii: przeliczamy je z powrotem z aktualnego builder.y.
      const firstLineY = builder.y - lines.length * STYLE.body.lineH
      builder.pdf.setFont(FONT_SANS, 'normal')
      builder.pdf.setFontSize(STYLE.body.size)
      builder.pdf.setTextColor(...STYLE.body.color)
      builder.pdf.text(marker, builder.margin.left + indent, firstLineY + STYLE.body.size * 0.78)
    }
    for (const block of restBlocks) {
      renderBlock(builder, block, null, imageMap, depth + 1, ctx)
    }
    builder.y += GAP.listItem
  })
  builder.y += GAP.list - GAP.listItem
}

function renderCodeBlock(builder, node, nextNode, imageMap) {
  const text = plainText(node)
  const padding = 8
  const lines = wrapCodeLines(builder.pdf, text, STYLE.code.size, builder.contentWidth - padding * 2)
  const totalTextHeight = lines.length * STYLE.code.lineH
  const blockHeight = totalTextHeight + padding * 2
  builder.y += 4
  const decision = builder.placeBlock(blockHeight, estimateLookahead(nextNode, imageMap))
  const drawBg = (topY, h) => {
    builder.pdf.setFillColor(...CODE_BG)
    builder.pdf.rect(builder.margin.left, topY, builder.contentWidth, h, 'F')
  }
  builder.pdf.setFont(FONT_MONO, 'normal')
  builder.pdf.setFontSize(STYLE.code.size)
  builder.pdf.setTextColor(...STYLE.code.color)

  if (decision !== 'split') {
    drawBg(builder.y, blockHeight)
    let ty = builder.y + padding
    for (const line of lines) {
      builder.pdf.text(line, builder.margin.left + padding, ty + STYLE.code.size * 0.78)
      ty += STYLE.code.lineH
    }
    builder.y += blockHeight
  } else {
    // Blok kodu wyższy niż cała strona — świadomie dozwolony podział linia-po-linii (spec pkt 7),
    // z tłem rysowanym per-strona-fragment, żeby nie zostawić białych "dziur" w tle kodu.
    let i = 0
    while (i < lines.length) {
      builder.ensureSpace(STYLE.code.lineH + padding)
      const startY = builder.y
      let h = padding
      let j = i
      while (j < lines.length && startY + h + STYLE.code.lineH <= builder.contentBottom) {
        h += STYLE.code.lineH
        j++
      }
      h += padding
      drawBg(startY, h)
      let ty = startY + padding
      for (let k = i; k < j; k++) {
        builder.pdf.setFont(FONT_MONO, 'normal')
        builder.pdf.setFontSize(STYLE.code.size)
        builder.pdf.setTextColor(...STYLE.code.color)
        builder.pdf.text(lines[k], builder.margin.left + padding, ty + STYLE.code.size * 0.78)
        ty += STYLE.code.lineH
      }
      builder.y = startY + h
      i = j
      if (i < lines.length) builder.freshPage()
    }
  }
  builder.y += GAP.code - 4
}

function renderImage(builder, node, issues, noteTitle, imageMap) {
  const src = node.attrs?.src
  const entry = imageMap?.get(src)
  builder.y += 3

  if (!entry || !entry.ok) {
    // Placeholder błędu — WIDOCZNY i opisany, nigdy cicha pusta przestrzeń udająca obraz.
    // Zawsze mieści się na jednej stronie (34pt), więc placeBlock nigdy nie zwróci tu 'split'.
    const height = 34
    builder.placeBlock(height)
    builder.pdf.setDrawColor(...ERROR_COLOR)
    builder.pdf.setFillColor(...ERROR_BG)
    builder.pdf.roundedRect(builder.margin.left, builder.y, builder.contentWidth, height, 3, 3, 'FD')
    builder.pdf.setFont(FONT_SANS, 'bold')
    builder.pdf.setFontSize(9)
    builder.pdf.setTextColor(...ERROR_COLOR)
    builder.pdf.text('Nie udało się załadować obrazu', builder.margin.left + 10, builder.y + 15)
    builder.pdf.setFont(FONT_SANS, 'normal')
    builder.pdf.setFontSize(8)
    const label = (src ?? 'nieznany plik').split('/').pop()
    builder.pdf.text(label, builder.margin.left + 10, builder.y + 27)
    builder.y += height + GAP.image
    return
  }

  const { width, height } = computeImageDrawSize(entry.width, entry.height, builder.contentWidth, builder.fullPageHeight)
  // Skalowany zawsze do <= fullPageHeight (zob. computeImageDrawSize), więc zawsze mieści się
  // na świeżej stronie — placeBlock tu nigdy nie zwróci 'split'.
  builder.placeBlock(height)
  const x = builder.margin.left + (builder.contentWidth - width) / 2 // wyśrodkowany w kolumnie treści
  builder.pdf.addImage(entry.dataUrl, 'PNG', x, builder.y, width, height, undefined, 'FAST')
  builder.y += height + GAP.image
}

function renderHr(builder) {
  builder.y += GAP.hr / 2
  builder.placeBlock(2) // linia (2pt) zawsze mieści się na jednej stronie
  builder.pdf.setDrawColor(...HR_COLOR)
  builder.pdf.setLineWidth(0.75)
  builder.pdf.line(builder.margin.left, builder.y, builder.margin.left + builder.contentWidth, builder.y)
  builder.y += GAP.hr / 2
}

function renderBlock(builder, node, nextNode, imageMap, depth = 0, ctx) {
  switch (node.type) {
    case 'heading': return renderHeading(builder, node, nextNode, imageMap)
    case 'paragraph': return renderParagraph(builder, node, nextNode, imageMap)
    case 'blockquote': return renderBlockquote(builder, node, nextNode, imageMap)
    case 'bulletList':
    case 'orderedList': return renderList(builder, node, depth, imageMap, ctx)
    case 'codeBlock': return renderCodeBlock(builder, node, nextNode, imageMap)
    case 'image': return renderImage(builder, node, ctx?.issues ?? [], ctx?.noteTitle, imageMap)
    case 'horizontalRule': return renderHr(builder)
    default: {
      // Nieznany/nieobsługiwany typ węzła (np. przyszły blok, którego edytor jeszcze nie ma) —
      // lepiej pokazać jego tekst jako zwykły akapit niż po cichu zgubić treść notatki.
      const text = plainText(node)
      if (text.trim()) renderParagraph(builder, { content: [{ type: 'text', text }] }, nextNode, imageMap)
    }
  }
}

// ── Nagłówek sekcji (tytuł notatki + metadane) i stopka całego dokumentu ────────────────────

function renderNoteHeader(builder, meta) {
  const lines = wrapRuns(builder.pdf, [{ text: meta.title || 'Bez tytułu', style: { bold: true } }],
    STYLE.noteTitle.size, builder.contentWidth)
  for (const line of lines) {
    drawTextLine(builder.pdf, line, builder.margin.left, builder.y, STYLE.noteTitle.size, STYLE.noteTitle.color)
    builder.y += STYLE.noteTitle.lineH
  }
  builder.y += 4

  const metaLines = []
  if (meta.categoryPath?.length) metaLines.push(meta.categoryPath.join(' › '))
  if (meta.author) metaLines.push(`Autor: ${meta.author}`)
  const created = formatMetaDate(meta.createdAt)
  const updated = formatMetaDate(meta.updatedAt)
  if (created) metaLines.push(`Utworzono: ${created}`)
  if (updated && updated !== created) metaLines.push(`Zaktualizowano: ${updated}`)
  // Trzecia data obok "Utworzono"/"Zaktualizowano" — ostatnie ŚWIADOME potwierdzenie aktualności
  // (ręczny przycisk albo zapis treści notatki, zob. dokumentacja/ACTUALITY_VERIFICATION.txt). Zawsze pokazana
  // (nie tylko gdy jest ustawiona), z jawnym "Nigdy", żeby brak weryfikacji był tak samo widoczny
  // w wydrukowanym dokumencie, jak na ekranie.
  const verified = formatMetaDate(meta.lastVerificationOfActualityDate)
  metaLines.push(`Ostatnio zweryfikowano: ${verified ?? 'Nigdy — termin liczony od utworzenia'}`)

  builder.pdf.setFont(FONT_SANS, 'normal')
  builder.pdf.setFontSize(STYLE.meta.size)
  builder.pdf.setTextColor(...STYLE.meta.color)
  for (const line of metaLines) {
    builder.pdf.text(line, builder.margin.left, builder.y + STYLE.meta.size * 0.78)
    builder.y += STYLE.meta.lineH
  }
  builder.y += 6
  builder.pdf.setDrawColor(...ACCENT)
  builder.pdf.setLineWidth(1.4)
  builder.pdf.line(builder.margin.left, builder.y, builder.margin.left + 34, builder.y)
  builder.y += 14
}

function formatMetaDate(iso) {
  if (!iso) return null
  try {
    return new Date(iso).toLocaleString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return null
  }
}

/** Numeruje i podpisuje KAŻDĄ stronę dokumentu na końcu (numeracja obejmuje cały dokument, nie per-notatkę). */
function stampFooters(builder, generatedAt) {
  const pdf = builder.pdf
  const total = pdf.internal.getNumberOfPages()
  const dateStr = generatedAt.toLocaleDateString('pl-PL')
  for (let p = 1; p <= total; p++) {
    pdf.setPage(p)
    pdf.setFont(FONT_SANS, 'normal')
    pdf.setFontSize(STYLE.footer.size)
    pdf.setTextColor(...STYLE.footer.color)
    const footerY = builder.pageHeight - 30
    pdf.text(FOOTER_TEXT, builder.margin.left, footerY)
    pdf.text(`Wygenerowano: ${dateStr}`, builder.pageWidth / 2, footerY, { align: 'center' })
    pdf.text(`${p} / ${total}`, builder.pageWidth - builder.margin.right, footerY, { align: 'right' })
  }
}

function renderToc(builder, sections) {
  builder.freshPage()
  builder.pdf.setFont(FONT_SANS, 'bold')
  builder.pdf.setFontSize(STYLE.tocTitle.size)
  builder.pdf.setTextColor(...STYLE.tocTitle.color)
  builder.pdf.text('Spis treści', builder.margin.left, builder.y + STYLE.tocTitle.size * 0.78)
  builder.y += STYLE.tocTitle.lineH + 10

  sections.forEach((s, i) => {
    const titleLines = wrapRuns(builder.pdf, [{ text: `${i + 1}. ${s.title || 'Bez tytułu'}`, style: {} }],
      STYLE.tocItem.size, builder.contentWidth)
    const pathText = s.categoryPath?.length ? s.categoryPath.join(' › ') : null
    const height = titleLines.length * STYLE.tocItem.lineH + (pathText ? STYLE.tocMeta.lineH : 0) + 4
    builder.ensureSpace(height)
    for (const line of titleLines) {
      drawTextLine(builder.pdf, line, builder.margin.left, builder.y, STYLE.tocItem.size, STYLE.tocItem.color)
      builder.y += STYLE.tocItem.lineH
    }
    if (pathText) {
      builder.pdf.setFont(FONT_SANS, 'italic')
      builder.pdf.setFontSize(STYLE.tocMeta.size)
      builder.pdf.setTextColor(...STYLE.tocMeta.color)
      builder.pdf.text(pathText, builder.margin.left + 12, builder.y + STYLE.tocMeta.size * 0.78)
      builder.y += STYLE.tocMeta.lineH
    }
    builder.y += 4
  })
}

// ═════════════════════════════════════════════════════════════════════════════════════════
// Renderowanie jednej sekcji (notatki) — dzielone przez single i bulk
// ═════════════════════════════════════════════════════════════════════════════════════════

async function renderSection(builder, note, issues, signal) {
  const imageMap = await preloadImages(note.contentJson, note.title, issues, signal)
  builder.freshPage()
  renderNoteHeader(builder, note)

  const blocks = note.contentJson?.content ?? []
  if (blocks.length === 0) {
    builder.pdf.setFont(FONT_SANS, 'italic')
    builder.pdf.setFontSize(STYLE.body.size)
    builder.pdf.setTextColor(...STYLE.meta.color)
    builder.pdf.text('(Notatka nie ma jeszcze treści)', builder.margin.left, builder.y + STYLE.body.size * 0.78)
    builder.y += STYLE.body.lineH
    return
  }
  blocks.forEach((node, i) => {
    renderBlock(builder, node, blocks[i + 1], imageMap, 0, { issues, noteTitle: note.title })
  })
}

// ═════════════════════════════════════════════════════════════════════════════════════════
// API publiczne
// ═════════════════════════════════════════════════════════════════════════════════════════

/**
 * noteData: { title, categoryPath: string[], author, createdAt, updatedAt, lastVerificationOfActualityDate, contentJson }
 * opts: { filename, signal, output: 'save' | 'blob' }
 * Domyślnie (output pominięty albo 'save') zapisuje plik na dysk (jak dotychczas) i zwraca
 * { issues }. Z output:'blob' NIE zapisuje niczego automatycznie — zwraca { issues, blob }, żeby
 * wywołujący sam zdecydował co zrobić z wynikiem (np. panel ratunkowy otwiera go w nowej karcie
 * zamiast wymuszać pobranie, zob. emergencyRecovery.js).
 * Rzuca, jeśli sama biblioteka jsPDF nie może się załadować albo zapis się nie powiedzie —
 * błędy POJEDYNCZYCH obrazów są zbierane do `issues`, nie przerywają generowania.
 */
export async function generateSingleNotePdf(noteData, opts = {}) {
  const { jsPDF } = await import('jspdf')
  const builder = await new PdfBuilder(jsPDF).ready()
  const issues = []
  await renderSection(builder, noteData, issues, opts.signal)
  stampFooters(builder, opts.now ?? new Date())
  if (opts.output === 'blob') return { issues, blob: builder.blob() }
  builder.save(opts.filename || singleNoteFilename(noteData.title, opts.now))
  return { issues }
}

/**
 * notesData: [{ topicId, title, categoryPath, author, createdAt, updatedAt, lastVerificationOfActualityDate, contentJson }]
 *   — kolejność w tablicy JEST kolejnością w dokumencie (wywołujący odpowiada za sortowanie
 *   wg drzewa kategorii, zob. `flattenSelectedTopicsInTreeOrder` w dashboardTreeUtils.js).
 * opts: { filename, signal, onProgress(info), includeToc = true, output: 'save' | 'blob' }
 */
export async function generateBulkNotesPdf(notesData, opts = {}) {
  if (notesData.length > MAX_BULK_NOTES) {
    throw new Error(`Zbyt wiele notatek naraz (${notesData.length}) — limit to ${MAX_BULK_NOTES}.`)
  }
  const { jsPDF } = await import('jspdf')
  const builder = await new PdfBuilder(jsPDF).ready()
  const issues = []

  if (opts.includeToc !== false && notesData.length > 1) {
    opts.onProgress?.({ stage: 'toc' })
    renderToc(builder, notesData)
  }

  for (let i = 0; i < notesData.length; i++) {
    opts.onProgress?.({ stage: 'rendering', index: i + 1, total: notesData.length, title: notesData[i].title })
    await renderSection(builder, notesData[i], issues, opts.signal)
  }

  opts.onProgress?.({ stage: 'finalizing' })
  stampFooters(builder, opts.now ?? new Date())
  if (opts.output === 'blob') return { issues, blob: builder.blob() }
  builder.save(opts.filename || bulkNotesFilename(notesData.length, opts.now))
  return { issues }
}

export const PDF_LIMITS = { maxBulkNotes: MAX_BULK_NOTES, maxConcurrentRequests: MAX_CONCURRENT_REQUESTS }
