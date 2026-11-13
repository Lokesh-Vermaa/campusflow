import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import * as useAuthModule from '@/features/auth/useAuth';

// Mock useAuth
vi.mock('@/features/auth/useAuth', () => ({
  useAuth: vi.fn(),
}));

function mockAuth(overrides = {}) {
  return {
    user: null,
    isAuthenticated: false,
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

describe('Sidebar Component & Navigation Shell', () => {
  const defaultProps = {
    isCollapsed: false,
    onToggleCollapse: vi.fn(),
    mobileOpen: false,
    onCloseMobile: vi.fn(),
    recentChats: [],
  };

  it('renders workspace links and hides Admin group for STUDENT role', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue(
      mockAuth({
        user: { id: 'u1', name: 'Aarav Sharma', email: 'student@demo.jklu.test', role: 'STUDENT', department: 'CS' },
        isAuthenticated: true,
        hasRole: (roles: string | string[]) => {
          const arr = Array.isArray(roles) ? roles : [roles];
          return arr.includes('STUDENT');
        },
      }),
    );

    render(
      <MemoryRouter initialEntries={['/']}>
        <Sidebar {...defaultProps} />
      </MemoryRouter>,
    );

    // Workspace links should exist
    expect(screen.getByText('Chat')).toBeInTheDocument();
    expect(screen.getByText('Find a Room')).toBeInTheDocument();
    expect(screen.getByText('Venues & Labs')).toBeInTheDocument();
    expect(screen.getByText('Parking')).toBeInTheDocument();
    expect(screen.getByText('My Bookings')).toBeInTheDocument();
    expect(screen.getByText('Overview')).toBeInTheDocument();

    // Guest House is staff/admin only, so student shouldn't see it
    expect(screen.queryByText('Guest House')).not.toBeInTheDocument();

    // Admin nav should NOT be rendered
    expect(screen.queryByRole('navigation', { name: 'Admin' })).not.toBeInTheDocument();
    expect(screen.queryByText('Approvals')).not.toBeInTheDocument();
    expect(screen.queryByText('Timetable Upload')).not.toBeInTheDocument();
  });

  it('renders Admin group and Guest House for ADMIN role', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue(
      mockAuth({
        user: { id: 'u3', name: 'Rajesh Patel', email: 'admin@demo.jklu.test', role: 'ADMIN', department: 'Admin' },
        isAuthenticated: true,
        hasRole: () => true, // Admin has all roles
      }),
    );

    render(
      <MemoryRouter initialEntries={['/']}>
        <Sidebar {...defaultProps} />
      </MemoryRouter>,
    );

    // Guest house visible
    expect(screen.getByText('Guest House')).toBeInTheDocument();

    // Admin nav and links should be rendered
    expect(screen.getByRole('navigation', { name: 'Admin' })).toBeInTheDocument();
    expect(screen.getByText('Approvals')).toBeInTheDocument();
    expect(screen.getByText('Timetable Upload')).toBeInTheDocument();
    expect(screen.getByText('Master Data')).toBeInTheDocument();
    expect(screen.getByText('Audit Log')).toBeInTheDocument();
    expect(screen.getByText('Reports')).toBeInTheDocument();
  });

  it('sets aria-current="page" on the active navigation item', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue(
      mockAuth({
        user: { id: 'u1', name: 'Aarav Sharma', email: 'student@demo.jklu.test', role: 'STUDENT', department: 'CS' },
        isAuthenticated: true,
        hasRole: (roles: string | string[]) => {
          const arr = Array.isArray(roles) ? roles : [roles];
          return arr.includes('STUDENT');
        },
      }),
    );

    render(
      <MemoryRouter initialEntries={['/rooms']}>
        <Sidebar {...defaultProps} />
      </MemoryRouter>,
    );

    const activeLink = screen.getByRole('link', { name: 'Find a Room' });
    expect(activeLink).toHaveAttribute('aria-current', 'page');

    const inactiveLink = screen.getByRole('link', { name: 'Venues & Labs' });
    expect(inactiveLink).not.toHaveAttribute('aria-current');
  });

  it('handles mobile drawer escape key and backdrop click to close', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue(
      mockAuth({
        user: { id: 'u1', name: 'Aarav Sharma', email: 'student@demo.jklu.test', role: 'STUDENT', department: 'CS' },
        isAuthenticated: true,
        hasRole: () => false,
      }),
    );

    const onCloseMobile = vi.fn();

    render(
      <MemoryRouter initialEntries={['/']}>
        <Sidebar {...defaultProps} mobileOpen={true} onCloseMobile={onCloseMobile} />
      </MemoryRouter>,
    );

    // Escape closes drawer
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onCloseMobile).toHaveBeenCalledTimes(1);

    // Backdrop click closes drawer
    const backdrop = document.querySelector('.bg-black\\/40');
    expect(backdrop).toBeInTheDocument();
    if (backdrop) {
      fireEvent.click(backdrop);
      expect(onCloseMobile).toHaveBeenCalledTimes(2);
    }
  });

  it('renders user initials and role badge in footer', () => {
    vi.mocked(useAuthModule.useAuth).mockReturnValue(
      mockAuth({
        user: { id: 'u2', name: 'Neha Verma', email: 'staff@demo.jklu.test', role: 'STAFF', department: 'ECE' },
        isAuthenticated: true,
        hasRole: (roles: string | string[]) => {
          const arr = Array.isArray(roles) ? roles : [roles];
          return arr.includes('STAFF');
        },
      }),
    );

    render(
      <MemoryRouter initialEntries={['/']}>
        <Sidebar {...defaultProps} />
      </MemoryRouter>,
    );

    expect(screen.getByText('NV')).toBeInTheDocument();
    expect(screen.getByText('Neha Verma')).toBeInTheDocument();
    expect(screen.getByText('STAFF')).toBeInTheDocument();
  });
});
