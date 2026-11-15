import { apiClient } from './client';
import { z } from 'zod';
import {
  ApproveBookingRequestSchema,
  PaginatedBookingsResponse,
  PaginatedBookingsResponseSchema,
  RejectBookingRequestSchema,
} from './types';

// Simple response schema that matches what the backend actually returns
// for approve/reject: { id, status, message }
const ApprovalActionResponseSchema = z.object({
  id: z.string(),
  status: z.string(),
  message: z.string(),
});
type ApprovalActionResponse = z.infer<typeof ApprovalActionResponseSchema>;

export interface TimetableUploadResponse {
  totalRows: number;
  validRows: number;
  errorRows: Array<{
    rowNumber: number;
    course: string;
    room: string;
    error: string;
  }>;
  preview: Array<{
    rowNumber: number;
    semester: string;
    courseCode: string;
    courseName: string;
    instructor: string;
    roomNumber: string;
    dayOfWeek: string;
    startTime: string;
    endTime: string;
  }>;
}

export const adminApi = {
  getApprovals: async (params?: { page?: number; pageSize?: number }): Promise<PaginatedBookingsResponse> => {
    return apiClient<PaginatedBookingsResponse>('/admin/approvals', {
      method: 'GET',
      params: {
        page: params?.page ?? 1,
        pageSize: params?.pageSize ?? 10,
      },
      schema: PaginatedBookingsResponseSchema,
    });
  },

  approveBooking: async (bookingId: string, note?: string): Promise<ApprovalActionResponse> => {
    const payload = ApproveBookingRequestSchema.parse({ note });
    return apiClient<ApprovalActionResponse>(`/admin/approvals/${bookingId}/approve`, {
      method: 'POST',
      body: JSON.stringify(payload),
      schema: ApprovalActionResponseSchema,
    });
  },

  rejectBooking: async (bookingId: string, reason: string): Promise<ApprovalActionResponse> => {
    const payload = RejectBookingRequestSchema.parse({ reason });
    return apiClient<ApprovalActionResponse>(`/admin/approvals/${bookingId}/reject`, {
      method: 'POST',
      body: JSON.stringify(payload),
      schema: ApprovalActionResponseSchema,
    });
  },

  uploadTimetable: async (file: File): Promise<TimetableUploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient<TimetableUploadResponse>('/admin/timetable/upload', {
      method: 'POST',
      body: formData,
    });
  },

  publishTimetable: async (semester: string, entries: unknown[]): Promise<{ publishedCount: number; message: string }> => {
    return apiClient<{ publishedCount: number; message: string }>('/admin/timetable/publish', {
      method: 'POST',
      body: JSON.stringify({ semester, entries }),
    });
  },

  getMasterData: async (resource: 'blocks' | 'rooms' | 'guesthouse' | 'parking'): Promise<unknown[]> => {
    return apiClient<unknown[]>(`/admin/master/${resource}`, {
      method: 'GET',
    });
  },
};

