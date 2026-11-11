import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Building,
  Users,
  MapPin,
  CheckCircle2,
  XCircle,
  ShieldAlert,
} from 'lucide-react';
import { venuesApi } from '@/api/venues';
import { Venue } from '@/api/types';
import { GradientHeading } from '@/components/ui/GradientHeading';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';

export function VenuesPage() {
  const navigate = useNavigate();

  const { data: venues = [], isLoading } = useQuery({
    queryKey: ['venues'],
    queryFn: () => venuesApi.getAll(),
  });

  const handleBookVenue = (venue: Venue) => {
    navigate('/wizard', {
      state: {
        prefill: {
          resourceType: 'VENUE',
          resourceId: venue.id,
          expectedAttendees: venue.capacity,
        },
      },
    });
  };

  return (
    <div className="min-h-screen bg-surface-alt py-8">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div>
          <GradientHeading as="h1" className="text-3xl font-extrabold sm:text-4xl">
            Campus Venues & Specialised Labs
          </GradientHeading>
          <p className="mt-2 text-sm text-text-body/80 sm:text-base">
            Book Rabindranath Tagore Amphitheatre, Open Air Theatre, and eligible engineering laboratories for events, hackathons, and guest lectures.
          </p>
        </div>

        {isLoading ? (
          <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="h-72 animate-pulse rounded-lg bg-surface shadow-sm" />
            ))}
          </div>
        ) : (
          <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {venues.map((venue) => {
              const isEligible = venue.isEligible;
              return (
                <Card
                  key={venue.id}
                  className={`flex flex-col justify-between shadow-sm transition hover:shadow-md ${
                    !isEligible ? 'border-dashed border-red-300 bg-surface/80' : ''
                  }`}
                >
                  <CardContent className="p-6">
                    <div className="flex items-start justify-between">
                      <Badge
                        variant={venue.type === 'AMPHITHEATRE' ? 'orange' : 'default'}
                        className="text-xs font-semibold"
                      >
                        {venue.type}
                      </Badge>
                      <span className="inline-flex items-center gap-1 rounded bg-surface-alt px-2 py-0.5 text-xs font-bold text-text-heading">
                        <Users className="h-3.5 w-3.5 text-brand-azure-strong" aria-hidden="true" />
                        {venue.capacity} seats
                      </span>
                    </div>

                    <h2 className="mt-3 font-heading text-xl font-bold text-text-heading">
                      {venue.name}
                    </h2>

                    <div className="mt-2 flex items-center gap-1.5 text-xs text-text-body/70">
                      <MapPin className="h-3.5 w-3.5 text-brand-azure-strong shrink-0" aria-hidden="true" />
                      <span>{venue.location}</span>
                    </div>

                    {/* Features Badges */}
                    <div className="mt-4 flex flex-wrap gap-1.5">
                      {venue.features.map((feat) => (
                        <span
                          key={feat}
                          className="rounded bg-surface-alt px-2 py-0.5 text-[11px] font-medium text-text-body/80 border border-border-color"
                        >
                          {feat.replace(/_/g, ' ')}
                        </span>
                      ))}
                    </div>

                    {/* Eligibility Status Requirement */}
                    <div className="mt-4 border-t border-border-color pt-3">
                      {isEligible ? (
                        <div className="flex items-center gap-1.5 text-xs font-medium text-green-700">
                          <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
                          <span>Eligible for Student Club & Faculty Bookings</span>
                        </div>
                      ) : (
                        <div className="rounded-md border border-red-200 bg-red-50 p-2.5 text-xs text-red-800">
                          <div className="flex items-start gap-1.5">
                            <XCircle className="mt-0.5 h-3.5 w-3.5 shrink-0 text-red-600" aria-hidden="true" />
                            <div>
                              <span className="font-semibold">Booking Restricted: </span>
                              <span>{venue.eligibilityReason}</span>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>

                    <div className="mt-6 border-t border-border-color pt-4">
                      {isEligible ? (
                        <Button
                          type="button"
                          onClick={() => handleBookVenue(venue)}
                          variant="orange"
                          className="w-full font-semibold"
                        >
                          <Building className="mr-2 h-4 w-4" aria-hidden="true" />
                          Book This Venue
                        </Button>
                      ) : (
                        <Button
                          type="button"
                          disabled
                          variant="outline"
                          className="w-full cursor-not-allowed opacity-60"
                        >
                          <ShieldAlert className="mr-2 h-4 w-4 text-red-500" aria-hidden="true" />
                          Special Authorization Required
                        </Button>
                      )}
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
