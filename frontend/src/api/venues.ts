import { apiClient } from './client';
import { Venue, VenuesResponseSchema } from './types';

export const venuesApi = {
  getAll: async (): Promise<Venue[]> => {
    return apiClient<Venue[]>('/venues', {
      method: 'GET',
      schema: VenuesResponseSchema,
    });
  },
};

