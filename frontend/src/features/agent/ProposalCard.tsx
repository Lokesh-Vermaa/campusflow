import { useState } from 'react';
import {
  Building,
  Home,
  Car,
  CheckCircle2,
  Send,
  ShieldCheck,
} from 'lucide-react';
import { AgentProposal } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';

interface ProposalCardProps {
  proposal: AgentProposal;
  onSubmitForApproval: (proposal: AgentProposal) => Promise<void>;
}

export function ProposalCard({ proposal, onSubmitForApproval }: ProposalCardProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(proposal.status === 'SUBMITTED');

  const handleSubmit = async () => {
    setIsSubmitting(true);
    try {
      await onSubmitForApproval(proposal);
      setSubmitted(true);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="my-3 rounded-xl border-2 border-brand-azure-strong/30 bg-surface p-5 shadow-md">
      <div className="flex items-start justify-between border-b border-border-color pb-3">
        <div>
          <span className="rounded bg-brand-azure-strong/10 px-2 py-0.5 text-[10px] font-bold text-brand-azure-strong uppercase">
            Synthesized Event Proposal
          </span>
          <h3 className="mt-1 font-heading text-lg font-bold text-text-heading">
            {proposal.title}
          </h3>
        </div>

        <Badge variant={submitted ? 'warning' : 'default'}>
          {submitted ? 'Awaiting Admin Approval' : 'Ready for Submission'}
        </Badge>
      </div>

      {/* Bundled Resource Elements */}
      <div className="mt-4 space-y-2.5 text-xs">
        {/* Venue Element */}
        <div className="flex items-center justify-between rounded-lg border border-border-color bg-surface-alt p-3">
          <div className="flex items-center gap-2.5">
            <Building className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
            <div>
              <span className="font-semibold text-text-heading">Room / Venue: {proposal.roomName}</span>
              <div className="flex items-center gap-2 text-[11px] text-text-body/70">
                <span>Capacity: {proposal.capacity} seats</span>
                <span>•</span>
                <span>{proposal.date} ({proposal.time})</span>
              </div>
            </div>
          </div>
          <Badge variant="success" className="text-[10px]">Conflict-Free</Badge>
        </div>

        {/* Guest House Element */}
        {proposal.guestHouse && (
          <div className="flex items-center justify-between rounded-lg border border-border-color bg-surface-alt p-3">
            <div className="flex items-center gap-2.5">
              <Home className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
              <div>
                <span className="font-semibold text-text-heading">MDC Guest House</span>
                <p className="text-[11px] text-text-body/70">{proposal.guestHouse}</p>
              </div>
            </div>
            <Badge variant="success" className="text-[10px]">Reserved</Badge>
          </div>
        )}

        {/* Parking Element */}
        {proposal.parking && (
          <div className="flex items-center justify-between rounded-lg border border-border-color bg-surface-alt p-3">
            <div className="flex items-center gap-2.5">
              <Car className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
              <div>
                <span className="font-semibold text-text-heading">Visitor Security Gate Pass</span>
                <p className="text-[11px] text-text-body/70">{proposal.parking}</p>
              </div>
            </div>
            <Badge variant="success" className="text-[10px]">Pass Allocated</Badge>
          </div>
        )}
      </div>

      <div className="mt-4 flex items-center gap-2 text-xs text-text-body/70">
        <ShieldCheck className="h-4 w-4 text-brand-azure-strong shrink-0" aria-hidden="true" />
        <span>Submitting routes this 3-resource package directly to the campus admin approval desk.</span>
      </div>

      <div className="mt-5 border-t border-border-color pt-4">
        {submitted ? (
          <div className="flex items-center justify-center gap-2 rounded-md bg-amber-50 py-2.5 text-xs font-semibold text-amber-900 border border-amber-200">
            <CheckCircle2 className="h-4 w-4 text-amber-700" aria-hidden="true" />
            <span>Successfully Routed to Admin Queue (Ref #JKLU-BK-PROPOSAL)</span>
          </div>
        ) : (
          <Button
            type="button"
            onClick={handleSubmit}
            variant="orange"
            className="w-full font-bold text-text-heading shadow-sm"
            disabled={isSubmitting}
          >
            <Send className="mr-2 h-4 w-4" aria-hidden="true" />
            {isSubmitting ? 'Routing to Admin Queue...' : 'Submit for Admin Approval'}
          </Button>
        )}
      </div>
    </div>
  );
}

