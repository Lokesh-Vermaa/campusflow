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

## Phase 2: Campus Data, Timetable Upload, Availability Engine, Bounded Queries
- **Duplicate YAML root key is fatal**: SnakeYAML rejects duplicate top-level keys silently in some parsers but throws in Spring Boot; always merge new `spring.*` config into the existing `spring:` block — never append a second `spring:` block at the bottom of the file.
- **commons-csv 1.10+ requires commons-io at runtime**: `UnsynchronizedBufferedReader` moved to `commons-io`; since it is not a transitive Maven dependency, it must be declared explicitly with version `2.18.0`.
- **Flyway V2 seed migration is essential for integration tests**: After `flyway.clean() + migrate()` the DB is empty; every integration test that calls `repository.findAll().get(0)` will throw an `IndexOutOfBoundsException` unless a V2 seed migration populates reference data (blocks, rooms, guest rooms, parking zones).
- **Bounded availability query pattern**: The engine fetches exactly 3 queries (all rooms, all timetable slots for the day, all booking items for the day) regardless of room count. All filtering (capacity, block, features, overlap) is done in-memory on the pre-fetched lists — no N+1.
- **Pure POJO engine is easier to test**: `RoomAvailabilityEngine` has zero Spring dependencies; unit tests create entities directly, pass lists in, assert on records out — no `@SpringBootTest` context, no DB, instant feedback.
- **Timetable exclusion constraint**: PostgreSQL `EXCLUDE USING gist (room_id WITH =, academic_term WITH =, day_of_week WITH =, int4range(start_minute, end_minute, '[)') WITH &&) WHERE (active = TRUE)` prevents overlapping timetable slots at the DB level; `DataIntegrityViolationException` is caught in the service and re-thrown as `ConflictException`.
- **Touching intervals must NOT overlap**: Business rule requires `10:00–11:00` and `11:00–12:00` to be independent bookable slots. The overlap check uses strict inequality `startA < endB && startB < endA` (not `<=`).
