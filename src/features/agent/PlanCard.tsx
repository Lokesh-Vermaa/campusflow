import { CheckCircle2, Circle, Loader2, XCircle, ListOrdered } from 'lucide-react';
import { AgentPlanStep } from '@/api/types';

interface PlanCardProps {
  steps: AgentPlanStep[];
}

export function PlanCard({ steps }: PlanCardProps) {
  return (
    <div className="my-3 rounded-lg border border-brand-azure-strong/30 bg-brand-azure-strong/5 p-4 shadow-2xs">
      <div className="flex items-center gap-2 border-b border-brand-azure-strong/20 pb-2 text-xs font-bold uppercase tracking-wider text-brand-azure-strong">
        <ListOrdered className="h-4 w-4" aria-hidden="true" />
        <span>Execution Plan & Sub-tasks</span>
      </div>

      <div className="mt-3 space-y-2.5">
        {steps.map((step) => {
          let icon = <Circle className="h-4 w-4 text-text-body/40" aria-hidden="true" />;
          let statusText = 'Pending';
          let textColor = 'text-text-body/70';

          if (step.status === 'running') {
            icon = <Loader2 className="h-4 w-4 animate-spin text-brand-azure-strong" aria-hidden="true" />;
            statusText = 'In progress...';
            textColor = 'text-brand-azure-strong font-medium';
          } else if (step.status === 'done') {
            icon = <CheckCircle2 className="h-4 w-4 text-green-600" aria-hidden="true" />;
            statusText = 'Completed';
            textColor = 'text-text-heading';
          } else if (step.status === 'failed') {
            icon = <XCircle className="h-4 w-4 text-red-600" aria-hidden="true" />;
            statusText = 'Failed';
            textColor = 'text-red-700';
          }

          return (
            <div key={step.id} className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2.5">
                {icon}
                <span className={textColor}>
                  {step.id}. {step.title}
                </span>
              </div>
              <span className="text-[10px] font-mono text-text-body/60">{statusText}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

