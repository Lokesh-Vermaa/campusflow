import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Calendar,
  Clock,
  Bot,
  Ban,
  ChevronRight,
  Filter,
  Users,
} from 'lucide-react';
import { bookingsApi, BookingListParams } from '@/api/bookings';
import { Booking } from '@/api/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
  DrawerDescription,
  DrawerClose,
} from '@/components/ui/Drawer';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from '@/components/ui/AlertDialog';

export function MyBookingsPage() {
  const queryClient = useQueryClient();

  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const [activeBooking, setActiveBooking] = useState<Booking | null>(null);
  const [cancellingBookingId, setCancellingBookingId] = useState<string | null>(null);

  const queryParams: BookingListParams = {
    status: selectedStatus === 'ALL' ? undefined : selectedStatus,
    page: currentPage,
    pageSize: 8,
  };

  const { data, isLoading } = useQuery({
    queryKey: ['bookings', 'my', queryParams],
    queryFn: () => bookingsApi.getMyBookings(queryParams),
  });

  const cancelMutation = useMutation({
    mutationFn: (id: string) => bookingsApi.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      setCancellingBookingId(null);
      if (activeBooking) {
        setActiveBooking((prev) => (prev ? { ...prev, status: 'CANCELLED' } : null));
      }
    },
  });

  const bookings = data?.items || [];
  const pagination = data?.pagination;

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <PageHeader
          title="My Bookings"
          description="Track and manage all your venue, guest house, and parking booking requests."
        />

        {/* Status Filter Tabs */}
        <div className="mt-6 flex flex-wrap items-center gap-2 border-b border-border-color pb-4">
          <Filter className="mr-1 h-4 w-4 text-text-body/50" aria-hidden="true" />
          {['ALL', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'].map((tab) => (
            <button
              key={tab}
              type="button"
              onClick={() => {
                setSelectedStatus(tab);
                setCurrentPage(1);
              }}
              className={`rounded-full px-3.5 py-1 text-xs font-semibold transition ${
                selectedStatus === tab
                  ? 'bg-brand-azure-strong text-white'
                  : 'bg-surface text-text-body hover:bg-surface-alt border border-border-color'
              }`}
            >
              {tab === 'ALL' ? 'All Bookings' : tab}
            </button>
          ))}
        </div>

        {/* Bookings List */}
        {isLoading ? (
          <div className="mt-6 space-y-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-28 animate-pulse rounded-lg bg-surface shadow-sm" />
            ))}
          </div>
        ) : bookings.length === 0 ? (
          <div className="mt-8 rounded-lg border border-dashed border-border-color bg-surface p-12 text-center">
            <Calendar className="mx-auto h-12 w-12 text-text-body/40" aria-hidden="true" />
            <h3 className="mt-2 font-heading font-semibold text-text-heading">No bookings found</h3>
            <p className="mt-1 text-sm text-text-body/70">
              There are no bookings matching the selected filter criteria.
            </p>
          </div>
        ) : (
          <div className="mt-6 space-y-4">
            {bookings.map((b) => (
              <Card
                key={b.id}
                className="shadow-sm transition hover:shadow-md cursor-pointer"
                onClick={() => setActiveBooking(b)}
              >
                <CardContent className="flex flex-col justify-between gap-4 p-5 sm:flex-row sm:items-center">
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-mono text-xs font-bold text-brand-azure-strong">
                        {b.bookingNumber}
                      </span>
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
                      {b.agentReasoning && (
                        <span className="inline-flex items-center gap-1 rounded bg-purple-100 px-2 py-0.5 text-[10px] font-semibold text-purple-800">
                          <Bot className="h-3 w-3" aria-hidden="true" />
                          AI Coordinated
                        </span>
                      )}
                    </div>

                    <h2 className="font-heading text-lg font-bold text-text-heading">
                      {b.title}
                    </h2>

                    <p className="text-xs italic text-brand-azure-strong">
                      {b.resourceName} ({b.resourceType})
                    </p>

                    <div className="flex flex-wrap gap-4 pt-1 text-xs text-text-body/70">
                      <span className="flex items-center gap-1">
                        <Calendar className="h-3.5 w-3.5 text-text-body/50" aria-hidden="true" />
                        {b.bookingDate}
                      </span>
                      <span className="flex items-center gap-1">
                        <Clock className="h-3.5 w-3.5 text-text-body/50" aria-hidden="true" />
                        {b.startTime} - {b.endTime}
                      </span>
                      {b.expectedAttendees && (
                        <span className="flex items-center gap-1">
                          <Users className="h-3.5 w-3.5 text-text-body/50" aria-hidden="true" />
                          {b.expectedAttendees} Attendees
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    {b.status === 'PENDING' && (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          setCancellingBookingId(b.id);
                        }}
                        className="text-red-600 hover:bg-red-50 hover:text-red-700"
                      >
                        <Ban className="mr-1.5 h-3.5 w-3.5" aria-hidden="true" />
                        Cancel
                      </Button>
                    )}
                    <Button variant="ghost" size="sm" className="hidden sm:inline-flex">
                      Details
                      <ChevronRight className="ml-1 h-4 w-4" aria-hidden="true" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        {/* Server-Side Pagination */}
        {pagination && pagination.totalPages > 1 && (
          <div className="mt-8 flex items-center justify-between border-t border-border-color pt-4 text-xs">
            <span className="text-text-body/70">
              Showing page {pagination.page} of {pagination.totalPages} ({pagination.totalItems} total)
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={pagination.page <= 1}
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={pagination.page >= pagination.totalPages}
                onClick={() => setCurrentPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        )}

        {/* Booking Detail Drawer with Approval History & Agent Reasoning */}
        <Drawer open={Boolean(activeBooking)} onOpenChange={() => setActiveBooking(null)}>
          <DrawerContent className="max-h-[85vh]">
            <DrawerHeader>
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs font-bold text-brand-azure-strong">
                  {activeBooking?.bookingNumber}
                </span>
                <Badge
                  variant={
                    activeBooking?.status === 'APPROVED'
                      ? 'success'
                      : activeBooking?.status === 'PENDING'
                        ? 'warning'
                        : activeBooking?.status === 'REJECTED'
                          ? 'destructive'
                          : 'secondary'
                  }
                >
                  {activeBooking?.status}
                </Badge>
              </div>
              <DrawerTitle className="text-left font-heading text-xl">
                {activeBooking?.title}
              </DrawerTitle>
              <DrawerDescription className="text-left">
                {activeBooking?.resourceName} • {activeBooking?.bookingDate}
              </DrawerDescription>
            </DrawerHeader>

            {activeBooking && (
              <div className="space-y-6 overflow-y-auto px-6 py-4 text-sm">
                <div>
                  <h4 className="font-semibold text-text-heading">Event Details</h4>
                  <p className="mt-1 text-xs text-text-body/80">{activeBooking.description || 'No description provided.'}</p>
                  <div className="mt-3 grid grid-cols-2 gap-4 text-xs">
                    <div>
                      <span className="text-text-body/60">Timing: </span>
                      <span className="font-semibold">{activeBooking.startTime} - {activeBooking.endTime}</span>
                    </div>
                    <div>
                      <span className="text-text-body/60">Requester: </span>
                      <span className="font-semibold">{activeBooking.requesterName}</span>
                    </div>
                  </div>
                </div>

                {/* Agent Reasoning Trace if Agent Created */}
                {activeBooking.agentReasoning && (
                  <div className="rounded-lg border border-purple-200 bg-purple-50/70 p-4">
                    <div className="flex items-center gap-1.5 text-purple-900 font-semibold text-xs">
                      <Bot className="h-4 w-4" aria-hidden="true" />
                      <span>AI Agent Reasoning & Conflict Trace</span>
                    </div>
                    <p className="mt-2 text-xs text-purple-950 font-mono">
                      {activeBooking.agentReasoning}
                    </p>
                  </div>
                )}

                {/* Conflict Check Verification */}
                {activeBooking.conflictCheckResult && (
                  <div className="rounded-lg border border-border-color bg-surface-alt p-4 text-xs">
                    <span className="font-semibold text-text-heading">Automated Conflict Verification:</span>
                    <p className="mt-1 text-text-body/80">{activeBooking.conflictCheckResult.summary}</p>
                  </div>
                )}

                {/* Admin Note / Rejection Reason */}
                {activeBooking.adminNote && (
                  <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-xs">
                    <span className="font-semibold text-amber-900">Administrator Review Note:</span>
                    <p className="mt-1 text-amber-950">{activeBooking.adminNote}</p>
                  </div>
                )}

                {/* Bundled Resources */}
                {(activeBooking.guestHouseRoomName || activeBooking.parkingZoneName) && (
                  <div className="border-t border-border-color pt-4 text-xs space-y-2">
                    <span className="font-semibold text-text-heading">Additional Bundled Allocations:</span>
                    {activeBooking.guestHouseRoomName && (
                      <p className="text-text-body/80">
                        • Guest House: {activeBooking.guestHouseRoomName} ({activeBooking.guestHouseCheckIn} to {activeBooking.guestHouseCheckOut})
                      </p>
                    )}
                    {activeBooking.parkingZoneName && (
                      <p className="text-text-body/80">• Parking: {activeBooking.parkingZoneName}</p>
                    )}
                  </div>
                )}

                <div className="flex justify-end gap-3 pt-4 border-t border-border-color">
                  {activeBooking.status === 'PENDING' && (
                    <Button
                      variant="destructive"
                      size="sm"
                      onClick={() => setCancellingBookingId(activeBooking.id)}
                    >
                      Cancel Booking
                    </Button>
                  )}
                  <DrawerClose asChild>
                    <Button variant="outline" size="sm">
                      Close
                    </Button>
                  </DrawerClose>
                </div>
              </div>
            )}
          </DrawerContent>
        </Drawer>

        {/* Cancellation Confirmation Dialog */}
        <AlertDialog
          open={Boolean(cancellingBookingId)}
          onOpenChange={() => setCancellingBookingId(null)}
        >
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Cancel Booking Request?</AlertDialogTitle>
              <AlertDialogDescription>
                Are you sure you want to cancel this booking? The reserved room and any associated guest house or parking allocations will immediately be released.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Keep Booking</AlertDialogCancel>
              <AlertDialogAction
                onClick={() => {
                  if (cancellingBookingId) {
                    cancelMutation.mutate(cancellingBookingId);
                  }
                }}
                className="bg-red-600 hover:bg-red-700 text-white"
              >
                Confirm Cancellation
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
  );
}
