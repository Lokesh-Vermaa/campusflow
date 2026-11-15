import { describe, it, expect } from 'vitest';
import {
  UserSchema,
  LoginRequestSchema,
  CreateBookingRequestSchema,
  ApiErrorSchema,
} from './types';

describe('Zod Schemas Validation at Boundary', () => {
  it('validates a correct User object', () => {
    const validUser = {
      id: '33333333-3333-4333-8333-333333333333',
      email: 'admin@demo.jklu.test',
      name: 'Rajesh Patel',
      role: 'ADMIN',
      department: 'Campus Administration',
      avatarUrl: null,
    };
    expect(() => UserSchema.parse(validUser)).not.toThrow();
  });

  it('rejects an invalid role on User', () => {
    const invalidUser = {
      id: '33333333-3333-4333-8333-333333333333',
      email: 'admin@demo.jklu.test',
      name: 'Rajesh Patel',
      role: 'SUPERADMIN', // Not in RoleEnum
      department: 'Campus Administration',
    };
    expect(() => UserSchema.parse(invalidUser)).toThrow();
  });

  it('validates login request schema and rejects invalid email', () => {
    expect(() =>
      LoginRequestSchema.parse({
        email: 'invalid-email',
        password: 'dummy_secret_pass',
      }),
    ).toThrow();

    expect(() =>
      LoginRequestSchema.parse({
        email: 'student@demo.jklu.test',
        password: '123', // < 6 chars
      }),
    ).toThrow();
  });

  it('enforces booking end time after start time', () => {
    const validBooking = {
      title: 'Robotics Workshop',
      resourceType: 'ROOM' as const,
      resourceId: 'a0000001-0000-4000-8000-000000000001',
      bookingDate: '2026-11-20',
      startTime: '10:00',
      endTime: '12:00',
      expectedAttendees: 30,
    };
    expect(() => CreateBookingRequestSchema.parse(validBooking)).not.toThrow();

    const invalidTiming = {
      ...validBooking,
      startTime: '14:00',
      endTime: '13:00', // End before start
    };
    expect(() => CreateBookingRequestSchema.parse(invalidTiming)).toThrow(
      'End time must be after start time',
    );
  });

  it('parses standardized API error format', () => {
    const errorPayload = {
      code: 'ROOM_UNAVAILABLE',
      message: 'Room is occupied by class.',
      requestId: 'req-12345',
    };
    const parsed = ApiErrorSchema.parse(errorPayload);
    expect(parsed.code).toBe('ROOM_UNAVAILABLE');
    expect(parsed.requestId).toBe('req-12345');
  });
});

