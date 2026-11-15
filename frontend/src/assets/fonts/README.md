# Czcionki osadzane w PDF

jsPDF-owe wbudowane "standard 14" fonty (`helvetica`/`courier`/`times`) używają WinAnsiEncoding,
które **nie obejmuje polskich znaków diakrytycznych** (ł, ń, ś, ż, ź, ć, ą, ę) — tekst z tymi
znakami renderował się uszkodzony (np. "Nagłówek" -> "NagBówek"). Dlatego generator PDF
(`src/utils/pdfGenerator.js`) osadza własne czcionki TrueType z pełnym pokryciem Unicode/Latin
Extended-A zamiast fontów wbudowanych w jsPDF.

- **Noto Sans** (Regular/Bold/Italic/BoldItalic) — tekst główny, nagłówki, cytaty, metadane.
- **JetBrains Mono** (Regular/Bold) — bloki kodu i kod inline.

Obie rodziny to **Google Fonts, licencja SIL Open Font License 1.1** (OFL) — wolno osadzać,
dystrybuować i pakietować bez opłat i bez dodatkowych ograniczeń, pod warunkiem zachowania noty
licencyjnej w samym pliku fontu (nienaruszona — pliki pobrane bez modyfikacji z oficjalnego CDN
Google Fontów, wariant `latin-ext`, obejmujący polskie znaki).

Pliki pobrane z `fonts.gstatic.com` (statyczne instancje wagi z font variable, przez
`gwfh.mranftl.com` — publiczne API zwracające bezpośrednie URL-e do plików Google Fonts).

Ładowane w PDF-ie WYŁĄCZNIE dynamicznie, w tym samym leniwym module co jsPDF (dopiero po
kliknięciu "Pobierz PDF" / "Druk masowy") — nie powiększają głównego bundla aplikacji.
