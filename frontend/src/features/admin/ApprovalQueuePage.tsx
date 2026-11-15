import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ShieldCheck,
  CheckCircle2,
  XCircle,
  Calendar,
  User,
  Building,
  Bot,
} from 'lucide-react';
import { adminApi } from '@/api/admin';
import { Booking } from '@/api/types';
import { PageHeader } from '@/components/layout/PageHeader';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/Dialog';

export function ApprovalQueuePage() {
  const queryClient = useQueryClient();
  const [currentPage, setCurrentPage] = useState(1);
  const [approvingBooking, setApprovingBooking] = useState<Booking | null>(null);
  const [rejectingBooking, setRejectingBooking] = useState<Booking | null>(null);
  const [approvalNote, setApprovalNote] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [rejectionError, setRejectionError] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'approvals', currentPage],
    queryFn: () => adminApi.getApprovals({ page: currentPage, pageSize: 8 }),
  });

  const approveMutation = useMutation({
    mutationFn: ({ id, note }: { id: string; note?: string }) => adminApi.approveBooking(id, note),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'approvals'] });
      setApprovingBooking(null);
      setApprovalNote('');
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => adminApi.rejectBooking(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'approvals'] });
      setRejectingBooking(null);
      setRejectionReason('');
      setRejectionError('');
    },
  });

  const handleConfirmReject = () => {
    if (rejectionReason.trim().length < 5) {
      setRejectionError('Rejection reason must be at least 5 characters to audit properly.');
      return;
    }
    if (rejectingBooking) {
      rejectMutation.mutate({ id: rejectingBooking.id, reason: rejectionReason.trim() });
    }
  };

  const pendingItems = data?.items || [];
  const pagination = data?.pagination;

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <PageHeader
        title="Booking Approval Queue"
        description="Every state-changing booking requires admin verification. Review timetable conflict reports and agent recommendations before approving."
        badge={
          <span className="inline-flex items-center gap-2 rounded-full border border-brand-azure-strong/20 bg-brand-azure-strong/10 px-3 py-1 text-xs font-semibold text-brand-azure-strong">
            <ShieldCheck className="h-4 w-4" aria-hidden="true" />
            Administrative Governance
          </span>
        }
      />

        {/* Queue Items */}
        {isLoading ? (
          <div className="mt-8 space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-44 animate-pulse rounded-lg bg-surface shadow-sm" />
            ))}
          </div>
        ) : pendingItems.length === 0 ? (
          <div className="mt-8 rounded-lg border border-dashed border-border-color bg-surface p-12 text-center">
            <CheckCircle2 className="mx-auto h-12 w-12 text-green-600" aria-hidden="true" />
            <h3 className="mt-2 font-heading font-semibold text-text-heading">Approval Queue Empty</h3>
            <p className="mt-1 text-sm text-text-body/70">
              All submitted campus bookings have been processed. Great job!
            </p>
          </div>
        ) : (
          <div className="mt-8 space-y-6">
            {pendingItems.map((booking) => (
              <Card key={booking.id} className="border-l-4 border-l-amber-500 shadow-sm">
                <CardContent className="p-6">
                  <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-start">
                    <div className="space-y-3 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-mono text-xs font-bold text-brand-azure-strong">
                          {booking.bookingNumber}
                        </span>
                        <Badge variant="warning">Awaiting Admin Action</Badge>
                        <span className="text-xs text-text-body/60">
                          Submitted {new Date(booking.createdAt).toLocaleDateString()}
                        </span>
                      </div>

                      <h2 className="font-heading text-xl font-bold text-text-heading">
                        {booking.title}
                      </h2>
                      <p className="text-sm text-text-body/80">{booking.description}</p>

                      <div className="grid grid-cols-1 gap-2 sm:grid-cols-3 text-xs border-y border-border-color py-3">
                        <div className="flex items-center gap-1.5">
                          <User className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
                          <div>
                            <span className="font-semibold text-text-heading">{booking.requesterName}</span>
                            <span className="block text-[11px] text-text-body/60">{booking.requesterRole} • {booking.requesterEmail}</span>
                          </div>
                        </div>

                        <div className="flex items-center gap-1.5">
                          <Building className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
                          <div>
                            <span className="font-semibold text-text-heading">{booking.resourceName}</span>
                            <span className="block text-[11px] text-text-body/60">{booking.expectedAttendees || 40} expected attendees</span>
                          </div>
                        </div>

                        <div className="flex items-center gap-1.5">
                          <Calendar className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
                          <div>
                            <span className="font-semibold text-text-heading">{booking.bookingDate}</span>
                            <span className="block text-[11px] text-text-body/60">{booking.startTime} - {booking.endTime}</span>
                          </div>
                        </div>
                      </div>

                      {/* Conflict Check Verification Result */}
                      {booking.conflictCheckResult && (
                        <div className="rounded-md border border-green-200 bg-green-50 p-3 text-xs text-green-900">
                          <div className="flex items-center gap-1.5 font-semibold">
                            <CheckCircle2 className="h-4 w-4 text-green-700" aria-hidden="true" />
                            <span>Automated Conflict Check: Passed</span>
                          </div>
                          <p className="mt-1 text-[11px] text-green-800">
                            {booking.conflictCheckResult.summary}
                          </p>
                        </div>
                      )}

                      {/* Agent Reasoning & Recommendation Trace */}
                      {booking.agentReasoning && (
                        <div className="rounded-md border border-purple-200 bg-purple-50 p-3 text-xs text-purple-950">
                          <div className="flex items-center gap-1.5 font-semibold text-purple-900">
                            <Bot className="h-4 w-4" aria-hidden="true" />
                            <span>CampusFlow Agent Recommendation:</span>
                          </div>
                          <p className="mt-1 font-mono text-[11px] leading-relaxed">
                            {booking.agentReasoning}
                          </p>
                        </div>
                      )}

                      {/* Bundled Resources */}
                      {(booking.guestHouseRoomName || booking.parkingZoneName) && (
                        <div className="rounded-md border border-border-color bg-surface-alt p-3 text-xs text-text-body/80 space-y-1">
                          <span className="font-semibold text-text-heading">Bundled Resources:</span>
                          {booking.guestHouseRoomName && (
                            <p>• MDC Guest House Room: {booking.guestHouseRoomName}</p>
                          )}
                          {booking.parkingZoneName && (
                            <p>• Parking Bay: {booking.parkingZoneName}</p>
                          )}
                        </div>
                      )}
                    </div>

                    {/* Action Buttons */}
                    <div className="flex flex-row lg:flex-col gap-2 shrink-0">
                      <Button
                        type="button"
                        onClick={() => {
                          setApprovingBooking(booking);
                          setApprovalNote('Approved after schedule verification.');
                        }}
                        className="bg-green-600 hover:bg-green-700 text-white font-semibold"
                        size="sm"
                      >
                        <CheckCircle2 className="mr-1.5 h-4 w-4" aria-hidden="true" />
                        Approve Booking
                      </Button>

                      <Button
                        type="button"
                        onClick={() => {
                          setRejectingBooking(booking);
                          setRejectionReason('');
                          setRejectionError('');
                        }}
                        variant="destructive"
                        size="sm"
                      >
                        <XCircle className="mr-1.5 h-4 w-4" aria-hidden="true" />
                        Reject Request
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        {/* Server-side Pagination */}
        {pagination && pagination.totalPages > 1 && (
          <div className="mt-8 flex items-center justify-between border-t border-border-color pt-4 text-xs">
            <span className="text-text-body/70">
              Showing page {pagination.page} of {pagination.totalPages}
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

        {/* Approve Confirmation Dialog */}
        <Dialog open={Boolean(approvingBooking)} onOpenChange={() => setApprovingBooking(null)}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Approve Booking Request?</DialogTitle>
              <DialogDescription>
                Confirm allocation of {approvingBooking?.resourceName} for {approvingBooking?.requesterName}.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-3 py-2">
              <Label htmlFor="approve-note">Administrative Note (Optional)</Label>
              <Input
                id="approve-note"
                value={approvalNote}
                onChange={(e) => setApprovalNote(e.target.value)}
                placeholder="e.g. Approved. Keys available at security desk."
              />
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => setApprovingBooking(null)}>
                Cancel
              </Button>
              <Button
                onClick={() => {
                  if (approvingBooking) {
                    approveMutation.mutate({ id: approvingBooking.id, note: approvalNote });
                  }
                }}
                className="bg-green-600 hover:bg-green-700 text-white font-semibold"
                disabled={approveMutation.isPending}
              >
                {approveMutation.isPending ? 'Confirming...' : 'Confirm Approval'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Reject Confirmation Dialog (Rejection requires reason with validation) */}
        <Dialog open={Boolean(rejectingBooking)} onOpenChange={() => setRejectingBooking(null)}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle className="text-red-600">Reject Booking Request</DialogTitle>
              <DialogDescription>
                A formal rejection reason is required for administrative audit and requester feedback.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-3 py-2">
              <Label htmlFor="reject-reason">
                Rejection Reason <span className="text-red-600">*</span>
              </Label>
              <textarea
                id="reject-reason"
                rows={3}
                value={rejectionReason}
                onChange={(e) => {
                  setRejectionReason(e.target.value);
                  setRejectionError('');
                }}
                placeholder="e.g. VIP delegation scheduled in adjacent hall; please select Block B."
                className="w-full rounded-md border border-border-color p-2.5 text-sm text-text-body focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong"
              />
              {rejectionError && (
                <p className="text-xs text-red-600">{rejectionError}</p>
              )}
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => setRejectingBooking(null)}>
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleConfirmReject}
                disabled={rejectMutation.isPending}
              >
                {rejectMutation.isPending ? 'Rejecting...' : 'Confirm Rejection'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
  );
}
