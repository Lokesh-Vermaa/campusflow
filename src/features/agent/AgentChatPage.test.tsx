import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import * as useAuthModule from '@/features/auth/useAuth';

// Mock useAuth
vi.mock('@/features/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

// Mock conversations API
vi.mock('@/api/conversations', () => ({
  conversationsApi: {
    list: vi.fn().mockResolvedValue({ items: [], pagination: { page: 1, pageSize: 50, totalItems: 0, totalPages: 0 } }),
    create: vi.fn().mockResolvedValue({ id: 'conv-1', title: 'New conversation', userId: 'u1', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }),
    rename: vi.fn().mockResolvedValue({}),
    get: vi.fn().mockRejectedValue({ status: 404, message: 'Not found' }),
    delete: vi.fn().mockResolvedValue(undefined),
  },
}));

// Mock agent streaming
vi.mock('@/api/agent', () => ({
  streamAgentChat: vi.fn(),
}));

// Mock bookings API
vi.mock('@/api/bookings', () => ({
  bookingsApi: {
    create: vi.fn().mockResolvedValue({}),
  },
}));

function mockAuth(overrides = {}) {
  return {
    user: { id: 'u1', name: 'Aarav Sharma', email: 'student@demo.jklu.test', role: 'STUDENT' as const, department: 'CS' },
    isAuthenticated: true,
    isLoading: false,
    isError: false,
    error: null,
    isLoggingIn: false,
    loginError: null,
    isLoggingOut: false,
    login: vi.fn(),
    logout: vi.fn(),
    refetch: vi.fn(),
    hasRole: () => false,
    ...overrides,
  };
}

function makeQC() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

import { AgentChatPage } from '@/features/agent/AgentChatPage';

describe('AgentChatPage – Chat Home & Composer', () => {
  it('renders greeting and suggestion cards on empty state', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue(mockAuth());

    render(
      <QueryClientProvider client={makeQC()}>
        <MemoryRouter initialEntries={['/']}>
          <AgentChatPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Greeting contains first name
    expect(screen.getByText(/Aarav/i)).toBeInTheDocument();
    // Subtitle
    expect(screen.getByText(/arrange on campus/i)).toBeInTheDocument();
    // Suggestion cards (STUDENT suggestions)
    expect(screen.getByText(/Find a free classroom/i)).toBeInTheDocument();
    expect(screen.getByText(/Book amphitheatre/i)).toBeInTheDocument();
  });

  it('renders ADMIN suggestion cards for admin user', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue(
      mockAuth({
        user: { id: 'u3', name: 'Rajesh Patel', email: 'admin@demo.jklu.test', role: 'ADMIN' as const, department: 'Admin' },
        hasRole: () => true,
      }),
    );

    render(
      <QueryClientProvider client={makeQC()}>
        <MemoryRouter initialEntries={['/']}>
          <AgentChatPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText(/pending approvals/i)).toBeInTheDocument();
  });

  it('blocks sending empty message', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue(mockAuth());

    render(
      <QueryClientProvider client={makeQC()}>
        <MemoryRouter initialEntries={['/']}>
          <AgentChatPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const sendBtn = screen.getByRole('button', { name: /send message/i });
    expect(sendBtn).toBeDisabled();
  });

  it('enables send button when text is entered', async () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue(mockAuth());

    render(
      <QueryClientProvider client={makeQC()}>
        <MemoryRouter initialEntries={['/']}>
          <AgentChatPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const textarea = screen.getByRole('textbox', { name: /message campusflow/i });
    fireEvent.change(textarea, { target: { value: 'Book a room' } });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /send message/i })).not.toBeDisabled();
    });
  });

  it('shows char counter near limit', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue(mockAuth());

    render(
      <QueryClientProvider client={makeQC()}>
        <MemoryRouter initialEntries={['/']}>
          <AgentChatPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const textarea = screen.getByRole('textbox', { name: /message campusflow/i });
    // Type 1850 chars (2000 - 1850 = 150, near limit)
    fireEvent.change(textarea, { target: { value: 'x'.repeat(1850) } });

    expect(screen.getByText(/150 left/i)).toBeInTheDocument();
  });

  it('Shift+Enter adds newline instead of sending', async () => {
    const { streamAgentChat } = await import('@/api/agent');
    const mockStream = vi.mocked(streamAgentChat);
    mockStream.mockClear();

    vi.mocked(useAuthModule.useAuth).mockReturnValue(mockAuth());

    render(
      <QueryClientProvider client={makeQC()}>
        <MemoryRouter initialEntries={['/']}>
          <AgentChatPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const textarea = screen.getByRole('textbox', { name: /message campusflow/i });
    fireEvent.change(textarea, { target: { value: 'Hello' } });
    fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: true });

    // streamAgentChat should NOT have been called
    expect(mockStream).not.toHaveBeenCalled();
  });
});

