import {
  User,
  Venue,
  GuestHouseRoom,
  ParkingZone,
  Booking,
  AuditLogEntry,
  Announcement,
} from '@/api/types';

// ==========================================
// 1. DEMO USERS (Synthetic data only, @demo.jklu.test)
// ==========================================
export const SEED_USERS: Record<string, User> = {
  student: {
    id: '11111111-1111-4111-8111-111111111111',
    email: 'student@demo.jklu.test',
    name: 'Aarav Sharma',
    role: 'STUDENT',
    department: 'Computer Science & Engineering',
    avatarUrl: null,
  },
  staff: {
    id: '22222222-2222-4222-8222-222222222222',
    email: 'staff@demo.jklu.test',
    name: 'Dr. Neha Verma',
    role: 'STAFF',
    department: 'Electronics & Communication',
    avatarUrl: null,
  },
  admin: {
    id: '33333333-3333-4333-8333-333333333333',
    email: 'admin@demo.jklu.test',
    name: 'Rajesh Patel',
    role: 'ADMIN',
    department: 'Campus Administration',
    avatarUrl: null,
  },
};

// ==========================================
// 2. BLOCKS & 25 ROOMS ACROSS 3 BLOCKS
// ==========================================
export interface SeedRoom {
  id: string;
  roomNumber: string;
  block: string;
  floor: number;
  capacity: number;
  type: 'CLASSROOM' | 'LAB' | 'AMPHITHEATRE' | 'SEMINAR_HALL';
  features: string[];
  bookableOutside: boolean;
}

export const SEED_BLOCKS = [
  { id: 'b0000001-0000-4000-8000-000000000001', name: 'Block A (Academic & Computing)' },
  { id: 'b0000002-0000-4000-8000-000000000002', name: 'Block B (Science & Engineering)' },
  { id: 'b0000003-0000-4000-8000-000000000003', name: 'Block C (Management & Design)' },
];

export const SEED_ROOMS: SeedRoom[] = [
  // Block A (Academic & Computing) - 8 rooms
  {
    id: 'a0000001-0000-4000-8000-000000000001',
    roomNumber: 'A-101',
    block: 'Block A',
    floor: 1,
    capacity: 60,
    type: 'CLASSROOM',
    features: ['projector', 'ac', 'smartboard', 'mic'],
    bookableOutside: true,
  },
  {
    id: 'a0000002-0000-4000-8000-000000000002',
    roomNumber: 'A-102',
    block: 'Block A',
    floor: 1,
    capacity: 60,
    type: 'CLASSROOM',
    features: ['projector', 'ac'],
    bookableOutside: true,
  },
  {
    id: 'a0000003-0000-4000-8000-000000000003',
    roomNumber: 'A-103',
    block: 'Block A',
    floor: 1,
    capacity: 40,
    type: 'CLASSROOM',
    features: ['projector', 'ac'],
    bookableOutside: true,
  },
  {
    id: 'a0000004-0000-4000-8000-000000000004',
    roomNumber: 'A-201',
    block: 'Block A',
    floor: 2,
    capacity: 80,
    type: 'CLASSROOM',
    features: ['projector', 'ac', 'smartboard', 'mic', 'recording'],
    bookableOutside: true,
  },
  {
    id: 'a0000005-0000-4000-8000-000000000005',
    roomNumber: 'A-202',
    block: 'Block A',
    floor: 2,
    capacity: 80,
    type: 'CLASSROOM',
    features: ['projector', 'ac', 'smartboard'],
    bookableOutside: true,
  },
  {
    id: 'a0000006-0000-4000-8000-000000000006',
    roomNumber: 'A-203',
    block: 'Block A',
    floor: 2,
    capacity: 50,
    type: 'CLASSROOM',
    features: ['projector', 'ac'],
    bookableOutside: true,
  },
  {
    id: 'a0000007-0000-4000-8000-000000000007',
    roomNumber: 'A-301',
    block: 'Block A',
    floor: 3,
    capacity: 120,
    type: 'SEMINAR_HALL',
    features: ['projector', 'ac', 'smartboard', 'mic', 'recording', 'podium'],
    bookableOutside: true,
  },
  {
    id: 'a0000008-0000-4000-8000-000000000008',
    roomNumber: 'A-302',
    block: 'Block A',
    floor: 3,
    capacity: 45,
    type: 'CLASSROOM',
    features: ['projector'],
    bookableOutside: true,
  },

  // Block B (Science & Engineering) - 9 rooms
  {
    id: 'b0000001-0000-4000-8000-000000000009',
    roomNumber: 'B-101',
    block: 'Block B',
    floor: 1,
    capacity: 55,
    type: 'CLASSROOM',
    features: ['projector', 'ac'],
    bookableOutside: true,
  },
  {
    id: 'b0000002-0000-4000-8000-000000000010',
    roomNumber: 'B-102',
    block: 'Block B',
    floor: 1,
    capacity: 55,
    type: 'CLASSROOM',
    features: ['projector', 'ac', 'smartboard'],
    bookableOutside: true,
  },
  {
    id: 'b0000003-0000-4000-8000-000000000011',
    roomNumber: 'B-201',
    block: 'Block B',
    floor: 2,
    capacity: 70,
    type: 'CLASSROOM',
    features: ['projector', 'ac', 'mic'],
    bookableOutside: true,
  },
  {
    id: 'b0000004-0000-4000-8000-000000000012',
    roomNumber: 'B-202',
    block: 'Block B',
    floor: 2,
    capacity: 70,
    type: 'CLASSROOM',
    features: ['projector', 'ac'],
    bookableOutside: true,
  },
  {
    id: 'b0000005-0000-4000-8000-000000000013',
    roomNumber: 'B-203',
    block: 'Block B',
    floor: 2,
    capacity: 40,
    type: 'CLASSROOM',
    features: ['projector'],
    bookableOutside: true,
  },
  {
    id: 'b0000006-0000-4000-8000-000000000014',
    roomNumber: 'B-204',
    block: 'Block B',
    floor: 2,
    capacity: 40,
    type: 'CLASSROOM',
    features: ['projector', 'ac'],
    bookableOutside: true,
  },
  {
    id: 'b0000007-0000-4000-8000-000000000015',
    roomNumber: 'B-301',
    block: 'Block B',
    floor: 3,
    capacity: 90,
    type: 'SEMINAR_HALL',
    features: ['projector', 'ac', 'smartboard', 'mic', 'podium'],
    bookableOutside: true,
  },
  {
    id: 'b0000008-0000-4000-8000-000000000016',
    roomNumber: 'B-302',
    block: 'Block B',
    floor: 3,
    capacity: 50,
    type: 'CLASSROOM',
    features: ['projector', 'ac'],
    bookableOutside: true,
  },
  {
    id: 'b0000009-0000-4000-8000-000000000017',
    roomNumber: 'B-303',
    block: 'Block B',
    floor: 3,
    capacity: 50,
    type: 'CLASSROOM',
    features: ['projector'],
    bookableOutside: true,
  },

  // Block C (Management & Design) - 8 rooms
  {
    id: 'c0000001-0000-4000-8000-000000000018',
    roomNumber: 'C-101',
    block: 'Block C',
    floor: 1,
    capacity: 65,
    type: 'CLASSROOM',
    features: ['projector', 'ac', 'smartboard'],
    bookableOutside: true,
  },
  {
    id: 'c0000002-0000-4000-8000-000000000019',
    roomNumber: 'C-102',
    block: 'Block C',
    floor: 1,
    capacity: 65,
    type: 'CLASSROOM',
    features: ['projector', 'ac'],
    bookableOutside: true,
  },
  {
    id: 'c0000003-0000-4000-8000-000000000020',
    roomNumber: 'C-201',
    block: 'Block C',
    floor: 2,
    capacity: 75,
    type: 'CLASSROOM',
    features: ['projector', 'ac', 'mic'],
    bookableOutside: true,
  },
  {
    id: 'c0000004-0000-4000-8000-000000000021',
    roomNumber: 'C-202',
    block: 'Block C',
    floor: 2,
    capacity: 75,
    type: 'CLASSROOM',
    features: ['projector', 'ac'],
    bookableOutside: true,
  },
  {
    id: 'c0000005-0000-4000-8000-000000000022',
    roomNumber: 'C-203',
    block: 'Block C',
    floor: 2,
    capacity: 45,
    type: 'CLASSROOM',
    features: ['projector'],
    bookableOutside: true,
  },
  {
    id: 'c0000006-0000-4000-8000-000000000023',
    roomNumber: 'C-204',
    block: 'Block C',
    floor: 2,
    capacity: 85,
    type: 'CLASSROOM',
    features: ['projector', 'ac', 'smartboard', 'mic'],
    bookableOutside: true,
  },
  {
    id: 'c0000007-0000-4000-8000-000000000024',
    roomNumber: 'C-301',
    block: 'Block C',
    floor: 3,
    capacity: 100,
    type: 'SEMINAR_HALL',
    features: ['projector', 'ac', 'smartboard', 'mic', 'recording', 'podium'],
    bookableOutside: true,
  },
  {
    id: 'c0000008-0000-4000-8000-000000000025',
    roomNumber: 'C-302',
    block: 'Block C',
    floor: 3,
    capacity: 60,
    type: 'CLASSROOM',
    features: ['projector', 'ac'],
    bookableOutside: true,
  },
];

// ==========================================
// 3. VENUES: 2 Amphitheatres + 4 Labs
// ==========================================
export const SEED_VENUES: Venue[] = [
  {
    id: 'v0000001-0000-4000-8000-000000000001',
    name: 'Rabindranath Tagore Amphitheatre',
    type: 'AMPHITHEATRE',
    capacity: 350,
    location: 'Central Academic Quad',
    isEligible: true,
    eligibilityReason: null,
    features: ['sound_system', 'stage_lighting', 'green_rooms', 'open_air_acoustics'],
    requiresApproval: true,
  },
  {
    id: 'v0000002-0000-4000-8000-000000000002',
    name: 'Open Air Theatre (OAT)',
    type: 'AMPHITHEATRE',
    capacity: 600,
    location: 'Student Activity Centre Lawn',
    isEligible: true,
    eligibilityReason: null,
    features: ['spacious_stage', 'tiered_seating', 'high_power_audio'],
    requiresApproval: true,
  },
  {
    id: 'v0000003-0000-4000-8000-000000000003',
    name: 'Advanced Computing & AI Lab',
    type: 'LAB',
    capacity: 45,
    location: 'Block A, 2nd Floor (A-205)',
    isEligible: true,
    eligibilityReason: null,
    features: ['workstations', 'gpu_server_access', 'gigabit_lan', 'dual_projectors'],
    requiresApproval: true,
  },
  {
    id: 'v0000004-0000-4000-8000-000000000004',
    name: 'Robotics & IoT Research Lab',
    type: 'LAB',
    capacity: 30,
    location: 'Block B, 3rd Floor (B-305)',
    isEligible: false,
    eligibilityReason: 'Restricted to Semester 7/8 Capstone projects and faculty research only',
    features: ['soldering_stations', '3d_printers', 'oscilloscopes'],
    requiresApproval: true,
  },
  {
    id: 'v0000005-0000-4000-8000-000000000005',
    name: 'VLSI & Embedded Systems Lab',
    type: 'LAB',
    capacity: 35,
    location: 'Block B, 2nd Floor (B-205)',
    isEligible: false,
    eligibilityReason: 'Specialized FPGA hardware requires Department In-Charge written authorization',
    features: ['fpga_boards', 'cadence_eda_licenses', 'clean_bench'],
    requiresApproval: true,
  },
  {
    id: 'v0000006-0000-4000-8000-000000000006',
    name: 'Applied Physics & Materials Lab',
    type: 'LAB',
    capacity: 40,
    location: 'Block B, Ground Floor (B-002)',
    isEligible: true,
    eligibilityReason: null,
    features: ['optical_benches', 'spectrometers', 'safety_showers'],
    requiresApproval: true,
  },
];

// ==========================================
// 4. MDC GUEST HOUSE: 12 Rooms
// ==========================================
export const SEED_GUESTHOUSE_ROOMS: GuestHouseRoom[] = [
  {
    id: 'gh000001-0000-4000-8000-000000000001',
    roomNumber: 'MDC-101',
    roomType: 'DELUXE',
    bedType: 'DOUBLE',
    capacity: 2,
    isAvailable: true,
    amenities: ['ac', 'wifi', 'tv', 'work_desk', 'attached_bath', 'kettle'],
    ratePerNight: 2000,
  },
  {
    id: 'gh000002-0000-4000-8000-000000000002',
    roomNumber: 'MDC-102',
    roomType: 'DELUXE',
    bedType: 'DOUBLE',
    capacity: 2,
    isAvailable: true,
    amenities: ['ac', 'wifi', 'tv', 'work_desk', 'attached_bath', 'kettle'],
    ratePerNight: 2000,
  },
  {
    id: 'gh000003-0000-4000-8000-000000000003',
    roomNumber: 'MDC-103',
    roomType: 'STANDARD',
    bedType: 'SINGLE',
    capacity: 1,
    isAvailable: true,
    amenities: ['ac', 'wifi', 'attached_bath'],
    ratePerNight: 1200,
  },
  {
    id: 'gh000004-0000-4000-8000-000000000004',
    roomNumber: 'MDC-104',
    roomType: 'STANDARD',
    bedType: 'SINGLE',
    capacity: 1,
    isAvailable: false,
    amenities: ['ac', 'wifi', 'attached_bath'],
    ratePerNight: 1200,
  },
  {
    id: 'gh000005-0000-4000-8000-000000000005',
    roomNumber: 'MDC-201',
    roomType: 'EXECUTIVE',
    bedType: 'DOUBLE',
    capacity: 2,
    isAvailable: true,
    amenities: ['ac', 'high_speed_wifi', 'smart_tv', 'ergonomic_desk', 'mini_fridge', 'balcony'],
    ratePerNight: 3000,
  },
  {
    id: 'gh000006-0000-4000-8000-000000000006',
    roomNumber: 'MDC-202',
    roomType: 'EXECUTIVE',
    bedType: 'DOUBLE',
    capacity: 2,
    isAvailable: true,
    amenities: ['ac', 'high_speed_wifi', 'smart_tv', 'ergonomic_desk', 'mini_fridge', 'balcony'],
    ratePerNight: 3000,
  },
  {
    id: 'gh000007-0000-4000-8000-000000000007',
    roomNumber: 'MDC-203',
    roomType: 'STANDARD',
    bedType: 'TWIN',
    capacity: 2,
    isAvailable: true,
    amenities: ['ac', 'wifi', 'attached_bath'],
    ratePerNight: 1500,
  },
  {
    id: 'gh000008-0000-4000-8000-000000000008',
    roomNumber: 'MDC-204',
    roomType: 'STANDARD',
    bedType: 'TWIN',
    capacity: 2,
    isAvailable: true,
    amenities: ['ac', 'wifi', 'attached_bath'],
    ratePerNight: 1500,
  },
  {
    id: 'gh000009-0000-4000-8000-000000000009',
    roomNumber: 'MDC-301',
    roomType: 'SUITE',
    bedType: 'DOUBLE',
    capacity: 3,
    isAvailable: true,
    amenities: ['ac', 'living_room', 'pantry', 'high_speed_wifi', 'smart_tv', 'vip_lounge_access'],
    ratePerNight: 4500,
  },
  {
    id: 'gh000010-0000-4000-8000-000000000010',
    roomNumber: 'MDC-302',
    roomType: 'SUITE',
    bedType: 'DOUBLE',
    capacity: 3,
    isAvailable: false,
    amenities: ['ac', 'living_room', 'pantry', 'high_speed_wifi', 'smart_tv', 'vip_lounge_access'],
    ratePerNight: 4500,
  },
  {
    id: 'gh000011-0000-4000-8000-000000000011',
    roomNumber: 'MDC-303',
    roomType: 'DELUXE',
    bedType: 'DOUBLE',
    capacity: 2,
    isAvailable: true,
    amenities: ['ac', 'wifi', 'tv', 'work_desk', 'attached_bath'],
    ratePerNight: 2000,
  },
  {
    id: 'gh000012-0000-4000-8000-000000000012',
    roomNumber: 'MDC-304',
    roomType: 'STANDARD',
    bedType: 'SINGLE',
    capacity: 1,
    isAvailable: true,
    amenities: ['ac', 'wifi', 'attached_bath'],
    ratePerNight: 1200,
  },
];

// ==========================================
// 5. PARKING: 3 Zones
// ==========================================
export const SEED_PARKING_ZONES: ParkingZone[] = [
  {
    id: 'pk000001-0000-4000-8000-000000000001',
    name: 'Zone P1 (Main Gate - Visitor & VIP)',
    code: 'P1',
    totalSlots: 35,
    occupiedSlots: 22,
    availableSlots: 13,
    status: 'AVAILABLE',
    ratePerHour: 0,
  },
  {
    id: 'pk000002-0000-4000-8000-000000000002',
    name: 'Zone P2 (Academic Block South)',
    code: 'P2',
    totalSlots: 60,
    occupiedSlots: 54,
    availableSlots: 6,
    status: 'NEAR_FULL',
    ratePerHour: 0,
  },
  {
    id: 'pk000003-0000-4000-8000-000000000003',
    name: 'Zone P3 (MDC Guest House & Residential)',
    code: 'P3',
    totalSlots: 25,
    occupiedSlots: 11,
    availableSlots: 14,
    status: 'AVAILABLE',
    ratePerHour: 0,
  },
];

// ==========================================
// 6. TIMETABLE SLOTS (8 Semesters)
// ==========================================
export interface TimetableSlot {
  id: string;
  semester: string;
  courseCode: string;
  courseName: string;
  instructor: string;
  roomId: string;
  roomNumber: string;
  dayOfWeek: number; // 1 = Monday ... 5 = Friday
  startTime: string; // HH:mm
  endTime: string;   // HH:mm
}

export const SEED_TIMETABLE: TimetableSlot[] = [
  // B.Tech CSE Sem 1
  {
    id: 'tt000001-0000-4000-8000-000000000001',
    semester: 'B.Tech CSE Sem 1',
    courseCode: 'CS101',
    courseName: 'Intro to Computer Science',
    instructor: 'Dr. Neha Verma',
    roomId: 'a0000001-0000-4000-8000-000000000001',
    roomNumber: 'A-101',
    dayOfWeek: 1,
    startTime: '09:00',
    endTime: '10:00',
  },
  {
    id: 'tt000002-0000-4000-8000-000000000002',
    semester: 'B.Tech CSE Sem 1',
    courseCode: 'MA101',
    courseName: 'Calculus & Linear Algebra',
    instructor: 'Dr. S. K. Jain',
    roomId: 'a0000001-0000-4000-8000-000000000001',
    roomNumber: 'A-101',
    dayOfWeek: 2,
    startTime: '10:00',
    endTime: '11:00',
  },
  {
    id: 'tt000003-0000-4000-8000-000000000003',
    semester: 'B.Tech CSE Sem 1',
    courseCode: 'PH101',
    courseName: 'Engineering Physics',
    instructor: 'Dr. R. C. Mathur',
    roomId: 'a0000002-0000-4000-8000-000000000002',
    roomNumber: 'A-102',
    dayOfWeek: 3,
    startTime: '11:15',
    endTime: '12:15',
  },

  // B.Tech CSE Sem 3 (Includes Room C-204 Data Structures 15:00-16:00 specifically for prompt requirement!)
  {
    id: 'tt000004-0000-4000-8000-000000000004',
    semester: 'B.Tech CSE Sem 3',
    courseCode: 'CS201',
    courseName: 'Data Structures & Algorithms',
    instructor: 'Prof. Ananya Sen',
    roomId: 'c0000006-0000-4000-8000-000000000023',
    roomNumber: 'C-204',
    dayOfWeek: 4, // Thursday
    startTime: '15:00',
    endTime: '16:00',
  },
  {
    id: 'tt000005-0000-4000-8000-000000000005',
    semester: 'B.Tech CSE Sem 3',
    courseCode: 'CS202',
    courseName: 'Discrete Mathematical Structures',
    instructor: 'Dr. Vivek Sharma',
    roomId: 'a0000004-0000-4000-8000-000000000004',
    roomNumber: 'A-201',
    dayOfWeek: 1,
    startTime: '10:00',
    endTime: '11:00',
  },

  // B.Tech CSE Sem 5
  {
    id: 'tt000006-0000-4000-8000-000000000006',
    semester: 'B.Tech CSE Sem 5',
    courseCode: 'CS301',
    courseName: 'Operating Systems',
    instructor: 'Prof. S. R. Naidu',
    roomId: 'b0000001-0000-4000-8000-000000000009',
    roomNumber: 'B-101',
    dayOfWeek: 2,
    startTime: '14:00',
    endTime: '15:30',
  },
  {
    id: 'tt000007-0000-4000-8000-000000000007',
    semester: 'B.Tech CSE Sem 5',
    courseCode: 'CS302',
    courseName: 'Database Management Systems',
    instructor: 'Dr. Neha Verma',
    roomId: 'b0000003-0000-4000-8000-000000000011',
    roomNumber: 'B-201',
    dayOfWeek: 4,
    startTime: '11:15',
    endTime: '12:45',
  },

  // B.Tech CSE Sem 7
  {
    id: 'tt000008-0000-4000-8000-000000000008',
    semester: 'B.Tech CSE Sem 7',
    courseCode: 'CS401',
    courseName: 'Machine Learning & Neural Nets',
    instructor: 'Dr. K. Raghavan',
    roomId: 'a0000007-0000-4000-8000-000000000007',
    roomNumber: 'A-301',
    dayOfWeek: 3,
    startTime: '14:00',
    endTime: '16:00',
  },

  // BBA Sem 1
  {
    id: 'tt000009-0000-4000-8000-000000000009',
    semester: 'BBA Sem 1',
    courseCode: 'BB101',
    courseName: 'Principles of Management',
    instructor: 'Prof. M. K. Gupta',
    roomId: 'c0000001-0000-4000-8000-000000000018',
    roomNumber: 'C-101',
    dayOfWeek: 1,
    startTime: '09:00',
    endTime: '10:30',
  },

  // BBA Sem 3
  {
    id: 'tt000010-0000-4000-8000-000000000010',
    semester: 'BBA Sem 3',
    courseCode: 'BB201',
    courseName: 'Corporate Finance',
    instructor: 'Dr. Pooja Mehra',
    roomId: 'c0000003-0000-4000-8000-000000000020',
    roomNumber: 'C-201',
    dayOfWeek: 2,
    startTime: '11:15',
    endTime: '12:45',
  },

  // BBA Sem 5
  {
    id: 'tt000011-0000-4000-8000-000000000011',
    semester: 'BBA Sem 5',
    courseCode: 'BB301',
    courseName: 'Strategic Business Planning',
    instructor: 'Prof. A. K. Roy',
    roomId: 'c0000007-0000-4000-8000-000000000024',
    roomNumber: 'C-301',
    dayOfWeek: 5,
    startTime: '10:00',
    endTime: '12:00',
  },

  // M.Tech Sem 1
  {
    id: 'tt000012-0000-4000-8000-000000000012',
    semester: 'M.Tech CSE Sem 1',
    courseCode: 'MT501',
    courseName: 'Advanced Distributed Systems',
    instructor: 'Dr. Amit Sengupta',
    roomId: 'b0000007-0000-4000-8000-000000000015',
    roomNumber: 'B-301',
    dayOfWeek: 4,
    startTime: '09:00',
    endTime: '11:00',
  },
];

// ==========================================
// 7. 20 BOOKINGS IN MIXED STATES
// ==========================================
export const SEED_BOOKINGS: Booking[] = [
  {
    id: 'bk000001-0000-4000-8000-000000000001',
    bookingNumber: 'JKLU-BK-2026-1001',
    title: 'AI & Robotics Club Buildathon Prep',
    description: 'Hardware setup and team hackathon rehearsals',
    status: 'APPROVED',
    requesterId: SEED_USERS.student.id,
    requesterName: SEED_USERS.student.name,
    requesterEmail: SEED_USERS.student.email,
    requesterRole: 'STUDENT',
    resourceType: 'ROOM',
    resourceId: 'a0000001-0000-4000-8000-000000000001',
    resourceName: 'Room A-101',
    bookingDate: '2026-11-12',
    startTime: '16:00',
    endTime: '18:00',
    expectedAttendees: 40,
    createdAt: '2026-11-08T10:15:00Z',
    adminNote: 'Approved. Key will be with floor supervisor.',
  },
  {
    id: 'bk000002-0000-4000-8000-000000000002',
    bookingNumber: 'JKLU-BK-2026-1002',
    title: 'Faculty Academic Council Meeting',
    description: 'Quarterly curriculum revision review with Dean',
    status: 'APPROVED',
    requesterId: SEED_USERS.staff.id,
    requesterName: SEED_USERS.staff.name,
    requesterEmail: SEED_USERS.staff.email,
    requesterRole: 'STAFF',
    resourceType: 'ROOM',
    resourceId: 'a0000007-0000-4000-8000-000000000007',
    resourceName: 'Room A-301 (Seminar Hall)',
    bookingDate: '2026-11-13',
    startTime: '14:00',
    endTime: '17:00',
    expectedAttendees: 35,
    createdAt: '2026-11-09T09:00:00Z',
    adminNote: 'Approved. Projector and recording enabled.',
  },
  {
    id: 'bk000003-0000-4000-8000-000000000003',
    bookingNumber: 'JKLU-BK-2026-1003',
    title: 'Annual Cultural Fest Inaugural Ceremony',
    description: 'Stage lighting rehearsal and student dance trials',
    status: 'PENDING',
    requesterId: SEED_USERS.student.id,
    requesterName: SEED_USERS.student.name,
    requesterEmail: SEED_USERS.student.email,
    requesterRole: 'STUDENT',
    resourceType: 'VENUE',
    resourceId: 'v0000001-0000-4000-8000-000000000001',
    resourceName: 'Rabindranath Tagore Amphitheatre',
    bookingDate: '2026-11-18',
    startTime: '17:00',
    endTime: '21:00',
    expectedAttendees: 250,
    createdAt: '2026-11-10T14:30:00Z',
    agentReasoning: 'No timetable clash detected on OAT. Sound permissions required past 20:00.',
    conflictCheckResult: {
      hasClash: false,
      summary: 'Zero resource conflicts detected. Capacity 350 exceeds 250 attendees safely.',
    },
  },
  {
    id: 'bk000004-0000-4000-8000-000000000004',
    bookingNumber: 'JKLU-BK-2026-1004',
    title: 'Distinguished Guest Speaker: Cloud Native Architecture',
    description: 'Session by VP of Engineering from Delhi. Stay in MDC + parking needed.',
    status: 'PENDING',
    requesterId: SEED_USERS.staff.id,
    requesterName: SEED_USERS.staff.name,
    requesterEmail: SEED_USERS.staff.email,
    requesterRole: 'STAFF',
    resourceType: 'MULTI_RESOURCE',
    resourceId: 'a0000004-0000-4000-8000-000000000004',
    resourceName: 'Room A-201',
    bookingDate: '2026-11-19',
    startTime: '15:00',
    endTime: '17:00',
    expectedAttendees: 80,
    guestHouseRoomId: 'gh000001-0000-4000-8000-000000000001',
    guestHouseRoomName: 'MDC-101 (Deluxe)',
    guestHouseCheckIn: '2026-11-18',
    guestHouseCheckOut: '2026-11-20',
    parkingRequested: true,
    parkingZoneName: 'Zone P1 (Main Gate)',
    createdAt: '2026-11-11T08:00:00Z',
    agentReasoning:
      'Agent coordinated multi-resource allocation: Room A-201 (capacity 80), MDC-101 (Deluxe 2-night stay), Zone P1 slot P1-04.',
    conflictCheckResult: {
      hasClash: false,
      summary: 'Room A-201 free. MDC-101 free for 18-20 Nov. Parking Zone P1 available.',
    },
  },
  {
    id: 'bk000005-0000-4000-8000-000000000005',
    bookingNumber: 'JKLU-BK-2026-1005',
    title: 'Late Night LAN Gaming Tournament',
    description: 'Inter-hostel e-sports competition',
    status: 'REJECTED',
    requesterId: SEED_USERS.student.id,
    requesterName: SEED_USERS.student.name,
    requesterEmail: SEED_USERS.student.email,
    requesterRole: 'STUDENT',
    resourceType: 'VENUE',
    resourceId: 'v0000003-0000-4000-8000-000000000003',
    resourceName: 'Advanced Computing & AI Lab',
    bookingDate: '2026-11-07',
    startTime: '22:00',
    endTime: '04:00',
    expectedAttendees: 40,
    createdAt: '2026-11-05T12:00:00Z',
    adminNote: 'Labs cannot be booked past 20:00 without explicit faculty supervisor present.',
  },
  {
    id: 'bk000006-0000-4000-8000-000000000006',
    bookingNumber: 'JKLU-BK-2026-1006',
    title: 'Design Club Typography Workshop',
    description: 'Hands-on lettering and poster illustration',
    status: 'CANCELLED',
    requesterId: SEED_USERS.student.id,
    requesterName: SEED_USERS.student.name,
    requesterEmail: SEED_USERS.student.email,
    requesterRole: 'STUDENT',
    resourceType: 'ROOM',
    resourceId: 'c0000001-0000-4000-8000-000000000018',
    resourceName: 'Room C-101',
    bookingDate: '2026-11-10',
    startTime: '16:00',
    endTime: '18:00',
    expectedAttendees: 30,
    createdAt: '2026-11-06T11:20:00Z',
    adminNote: 'Cancelled by requester due to rescheduled mid-terms.',
  },
  // Additional 14 synthetic bookings to hit 20 total
  ...Array.from({ length: 14 }).map((_, i) => {
    const num = i + 7;
    const isApproved = i % 3 === 0;
    const isPending = i % 3 === 1;
    const status: 'APPROVED' | 'PENDING' | 'REJECTED' = isApproved
      ? 'APPROVED'
      : isPending
        ? 'PENDING'
        : 'REJECTED';
    const day = 12 + (i % 12);
    const room = SEED_ROOMS[i % SEED_ROOMS.length];
    return {
      id: `bk0000${num < 10 ? '0' : ''}${num}-0000-4000-8000-0000000000${num < 10 ? '0' : ''}${num}`,
      bookingNumber: `JKLU-BK-2026-10${num < 10 ? '0' : ''}${num}`,
      title: `Event Booking ${num}: ${room.block} Workshop`,
      description: `Academic and technical seminar session for department students`,
      status,
      requesterId: i % 2 === 0 ? SEED_USERS.student.id : SEED_USERS.staff.id,
      requesterName: i % 2 === 0 ? SEED_USERS.student.name : SEED_USERS.staff.name,
      requesterEmail: i % 2 === 0 ? SEED_USERS.student.email : SEED_USERS.staff.email,
      requesterRole: (i % 2 === 0 ? 'STUDENT' : 'STAFF') as 'STUDENT' | 'STAFF',
      resourceType: 'ROOM' as const,
      resourceId: room.id,
      resourceName: `Room ${room.roomNumber}`,
      bookingDate: `2026-11-${day < 10 ? '0' : ''}${day}`,
      startTime: '14:00',
      endTime: '16:00',
      expectedAttendees: 30 + (i * 2),
      createdAt: `2026-11-08T${10 + (i % 8)}:00:00Z`,
      adminNote: status === 'APPROVED' ? 'Approved by Admin office.' : status === 'REJECTED' ? 'Clash with existing seminar schedule.' : undefined,
      conflictCheckResult: {
        hasClash: status === 'REJECTED',
        summary: status === 'REJECTED' ? 'Clashed with Departmental meeting.' : 'No timetable clash detected.',
      },
    };
  }),
];

// ==========================================
// 8. 60 AUDIT LOG ENTRIES
// ==========================================
const AUDIT_EVENT_TYPES = [
  'AGENT_REASONING',
  'TOOL_CALL',
  'API_CALL',
  'APPROVAL',
  'REJECTION',
  'LOGIN',
  'DATA_CHANGE',
] as const;

export const SEED_AUDIT_LOGS: AuditLogEntry[] = Array.from({ length: 60 }).map((_, i) => {
  const num = i + 1;
  const eventType = AUDIT_EVENT_TYPES[i % AUDIT_EVENT_TYPES.length];
  const user = i % 3 === 0 ? SEED_USERS.admin : i % 3 === 1 ? SEED_USERS.staff : SEED_USERS.student;
  const hour = (9 + (i % 12)).toString().padStart(2, '0');
  const minute = ((i * 7) % 60).toString().padStart(2, '0');

  let action = '';
  let details: Record<string, unknown> = {};

  switch (eventType) {
    case 'LOGIN':
      action = `User authenticated successfully via session`;
      details = { userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64)', role: user.role };
      break;
    case 'AGENT_REASONING':
      action = `CampusFlow Agent evaluated multi-resource availability query`;
      details = {
        query: 'Guest lecture next Thursday 3-5 PM, 80 people, speaker from Delhi',
        consideredRooms: ['C-204', 'A-201', 'A-301'],
        decision: 'Detected Sem 3 clash on C-204; recommended A-201 and MDC-101.',
      };
      break;
    case 'TOOL_CALL':
      action = `Agent invoked tool: checkRoomAvailability`;
      details = {
        tool: 'checkRoomAvailability',
        args: { date: '2026-11-19', startTime: '15:00', endTime: '17:00', minCapacity: 80 },
        result: { availableRoomsCount: 2, clashingRoomsCount: 1 },
        durationMs: 142,
      };
      break;
    case 'API_CALL':
      action = `HTTP GET /api/rooms/availability`;
      details = { method: 'GET', path: '/api/rooms/availability', status: 200, latencyMs: 38 };
      break;
    case 'APPROVAL':
      action = `Admin approved booking request JKLU-BK-2026-100${(i % 5) + 1}`;
      details = {
        bookingId: `bk00000${(i % 5) + 1}-0000-4000-8000-00000000000${(i % 5) + 1}`,
        note: 'Approved after timetable verification.',
      };
      break;
    case 'REJECTION':
      action = `Admin rejected booking request JKLU-BK-2026-1005`;
      details = { reason: 'Unauthorized after-hours lab usage requested without faculty.' };
      break;
    case 'DATA_CHANGE':
      action = `Admin updated room feature flags for Room A-101`;
      details = { modifiedField: 'features', added: 'smartboard', updatedBy: user.email };
      break;
  }

  return {
    id: `aud00000-0000-4000-8000-${num.toString().padStart(12, '0')}`,
    timestamp: `2026-11-${(10 - Math.floor(i / 15)).toString().padStart(2, '0')}T${hour}:${minute}:00Z`,
    actor: user.name,
    actorRole: user.role,
    eventType,
    action,
    details,
    ipAddress: `172.16.${(i % 5) + 1}.${(i * 3) % 250}`,
  };
});

// ==========================================
// 9. ANNOUNCEMENT STRIPE SEED
// ==========================================
export const SEED_ANNOUNCEMENT: Announcement = {
  id: 'ann-00000000-0000-4000-8000-000000000001',
  message: 'Admissions peak week: book venues early',
  linkText: 'See booking rules',
  linkHref: '/rules',
  active: true,
};
