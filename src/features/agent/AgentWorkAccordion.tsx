/**
 * AgentWorkAccordion
 *
 * Wraps the plan steps + tool call rows inside an assistant turn.
 * While streaming: shows the plan accordion expanded.
 * When done: collapses to "Used N tools – X ms" summary with a "View full trace" button.
 */
import { useState, useEffect } from 'react';
import { ChevronDown, ChevronRight, Wrench, Eye } from 'lucide-react';
import { AgentPlanStep, AgentToolCall } from '@/api/types';
import { PlanCard } from './PlanCard';
import { ToolCallRow } from './ToolCallRow';
import { cn } from '@/lib/cn';

interface AgentWorkAccordionProps {
  plan?: AgentPlanStep[];
  toolCalls?: AgentToolCall[];
  isStreaming: boolean;
  onViewTrace?: () => void;
}

export function AgentWorkAccordion({ plan, toolCalls, isStreaming, onViewTrace }: AgentWorkAccordionProps) {
  const [expanded, setExpanded] = useState(true);

  // Auto-collapse when streaming ends and all plan steps are done
  useEffect(() => {
    if (!isStreaming && plan && plan.every((s) => s.status === 'done' || s.status === 'failed')) {
      const timer = setTimeout(() => setExpanded(false), 800);
      return () => clearTimeout(timer);
    }
  }, [isStreaming, plan]);

  const totalTools = toolCalls?.length ?? 0;
  const totalMs = toolCalls?.reduce((sum, t) => sum + t.durationMs, 0) ?? 0;
  const hasContent = (plan && plan.length > 0) || (toolCalls && toolCalls.length > 0);

  if (!hasContent) return null;

  // ── Collapsed summary view (after completion) ──
  if (!expanded && !isStreaming) {
    return (
      <div className="flex items-center gap-2 rounded-lg border border-border-color bg-surface-alt/60 px-3 py-2 text-xs text-text-body/60">
        <Wrench className="h-3.5 w-3.5 shrink-0 text-brand-azure-strong/60" aria-hidden="true" />
        <span>
          Used {totalTools} tool{totalTools !== 1 ? 's' : ''}
          {totalMs > 0 && ` · ${(totalMs / 1000).toFixed(1)}s`}
        </span>
        <div className="ml-auto flex items-center gap-2">
          <button
            type="button"
            onClick={() => setExpanded(true)}
            aria-label="Expand work details"
            className="flex items-center gap-1 text-text-body/50 hover:text-text-body transition"
          >
            <ChevronRight className="h-3.5 w-3.5" />
          </button>
          {onViewTrace && (
            <button
              type="button"
              onClick={onViewTrace}
              aria-label="View full reasoning trace"
              className="flex items-center gap-1 text-brand-azure-strong hover:underline"
            >
              <Eye className="h-3 w-3" />
              View trace
            </button>
          )}
        </div>
      </div>
    );
  }

  // ── Expanded accordion ──
  return (
    <div className={cn('rounded-lg border border-border-color bg-surface-alt/40 overflow-hidden')}>
      {/* Header: toggle + optional trace link side-by-side (no nested buttons) */}
      <div className="flex w-full items-center px-3 py-2 text-xs text-text-body/60">
        <button
          type="button"
          onClick={() => setExpanded((p) => !p)}
          aria-expanded={expanded}
          aria-label={expanded ? 'Collapse work details' : 'Expand work details'}
          className="flex flex-1 items-center gap-2 text-left hover:text-text-body/80 transition bg-transparent border-0"
        >
          {expanded ? (
            <ChevronDown className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          ) : (
            <ChevronRight className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          )}
          <Wrench className="h-3.5 w-3.5 shrink-0 text-brand-azure-strong/60" aria-hidden="true" />
          <span className="font-medium text-text-body/70">
            {isStreaming ? 'Working…' : `Used ${totalTools} tool${totalTools !== 1 ? 's' : ''}`}
            {!isStreaming && totalMs > 0 && ` · ${(totalMs / 1000).toFixed(1)}s`}
          </span>
        </button>

        {onViewTrace && !isStreaming && (
          <button
            type="button"
            onClick={onViewTrace}
            aria-label="View full reasoning trace"
            className="ml-2 flex shrink-0 items-center gap-1 text-[11px] text-brand-azure-strong hover:underline"
          >
            <Eye className="h-3 w-3" />
            Full trace
          </button>
        )}
      </div>

      {/* Body */}
      {expanded && (
        <div className="border-t border-border-color px-3 pb-3 pt-2 space-y-2">
          {plan && plan.length > 0 && <PlanCard steps={plan} />}
          {toolCalls && toolCalls.length > 0 && (
            <div className="space-y-1">
              <span className="text-[10px] font-mono uppercase tracking-wider text-text-body/40">Tool executions</span>
              {toolCalls.map((tc) => (
                <ToolCallRow key={tc.id} tool={tc} />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
