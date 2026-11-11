import { apiClient, clearCsrfToken } from './client';
import {
  User,
  UserSchema,
  LoginRequest,
  LoginResponse,
  LoginResponseSchema,
  CsrfResponse,
  CsrfResponseSchema,
} from './types';

export const authApi = {
  getMe: async (): Promise<User> => {
    const res = await apiClient<{ user: User }>('/auth/me', {
      method: 'GET',
    });
    return UserSchema.parse(res.user);
  },

  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    return apiClient<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
      schema: LoginResponseSchema,
    });
  },

  logout: async (): Promise<{ success: boolean }> => {
    const res = await apiClient<{ success: boolean }>('/auth/logout', {
      method: 'POST',
    });
    clearCsrfToken();
    return res;
  },

  getCsrf: async (): Promise<CsrfResponse> => {
    return apiClient<CsrfResponse>('/auth/csrf', {
      method: 'GET',
      schema: CsrfResponseSchema,
    });
  },
};

