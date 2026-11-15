# JKLU CampusFlow Backend - Learning Notes

## Phase 0: Skeleton, Build, Toolchain, and Initial Schema
- **JDK & Compiler Compatibility**: While JDK 26.0.2 is the installed host runtime, Spring Boot 3.4.3 uses Spring Framework 6.2.3 and repackaged ASM 9.7, which accepts up to bytecode version 65 (Java 21). Setting `<maven.compiler.release>21</maven.compiler.release>` enables seamless cross-compilation on JDK 26 without class format errors.
- **ByteBuddy & Mockito**: ByteBuddy requires `-Dnet.bytebuddy.experimental=true -XX:+EnableDynamicAgentLoading` to attach and mock classes cleanly when running on modern JVMs. Configuring this inside `maven-surefire-plugin` ensures tests run warning-free.
- **Flyway & btree_gist**: PostgreSQL 16 `btree_gist` extension is successfully enabled for both `campusflow` and `campusflow_test` databases. Flyway handles temporal range and integer range exclusion constraints (`tstzrange`, `int4range`) natively.
- **Test Database Hygiene**: Test profile runs against `campusflow_test` with `clean-disabled: false`, performing clean and migrate before integration tests. Dev profile has `clean-disabled: true` to prevent accidental production/dev data drops.
