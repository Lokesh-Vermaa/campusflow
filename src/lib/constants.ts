// Navigation items
export const NAV_ITEMS = [
  { label: 'DASHBOARD', href: '/dashboard', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
  { label: 'FIND A ROOM', href: '/finder', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
  { label: 'VENUES', href: '/venues', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
  { label: 'GUEST HOUSE', href: '/guesthouse', roles: ['STAFF', 'ADMIN'] },
  { label: 'PARKING', href: '/parking', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
  { label: 'MY BOOKINGS', href: '/bookings', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
  { label: 'AGENT', href: '/agent', roles: ['STUDENT', 'STAFF', 'ADMIN'] },
  { label: 'ADMIN', href: '/admin/approvals', roles: ['ADMIN'] },
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

// Announcement (static — Phase 1 will fetch from API)
export const DEFAULT_ANNOUNCEMENT = {
  message: 'Seminar Hall bookings are now open for the upcoming AI Buildathon.',
  linkText: 'Book Now',
  linkHref: '/venues',
};
