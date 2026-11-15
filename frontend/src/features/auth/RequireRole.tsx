import { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './useAuth';
import { Role } from '@/api/types';
import { NotFoundOrForbidden } from '@/features/common/NotFoundOrForbidden';

interface RequireRoleProps {
  roles?: Role[];
  children: ReactNode;
}

/**
 * Route guard component.
 * NOTE: UI route guards are UX only! The Spring Boot backend independently
 * verifies and enforces authorization checks on every HTTP endpoint.
 *
 * To prevent attackers from probing whether sensitive IDs exist, 403 (unauthorized)
 * and 404 (not found) are handled identically in the UI.
 */
export function RequireRole({ roles, children }: RequireRoleProps) {
  const { user, isLoading, isAuthenticated } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center p-8" role="status" aria-label="Loading session">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-border-color border-t-brand-azure-strong" />
        <span className="sr-only">Checking authorization...</span>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles && roles.length > 0 && user && !roles.includes(user.role)) {
    // 403 / 404 identical handling
    return <NotFoundOrForbidden />;
  }

  return <>{children}</>;
}

