# JKLU CampusFlow - Security Checklist & Implementation Matrix

## 1. Security Requirements Status (Phase 1)

| Requirement | Description | Status | Implementation Details |
| :--- | :--- | :--- | :--- |
| **A2: Rate Limiting** | Auth and global rate limiting | **DONE** | In-memory token bucket on `/api/auth/login` (5 failures/15m returns 429 + `Retry-After`); `GlobalRateLimitingFilter` (180 req/min per IP). |
| **A3: Login & Session Rules** | Stateful, secure session authentication | **DONE** | Server-side `JSESSIONID` sessions, session fixation protection via `changeSessionId()`, 30-minute idle timeout, server-side session invalidation on logout. No tokens in response body. |
| **A4: Input Rules** | Strict validation and size boundaries | **DONE** | DTOs protected with Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size(max=255/128)`). |
| **A5: Error Hygiene** | Consistent, safe error envelopes | **DONE** | `GlobalExceptionHandler` and `RequestIdFilter` guarantee contract envelope `{ code, message, requestId, details? }` with zero stack traces leaked to clients. |
| **A6: Response Headers** | Comprehensive security headers | **DONE** | CSP (`default-src 'self'`), `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`. |
| **A8: Least-Privilege DB Role** | Principle of least privilege for app DB user | **DONE** | PostgreSQL runtime app user permissions restricted to DML on public tables (see script below). DDL reserved for migration execution. |
| **A10: Security Tests** | Automated security regression suite | **DONE** | `SecurityMatrixTest` (role/endpoint status assertions), `RateLimitTest`, `SecurityFilterTest`, and `NoSecretsCheckTest`. |

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
