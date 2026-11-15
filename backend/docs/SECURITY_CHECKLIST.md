# JKLU CampusFlow — Security Checklist & Implementation Matrix

## 1. Security Requirements Status

| Requirement | Description | Status | Implementation Details |
| :--- | :--- | :--- | :--- |
| **A2: Rate Limiting** | Auth and global rate limiting | **DONE** | Path-specific & global limits: Login (5/min), Messages/Agent (10/min), Uploads (5/min), Global (180/min). Returns HTTP 429 + `Retry-After: 60`. Tested in `RateLimitTest`. |
| **A3: Login & Session Rules** | Stateful, secure session authentication | **DONE** | Server-side `JSESSIONID` sessions, session fixation protection via `changeSessionId()`, 30-minute idle timeout, server-side session invalidation on logout. No tokens in response body. Tested in `AuthControllerTest`. |
| **A4: Input Rules** | Strict validation and size boundaries | **DONE** | DTOs protected with Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Min`). Unknown properties rejected with 400 (`fail-on-unknown-properties: true`). Max upload 10 MB with magic bytes verification. Pagination capped at 100. |
| **A5: Error Hygiene** | Consistent, safe error envelopes | **DONE** | `GlobalExceptionHandler` and `RequestIdFilter` guarantee contract envelope `{ code, message, requestId, details? }` with zero stack traces or class names leaked to clients (`include-stacktrace: never`). |
| **A6: Response Headers** | Comprehensive security headers | **DONE** | `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-cache, no-store, max-age=0, must-revalidate`. Tested in `SecurityFilterTest`. |
| **A8: Least-Privilege DB Role & Trigger** | Append-only audit log & restricted DB role | **DONE (partial)** | Append-only PostgreSQL trigger `trg_audit_log_append_only` forbidding UPDATE and DELETE on `audit_logs` table (migration `V3`). **HONEST NOTE**: The application role currently owns all tables (not fully least-privilege). See §3 for production guidance. Tested in `AuditLogIntegrationTest`. |
| **A10: Security Tests** | Automated security regression suite | **DONE** | `SecurityMatrixTest` (every endpoint, all roles), `RateLimitTest`, `SecurityFilterTest`, `NoSecretsCheckTest`, `BookingIntegrationTest` (IDOR → 404, concurrent double-booking, conflict shape, bundle rollback), `AuditLogIntegrationTest`, `AgentScriptedFlowTest`, `AgentConversationIdorTest`, `NoDemoProfileEmptyTablesTest`. |

---

## 2. Audit Events Written

The following events are written to `audit_logs` and appear via `GET /api/admin/audit` (newest first by `created_at DESC`):

| Event | Action String | Entity Type | Trigger |
|-------|--------------|-------------|---------|
| Booking created | `BOOKING_CREATED` | BOOKING | `BookingService.createBooking()` |
| Booking approved | `APPROVAL` | BOOKING | `BookingService.approveBooking()` |
| Booking rejected | `REJECTION` | BOOKING | `BookingService.rejectBooking()` |
| Booking cancelled | `BOOKING_CANCELLED` | BOOKING | `BookingService.cancelBooking()` |
| Login success | `LOGIN_SUCCESS` | AUTH | `AuthService.login()` |
| Login failure | `LOGIN_FAILURE` | AUTH | `AuthService.login()` (bad credentials) |
| Access denied | `ACCESS_DENIED` | AUTH | Security exception handler |
| Agent chat | `AGENT_CHAT` | AGENT | `AgentController.chat()` |
| Timetable upload | `TIMETABLE_UPLOAD` | TIMETABLE | `TimetableAdminController` |

Tested by: `AuditLogIntegrationTest.testAuditLogRecordsBookingApprovalAndRejection()`

---

## 3. A8 Database Privilege Model — Honest Assessment

### Current State (Demo / Dev)
The Spring Boot application connects as a database superuser (or the user specified in `DB_USER`). This user **owns** all tables, which is convenient for development but is **not least-privilege**.

The only real enforcement is the `audit_logs` trigger (`V3__audit_trigger.sql`), which prevents UPDATE/DELETE on the audit log regardless of the application role.

### Production Recommended Setup

```sql
-- Create migration role (DDL only — runs Flyway)
CREATE ROLE campusflow_migrate WITH LOGIN PASSWORD '<strong_migration_password>';
GRANT CREATE ON DATABASE campusflow TO campusflow_migrate;
GRANT CREATE ON SCHEMA public TO campusflow_migrate;

-- Create runtime role (DML only — runs the application)
CREATE ROLE campusflow_app WITH LOGIN PASSWORD '<strong_runtime_password>';
GRANT CONNECT ON DATABASE campusflow TO campusflow_app;
GRANT USAGE ON SCHEMA public TO campusflow_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO campusflow_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO campusflow_app;

-- Lock down the audit log further: app can INSERT but not UPDATE/DELETE
REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM campusflow_app;

-- Ensure future tables inherit runtime grants
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO campusflow_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO campusflow_app;

-- Revoke DDL from runtime role
REVOKE CREATE ON SCHEMA public FROM campusflow_app;
```

**Summary**: In production, `DB_USER` should be `campusflow_app` (runtime role), and Flyway should run migrations as `campusflow_migrate`. The trigger provides a defence-in-depth layer on `audit_logs` even if the application role is more permissive than ideal.

---

## 4. Demo Profile Isolation

- Demo users exist **only** in the `demo` Spring profile (`@Profile("demo")`).
- `DemoUserSeeder` is a `CommandLineRunner` annotated `@Profile("demo")`.
- In `dev`, `test` (without `demo`), and `prod` profiles, the seeder does not run.
- Proven by: `NoDemoProfileEmptyTablesTest` — asserts rooms, users, and bookings are all empty after `flyway.clean() + flyway.migrate()` with no demo profile.
- Proven by: `DemoProfileIsolationTest` — asserts login fails without demo profile.
- Proven by: `DemoProfileActiveTest` — asserts login succeeds with demo profile.

---

## 5. No JWT / Bearer Token

- Zero JJWT dependency in `pom.xml` (confirmed by `NoSecretsCheckTest`).
- No `Authorization: Bearer` header anywhere in the application.
- Authentication is **exclusively** via `JSESSIONID` HttpOnly session cookie + `X-CSRF-TOKEN` on mutating requests.
- Tested in `SecurityFilterTest.testLoginResponseHasNoBearerToken()`.

---

## 6. IDOR Policy

All resource-by-ID endpoints return **the same 404 `NOT_FOUND`** for:
- An ID that does not exist at all, AND
- An ID that exists but belongs to a different user

This prevents resource enumeration. Tested:
- `BookingIntegrationTest.testCancellationAndIdorProtection()` — booking IDOR → 404
- `AgentConversationIdorTest.testConversationIdorReturns404SameAsNonExistent()` — conversation IDOR → 404
