import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastProvider, ToastViewport } from '@/components/ui/Toast';
import { PublicLayout } from '@/components/layout/PublicLayout';
import { AppLayout } from '@/components/layout/AppLayout';
import { ErrorBoundary } from '@/components/common/ErrorBoundary';
import { LoginPage } from '@/features/auth/LoginPage';
import { RequireRole } from '@/features/auth/RequireRole';
import { NotFoundOrForbidden } from '@/features/common/NotFoundOrForbidden';

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
                      <div className="mx-auto max-w-7xl p-6">
                        <h1 className="font-heading text-2xl font-bold">Dashboard</h1>
                      </div>
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/finder"
                element={
                  <RequireRole>
                    <AppLayout>
                      <div className="mx-auto max-w-7xl p-6">
                        <h1 className="font-heading text-2xl font-bold">Find a Room</h1>
                      </div>
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/venues"
                element={
                  <RequireRole>
                    <AppLayout>
                      <div className="mx-auto max-w-7xl p-6">
                        <h1 className="font-heading text-2xl font-bold">Venues & Labs</h1>
                      </div>
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/guesthouse"
                element={
                  <RequireRole roles={['STAFF', 'ADMIN']}>
                    <AppLayout>
                      <div className="mx-auto max-w-7xl p-6">
                        <h1 className="font-heading text-2xl font-bold">MDC Guest House</h1>
                      </div>
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/parking"
                element={
                  <RequireRole>
                    <AppLayout>
                      <div className="mx-auto max-w-7xl p-6">
                        <h1 className="font-heading text-2xl font-bold">Visitor Parking</h1>
                      </div>
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/bookings"
                element={
                  <RequireRole>
                    <AppLayout>
                      <div className="mx-auto max-w-7xl p-6">
                        <h1 className="font-heading text-2xl font-bold">My Bookings</h1>
                      </div>
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/wizard"
                element={
                  <RequireRole>
                    <AppLayout>
                      <div className="mx-auto max-w-7xl p-6">
                        <h1 className="font-heading text-2xl font-bold">New Booking Wizard</h1>
                      </div>
                    </AppLayout>
                  </RequireRole>
                }
              />

              <Route
                path="/agent"
                element={
                  <RequireRole>
                    <AppLayout>
                      <div className="mx-auto max-w-7xl p-6">
                        <h1 className="font-heading text-2xl font-bold">CampusFlow Agent</h1>
                      </div>
                    </AppLayout>
                  </RequireRole>
                }
              />

              {/* Admin Only Routes */}
              <Route
                path="/admin/*"
                element={
                  <RequireRole roles={['ADMIN']}>
                    <AppLayout>
                      <div className="mx-auto max-w-7xl p-6">
                        <h1 className="font-heading text-2xl font-bold">Admin Console</h1>
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
