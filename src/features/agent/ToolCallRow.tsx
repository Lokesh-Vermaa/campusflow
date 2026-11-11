import { useState } from 'react';
import {
  Wrench,
  CheckCircle2,
  XCircle,
  RefreshCw,
  ChevronDown,
  ChevronRight,
  Clock,
} from 'lucide-react';
import { AgentToolCall } from '@/api/types';

interface ToolCallRowProps {
  tool: AgentToolCall;
}

export function ToolCallRow({ tool }: ToolCallRowProps) {
  const [expanded, setExpanded] = useState(false);

  const isSuccess = tool.status === 'success';
  const isRetrying = tool.status === 'retrying';
  const isFailed = tool.status === 'failed';

  return (
    <div className="my-2 rounded-md border border-border-color bg-surface-alt/70 text-xs shadow-2xs">
      <button
        type="button"
        onClick={() => setExpanded((prev) => !prev)}
        className="w-full flex cursor-pointer items-center justify-between p-2.5 transition hover:bg-surface-alt text-left border-0 bg-transparent"
        aria-expanded={expanded}
      >
        <div className="flex items-center gap-2">
          {expanded ? (
            <ChevronDown className="h-3.5 w-3.5 text-text-body/60" aria-hidden="true" />
          ) : (
            <ChevronRight className="h-3.5 w-3.5 text-text-body/60" aria-hidden="true" />
          )}
          <Wrench className="h-3.5 w-3.5 text-brand-azure-strong" aria-hidden="true" />
          <span className="font-mono font-semibold text-text-heading">{tool.toolName}</span>
        </div>

        <div className="flex items-center gap-2">
          <span className="flex items-center gap-1 font-mono text-[10px] text-text-body/60">
            <Clock className="h-3 w-3" aria-hidden="true" />
            {tool.durationMs}ms
          </span>

          {isSuccess && (
            <span className="inline-flex items-center gap-1 rounded bg-green-100 px-1.5 py-0.5 text-[10px] font-semibold text-green-800">
              <CheckCircle2 className="h-3 w-3" aria-hidden="true" />
              Success
            </span>
          )}

          {isRetrying && (
            <span className="inline-flex items-center gap-1 rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-semibold text-amber-800 animate-pulse">
              <RefreshCw className="h-3 w-3 animate-spin" aria-hidden="true" />
              Retrying (Attempt {tool.retryCount || 1})...
            </span>
          )}

          {isFailed && (
            <span className="inline-flex items-center gap-1 rounded bg-red-100 px-1.5 py-0.5 text-[10px] font-semibold text-red-800">
              <XCircle className="h-3 w-3" aria-hidden="true" />
              Failed
            </span>
          )}
        </div>
      </button>

      {expanded && (
        <div className="border-t border-border-color bg-surface p-3 font-mono text-[11px] space-y-2">
          <div>
            <span className="font-sans font-semibold text-text-body/70">Tool Arguments:</span>
            <pre className="mt-1 overflow-x-auto rounded bg-surface-alt p-2 text-text-body">
              {JSON.stringify(tool.args, null, 2)}
            </pre>
          </div>
          <div>
            <span className="font-sans font-semibold text-text-body/70">Result Summary:</span>
            <p className="mt-0.5 font-sans text-text-heading">{tool.resultSummary}</p>
          </div>
        </div>
      )}
    </div>
  );
}

