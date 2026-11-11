import { apiClient } from './client';
import {
  ParkingZone,
  ParkingZonesResponseSchema,
  VisitorParkingRequest,
  ParkingPermitResponse,
  ParkingPermitResponseSchema,
} from './types';

export const parkingApi = {
  getZones: async (): Promise<ParkingZone[]> => {
    return apiClient<ParkingZone[]>('/parking/zones', {
      method: 'GET',
      schema: ParkingZonesResponseSchema,
    });
  },

  requestPermit: async (data: VisitorParkingRequest): Promise<ParkingPermitResponse> => {
    return apiClient<ParkingPermitResponse>('/parking/permits', {
      method: 'POST',
      body: JSON.stringify(data),
      schema: ParkingPermitResponseSchema,
    });
  },
};

