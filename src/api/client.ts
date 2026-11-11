import { z } from 'zod';
import { ApiError, ApiErrorSchema } from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

interface RequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean | undefined | null>;
  schema?: z.ZodTypeAny;
  skipCsrf?: boolean;
}

let cachedCsrfToken: string | null = null;
let csrfPromise: Promise<string | null> | null = null;

/**
 * Fetch CSRF token from the server
 */
export async function getCsrfToken(): Promise<string | null> {
  if (cachedCsrfToken) return cachedCsrfToken;

  if (!csrfPromise) {
    csrfPromise = fetch(`${BASE_URL}/auth/csrf`, {
      method: 'GET',
      credentials: 'include',
    })
      .then(async (res) => {
        if (!res.ok) return null;
        const data = await res.json();
        cachedCsrfToken = data.token || null;
        return cachedCsrfToken;
      })
      .catch(() => null)
      .finally(() => {
        csrfPromise = null;
      });
  }

  return csrfPromise;
}

export function clearCsrfToken(): void {
  cachedCsrfToken = null;
}

export type AuthExpiredHandler = () => void;
const authExpiredListeners: AuthExpiredHandler[] = [];

export function onAuthExpired(handler: AuthExpiredHandler) {
  authExpiredListeners.push(handler);
  return () => {
    const idx = authExpiredListeners.indexOf(handler);
    if (idx !== -1) authExpiredListeners.splice(idx, 1);
  };
}

function notifyAuthExpired() {
  authExpiredListeners.forEach((fn) => {
    try {
      fn();
    } catch {
      // ignore
    }
  });
}

/**
 * Unified typed fetch wrapper
 */
export async function apiClient<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { params, schema, skipCsrf = false, headers: customHeaders, ...fetchOptions } = options;

  let url = `${BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null) {
        searchParams.append(key, String(val));
      }
    });
    const qs = searchParams.toString();
    if (qs) {
      url += (url.includes('?') ? '&' : '?') + qs;
    }
  }

  const headers = new Headers(customHeaders);
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }

  const method = (fetchOptions.method || 'GET').toUpperCase();
  const isStateChanging = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method);

  // Attach Content-Type for JSON payloads if not already set or FormData
  if (fetchOptions.body && !(fetchOptions.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  // Attach CSRF token on state-changing requests
  if (isStateChanging && !skipCsrf) {
    const token = await getCsrfToken();
    if (token) {
      headers.set('X-CSRF-TOKEN', token);
    }
  }

  let response: Response;
  try {
    response = await fetch(url, {
      ...fetchOptions,
      headers,
      credentials: 'include',
    });
  } catch (networkError: unknown) {
    const msg = networkError instanceof Error ? networkError.message : 'Network request failed';
    throw new ApiError(0, msg, 'NETWORK_ERROR', 'client-network');
  }

  // Handle 401 Unauthorized
  if (response.status === 401) {
    notifyAuthExpired();
    throw new ApiError(401, 'Your session has expired. Please sign in again.', 'UNAUTHORIZED');
  }

  // Handle Non-2xx Responses
  if (!response.ok) {
    let errorCode = `HTTP_${response.status}`;
    let errorMessage = `Request failed with status ${response.status}`;
    let requestId = 'unknown';

    try {
      const errBody = await response.json();
      const parsed = ApiErrorSchema.safeParse(errBody);
      if (parsed.success) {
        errorCode = parsed.data.code;
        errorMessage = parsed.data.message;
        requestId = parsed.data.requestId;
      } else if (errBody.message) {
        errorMessage = String(errBody.message);
      }
    } catch {
      // ignore JSON parse failure on non-JSON error
    }

    throw new ApiError(response.status, errorMessage, errorCode, requestId);
  }

  // 204 No Content
  if (response.status === 204) {
    return {} as T;
  }

  const json = await response.json();

  if (schema) {
    const parseResult = schema.safeParse(json);
    if (!parseResult.success) {
      console.error('API Schema Validation Error:', parseResult.error);
      throw new ApiError(
        500,
        'Server returned invalid data format according to contract',
        'SCHEMA_VALIDATION_ERROR',
      );
    }
    return parseResult.data as T;
  }

  return json as T;
}
