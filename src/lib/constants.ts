// Workspace navigation items
export const WORKSPACE_NAV_ITEMS = [
  { label: 'Chat', href: '/', icon: 'MessageSquare', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
  { label: 'Find a Room', href: '/rooms', icon: 'Search', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
  { label: 'Venues & Labs', href: '/venues', icon: 'Building2', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
  { label: 'Guest House', href: '/guest-house', icon: 'Home', roles: ['STAFF', 'ADMIN'] },
  { label: 'Parking', href: '/parking', icon: 'Car', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
  { label: 'My Bookings', href: '/bookings', icon: 'Calendar', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
  { label: 'Overview', href: '/overview', icon: 'LayoutDashboard', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
] as const;

// Admin navigation items
export const ADMIN_NAV_ITEMS = [
  { label: 'Approvals', href: '/admin/approvals', icon: 'CheckSquare', roles: ['ADMIN'], badge: true },
  { label: 'Timetable Upload', href: '/admin/timetable', icon: 'UploadCloud', roles: ['ADMIN'] },
  { label: 'Master Data', href: '/admin/master-data', icon: 'Database', roles: ['ADMIN'] },
  { label: 'Audit Log', href: '/admin/audit', icon: 'ShieldAlert', roles: ['ADMIN'] },
  { label: 'Reports', href: '/admin/reports', icon: 'BarChart3', roles: ['ADMIN'] },
] as const;

// User roles
export enum ROLES {
  STUDENT = 'STUDENT',
  STAFF = 'STAFF',
  ADMIN = 'ADMIN',
}

// Booking statuses
export const BOOKING_STATUSES = {
  PENDING: 'PENDING',
  CONFIRMED: 'CONFIRMED',
  CANCELLED: 'CANCELLED',
  COMPLETED: 'COMPLETED',
  REJECTED: 'REJECTED',
} as const;

export type BookingStatus = (typeof BOOKING_STATUSES)[keyof typeof BOOKING_STATUSES];

// Resource types
export const RESOURCE_TYPES = {
  VENUE: 'VENUE',
  GUEST_HOUSE: 'GUEST_HOUSE',
  PARKING: 'PARKING',
  ROOM: 'ROOM',
} as const;

export type ResourceType = (typeof RESOURCE_TYPES)[keyof typeof RESOURCE_TYPES];
