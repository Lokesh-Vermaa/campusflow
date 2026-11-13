import { z } from 'zod';

// ==========================================
// 1. Common & Error Schemas
// ==========================================

export const ApiErrorSchema = z.object({
  code: z.string(),
  message: z.string(),
  requestId: z.string(),
});
export type ApiErrorResponse = z.infer<typeof ApiErrorSchema>;

export class ApiError extends Error {
  code: string;
  requestId: string;
  status: number;

  constructor(status: number, message: string, code = 'API_ERROR', requestId = 'unknown') {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.requestId = requestId;
  }
}

export const PaginationSchema = z.object({
  page: z.number().int().min(1),
  pageSize: z.number().int().min(1),
  totalItems: z.number().int().min(0),
  totalPages: z.number().int().min(0),
});
export type Pagination = z.infer<typeof PaginationSchema>;

// ==========================================
// 2. Authentication & User Schemas
// ==========================================

export const RoleEnum = z.enum(['STUDENT', 'STAFF', 'ADMIN']);
export type Role = z.infer<typeof RoleEnum>;

export const UserSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email(),
  name: z.string(),
  role: RoleEnum,
  department: z.string(),
  avatarUrl: z.string().nullable().optional(),
});
export type User = z.infer<typeof UserSchema>;

export const LoginRequestSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});
export type LoginRequest = z.infer<typeof LoginRequestSchema>;

export const LoginResponseSchema = z.object({
  user: UserSchema,
});
export type LoginResponse = z.infer<typeof LoginResponseSchema>;

export const CsrfResponseSchema = z.object({
  token: z.string(),
  headerName: z.string(),
});
export type CsrfResponse = z.infer<typeof CsrfResponseSchema>;

// ==========================================
// 3. Announcement
// ==========================================

export const AnnouncementSchema = z.object({
  id: z.string(),
  message: z.string(),
  linkText: z.string(),
  linkHref: z.string(),
  active: z.boolean(),
});
export type Announcement = z.infer<typeof AnnouncementSchema>;

// ==========================================
// 4. Rooms & Availability Finder Schemas
// ==========================================

export const RoomFeatureEnum = z.enum([
  'projector',
  'ac',
  'smartboard',
  'mic',
  'recording',
  'podium',
]);
export type RoomFeature = z.infer<typeof RoomFeatureEnum>;

export const RoomTypeEnum = z.enum(['CLASSROOM', 'LAB', 'AMPHITHEATRE', 'SEMINAR_HALL']);
export type RoomType = z.infer<typeof RoomTypeEnum>;

export const TimeWindowSchema = z.object({
  startTime: z.string(),
  endTime: z.string(),
});
export type TimeWindow = z.infer<typeof TimeWindowSchema>;

export const AvailableRoomSchema = z.object({
  id: z.string().uuid(),
  roomNumber: z.string(),
  block: z.string(),
  floor: z.number(),
  capacity: z.number(),
  type: RoomTypeEnum,
  features: z.array(z.string()),
  bookableOutside: z.boolean(),
  freeWindows: z.array(TimeWindowSchema),
});
export type AvailableRoom = z.infer<typeof AvailableRoomSchema>;

export const RoomTimelineSlotSchema = z.object({
  slot: z.string(),
  type: z.enum(['CLASS', 'BOOKING', 'MAINTENANCE']),
  title: z.string(),
});
export type RoomTimelineSlot = z.infer<typeof RoomTimelineSlotSchema>;

export const UnavailableRoomSchema = z.object({
  id: z.string().uuid(),
  roomNumber: z.string(),
  block: z.string(),
  floor: z.number(),
  capacity: z.number(),
  type: RoomTypeEnum,
  features: z.array(z.string()),
  conflictReason: z.string(),
  timeline: z.array(RoomTimelineSlotSchema),
});
export type UnavailableRoom = z.infer<typeof UnavailableRoomSchema>;

export const AvailabilityResponseSchema = z.object({
  date: z.string(),
  requestedInterval: TimeWindowSchema,
  availableRooms: z.array(AvailableRoomSchema),
  unavailableRooms: z.array(UnavailableRoomSchema),
});
export type AvailabilityResponse = z.infer<typeof AvailabilityResponseSchema>;

export const RoomScheduleSlotSchema = z.object({
  startTime: z.string(),
  endTime: z.string(),
  status: z.enum(['FREE', 'BUSY']),
  title: z.string().optional(),
  category: z.enum(['TIMETABLE', 'BOOKING', 'MAINTENANCE']).optional(),
});
export type RoomScheduleSlot = z.infer<typeof RoomScheduleSlotSchema>;

export const RoomScheduleResponseSchema = z.object({
  roomId: z.string().uuid(),
  roomNumber: z.string(),
  date: z.string(),
  slots: z.array(RoomScheduleSlotSchema),
});
export type RoomScheduleResponse = z.infer<typeof RoomScheduleResponseSchema>;

// ==========================================
// 5. Venues (Amphitheatres & Labs) Schemas
// ==========================================

export const VenueSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  type: z.enum(['AMPHITHEATRE', 'LAB', 'AUDITORIUM']),
  capacity: z.number(),
  location: z.string(),
  isEligible: z.boolean(),
  eligibilityReason: z.string().nullable(),
  features: z.array(z.string()),
  requiresApproval: z.boolean(),
});
export type Venue = z.infer<typeof VenueSchema>;

export const VenuesResponseSchema = z.array(VenueSchema);
export type VenuesResponse = z.infer<typeof VenuesResponseSchema>;

// ==========================================
// 6. MDC Guest House Schemas
// ==========================================

export const GuestHouseRoomTypeEnum = z.enum(['STANDARD', 'DELUXE', 'EXECUTIVE', 'SUITE']);
export type GuestHouseRoomType = z.infer<typeof GuestHouseRoomTypeEnum>;

export const GuestHouseRoomSchema = z.object({
  id: z.string().uuid(),
  roomNumber: z.string(),
  roomType: GuestHouseRoomTypeEnum,
  bedType: z.enum(['SINGLE', 'DOUBLE', 'TWIN']),
  capacity: z.number(),
  isAvailable: z.boolean(),
  amenities: z.array(z.string()),
  ratePerNight: z.number(),
});
export type GuestHouseRoom = z.infer<typeof GuestHouseRoomSchema>;

export const GuestHouseRoomsResponseSchema = z.array(GuestHouseRoomSchema);
export type GuestHouseRoomsResponse = z.infer<typeof GuestHouseRoomsResponseSchema>;

// ==========================================
// 7. Parking Schemas
// ==========================================

export const ParkingZoneSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  code: z.string(),
  totalSlots: z.number(),
  occupiedSlots: z.number(),
  availableSlots: z.number(),
  status: z.enum(['AVAILABLE', 'NEAR_FULL', 'FULL']),
  ratePerHour: z.number(),
});
export type ParkingZone = z.infer<typeof ParkingZoneSchema>;

export const ParkingZonesResponseSchema = z.array(ParkingZoneSchema);
export type ParkingZonesResponse = z.infer<typeof ParkingZonesResponseSchema>;

export const VisitorParkingRequestSchema = z.object({
  visitorName: z.string().min(2, 'Visitor name is required'),
  vehicleNumber: z
    .string()
    .min(5, 'Valid vehicle number required')
    .regex(/^[A-Z]{2}[ -]?[0-9]{1,2}[ -]?[A-Z]{1,3}[ -]?[0-9]{4}$/i, 'Format e.g. RJ-14-CB-1234'),
  vehicleType: z.enum(['TWO_WHEELER', 'FOUR_WHEELER', 'BUS_VAN']),
  zoneId: z.string().uuid(),
  visitDate: z.string(),
  startTime: z.string(),
  endTime: z.string(),
  purpose: z.string().min(3, 'Purpose required'),
});
export type VisitorParkingRequest = z.infer<typeof VisitorParkingRequestSchema>;

export const ParkingPermitResponseSchema = z.object({
  permitId: z.string().uuid(),
  permitCode: z.string(),
  status: z.enum(['ALLOCATED', 'WAITLISTED', 'REJECTED']),
  allocatedSlot: z.string(),
  validUntil: z.string(),
});
export type ParkingPermitResponse = z.infer<typeof ParkingPermitResponseSchema>;

// ==========================================
// 8. Bookings Schemas
// ==========================================

export const BookingStatusEnum = z.enum(['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED']);
export type BookingStatus = z.infer<typeof BookingStatusEnum>;

export const BookingSchema = z.object({
  id: z.string().uuid(),
  bookingNumber: z.string(),
  title: z.string(),
  description: z.string().optional(),
  status: BookingStatusEnum,
  requesterId: z.string().uuid(),
  requesterName: z.string(),
  requesterEmail: z.string().email().optional(),
  requesterRole: RoleEnum.optional(),
  resourceType: z.enum(['ROOM', 'VENUE', 'GUEST_HOUSE', 'PARKING', 'MULTI_RESOURCE']),
  resourceId: z.string().uuid(),
  resourceName: z.string(),
  bookingDate: z.string(),
  startTime: z.string(),
  endTime: z.string(),
  expectedAttendees: z.number().optional(),
  guestHouseRoomId: z.string().uuid().nullable().optional(),
  guestHouseRoomName: z.string().nullable().optional(),
  guestHouseCheckIn: z.string().nullable().optional(),
  guestHouseCheckOut: z.string().nullable().optional(),
  parkingRequested: z.boolean().optional(),
  parkingZoneName: z.string().nullable().optional(),
  createdAt: z.string(),
  adminNote: z.string().nullable().optional(),
  agentReasoning: z.string().nullable().optional(),
  conflictCheckResult: z
    .object({
      hasClash: z.boolean(),
      summary: z.string(),
    })
    .optional(),
});
export type Booking = z.infer<typeof BookingSchema>;

export const CreateBookingRequestSchema = z
  .object({
    title: z.string().min(3, 'Title must be at least 3 characters').max(100),
    description: z.string().max(500).optional(),
    resourceType: z.enum(['ROOM', 'VENUE', 'GUEST_HOUSE', 'MULTI_RESOURCE']),
    resourceId: z.string().uuid('Please select a valid resource'),
    bookingDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Valid date YYYY-MM-DD required'),
    startTime: z.string().regex(/^\d{2}:\d{2}$/, 'Valid time HH:mm required'),
    endTime: z.string().regex(/^\d{2}:\d{2}$/, 'Valid time HH:mm required'),
    expectedAttendees: z.number().int().min(1, 'At least 1 attendee required').max(1000),
    guestHouseRoomId: z.string().uuid().optional().nullable(),
    guestHouseCheckIn: z.string().optional().nullable(),
    guestHouseCheckOut: z.string().optional().nullable(),
    parkingZoneId: z.string().uuid().optional().nullable(),
    visitorName: z.string().optional().nullable(),
    vehicleNumber: z.string().optional().nullable(),
  })
  .refine(
    (data) => {
      return data.endTime > data.startTime;
    },
    {
      message: 'End time must be after start time',
      path: ['endTime'],
    },
  );
export type CreateBookingRequest = z.infer<typeof CreateBookingRequestSchema>;

export const PaginatedBookingsResponseSchema = z.object({
  items: z.array(BookingSchema),
  pagination: PaginationSchema,
});
export type PaginatedBookingsResponse = z.infer<typeof PaginatedBookingsResponseSchema>;

// ==========================================
// 9. Admin Approvals & Audit Log
// ==========================================

export const ApproveBookingRequestSchema = z.object({
  note: z.string().max(300).optional(),
});

export const RejectBookingRequestSchema = z.object({
  reason: z.string().min(5, 'Rejection reason must be at least 5 characters').max(300),
});

export const AuditEventTypeEnum = z.enum([
  'AGENT_REASONING',
  'TOOL_CALL',
  'API_CALL',
  'APPROVAL',
  'REJECTION',
  'LOGIN',
  'DATA_CHANGE',
]);
export type AuditEventType = z.infer<typeof AuditEventTypeEnum>;

export const AuditLogEntrySchema = z.object({
  id: z.string().uuid(),
  timestamp: z.string(),
  actor: z.string(),
  actorRole: RoleEnum,
  eventType: AuditEventTypeEnum,
  action: z.string(),
  details: z.record(z.unknown()),
  ipAddress: z.string().optional(),
});
export type AuditLogEntry = z.infer<typeof AuditLogEntrySchema>;

export const PaginatedAuditResponseSchema = z.object({
  items: z.array(AuditLogEntrySchema),
  pagination: PaginationSchema,
});
export type PaginatedAuditResponse = z.infer<typeof PaginatedAuditResponseSchema>;

// ==========================================
// 10. Reports Schemas
// ==========================================

export const HourlyUtilisationSchema = z.object({
  hour: z.string(),
  occupancyRate: z.number(),
});

export const RoomUtilisationSchema = z.object({
  roomNumber: z.string(),
  bookingCount: z.number(),
  utilPct: z.number(),
});

export const UtilisationReportSchema = z.object({
  byHour: z.array(HourlyUtilisationSchema),
  topRooms: z.array(RoomUtilisationSchema),
  leastRooms: z.array(RoomUtilisationSchema),
  guestHouseOccupancyPct: z.number(),
  parkingPeakOccupancyPct: z.number(),
  totalBookingsPeriod: z.number(),
  pendingApprovalsCount: z.number(),
  freeRoomsNow: z.number(),
});
export type UtilisationReport = z.infer<typeof UtilisationReportSchema>;

// ==========================================
// 11. Agent Message Schemas
// ==========================================

export const AgentPlanStepSchema = z.object({
  id: z.number(),
  title: z.string(),
  status: z.enum(['pending', 'running', 'done', 'failed']),
});
export type AgentPlanStep = z.infer<typeof AgentPlanStepSchema>;

export const AgentToolCallSchema = z.object({
  id: z.string(),
  toolName: z.string(),
  args: z.record(z.unknown()),
  resultSummary: z.string(),
  durationMs: z.number(),
  status: z.enum(['success', 'failed', 'retrying']),
  retryCount: z.number().optional(),
});
export type AgentToolCall = z.infer<typeof AgentToolCallSchema>;

export const AgentProposalSchema = z.object({
  title: z.string(),
  roomName: z.string(),
  roomId: z.string().uuid(),
  date: z.string(),
  time: z.string(),
  capacity: z.number(),
  guestHouse: z.string().optional(),
  parking: z.string().optional(),
  requiresAdminApproval: z.boolean(),
  status: z.enum(['DRAFT', 'SUBMITTED', 'APPROVED']),
});
export type AgentProposal = z.infer<typeof AgentProposalSchema>;

export const AgentConflictSchema = z.object({
  resourceName: z.string(),
  reason: z.string(),
  clashingSlot: z.string(),
  alternatives: z.array(
    z.object({
      id: z.string().uuid(),
      name: z.string(),
      capacity: z.number(),
      features: z.array(z.string()),
    }),
  ),
});
export type AgentConflict = z.infer<typeof AgentConflictSchema>;

export const AgentChatMessageSchema = z.object({
  id: z.string(),
  sender: z.enum(['user', 'assistant', 'system']),
  timestamp: z.string(),
  text: z.string().optional(),
  plan: z.array(AgentPlanStepSchema).optional(),
  toolCalls: z.array(AgentToolCallSchema).optional(),
  proposal: AgentProposalSchema.optional(),
  conflict: AgentConflictSchema.optional(),
  clarifyingQuestions: z.array(z.string()).optional(),
});
export type AgentChatMessage = z.infer<typeof AgentChatMessageSchema>;

// ==========================================
// 12. Conversation Schemas
// ==========================================

export const ConversationSchema = z.object({
  id: z.string().uuid(),
  title: z.string(),
  userId: z.string().uuid(),
  createdAt: z.string(),
  updatedAt: z.string(),
});
export type Conversation = z.infer<typeof ConversationSchema>;

export const PaginatedConversationsResponseSchema = z.object({
  items: z.array(ConversationSchema),
  pagination: PaginationSchema,
});
export type PaginatedConversationsResponse = z.infer<typeof PaginatedConversationsResponseSchema>;

