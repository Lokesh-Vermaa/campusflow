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
import { AgentChatPage } from '@/features/agent/AgentChatPage';
import { ApprovalQueuePage } from '@/features/admin/ApprovalQueuePage';
import { TimetableUploadPage } from '@/features/admin/TimetableUploadPage';
import { MasterDataPage } from '@/features/admin/MasterDataPage';
import { AuditLogPage } from '@/features/admin/AuditLogPage';
import { ReportsPage } from '@/features/admin/ReportsPage';

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
              <Route
                path="/login"
                element={
                  <PublicLayout>
                    <LoginPage />
                  </PublicLayout>
                }
              />

              {/* Chat-First Home (`/`) and Existing Conversations (`/c/:conversationId`) */}
              <Route
                path="/"
                element={
                  <RequireRole>
                    <AppLayout>
                      <AgentChatPage />
                    </AppLayout>
                  </RequireRole>
                }
              />
              <Route
                path="/c/:conversationId"
                element={
                  <RequireRole>
                    <AppLayout>
                      <AgentChatPage />
                    </AppLayout>
                  </RequireRole>
                }
              />

              {/* Workspace Routes */}
              <Route
                path="/overview"
                element={
                  <RequireRole>
                    <AppLayout>
                      <DashboardPage />
                    </AppLayout>
                  </RequireRole>
                }
              />
              <Route path="/dashboard" element={<Navigate to="/overview" replace />} />

              <Route
                path="/rooms"
                element={
                  <RequireRole>
                    <AppLayout>
                      <FinderPage />
                    </AppLayout>
                  </RequireRole>
                }
              />
              <Route path="/finder" element={<Navigate to="/rooms" replace />} />

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
                path="/guest-house"
                element={
                  <RequireRole roles={['STAFF', 'ADMIN']}>
                    <AppLayout>
                      <GuestHousePage />
                    </AppLayout>
                  </RequireRole>
                }
              />
              <Route path="/guesthouse" element={<Navigate to="/guest-house" replace />} />

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
                path="/bookings"
                element={
                  <RequireRole>
                    <AppLayout>
                      <MyBookingsPage />
                    </AppLayout>
                  </RequireRole>
                }
              />
              <Route path="/my-bookings" element={<Navigate to="/bookings" replace />} />

              <Route
                path="/bookings/new"
                element={
                  <RequireRole>
                    <AppLayout>
                      <BookingWizardPage />
                    </AppLayout>
                  </RequireRole>
                }
              />
              <Route path="/wizard" element={<Navigate to="/bookings/new" replace />} />
              <Route path="/agent" element={<Navigate to="/" replace />} />

              {/* Admin-only Routes */}
              <Route
                path="/admin/approvals"
                element={
                  <RequireRole roles={['ADMIN']}>
                    <AppLayout>
                      <ApprovalQueuePage />
                    </AppLayout>
                  </RequireRole>
                }
              />
              <Route
                path="/admin/timetable"
                element={
                  <RequireRole roles={['ADMIN']}>
                    <AppLayout>
                      <TimetableUploadPage />
                    </AppLayout>
                  </RequireRole>
                }
              />
              <Route
                path="/admin/master-data"
                element={
                  <RequireRole roles={['ADMIN']}>
                    <AppLayout>
                      <MasterDataPage />
                    </AppLayout>
                  </RequireRole>
                }
              />
              <Route path="/admin/master" element={<Navigate to="/admin/master-data" replace />} />
              <Route
                path="/admin/audit"
                element={
                  <RequireRole roles={['ADMIN']}>
                    <AppLayout>
                      <AuditLogPage />
                    </AppLayout>
                  </RequireRole>
                }
              />
              <Route
                path="/admin/reports"
                element={
                  <RequireRole roles={['ADMIN']}>
                    <AppLayout>
                      <ReportsPage />
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
