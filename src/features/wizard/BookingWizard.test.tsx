import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BookingWizardPage } from './BookingWizardPage';

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

describe('Booking Wizard Form Validation & Flow', () => {
  it('renders step 1 with pre-filled event fields', () => {
    const queryClient = createTestQueryClient();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <BookingWizardPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText(/step 1: event details & schedule/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/event title/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/booking date/i)).toBeInTheDocument();
  });

  it('navigates from step 1 to step 2 when clicking proceed', async () => {
    const user = userEvent.setup();
    const queryClient = createTestQueryClient();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <BookingWizardPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const proceedBtn = screen.getByRole('button', { name: /proceed to select venue/i });
    await user.click(proceedBtn);

    expect(screen.getByText(/step 2: choose room or amphitheatre/i)).toBeInTheDocument();
  });
});
