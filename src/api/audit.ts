import { apiClient } from './client';
import { PaginatedAuditResponse, PaginatedAuditResponseSchema } from './types';

export interface AuditSearchParams {
  actor?: string;
  eventType?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  pageSize?: number;
}

export const auditApi = {
  getLogs: async (params?: AuditSearchParams): Promise<PaginatedAuditResponse> => {
    return apiClient<PaginatedAuditResponse>('/admin/audit', {
      method: 'GET',
      params: {
        actor: params?.actor,
        eventType: params?.eventType,
        startDate: params?.startDate,
        endDate: params?.endDate,
        page: params?.page ?? 1,
        pageSize: params?.pageSize ?? 10,
      },
      schema: PaginatedAuditResponseSchema,
    });
  },
};

