# JKLU CampusFlow

> **AI-Agent-Powered Campus Venue, Guest House, and Parking Management System**  
> Built for the **JKLU AI-Buildathon 1.0 (Problem Statement PS-02)**.

---

## 1. Overview

Organisers across university campuses (faculty, staff, student club leads) struggle to find genuinely free rooms because they must manually cross-check timetables of many semesters, avoiding clashes with regular classes, labs, pre-scheduled campus events, and existing bookings.

**JKLU CampusFlow** solves this through a modern, **chat-first AI assistant** with deterministic conflict-solving tools, room availability heatmaps, guest house reservations, and visitor parking allocation.

---

## 2. Repository Architecture

This repository is organized as a unified monorepo:

```text
campusflow/
├── frontend/             # React 18 + Vite + TypeScript (Chat-first UI & Mock API)
│   ├── src/              # Application source code
│   ├── docs/             # API Contract & Security Checklist
│   ├── package.json      # Dependencies and build scripts
│   └── README.md         # Detailed Frontend documentation & demo flows
└── backend/              # Spring Boot 3.5.16 + Java 26 (release=21 bytecode) + PostgreSQL 16
    ├── src/              # Java backend sources and Flyway migrations
    ├── docs/             # Backend Architecture & Learning Notes
    └── pom.xml           # Pinned dependencies & Maven configuration
```

### Backend Toolchain & Runtime
- **JDK Runtime**: JDK 26 (`26.0.2`).
- **Bytecode Target**: Java 21 LTS (`release=21`, bytecode version 65) for full compatibility with Spring ASM classpath scanning and ByteBuddy.
- **Spring Boot**: `3.5.16`.
- **Database**: PostgreSQL 16 with `btree_gist` extension on port 5432.
- **LLM Engine**: Custom `RestClient` implementation behind `AgentPlanner` interface.

---

## 3. Quick Start (Frontend)

To run the frontend locally:

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) in your browser.

- **Available Demo Accounts** (1-click login on `/login`):
  - **Student**: `student@demo.jklu.test`
  - **Staff**: `staff@demo.jklu.test`
  - **Admin**: `admin@demo.jklu.test`
  - *(Password for all demo accounts: `password123`)*

---

## 4. Key Capabilities

- **Chat-First AI Assistant**: Landing experience directly into an AI composer with role-aware suggestions and persistent conversation history.
- **Deterministic Conflict Solving**: Real-time cross-checks against 8 semester timetables to guarantee clash-free room recommendations.
- **Multi-Resource Proposals**: Coordinated booking packages bundling classrooms, MDC Guest House rooms, and Main Gate visitor parking.
- **Administrative Governance**: Approval queue, timetable CSV syncing, master data management, and tamper-evident audit logs.
- **Dark Mode**: Neutral black and dark charcoal theme with top-right toggle (Light, Dark, System).

For detailed documentation, API contracts, security checklists, and demo scripts, see [frontend/README.md](frontend/README.md).
