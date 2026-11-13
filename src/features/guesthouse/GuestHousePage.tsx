import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Home,
  Users,
  Calendar,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import { guestHouseApi } from '@/api/guesthouse';
import { GuestHouseRoom } from '@/api/types';
import { useAuth } from '@/features/auth/useAuth';
import { PageHeader } from '@/components/layout/PageHeader';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';

export function GuestHousePage() {
  const { hasRole } = useAuth();
  const navigate = useNavigate();

  const today = new Date().toISOString().split('T')[0];
  const nextDay = new Date(Date.now() + 86400000 * 2).toISOString().split('T')[0];

  const [checkIn, setCheckIn] = useState(today);
  const [checkOut, setCheckOut] = useState(nextDay);
  const [selectedType, setSelectedType] = useState<string>('ALL');

  const { data: rooms = [], isLoading } = useQuery({
    queryKey: ['guesthouse', 'rooms', { checkIn, checkOut }],
    queryFn: () => guestHouseApi.getRooms({ checkIn, checkOut }),
  });

  const filteredRooms = rooms.filter((r) => {
    if (selectedType === 'ALL') return true;
    return r.roomType === selectedType;
  });

  const canBook = hasRole(['STAFF', 'ADMIN']);

  const handleReserve = (room: GuestHouseRoom) => {
    navigate('/wizard', {
      state: {
        prefill: {
          resourceType: 'GUEST_HOUSE',
          guestHouseRoomId: room.id,
          guestHouseCheckIn: checkIn,
          guestHouseCheckOut: checkOut,
        },
      },
    });
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <PageHeader
          title="MDC Guest House"
          description="Reserve rooms at the JKLU Management Development Centre guest house for visiting speakers, delegates, and official guests."
        />

        {/* Date & Type Selector Panel */}
        <Card className="mt-6 shadow-sm">
          <CardContent className="p-6">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div>
                <Label htmlFor="gh-checkin" className="flex items-center gap-1.5 text-xs font-semibold">
                  <Calendar className="h-3.5 w-3.5 text-text-body/60" aria-hidden="true" />
                  Check-in Date
                </Label>
                <Input
                  id="gh-checkin"
                  type="date"
                  value={checkIn}
                  onChange={(e) => setCheckIn(e.target.value)}
                  className="mt-1.5 text-sm"
                />
              </div>

              <div>
                <Label htmlFor="gh-checkout" className="flex items-center gap-1.5 text-xs font-semibold">
                  <Calendar className="h-3.5 w-3.5 text-text-body/60" aria-hidden="true" />
                  Check-out Date
                </Label>
                <Input
                  id="gh-checkout"
                  type="date"
                  value={checkOut}
                  onChange={(e) => setCheckOut(e.target.value)}
                  className="mt-1.5 text-sm"
                />
              </div>

              <div>
                <Label htmlFor="gh-type" className="flex items-center gap-1.5 text-xs font-semibold">
                  Room Category
                </Label>
                <select
                  id="gh-type"
                  value={selectedType}
                  onChange={(e) => setSelectedType(e.target.value)}
                  className="mt-1.5 flex h-9 w-full rounded-md border border-border-color bg-surface px-3 py-1 text-sm text-text-body shadow-sm focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong"
                >
                  <option value="ALL">All Categories (12 Rooms)</option>
                  <option value="STANDARD">Standard Rooms</option>
                  <option value="DELUXE">Deluxe Rooms</option>
                  <option value="EXECUTIVE">Executive Suites</option>
                  <option value="SUITE">VIP Presidential Suites</option>
                </select>
              </div>
            </div>

            {!canBook && (
              <div className="mt-4 rounded-md border border-brand-azure/30 bg-brand-azure/10 p-3 text-xs text-brand-azure-strong">
                <strong>Student Notice:</strong> Guest house reservations must be initiated by faculty or department staff. Student club leads can coordinate speaker stays using the Unified Event Wizard with staff co-sponsorship.
              </div>
            )}
          </CardContent>
        </Card>

        {/* Room Grid */}
        {isLoading ? (
          <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="h-72 animate-pulse rounded-lg bg-surface shadow-sm" />
            ))}
          </div>
        ) : (
          <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {filteredRooms.map((room) => (
              <Card key={room.id} className="flex flex-col justify-between shadow-sm transition hover:shadow-md">
                <CardContent className="p-6">
                  <div className="flex items-start justify-between">
                    <div>
                      <Badge variant="default" className="text-xs">
                        {room.roomType}
                      </Badge>
                      <h2 className="mt-2 font-heading text-2xl font-bold text-text-heading">
                        Room {room.roomNumber}
                      </h2>
                    </div>
                    <div className="text-right">
                      <span className="font-heading text-lg font-bold text-brand-azure-strong">
                        ₹{room.ratePerNight}
                      </span>
                      <span className="block text-[11px] text-text-body/60">/ night</span>
                    </div>
                  </div>

                  <div className="mt-3 flex items-center gap-4 text-xs text-text-body/70">
                    <span className="flex items-center gap-1">
                      <Users className="h-3.5 w-3.5 text-brand-azure-strong" aria-hidden="true" />
                      Up to {room.capacity} Guests
                    </span>
                    <span>•</span>
                    <span>{room.bedType} Bed</span>
                  </div>

                  {/* Amenities */}
                  <div className="mt-4 flex flex-wrap gap-1.5">
                    {room.amenities.map((amenity) => (
                      <span
                        key={amenity}
                        className="rounded bg-surface-alt px-2 py-0.5 text-[11px] font-medium text-text-body/80 border border-border-color"
                      >
                        {amenity.replace(/_/g, ' ')}
                      </span>
                    ))}
                  </div>

                  {/* Availability Badge */}
                  <div className="mt-5 border-t border-border-color pt-3">
                    {room.isAvailable ? (
                      <div className="flex items-center gap-1.5 text-xs font-semibold text-green-700">
                        <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
                        <span>Available for Selected Dates</span>
                      </div>
                    ) : (
                      <div className="flex items-center gap-1.5 text-xs font-semibold text-red-600">
                        <XCircle className="h-4 w-4" aria-hidden="true" />
                        <span>Reserved for Visiting Delegation</span>
                      </div>
                    )}
                  </div>

                  <div className="mt-5 border-t border-border-color pt-4">
                    <Button
                      type="button"
                      disabled={!room.isAvailable || !canBook}
                      onClick={() => handleReserve(room)}
                      variant="orange"
                      className="w-full font-semibold"
                    >
                      <Home className="mr-2 h-4 w-4" aria-hidden="true" />
                      {room.isAvailable ? 'Reserve This Room' : 'Unavailable'}
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
  );
}
