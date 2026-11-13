import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Calendar,
  Clock,
  MapPin,
  Bot,
  PlusCircle,
  Building2,
  ArrowRight,
  TrendingUp,
} from 'lucide-react';
import { useAuth } from '@/features/auth/useAuth';
import { bookingsApi } from '@/api/bookings';
import { reportsApi } from '@/api/reports';
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
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      {/* 1. Clean Page Header */}
      <div className="flex flex-col items-start justify-between gap-4 border-b border-border-color pb-6 sm:flex-row sm:items-center">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight text-text-heading sm:text-3xl">
            Campus Overview
          </h1>
          <p className="mt-1 text-sm text-text-body/70">
            Welcome back, {user?.name || 'Organiser'} ({user?.role}). Real-time campus room availability, pending approvals, and booking activity.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button asChild variant="default" size="sm">
            <Link to="/">
              <Bot className="mr-2 h-4 w-4" aria-hidden="true" />
              Ask AI Agent
            </Link>
          </Button>
          <Button asChild variant="orange" size="sm">
            <Link to="/bookings/new">
              <PlusCircle className="mr-2 h-4 w-4" aria-hidden="true" />
              New Booking
            </Link>
          </Button>
        </div>
      </div>

      {/* 2. Stat Tiles */}
      <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="border border-border-color bg-surface shadow-xs">
          <CardContent className="p-5">
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

        <Card className="border border-border-color bg-surface shadow-xs">
          <CardContent className="p-5">
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

        <Card className="border border-border-color bg-surface shadow-xs">
          <CardContent className="p-5">
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

        <Card className="border border-border-color bg-surface shadow-xs">
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-text-body/70 sm:text-sm">Room Utilisation</span>
              <TrendingUp className="h-5 w-5 text-emerald-600" aria-hidden="true" />
            </div>
            <p className="mt-2 font-heading text-2xl font-bold text-text-heading sm:text-3xl">
              82%
            </p>
            <p className="mt-1 text-xs text-text-body/60">Peak hours 11:00-16:00</p>
          </CardContent>
        </Card>
      </div>

      {/* 3. Content Section: "Recent Bookings" */}
      <section className="mt-10" aria-labelledby="recent-bookings-heading">
        <ScrollReveal>
          <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
            {/* Left 1/3: Title and Description */}
            <div className="lg:col-span-4">
              <h2 id="recent-bookings-heading" className="font-heading text-xl font-bold text-text-heading sm:text-2xl">
                Recent Bookings
              </h2>
              <p className="mt-2 text-sm text-text-body/70">
                Track status, cancellation requests, and administrative approval notes for your venue allocations.
              </p>

              <div className="mt-6 flex flex-col gap-3">
                <Button asChild variant="outline" className="w-fit">
                  <Link to="/bookings">
                    View All Bookings
                    <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
                  </Link>
                </Button>

                {/* AI Agent Highlight Callout */}
                <div className="mt-4 rounded-xl border border-border-color bg-surface-alt p-4">
                  <div className="flex items-center gap-2 text-brand-azure-strong">
                    <Bot className="h-5 w-5" aria-hidden="true" />
                    <span className="font-heading font-semibold text-sm">Complex Booking?</span>
                  </div>
                  <p className="mt-2 text-xs text-text-body/80">
                    Talk to the CampusFlow AI Agent. It coordinates room, guest house, and parking simultaneously with zero clash.
                  </p>
                  <Button asChild variant="default" size="sm" className="mt-3">
                    <Link to="/">Open Chat Assistant</Link>
                  </Button>
                </div>
              </div>
            </div>

            {/* Right 2/3: 2-Column Grid of White Cards */}
            <div className="lg:col-span-8">
              {bookingsLoading ? (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {[1, 2, 3, 4].map((n) => (
                    <div key={n} className="h-44 animate-pulse rounded-xl border border-border-color bg-surface" />
                  ))}
                </div>
              ) : recentBookings.length === 0 ? (
                <div className="rounded-xl border border-dashed border-border-color bg-surface p-8 text-center">
                  <Calendar className="mx-auto h-10 w-10 text-text-body/40" aria-hidden="true" />
                  <h3 className="mt-2 font-heading font-semibold text-text-heading">No bookings yet</h3>
                  <p className="mt-1 text-sm text-text-body/70">
                    Use our Classroom Finder or Booking Wizard to reserve your first venue.
                  </p>
                  <Button asChild variant="orange" size="sm" className="mt-4">
                    <Link to="/bookings/new">Create a Booking</Link>
                  </Button>
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {recentBookings.map((b) => (
                    <Card key={b.id} className="border border-border-color bg-surface shadow-xs transition hover:shadow-sm">
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
                            <span>View Details</span>
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
