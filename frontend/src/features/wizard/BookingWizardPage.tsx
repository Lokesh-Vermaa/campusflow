import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  Building,
  Home,
  Car,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  ArrowLeft,
  ShieldCheck,
} from 'lucide-react';
import { bookingsApi } from '@/api/bookings';
import { roomsApi } from '@/api/rooms';
import { venuesApi } from '@/api/venues';
import { guestHouseApi } from '@/api/guesthouse';
import { parkingApi } from '@/api/parking';
import { CreateBookingRequest, Booking } from '@/api/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';

export function BookingWizardPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const prefill = (location.state as { prefill?: Partial<CreateBookingRequest> })?.prefill;

  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const defaultDate = tomorrow.toISOString().split('T')[0];

  const [currentStep, setCurrentStep] = useState(1);

  // Form State
  const [title, setTitle] = useState(prefill?.title || 'AI Buildathon Seminar');
  const [description, setDescription] = useState(
    prefill?.description || 'Department technical workshop and keynote speaker session',
  );
  const [bookingDate, setBookingDate] = useState(prefill?.bookingDate || defaultDate);
  const [startTime, setStartTime] = useState(prefill?.startTime || '14:00');
  const [endTime, setEndTime] = useState(prefill?.endTime || '16:00');
  const [expectedAttendees, setExpectedAttendees] = useState(prefill?.expectedAttendees || 40);

  // Primary Resource
  const [resourceType, setResourceType] = useState<'ROOM' | 'VENUE'>('ROOM');
  const [selectedResourceId, setSelectedResourceId] = useState<string>(
    prefill?.resourceId || '',
  );

  // Fetch real rooms and venues from API
  const { data: allRooms = [] } = useQuery({
    queryKey: ['rooms', 'all'],
    queryFn: () => roomsApi.getRooms(),
  });

  const { data: allVenues = [] } = useQuery({
    queryKey: ['venues', 'all'],
    queryFn: () => venuesApi.getAll(),
  });

  // Optional Guest House
  const [includeGuestHouse, setIncludeGuestHouse] = useState(false);
  const [selectedGuestHouseRoomId, setSelectedGuestHouseRoomId] = useState<string>('');
  const [guestHouseCheckIn, setGuestHouseCheckIn] = useState(defaultDate);
  const [guestHouseCheckOut, setGuestHouseCheckOut] = useState(defaultDate);

  // Optional Parking
  const [includeParking, setIncludeParking] = useState(false);
  const [selectedParkingZoneId, setSelectedParkingZoneId] = useState<string>('');
  const [visitorName, setVisitorName] = useState('Dr. R. K. Sharma');
  const [vehicleNumber, setVehicleNumber] = useState('RJ-14-CB-9021');

  // Completed booking state & error feedback
  const [createdBooking, setCreatedBooking] = useState<Booking | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Check room availability inline for conflict detection
  const { data: availability } = useQuery({
    queryKey: ['rooms', 'availability', { date: bookingDate, startTime, endTime }],
    queryFn: () =>
      roomsApi.searchAvailability({
        date: bookingDate,
        startTime,
        endTime,
      }),
  });

  const { data: guestHouseRooms = [] } = useQuery({
    queryKey: ['guesthouse', 'rooms'],
    queryFn: () => guestHouseApi.getRooms(),
  });

  const { data: parkingZones = [] } = useQuery({
    queryKey: ['parking', 'zones'],
    queryFn: () => parkingApi.getZones(),
  });

  useEffect(() => {
    if (!selectedResourceId) {
      if (resourceType === 'ROOM' && allRooms.length > 0) {
        setSelectedResourceId(allRooms[0].id);
      } else if (resourceType === 'VENUE' && allVenues.length > 0) {
        setSelectedResourceId(allVenues[0].id);
      }
    }
  }, [allRooms, allVenues, resourceType, selectedResourceId]);

  useEffect(() => {
    if (guestHouseRooms.length > 0 && !selectedGuestHouseRoomId) {
      setSelectedGuestHouseRoomId(guestHouseRooms[0].id);
    }
    if (parkingZones.length > 0 && !selectedParkingZoneId) {
      setSelectedParkingZoneId(parkingZones[0].id);
    }
  }, [guestHouseRooms, parkingZones, selectedGuestHouseRoomId, selectedParkingZoneId]);

  // Check if chosen room has an inline clash
  const currentRoomClash = availability?.unavailableRooms.find((r) => r.id === selectedResourceId);

  const bookingMutation = useMutation({
    mutationFn: (payload: CreateBookingRequest) => {
      const idempotencyKey = crypto.randomUUID();
      return bookingsApi.create(payload, idempotencyKey);
    },
    onSuccess: (data) => {
      setCreatedBooking(data);
    },
    onError: (err: unknown) => {
      const msg = err instanceof Error ? err.message : 'Failed to submit booking for approval.';
      setSubmitError(msg);
    },
  });

  const handleSubmitBooking = () => {
    setSubmitError(null);
    const chosenId = selectedResourceId || (resourceType === 'ROOM' ? allRooms[0]?.id : allVenues[0]?.id);
    if (!chosenId) {
      setSubmitError('Please select a valid venue before submitting.');
      return;
    }

    const payload: CreateBookingRequest = {
      title,
      description,
      resourceType: includeGuestHouse || includeParking ? 'MULTI_RESOURCE' : resourceType,
      resourceId: chosenId,
      bookingDate,
      startTime,
      endTime,
      expectedAttendees: Number(expectedAttendees),
      guestHouseRoomId: includeGuestHouse && selectedGuestHouseRoomId ? selectedGuestHouseRoomId : undefined,
      guestHouseCheckIn: includeGuestHouse ? guestHouseCheckIn : undefined,
      guestHouseCheckOut: includeGuestHouse ? guestHouseCheckOut : undefined,
      parkingRequests: includeParking && selectedParkingZoneId ? [{
        zoneId: selectedParkingZoneId,
        visitorName: visitorName || 'Campus Visitor',
        vehicleNumber: vehicleNumber || 'N/A',
      }] : undefined,
    };

    bookingMutation.mutate(payload);
  };

  // If completed, show confirmation card
  if (createdBooking) {
    return (
      <div className="min-h-screen bg-surface-alt py-12">
        <div className="mx-auto max-w-2xl px-4 sm:px-6">
          <Card className="border-t-4 border-t-green-600 shadow-lg">
            <CardContent className="p-8 text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-700">
                <CheckCircle2 className="h-8 w-8" aria-hidden="true" />
              </div>

              <h1 className="mt-4 font-heading text-2xl font-bold text-text-heading">
                Booking Request Submitted!
              </h1>

              <p className="mt-2 text-sm text-text-body/80">
                Your request has been routed to the administrative approval queue. A notification has
                been dispatched with zero resource conflicts.
              </p>

              <div className="my-6 rounded-lg border border-border-color bg-surface-alt p-4">
                <span className="text-xs text-text-body/60 uppercase">Booking Reference</span>
                <p className="font-heading text-2xl font-extrabold text-brand-azure-strong">
                  {createdBooking.bookingNumber}
                </p>
                <div className="mt-3 flex justify-center">
                  <Badge variant="warning">Status: Awaiting Admin Approval</Badge>
                </div>
              </div>

              <div className="flex flex-wrap justify-center gap-3">
                <Button onClick={() => navigate('/bookings')} variant="default">
                  View in My Bookings
                </Button>
                <Button onClick={() => navigate('/dashboard')} variant="outline">
                  Return to Dashboard
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
        <PageHeader
          title="Unified Event Booking Wizard"
          description="Coordinate classrooms, guest house rooms, and visitor parking in one bundled reservation."
        />

        {/* Wizard Steps Stepper */}
        <div className="mt-8 flex items-center justify-between border-b border-border-color pb-4">
          {[
            { num: 1, label: 'Event Details' },
            { num: 2, label: 'Primary Venue' },
            { num: 3, label: 'Guest House' },
            { num: 4, label: 'Parking' },
            { num: 5, label: 'Review & Submit' },
          ].map((step) => (
            <div key={step.num} className="flex items-center gap-2">
              <span
                className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold ${
                  currentStep === step.num
                    ? 'bg-brand-orange text-text-heading'
                    : currentStep > step.num
                      ? 'bg-brand-azure-strong text-white'
                      : 'bg-border-color text-text-body/60'
                }`}
              >
                {step.num}
              </span>
              <span
                className={`hidden text-xs font-semibold sm:inline ${
                  currentStep === step.num ? 'text-text-heading' : 'text-text-body/60'
                }`}
              >
                {step.label}
              </span>
            </div>
          ))}
        </div>

        {/* Step 1: Event Details */}
        {currentStep === 1 && (
          <Card className="mt-6 shadow-sm">
            <CardHeader>
              <CardTitle>Step 1: Event Details & Schedule</CardTitle>
              <CardDescription>Specify the timing, expected attendance, and event objective.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <Label htmlFor="wiz-title">Event Title</Label>
                <Input
                  id="wiz-title"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="mt-1"
                />
              </div>

              <div>
                <Label htmlFor="wiz-desc">Event Description</Label>
                <Input
                  id="wiz-desc"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="mt-1"
                />
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <div>
                  <Label htmlFor="wiz-date">Booking Date</Label>
                  <Input
                    id="wiz-date"
                    type="date"
                    value={bookingDate}
                    onChange={(e) => setBookingDate(e.target.value)}
                    className="mt-1"
                  />
                </div>
                <div>
                  <Label htmlFor="wiz-start">Start Time</Label>
                  <Input
                    id="wiz-start"
                    type="time"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    className="mt-1"
                  />
                </div>
                <div>
                  <Label htmlFor="wiz-end">End Time</Label>
                  <Input
                    id="wiz-end"
                    type="time"
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    className="mt-1"
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="wiz-attendees">Expected Attendees</Label>
                <Input
                  id="wiz-attendees"
                  type="number"
                  min="5"
                  max="500"
                  value={expectedAttendees}
                  onChange={(e) => setExpectedAttendees(Number(e.target.value))}
                  className="mt-1 max-w-xs"
                />
              </div>

              <div className="flex justify-end pt-4">
                <Button type="button" onClick={() => setCurrentStep(2)} variant="default">
                  Proceed to Select Venue
                  <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Step 2: Pick Primary Resource with Inline Conflict Detection */}
        {currentStep === 2 && (
          <Card className="mt-6 shadow-sm">
            <CardHeader>
              <CardTitle>Step 2: Choose Room or Amphitheatre</CardTitle>
              <CardDescription>
                Inline clash detection runs against scheduled semester timetables.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant={resourceType === 'ROOM' ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => {
                    setResourceType('ROOM');
                    if (allRooms.length > 0) setSelectedResourceId(allRooms[0].id);
                  }}
                >
                  Academic Classrooms
                </Button>
                <Button
                  type="button"
                  variant={resourceType === 'VENUE' ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => {
                    setResourceType('VENUE');
                    if (allVenues.length > 0) setSelectedResourceId(allVenues[0].id);
                  }}
                >
                  Amphitheatres & Labs
                </Button>
              </div>

              <div>
                <Label htmlFor="wiz-resource">Select Target Venue</Label>
                <select
                  id="wiz-resource"
                  value={selectedResourceId}
                  onChange={(e) => setSelectedResourceId(e.target.value)}
                  className="mt-1.5 flex h-10 w-full rounded-md border border-border-color bg-surface px-3 py-2 text-sm text-text-body shadow-sm focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong"
                >
                  {resourceType === 'ROOM'
                    ? allRooms.map((r) => (
                        <option key={r.id} value={r.id}>
                          Room {r.roomNumber} ({r.block}) — {r.capacity} seats
                        </option>
                      ))
                    : allVenues.map((v) => (
                        <option key={v.id} value={v.id} disabled={!v.isEligible}>
                          {v.name} ({v.type}) — {v.capacity} seats {!v.isEligible ? '[Restricted]' : ''}
                        </option>
                      ))}
                </select>
              </div>

              {/* Inline Conflict Warning & Alternatives Requirement */}
              {currentRoomClash ? (
                <div className="rounded-md border border-amber-300 bg-amber-50 p-4">
                  <div className="flex items-start gap-2">
                    <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-700" aria-hidden="true" />
                    <div>
                      <p className="font-semibold text-amber-900">
                        Timetable Clash Detected for Selected Room!
                      </p>
                      <p className="mt-1 text-xs text-amber-800">{currentRoomClash.conflictReason}</p>

                      <div className="mt-3">
                        <span className="text-xs font-bold text-amber-900">Suggested Free Alternatives:</span>
                        <div className="mt-1 flex flex-wrap gap-2">
                          {availability?.availableRooms.slice(0, 3).map((alt) => (
                            <button
                              key={alt.id}
                              type="button"
                              onClick={() => setSelectedResourceId(alt.id)}
                              className="rounded border border-amber-400 bg-white px-2.5 py-1 text-xs font-semibold text-amber-950 hover:bg-amber-100"
                            >
                              Switch to Room {alt.roomNumber} ({alt.capacity} seats)
                            </button>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex items-center gap-2 rounded-md border border-green-200 bg-green-50 p-3 text-xs text-green-900 font-medium">
                  <CheckCircle2 className="h-4 w-4 text-green-700" aria-hidden="true" />
                  No timetable conflicts detected for this venue and time slot.
                </div>
              )}

              <div className="flex justify-between pt-4">
                <Button type="button" onClick={() => setCurrentStep(1)} variant="outline">
                  <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
                  Back
                </Button>
                <Button
                  type="button"
                  onClick={() => setCurrentStep(3)}
                  variant="default"
                  disabled={Boolean(currentRoomClash)}
                >
                  Proceed to Guest House
                  <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Step 3: Optional Guest House */}
        {currentStep === 3 && (
          <Card className="mt-6 shadow-sm">
            <CardHeader>
              <CardTitle>Step 3: MDC Guest House (Optional)</CardTitle>
              <CardDescription>Provide accommodation for invited guests and keynote speakers.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-2 rounded-md border border-border-color p-4">
                <input
                  id="include-gh"
                  type="checkbox"
                  checked={includeGuestHouse}
                  onChange={(e) => setIncludeGuestHouse(e.target.checked)}
                  className="h-4 w-4 rounded accent-brand-azure-strong"
                />
                <Label htmlFor="include-gh" className="cursor-pointer text-sm font-semibold">
                  Reserve a room in MDC Guest House for an external speaker or visitor
                </Label>
              </div>

              {includeGuestHouse && (
                <div className="space-y-4 rounded-md border border-border-color bg-surface-alt p-4">
                  <div>
                    <Label htmlFor="wiz-gh-room">Select Guest House Room</Label>
                    <select
                      id="wiz-gh-room"
                      value={selectedGuestHouseRoomId}
                      onChange={(e) => setSelectedGuestHouseRoomId(e.target.value)}
                      className="mt-1 flex h-10 w-full rounded-md border border-border-color bg-surface px-3 py-2 text-sm text-text-body shadow-sm"
                    >
                      {guestHouseRooms.map((gh) => (
                        <option key={gh.id} value={gh.id}>
                          Room {gh.roomNumber} ({gh.roomType}) — ₹{gh.ratePerNight}/night
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                      <Label htmlFor="wiz-gh-in">Check-in Date</Label>
                      <Input
                        id="wiz-gh-in"
                        type="date"
                        value={guestHouseCheckIn}
                        onChange={(e) => setGuestHouseCheckIn(e.target.value)}
                        className="mt-1"
                      />
                    </div>
                    <div>
                      <Label htmlFor="wiz-gh-out">Check-out Date</Label>
                      <Input
                        id="wiz-gh-out"
                        type="date"
                        value={guestHouseCheckOut}
                        onChange={(e) => setGuestHouseCheckOut(e.target.value)}
                        className="mt-1"
                      />
                    </div>
                  </div>
                </div>
              )}

              <div className="flex justify-between pt-4">
                <Button type="button" onClick={() => setCurrentStep(2)} variant="outline">
                  <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
                  Back
                </Button>
                <Button type="button" onClick={() => setCurrentStep(4)} variant="default">
                  Proceed to Parking
                  <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Step 4: Optional Parking */}
        {currentStep === 4 && (
          <Card className="mt-6 shadow-sm">
            <CardHeader>
              <CardTitle>Step 4: Visitor Parking Allocation (Optional)</CardTitle>
              <CardDescription>Reserve a designated parking spot at the main gate or venue bay.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-2 rounded-md border border-border-color p-4">
                <input
                  id="include-pk"
                  type="checkbox"
                  checked={includeParking}
                  onChange={(e) => setIncludeParking(e.target.checked)}
                  className="h-4 w-4 rounded accent-brand-azure-strong"
                />
                <Label htmlFor="include-pk" className="cursor-pointer text-sm font-semibold">
                  Allocate reserved visitor parking pass
                </Label>
              </div>

              {includeParking && (
                <div className="space-y-4 rounded-md border border-border-color bg-surface-alt p-4">
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <div>
                      <Label htmlFor="wiz-vis-name">Visitor Name</Label>
                      <Input
                        id="wiz-vis-name"
                        value={visitorName}
                        onChange={(e) => setVisitorName(e.target.value)}
                        className="mt-1"
                      />
                    </div>
                    <div>
                      <Label htmlFor="wiz-veh-num">Vehicle Registration Number</Label>
                      <Input
                        id="wiz-veh-num"
                        value={vehicleNumber}
                        onChange={(e) => setVehicleNumber(e.target.value)}
                        className="mt-1 font-mono uppercase"
                      />
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="wiz-pk-zone">Target Parking Zone</Label>
                    <select
                      id="wiz-pk-zone"
                      value={selectedParkingZoneId}
                      onChange={(e) => setSelectedParkingZoneId(e.target.value)}
                      className="mt-1 flex h-10 w-full rounded-md border border-border-color bg-surface px-3 py-2 text-sm text-text-body shadow-sm"
                    >
                      {parkingZones.map((z) => (
                        <option key={z.id} value={z.id}>
                          {z.name} ({z.availableSlots} slots free)
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              )}

              <div className="flex justify-between pt-4">
                <Button type="button" onClick={() => setCurrentStep(3)} variant="outline">
                  <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
                  Back
                </Button>
                <Button type="button" onClick={() => setCurrentStep(5)} variant="default">
                  Review & Finalize
                  <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Step 5: Review & Submit */}
        {currentStep === 5 && (
          <Card className="mt-6 shadow-sm">
            <CardHeader>
              <CardTitle>Step 5: Review & Submit for Approval</CardTitle>
              <CardDescription>Verify all bundled resources before dispatching to the admin queue.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="rounded-lg border border-border-color p-4 space-y-3">
                <div className="flex justify-between border-b border-border-color pb-2">
                  <span className="font-semibold text-text-heading">{title}</span>
                  <Badge variant="warning">Awaiting Admin Approval</Badge>
                </div>
                <p className="text-xs text-text-body/70">{description}</p>

                <div className="grid grid-cols-2 gap-4 text-xs">
                  <div>
                    <span className="text-text-body/60">Schedule: </span>
                    <span className="font-semibold">{bookingDate} ({startTime} - {endTime})</span>
                  </div>
                  <div>
                    <span className="text-text-body/60">Expected Attendees: </span>
                    <span className="font-semibold">{expectedAttendees}</span>
                  </div>
                </div>
              </div>

              {/* Bundled Resources Summary */}
              <div className="space-y-2">
                <span className="text-xs font-bold text-text-heading uppercase">Allocated Resources:</span>

                <div className="flex items-center justify-between rounded-md border border-border-color bg-surface-alt p-3 text-xs">
                  <span className="flex items-center gap-2 font-semibold">
                    <Building className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
                    Primary Venue: {(() => {
                      if (resourceType === 'ROOM') {
                        const r = allRooms.find((x) => x.id === selectedResourceId);
                        return r ? `Room ${r.roomNumber} (${r.block})` : 'Academic Classroom';
                      } else {
                        const v = allVenues.find((x) => x.id === selectedResourceId);
                        return v ? `${v.name} (${v.type})` : 'Amphitheatre / Lab';
                      }
                    })()}
                  </span>
                  <Badge variant="success">Confirmed Available</Badge>
                </div>

                {includeGuestHouse && (
                  <div className="flex items-center justify-between rounded-md border border-border-color bg-surface-alt p-3 text-xs">
                    <span className="flex items-center gap-2 font-semibold">
                      <Home className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
                      MDC Guest House Stay: {guestHouseCheckIn} to {guestHouseCheckOut}
                    </span>
                    <Badge variant="success">Room Reserved</Badge>
                  </div>
                )}

                {includeParking && (
                  <div className="flex items-center justify-between rounded-md border border-border-color bg-surface-alt p-3 text-xs">
                    <span className="flex items-center gap-2 font-semibold">
                      <Car className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
                      Visitor Parking Permit: {vehicleNumber} ({visitorName})
                    </span>
                    <Badge variant="success">Slot Allocated</Badge>
                  </div>
                )}
              </div>

              <div className="rounded-md border border-brand-azure-strong/20 bg-brand-azure-strong/5 p-3 text-xs text-brand-azure-strong flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 shrink-0" aria-hidden="true" />
                <span>Every state-changing campus booking requires administrative approval. Your submission will appear in the Admin Approval Queue immediately.</span>
              </div>

              {submitError && (
                <div className="rounded-md border border-red-300 bg-red-50 p-3 text-xs font-semibold text-red-700">
                  {submitError}
                </div>
              )}

              <div className="flex justify-between pt-4">
                <Button type="button" onClick={() => setCurrentStep(4)} variant="outline">
                  <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
                  Back
                </Button>
                <Button
                  type="button"
                  onClick={handleSubmitBooking}
                  variant="orange"
                  className="font-bold text-text-heading"
                  disabled={bookingMutation.isPending}
                >
                  {bookingMutation.isPending ? 'Submitting to Admin Queue...' : 'Submit Booking for Approval'}
                </Button>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
  );
}
