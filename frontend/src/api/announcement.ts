import { apiClient } from './client';
import { Announcement, AnnouncementSchema } from './types';

export const announcementApi = {
  get: async (): Promise<Announcement> => {
    return apiClient<Announcement>('/content/announcement', {
      method: 'GET',
      schema: AnnouncementSchema,
    });
  },
};

