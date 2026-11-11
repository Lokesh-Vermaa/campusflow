import { http, HttpResponse, delay } from 'msw';
import {
  SEED_USERS,
  SEED_ROOMS,
  SEED_VENUES,
  SEED_GUESTHOUSE_ROOMS,
  SEED_PARKING_ZONES,
  SEED_BOOKINGS,
  SEED_AUDIT_LOGS,
  SEED_ANNOUNCEMENT,
  SEED_TIMETABLE,
} from './seedData';
import { computeAvailability } from './availabilityEngine';
import { Booking, BookingStatus, User } from '@/api/types';

// ==========================================
// In-Memory Mutable State
// ==========================================
let currentUser: User | null = SEED_USERS.student; // Default active session for demo convenience
const bookingsDb: Booking[] = [...SEED_BOOKINGS];
const auditLogsDb = [...SEED_AUDIT_LOGS];
const roomsDb = [...SEED_ROOMS];
const guestHouseDb = [...SEED_GUESTHOUSE_ROOMS];
const parkingDb = [...SEED_PARKING_ZONES];
const announcementDb = { ...SEED_ANNOUNCEMENT };

// Track idempotency keys to prevent duplicate booking creation
const seenIdempotencyKeys = new Map<string, Booking>();

export const handlers = [
  // ------------------------------------------
  // 1. Auth & Session
  // ------------------------------------------
  http.get('/api/auth/me', async () => {
    await delay(100);
    if (!currentUser) {
      return HttpResponse.json(
        { code: 'UNAUTHORIZED', message: 'Not logged in', requestId: crypto.randomUUID() },
        { status: 401 },
      );
    }
    return HttpResponse.json({ user: currentUser });
  }),

  http.post('/api/auth/login', async ({ request }) => {
    await delay(250);
    const body = (await request.json()) as { email?: string; password?: string };
    const email = body.email?.toLowerCase().trim();

    const matchedUser = Object.values(SEED_USERS).find((u) => u.email.toLowerCase() === email);

    if (!matchedUser) {
      return HttpResponse.json(
        {
          code: 'INVALID_CREDENTIALS',
          message: 'Invalid email or password. Use demo accounts provided below.',
          requestId: crypto.randomUUID(),
        },
        { status: 401 },
      );
    }

    currentUser = matchedUser;

    // Log login audit
    auditLogsDb.unshift({
      id: crypto.randomUUID(),
      timestamp: new Date().toISOString(),
      actor: currentUser.name,
      actorRole: currentUser.role,
      eventType: 'LOGIN',
      action: `User signed in with role ${currentUser.role}`,
      details: { email: currentUser.email },
      ipAddress: '127.0.0.1',
    });

    return HttpResponse.json({ user: currentUser });
  }),

  http.post('/api/auth/logout', async () => {
    await delay(150);
    currentUser = null;
    return HttpResponse.json({ success: true, message: 'Logged out successfully' });
  }),

  http.get('/api/auth/csrf', async () => {
    return HttpResponse.json({
      token: `csrf-${crypto.randomUUID()}`,
      headerName: 'X-CSRF-TOKEN',
    });
  }),

  // ------------------------------------------
  // 2. Announcement Stripe
  // ------------------------------------------
  http.get('/api/content/announcement', async () => {
    return HttpResponse.json(announcementDb);
  }),

  // ------------------------------------------
  // 3. Classrooms & Availability
  // ------------------------------------------
  http.get('/api/rooms/availability', async ({ request }) => {
    await delay(200);
    const url = new URL(request.url);
    const date = url.searchParams.get('date') || new Date().toISOString().split('T')[0];
    const startTime = url.searchParams.get('startTime') || '14:00';
    const endTime = url.searchParams.get('endTime') || '16:00';
    const minCapacity = Number(url.searchParams.get('minCapacity') || 0);
    const blockId = url.searchParams.get('blockId') || undefined;
    const featuresParam = url.searchParams.get('features');
    const features = featuresParam ? featuresParam.split(',') : [];

    const { availableRooms, unavailableRooms } = computeAvailability({
      date,
      startTime,
      endTime,
      minCapacity,
      blockId,
      features,
    });

    return HttpResponse.json({
      date,
      requestedInterval: { startTime, endTime },
      availableRooms,
      unavailableRooms,
    });
  }),

  http.get('/api/rooms/:id/schedule', async ({ params, request }) => {
    await delay(150);
    const roomId = params.id as string;
    const url = new URL(request.url);
    const date = url.searchParams.get('date') || new Date().toISOString().split('T')[0];

    const room = roomsDb.find((r) => r.id === roomId) || roomsDb[0];

    // Compute slots
    const dayOfWeek = new Date(date).getDay() || 7;
    const timetableClashes = SEED_TIMETABLE.filter((s) => s.roomId === room.id && s.dayOfWeek === dayOfWeek);
    const bookingClashes = bookingsDb.filter(
      (b) => b.resourceId === room.id && b.bookingDate === date && b.status === 'APPROVED',
    );

    const hours = ['09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00'];
    const slots = hours.map((hour, idx) => {
      const nextHour = hours[idx + 1] || '19:00';
      const tc = timetableClashes.find((t) => t.startTime <= hour && t.endTime >= nextHour);
      const bc = bookingClashes.find((b) => b.startTime <= hour && b.endTime >= nextHour);

      if (tc) {
        return {
          startTime: hour,
          endTime: nextHour,
          status: 'BUSY' as const,
          title: `${tc.courseCode}: ${tc.courseName}`,
          category: 'TIMETABLE' as const,
        };
      }
      if (bc) {
        return {
          startTime: hour,
          endTime: nextHour,
          status: 'BUSY' as const,
          title: bc.title,
          category: 'BOOKING' as const,
        };
      }
      return {
        startTime: hour,
        endTime: nextHour,
        status: 'FREE' as const,
      };
    });

    return HttpResponse.json({
      roomId: room.id,
      roomNumber: room.roomNumber,
      date,
      slots,
    });
  }),

  // ------------------------------------------
  // 4. Venues (Amphitheatres & Labs)
  // ------------------------------------------
  http.get('/api/venues', async () => {
    await delay(150);
    return HttpResponse.json(SEED_VENUES);
  }),

  // ------------------------------------------
  // 5. MDC Guest House
  // ------------------------------------------
  http.get('/api/guesthouse/rooms', async () => {
    await delay(150);
    return HttpResponse.json(guestHouseDb);
  }),

  // ------------------------------------------
  // 6. Parking
  // ------------------------------------------
  http.get('/api/parking/zones', async () => {
    await delay(120);
    return HttpResponse.json(parkingDb);
  }),

  http.post('/api/parking/permits', async ({ request }) => {
    await delay(200);
    const body = (await request.json()) as {
      visitorName: string;
      vehicleNumber: string;
      zoneId: string;
      visitDate: string;
      startTime: string;
      endTime: string;
      purpose: string;
    };

    const zone = parkingDb.find((p) => p.id === body.zoneId) || parkingDb[0];

    // Decrement available slots in mock
    if (zone.availableSlots > 0) {
      zone.availableSlots -= 1;
      zone.occupiedSlots += 1;
    }

    return HttpResponse.json(
      {
        permitId: crypto.randomUUID(),
        permitCode: `JKLU-PRK-${Math.floor(1000 + Math.random() * 9000)}`,
        status: 'ALLOCATED',
        allocatedSlot: `${zone.code}-${Math.floor(10 + Math.random() * 25)}`,
        validUntil: `${body.visitDate}T${body.endTime}:00`,
      },
      { status: 201 },
    );
  }),

  // ------------------------------------------
  // 7. Bookings ("My Bookings" & Creation)
  // ------------------------------------------
  http.post('/api/bookings', async ({ request }) => {
    await delay(250);
    const idempotencyKey = request.headers.get('Idempotency-Key');

    if (idempotencyKey && seenIdempotencyKeys.has(idempotencyKey)) {
      // Return idempotently cached response
      return HttpResponse.json(seenIdempotencyKeys.get(idempotencyKey)!, { status: 200 });
    }

    const body = (await request.json()) as {
      title: string;
      description?: string;
      resourceType: 'ROOM' | 'VENUE' | 'GUEST_HOUSE' | 'MULTI_RESOURCE';
      resourceId: string;
      bookingDate: string;
      startTime: string;
      endTime: string;
      expectedAttendees: number;
      guestHouseRoomId?: string | null;
      guestHouseCheckIn?: string | null;
      guestHouseCheckOut?: string | null;
      parkingZoneId?: string | null;
    };

    // Find resource name
    let resourceName = 'Requested Resource';
    const foundRoom = roomsDb.find((r) => r.id === body.resourceId);
    if (foundRoom) resourceName = `Room ${foundRoom.roomNumber} (${foundRoom.block})`;
    const foundVenue = SEED_VENUES.find((v) => v.id === body.resourceId);
    if (foundVenue) resourceName = foundVenue.name;

    const newBooking: Booking = {
      id: crypto.randomUUID(),
      bookingNumber: `JKLU-BK-2026-${Math.floor(2000 + Math.random() * 8000)}`,
      title: body.title,
      description: body.description,
      status: 'PENDING',
      requesterId: currentUser?.id || SEED_USERS.student.id,
      requesterName: currentUser?.name || 'Aarav Sharma',
      requesterEmail: currentUser?.email || 'student@demo.jklu.test',
      requesterRole: currentUser?.role || 'STUDENT',
      resourceType: body.resourceType,
      resourceId: body.resourceId,
      resourceName,
      bookingDate: body.bookingDate,
      startTime: body.startTime,
      endTime: body.endTime,
      expectedAttendees: body.expectedAttendees,
      guestHouseRoomId: body.guestHouseRoomId,
      guestHouseRoomName: body.guestHouseRoomId ? 'MDC-101 (Deluxe)' : undefined,
      guestHouseCheckIn: body.guestHouseCheckIn,
      guestHouseCheckOut: body.guestHouseCheckOut,
      parkingRequested: Boolean(body.parkingZoneId),
      parkingZoneName: body.parkingZoneId ? 'Zone P1 (Main Gate)' : undefined,
      createdAt: new Date().toISOString(),
      conflictCheckResult: {
        hasClash: false,
        summary: 'Zero immediate timetable conflicts detected.',
      },
    };

    bookingsDb.unshift(newBooking);

    if (idempotencyKey) {
      seenIdempotencyKeys.set(idempotencyKey, newBooking);
    }

    // Add audit log
    auditLogsDb.unshift({
      id: crypto.randomUUID(),
      timestamp: new Date().toISOString(),
      actor: currentUser?.name || 'Student',
      actorRole: currentUser?.role || 'STUDENT',
      eventType: 'API_CALL',
      action: `Created booking request ${newBooking.bookingNumber}`,
      details: { title: newBooking.title, resource: newBooking.resourceName },
    });

    return HttpResponse.json(newBooking, { status: 201 });
  }),

  http.get('/api/bookings/my', async ({ request }) => {
    await delay(150);
    const url = new URL(request.url);
    const status = url.searchParams.get('status') as BookingStatus | null;
    const page = Number(url.searchParams.get('page') || 1);
    const pageSize = Number(url.searchParams.get('pageSize') || 10);

    let filtered = currentUser?.role === 'ADMIN'
      ? bookingsDb
      : bookingsDb.filter((b) => b.requesterId === currentUser?.id || b.requesterEmail === currentUser?.email);

    if (status) {
      filtered = filtered.filter((b) => b.status === status);
    }

    const totalItems = filtered.length;
    const totalPages = Math.ceil(totalItems / pageSize) || 1;
    const items = filtered.slice((page - 1) * pageSize, page * pageSize);

    return HttpResponse.json({
      items,
      pagination: {
        page,
        pageSize,
        totalItems,
        totalPages,
      },
    });
  }),

  http.post('/api/bookings/:id/cancel', async ({ params }) => {
    await delay(150);
    const bookingId = params.id as string;
    const booking = bookingsDb.find((b) => b.id === bookingId);

    if (!booking) {
      return HttpResponse.json(
        { code: 'BOOKING_NOT_FOUND', message: 'Booking not found', requestId: crypto.randomUUID() },
        { status: 404 },
      );
    }

    booking.status = 'CANCELLED';
    return HttpResponse.json(booking);
  }),

  // ------------------------------------------
  // 8. Admin Approval Queue & Actions
  // ------------------------------------------
  http.get('/api/admin/approvals', async ({ request }) => {
    await delay(180);
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page') || 1);
    const pageSize = Number(url.searchParams.get('pageSize') || 10);

    const pending = bookingsDb.filter((b) => b.status === 'PENDING');
    const totalItems = pending.length;
    const totalPages = Math.ceil(totalItems / pageSize) || 1;
    const items = pending.slice((page - 1) * pageSize, page * pageSize);

    return HttpResponse.json({
      items,
      pagination: {
        page,
        pageSize,
        totalItems,
        totalPages,
      },
    });
  }),

  http.post('/api/admin/approvals/:id/approve', async ({ params, request }) => {
    await delay(200);
    const bookingId = params.id as string;
    const body = (await request.json().catch(() => ({}))) as { note?: string };
    const booking = bookingsDb.find((b) => b.id === bookingId);

    if (!booking) {
      return HttpResponse.json(
        { code: 'NOT_FOUND', message: 'Booking not found or already processed', requestId: crypto.randomUUID() },
        { status: 404 },
      );
    }

    booking.status = 'APPROVED';
    booking.adminNote = body.note || 'Approved by campus administration';

    // Add audit log
    auditLogsDb.unshift({
      id: crypto.randomUUID(),
      timestamp: new Date().toISOString(),
      actor: currentUser?.name || 'Admin',
      actorRole: 'ADMIN',
      eventType: 'APPROVAL',
      action: `Approved booking ${booking.bookingNumber} (${booking.title})`,
      details: { bookingId: booking.id, note: booking.adminNote },
    });

    return HttpResponse.json(booking);
  }),

  http.post('/api/admin/approvals/:id/reject', async ({ params, request }) => {
    await delay(200);
    const bookingId = params.id as string;
    const body = (await request.json()) as { reason: string };
    const booking = bookingsDb.find((b) => b.id === bookingId);

    if (!booking) {
      return HttpResponse.json(
        { code: 'NOT_FOUND', message: 'Booking not found or already processed', requestId: crypto.randomUUID() },
        { status: 404 },
      );
    }

    booking.status = 'REJECTED';
    booking.adminNote = body.reason;

    // Add audit log
    auditLogsDb.unshift({
      id: crypto.randomUUID(),
      timestamp: new Date().toISOString(),
      actor: currentUser?.name || 'Admin',
      actorRole: 'ADMIN',
      eventType: 'REJECTION',
      action: `Rejected booking ${booking.bookingNumber} (${booking.title})`,
      details: { bookingId: booking.id, reason: body.reason },
    });

    return HttpResponse.json(booking);
  }),

  // ------------------------------------------
  // 9. Admin Timetable Upload & Publish
  // ------------------------------------------
  http.post('/api/admin/timetable/upload', async () => {
    await delay(400);
    // Mock parser result with realistic validation errors
    return HttpResponse.json({
      totalRows: 64,
      validRows: 62,
      errorRows: [
        {
          rowNumber: 18,
          course: 'CS-409 Cloud Computing',
          room: 'X-999',
          error: 'Room X-999 does not exist in master data',
        },
        {
          rowNumber: 37,
          course: 'BB-203 Marketing Strategy',
          room: 'C-204',
          error: 'Time slot 15:00-16:00 clashes with existing CS201 Data Structures',
        },
      ],
      preview: [
        {
          rowNumber: 1,
          semester: 'B.Tech CSE Sem 4',
          courseCode: 'CS204',
          courseName: 'Algorithms Design',
          instructor: 'Dr. Vivek Sharma',
          roomNumber: 'A-102',
          dayOfWeek: 'Monday',
          startTime: '09:00',
          endTime: '10:00',
        },
        {
          rowNumber: 2,
          semester: 'B.Tech CSE Sem 6',
          courseCode: 'CS308',
          courseName: 'Compiler Design',
          instructor: 'Prof. Ananya Sen',
          roomNumber: 'A-201',
          dayOfWeek: 'Tuesday',
          startTime: '11:15',
          endTime: '12:15',
        },
        {
          rowNumber: 3,
          semester: 'BBA Sem 2',
          courseCode: 'BB104',
          courseName: 'Financial Accounting',
          instructor: 'Dr. Pooja Mehra',
          roomNumber: 'C-102',
          dayOfWeek: 'Wednesday',
          startTime: '10:00',
          endTime: '11:30',
        },
      ],
    });
  }),

  http.post('/api/admin/timetable/publish', async () => {
    await delay(350);
    return HttpResponse.json({
      publishedCount: 62,
      message: 'Timetable published successfully. Classroom availability models refreshed.',
    });
  }),

  // ------------------------------------------
  // 10. Admin Master Data
  // ------------------------------------------
  http.get('/api/admin/master/:resource', async ({ params }) => {
    await delay(120);
    const resource = params.resource;
    switch (resource) {
      case 'blocks':
        return HttpResponse.json(SEED_ROOMS.map((r) => r.block).filter((v, i, a) => a.indexOf(v) === i));
      case 'rooms':
        return HttpResponse.json(roomsDb);
      case 'guesthouse':
        return HttpResponse.json(guestHouseDb);
      case 'parking':
        return HttpResponse.json(parkingDb);
      default:
        return HttpResponse.json([]);
    }
  }),

  // ------------------------------------------
  // 11. Audit Logs & Reports
  // ------------------------------------------
  http.get('/api/admin/audit', async ({ request }) => {
    await delay(180);
    const url = new URL(request.url);
    const eventType = url.searchParams.get('eventType');
    const actor = url.searchParams.get('actor');
    const page = Number(url.searchParams.get('page') || 1);
    const pageSize = Number(url.searchParams.get('pageSize') || 10);

    let filtered = [...auditLogsDb];
    if (eventType) {
      filtered = filtered.filter((a) => a.eventType === eventType);
    }
    if (actor) {
      filtered = filtered.filter((a) => a.actor.toLowerCase().includes(actor.toLowerCase()));
    }

    const totalItems = filtered.length;
    const totalPages = Math.ceil(totalItems / pageSize) || 1;
    const items = filtered.slice((page - 1) * pageSize, page * pageSize);

    return HttpResponse.json({
      items,
      pagination: {
        page,
        pageSize,
        totalItems,
        totalPages,
      },
    });
  }),

  http.get('/api/admin/reports/utilisation', async () => {
    await delay(220);
    return HttpResponse.json({
      byHour: [
        { hour: '09:00', occupancyRate: 68 },
        { hour: '10:00', occupancyRate: 84 },
        { hour: '11:00', occupancyRate: 92 },
        { hour: '12:00', occupancyRate: 75 },
        { hour: '13:00', occupancyRate: 42 },
        { hour: '14:00', occupancyRate: 88 },
        { hour: '15:00', occupancyRate: 95 },
        { hour: '16:00', occupancyRate: 82 },
        { hour: '17:00', occupancyRate: 60 },
        { hour: '18:00', occupancyRate: 35 },
      ],
      topRooms: [
        { roomNumber: 'A-101', bookingCount: 46, utilPct: 88 },
        { roomNumber: 'A-201', bookingCount: 41, utilPct: 82 },
        { roomNumber: 'C-301', bookingCount: 38, utilPct: 79 },
        { roomNumber: 'B-301', bookingCount: 35, utilPct: 76 },
      ],
      leastRooms: [
        { roomNumber: 'B-203', bookingCount: 6, utilPct: 15 },
        { roomNumber: 'B-204', bookingCount: 8, utilPct: 19 },
        { roomNumber: 'C-203', bookingCount: 9, utilPct: 22 },
      ],
      guestHouseOccupancyPct: 75,
      parkingPeakOccupancyPct: 88,
      totalBookingsPeriod: 84,
      pendingApprovalsCount: bookingsDb.filter((b) => b.status === 'PENDING').length,
      freeRoomsNow: 14,
    });
  }),
];
