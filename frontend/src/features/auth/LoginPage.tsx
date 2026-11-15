import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useLocation, useNavigate } from 'react-router-dom';
import { ShieldCheck, UserCheck, AlertTriangle, KeyRound, Mail, ArrowRight } from 'lucide-react';
import { LoginRequest, LoginRequestSchema } from '@/api/types';
import { useAuth } from './useAuth';
import { SEED_USERS } from '@/mocks/seedData';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/Card';

const MAX_FAILED_ATTEMPTS = 5;
const LOCKOUT_SECONDS = 30;

// Demo mode: controlled by env vars, never hardcoded
const IS_DEMO_MODE = import.meta.env.VITE_DEMO_MODE === 'true';
const DEMO_PASSWORD = import.meta.env.VITE_DEMO_PASSWORD ?? '';

export function LoginPage() {
  const { login, isLoggingIn } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const [failedAttempts, setFailedAttempts] = useState(0);
  const [lockoutRemaining, setLockoutRemaining] = useState(0);
  const [authError, setAuthError] = useState<string | null>(null);

  const queryParams = new URLSearchParams(location.search);
  const isSessionExpired = queryParams.get('expired') === 'true';

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<LoginRequest>({
    resolver: zodResolver(LoginRequestSchema),
    defaultValues: {
      email: 'student@demo.jklu.test',
      password: DEMO_PASSWORD,
    },
  });

  // Lockout countdown timer
  useEffect(() => {
    if (lockoutRemaining <= 0) return;
    const interval = setInterval(() => {
      setLockoutRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          setFailedAttempts(0);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [lockoutRemaining]);

  const onSubmit = async (data: LoginRequest) => {
    if (lockoutRemaining > 0) return;

    setAuthError(null);
    try {
      await login(data);
      const destination = (location.state as { from?: { pathname?: string } })?.from?.pathname || '/';
      navigate(destination, { replace: true });
    } catch {
      const newFails = failedAttempts + 1;
      setFailedAttempts(newFails);

      if (newFails >= MAX_FAILED_ATTEMPTS) {
        setLockoutRemaining(LOCKOUT_SECONDS);
        setAuthError(`Too many failed attempts. Login locked for ${LOCKOUT_SECONDS} seconds for security.`);
      } else {
        const remaining = MAX_FAILED_ATTEMPTS - newFails;
        setAuthError(
          `Invalid credentials. ${remaining} attempt${remaining === 1 ? '' : 's'} remaining before temporary security lockout.`,
        );
      }
    }
  };

  const fillDemoAccount = (roleKey: 'student' | 'staff' | 'admin') => {
    const demoUser = SEED_USERS[roleKey];
    setValue('email', demoUser.email);
    setValue('password', DEMO_PASSWORD);
    setAuthError(null);
  };

  return (
    <div className="min-h-screen bg-surface-alt px-4 py-12 sm:px-6 lg:px-8">
      {/* Clean Brand Header */}
      <div className="mx-auto max-w-4xl text-center">
        <div className="inline-flex items-center gap-2 rounded-full border border-border-color bg-surface px-3.5 py-1 text-xs font-semibold tracking-wide text-brand-azure-strong shadow-xs">
          <ShieldCheck className="h-4 w-4" aria-hidden="true" />
          JKLU AI-Buildathon 1.0 (PS-02)
        </div>

        <h1 className="mt-4 font-heading text-3xl font-extrabold tracking-tight text-text-heading sm:text-4xl">
          JKLU CampusFlow
        </h1>

        <p className="mx-auto mt-2 max-w-xl text-sm text-text-body/80 sm:text-base">
          AI-agent-powered campus venue, guest house, and parking management system. Seamless,
          conflict-free scheduling for faculty, student clubs, and campus staff.
        </p>
      </div>

      {/* Main Login & Demo Accounts Container */}
      <div className="mx-auto mt-8 max-w-4xl">
        <div className="grid grid-cols-1 gap-8 md:grid-cols-12">
          {/* Sign In Form */}
          <Card className="border border-border-color bg-surface shadow-xs md:col-span-7">
            <CardHeader>
              <CardTitle>Sign In to CampusFlow</CardTitle>
              <CardDescription>
                Authenticate with your university credentials or select a demo role.
              </CardDescription>
            </CardHeader>

            <CardContent>
              {isSessionExpired && (
                <div
                  role="alert"
                  className="mb-4 flex items-center gap-2 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900"
                >
                  <AlertTriangle className="h-4 w-4 shrink-0 text-amber-600" aria-hidden="true" />
                  Your previous session has expired. Please sign in again.
                </div>
              )}

              {authError && (
                <div
                  role="alert"
                  className="mb-4 flex items-start gap-2 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-900"
                >
                  <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-red-600" aria-hidden="true" />
                  <span>{authError}</span>
                </div>
              )}

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
                <div>
                  <Label htmlFor="email">JKLU Email Address</Label>
                  <div className="relative mt-1">
                    <Mail
                      className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-text-body/50"
                      aria-hidden="true"
                    />
                    <Input
                      id="email"
                      type="email"
                      autoComplete="username"
                      placeholder="name@demo.jklu.test"
                      className="pl-9"
                      disabled={lockoutRemaining > 0 || isLoggingIn}
                      aria-invalid={Boolean(errors.email)}
                      aria-describedby={errors.email ? 'email-error' : undefined}
                      {...register('email')}
                    />
                  </div>
                  {errors.email && (
                    <p id="email-error" className="mt-1 text-xs text-red-600">
                      {errors.email.message}
                    </p>
                  )}
                </div>

                <div>
                  <Label htmlFor="password">Password</Label>
                  <div className="relative mt-1">
                    <KeyRound
                      className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-text-body/50"
                      aria-hidden="true"
                    />
                    <Input
                      id="password"
                      type="password"
                      autoComplete="current-password"
                      placeholder="••••••••"
                      className="pl-9"
                      disabled={lockoutRemaining > 0 || isLoggingIn}
                      aria-invalid={Boolean(errors.password)}
                      aria-describedby={errors.password ? 'password-error' : undefined}
                      {...register('password')}
                    />
                  </div>
                  {errors.password && (
                    <p id="password-error" className="mt-1 text-xs text-red-600">
                      {errors.password.message}
                    </p>
                  )}
                </div>

                {lockoutRemaining > 0 ? (
                  <div
                    role="alert"
                    aria-live="polite"
                    className="rounded-md border border-red-300 bg-red-100 p-3 text-center text-sm font-semibold text-red-800"
                  >
                    Login locked for security. Try again in {lockoutRemaining}s.
                  </div>
                ) : (
                  <Button
                    type="submit"
                    variant="orange"
                    className="w-full text-base font-semibold"
                    disabled={isLoggingIn}
                  >
                    {isLoggingIn ? 'Authenticating...' : 'Sign In'}
                    <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
                  </Button>
                )}
              </form>
            </CardContent>
          </Card>

          {/* Demo Accounts Box — shown only when VITE_DEMO_MODE=true */}
          {IS_DEMO_MODE && (
          <aside className="md:col-span-5" aria-label="Demo Credentials Box">
            <Card className="border-2 border-brand-azure-strong/30 bg-surface shadow-md">
              <CardHeader className="pb-3">
                <div className="flex items-center gap-2 text-brand-azure-strong">
                  <UserCheck className="h-5 w-5" aria-hidden="true" />
                  <CardTitle className="text-lg">Demo Accounts</CardTitle>
                </div>
                <CardDescription>
                  Click any account below to pre-fill credentials for judge testing:
                </CardDescription>
              </CardHeader>

              <CardContent className="space-y-3">
                {/* Student Demo */}
                <button
                  type="button"
                  onClick={() => fillDemoAccount('student')}
                  className="w-full rounded-md border border-border-color bg-surface-alt p-3 text-left transition hover:border-brand-azure-strong hover:bg-brand-azure-strong/5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-text-heading">Student (Club Lead)</span>
                    <span className="rounded bg-brand-periwinkle/20 px-2 py-0.5 text-xs font-medium text-brand-periwinkle-deep">
                      STUDENT
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-text-body/70">Aarav Sharma • student@demo.jklu.test</p>
                  <p className="mt-1 text-[11px] text-text-body/60">
                    Find free classrooms, book events, talk to AI agent.
                  </p>
                </button>

                {/* Staff Demo */}
                <button
                  type="button"
                  onClick={() => fillDemoAccount('staff')}
                  className="w-full rounded-md border border-border-color bg-surface-alt p-3 text-left transition hover:border-brand-azure-strong hover:bg-brand-azure-strong/5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-text-heading">Faculty / Staff</span>
                    <span className="rounded bg-brand-azure/20 px-2 py-0.5 text-xs font-medium text-brand-azure-strong">
                      STAFF
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-text-body/70">Dr. Neha Verma • staff@demo.jklu.test</p>
                  <p className="mt-1 text-[11px] text-text-body/60">
                    All student capabilities + amphitheatre, labs & guest house.
                  </p>
                </button>

                {/* Admin Demo */}
                <button
                  type="button"
                  onClick={() => fillDemoAccount('admin')}
                  className="w-full rounded-md border border-border-color bg-surface-alt p-3 text-left transition hover:border-brand-azure-strong hover:bg-brand-azure-strong/5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-text-heading">Campus Admin</span>
                    <span className="rounded bg-brand-orange/30 px-2 py-0.5 text-xs font-medium text-brand-orange-ink">
                      ADMIN
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-text-body/70">Rajesh Patel • admin@demo.jklu.test</p>
                  <p className="mt-1 text-[11px] text-text-body/60">
                    Full privileges: approval queue, timetable upload, master data, audit logs.
                  </p>
                </button>
              </CardContent>
            </Card>
          </aside>
          )}
        </div>
      </div>
    </div>
  );
}
