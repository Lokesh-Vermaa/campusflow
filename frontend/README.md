# JKLU CampusFlow

> **AI-Agent-Powered Campus Venue, Guest House, and Parking Management System**  
> Built for the **JKLU AI-Buildathon 1.0 (Problem Statement PS-02)**.

---

## 1. Overview & Vision

Organisers across university campuses (faculty, staff, student club leads) struggle to find genuinely free rooms because they must manually cross-check timetables of many semesters, avoiding clashes with regular classes, labs, pre-scheduled campus events, and existing bookings.

**JKLU CampusFlow** solves this through a modern, **chat-first AI assistant shell** (ChatGPT/Claude/Gemini pattern) with full workspace navigation:
1. **Chat-First AI Assistant Shell (`/` and `/c/:id`)**: The landing experience after login puts the AI agent front and center: a clean greeting, centered auto-growing composer, role-aware suggestion cards, and a collapsible left sidebar with recent conversations grouped chronologically.
2. **Deterministic Conflict Solver & Multi-Resource Planner**: The agent generates execution plans, invokes deterministic tools, recovers from simulated tool glitches, detects timetable clashes (e.g. Sem 3 Data Structures in C-204), and packages room + guest house + parking into a unified approval proposal.
3. **Dedicated Self-Service Portals**: High-performance search engines with visual timetable heatmaps (08:00–20:00), conflict explanations, amphitheatres & lab permissions, MDC guest house reservations, and visitor parking passes.
4. **Administrative Governance**: Every state-changing booking requires Admin Approval with auditable governance, automated clash re-checks, timetable CSV syncing, and client-side validated rejection reasons.

---

## 2. Tech Stack & Architectural Principles

- **Framework**: React 18, Vite 5, TypeScript (strict mode enabled).
- **Styling & Design Tokens**: Tailwind CSS with custom design tokens mapped in `src/styles/tokens.css` based on the JKLU visual identity (`#4A67B0`, `#0069C9`, `#FF6B00`, `#C74A00`).
- **UI Primitives**: Headless UI inspired by Radix UI / shadcn/ui.
- **Routing & State**: React Router v6, TanStack Query v5.
- **Form Management**: `react-hook-form` + `@hookform/resolvers/zod` with Zod schema validation.
- **Mock Service Worker (MSW)**: Full network mocking layer (`public/mockServiceWorker.js`) simulating the Java Spring Boot + PostgreSQL backend with realistic latency and deterministic interval-overlap clash logic.
- **Date & Timezone**: `date-fns` pinned to `Asia/Kolkata`.
- **Data Visualizations**: `recharts` for hourly campus occupancy, top/least-used rooms, guest house and parking utilization.
- **Security & A11y**: Strict CSP compliance, zero `dangerouslySetInnerHTML`, `react-markdown` with `rehype-sanitize`, self-hosted `@fontsource` typography (`Inter`, `Poppins`), and WCAG AA contrast compliance.

---

## 3. Project Structure

```text
campusflow/
├── docs/
│   ├── API_CONTRACT.md          # Complete REST API specification & Zod schemas
│   └── SECURITY_CHECKLIST.md    # Security architecture, CSP headers & WCAG AA contrast matrix
├── public/
│   ├── mockServiceWorker.js     # MSW worker script for browser mock interception
│   └── favicon.svg              # Local SVG favicon
├── src/
│   ├── api/                     # Swappable API layer
│   │   ├── admin.ts             # Approvals, timetable uploads, master data, audit logs
│   │   ├── agent.ts             # Agent SSE stream simulation & chat endpoints
│   │   ├── announcement.ts      # Top announcement API
│   │   ├── auth.ts              # Login & session validation
│   │   ├── bookings.ts          # Booking mutations, idempotency keys & listing
│   │   ├── client.ts            # Typed HTTP client (CSRF token, error mapping, 401 handling)
│   │   ├── conversations.ts     # User conversation management & chat sessions
│   │   ├── guesthouse.ts        # MDC Guest house availability & reservations
│   │   ├── parking.ts           # Parking zone status & visitor permits
│   │   ├── reports.ts           # Analytics and utilization metrics
│   │   ├── rooms.ts             # Classroom finder & timetable clash queries
│   │   └── types.ts             # Shared Zod schemas and TypeScript types
│   ├── assets/                  # Local SVG icons and placeholders
│   ├── components/
│   │   ├── layout/              # AppLayout, Sidebar (collapsible rail + drawer), MobileTopBar, PageHeader
│   │   ├── common/              # ErrorBoundary, GradientHeading, ScrollReveal
│   │   └── ui/                  # Accessible UI primitives (Button, Card, Dialog, DropdownMenu, etc.)
│   ├── features/
│   │   ├── admin/               # ApprovalQueue, TimetableUpload, MasterData, AuditLog, Reports
│   │   ├── agent/               # AgentChatPage, AgentWorkAccordion, PlanCard, ToolCallRow, ProposalCard,
│   │   │                        # AlternativesCard, ReasoningSidePanel, AgentMessageRenderer
│   │   ├── auth/                # LoginPage (1-click demo logins, lockout counter), RequireRole
│   │   ├── bookings/            # BookingWizard (5 steps), MyBookings, BookingDetailDrawer
│   │   ├── dashboard/           # DashboardPage (Campus Overview stat tiles & activity)
│   │   ├── finder/              # FinderPage (Classroom Finder with timetable clash heatmaps)
│   │   ├── guesthouse/          # GuestHousePage
│   │   ├── parking/             # ParkingPage
│   │   └── venues/              # VenuesPage (Amphitheatres & Labs)
│   ├── hooks/                   # useAuth, useDebounce, useMediaQuery, useToast
│   ├── lib/                     # utils.ts, constants.ts
│   ├── mocks/                   # MSW Handlers, Seed Data & Availability Engine
│   │   ├── availabilityEngine.ts# Deterministic interval-overlap conflict detection
│   │   ├── handlers.ts          # MSW request handlers mirroring API_CONTRACT.md
│   │   └── seedData.ts          # 25 rooms, 8 semester timetables, 20 bookings, 60 audit logs
│   ├── styles/
│   │   ├── globals.css          # Tailwind base directives & layout resets
│   │   └── tokens.css           # Design tokens (--brand-azure, --brand-orange, etc.)
│   ├── test/                    # Vitest setup, RTL renderers, test mocks
│   ├── App.tsx                  # Root routes, QueryClientProvider, AuthProvider
│   └── main.tsx                 # Entry point with MSW conditionally initialized
├── eslint.config.js             # ESLint flat config with jsx-a11y and react-refresh rules
├── knip.json                    # Knip unused export & dependency config
├── package.json                 # Pinned dependencies
├── tsconfig.json                # TypeScript strict configuration
└── vite.config.ts               # Vite bundler config with vendor code splitting
```

---

## 4. Getting Started (Judge Instructions)

### Prerequisites
- Node.js `v18.0.0` or higher (`v20+` recommended)
- npm `v9+`

### Installation & Launch
```bash
# Navigate to frontend directory
cd frontend

# 1. Copy environment template
cp .env.example .env
# Windows PowerShell:
# Copy-Item .env.example .env

# 2. Install pinned dependencies
npm install

# 3. Start development server
npm run dev
```

Visit `http://localhost:5173` in your browser.

- **Real Backend Connectivity**: In `.env`, ensure:
  ```properties
  VITE_USE_MOCKS=false
  VITE_API_BASE_URL=http://localhost:8080
  VITE_DEMO_MODE=true
  VITE_DEMO_PASSWORD=change-me-demo-password
  ```
- **Demo Mode**: The login screen (`/login`) will display quick 1-click login buttons for Student, Staff, and Admin roles.

---

## 5. Scripts & Quality Gates

CampusFlow enforces strict code hygiene across all layers:

| Command | Description | Status |
| :--- | :--- | :--- |
| `npm run dev` | Launches Vite local dev server with Hot Module Replacement (HMR). | Ready |
| `npm run build` | Compiles TypeScript and builds production distribution into `dist/`. | Passing |
| `npm run preview` | Locally serves production `dist/` build. | Ready |
| `npm run typecheck` | Strict TypeScript compiler check (`tsc --noEmit`). Zero type errors. | Passing |
| `npm run lint` | ESLint 9 check including `jsx-a11y` accessibility and React Hooks rules. | Clean |
| `npm run test` | Executes Vitest test suite (14 suites, 60 tests passing). | 60/60 Passing |
| `npm run knip` | Audits unused files, dependencies, and unexported declarations. | Clean |

---

## 6. Troubleshooting & FAQ

| Issue | Cause | Resolution |
| :--- | :--- | :--- |
| **Port 5173 already in use** | Stray Vite/Node process | Run `npm run dev -- --port 5174` or kill node processes via Task Manager / `pkill -f node`. |
| **CORS errors to backend** | Backend origin mismatch | Ensure Spring Boot is running with `dev,demo` profile which explicitly allows `http://localhost:5173`. |
| **401 Unauthorized loops** | Expired session cookie | Clear browser cookies for `localhost` and log in again via `/login`. |
| **CSRF Token errors** | Missing CSRF token | The frontend client automatically extracts `XSRF-TOKEN` cookie from `GET /api/auth/me` and forwards it in `X-XSRF-TOKEN` header on all mutating requests. |


---

## 6. Environment Variables & Swappable Backend

CampusFlow is designed with a clean API separation layer. In development and standalone demo mode, it runs against an in-browser **Mock Service Worker (MSW)** engine. When the Java Spring Boot + PostgreSQL backend is ready, switch with a single environment variable:

```env
# .env or .env.local

# API Base URL (empty string for relative /api with MSW or Vite proxy)
VITE_API_BASE_URL=

# Set to "false" to connect to the real Java Spring Boot backend
VITE_USE_MOCKS=true
```

### How the Swappable Layer Works:
- `src/main.tsx` inspects `import.meta.env.VITE_USE_MOCKS !== 'false'`.
- If true, `setupWorker(...handlers).start({ onUnhandledRequest: 'bypass' })` intercepts browser network requests matching `/api/*`.
- When connecting to a deployed Spring Boot backend:
  1. Set `VITE_USE_MOCKS=false`.
  2. Set `VITE_API_BASE_URL=https://campusflow-api.jklu.edu.in/api`.
  3. The `apiClient` in `src/api/client.ts` automatically dispatches requests to the real backend with credentials, CSRF token propagation, and identical Zod response validation.

---

## 6. Application Routes

| Path | Description | Access Scope |
| :--- | :--- | :--- |
| `/login` | Authentication page with 1-click synthetic demo accounts | Public |
| `/` | **AI Agent Chat Home** (empty state greeting + composer + role suggestions) | Authenticated |
| `/c/:conversationId` | Active AI chat session with message history & streaming | Authenticated |
| `/overview` | Campus Overview dashboard (stat tiles, bookings activity) | Authenticated |
| `/rooms` | Free Classroom Finder with interval overlap engine & heatmap | Authenticated |
| `/venues` | Campus Venues & Specialised Labs directory | Authenticated |
| `/guest-house` | MDC Guest House room reservations | `STAFF`, `ADMIN` |
| `/parking` | Visitor parking slot allocation & passes | Authenticated |
| `/bookings` | User booking management & status tracking | Authenticated |
| `/bookings/new` | Unified 5-step event booking wizard | Authenticated |
| `/admin/approvals` | State-changing booking approval queue | `ADMIN` only |
| `/admin/timetable` | Semester timetable CSV/Excel/PDF upload & synchronization | `ADMIN` only |
| `/admin/master-data` | Master institutional resource registries | `ADMIN` only |
| `/admin/audit` | Tamper-evident compliance & AI reasoning audit log | `ADMIN` only |
| `/admin/reports` | Resource utilisation analytics & hourly heatmaps | `ADMIN` only |

*Note: Legacy paths `/dashboard`, `/finder`, `/guesthouse`, `/wizard`, and `/admin/master` automatically 301-redirect to their canonical routes.*

---

## 7. Demo Accounts & Role Permissions

The login page (`/login`) includes 1-click synthetic demo accounts (under `@demo.jklu.test`):

| Role | Demo Account | Permissions & Access Scope |
| :--- | :--- | :--- |
| **Student** | `student@demo.jklu.test` | Search classroom availability, request club event bookings, use AI Agent, view own bookings. |
| **Staff** | `staff@demo.jklu.test` | Student capabilities + book Amphitheatres, academic labs, and MDC Guest House rooms. |
| **Admin** | `admin@demo.jklu.test` | Full system access: Approval Queue, Timetable Drag-Drop Upload, Master Data Management, Audit Log, Analytics. |

*Security feature: 5 consecutive failed login attempts trigger an interactive 30-second lockout cooldown.*

---

## 8. Judge's Demo Script (3 Best Flows)

### Flow 1: Chat-First AI Multi-Resource Booking (Home -> Stream -> Conflict -> Recovery -> Proposal)
1. Log in as **Staff** (`staff@demo.jklu.test`).
2. You land directly on the **Chat Home (`/`)**. Notice the personalized greeting (*"Good afternoon, Dr. Neha"*), the centered composer, and 4 role-tailored prompt suggestion cards.
3. Click the suggestion card:  
   `"Plan a guest lecture: room + guest house"`.
4. Observe the interactive SSE stream in the conversation view (`/c/:id`):
   - **Agent Work Accordion**: Displays real-time sub-tasks (*"Analyze capacity requirements"*, *"Cross-check 8 semester timetables"*). When done, smoothly collapses into a compact *"Used 3 tools · 0.4s"* summary with a *"Full trace"* button.
   - **Simulated Tool Glitch & Automatic Recovery**: `queryTimetableDatabase` encounters a timeout on replica 2 and automatically retries with primary shard, completing successfully.
   - **Conflict Explanation Card**: The agent explains that *Room C-204 is occupied by Sem 3 Data Structures from 15:00 to 16:00*, and automatically presents free alternatives (*Room A-201* or *Room A-301*).
   - **Proposal Card**: The agent bundles **Room A-201**, **MDC-101 (Deluxe Room)**, and **Main Gate Visitor Parking (Zone P1)** into an all-in-one proposal.
   - Click **"Submit for Admin Approval"** -> instant submission with an auto-generated tracking ID.
5. Notice the new chat entry appeared in the left sidebar under *"Today"*. Hover over it to test **Rename** or **Delete**.
6. Click **"View trace"** in the bottom bar or accordion to slide open the **Reasoning Trace Drawer** displaying tool arguments, JSON outputs, and latency.

---

### Flow 2: Free Classroom Finder (Timetable Overlap Engine & Heatmap)
1. Click **"Find a Room"** in the sidebar (`/rooms`).
2. Select **Tomorrow's Date** and set time from **14:00 to 16:00**.
3. Move the capacity slider to **60+ seats**, select **Computing Block**, and check the **Projector** feature.
4. Review the search results:
   - Clashing rooms (like *C-204*) display clear red conflict callouts explaining the exact class or event causing the clash (*"Clash: B.Tech CSE Sem 3 Data Structures (15:00 - 16:00)"*).
   - Available rooms display green badges with one-click **"Book Room"** actions.
5. Click **"View Day Schedule"** on any room to toggle the **08:00 - 20:00 visual timeline heatmap**, showing scheduled class slots, existing reservations, and open windows at a glance.

---

### Flow 3: Admin Governance (Approval Queue, Rejection Validation & Timetable Sync)
1. Switch account to **Admin** (`admin@demo.jklu.test`) via the bottom-left profile menu.
2. The sidebar dynamically reveals the **Admin** group with an **Approvals** badge indicating pending requests.
3. Open the **Approval Queue** (`/admin/approvals`):
   - Review pending requests. Each card details the requester, resources requested, timetable clash check status (*"Clash check: Clean"*), and AI Agent recommendation.
   - Test rejection validation: Click **"Reject"**, enter fewer than 5 characters (e.g. *"No"*), and note client-side Zod validation blocking the submission (*"Rejection reason must be at least 5 characters"*). Enter *"Venue reserved for Academic Council"* to confirm rejection.
4. Navigate to **Timetable Upload** (`/admin/timetable`):
   - Drag and drop a sample timetable file (CSV/Excel).
   - Review the parsed preview table with per-row validation highlighting room or slot anomalies.
   - Click **"Approve & Publish Timetable"** to sync room schedules.
5. Check the **Audit Log** (`/admin/audit`):
   - View structured event logs (`AGENT_REASONING`, `TOOL_CALL`, `APPROVAL`, `REJECTION`, `LOGIN`).
   - Click any entry to inspect the full expandable JSON payload with actor details, timestamps, and request IDs.

---

## 9. Accessibility & WCAG AA Compliance

- **Contrast Ratios**: Verified via automated scripts. High-contrast dark text (`#111827`) is strictly enforced over orange (`#FF6B00`), achieving a **6.21:1** ratio. White text is restricted exclusively to deep surfaces (`#4A67B0` = 5.44:1, `#0069C9` = 5.43:1).
- **Reduced Motion**: All scroll animations respect `prefers-reduced-motion: reduce`.
- **Full Keyboard Navigation**: Skip links, visible focus rings, ARIA roles, and semantic HTML landmarks are present across all pages.
- Detailed audit and contrast matrix are documented in [`docs/SECURITY_CHECKLIST.md`](docs/SECURITY_CHECKLIST.md).

---

## 10. License & Attribution

Built for the **JKLU AI-Buildathon 1.0**. All campus components, logos, and layouts are original implementations inspired by the JKLU design language.

