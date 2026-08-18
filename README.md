# Knowledge-Forge

Baza wiedzy: kategorie → wpisy (instrukcje albo notatki), pisane w edytorze WYSIWYG z wklejaniem obrazków ze schowka. Każdy wpis trzyma treść do dalszej edycji oraz wygenerowany PDF. Działa bez logowania — wszystko trafia do jednego, lokalnego użytkownika.

---

## Stos technologiczny

| Warstwa | Technologie |
|---------|------------|
| Backend | Java 17, czyste serwlety (`jakarta.servlet`), embedded Jetty — **bez Springa, bez Hibernate** |
| Dostęp do danych | JDBC + ręczne DAO, HikariCP (pula połączeń) |
| Baza danych | PostgreSQL |
| JSON | Jackson (biblioteka, nie framework) |
| Frontend | React 19, Vite 7, React Router 7, Zustand, Axios |
| Edytor treści | TipTap (MIT, rdzeń open-source — bez płatnych rozszerzeń Pro) |
| Eksport PDF | html2pdf.js (MIT), generowany po stronie klienta, ładowany leniwie dopiero przy zapisie |
| Budowanie | Maven (`maven-shade-plugin` → jeden wykonywalny JAR) |

Backend nie używa żadnego frameworka DI/IoC — całe drzewo obiektów (DAO → serwisy → serwlety) jest ręcznie spięte w `Main.java`. Persystencja to zwykłe `PreparedStatement`, bez ORM.

---

## Model danych

```
Kategoria (drzewo, dowolna głębokość)
  └── Wpis (Topic): tytuł, typ [Instrukcja | Notatka], poziom szczegółowości [Niski | Średni | Wysoki]
        └── Notatka (Note): treść edytora + wklejone obrazki + PDF
```

**Poziom szczegółowości** to nie trudność treści, tylko to, jak dopracowany jest wpis:
- **Niski** — szkic, zanotowany i zaplanowany do uzupełnienia później
- **Średni** — notatka robocza, spisana szybko, czytelna głównie dla autora
- **Wysoki** — notatka dopracowana, napisana tak, by zrozumiał ją każdy

## Format treści — `.kfdoc`

Treść notatki (tekst sformatowany przez edytor + wklejone obrazki) jest pakowana do własnego,
prostego formatu binarnego — ten sam pomysł co `.docx`/`.odt`: zwykły ZIP (`java.util.zip`,
biblioteka standardowa, zero zależności) zawierający:

```
manifest.json     — {"format":"kfdoc","version":1}
content.json      — drzewo dokumentu edytora (TipTap/ProseMirror JSON)
assets/<id>.png   — wklejone obrazki, referencjonowane z content.json jako "asset:<id>.png"
```

Całość trzymana jest jako `BYTEA` w kolumnie `note.content_blob`; wygenerowany na kliencie PDF
leży obok w `note.pdf_blob`. Zob. `document.DocumentContainer`.

---

## Architektura

```
HTTP request → Servlet (routing po ścieżce) → Service (logika biznesowa) → DAO (JDBC) → PostgreSQL
```

```
src/main/java/com/example/knowledgeforge/
├── Main.java              # composition root — tu są spięte wszystkie obiekty, tu startuje Jetty
├── config/                 # AppConfig (properties + env), Database (HikariCP), Schema (DDL/migracje), DefaultUserSeeder
├── document/                # DocumentContainer — pakowanie/odczyt formatu .kfdoc
├── domain/                 # POJO — encje, DTO, wyjątki (bez adnotacji JPA/validation)
│   ├── category/ note/ topic/ user/
│   └── exception/
├── dao/                    # JDBC, jeden DAO na tabelę
├── service/                # logika biznesowa, ręczna walidacja (bez Spring Data / Bean Validation)
├── web/                    # ApiServlet (bazowa klasa: routing, JSON, mapowanie wyjątków na HTTP),
│                            # konkretne serwlety, CorsFilter, SpaServlet (serwuje frontend)
└── json/                   # współdzielony ObjectMapper

frontend/src/
├── api/                     # klient axios + wywołania REST
├── components/knowledge/    # drzewo kategorii, edytor (TipTap), modale
├── pages/                   # DashboardPage, AdminPage
├── router/ store/           # React Router, Zustand
```

Zasada routingu w serwletach: każdy zasób REST ma swój serwlet zamontowany na prefiksie (`/api/topics/*`, `/api/categories/*`, ...), a `ApiServlet` parsuje resztę ścieżki (`pathSegments`) ręcznie — tak jak wcześniej robił to `@PathVariable` w Springu, tylko bez adnotacji.

---

## Endpointy REST

| Metoda | Ścieżka | Opis |
|--------|---------|------|
| `GET` | `/api/categories/tree` | Drzewo kategorii |
| `POST` | `/api/categories` | Nowa kategoria |
| `PATCH` | `/api/categories/{id}` | Edytuj kategorię |
| `DELETE` | `/api/categories/{id}` | Usuń kategorię |
| `GET` | `/api/categories/{id}/topics` | Wpisy w kategorii |
| `POST` | `/api/topics` | Nowy wpis (tytuł, typ, poziom szczegółowości) |
| `GET` | `/api/topics/{id}` | Szczegóły wpisu |
| `PATCH` | `/api/topics/{id}` | Edytuj wpis |
| `DELETE` | `/api/topics/{id}` | Usuń wpis |
| `GET` | `/api/topics/{id}/note` | Treść notatki (JSON edytora, obrazki jako URL-e) |
| `PUT` | `/api/topics/{id}/note` | Zapisz notatkę (upsert — treść + obrazki + PDF) |
| `GET` | `/api/topics/{id}/note/assets/{plik}` | Pojedynczy wklejony obrazek |
| `GET` | `/api/topics/{id}/note/pdf` | Wygenerowany PDF |

---

## Uruchomienie

### Backend

```bash
# baza danych (Postgres) musi działać pod adresem z config.properties / zmiennych środowiskowych
mvn clean package
java -jar target/knowledge-forge.jar
```

Konfiguracja: `src/main/resources/config.properties`, nadpisywana przez (w tej kolejności rosnącego priorytetu):
1. `src/main/resources/config-local.properties` (gitignorowany, na sekrety lokalne)
2. zmienne środowiskowe — konwencja `db.url` → `DB_URL` (patrz `.env.example`)

Tabele i migracje zakłada aplikacja sama przy starcie (`config.Schema` — `CREATE TABLE IF NOT EXISTS` + idempotentne `ALTER TABLE` dla zmian schematu), nie ma osobnego narzędzia migracyjnego.

### Frontend (dev)

```bash
cd frontend
npm install
npm run dev
```

Build produkcyjny frontendu trafia bezpośrednio do `src/main/resources/static/` i jest serwowany przez `SpaServlet` w tym samym JAR-ze co API — jeden proces, jeden port.

```bash
cd frontend
npm run build
```

---

## Dostęp bez logowania

Wszystkie dane należą do jednego, domyślnego użytkownika zakładanego przy pierwszym starcie (`config.DefaultUserSeeder`, nazwa z `default-user.username`). Nie ma rejestracji ani logowania.
