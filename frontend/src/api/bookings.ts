import { apiClient } from './client';
import {
  Booking,
  BookingSchema,
  CreateBookingRequest,
  PaginatedBookingsResponse,
  PaginatedBookingsResponseSchema,
} from './types';

export interface BookingListParams {
  status?: string;
  page?: number;
  pageSize?: number;
}

export const bookingsApi = {
  create: async (data: CreateBookingRequest, idempotencyKey?: string): Promise<Booking> => {
    const key = idempotencyKey || crypto.randomUUID();
    return apiClient<Booking>('/bookings', {
      method: 'POST',
      headers: {
        'Idempotency-Key': key,
        'X-Idempotency-Key': key,
      },
      body: JSON.stringify(data),
      schema: BookingSchema,
    });
  },

  getMyBookings: async (params?: BookingListParams): Promise<PaginatedBookingsResponse> => {
    return apiClient<PaginatedBookingsResponse>('/bookings/my', {
      method: 'GET',
      params: {
        status: params?.status,
        page: params?.page ?? 1,
        pageSize: params?.pageSize ?? 10,
      },
      schema: PaginatedBookingsResponseSchema,
    });
  },

  cancel: async (bookingId: string): Promise<Booking> => {
    return apiClient<Booking>(`/bookings/${bookingId}/cancel`, {
      method: 'POST',
      schema: BookingSchema,
    });
  },
};

