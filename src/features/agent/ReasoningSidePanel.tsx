import { Bot, Terminal, Cpu, Clock, X } from 'lucide-react';
import { AgentToolCall } from '@/api/types';

interface ReasoningSidePanelProps {
  isOpen: boolean;
  onClose: () => void;
  thoughtChain: string[];
  toolCalls: AgentToolCall[];
}

export function ReasoningSidePanel({
  isOpen,
  onClose,
  thoughtChain,
  toolCalls,
}: ReasoningSidePanelProps) {
  if (!isOpen) return null;

  return (
    <aside
      className="fixed inset-y-0 right-0 z-40 w-80 sm:w-96 border-l border-border-color bg-surface shadow-2xl flex flex-col"
      aria-label="Agent Reasoning and Tool Trace"
    >
      <div className="flex items-center justify-between border-b border-border-color px-4 py-3 bg-surface-alt">
        <div className="flex items-center gap-2 text-brand-azure-strong">
          <Cpu className="h-5 w-5" aria-hidden="true" />
          <h2 className="font-heading font-bold text-sm text-text-heading">
            Reasoning & Tool Trace
          </h2>
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Close reasoning trace"
          className="rounded p-1 text-text-body hover:bg-border-color/30"
        >
          <X className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-6 text-xs">
        {/* Thought Chain */}
        <div>
          <span className="font-bold uppercase tracking-wider text-text-body/60 flex items-center gap-1.5">
            <Bot className="h-3.5 w-3.5 text-brand-azure-strong" aria-hidden="true" />
            Active Thought Chain:
          </span>
          <div className="mt-2 space-y-2">
            {thoughtChain.map((thought, i) => (
              <div
                key={i}
                className="rounded-md border border-brand-azure-strong/20 bg-brand-azure-strong/5 p-2.5 font-mono text-[11px] leading-relaxed text-text-heading"
              >
                <span className="text-brand-azure-strong font-bold">Step {i + 1}: </span>
                {thought}
              </div>
            ))}
          </div>
        </div>

        {/* Real-time Tool Trace */}
        <div>
          <span className="font-bold uppercase tracking-wider text-text-body/60 flex items-center gap-1.5">
            <Terminal className="h-3.5 w-3.5 text-brand-azure-strong" aria-hidden="true" />
            Deterministic Tool Invocations ({toolCalls.length}):
          </span>

          <div className="mt-2 space-y-2">
            {toolCalls.length === 0 ? (
              <p className="text-text-body/60 italic">No tool calls triggered yet.</p>
            ) : (
              toolCalls.map((tc) => (
                <div key={tc.id} className="rounded-md border border-border-color bg-surface-alt p-2.5">
                  <div className="flex items-center justify-between font-mono font-semibold text-text-heading">
                    <span>{tc.toolName}</span>
                    <span className="text-[10px] text-text-body/60 flex items-center gap-1">
                      <Clock className="h-3 w-3" aria-hidden="true" />
                      {tc.durationMs}ms
                    </span>
                  </div>

                  <p className="mt-1 font-sans text-[11px] text-text-body/80">{tc.resultSummary}</p>

                  <div className="mt-2 flex items-center justify-between border-t border-border-color/60 pt-1.5 text-[10px]">
                    <span className="text-text-body/60">Status: {tc.status}</span>
                    {tc.retryCount && tc.retryCount > 0 && (
                      <span className="text-amber-700 font-semibold">
                        Auto-retried {tc.retryCount}x
                      </span>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </aside>
  );
}

