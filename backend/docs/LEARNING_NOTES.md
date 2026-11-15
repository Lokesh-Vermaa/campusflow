# JKLU CampusFlow Backend - Learning Notes

## Phase 0: Skeleton, Build, Toolchain, and Initial Schema
- **JDK & Compiler Compatibility**: While JDK 26.0.2 is the installed host runtime, Spring Boot 3.5.16 uses Spring Framework 6.2.x and repackaged ASM 9.7, which accepts up to bytecode version 65 (Java 21). Setting `<maven.compiler.release>21</maven.compiler.release>` enables seamless cross-compilation on JDK 26 without class format errors.
- **ByteBuddy & Mockito**: ByteBuddy requires `-Dnet.bytebuddy.experimental=true -XX:+EnableDynamicAgentLoading` to attach and mock classes cleanly when running on modern JVMs. Configuring this inside `maven-surefire-plugin` ensures tests run warning-free.
- **Flyway & btree_gist**: PostgreSQL 16 `btree_gist` extension is successfully enabled for both `campusflow` and `campusflow_test` databases. Flyway handles temporal range and integer range exclusion constraints (`tstzrange`, `int4range`) natively.
- **Test Database Hygiene**: Test profile runs against `campusflow_test` with `clean-disabled: false`, performing clean and migrate before integration tests. Dev profile has `clean-disabled: true` to prevent accidental production/dev data drops.

## Phase 1: Foundation - Auth, CSRF, Security, Error Handling, Demo Seed
- **Dual Session & Token Model**: Dual extraction of JWT tokens from both HttpOnly cookies (`AUTH-TOKEN` / `JSESSIONID`) and `Authorization: Bearer <token>` enables both browser web apps and CLI/curl/Swagger clients to authenticate seamlessly.
- **Rate Limiting with Sliding Window**: In-memory token bucket/sliding window records IP-based failed login attempts. After 5 consecutive failures within a 15-minute window, subsequent requests return 429 Too Many Requests with a dynamic `Retry-After` header.
- **Error Hygiene & MDC Request Tracing**: `RequestIdFilter` injects `req-<UUID>` into SLF4J MDC and response headers. `GlobalExceptionHandler` ensures stack traces are never leaked; all exceptions map to `{ code, message, requestId, details? }`.
- **Security Matrix Verification**: Parameterized security matrix tests validate that anonymous, STUDENT, STAFF, and ADMIN principals are checked against protected endpoints with expected 200, 401, or 403 responses.
