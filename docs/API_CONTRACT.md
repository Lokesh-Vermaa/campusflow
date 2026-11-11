# JKLU CampusFlow — API Contract & Specification

**Target Backend**: Java Spring Boot 3.x + PostgreSQL 15+  
**Frontend Client**: React 18 + Vite + TypeScript (Strict)  
**Security Model**: Cookie-based HTTP-only session (`JSESSIONID`) + CSRF token header (`X-CSRF-TOKEN`).

---

## 1. Standard Error & Response Formats

### 1.1 Error Response (`application/json`)
HTTP Status: `400`, `401`, `403`, `404`, `409`, `422`, `500`
```json
{
  "code": "ROOM_UNAVAILABLE",
  "message": "Room C-204 is occupied by Sem 3 Data Structures from 15:00 to 16:00.",
  "requestId": "req-9f4a1c2e-4b68-45a2-8b9a-1c23456789ab"
}
```

### 1.2 Paginated List Response (`application/json`)
```json
{
  "items": [ /* array of resource items */ ],
  "pagination": {
    "page": 1,
    "pageSize": 10,
    "totalItems": 48,
    "totalPages": 5
  }
}
```

---

## 2. Authentication & Session Endpoints

### 2.1 Get Current Session
- **Method**: `GET`
- **Path**: `/api/auth/me`
- **Roles**: Public / Any (returns 401 if unauthenticated)
- **Response**: `200 OK`
```json
{
  "user": {
    "id": "u-00000000-0000-0000-0000-000000000001",
    "email": "student@demo.jklu.test",
    "name": "Aarav Sharma",
    "role": "STUDENT",
    "department": "Computer Science & Engineering",
    "avatarUrl": null
  }
}
```

### 2.2 Login
- **Method**: `POST`
- **Path**: `/api/auth/login`
- **Roles**: Public
- **Request Body**:
```json
{
  "email": "admin@demo.jklu.test",
  "password": "password123"
}
```
- **Response**: `200 OK` (Sets HTTP-only `JSESSIONID` cookie)
```json
{
  "user": {
    "id": "u-00000000-0000-0000-0000-000000000003",
    "email": "admin@demo.jklu.test",
    "name": "Rajesh Patel",
    "role": "ADMIN",
    "department": "Campus Administration",
    "avatarUrl": null
  }
}
```

### 2.3 Logout
- **Method**: `POST`
- **Path**: `/api/auth/logout`
- **Roles**: Authenticated
- **Response**: `200 OK`
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

### 2.4 CSRF Token
- **Method**: `GET`
- **Path**: `/api/auth/csrf`
- **Roles**: Public / Any
- **Response**: `200 OK`
```json
{
  "token": "csrf-tok-84729183-abc",
  "headerName": "X-CSRF-TOKEN"
}
```

---

## 3. Classrooms & Availability Finder

### 3.1 Search Free Classrooms
- **Method**: `GET`
- **Path**: `/api/rooms/availability`
- **Query Params**:
  - `date`: `YYYY-MM-DD` (Required, e.g. `2026-11-12`)
  - `startTime`: `HH:mm` (Required, e.g. `14:00`)
  - `endTime`: `HH:mm` (Required, e.g. `16:00`)
  - `minCapacity`: `integer` (Optional)
  - `blockId`: `string` (Optional)
  - `features`: `comma-separated strings` (Optional: `projector,ac,smartboard,mic`)
- **Roles**: `STUDENT`, `STAFF`, `ADMIN`
- **Response**: `200 OK`
```json
{
  "date": "2026-11-12",
  "requestedInterval": { "startTime": "14:00", "endTime": "16:00" },
  "availableRooms": [
    {
      "id": "r-00000000-0000-0000-0000-000000000001",
      "roomNumber": "A-101",
      "block": "Block A",
      "floor": 1,
      "capacity": 60,
      "type": "CLASSROOM",
      "features": ["projector", "ac", "smartboard"],
      "bookableOutside": true,
      "freeWindows": [
        { "startTime": "09:00", "endTime": "12:00" },
        { "startTime": "14:00", "endTime": "17:00" }
      ]
    }
  ],
  "unavailableRooms": [
    {
      "id": "r-00000000-0000-0000-0000-000000000008",
      "roomNumber": "C-204",
      "block": "Block C",
      "floor": 2,
      "capacity": 75,
      "type": "CLASSROOM",
      "features": ["projector", "ac"],
      "conflictReason": "Occupied by Sem 3 Data Structures from 15:00 to 16:00",
      "timeline": [
        { "slot": "09:00-11:00", "type": "CLASS", "title": "Database Systems" },
        { "slot": "15:00-16:00", "type": "CLASS", "title": "Data Structures" }
      ]
    }
  ]
}
```

### 3.2 Get Room Schedule / Heatmap
- **Method**: `GET`
- **Path**: `/api/rooms/{id}/schedule`
- **Query Params**: `date=YYYY-MM-DD`
- **Roles**: `STUDENT`, `STAFF`, `ADMIN`
- **Response**: `200 OK`
```json
{
  "roomId": "r-00000000-0000-0000-0000-000000000001",
  "roomNumber": "A-101",
  "date": "2026-11-12",
  "slots": [
    { "startTime": "09:00", "endTime": "10:00", "status": "FREE" },
    { "startTime": "10:00", "endTime": "11:00", "status": "BUSY", "title": "Engineering Physics", "category": "TIMETABLE" },
    { "startTime": "11:00", "endTime": "12:00", "status": "BUSY", "title": "AI Club Hackathon Prep", "category": "BOOKING" },
    { "startTime": "12:00", "endTime": "14:00", "status": "FREE" }
  ]
}
```

---

## 4. Venues (Amphitheatres & Labs)

### 4.1 List All Venues
- **Method**: `GET`
- **Path**: `/api/venues`
- **Roles**: `STUDENT`, `STAFF`, `ADMIN`
- **Response**: `200 OK`
```json
[
  {
    "id": "v-00000000-0000-0000-0000-000000000001",
    "name": "Rabindranath Tagore Amphitheatre",
    "type": "AMPHITHEATRE",
    "capacity": 300,
    "location": "Central Plaza",
    "isEligible": true,
    "eligibilityReason": null,
    "features": ["sound_system", "stage_lighting", "open_air", "green_room"],
    "requiresApproval": true
  },
  {
    "id": "v-00000000-0000-0000-0000-000000000003",
    "name": "Robotics & IoT Research Lab",
    "type": "LAB",
    "capacity": 30,
    "location": "Block B, 3rd Floor",
    "isEligible": false,
    "eligibilityReason": "Restricted to Semester 7/8 Capstone projects only",
    "features": ["3d_printers", "soldering_stations", "microcontrollers"],
    "requiresApproval": true
  }
]
```

---

## 5. MDC Guest House

### 5.1 List Guest House Rooms & Availability
- **Method**: `GET`
- **Path**: `/api/guesthouse/rooms`
- **Query Params**: `checkIn=YYYY-MM-DD`, `checkOut=YYYY-MM-DD`
- **Roles**: `STAFF`, `ADMIN`
- **Response**: `200 OK`
```json
[
  {
    "id": "gh-00000000-0000-0000-0000-000000000001",
    "roomNumber": "MDC-101",
    "roomType": "DELUXE",
    "bedType": "DOUBLE",
    "capacity": 2,
    "isAvailable": true,
    "amenities": ["ac", "wifi", "tv", "work_desk", "attached_bath"],
    "ratePerNight": 1800
  }
]
```

---

## 6. Parking Management

### 6.1 Get Parking Zones Status
- **Method**: `GET`
- **Path**: `/api/parking/zones`
- **Roles**: `STUDENT`, `STAFF`, `ADMIN`
- **Response**: `200 OK`
```json
[
  {
    "id": "pk-00000000-0000-0000-0000-000000000001",
    "name": "Zone P1 (Main Gate - Visitor Parking)",
    "code": "P1",
    "totalSlots": 30,
    "occupiedSlots": 18,
    "availableSlots": 12,
    "status": "AVAILABLE",
    "ratePerHour": 0
  }
]
```

### 6.2 Request Visitor Parking Pass
- **Method**: `POST`
- **Path**: `/api/parking/permits`
- **Roles**: `STUDENT`, `STAFF`, `ADMIN`
- **Request Body**:
```json
{
  "visitorName": "Prof. Amit Sengupta",
  "vehicleNumber": "RJ-14-CB-9021",
  "vehicleType": "FOUR_WHEELER",
  "zoneId": "pk-00000000-0000-0000-0000-000000000001",
  "visitDate": "2026-11-15",
  "startTime": "10:00",
  "endTime": "17:00",
  "purpose": "Guest Lecture on Distributed Systems"
}
```
- **Response**: `201 Created`
```json
{
  "permitId": "perm-00000000-0000-0000-0000-000000000001",
  "permitCode": "JKLU-PARK-2026-081",
  "status": "ALLOCATED",
  "allocatedSlot": "P1-14",
  "validUntil": "2026-11-15T17:00:00"
}
```

---

## 7. Bookings & Approvals

### 7.1 Create Booking (Unified Event Wizard & Forms)
- **Method**: `POST`
- **Path**: `/api/bookings`
- **Headers**: `Idempotency-Key: uuid`
- **Roles**: `STUDENT`, `STAFF`, `ADMIN`
- **Request Body**:
```json
{
  "title": "AI & Robotics Club Annual Hackathon",
  "description": "24-hour inter-college AI buildathon with 80 participants",
  "resourceType": "ROOM",
  "resourceId": "r-00000000-0000-0000-0000-000000000001",
  "bookingDate": "2026-11-20",
  "startTime": "09:00",
  "endTime": "18:00",
  "expectedAttendees": 75,
  "guestHouseRoomId": "gh-00000000-0000-0000-0000-000000000001",
  "guestHouseCheckIn": "2026-11-19",
  "guestHouseCheckOut": "2026-11-21",
  "parkingRequests": [
    {
      "visitorName": "Dr. R. Sharma",
      "vehicleNumber": "DL-3C-AZ-4521",
      "zoneId": "pk-00000000-0000-0000-0000-000000000001"
    }
  ]
}
```
- **Response**: `201 Created`
```json
{
  "id": "b-00000000-0000-0000-0000-000000000001",
  "bookingNumber": "JKLU-BK-2026-1042",
  "title": "AI & Robotics Club Annual Hackathon",
  "status": "PENDING",
  "requesterId": "u-00000000-0000-0000-0000-000000000001",
  "requesterName": "Aarav Sharma",
  "resourceName": "Room A-101",
  "bookingDate": "2026-11-20",
  "startTime": "09:00",
  "endTime": "18:00",
  "createdAt": "2026-11-11T10:30:00Z"
}
```

### 7.2 List User Bookings ("My Bookings")
- **Method**: `GET`
- **Path**: `/api/bookings/my`
- **Query Params**: `status=PENDING|APPROVED|REJECTED|CANCELLED`, `page=1`, `pageSize=10`
- **Roles**: `STUDENT`, `STAFF`, `ADMIN`
- **Response**: `200 OK` (Paginated)

### 7.3 Cancel Booking
- **Method**: `POST`
- **Path**: `/api/bookings/{id}/cancel`
- **Roles**: Owner or `ADMIN`
- **Response**: `200 OK`

### 7.4 Admin Approval Queue
- **Method**: `GET`
- **Path**: `/api/admin/approvals`
- **Query Params**: `page=1`, `pageSize=10`
- **Roles**: `ADMIN`
- **Response**: `200 OK` (Includes conflict verification result and agent reasoning summary)

### 7.5 Approve Booking
- **Method**: `POST`
- **Path**: `/api/admin/approvals/{id}/approve`
- **Roles**: `ADMIN`
- **Request Body**: `{ "note": "Approved after checking schedule" }`
- **Response**: `200 OK`

### 7.6 Reject Booking
- **Method**: `POST`
- **Path**: `/api/admin/approvals/{id}/reject`
- **Roles**: `ADMIN`
- **Request Body**: `{ "reason": "VIP delegation scheduled in adjacent hall" }`
- **Response**: `200 OK`

---

## 8. Admin Master Data & Timetable

### 8.1 Timetable Upload & Validate
- **Method**: `POST`
- **Path**: `/api/admin/timetable/upload`
- **Multipart Form**: `file` (CSV / XLSX / PDF, max 10MB)
- **Roles**: `ADMIN`
- **Response**: `200 OK`
```json
{
  "totalRows": 142,
  "validRows": 138,
  "errorRows": [
    {
      "rowNumber": 14,
      "course": "CS302",
      "room": "Z-999",
      "error": "Room Z-999 does not exist in master data"
    }
  ],
  "preview": [ /* parsed rows */ ]
}
```

### 8.2 Timetable Publish
- **Method**: `POST`
- **Path**: `/api/admin/timetable/publish`
- **Roles**: `ADMIN`
- **Request Body**: `{ "semester": "Spring 2027", "entries": [ /* confirmed rows */ ] }`
- **Response**: `200 OK`

### 8.3 Master Data CRUD
- **Path**: `/api/admin/master/{resource}` (where resource is `blocks`, `rooms`, `guesthouse`, `parking`)
- **Methods**: `GET`, `POST`, `PUT`, `DELETE`
- **Roles**: `ADMIN`

---

## 9. Audit Log & Utilisation Reports

### 9.1 Audit Log Search
- **Method**: `GET`
- **Path**: `/api/admin/audit`
- **Query Params**: `actor`, `eventType`, `startDate`, `endDate`, `page`, `pageSize`
- **Event Types**: `AGENT_REASONING`, `TOOL_CALL`, `API_CALL`, `APPROVAL`, `REJECTION`, `LOGIN`, `DATA_CHANGE`
- **Roles**: `ADMIN`
- **Response**: `200 OK` (Paginated)

### 9.2 Utilisation Analytics
- **Method**: `GET`
- **Path**: `/api/admin/reports/utilisation`
- **Query Params**: `startDate`, `endDate`
- **Roles**: `ADMIN`
- **Response**: `200 OK`
```json
{
  "byHour": [
    { "hour": "09:00", "occupancyRate": 65 },
    { "hour": "10:00", "occupancyRate": 88 },
    { "hour": "14:00", "occupancyRate": 92 }
  ],
  "topRooms": [
    { "roomNumber": "A-101", "bookingCount": 42, "utilPct": 85 }
  ],
  "leastRooms": [
    { "roomNumber": "B-208", "bookingCount": 4, "utilPct": 12 }
  ],
  "guestHouseOccupancyPct": 78,
  "parkingPeakOccupancyPct": 90
}
```

---

## 10. AI Agent SSE Streaming

### 10.1 Chat & Multi-Resource Planning Stream
- **Method**: `POST`
- **Path**: `/api/agent/chat`
- **Accept**: `text/event-stream`
- **Request Body**:
```json
{
  "message": "Guest lecture next Thursday 3-5 PM, 80 people, speaker from Delhi needing stay and parking",
  "conversationId": "conv-00000000-0000-0000-0000-000000000001"
}
```
- **SSE Events**:
  - `event: thinking` -> `{"content": "Checking schedule for 80+ capacity rooms on Thursday 15:00-17:00..."}`
  - `event: plan` -> `{"steps": [{"id": 1, "title": "Check auditorium and large rooms", "status": "running"}]}`
  - `event: tool_call` -> `{"tool": "checkAvailability", "args": {...}, "status": "retry|success|failed", "durationMs": 140}`
  - `event: conflict` -> `{"resource": "Room C-204", "clashWith": "Sem 3 Data Structures 15:00-16:00"}`
  - `event: alternatives` -> `{"suggested": ["Room A-101 (cap 90, free)", "Tagore Amphitheatre"]}`
  - `event: proposal` -> `{"proposal": { "room": "A-101", "guestHouse": "MDC-101", "parking": "Zone P1", "requiresApproval": true }}`
  - `event: done` -> `{"content": "Full plan prepared and bundled. Click 'Submit for Approval' to send to admin."}`

---

## 11. Public Content
### 11.1 Announcement Banner
- **Method**: `GET`
- **Path**: `/api/content/announcement`
- **Roles**: Public
- **Response**: `200 OK`
```json
{
  "id": "ann-1",
  "message": "Admissions peak week: book venues early",
  "linkText": "See booking rules",
  "linkHref": "/rules",
  "active": true
}
```

