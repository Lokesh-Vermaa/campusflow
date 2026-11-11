// =========================================================================
// MOCK AVAILABILITY ENGINE
// "mock only, replaced by backend"
// This deterministic in-memory logic computes availability from the seeded
// semester timetable and approved bookings using true interval-overlap checks.
// =========================================================================

import { AvailableRoom, UnavailableRoom, TimeWindow, RoomTimelineSlot } from '@/api/types';
import { SEED_ROOMS, SEED_TIMETABLE, SEED_BOOKINGS, TimetableSlot } from './seedData';

/**
 * Convert HH:mm to minutes from midnight
 */
export function timeToMinutes(timeStr: string): number {
  const [h, m] = timeStr.split(':').map(Number);
  return h * 60 + m;
}

/**
 * Convert minutes from midnight to HH:mm
 */
export function minutesToTime(mins: number): string {
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
}

/**
 * Check if two time intervals [startA, endA] and [startB, endB] overlap
 * Overlap occurs if max(startA, startB) < min(endA, endB)
 */
export function isIntervalOverlap(
  startA: string,
  endA: string,
  startB: string,
  endB: string,
): boolean {
  const sA = timeToMinutes(startA);
  const eA = timeToMinutes(endA);
  const sB = timeToMinutes(startB);
  const eB = timeToMinutes(endB);
  return Math.max(sA, sB) < Math.min(eA, eB);
}

/**
 * Get day of week for a date string YYYY-MM-DD
 * Returns 1 for Monday through 7 for Sunday (ISO standard)
 */
export function getDayOfWeek(dateStr: string): number {
  const date = new Date(dateStr + 'T00:00:00');
  const day = date.getDay(); // 0 is Sunday, 1 is Monday ... 6 is Saturday
  return day === 0 ? 7 : day;
}

export interface EngineSearchOptions {
  date: string;       // YYYY-MM-DD
  startTime: string;  // HH:mm
  endTime: string;    // HH:mm
  minCapacity?: number;
  blockId?: string;
  features?: string[];
}

/**
 * Compute availability for all seeded rooms on a given date and time window
 */
export function computeAvailability(options: EngineSearchOptions): {
  availableRooms: AvailableRoom[];
  unavailableRooms: UnavailableRoom[];
} {
  const { date, startTime, endTime, minCapacity = 0, features = [] } = options;
  const dayOfWeek = getDayOfWeek(date);

  const availableRooms: AvailableRoom[] = [];
  const unavailableRooms: UnavailableRoom[] = [];

  // Filter eligible rooms by capacity, block, features
  let candidateRooms = SEED_ROOMS.filter((room) => room.capacity >= minCapacity);

  if (options.blockId) {
    candidateRooms = candidateRooms.filter(
      (room) => room.block.toLowerCase().includes(options.blockId!.toLowerCase()),
    );
  }

  if (features.length > 0) {
    candidateRooms = candidateRooms.filter((room) =>
      features.every((reqFeat) => room.features.includes(reqFeat)),
    );
  }

  for (const room of candidateRooms) {
    // 1. Find timetable slots for this room on this day of week
    const timetableClashes: TimetableSlot[] = SEED_TIMETABLE.filter(
      (slot) => slot.roomId === room.id && slot.dayOfWeek === dayOfWeek,
    );

    // 2. Find approved bookings for this room on this date
    const bookingClashes = SEED_BOOKINGS.filter(
      (b) => b.resourceId === room.id && b.bookingDate === date && b.status === 'APPROVED',
    );

    // 3. Check for specific overlaps during [startTime, endTime]
    const conflictingClass = timetableClashes.find((slot) =>
      isIntervalOverlap(startTime, endTime, slot.startTime, slot.endTime),
    );

    const conflictingBooking = bookingClashes.find((b) =>
      isIntervalOverlap(startTime, endTime, b.startTime, b.endTime),
    );

    // Build timeline for this room on this day
    const timeline: RoomTimelineSlot[] = [];
    timetableClashes.forEach((c) => {
      timeline.push({
        slot: `${c.startTime}-${c.endTime}`,
        type: 'CLASS',
        title: `${c.semester}: ${c.courseName}`,
      });
    });
    bookingClashes.forEach((b) => {
      timeline.push({
        slot: `${b.startTime}-${b.endTime}`,
        type: 'BOOKING',
        title: b.title,
      });
    });
    // Sort timeline by start time
    timeline.sort((a, b) => timeToMinutes(a.slot.split('-')[0]) - timeToMinutes(b.slot.split('-')[0]));

    if (conflictingClass) {
      unavailableRooms.push({
        id: room.id,
        roomNumber: room.roomNumber,
        block: room.block,
        floor: room.floor,
        capacity: room.capacity,
        type: room.type,
        features: room.features,
        conflictReason: `Room ${room.roomNumber} is occupied by ${conflictingClass.semester} ${conflictingClass.courseName} ${conflictingClass.startTime}-${conflictingClass.endTime}`,
        timeline,
      });
    } else if (conflictingBooking) {
      unavailableRooms.push({
        id: room.id,
        roomNumber: room.roomNumber,
        block: room.block,
        floor: room.floor,
        capacity: room.capacity,
        type: room.type,
        features: room.features,
        conflictReason: `Room ${room.roomNumber} is reserved for "${conflictingBooking.title}" ${conflictingBooking.startTime}-${conflictingBooking.endTime}`,
        timeline,
      });
    } else {
      // Room is free during requested interval! Calculate free windows between 08:00 and 20:00
      const freeWindows = calculateFreeWindows(timeline, '08:00', '20:00');

      availableRooms.push({
        id: room.id,
        roomNumber: room.roomNumber,
        block: room.block,
        floor: room.floor,
        capacity: room.capacity,
        type: room.type,
        features: room.features,
        bookableOutside: room.bookableOutside,
        freeWindows,
      });
    }
  }

  return { availableRooms, unavailableRooms };
}

/**
 * Calculate available non-overlapping time windows given a list of occupied slots
 */
export function calculateFreeWindows(
  busySlots: RoomTimelineSlot[],
  dayStart = '08:00',
  dayEnd = '20:00',
): TimeWindow[] {
  const startMins = timeToMinutes(dayStart);
  const endMins = timeToMinutes(dayEnd);

  if (busySlots.length === 0) {
    return [{ startTime: dayStart, endTime: dayEnd }];
  }

  const occupiedIntervals = busySlots
    .map((s) => {
      const [start, end] = s.slot.split('-');
      return {
        start: timeToMinutes(start),
        end: timeToMinutes(end),
      };
    })
    .sort((a, b) => a.start - b.start);

  const freeWindows: TimeWindow[] = [];
  let currentCursor = startMins;

  for (const interval of occupiedIntervals) {
    if (interval.start > currentCursor) {
      freeWindows.push({
        startTime: minutesToTime(currentCursor),
        endTime: minutesToTime(interval.start),
      });
    }
    currentCursor = Math.max(currentCursor, interval.end);
  }

  if (currentCursor < endMins) {
    freeWindows.push({
      startTime: minutesToTime(currentCursor),
      endTime: minutesToTime(endMins),
    });
  }

  return freeWindows;
}
