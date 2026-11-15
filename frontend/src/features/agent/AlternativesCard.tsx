import { AlertTriangle, ArrowRight, Building } from 'lucide-react';
import { AgentConflict } from '@/api/types';
import { Button } from '@/components/ui/Button';

interface AlternativesCardProps {
  conflict: AgentConflict;
  onSelectAlternative: (roomName: string, roomId?: string) => void;
}

export function AlternativesCard({ conflict, onSelectAlternative }: AlternativesCardProps) {
  return (
    <div className="my-3 rounded-lg border-2 border-amber-300 bg-amber-50/80 p-4 shadow-sm">
      <div className="flex items-start gap-2">
        <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" aria-hidden="true" />
        <div>
          <h4 className="font-heading font-bold text-sm text-amber-950">
            Resource Clash Identified: {conflict.resourceName}
          </h4>
          <p className="mt-1 text-xs text-amber-900 leading-relaxed">
            {conflict.reason}
          </p>
        </div>
      </div>

      <div className="mt-4 border-t border-amber-200/80 pt-3">
        <span className="text-xs font-bold uppercase tracking-wider text-amber-950">
          Agent-Recommended Alternatives:
        </span>

        <div className="mt-2 space-y-2">
          {conflict.alternatives.map((alt) => (
            <div
              key={alt.id}
              className="flex items-center justify-between rounded-md border border-amber-300/80 bg-white p-3 shadow-2xs"
            >
              <div>
                <div className="flex items-center gap-1.5 font-semibold text-xs text-text-heading">
                  <Building className="h-3.5 w-3.5 text-brand-azure-strong" aria-hidden="true" />
                  <span>{alt.name}</span>
                </div>
                <div className="mt-1 flex items-center gap-2 text-[11px] text-text-body/70">
                  <span>Capacity: {alt.capacity} seats</span>
                  <span>•</span>
                  <span>{alt.features.join(', ')}</span>
                </div>
              </div>

              <Button
                type="button"
                size="sm"
                variant="default"
                onClick={() => onSelectAlternative(alt.name, alt.id)}
                className="text-xs"
              >
                Choose This
                <ArrowRight className="ml-1 h-3.5 w-3.5" aria-hidden="true" />
              </Button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

