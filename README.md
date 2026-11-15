# JKLU CampusFlow

> **AI-Agent-Powered Campus Venue, Guest House, and Parking Management System**  
> Built for the **JKLU AI-Buildathon 1.0 (Problem Statement PS-02)**.

---

## 1. Architecture Overview

Organisers across university campuses (faculty, staff, student club leads) struggle to find genuinely free rooms because they must manually cross-check timetables of many semesters, avoiding clashes with regular classes, labs, pre-scheduled campus events, and existing bookings.

**JKLU CampusFlow** solves this through a modern, **chat-first AI assistant** with deterministic conflict-solving tools, room availability heatmaps, guest house reservations, and visitor parking allocation.

```
                    ┌────────────────────────────────────────────────────────┐
                    │               React 18 + Vite Frontend                 │
                    │        (Port 5173, SSE Stream Reader, Tailwind)         │
                    └───────────────────────────┬────────────────────────────┘
                                                │ REST / SSE / Session Cookie
                                                ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                Spring Boot 3.5 Backend                                  │
│                                      (Port 8080)                                       │
│                                                                                        │
│  ┌────────────────────────┐  ┌────────────────────────┐  ┌──────────────────────────┐  │
│  │    Agent Controller    │  │    Booking Service     │  │   Availability Engine    │  │
│  │ (SSE Stream, Chat,     │  │ (Multi-resource bundle:│  │  (Timetable clash check, │  │
│  │  Conversations)        │  │  Room + Guest + Park)  │  │   Interval overlap)      │  │
│  └───────────┬────────────┘  └───────────┬────────────┘  └────────────┬─────────────┘  │
│              │                           │                            │                │
│              ▼                           ▼                            ▼                │
│  ┌────────────────────────┐  ┌────────────────────────┐  ┌──────────────────────────┐  │
│  │  Deterministic Planner │  │     Audit Service      │  │    Security & Rate       │  │
│  │ (Smallest fit, real    │  │ (Tamper-evident logs,  │  │   (Session auth, CSRF,   │  │
│  │  data, optional LLM)   │  │  AGENT_REASONING, TOOL)│  │    IDOR, RBAC matrix)    │  │
│  └───────────┬────────────┘  └───────────┬────────────┘  └──────────────────────────┘  │
└──────────────┼───────────────────────────┼─────────────────────────────────────────────┘
               ▼                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        PostgreSQL 16 Database (`campusflow`)                            │
│           (btree_gist exclusion constraints, Flyway V1-V3 migrations, UTF-8)           │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Quick Run Steps for Judges (5 Minutes Setup)

### Step 1: Prerequisites
- **Java**: JDK 21 or JDK 26 installed (`java -version`).
- **Node.js**: Node 18+ and npm installed (`node -v`, `npm -v`).
- **PostgreSQL**: PostgreSQL 16 running on `localhost:5432`.

### Step 2: Database Setup
Open `psql` or pgAdmin as the `postgres` superuser:
```sql
CREATE DATABASE campusflow;
CREATE DATABASE campusflow_test;

\c campusflow;
CREATE EXTENSION IF NOT EXISTS btree_gist;

\c campusflow_test;
CREATE EXTENSION IF NOT EXISTS btree_gist;
```

### Step 3: Backend Setup & Launch
1. Open a terminal and navigate to `backend/`:
   ```powershell
   cd backend
   Copy-Item .env.example .env
   ```
2. Inspect `backend/.env` (default credentials: `postgres` / `postgres` on port 5432).
3. Start the backend with the `dev,demo` profiles active:
   - **PowerShell (Windows)**:
     ```powershell
     $env:SPRING_PROFILES_ACTIVE="dev,demo"
     .\mvnw.cmd spring-boot:run
     ```
   - **Bash (Linux / macOS)**:
     ```bash
     export SPRING_PROFILES_ACTIVE="dev,demo"
     ./mvnw spring-boot:run
     ```
4. Verify backend health:
   ```bash
   curl http://localhost:8080/api/content/announcement
   ```

### Step 4: Frontend Setup & Launch
1. Open a second terminal and navigate to `frontend/`:
   ```powershell
   cd frontend
   Copy-Item .env.example .env
   npm install
   npm run dev
   ```
2. Open your browser at [http://localhost:5173](http://localhost:5173).

---

## 3. Demo Credentials & Acceptance Flow

### Demo Accounts (`/login`)
When `VITE_DEMO_MODE=true` (set in `frontend/.env`), the login screen presents one-click login pills:

| Role | Email | Password | Allowed Capabilities |
| :--- | :--- | :--- | :--- |
| **Student** | `student@demo.jklu.test` | Value in `VITE_DEMO_PASSWORD` (`change-me-demo-password`) | Classrooms, general zones, basic bookings |
| **Staff** | `staff@demo.jklu.test` | Value in `VITE_DEMO_PASSWORD` (`change-me-demo-password`) | Amphitheatres, Labs, MDC Guest House, Visitor Parking |
| **Admin** | `admin@demo.jklu.test` | Value in `VITE_DEMO_PASSWORD` (`change-me-demo-password`) | Approvals Queue, Timetable Sync, Master Data, Audit Log, Reports |

### 5-Minute End-to-End Acceptance Walkthrough
Follow the automated script verified in `backend/docs/DEMO_SCRIPT.md`:
1. **Login as Staff**: Click `staff@demo.jklu.test` on `/login`.
2. **Chat with Agent**: On `/`, type:
   > *"Guest lecture next Thursday 3-5 PM, 80 people, speaker from Delhi needing stay and parking"*
3. **Observe SSE Streaming**:
   - The agent outputs its execution plan and live reasoning.
   - It invokes `find_free_rooms(minCapacity: 80)` and evaluates candidate room `C-204`.
   - It simulates tool retry, detects clash with `CS201 Sem 3 Data Structures`, displays the **Conflict Card**, and recommends free alternatives (`A-201`, `M-201`).
   - It reserves guest room `G-102` and visitor parking `P-VISITOR`.
   - It presents the structured **Proposal Card**.
4. **Submit for Approval**: Click **"Submit for Approval"** on the proposal card. A pending booking bundle is created (`201 CREATED`).
5. **Switch to Admin**: Logout and log in as `admin@demo.jklu.test`.
6. **Approve in Queue**: Navigate to `/admin/approvals`. Find the pending guest lecture reservation and click **Approve**.
7. **Verify Calendar & Audit Log**:
   - Navigate to `/admin/audit` to view tamper-evident records: `AGENT_REASONING`, `TOOL_CALL`, and `APPROVAL`.
   - Check Room Availability at `/finder` — Room `A-201` is now marked reserved for that Thursday 15:00–17:00 slot.

---

## 4. Test Suites & Verification Gates

### Backend Test Suite
```powershell
cd backend
.\mvnw.cmd test "-Dspring.profiles.active=test,demo"
```
- **148 tests passing (0 failures, 0 errors)**.
- Includes `EndToEndAcceptanceFlowIntegrationTest` (8-step complete lifecycle), `AgentDataDrivenPlannerIntegrationTest`, `SecurityMatrixTest` (53 tests), `ReportsAndContentIntegrationTest`, `TimetableParserTest`, `BoundedQueryTest`.

### Frontend Quality Gates
```powershell
cd frontend
npm run lint          # ESLint 9 clean (0 errors)
npm run typecheck     # TypeScript strict compilation clean (0 errors)
npm test              # Vitest 14 test suites, 60 tests passing
npm run knip          # Zero unused files, dependencies, or exports
npm run build         # Production Vite build clean
```

---

## 5. Security & Governance

- **Session Security**: HttpOnly, SameSite=Lax session cookies with server-side session invalidation on logout.
- **CSRF Protection**: Double-submit cookie pattern (`X-XSRF-TOKEN` header verified against `XSRF-TOKEN` cookie).
- **IDOR Protection**: Strict ownership checks; students cannot access or manipulate other users' bookings or conversations.
- **Tamper-Evident Audit Logs**: Database-enforced append-only trigger prevents updating or deleting `audit_logs` entries.
- **Optional LLM Planner**: Fully functional offline deterministic planner runs when `LLM_API_KEY` is not provided.

---

## 6. Troubleshooting

| Symptom | Cause | Solution |
| :--- | :--- | :--- |
| **Port 8080 already in use** | Stray Java process running in background | Run `Get-Process -Name java \| Stop-Process -Force` (Windows) or `pkill -f campusflow` (Linux). |
| **Port 5173 already in use** | Previous Vite dev server running | Run `Get-Process -Name node \| Stop-Process -Force` or use `npm run dev -- --port 5174`. |
| **PostgreSQL `btree_gist` error** | Extension not enabled on database | Connect as superuser and run `CREATE EXTENSION IF NOT EXISTS btree_gist;` on `campusflow` and `campusflow_test`. |
| **`Access is denied` in PowerShell** | Restricted execution policy | Run PowerShell as Administrator and execute `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`. |
| **Frontend 401 Unauthorized loops** | Expired session or mismatched cookie | Clear browser cookies for `localhost`, ensure `VITE_API_BASE_URL=http://localhost:8080`, and log in again via `/login`. |
| **Rate Limit 429 Too Many Requests** | Exceeded 5 logins/min or 10 msgs/min | Wait 60 seconds for the rate limiting sliding window to clear (`Retry-After: 60`). |
