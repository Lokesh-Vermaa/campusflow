# CampusFlow — API Contract vs Implementation (Phase 5)

Every endpoint in `frontend/docs/API_CONTRACT.md` mapped against the implemented backend controller.

## Legend
- ✅ MATCH — path, method, roles, and shape all match the contract
- ⚠️ MINOR — implemented but minor shape deviation (noted inline)
- ❌ NOT IMPLEMENTED — planned for a later phase

---

## Section 2: Authentication

| Contract | Method | Path | Backend Controller | Status |
|----------|--------|------|-------------------|--------|
| 2.1 Get Current Session | GET | `/api/auth/me` | `AuthController.me()` | ✅ MATCH |
| 2.2 Login | POST | `/api/auth/login` | `AuthController.login()` | ✅ MATCH (cookie, no token in body) |
| 2.3 Logout | POST | `/api/auth/logout` | `AuthController.logout()` | ✅ MATCH |
| 2.4 CSRF Token | GET | `/api/auth/csrf` | `AuthController.csrf()` | ✅ MATCH |

---

## Section 3: Classrooms & Availability

| Contract | Method | Path | Backend Controller | Status |
|----------|--------|------|-------------------|--------|
| 3.1 Search Free Classrooms | GET | `/api/rooms/availability` | `RoomAvailabilityController.getAvailability()` | ✅ MATCH |
| 3.2 Get Room Schedule | GET | `/api/rooms/{id}/schedule` | `RoomAvailabilityController.getRoomSchedule()` | ✅ MATCH |

---

## Section 4: Venues

| Contract | Method | Path | Backend Controller | Status |
|----------|--------|------|-------------------|--------|
| 4.1 List All Venues | GET | `/api/venues` | `VenueController.listVenues()` | ✅ MATCH |

---

## Section 5: MDC Guest House

| Contract | Method | Path | Backend Controller | Status |
|----------|--------|------|-------------------|--------|
| 5.1 List Guest House Rooms | GET | `/api/guesthouse/rooms` | `GuestHouseController.listRooms()` | ✅ MATCH |

> **Response shape note**: Contract has `roomType` / `bedType` / `amenities` / `ratePerNight` / `isAvailable`. Backend `GuestRoomResponse` has `type` / `bedType` / `features` / `pricePerNight` / `available`. Field names are slightly different from the contract.
> **Decision**: Backend field names are acceptable for the demo — a frontend adapter maps them. This is a known minor mismatch to align in a future cleanup sprint (requires frontend change).

---

## Section 6: Parking

| Contract | Method | Path | Backend Controller | Status |
|----------|--------|------|-------------------|--------|
| 6.1 Get Parking Zones Status | GET | `/api/parking/zones` | `ParkingController.listZones()` | ✅ MATCH |
| 6.2 Request Visitor Parking Pass | POST | `/api/parking/permits` | `ParkingController.requestPermit()` | ✅ MATCH |

---

## Section 7: Bookings & Approvals

| Contract | Method | Path | Backend Controller | Status |
|----------|--------|------|-------------------|--------|
| 7.1 Create Booking | POST | `/api/bookings` | `BookingController.createBooking()` | ✅ MATCH (supports `Idempotency-Key`) |
| 7.2 List My Bookings | GET | `/api/bookings/my` | `BookingController.getMyBookings()` | ✅ MATCH |
| 7.3 Cancel Booking | POST | `/api/bookings/{id}/cancel` | `BookingController.cancelBooking()` | ✅ MATCH (IDOR returns 404) |
| 7.4 Admin Approval Queue | GET | `/api/admin/approvals` | `AdminApprovalController.getPendingApprovals()` | ✅ MATCH |
| 7.5 Approve Booking | POST | `/api/admin/approvals/{id}/approve` | `AdminApprovalController.approveBooking()` | ✅ MATCH |
| 7.6 Reject Booking | POST | `/api/admin/approvals/{id}/reject` | `AdminApprovalController.rejectBooking()` | ✅ MATCH |

> **Booking response shape**: Contract field `status: "PENDING"` — backend returns `"PENDING"` (maps `PENDING_APPROVAL` → `PENDING` in `toResponse()`). ✅

---

## Section 8: Admin Master Data & Timetable

| Contract | Method | Path | Backend Controller | Status |
|----------|--------|------|-------------------|--------|
| 8.1 Timetable Upload | POST | `/api/admin/timetable/upload` | `TimetableAdminController.uploadTimetable()` | ✅ MATCH |
| 8.2 Timetable Publish | POST | `/api/admin/timetable/publish` | `TimetableAdminController.publishTimetable()` | ✅ MATCH |
| 8.3 Master Data CRUD — rooms | GET | `/api/admin/master/rooms` | `MasterDataController.listRooms()` | ✅ MATCH |
| 8.3 Master Data CRUD — rooms | POST | `/api/admin/master/rooms` | `MasterDataController.createRoom()` | ✅ MATCH |
| 8.3 Master Data CRUD — blocks | GET | `/api/admin/master/blocks` | `MasterDataController.listBlocks()` | ✅ MATCH |
| 8.3 Master Data CRUD — guesthouse | GET | `/api/admin/master/guesthouse` | `MasterDataController.listGuestRooms()` | ✅ MATCH |
| 8.3 Master Data CRUD — parking | GET | `/api/admin/master/parking` | `MasterDataController.listParkingZones()` | ✅ MATCH |

---

## Section 9: Audit Log & Utilisation

| Contract | Method | Path | Backend Controller | Status |
|----------|--------|------|-------------------|--------|
| 9.1 Audit Log Search | GET | `/api/admin/audit` | `AuditController.searchAuditLogs()` | ✅ MATCH |
| 9.1 Audit Log Export | GET | `/api/admin/audit/export` | `AuditController.exportAuditCsv()` | ✅ MATCH |
| 9.2 Utilisation Analytics | GET | `/api/admin/reports/utilisation` | `ReportsController.getUtilisationReport()` | ✅ MATCH (Phase 5) |

---

## Section 10: AI Agent SSE

| Contract | Method | Path | Backend Controller | Status |
|----------|--------|------|-------------------|--------|
| 10.1 Chat SSE | POST | `/api/agent/chat` | `AgentController.chat()` | ✅ MATCH (Phase 4) |

**SSE event types** (contract §10.1 vs implemented):
| Contract Event | Implemented | Notes |
|---------------|------------|-------|
| `thinking` | ✅ | ScriptedPlanner emits `event:thinking` |
| `plan` | ✅ | ScriptedPlanner emits `event:plan` |
| `tool_call` | ✅ | ScriptedPlanner emits `event:tool_call` |
| `conflict` | ✅ | ScriptedPlanner emits `event:conflict` |
| `alternatives` | ✅ | ScriptedPlanner emits `event:alternatives` |
| `proposal` | ✅ | ScriptedPlanner emits `event:proposal` |
| `done` | ✅ | ScriptedPlanner emits `event:done` |

---

## Section 11: Public Content

| Contract | Method | Path | Backend Controller | Status |
|----------|--------|------|-------------------|--------|
| 11.1 Announcement Banner | GET | `/api/content/announcement` | `ContentController.getAnnouncement()` | ✅ MATCH (Phase 5, public) |

---

## Section 12: Conversations

| Contract | Method | Path | Backend Controller | Status |
|----------|--------|------|-------------------|--------|
| 12.1 List Conversations | GET | `/api/conversations` | `ConversationController.list()` | ✅ MATCH (Phase 4) |
| 12.2 Create Conversation | POST | `/api/conversations` | `ConversationController.create()` | ✅ MATCH (Phase 4) |
| 12.3 Get Conversation | GET | `/api/conversations/{id}` | `ConversationController.get()` | ✅ MATCH (Phase 4, IDOR = 404) |
| 12.4 Rename Conversation | PATCH | `/api/conversations/{id}` | `ConversationController.rename()` | ✅ MATCH (Phase 4) |
| 12.5 Delete Conversation | DELETE | `/api/conversations/{id}` | `ConversationController.delete()` | ✅ MATCH (Phase 4, 204) |

---

## Mismatches Summary

| # | Endpoint | Mismatch | Decision |
|---|---------|---------|---------|
| 1 | `GET /api/guesthouse/rooms` | Field names differ: `type` vs `roomType`, `features` vs `amenities`, `pricePerNight` vs `ratePerNight` | **Backend wins** — frontend mock already adapted. Align in Phase 5 cleanup (needs frontend change — awaiting user approval) |
> **No path mismatches** — all implemented endpoints match the contract exactly.
> The availability path is `/api/rooms/availability` on both sides ✅
> §9.2 Utilisation and §11.1 Announcement now implemented (Phase 5) ✅
