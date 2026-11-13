import { apiClient } from './client';
import { Conversation, PaginatedConversationsResponse } from './types';

export const conversationsApi = {
  /** List conversations for the current user (paginated) */
  list: async (page = 1, pageSize = 50): Promise<PaginatedConversationsResponse> => {
    const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) });
    return apiClient<PaginatedConversationsResponse>(`/conversations?${params}`, { method: 'GET' });
  },

  /** Create a new conversation */
  create: async (firstMessage?: string): Promise<Conversation> => {
    return apiClient<Conversation>('/conversations', {
      method: 'POST',
      body: JSON.stringify({ firstMessage }),
    });
  },

  /** Get a single conversation by id */
  get: async (id: string): Promise<Conversation> => {
    return apiClient<Conversation>(`/conversations/${id}`, { method: 'GET' });
  },

  /** Rename a conversation */
  rename: async (id: string, title: string): Promise<Conversation> => {
    return apiClient<Conversation>(`/conversations/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ title }),
    });
  },

  /** Delete a conversation */
  delete: async (id: string): Promise<void> => {
    return apiClient<void>(`/conversations/${id}`, { method: 'DELETE' });
  },
};
