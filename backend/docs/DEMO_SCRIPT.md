# CampusFlow Demo Script

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

## Run Commands

```bash
# dev profile (datasource from .env)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# demo profile (datasource + demo seeder)
SPRING_PROFILES_ACTIVE=dev,demo ./mvnw spring-boot:run

# test profile (used by mvnw test, no demo seeder by default)
SPRING_PROFILES_ACTIVE=test ./mvnw test
```
