# JKLU CampusFlow Backend

Production-grade Spring Boot backend for JKLU CampusFlow (Problem Statement PS-02: AI-agent campus venue, guest house, and parking management).

---

## 1. Architecture Overview

```
                      ┌─────────────────────────────────┐
                      │    Spring Security (Session)    │
                      │   CSRF Filter & Rate Limiter    │
                      └────────────────┬────────────────┘
                                       │
        ┌──────────────────────────────┼──────────────────────────────┐
        ▼                              ▼                              ▼
┌───────────────┐              ┌───────────────┐              ┌───────────────┐
│ Agent / Chat  │              │    Booking    │              │ Availability  │
│ Controller    │              │  Controller   │              │  Controller   │
└───────┬───────┘              └───────┬───────┘              └───────┬───────┘
        ▼                              ▼                              ▼
┌───────────────┐              ┌───────────────┐              ┌───────────────┐
│ Deterministic │              │    Booking    │              │ Availability  │
│ Planner Engine│              │    Service    │              │    Engine     │
└───────┬───────┘              └───────┬───────┘              └───────┬───────┘
        │                              │                              │
        └──────────────────────────────┼──────────────────────────────┘
                                       │
                                       ▼
                        ┌─────────────────────────────┐
                        │   Spring Data JPA (15 Repos)│
                        └──────────────┬──────────────┘
                                       │
                                       ▼
                        ┌─────────────────────────────┐
                        │ PostgreSQL 16 (btree_gist)  │
                        │ Flyway Migrations (V1,V2,V3)│
                        └─────────────────────────────┘
```

- **Deterministic Agent Planner**: Dynamic, data-driven room and resource selection based on capacity, timetable clashes, guest room availability, and visitor parking slots. Emits SSE stream conforming to `API_CONTRACT.md`.
- **Availability Engine**: SQL interval-overlap algorithms checking 8 semester timetables and active bookings.
- **Audit Service**: Append-only tamper-evident logs for all user mutations, approvals, and agent steps (`AGENT_REASONING`, `TOOL_CALL`).
- **Reports Service**: SQL aggregates computing hourly venue utilisation, top/least-used rooms, peak hours, and guest house / parking occupancy.

---

## 2. Quick Run Steps for Judges (5 Minutes Setup)

### Prerequisites
1. **Java**: JDK 21 or JDK 26 installed (`java -version`).
2. **PostgreSQL**: PostgreSQL 16 running on `localhost:5432`.

### Database Initialization
Connect to PostgreSQL as `postgres` and run:
```sql
CREATE DATABASE campusflow;
CREATE DATABASE campusflow_test;

\c campusflow;
CREATE EXTENSION IF NOT EXISTS btree_gist;

\c campusflow_test;
CREATE EXTENSION IF NOT EXISTS btree_gist;
```

### Backend Launch
1. Copy the environment configuration:
   ```powershell
   Copy-Item .env.example .env
   ```
2. Start the Spring Boot application with the `dev,demo` profiles active:
   - **Windows (PowerShell)**:
     ```powershell
     $env:SPRING_PROFILES_ACTIVE="dev,demo"
     .\mvnw.cmd spring-boot:run
     ```
   - **Linux / macOS (Bash)**:
     ```bash
     export SPRING_PROFILES_ACTIVE="dev,demo"
     ./mvnw spring-boot:run
     ```
3. Verify startup:
   The backend starts on port `8080`.
   Open [http://localhost:8080/api/content/announcement](http://localhost:8080/api/content/announcement) to verify.

---

## 3. Profiles & Test Commands

| Profile | Purpose | Command |
| :--- | :--- | :--- |
| **`dev,demo`** | Local development with demo seeder | `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev,demo` |
| **`test`** | Automated integration tests against `campusflow_test` | `.\mvnw.cmd test "-Dspring.profiles.active=test,demo"` |
| **`verify`** | Full Maven verification & repackaging | `.\mvnw.cmd verify "-Dspring.profiles.active=test,demo"` |

### Test Suite Summary
- **Total Tests**: 148 tests executed, 0 failures, 0 errors.
- **Key Test Classes**:
  - `EndToEndAcceptanceFlowIntegrationTest`: Complete 8-step staff chat -> conflict -> proposal -> submit -> admin approve -> calendar update -> audit log lifecycle.
  - `AgentDataDrivenPlannerIntegrationTest`: Verifies deterministic capacity rules, timetable clashes, guest room allocation, clarifying questions, and relative date parsing in `Asia/Kolkata`.
  - `SecurityMatrixTest`: 53 tests enforcing strict role-based access control (Student vs Staff vs Admin).
  - `ReportsAndContentIntegrationTest`: Hourly utilisation aggregates, date-range filters, and announcements.
  - `TimetableParserTest`: Clean CSV import and validation of 6 distinct error cases.

---

## 4. Timetable CSV Import Format
Timetable files can be uploaded at `POST /api/admin/timetable/upload` (Max 10 MB, CSV or XLSX):

```csv
Room,Day,Start Time,End Time,Course Code,Section,Faculty,Term
A-101,1,09:00,10:00,CS201,Sec A,Dr. Neha Verma,Spring 2027
A-101,1,11:00,12:00,CS203,Sec A,Prof. Sharma,Spring 2027
T-101,2,14:00,16:00,CS302L,Lab 1,Dr. Amit Sengupta,Spring 2027
```

Sample files in `backend/sample-data/`:
- `timetable_valid.csv`: Clean semester timetable with standard slots.
- `timetable_with_errors.csv`: Includes 6 distinct error cases (missing room, invalid day, malformed time format, start after end, invalid header, non-existent room).

---

## 5. Security & Error Hygiene
- **Authentication**: Pure server-side session cookie authentication (`JSESSIONID`, HttpOnly, SameSite=Lax, Secure in prod) with session fixation protection.
- **CSRF**: `CookieCsrfTokenRepository.withHttpOnlyFalse()` enabled on all mutating requests.
- **Rate Limiting**: `GlobalRateLimitingFilter` enforces 5/min on login, 10/min on messages, 5/min on uploads, and 180/min globally, returning HTTP 429 + `Retry-After: 60`.
- **Security Headers**: CSP `default-src 'none'; frame-ancestors 'none'`, Referrer-Policy `no-referrer`, X-Content-Type-Options `nosniff`, and Cache-Control `no-store` on all authenticated endpoints.
- **Error Hygiene**: Generic error responses hiding stack traces, SQL syntax, or internal class names in compliance with API contract.

---

## 6. Troubleshooting

| Symptom | Cause | Solution |
| :--- | :--- | :--- |
| **`Process terminated with exit code: 1`** | Port 8080 already bound by another process | Run `Get-Process -Name java \| Stop-Process -Force` in PowerShell to terminate existing JVM instances. |
| **`type "gist" does not exist`** | `btree_gist` extension missing | Connect to `campusflow` and `campusflow_test` databases and run `CREATE EXTENSION IF NOT EXISTS btree_gist;`. |
| **`FATAL: password authentication failed`** | Mismatched database credentials in `.env` | Update `DB_USER` and `DB_PASSWORD` in `backend/.env` to match your local PostgreSQL configuration. |
| **`flyway_schema_history` lock** | Interrupted migration | Connect via `psql` and check `SELECT * FROM flyway_schema_history;` or run `.\mvnw.cmd flyway:repair`. |
