# JKLU CampusFlow - Backend Architecture & Runtime Environment

## Runtime Environment & Toolchain Decisions

### 1. Spring Boot & Java Configuration
- **Spring Boot Version**: `3.5.16` (the newest 3.5.x production release).
- **Runtime Host JDK**: Oracle JDK `26.0.2`.
- **Target Bytecode**: `release=21` (Java 21 LTS class file major version 65).

### 2. Rationale for `release=21` Bytecode on JDK 26 Runtime
When compiling with `release=26` (bytecode version 70), Spring Framework 6.x's embedded ASM ClassReader (version 9.7) fails during classpath scanning with:
```
java.lang.IllegalArgumentException: Unsupported class file major version 70
    at org.springframework.asm.ClassReader.<init>(ClassReader.java:200)
```
Similarly, ByteBuddy rejects bytecode version 70 without experimental flags:
```
java.lang.IllegalArgumentException: Java 26 (70) is not supported by the current version of Byte Buddy which officially supports Java 24 (68)
```
Configuring `maven-compiler-plugin` to target `<release>21</release>` leverages javac's native cross-compilation on JDK 26. This produces fully supported Java 21 LTS bytecode, ensuring 100% stability across Spring Data JPA, Hibernate ORM, Flyway, and Mockito while running directly on JDK 26.

### 3. LLM Orchestration: Direct RestClient vs Spring AI
- **Decision**: Direct Spring `RestClient` implementation behind a unified `AgentPlanner` interface.
- **Reason**: Direct `RestClient` eliminates Spring AI dependency/milestone version mismatches and provides strict, zero-overhead control over Server-Sent Events (SSE), tool calling, token streaming, and fallback mechanisms without external framework flux.

## Pinned Dependencies
| Component | Version | Description |
| :--- | :--- | :--- |
| **Spring Boot Parent** | `3.5.16` | Core framework BOM |
| **Spring Web (MVC)** | `3.5.16` | REST API and WebMVC controllers |
| **Spring Security** | `6.5.11` | Authentication, RBAC, CSRF, security filters |
| **Spring Data JPA** | `3.5.13` | Persistence layer & repositories |
| **PostgreSQL Driver** | `42.7.5` | PostgreSQL JDBC driver |
| **Flyway** | `11.3.4` | Database migration engine (PostgreSQL dialect) |
| **Springdoc OpenAPI** | `2.8.5` | Swagger UI & OpenAPI 3.0 specs |
| **Apache POI** | `5.3.0` | Excel spreadsheet parsing (.xlsx) |
| **Apache Commons CSV** | `1.12.0` | CSV timetable parsing |
| **JUnit Jupiter** | `5.12.2` | Test execution framework |
| **Mockito & ByteBuddy** | `5.14.2` / `1.15.11` | Unit & slice test mocking |
