import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Calendar,
  Clock,
  MapPin,
  Bot,
  PlusCircle,
  Search,
  Building2,
  Car,
  Home,
  ArrowRight,
  TrendingUp,
} from 'lucide-react';
import { useAuth } from '@/features/auth/useAuth';
import { bookingsApi } from '@/api/bookings';
import { reportsApi } from '@/api/reports';
import { GradientHeading } from '@/components/ui/GradientHeading';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { ScrollReveal } from '@/components/ui/ScrollReveal';

export function DashboardPage() {
  const { user } = useAuth();

  // Fetch recent user bookings
  const { data: bookingsData, isLoading: bookingsLoading } = useQuery({
    queryKey: ['bookings', 'my', { page: 1, pageSize: 4 }],
    queryFn: () => bookingsApi.getMyBookings({ page: 1, pageSize: 4 }),
  });

  // Fetch campus utilisation stats
  const { data: statsData } = useQuery({
    queryKey: ['reports', 'utilisation'],
    queryFn: () => reportsApi.getUtilisation(),
  });

  const recentBookings = bookingsData?.items || [];

  return (
    <div className="min-h-screen bg-surface-alt">
      {/* 1. Full-Bleed Hero Band */}
      <section
        className="relative overflow-hidden px-4 pb-16 pt-12 sm:px-6 lg:px-8"
        style={{
          background: 'linear-gradient(135deg, var(--brand-periwinkle-deep) 0%, var(--brand-azure-strong) 100%)',
        }}
        aria-label="Welcome banner"
      >
        <div
          className="pointer-events-none absolute inset-0 opacity-10"
          style={{
            backgroundImage: 'radial-gradient(circle at 2px 2px, white 1px, transparent 0)',
            backgroundSize: '24px 24px',
          }}
          aria-hidden="true"
        />

        <div className="relative mx-auto max-w-7xl">
          <div className="flex flex-col items-start justify-between gap-6 md:flex-row md:items-center">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium tracking-wide text-white/80">
                  Welcome back,
                </span>
                <span className="rounded bg-white/20 px-2 py-0.5 text-xs font-semibold text-white">
                  {user?.role}
                </span>
              </div>
              <h1 className="mt-1 font-heading text-3xl font-extrabold tracking-tight text-white sm:text-4xl lg:text-5xl">
                {user?.name || 'JKLU Organiser'}
              </h1>
              <p className="mt-2 max-w-xl text-sm text-white/90 sm:text-base">
                Department: {user?.department || 'JKLU Campus Community'}. Check classroom schedules, reserve amphitheatres, and coordinate guest accommodation.
              </p>
            </div>

            <div className="flex flex-wrap gap-3">
              <Button asChild variant="orange" size="lg" className="shadow-md">
                <Link to="/wizard">
                  <PlusCircle className="mr-2 h-5 w-5" aria-hidden="true" />
                  New Booking
                </Link>
              </Button>
              <Button asChild variant="outline" size="lg" className="border-white/40 bg-white/10 text-white hover:bg-white/20">
                <Link to="/agent">
                  <Bot className="mr-2 h-5 w-5" aria-hidden="true" />
                  Ask AI Agent
                </Link>
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* 2. Stat Tiles */}
      <div className="relative -mt-8 mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Card className="border-t-4 border-t-brand-azure-strong shadow-sm">
            <CardContent className="p-4 sm:p-6">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-text-body/70 sm:text-sm">Free Rooms Now</span>
                <Building2 className="h-5 w-5 text-brand-azure-strong" aria-hidden="true" />
              </div>
              <p className="mt-2 font-heading text-2xl font-bold text-text-heading sm:text-3xl">
                {statsData?.freeRoomsNow ?? 14}
              </p>
              <p className="mt-1 text-xs text-text-body/60">Across 3 academic blocks</p>
            </CardContent>
          </Card>

          <Card className="border-t-4 border-t-amber-500 shadow-sm">
            <CardContent className="p-4 sm:p-6">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-text-body/70 sm:text-sm">Pending Approvals</span>
                <Clock className="h-5 w-5 text-amber-600" aria-hidden="true" />
              </div>
              <p className="mt-2 font-heading text-2xl font-bold text-text-heading sm:text-3xl">
                {statsData?.pendingApprovalsCount ?? 4}
              </p>
              <p className="mt-1 text-xs text-text-body/60">Awaiting admin review</p>
            </CardContent>
          </Card>

          <Card className="border-t-4 border-t-brand-periwinkle-deep shadow-sm">
            <CardContent className="p-4 sm:p-6">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-text-body/70 sm:text-sm">Bookings This Week</span>
                <Calendar className="h-5 w-5 text-brand-periwinkle-deep" aria-hidden="true" />
              </div>
              <p className="mt-2 font-heading text-2xl font-bold text-text-heading sm:text-3xl">
                {statsData?.totalBookingsPeriod ?? 84}
              </p>
              <p className="mt-1 text-xs text-text-body/60">Events and academic sessions</p>
            </CardContent>
          </Card>

          <Card className="border-t-4 border-t-green-600 shadow-sm">
            <CardContent className="p-4 sm:p-6">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-text-body/70 sm:text-sm">Room Utilisation</span>
                <TrendingUp className="h-5 w-5 text-green-600" aria-hidden="true" />
              </div>
              <p className="mt-2 font-heading text-2xl font-bold text-text-heading sm:text-3xl">
                82%
              </p>
              <p className="mt-1 text-xs text-text-body/60">Peak hours 11:00-16:00</p>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* 3. Feature Band: Quick Action Links */}
      <section
        className="my-12 w-full py-10 text-white"
        style={{ backgroundColor: 'var(--brand-periwinkle-deep)' }}
        aria-label="Quick Actions"
      >
        <div className="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
          <h2 className="font-heading text-2xl font-bold tracking-tight text-white sm:text-3xl">
            Campus Resource Directory
          </h2>
          <p className="mx-auto mt-2 max-w-2xl text-sm text-white/90 sm:text-base">
            Instant scheduling, verified class timetables, and multi-resource event planning.
          </p>

          <hr className="my-6 border-white/20" />

          <div className="flex flex-wrap items-center justify-center gap-6 sm:gap-10">
            <Link
              to="/finder"
              className="group inline-flex items-center gap-2 text-sm font-semibold tracking-wide text-white hover:text-white/80 focus-visible:outline focus-visible:outline-2 focus-visible:outline-white"
            >
              <Search className="h-4 w-4" aria-hidden="true" />
              <span>Find a Free Room</span>
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-white/20 transition-transform group-hover:translate-x-1">
                →
              </span>
            </Link>

            <Link
              to="/venues"
              className="group inline-flex items-center gap-2 text-sm font-semibold tracking-wide text-white hover:text-white/80 focus-visible:outline focus-visible:outline-2 focus-visible:outline-white"
            >
              <Building2 className="h-4 w-4" aria-hidden="true" />
              <span>Book Venues & Labs</span>
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-white/20 transition-transform group-hover:translate-x-1">
                →
              </span>
            </Link>

            <Link
              to="/guesthouse"
              className="group inline-flex items-center gap-2 text-sm font-semibold tracking-wide text-white hover:text-white/80 focus-visible:outline focus-visible:outline-2 focus-visible:outline-white"
            >
              <Home className="h-4 w-4" aria-hidden="true" />
              <span>MDC Guest House</span>
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-white/20 transition-transform group-hover:translate-x-1">
                →
              </span>
            </Link>

            <Link
              to="/parking"
              className="group inline-flex items-center gap-2 text-sm font-semibold tracking-wide text-white hover:text-white/80 focus-visible:outline focus-visible:outline-2 focus-visible:outline-white"
            >
              <Car className="h-4 w-4" aria-hidden="true" />
              <span>Visitor Parking</span>
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-white/20 transition-transform group-hover:translate-x-1">
                →
              </span>
            </Link>
          </div>
        </div>
      </section>

      {/* 4. Two-Column Content Section: "My Bookings" */}
      <section className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8" aria-labelledby="my-bookings-heading">
        <ScrollReveal>
          <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
            {/* Left 1/3: Heading and Description */}
            <div className="lg:col-span-4">
              <GradientHeading as="h2" id="my-bookings-heading" className="text-3xl font-extrabold sm:text-4xl">
                My Bookings
              </GradientHeading>
              <p className="mt-3 text-sm text-text-body/80 sm:text-base">
                Track status, cancellation requests, and administrative approval notes for your venue and room allocations.
              </p>

              <div className="mt-6 flex flex-col gap-3">
                <Button asChild variant="outline" className="w-fit">
                  <Link to="/bookings">
                    View All My Bookings
                    <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
                  </Link>
                </Button>

                {/* AI Agent Highlight Callout */}
                <div className="mt-6 rounded-lg border border-brand-azure-strong/20 bg-brand-azure-strong/5 p-4">
                  <div className="flex items-center gap-2 text-brand-azure-strong">
                    <Bot className="h-5 w-5" aria-hidden="true" />
                    <span className="font-heading font-semibold text-sm">Need a complex multi-resource booking?</span>
                  </div>
                  <p className="mt-2 text-xs text-text-body/80">
                    Talk to the CampusFlow AI Agent. It coordinates room, guest house, and parking simultaneously with zero clash.
                  </p>
                  <Button asChild variant="default" size="sm" className="mt-3">
                    <Link to="/agent">Open Agent Chat</Link>
                  </Button>
                </div>
              </div>
            </div>

            {/* Right 2/3: 2-Column Grid of White Cards */}
            <div className="lg:col-span-8">
              {bookingsLoading ? (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {[1, 2, 3, 4].map((n) => (
                    <div key={n} className="h-44 animate-pulse rounded-lg bg-surface shadow-sm" />
                  ))}
                </div>
              ) : recentBookings.length === 0 ? (
                <div className="rounded-lg border border-dashed border-border-color bg-surface p-8 text-center">
                  <Calendar className="mx-auto h-10 w-10 text-text-body/40" aria-hidden="true" />
                  <h3 className="mt-2 font-heading font-semibold text-text-heading">No bookings yet</h3>
                  <p className="mt-1 text-sm text-text-body/70">
                    Use our Classroom Finder or Booking Wizard to reserve your first venue.
                  </p>
                  <Button asChild variant="orange" size="sm" className="mt-4">
                    <Link to="/wizard">Create a Booking</Link>
                  </Button>
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {recentBookings.map((b) => (
                    <Card key={b.id} className="shadow-sm transition hover:shadow-md">
                      <CardContent className="flex h-full flex-col justify-between p-5">
                        <div>
                          <div className="flex items-start justify-between gap-2">
                            <h3 className="font-heading text-base font-bold text-text-heading line-clamp-1">
                              {b.title}
                            </h3>
                            <Badge
                              variant={
                                b.status === 'APPROVED'
                                  ? 'success'
                                  : b.status === 'PENDING'
                                    ? 'warning'
                                    : b.status === 'REJECTED'
                                      ? 'destructive'
                                      : 'secondary'
                              }
                            >
                              {b.status}
                            </Badge>
                          </div>

                          {/* Italic parenthesised subtitle requirement */}
                          <p className="mt-1 text-xs italic text-brand-azure-strong">
                            {b.resourceName} ({b.resourceType})
                          </p>

                          <div className="mt-3 space-y-1.5 text-xs text-text-body/70">
                            <div className="flex items-center gap-1.5">
                              <Calendar className="h-3.5 w-3.5 text-text-body/50" aria-hidden="true" />
                              <span>{b.bookingDate}</span>
                            </div>
                            <div className="flex items-center gap-1.5">
                              <Clock className="h-3.5 w-3.5 text-text-body/50" aria-hidden="true" />
                              <span>{b.startTime} - {b.endTime}</span>
                            </div>
                            {b.expectedAttendees && (
                              <div className="flex items-center gap-1.5">
                                <MapPin className="h-3.5 w-3.5 text-text-body/50" aria-hidden="true" />
                                <span>{b.expectedAttendees} expected attendees</span>
                              </div>
                            )}
                          </div>
                        </div>

                        <div className="mt-4 border-t border-border-color pt-3">
                          <Link
                            to="/bookings"
                            className="inline-flex items-center text-xs font-semibold text-brand-azure-strong hover:underline"
                          >
                            <span>View Details & Timeline</span>
                            <ArrowRight className="ml-1 h-3 w-3" aria-hidden="true" />
                          </Link>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              )}
            </div>
          </div>
        </ScrollReveal>
      </section>
    </div>
  );
}
