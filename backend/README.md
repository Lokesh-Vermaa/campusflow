# JKLU CampusFlow Backend

Production-grade Spring Boot backend for JKLU CampusFlow (Problem Statement PS-02: AI-agent campus venue, guest house, and parking management).

---

## 1. Runtime & Environment Decisions
- **Spring Boot Version**: `3.5.16` (managed via Spring Boot dependencies BOM).
- **Runtime Host JDK**: Oracle JDK `26.0.2`.
- **Target Bytecode**: `release=21` (Java 21 LTS class file major version 65) via `maven-compiler-plugin`.
- **Database**: PostgreSQL 16 on `localhost:5432` with `btree_gist` extension for exclusion constraints.
- **Docker**: Docker is not used in this environment. All integration and unit tests run directly against the local PostgreSQL `campusflow_test` database. (Containerization is slated as future work).

---

## 2. Active Profiles & Exact Run Commands

Datasource configuration (`DB_URL`, `DB_USER`, `DB_PASSWORD` from `.env`) is declared in base `application.yml` via `spring.config.import=optional:file:.env[.properties]`.

| Profile | Purpose | Run Command |
| :--- | :--- | :--- |
| **`dev`** | Local development against `campusflow` database | `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev` |
| **`demo`** | Local demo mode with runtime demo user/seed sync | `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo` |
| **`dev,demo`**| Dev mode with demo seeder enabled | `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev,demo` |
| **`test`** | Integration test suite against `campusflow_test` | `.\mvnw.cmd test` |
| **`prod`** | Production mode (refuses start if blank password, `*` origin, or demo profile) | `java -jar target/campusflow-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod` |

---

## 3. Demo Data & Seeding Rules
- Demo users and demo bookings exist **ONLY** in the `demo` profile.
- On startup with the `demo` profile active, `DemoUserSeeder`:
  - Upserts `student@demo.jklu.test` (Aarav Sharma), `staff@demo.jklu.test` (Dr. Neha Verma), and `admin@demo.jklu.test` (Rajesh Patel) with the BCrypt hash of `DEMO_USER_PASSWORD`.
  - Seeds 25 rooms across 3 blocks (Academic Block, Technology Block, Management Block), including 2 amphitheatres and 4 labs.
  - Seeds 12 MDC guest rooms and 3 parking zones (`P-STAFF`, `P-VISITOR`, `P-STUDENT`).
  - Seeds semester timetable recurring slots and 20 sample bookings in mixed states.
  - Logs: `Demo users synced (demo profile)`.

---

## 4. Timetable CSV Import Format
Timetable files can be uploaded at `POST /api/admin/timetable/upload` (Max 10 MB, CSV or XLSX):

```csv
Room,Day,Start Time,End Time,Course Code,Section,Faculty,Term
A-101,1,09:00,10:00,CS201,Sec A,Dr. Neha Verma,Spring 2027
A-101,1,11:00,12:00,CS203,Sec A,Prof. Sharma,Spring 2027
T-101,2,14:00,16:00,CS302L,Lab 1,Dr. Amit Sengupta,Spring 2027
```

Sample files are available in `backend/sample-data/`:
- `timetable_valid.csv`: Clean semester timetable with standard slots.
- `timetable_with_errors.csv`: Includes 6 distinct error cases (missing room, invalid day, malformed time format, start after end, invalid header, non-existent room).

---

## 5. Security & Error Hygiene
- **Authentication**: Pure server-side session cookie authentication (`JSESSIONID`, HttpOnly, SameSite=Lax, Secure in prod) with session fixation protection.
- **CSRF**: `CookieCsrfTokenRepository.withHttpOnlyFalse()` enabled on all mutating requests.
- **Rate Limiting**: `GlobalRateLimitingFilter` enforces 5/min on login, 10/min on messages, 5/min on uploads, and 180/min globally, returning HTTP 429 + `Retry-After: 60`.
- **Security Headers**: CSP `default-src 'none'; frame-ancestors 'none'`, Referrer-Policy `no-referrer`, X-Content-Type-Options `nosniff`, and Cache-Control `no-store` on all authenticated endpoints.
- **Error Hygiene**: Generic error responses hiding stack traces, SQL syntax, or internal class names in compliance with API contract.
