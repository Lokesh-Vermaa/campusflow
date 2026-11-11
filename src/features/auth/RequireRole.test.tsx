import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { RequireRole } from './RequireRole';
import * as useAuthModule from './useAuth';
import { SEED_USERS } from '@/mocks/seedData';

describe('RequireRole Route Guard', () => {
  it('renders children when authenticated and role matches requirement', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: SEED_USERS.admin,
      isAuthenticated: true,
      isLoading: false,
      isError: false,
      error: null,
      login: vi.fn(),
      isLoggingIn: false,
      loginError: null,
      logout: vi.fn(),
      isLoggingOut: false,
      hasRole: vi.fn().mockReturnValue(true),
      refetch: vi.fn(),
    });

    render(
      <MemoryRouter>
        <RequireRole roles={['ADMIN']}>
          <div data-testid="admin-content">Admin Secret Controls</div>
        </RequireRole>
      </MemoryRouter>,
    );

    expect(screen.getByTestId('admin-content')).toBeInTheDocument();
  });

  it('shows loading indicator when session is still loading', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: true,
      isError: false,
      error: null,
      login: vi.fn(),
      isLoggingIn: false,
      loginError: null,
      logout: vi.fn(),
      isLoggingOut: false,
      hasRole: vi.fn(),
      refetch: vi.fn(),
    });

    render(
      <MemoryRouter>
        <RequireRole roles={['ADMIN']}>
          <div>Protected Content</div>
        </RequireRole>
      </MemoryRouter>,
    );

    expect(screen.getByRole('status', { name: /loading session/i })).toBeInTheDocument();
  });

  it('renders NotFoundOrForbidden when user has insufficient role', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: SEED_USERS.student, // STUDENT trying to access ADMIN route
      isAuthenticated: true,
      isLoading: false,
      isError: false,
      error: null,
      login: vi.fn(),
      isLoggingIn: false,
      loginError: null,
      logout: vi.fn(),
      isLoggingOut: false,
      hasRole: vi.fn().mockReturnValue(false),
      refetch: vi.fn(),
    });

    render(
      <MemoryRouter>
        <RequireRole roles={['ADMIN']}>
          <div>Admin Only</div>
        </RequireRole>
      </MemoryRouter>,
    );

    // Prompt rule: 403 / 404 identical handling ("not found or no access")
    expect(screen.getByText(/page not found or access restricted/i)).toBeInTheDocument();
  });
});

