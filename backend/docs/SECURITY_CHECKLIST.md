# JKLU CampusFlow - Security Checklist & Implementation Matrix

## 1. Security Requirements Status (Phase 1)

| Requirement | Description | Status | Implementation Details |
| :--- | :--- | :--- | :--- |
| **A2: Rate Limiting** | Auth and global rate limiting | **DONE** | Path-specific & global limits: Login (5/min), Messages (10/min), Uploads (5/min), Global (180/min). Returns HTTP 429 + `Retry-After: 60`. Tested in `RateLimitTest`. |
| **A3: Login & Session Rules** | Stateful, secure session authentication | **DONE** | Server-side `JSESSIONID` sessions, session fixation protection via `changeSessionId()`, 30-minute idle timeout, server-side session invalidation on logout. No tokens in response body. Tested in `AuthControllerTest`. |
| **A4: Input Rules** | Strict validation and size boundaries | **DONE** | DTOs protected with Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Min`). Unknown properties rejected with 400 (`fail-on-unknown-properties: true`). Max upload 10 MB with magic bytes verification. |
| **A5: Error Hygiene** | Consistent, safe error envelopes | **DONE** | `GlobalExceptionHandler` and `RequestIdFilter` guarantee contract envelope `{ code, message, requestId, details? }` with zero stack traces or class names leaked to clients (`include-stacktrace: never`). |
| **A6: Response Headers** | Comprehensive security headers | **DONE** | `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-cache, no-store, max-age=0, must-revalidate`. Tested in `SecurityFilterTest`. |
| **A8: Least-Privilege DB Role & Trigger** | Append-only audit log & restricted DB role | **DONE** | Append-only PostgreSQL trigger `trg_audit_log_append_only` forbidding UPDATE and DELETE on `audit_logs` table (migration `V3`). Tested in `AuditLogIntegrationTest`. |
| **A10: Security Tests** | Automated security regression suite | **DONE** | Comprehensive suite: `SecurityMatrixTest` (role/endpoint status assertions), `RateLimitTest`, `SecurityFilterTest`, `NoSecretsCheckTest`, `BookingIntegrationTest` (IDOR, double-booking exclusion constraints), and `AuditLogIntegrationTest`. |

---

## 2. A8 Least-Privilege Database Role Script

For production deployments, execute the following to separate migration (DDL) privileges from application runtime (DML) privileges:

```sql
-- Create restricted application runtime role
CREATE ROLE campusflow_app WITH LOGIN PASSWORD 'strong_runtime_password';

-- Grant connection to database
GRANT CONNECT ON DATABASE campusflow TO campusflow_app;

-- Grant DML permissions on public schema
GRANT USAGE ON SCHEMA public TO campusflow_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO campusflow_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO campusflow_app;

-- Ensure future tables inherit DML permissions
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO campusflow_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO campusflow_app;

-- Revoke dangerous DDL rights
REVOKE CREATE ON SCHEMA public FROM campusflow_app;
```
