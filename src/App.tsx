import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastProvider, ToastViewport } from '@/components/ui/Toast';
import { PublicLayout } from '@/components/layout/PublicLayout';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      retry: 1,
    },
  },
});

function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-brand-periwinkle-deep to-brand-azure-strong">
      <div className="w-full max-w-md rounded-xl bg-surface p-8 shadow-xl">
        <h1 className="mb-2 font-heading text-2xl font-bold text-text-heading">
          Sign in to CampusFlow
        </h1>
        <p className="mb-6 text-sm text-text-body">
          Manage campus bookings for venues, guest-house and parking.
        </p>
        <p className="text-sm text-text-body/60">
          Authentication coming in Phase 1.
        </p>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Navigate to="/login" replace />} />
            <Route
              path="/login"
              element={
                <PublicLayout>
                  <LoginPage />
                </PublicLayout>
              }
            />
            {/* Placeholder — Phase 1 will fill authenticated routes */}
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </BrowserRouter>
        <ToastViewport />
      </ToastProvider>
    </QueryClientProvider>
  );
}
