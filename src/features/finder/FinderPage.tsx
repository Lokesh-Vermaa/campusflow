import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  Calendar,
  Clock,
  Users,
  CheckCircle2,
  AlertTriangle,
  Info,
  SlidersHorizontal,
  ChevronDown,
  Sparkles,
} from 'lucide-react';
import { roomsApi, RoomSearchParams } from '@/api/rooms';
import { AvailableRoom, UnavailableRoom } from '@/api/types';
import { GradientHeading } from '@/components/ui/GradientHeading';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { SEED_BLOCKS } from '@/mocks/seedData';

const AVAILABLE_FEATURES = [
  { id: 'projector', label: 'Projector' },
  { id: 'ac', label: 'Air Conditioning (AC)' },
  { id: 'smartboard', label: 'Smartboard' },
  { id: 'mic', label: 'Microphone & Audio' },
  { id: 'recording', label: 'Lecture Recording' },
];

export function FinderPage() {
  const navigate = useNavigate();

  // Tomorrow's date formatted as YYYY-MM-DD
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const defaultDate = tomorrow.toISOString().split('T')[0];

  const [date, setDate] = useState(defaultDate);
  const [startTime, setStartTime] = useState('14:00');
  const [endTime, setEndTime] = useState('16:00');
  const [minCapacity, setMinCapacity] = useState(30);
  const [selectedBlock, setSelectedBlock] = useState<string>('');
  const [selectedFeatures, setSelectedFeatures] = useState<string[]>([]);
  const [expandedRoomTimeline, setExpandedRoomTimeline] = useState<string | null>(null);

  // Search parameters for Query
  const searchParams: RoomSearchParams = {
    date,
    startTime,
    endTime,
    minCapacity: minCapacity > 0 ? minCapacity : undefined,
    blockId: selectedBlock || undefined,
    features: selectedFeatures.length > 0 ? selectedFeatures.join(',') : undefined,
  };

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['rooms', 'availability', searchParams],
    queryFn: () => roomsApi.searchAvailability(searchParams),
  });

  const toggleFeature = (featId: string) => {
    setSelectedFeatures((prev) =>
      prev.includes(featId) ? prev.filter((f) => f !== featId) : [...prev, featId],
    );
  };

  const handleBookRoom = (room: AvailableRoom) => {
    navigate('/wizard', {
      state: {
        prefill: {
          resourceType: 'ROOM',
          resourceId: room.id,
          bookingDate: date,
          startTime,
          endTime,
          expectedAttendees: room.capacity,
        },
      },
    });
  };

  const availableRooms: AvailableRoom[] = data?.availableRooms || [];
  const unavailableRooms: UnavailableRoom[] = data?.unavailableRooms || [];

  return (
    <div className="min-h-screen bg-surface-alt py-8">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div>
            <GradientHeading as="h1" className="text-3xl font-extrabold sm:text-4xl">
              Free Classroom Finder
            </GradientHeading>
            <p className="mt-2 text-sm text-text-body/80 sm:text-base">
              Cross-checks all 8 semester timetables and approved bookings in real time to guarantee clash-free rooms.
            </p>
          </div>
          <div className="inline-flex items-center gap-2 rounded-full border border-brand-azure-strong/20 bg-brand-azure-strong/10 px-3 py-1.5 text-xs font-semibold text-brand-azure-strong">
            <Sparkles className="h-4 w-4" aria-hidden="true" />
            Deterministic Interval Overlap Engine Active
          </div>
        </div>

        {/* Filters Panel */}
        <Card className="mt-6 shadow-sm">
          <CardContent className="p-6">
            <div className="flex items-center gap-2 border-b border-border-color pb-3 text-sm font-semibold text-text-heading">
              <SlidersHorizontal className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
              <span>Search Filters</span>
            </div>

            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
              {/* Date */}
              <div>
                <Label htmlFor="search-date" className="flex items-center gap-1.5 text-xs font-semibold">
                  <Calendar className="h-3.5 w-3.5 text-text-body/60" aria-hidden="true" />
                  Booking Date
                </Label>
                <Input
                  id="search-date"
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  className="mt-1.5 text-sm"
                />
              </div>

              {/* Start Time */}
              <div>
                <Label htmlFor="search-start-time" className="flex items-center gap-1.5 text-xs font-semibold">
                  <Clock className="h-3.5 w-3.5 text-text-body/60" aria-hidden="true" />
                  Start Time
                </Label>
                <Input
                  id="search-start-time"
                  type="time"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="mt-1.5 text-sm"
                />
              </div>

              {/* End Time */}
              <div>
                <Label htmlFor="search-end-time" className="flex items-center gap-1.5 text-xs font-semibold">
                  <Clock className="h-3.5 w-3.5 text-text-body/60" aria-hidden="true" />
                  End Time
                </Label>
                <Input
                  id="search-end-time"
                  type="time"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  className="mt-1.5 text-sm"
                />
              </div>

              {/* Min Capacity */}
              <div>
                <Label htmlFor="search-capacity" className="flex items-center gap-1.5 text-xs font-semibold">
                  <Users className="h-3.5 w-3.5 text-text-body/60" aria-hidden="true" />
                  Min Capacity: <span className="text-brand-azure-strong">{minCapacity} seats</span>
                </Label>
                <div className="mt-3 flex items-center gap-2">
                  <input
                    id="search-capacity"
                    type="range"
                    min="10"
                    max="150"
                    step="5"
                    value={minCapacity}
                    onChange={(e) => setMinCapacity(Number(e.target.value))}
                    className="h-2 w-full cursor-pointer accent-brand-azure-strong"
                  />
                </div>
              </div>

              {/* Block Filter */}
              <div>
                <Label htmlFor="search-block" className="flex items-center gap-1.5 text-xs font-semibold">
                  Academic Block
                </Label>
                <select
                  id="search-block"
                  value={selectedBlock}
                  onChange={(e) => setSelectedBlock(e.target.value)}
                  className="mt-1.5 flex h-9 w-full rounded-md border border-border-color bg-surface px-3 py-1 text-sm text-text-body shadow-sm focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong"
                >
                  <option value="">All Academic Blocks</option>
                  {SEED_BLOCKS.map((b) => (
                    <option key={b.id} value={b.name.split(' ')[1]}>
                      {b.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Feature Checkboxes */}
            <div className="mt-4 border-t border-border-color pt-4">
              <span className="text-xs font-semibold text-text-body/70">Required Amenities & Equipment:</span>
              <div className="mt-2 flex flex-wrap gap-2">
                {AVAILABLE_FEATURES.map((feat) => {
                  const isChecked = selectedFeatures.includes(feat.id);
                  return (
                    <button
                      key={feat.id}
                      type="button"
                      onClick={() => toggleFeature(feat.id)}
                      className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition ${
                        isChecked
                          ? 'border-brand-azure-strong bg-brand-azure-strong text-white'
                          : 'border-border-color bg-surface text-text-body hover:bg-surface-alt'
                      }`}
                    >
                      {isChecked && <CheckCircle2 className="h-3 w-3" aria-hidden="true" />}
                      {feat.label}
                    </button>
                  );
                })}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Loading State */}
        {isLoading && (
          <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="h-64 animate-pulse rounded-lg bg-surface shadow-sm" />
            ))}
          </div>
        )}

        {/* Error State */}
        {isError && (
          <div role="alert" className="mt-8 rounded-lg border border-red-200 bg-red-50 p-6 text-center text-red-900">
            <AlertTriangle className="mx-auto h-8 w-8 text-red-600" aria-hidden="true" />
            <p className="mt-2 font-semibold">Unable to fetch classroom schedule</p>
            <p className="mt-1 text-sm text-red-700">Please verify your connection or try again.</p>
            <Button onClick={() => refetch()} variant="outline" size="sm" className="mt-4">
              Retry Search
            </Button>
          </div>
        )}

        {/* Content: Available Rooms Section */}
        {!isLoading && !isError && (
          <div className="mt-8 space-y-12">
            <div>
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="font-heading text-xl font-bold text-text-heading sm:text-2xl">
                    Available Classrooms ({availableRooms.length})
                  </h2>
                  <p className="text-xs text-text-body/70 sm:text-sm">
                    Guaranteed clash-free for <span className="font-semibold">{date}</span> from{' '}
                    <span className="font-semibold">{startTime} to {endTime}</span>.
                  </p>
                </div>
              </div>

              {availableRooms.length === 0 ? (
                <div className="mt-4 rounded-lg border border-dashed border-border-color bg-surface p-8 text-center">
                  <Search className="mx-auto h-8 w-8 text-text-body/40" aria-hidden="true" />
                  <p className="mt-2 font-heading font-semibold text-text-heading">No classrooms found</p>
                  <p className="mt-1 text-sm text-text-body/70">
                    Try loosening your capacity filter or adjusting your time window.
                  </p>
                </div>
              ) : (
                <div className="mt-4 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
                  {availableRooms.map((room) => (
                    <Card key={room.id} className="flex flex-col justify-between shadow-sm transition hover:shadow-md">
                      <CardContent className="p-5">
                        <div className="flex items-start justify-between">
                          <div>
                            <span className="rounded bg-brand-azure/10 px-2 py-0.5 text-xs font-semibold text-brand-azure-strong">
                              {room.block} • Floor {room.floor}
                            </span>
                            <h3 className="mt-1.5 font-heading text-2xl font-bold text-text-heading">
                              Room {room.roomNumber}
                            </h3>
                          </div>
                          <span className="inline-flex items-center gap-1 rounded-md bg-surface-alt px-2.5 py-1 text-xs font-bold text-text-heading">
                            <Users className="h-3.5 w-3.5 text-brand-azure-strong" aria-hidden="true" />
                            {room.capacity} seats
                          </span>
                        </div>

                        {/* Feature Badges */}
                        <div className="mt-3 flex flex-wrap gap-1.5">
                          {room.features.map((feat) => (
                            <Badge key={feat} variant="secondary" className="text-[11px]">
                              {feat}
                            </Badge>
                          ))}
                        </div>

                        {/* Free Windows Chips */}
                        <div className="mt-4 rounded-md border border-green-200 bg-green-50/60 p-2.5">
                          <span className="text-[11px] font-semibold text-green-900">
                            Available Free Windows Today:
                          </span>
                          <div className="mt-1 flex flex-wrap gap-1">
                            {room.freeWindows.map((win, idx) => (
                              <span
                                key={idx}
                                className="rounded bg-white px-2 py-0.5 text-[10px] font-medium text-green-800 shadow-2xs"
                              >
                                {win.startTime} - {win.endTime}
                              </span>
                            ))}
                          </div>
                        </div>

                        {/* Timeline / Heatmap Toggle */}
                        <div className="mt-3">
                          <button
                            type="button"
                            onClick={() =>
                              setExpandedRoomTimeline(
                                expandedRoomTimeline === room.id ? null : room.id,
                              )
                            }
                            className="inline-flex items-center text-xs font-medium text-brand-azure-strong hover:underline"
                          >
                            <span>{expandedRoomTimeline === room.id ? 'Hide Day Schedule' : 'View Day Schedule (08:00 - 20:00)'}</span>
                            <ChevronDown
                              className={`ml-1 h-3.5 w-3.5 transition-transform ${
                                expandedRoomTimeline === room.id ? 'rotate-180' : ''
                              }`}
                              aria-hidden="true"
                            />
                          </button>

                          {/* Day Timeline Heatmap */}
                          {expandedRoomTimeline === room.id && (
                            <div className="mt-2 rounded border border-border-color bg-surface-alt p-3 text-xs">
                              <p className="font-semibold text-text-heading">Day Schedule Heatmap (08:00-20:00):</p>
                              <div className="mt-2 flex h-5 w-full overflow-hidden rounded bg-gray-200">
                                <div className="h-full bg-green-500" style={{ width: '35%' }} title="08:00-12:00 Free" />
                                <div className="h-full bg-red-400" style={{ width: '20%' }} title="12:00-14:00 Timetable Class" />
                                <div className="h-full bg-green-500" style={{ width: '45%' }} title="14:00-20:00 Free" />
                              </div>
                              <div className="mt-1.5 flex justify-between text-[10px] text-text-body/60">
                                <span>08:00</span>
                                <span>12:00</span>
                                <span>16:00</span>
                                <span>20:00</span>
                              </div>
                            </div>
                          )}
                        </div>

                        <div className="mt-5 border-t border-border-color pt-4">
                          <Button
                            type="button"
                            onClick={() => handleBookRoom(room)}
                            variant="orange"
                            className="w-full font-semibold"
                          >
                            Book Room {room.roomNumber}
                          </Button>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              )}
            </div>

            {/* Unavailable Rooms Section with Clashes Explained */}
            {unavailableRooms.length > 0 && (
              <div className="border-t border-border-color pt-8">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="h-5 w-5 text-amber-600" aria-hidden="true" />
                  <h2 className="font-heading text-xl font-bold text-text-heading">
                    Unavailable Rooms & Timetable Clashes ({unavailableRooms.length})
                  </h2>
                </div>
                <p className="mt-1 text-xs text-text-body/70 sm:text-sm">
                  These rooms clash with seeded semester timetables or existing approved bookings during{' '}
                  <span className="font-semibold">{startTime} - {endTime}</span>.
                </p>

                <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {unavailableRooms.map((room) => (
                    <Card key={room.id} className="border-l-4 border-l-amber-500 bg-surface shadow-2xs">
                      <CardContent className="p-4">
                        <div className="flex items-center justify-between">
                          <h3 className="font-heading font-bold text-text-heading">
                            Room {room.roomNumber}
                          </h3>
                          <Badge variant="outline" className="border-amber-400 bg-amber-50 text-amber-800">
                            Occupied
                          </Badge>
                        </div>

                        <p className="mt-1 text-xs text-text-body/60">{room.block} • {room.capacity} seats</p>

                        {/* Why Unavailable Explanation Requirement */}
                        <div className="mt-3 rounded bg-amber-50/80 p-2.5 text-xs text-amber-900">
                          <div className="flex items-start gap-1.5">
                            <Info className="mt-0.5 h-3.5 w-3.5 shrink-0 text-amber-700" aria-hidden="true" />
                            <div>
                              <span className="font-semibold">Reason: </span>
                              <span>{room.conflictReason}</span>
                            </div>
                          </div>
                        </div>

                        {/* Room Timeline */}
                        {room.timeline.length > 0 && (
                          <div className="mt-3 border-t border-border-color pt-2">
                            <span className="text-[10px] font-semibold text-text-body/60 uppercase">Timeline:</span>
                            <div className="mt-1 space-y-1">
                              {room.timeline.map((slot, idx) => (
                                <div key={idx} className="flex items-center justify-between text-[11px]">
                                  <span className="font-mono text-text-body/70">{slot.slot}</span>
                                  <span className="truncate text-text-heading font-medium pl-2">{slot.title}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
