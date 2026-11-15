# JKLU CampusFlow Backend - Learning Notes

## Phase 0: Skeleton, Build, Toolchain, and Initial Schema
- **JDK & Compiler Compatibility**: While JDK 26.0.2 is the installed host runtime, Spring Boot 3.5.16 uses Spring Framework 6.2.x and repackaged ASM 9.7, which accepts up to bytecode version 65 (Java 21). Setting `<maven.compiler.release>21</maven.compiler.release>` enables seamless cross-compilation on JDK 26 without class format errors.
- **ByteBuddy & Mockito**: ByteBuddy requires `-Dnet.bytebuddy.experimental=true -XX:+EnableDynamicAgentLoading` to attach and mock classes cleanly when running on modern JVMs. Configuring this inside `maven-surefire-plugin` ensures tests run warning-free.
- **Flyway & btree_gist**: PostgreSQL 16 `btree_gist` extension is successfully enabled for both `campusflow` and `campusflow_test` databases. Flyway handles temporal range and integer range exclusion constraints (`tstzrange`, `int4range`) natively.
- **Test Database Hygiene**: Test profile runs against `campusflow_test` with `clean-disabled: false`, performing clean and migrate before integration tests. Dev profile has `clean-disabled: true` to prevent accidental production/dev data drops.

## Phase 1: Foundation - Auth, CSRF, Security, Error Handling, Demo Seed
- **Pure Server-Side Session Authentication**: Migrated completely away from Bearer tokens to native server-side `JSESSIONID` cookie sessions with session-fixation protection (`changeSessionId()`), 30-minute idle expiration, and full session invalidation on logout.
- **Decoupled Demo User Seeding**: Removed all static user credentials and plaintext demo passwords from Flyway SQL scripts. Demo users are created strictly at runtime via `DemoUserSeeder` in `demo`, `dev`, and `test` profiles using BCrypt-hashed passwords from `DEMO_USER_PASSWORD`.
- **Comprehensive Rate Limiting**: Implemented two tiers of rate limiting: 5 failed login attempts per 15-minute window returning HTTP 429 with `Retry-After`, plus global IP rate limiting (180 req/min).
- **Exact Contract Conformance**: Synchronized all authentication paths and JSON structures with `API_CONTRACT.md`. Mutating requests enforce standard CSRF verification via `CookieCsrfTokenRepository` (`XSRF-TOKEN` / `X-CSRF-TOKEN`).
