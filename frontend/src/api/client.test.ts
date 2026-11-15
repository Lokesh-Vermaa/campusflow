import { describe, it, expect, vi, beforeEach } from 'vitest';
import { apiClient, onAuthExpired } from './client';
import { ApiError } from './types';

describe('API Client & Error Mapping', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('successfully returns JSON data on 200 OK', async () => {
    const mockData = { id: 'test-1', name: 'Test Room' };
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockData,
    } as Response);

    const result = await apiClient<typeof mockData>('/rooms/test');
    expect(result).toEqual(mockData);
  });

  it('triggers auth expired notification on 401 Unauthorized', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 401,
      json: async () => ({ message: 'Unauthorized' }),
    } as Response);

    let expiredNotified = false;
    const unsubscribe = onAuthExpired(() => {
      expiredNotified = true;
    });

    await expect(apiClient('/protected')).rejects.toThrow(ApiError);
    expect(expiredNotified).toBe(true);

    unsubscribe();
  });

  it('maps custom server error response with code and requestId', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 409,
      json: async () => ({
        code: 'SCHEDULE_CLASH',
        message: 'Room already booked for this slot',
        requestId: 'req-clash-99',
      }),
    } as Response);

    try {
      await apiClient('/bookings');
      expect.fail('Should have thrown an error');
    } catch (err: unknown) {
      expect(err).toBeInstanceOf(ApiError);
      const apiErr = err as ApiError;
      expect(apiErr.status).toBe(409);
      expect(apiErr.code).toBe('SCHEDULE_CLASH');
      expect(apiErr.requestId).toBe('req-clash-99');
      expect(apiErr.message).toBe('Room already booked for this slot');
    }
  });

  it('handles network failure gracefully as ApiError with code NETWORK_ERROR', async () => {
    vi.spyOn(global, 'fetch').mockRejectedValueOnce(new Error('Failed to fetch'));

    try {
      await apiClient('/network-test');
      expect.fail('Should have thrown');
    } catch (err: unknown) {
      expect(err).toBeInstanceOf(ApiError);
      const apiErr = err as ApiError;
      expect(apiErr.code).toBe('NETWORK_ERROR');
      expect(apiErr.status).toBe(0);
    }
  });
});

