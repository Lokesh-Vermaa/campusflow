import { describe, it, expect } from 'vitest';
import {
  timeToMinutes,
  minutesToTime,
  isIntervalOverlap,
  computeAvailability,
  calculateFreeWindows,
} from './availabilityEngine';

describe('Availability Engine (Interval Overlap & Scheduling)', () => {
  describe('Time conversion utilities', () => {
    it('converts HH:mm to minutes from midnight', () => {
      expect(timeToMinutes('00:00')).toBe(0);
      expect(timeToMinutes('09:30')).toBe(570);
      expect(timeToMinutes('15:00')).toBe(900);
      expect(timeToMinutes('23:59')).toBe(1439);
    });

    it('converts minutes to HH:mm string', () => {
      expect(minutesToTime(0)).toBe('00:00');
      expect(minutesToTime(570)).toBe('09:30');
      expect(minutesToTime(900)).toBe('15:00');
    });
  });

  describe('Interval Overlap Logic', () => {
    it('detects overlapping intervals correctly', () => {
      // Partial overlap: 14:00-16:00 and 15:00-17:00
      expect(isIntervalOverlap('14:00', '16:00', '15:00', '17:00')).toBe(true);

      // Contained interval: 14:00-17:00 and 15:00-16:00
      expect(isIntervalOverlap('14:00', '17:00', '15:00', '16:00')).toBe(true);

      // Identical interval
      expect(isIntervalOverlap('10:00', '11:00', '10:00', '11:00')).toBe(true);
    });

    it('returns false for adjacent/touching intervals (no overlap)', () => {
      // 09:00-10:00 and 10:00-11:00 touch at 10:00 but do not overlap
      expect(isIntervalOverlap('09:00', '10:00', '10:00', '11:00')).toBe(false);
      expect(isIntervalOverlap('14:00', '15:00', '13:00', '14:00')).toBe(false);
    });

    it('returns false for completely disjoint intervals', () => {
      expect(isIntervalOverlap('09:00', '10:00', '14:00', '15:00')).toBe(false);
    });
  });

  describe('Free Windows Calculation', () => {
    it('returns full day window when no busy slots exist', () => {
      const windows = calculateFreeWindows([], '08:00', '20:00');
      expect(windows).toEqual([{ startTime: '08:00', endTime: '20:00' }]);
    });

    it('calculates free windows surrounding a middle class slot', () => {
      const busySlots = [
        { slot: '10:00-12:00', type: 'CLASS' as const, title: 'Data Structures' },
      ];
      const windows = calculateFreeWindows(busySlots, '08:00', '18:00');
      expect(windows).toEqual([
        { startTime: '08:00', endTime: '10:00' },
        { startTime: '12:00', endTime: '18:00' },
      ]);
    });
  });

  describe('Deterministic Seed Timetable Clashing', () => {
    it('detects Room C-204 clash on Thursday 15:00-16:00 with Sem 3 Data Structures', () => {
      // 2026-11-19 is a Thursday (Day 4)
      const res = computeAvailability({
        date: '2026-11-19',
        startTime: '15:00',
        endTime: '16:00',
      });

      const c204Unavailable = res.unavailableRooms.find((r) => r.roomNumber === 'C-204');
      expect(c204Unavailable).toBeDefined();
      expect(c204Unavailable?.conflictReason).toContain('Data Structures');
      expect(c204Unavailable?.conflictReason).toContain('Sem 3');
    });

    it('finds free rooms matching capacity requirement on Friday morning', () => {
      // 2026-11-20 is a Friday
      const res = computeAvailability({
        date: '2026-11-20',
        startTime: '08:30',
        endTime: '09:30',
        minCapacity: 70,
      });

      expect(res.availableRooms.length).toBeGreaterThan(0);
      res.availableRooms.forEach((r) => {
        expect(r.capacity).toBeGreaterThanOrEqual(70);
      });
    });
  });
});

