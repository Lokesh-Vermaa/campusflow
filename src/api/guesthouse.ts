import { apiClient } from './client';
import { GuestHouseRoom, GuestHouseRoomsResponseSchema } from './types';

export interface GuestHouseSearchParams {
  checkIn?: string;
  checkOut?: string;
}

export const guestHouseApi = {
  getRooms: async (params?: GuestHouseSearchParams): Promise<GuestHouseRoom[]> => {
    return apiClient<GuestHouseRoom[]>('/guesthouse/rooms', {
      method: 'GET',
      params: {
        checkIn: params?.checkIn,
        checkOut: params?.checkOut,
      },
      schema: GuestHouseRoomsResponseSchema,
    });
  },
};
