# CampusFlow Demo Script & Presentation Guide

## Before the Presentation (Pre-Flight Checklist)

Run through these 6 checks 15 minutes before presenting to the jury:

- [ ] **Reset Demo Database**: Run `powershell -File scripts/reset-demo.ps1` to ensure fresh, clean sample data (25 rooms, 163 timetable slots, 12 guest rooms, 3 parking zones, and no lingering dirty bookings).
- [ ] **Start Services in Correct Order**:
  1. **PostgreSQL 16**: Running on `localhost:5432`.
  2. **Spring Boot Backend**:
     ```powershell
     cd backend
     $env:SPRING_PROFILES_ACTIVE="dev,demo"
     .\mvnw.cmd spring-boot:run
     ```
     Verify console shows: `Tomcat started on port 8080 (http)` and `Demo users synced (demo profile)`.
  3. **Vite Frontend**:
     ```powershell
     cd frontend
     npm run dev
     ```
- [ ] **Prepare Browser Tabs**:
  - **Tab 1 (Staff Persona)**: [http://localhost:5173/login](http://localhost:5173/login) (ready to click `staff@demo.jklu.test`).
  - **Tab 2 (Incognito / Admin Persona)**: [http://localhost:5173/login](http://localhost:5173/login) (ready to click `admin@demo.jklu.test`).
  - **Tab 3 (Swagger / API Docs if asked)**: [http://localhost:8080/api/content/announcement](http://localhost:8080/api/content/announcement).
- [ ] **Backup Offline Video / Screen Recording**: Keep a local 2-minute MP4 screen recording of the guest lecture flow in `docs/screenshots/` in case the display projector or laptop experiences unexpected hiccups.
- [ ] **What to Say if External Network / WiFi is Down**:
  > *"CampusFlow is deliberately architected with an offline-first deterministic planning engine. The AI agent, availability engine, timetable conflict detection, guest house reservation, and parking allocation run 100% locally on real campus database algorithms without requiring an external internet connection or cloud LLM API."*
- [ ] **Ensure Theme Consistency**: Toggle the theme switch (top-right) to either Dark or Light mode according to presentation room lighting.

---

## Guaranteed Demo Facts

| Fact | Detail |
|------|--------|
| Room C-204 Timetable Conflict | Room ID `00000000-0000-0000-0000-000000000064`, Thursday (day_of_week=4), 15:00–16:00 (minutes 900–960), course `CS201` "Sem 3 Data Structures", Slot ID `00000000-0000-0000-0000-000000000999` |
| Free Amphitheatre Thursday Evening | M-201 Management Amphitheatre (ID `00000000-0000-0000-0000-000000000030`, capacity 150) has NO timetable slot on Thursday — always free for evening bookings |
| Restricted Lab | T-102 Networks Research Lab (ID `00000000-0000-0000-0000-000000000026`) has `bookable_outside = FALSE` — not bookable via the booking form |
| Guest House Suites | G-301 (ID `00000000-0000-0000-0000-000000000075`) and G-302 (ID `00000000-0000-0000-0000-000000000076`) are SUITE type |
| Restricted Rooms (not bookable outside) | A-301 Faculty Board Room (ID `00000000-0000-0000-0000-000000000055`), M-301 Dean Conference Room (ID `00000000-0000-0000-0000-000000000065`) |

## Demo Users

| Role | Email | Display Name | Department |
|------|-------|-------------|-----------|
| STUDENT | student@demo.jklu.test | Aarav Sharma | Computer Science |
| STAFF | staff@demo.jklu.test | Dr. Neha Verma | Electronics & Communication |
| ADMIN | admin@demo.jklu.test | Rajesh Patel | Estate & Campus Operations |

Password for all demo users: set in `DEMO_USER_PASSWORD` env var (placeholder: `change-me-demo-password`)

## Demo Flow: Guest Lecture Booking

**Scenario**: "Guest lecture next Thursday 3-5 PM, 80 people, speaker from Delhi"

1. **Availability check** (`GET /api/rooms/availability?date=<next-thursday>&startTime=15:00&endTime=17:00&minCapacity=80`)
   - C-204 appears in `unavailableRooms` — conflict with CS201 Sem 3 Data Structures 15:00–16:00
   - M-201 Management Amphitheatre (cap 150) appears in `availableRooms` ✅
   - A-201 Main Auditorium (cap 120) appears in `availableRooms` ✅

2. **Speaker accommodation** (`GET /api/guesthouse/rooms`) — G-301/G-302 Suites available

3. **Parking** (`GET /api/parking/zones`) — P-VISITOR zone available

4. **Bundle booking** (`POST /api/bookings`) — room + guest house + parking in one request

## Parking Zones

| Zone | Code | Total Slots | ID |
|------|------|------------|-----|
| Faculty & Staff | P-STAFF | 60 | `00000000-0000-0000-0000-000000000041` |
| Visitor Parking | P-VISITOR | 40 | `00000000-0000-0000-0000-000000000042` |
| Student & General | P-STUDENT | 50 | `00000000-0000-0000-0000-000000000043` |

## Phase 5 End-to-End Acceptance Flow (Automated & Tested)

The complete lifecycle is verified in `EndToEndAcceptanceFlowIntegrationTest.java`:

### Step 1: Staff Prompts the AI Agent
* **User**: `staff@demo.jklu.test` (Staff)
* **Endpoint**: `POST /api/agent/chat` (or `POST /api/conversations/{id}/messages`)
* **Prompt**: `"Guest lecture next Thursday 3-5 PM, 80 people, speaker from Delhi needing stay and parking"`
* **Events Streamed (SSE)**:
  1. `event:plan`: Dynamic multi-step plan identifying candidate venues, checking timetables, querying guest rooms, visitor parking, and campus policy.
  2. `event:thinking`: Real-time reasoning about capacity and scheduling constraints.
  3. `event:tool_call`: `find_free_rooms(minCapacity: 80, role: STAFF)` returning candidate venues (C-204, A-201, M-201).
  4. `event:tool_call`: `check_room_availability(room: C-204)` showing replica retry simulation and confirmed clash with `CS201 Sem 3 Data Structures 15:00-16:00`.
  5. `event:conflict`: Detailed conflict card explaining that Room C-204 is occupied by an academic lecture.
  6. `event:alternatives`: Recommends free alternatives `A-201 (Main Auditorium, cap 120)` and `M-201 (Management Amphitheatre, cap 150)`.
  7. `event:tool_call`: `reserveGuestHouseRoom` dynamically checks database and allocates free guest room `G-102`.
  8. `event:tool_call`: `generateVisitorParkingPermit` queries parking zones and assigns `P-STAFF` / `P-VISITOR`.
  9. `event:tool_call`: `search_booking_policy` retrieves approval policies from `policies/*.md`.
  10. `event:proposal`: Emits the structured `proposal` card containing `preFilledPayload` for a multi-resource bundle (Room + Guest House + Parking).
  11. `event:done`: Concluding summary advising human submitter to review and submit for approval.

### Step 2: Staff Submits Proposal for Approval
* **Endpoint**: `POST /api/bookings` with `X-Idempotency-Key: <uuid>`
* **Payload**:
  ```json
  {
    "title": "Campus Reservation — 80 Attendees",
    "description": "Automated plan: Guest lecture next Thursday 3-5 PM...",
    "resourceType": "ROOM",
    "resourceId": "00000000-0000-0000-0000-000000000023",
    "bookingDate": "2026-11-19",
    "startTime": "15:00",
    "endTime": "17:00",
    "expectedAttendees": 80,
    "guestHouseRoomId": "00000000-0000-0000-0000-000000000032",
    "guestHouseCheckIn": "2026-11-19",
    "guestHouseCheckOut": "2026-11-20",
    "parkingRequests": [
      {
        "visitorName": "Keynote Speaker",
        "vehicleNumber": "DL-01-AB-1234",
        "zoneId": "00000000-0000-0000-0000-000000000041"
      }
    ]
  }
  ```
* **Result**: `201 CREATED` with `status: "PENDING"` (`PENDING_APPROVAL` in database).

### Step 3: Admin Reviews & Approves in Queue
* **User**: `admin@demo.jklu.test` (Admin)
* **Endpoint**: `GET /api/admin/approvals?page=1&pageSize=10`
* **Result**: Pending reservation bundle is visible in queue.
* **Approve Action**: `POST /api/admin/approvals/{id}/approve`
  ```json
  {
    "note": "Approved by Campus Operations for visiting keynote guest."
  }
  ```
* **Result**: `200 OK` with `status: "APPROVED"` (`CONFIRMED` in database).

### Step 4: Staff Confirms Approval
* **Endpoint**: `GET /api/bookings/my`
* **Result**: Booking status is updated to `"APPROVED"`.

### Step 5: Availability Engine Reflects Occupancy
* **Endpoint**: `GET /api/rooms/availability?date=2026-11-19&startTime=15:00&endTime=17:00`
* **Result**: Room A-201 is now listed in `unavailableRooms` and no longer in `availableRooms`.

### Step 6: Audit Log Integrity Verification
* **Endpoint**: `GET /api/admin/audit?pageSize=50`
* **Result**:
  - `APPROVAL` row recorded for booking entity.
  - `AGENT_REASONING` entries visible with summary explanations.
  - `TOOL_CALL` entries visible with tool names, parameters, and durations.

---

## Run Commands

```bash
# dev profile (datasource from .env)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# demo profile (datasource + demo seeder)
SPRING_PROFILES_ACTIVE=dev,demo ./mvnw spring-boot:run

# test profile (used by mvnw test, no demo seeder by default)
SPRING_PROFILES_ACTIVE=test ./mvnw test
```
