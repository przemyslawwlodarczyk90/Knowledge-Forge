# Offer Browser Prototype

Fullstack aplikacja do zarządzania procesem rekrutacyjnym IT. Automatycznie importuje oferty pracy, śledzi status aplikacji, wysyła codzienne powiadomienia e-mail i analizuje wymagania rynkowe.

---

## Stos technologiczny

| Warstwa | Technologie |
|---------|------------|
| Backend | Java 17, Spring Boot 3.2.5, Spring Security, Spring Data JPA |
| Baza danych | PostgreSQL 15, Redis (cache) |
| Frontend | React 19, Vite 7, React Router 7, Recharts, Zustand, Axios |
| Email | Spring Mail + Thymeleaf |
| Scraper | Python + Selenium (NoFluff Jobs) |
| Dokumentacja API | SpringDoc OpenAPI 3 / Swagger UI |

---

## Architektura

Wzorzec **fasada + handler**: każda operacja biznesowa trafia do dedykowanego handlera, a fasada kompozytuje handlery i jest jedyną zależnością kontrolerów REST.

```
Controller → Facade → Handler(s) → Repository
```

Wymagania (skills) przechowywane są w osobnej tabeli faktów `requirements` — oddzielonej od `job_offers` — co umożliwia analytics per skill/level/source bez ładowania pełnych encji ofert.

---

## Funkcjonalności

### Import ofert
- **Scraper Python/Selenium** — uruchomienie przez API, działa asynchronicznie w tle, po zakończeniu automatycznie importuje wyniki
- **Import z JSON** — wklejenie tablicy obiektów ofert
- **Import z URL** — pobranie i zapis pojedynczej oferty po podaniu linku
- **Backfill** — przy ponownym imporcie uzupełnia brakujące `requirements`, `niceToHave`, `source` dla istniejących ofert

### Przeglądanie ofert
- Lista wszystkich ofert sortowana po dacie pobrania, serwowana z cache Redis
- Szczegóły oferty: firma, lokalizacja, wynagrodzenie, poziom, requirements, nice-to-have
- Oznaczanie oferty jako **duplikatu** — widoczne dla admina na karcie oferty

### Panel użytkownika ("Moje Oferty")
- **Watchlist** — oferty dodane do śledzenia (nie zaaplikowane)
- **Zaaplikowane** — oferty, na które wysłano CV
- **Nieprzydatne** — oferty ukryte z widoku (per-user, nie wpływa na innych użytkowników)

### Codzienne powiadomienia e-mail
- Automatyczny email o 17:00 (cron konfiguowalny) z listą niezaaplikowanych ofert
- Szablon HTML — ciemny motyw, lista ofert z przyciskami akcji
- **One-click akcje bez logowania** — bezpośrednio z treści maila:
  - `✓ ZAAPLIKUJ` — rejestruje aplikację
  - `✕ NIEPRZYDATNA` — ukrywa ofertę

### Analityka wymagań
- Top umiejętności ogółem lub per poziom seniorności (Junior, Mid, Senior, Trainee, Nieokreślony poziom)
- Filtrowanie po źródle danych (np. NoFluff)
- Wykresy słupkowe (Recharts)

### Notatki aplikacyjne
- Dodawanie notatek tekstowych do ofert
- Przeglądanie notatek per użytkownik, grupowanie według firmy z datami

### Statystyki
- Rozkład ofert według miast i poziomów doświadczenia
- Łączna liczba ofert z wyłączeniem duplikatów

### Panel admina
- Lista użytkowników ze statystykami (zaaplikowane / watchlist / nieprzydatne / notatki)
- Oferty oznaczone jako DUPLICATE lub USELESS z dominującą flagą per oferta
- Szczegóły flag: kto oznaczył i kiedy

### Konta użytkowników
- Rejestracja z potwierdzeniem e-mail (token jednorazowy)
- Logowanie JWT (token ważny 1h)
- Zmiana hasła, aktualizacja profilu

---

## Endpointy REST

| Metoda | Ścieżka | Opis |
|--------|---------|------|
| `POST` | `/api/auth/register` | Rejestracja |
| `POST` | `/api/auth/login` | Logowanie → JWT |
| `GET` | `/api/offers` | Lista ofert (z cache) |
| `GET` | `/api/offers/{id}` | Szczegóły oferty |
| `POST` | `/api/offers` | Dodaj ofertę |
| `PUT` | `/api/offers/{id}` | Edytuj ofertę |
| `DELETE` | `/api/offers/{id}` | Usuń ofertę |
| `POST` | `/api/user-offers/{id}/apply` | Zaaplikuj |
| `POST` | `/api/user-offers/{id}/useless` | Oznacz jako nieprzydatna |
| `GET` | `/api/user-offers/not-applied` | Watchlist |
| `GET` | `/api/user-offers/applied` | Zaaplikowane |
| `GET` | `/api/user-offers/{id}/apply-email` | One-click apply z emaila |
| `GET` | `/api/user-offers/{id}/useless-email` | One-click ukryj z emaila |
| `GET` | `/api/analytics/requirements/top` | Top skille ogółem |
| `GET` | `/api/analytics/requirements/top-by-level` | Top skille per poziom |
| `GET` | `/api/analytics/requirements/levels` | Dostępne poziomy |
| `GET` | `/api/analytics/requirements/sources` | Dostępne źródła |
| `GET` | `/api/python-script/run` | Uruchom scraper |
| `POST` | `/api/python-script/import` | Import z JSON |
| `POST` | `/api/python-script/import-from-url` | Import z URL |
| `GET` | `/api/notifications/daily-unapplied-offers` | Wyślij email ręcznie |
| `GET` | `/api/admin/users` | Lista użytkowników (ADMIN) |
| `GET` | `/api/stats/summary` | Podsumowanie statystyk |

Pełna dokumentacja interaktywna: `http://localhost:8081/swagger-ui.html`

---

## Struktura projektu

```
src/main/java/.../offerbrowserprototype/
├── domain/
│   ├── offer/              # encje Offer, OfferFlag, handlery CRUD
│   ├── requirement/        # encja Requirement, handlery analytics
│   ├── usseroffer/         # pipeline użytkownika (apply/useless/watchlist)
│   ├── loginaandregister/  # rejestracja, logowanie JWT, zmiana hasła
│   ├── statistics/         # handlery statystyk (city/level/count)
│   └── dto/                # DTO dla wszystkich warstw
└── infrastructure/
    ├── web/                # 12 kontrolerów REST
    ├── facade/             # fasady kompozytujące handlery
    ├── repository/         # interfejsy Spring Data JPA
    ├── security/           # JwtService, filtry Spring Security
    ├── service/            # MailService, OfferImportService, PythonScriptService
    └── cache/              # OfferCacheFacade (Redis)

frontend/src/pages/
├── offers/                 # OffersPage, OfferDetailPage, MyOffersPage
├── analytics/              # RequirementsPage (wykresy skills)
├── auth/                   # LoginPage, RegisterPage, ConfirmPage
├── AdminPage.jsx
├── DashboardPage.jsx
├── ImportPage.jsx
├── NotesPage.jsx
└── StatsPage.jsx
```
