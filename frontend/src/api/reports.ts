import { apiClient } from './client';
import { UtilisationReport, UtilisationReportSchema } from './types';

export const reportsApi = {
  getUtilisation: async (params?: { startDate?: string; endDate?: string }): Promise<UtilisationReport> => {
    return apiClient<UtilisationReport>('/admin/reports/utilisation', {
      method: 'GET',
      params: {
        startDate: params?.startDate,
        endDate: params?.endDate,
      },
      schema: UtilisationReportSchema,
    });
  },
};

