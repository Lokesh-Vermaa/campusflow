import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastProvider, ToastViewport } from '@/components/ui/Toast';
import { PublicLayout } from '@/components/layout/PublicLayout';
import { AppLayout } from '@/components/layout/AppLayout';
import { ErrorBoundary } from '@/components/common/ErrorBoundary';
import { LoginPage } from '@/features/auth/LoginPage';
import { RequireRole } from '@/features/auth/RequireRole';
import { NotFoundOrForbidden } from '@/features/common/NotFoundOrForbidden';
import { DashboardPage } from '@/features/dashboard/DashboardPage';
import { FinderPage } from '@/features/finder/FinderPage';
import { VenuesPage } from '@/features/venues/VenuesPage';
import { GuestHousePage } from '@/features/guesthouse/GuestHousePage';
import { ParkingPage } from '@/features/parking/ParkingPage';
import { BookingWizardPage } from '@/features/wizard/BookingWizardPage';
import { MyBookingsPage } from '@/features/bookings/MyBookingsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      retry: 1,
    },
  },
});

export default function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <BrowserRouter>
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<Navigate to="/login" replace />} />
              <Route
                path="/login"
                element={
                  <PublicLayout>
                    <LoginPage />
                  </PublicLayout>
                }
              />

              {/* Authenticated Routes wrapped in AppLayout and RequireRole */}
              <Route
                path="/dashboard"
                element={
                  <RequireRole>
                    <AppLayout>
                      <DashboardPage />
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/finder"
                element={
                  <RequireRole>
                    <AppLayout>
                      <FinderPage />
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/venues"
                element={
                  <RequireRole>
                    <AppLayout>
                      <VenuesPage />
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/guesthouse"
                element={
                  <RequireRole roles={['STAFF', 'ADMIN']}>
                    <AppLayout>
                      <GuestHousePage />
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/parking"
                element={
                  <RequireRole>
                    <AppLayout>
                      <ParkingPage />
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/wizard"
                element={
                  <RequireRole>
                    <AppLayout>
                      <BookingWizardPage />
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/bookings"
                element={
                  <RequireRole>
                    <AppLayout>
                      <MyBookingsPage />
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/agent"
                element={
                  <RequireRole>
                    <AppLayout>
                      <div className="mx-auto max-w-7xl p-8">
                        <h1 className="font-heading text-2xl font-bold">CampusFlow AI Agent</h1>
                        <p className="mt-2 text-sm text-text-body/70">Agent Chat module loading in Phase 4.</p>
                      </div>
                    </AppLayout>
                  </RequireRole>
                }
              />

              {/* Admin Console Placeholder (Phase 3) */}
              <Route
                path="/admin/*"
                element={
                  <RequireRole roles={['ADMIN']}>
                    <AppLayout>
                      <div className="mx-auto max-w-7xl p-8">
                        <h1 className="font-heading text-2xl font-bold">Admin Console</h1>
                        <p className="mt-2 text-sm text-text-body/70">Admin features loading in Phase 3.</p>
                      </div>
                    </AppLayout>
                  </RequireRole>
                }
              />

              {/* 403 & 404 Handled Identically */}
              <Route
                path="/403"
                element={
                  <PublicLayout>
                    <NotFoundOrForbidden />
                  </PublicLayout>
                }
              />
              <Route
                path="*"
                element={
                  <PublicLayout>
                    <NotFoundOrForbidden />
                  </PublicLayout>
                }
              />
            </Routes>
          </BrowserRouter>
          <ToastViewport />
        </ToastProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
