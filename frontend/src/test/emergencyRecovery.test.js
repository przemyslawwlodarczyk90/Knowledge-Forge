import { describe, it, expect, vi } from 'vitest'
import { zipSync, strToU8 } from 'fflate'
import {
  readEmergencyFile, RecoveryError, MAX_KFBUNDLE_INPUT_BYTES,
} from '../utils/emergencyRecovery'

// ── Budowanie syntetycznych plików .kfdoc/.kfbundle — struktura BYTE-DO-BYTE identyczna z tą,
// którą produkuje backend (com.example.knowledgeforge.document.DocumentContainer /
// NoteBundleBackupService), żeby zweryfikować kompatybilność formatu bez uruchamiania JVM.

function buildKfdocBytes({ metadata = {}, contentJson = { type: 'doc', content: [] }, assets = {}, format = 'kfdoc', version = 1 } = {}) {
  const manifest = { format, version, metadata }
  const files = {
    'manifest.json': strToU8(JSON.stringify(manifest)),
    'content.json': strToU8(JSON.stringify(contentJson)),
  }
  for (const [name, bytes] of Object.entries(assets)) {
    files[`assets/${name}`] = bytes
  }
  return zipSync(files)
}

function toFile(bytes, name) {
  return new File([bytes], name, { type: 'application/octet-stream' })
}

async function sha256Hex(bytes) {
  const digest = await crypto.subtle.digest('SHA-256', bytes)
  return [...new Uint8Array(digest)].map(b => b.toString(16).padStart(2, '0')).join('')
}

describe('emergencyRecovery — odczyt .kfdoc', () => {
  it('poprawny .kfdoc jest odczytywany do kształtu oczekiwanego przez generator PDF', async () => {
    const bytes = buildKfdocBytes({
      metadata: {
        title: 'Moja notatka', author: 'Ala', type: 'NOTE',
        categoryPath: ['IT', 'Java'], categoryName: 'Java',
        topicCreatedAt: '2026-01-01T10:00:00Z', noteCreatedAt: '2026-01-02T10:00:00Z',
        lastVerificationOfActualityDate: '2026-01-03T10:00:00Z',
      },
      contentJson: { type: 'doc', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Treść' }] }] },
    })

    const result = await readEmergencyFile(toFile(bytes, 'note.kfdoc'))

    expect(result.kind).toBe('kfdoc')
    expect(result.noteData.title).toBe('Moja notatka')
    expect(result.noteData.author).toBe('Ala')
    expect(result.noteData.categoryPath).toEqual(['IT', 'Java'])
    expect(result.noteData.contentJson.content[0].content[0].text).toBe('Treść')
  })

  it('obraz "asset:xyz" w treści jest podmieniany na lokalny Blob URL, nie na URL sieciowy', async () => {
    const png = new Uint8Array([137, 80, 78, 71]) // nagłówek PNG, wystarczy do testu
    const bytes = buildKfdocBytes({
      metadata: { title: 'Z obrazkiem' },
      contentJson: { type: 'doc', content: [{ type: 'image', attrs: { src: 'asset:pic.png' } }] },
      assets: { 'pic.png': png },
    })

    const result = await readEmergencyFile(toFile(bytes, 'note.kfdoc'))

    const src = result.noteData.contentJson.content[0].attrs.src
    expect(src.startsWith('blob:')).toBe(true)
    expect(result.blobUrls).toContain(src)
  })

  it('starszy .kfdoc bez categoryPath spada z powrotem na categoryName (kompatybilność wsteczna)', async () => {
    const bytes = buildKfdocBytes({
      metadata: { title: 'Stara notatka', categoryName: 'Stara kategoria' }, // brak categoryPath
    })

    const result = await readEmergencyFile(toFile(bytes, 'old.kfdoc'))

    expect(result.noteData.categoryPath).toEqual(['Stara kategoria'])
  })

  it('brakujący manifest.json jest odrzucany z czytelnym błędem', async () => {
    const bytes = zipSync({ 'content.json': strToU8('{}') })
    await expect(readEmergencyFile(toFile(bytes, 'broken.kfdoc'))).rejects.toThrow(RecoveryError)
  })

  it('plik, który nie jest archiwum ZIP, jest odrzucany po sygnaturze', async () => {
    const bytes = strToU8('to nie jest ZIP')
    await expect(readEmergencyFile(toFile(bytes, 'fake.kfdoc'))).rejects.toThrow(/sygnatura/)
  })

  it('nieobsługiwana wersja formatu jest odrzucana', async () => {
    const bytes = buildKfdocBytes({ version: 99, metadata: { title: 'X' } })
    await expect(readEmergencyFile(toFile(bytes, 'x.kfdoc'))).rejects.toThrow(/[Ww]ersj/)
  })

  it('nieznany format w manifeście jest odrzucany', async () => {
    const bytes = zipSync({
      'manifest.json': strToU8(JSON.stringify({ format: 'something-else', version: 1 })),
      'content.json': strToU8('{}'),
    })
    await expect(readEmergencyFile(toFile(bytes, 'x.kfdoc'))).rejects.toThrow(/[Nn]ierozpoznany/)
  })

  it('próba path traversal w nazwie assetu jest blokowana', async () => {
    const bytes = zipSync({
      'manifest.json': strToU8(JSON.stringify({ format: 'kfdoc', version: 1, metadata: { title: 'X' } })),
      'content.json': strToU8(JSON.stringify({ type: 'doc', content: [{ type: 'image', attrs: { src: 'asset:../../../etc/passwd' } }] })),
    })
    // assertSafeEntryName rzuca dopiero przy próbie odczytania assetu — sam plik nadal się otwiera,
    // ale rewriteAssetRefs propaguje wyjątek w górę.
    await expect(readEmergencyFile(toFile(bytes, 'x.kfdoc'))).rejects.toThrow(RecoveryError)
  })

  it('plik przekraczający limit rozmiaru jest odrzucany przed próbą rozpakowania', async () => {
    const file = { size: MAX_KFBUNDLE_INPUT_BYTES + 1, name: 'huge.kfbundle', arrayBuffer: async () => new ArrayBuffer(0) }
    await expect(readEmergencyFile(file)).rejects.toThrow(/za?byt du/i)
  })
})

describe('emergencyRecovery — odczyt .kfbundle', () => {
  it('poprawny .kfbundle jest odczytywany jako lista notatek, w kolejności indeksu', async () => {
    const doc1 = buildKfdocBytes({ metadata: { title: 'Pierwsza' } })
    const doc2 = buildKfdocBytes({ metadata: { title: 'Druga' } })
    const bundle = zipSync({
      'manifest.json': strToU8(JSON.stringify({ format: 'kfbundle', version: 1, createdAt: '2026-01-01T00:00:00Z', documentCount: 2, generator: 'Knowledge Forge 2.0' })),
      'index.json': strToU8(JSON.stringify([
        { fileName: 'documents/a.kfdoc', title: 'Pierwsza', corrupted: false },
        { fileName: 'documents/b.kfdoc', title: 'Druga', corrupted: false },
      ])),
      'documents/a.kfdoc': doc1,
      'documents/b.kfdoc': doc2,
    })

    const result = await readEmergencyFile(toFile(bundle, 'backup.kfbundle'))

    expect(result.kind).toBe('kfbundle')
    expect(result.notesData.map(n => n.title)).toEqual(['Pierwsza', 'Druga'])
    expect(result.issues).toHaveLength(0)
  })

  it('dokument oznaczony jako uszkodzony w indeksie trafia do issues, reszta backupu działa dalej', async () => {
    const goodDoc = buildKfdocBytes({ metadata: { title: 'Dobra' } })
    const bundle = zipSync({
      'manifest.json': strToU8(JSON.stringify({ format: 'kfbundle', version: 1 })),
      'index.json': strToU8(JSON.stringify([
        { fileName: 'documents/good.kfdoc', title: 'Dobra', corrupted: false },
        { fileName: 'documents/bad.kfdoc', title: 'Zła', corrupted: true, error: 'Uszkodzony kontener ZIP' },
      ])),
      'documents/good.kfdoc': goodDoc,
      'documents/bad.kfdoc': strToU8('not a zip'),
    })

    const result = await readEmergencyFile(toFile(bundle, 'backup.kfbundle'))

    expect(result.notesData.map(n => n.title)).toEqual(['Dobra'])
    expect(result.issues).toHaveLength(1)
    expect(result.issues[0].title).toBe('Zła')
  })

  it('niezgodność SHA-256 jest wykrywana i dokument trafia do issues zamiast do wyniku', async () => {
    const doc = buildKfdocBytes({ metadata: { title: 'Podmieniona' } })
    const bundle = zipSync({
      'manifest.json': strToU8(JSON.stringify({ format: 'kfbundle', version: 1 })),
      'index.json': strToU8(JSON.stringify([
        { fileName: 'documents/a.kfdoc', title: 'Podmieniona', corrupted: false, sha256: 'deadbeef'.repeat(8) },
      ])),
      'documents/a.kfdoc': doc,
    })

    const result = await readEmergencyFile(toFile(bundle, 'backup.kfbundle'))

    expect(result.kind).toBe('kfbundle')
    expect(result.notesData).toHaveLength(0)
    expect(result.issues[0].reason).toMatch(/[Ss]uma kontrolna|SHA-256/)
  })

  it('poprawna suma SHA-256 w indeksie jest weryfikowana pozytywnie', async () => {
    const doc = buildKfdocBytes({ metadata: { title: 'Z checksumą' } })
    const hash = await sha256Hex(doc)
    const bundle = zipSync({
      'manifest.json': strToU8(JSON.stringify({ format: 'kfbundle', version: 1 })),
      'index.json': strToU8(JSON.stringify([
        { fileName: 'documents/a.kfdoc', title: 'Z checksumą', corrupted: false, sha256: hash },
      ])),
      'documents/a.kfdoc': doc,
    })

    const result = await readEmergencyFile(toFile(bundle, 'backup.kfbundle'))

    expect(result.notesData).toHaveLength(1)
    expect(result.issues).toHaveLength(0)
  })

  it('pusty .kfbundle (0 dokumentów) odczytuje się poprawnie, bez błędu', async () => {
    const bundle = zipSync({
      'manifest.json': strToU8(JSON.stringify({ format: 'kfbundle', version: 1, documentCount: 0 })),
      'index.json': strToU8('[]'),
    })
    const result = await readEmergencyFile(toFile(bundle, 'empty.kfbundle'))
    expect(result.notesData).toHaveLength(0)
    expect(result.issues).toHaveLength(0)
  })

  it('działa bez żadnego zapytania sieciowego — fetch nie jest w ogóle wołany', async () => {
    const fetchSpy = vi.fn()
    const originalFetch = globalThis.fetch
    globalThis.fetch = fetchSpy
    try {
      const bundle = zipSync({
        'manifest.json': strToU8(JSON.stringify({ format: 'kfbundle', version: 1 })),
        'index.json': strToU8(JSON.stringify([{ fileName: 'documents/a.kfdoc', title: 'A', corrupted: false }])),
        'documents/a.kfdoc': buildKfdocBytes({ metadata: { title: 'A' } }),
      })
      await readEmergencyFile(toFile(bundle, 'backup.kfbundle'))
      expect(fetchSpy).not.toHaveBeenCalled()
    } finally {
      globalThis.fetch = originalFetch
    }
  })
})
