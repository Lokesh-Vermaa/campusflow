import { apiClient } from './client';
import {
  AvailabilityResponse,
  AvailabilityResponseSchema,
  RoomScheduleResponse,
  RoomScheduleResponseSchema,
} from './types';

export interface RoomSearchParams {
  date: string;
  startTime: string;
  endTime: string;
  minCapacity?: number;
  blockId?: string;
  features?: string;
}

export const roomsApi = {
  searchAvailability: async (params: RoomSearchParams): Promise<AvailabilityResponse> => {
    return apiClient<AvailabilityResponse>('/rooms/availability', {
      method: 'GET',
      params: {
        date: params.date,
        startTime: params.startTime,
        endTime: params.endTime,
        minCapacity: params.minCapacity,
        blockId: params.blockId,
        features: params.features,
      },
      schema: AvailabilityResponseSchema,
    });
  },

  getRoomSchedule: async (roomId: string, date: string): Promise<RoomScheduleResponse> => {
    return apiClient<RoomScheduleResponse>(`/rooms/${roomId}/schedule`, {
      method: 'GET',
      params: { date },
      schema: RoomScheduleResponseSchema,
    });
  },
};
