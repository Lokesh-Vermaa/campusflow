import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ApprovalQueuePage } from './ApprovalQueuePage';
import * as adminApiModule from '@/api/admin';
import { SEED_BOOKINGS } from '@/mocks/seedData';

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

describe('Admin Approval Queue UI & Governance', () => {
  it('renders pending bookings with conflict checks and agent recommendations', async () => {
    const queryClient = createTestQueryClient();
    const mockPendingBookings = SEED_BOOKINGS.filter((b) => b.status === 'PENDING').slice(0, 2);

    vi.spyOn(adminApiModule.adminApi, 'getApprovals').mockResolvedValueOnce({
      items: mockPendingBookings,
      pagination: { page: 1, pageSize: 8, totalItems: mockPendingBookings.length, totalPages: 1 },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ApprovalQueuePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByText(mockPendingBookings[0].title)).toBeInTheDocument();
    expect(screen.getAllByText(/approve booking/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/reject request/i).length).toBeGreaterThan(0);
  });

  it('validates that rejection requires a reason of at least 5 characters', async () => {
    const user = userEvent.setup();
    const queryClient = createTestQueryClient();
    const mockPendingBookings = SEED_BOOKINGS.filter((b) => b.status === 'PENDING').slice(0, 1);

    vi.spyOn(adminApiModule.adminApi, 'getApprovals').mockResolvedValueOnce({
      items: mockPendingBookings,
      pagination: { page: 1, pageSize: 8, totalItems: 1, totalPages: 1 },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ApprovalQueuePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const rejectBtn = (await screen.findAllByText(/reject request/i))[0];
    await user.click(rejectBtn);

    // Dialog opens
    expect(screen.getByText(/formal rejection reason is required/i)).toBeInTheDocument();

    const confirmRejectBtn = screen.getByRole('button', { name: /confirm rejection/i });
    await user.click(confirmRejectBtn);

    // Error message appears because input is empty
    expect(screen.getByText(/rejection reason must be at least 5 characters/i)).toBeInTheDocument();
  });
});
