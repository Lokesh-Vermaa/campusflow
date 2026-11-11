import { useState, useRef, useEffect } from 'react';
import {
  Bot,
  User,
  Send,
  Sparkles,
  RotateCcw,
  Cpu,
  AlertCircle,
} from 'lucide-react';
import { streamAgentChat } from '@/api/agent';
import { AgentChatMessage, AgentProposal, AgentToolCall } from '@/api/types';
import { bookingsApi } from '@/api/bookings';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { AgentMessageRenderer } from './AgentMessageRenderer';
import { PlanCard } from './PlanCard';
import { ToolCallRow } from './ToolCallRow';
import { AlternativesCard } from './AlternativesCard';
import { ProposalCard } from './ProposalCard';
import { ReasoningSidePanel } from './ReasoningSidePanel';

const PROMPT_SUGGESTIONS = [
  'Guest lecture next Thursday 3-5 PM, 80 people, speaker from Delhi',
  'Need free seminar hall for 120 students this Friday 2-4 PM',
  'Hackathon venue with guest house for 2 judges next weekend',
];

export function AgentChatPage() {
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const [inputPrompt, setInputPrompt] = useState('');
  const [messages, setMessages] = useState<AgentChatMessage[]>([
    {
      id: 'welcome-msg',
      sender: 'assistant',
      timestamp: new Date().toISOString(),
      text: "Hello! I am **CampusFlow Agent**, your campus scheduling intelligence. Tell me about your event — room requirements, guest house stays, or parking — and I will cross-reference timetables to prepare a clash-free proposal.",
    },
  ]);

  const [isStreaming, setIsStreaming] = useState(false);
  const [streamError, setStreamError] = useState<string | null>(null);
  const [isReasoningPanelOpen, setIsReasoningPanelOpen] = useState(false);
  const [activeThoughtChain, setActiveThoughtChain] = useState<string[]>([]);
  const [allToolCalls, setAllToolCalls] = useState<AgentToolCall[]>([]);

  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendPrompt = (promptText: string) => {
    if (!promptText.trim() || isStreaming) return;

    setStreamError(null);
    const userMessage: AgentChatMessage = {
      id: crypto.randomUUID(),
      sender: 'user',
      timestamp: new Date().toISOString(),
      text: promptText,
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputPrompt('');
    setIsStreaming(true);

    setActiveThoughtChain([
      `Received user query: "${promptText}". Extracting time intervals and resource constraints.`,
      'Querying backend timetable database to determine semester class conflicts.',
      'Checking MDC Guest House booking availability calendar.',
    ]);

    const abortCtrl = new AbortController();
    abortControllerRef.current = abortCtrl;

    streamAgentChat(
      promptText,
      {
        onChunk: (assistantMsg) => {
          setMessages((prev) => {
            const existingIdx = prev.findIndex((m) => m.id === assistantMsg.id);
            if (existingIdx !== -1) {
              const updated = [...prev];
              updated[existingIdx] = assistantMsg;
              return updated;
            }
            return [...prev, assistantMsg];
          });

          if (assistantMsg.toolCalls) {
            setAllToolCalls(assistantMsg.toolCalls);
          }
        },
        onDone: () => {
          setIsStreaming(false);
        },
        onError: (err) => {
          setIsStreaming(false);
          setStreamError(err.message);
        },
      },
      abortCtrl.signal,
    );
  };

  const handleRetry = () => {
    const lastUserMsg = [...messages].reverse().find((m) => m.sender === 'user');
    if (lastUserMsg?.text) {
      handleSendPrompt(lastUserMsg.text);
    }
  };

  const handleSubmitProposal = async (proposal: AgentProposal) => {
    // Call mock API booking creation
    await bookingsApi.create({
      title: proposal.title,
      description: 'Agent Coordinated Multi-Resource Allocation',
      resourceType: 'MULTI_RESOURCE',
      resourceId: proposal.roomId,
      bookingDate: proposal.date,
      startTime: proposal.time.split(' - ')[0],
      endTime: proposal.time.split(' - ')[1],
      expectedAttendees: proposal.capacity,
    });
  };

  const handleSelectAlternative = (roomName: string) => {
    handleSendPrompt(`Switch the venue to ${roomName} and proceed with the proposal.`);
  };

  return (
    <div className="relative min-h-[calc(100vh-140px)] bg-surface-alt flex flex-col">
      {/* Top Banner with Reasoning Side Panel Toggle */}
      <div className="border-b border-border-color bg-surface px-4 py-3 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-5xl flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-azure-strong text-white shadow-sm">
              <Bot className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <h1 className="font-heading text-lg font-bold text-text-heading flex items-center gap-2">
                CampusFlow AI Agent
                <span className="rounded bg-green-100 px-2 py-0.5 text-[10px] font-semibold text-green-800">
                  Online (SSE Stream Active)
                </span>
              </h1>
              <p className="text-xs text-text-body/70">
                Deterministic conflict solver & multi-resource booking planner
              </p>
            </div>
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => setIsReasoningPanelOpen((prev) => !prev)}
            className="flex items-center gap-1.5 text-xs font-semibold"
          >
            <Cpu className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
            <span>Reasoning Trace ({allToolCalls.length})</span>
          </Button>
        </div>
      </div>

      {/* Messages Thread Container */}
      <div className="flex-1 overflow-y-auto px-4 py-6 sm:px-6">
        <div className="mx-auto max-w-4xl space-y-6">
          {messages.map((msg) => {
            const isUser = msg.sender === 'user';
            return (
              <div
                key={msg.id}
                className={`flex items-start gap-3 ${isUser ? 'flex-row-reverse' : ''}`}
              >
                {/* Avatar */}
                <div
                  className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                    isUser
                      ? 'bg-brand-orange text-text-heading'
                      : 'bg-brand-azure-strong text-white'
                  }`}
                >
                  {isUser ? <User className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
                </div>

                {/* Message Bubble Content */}
                <div
                  className={`max-w-2xl rounded-2xl p-4 text-sm shadow-2xs ${
                    isUser
                      ? 'bg-brand-azure-strong text-white rounded-tr-none'
                      : 'border border-border-color bg-surface text-text-body rounded-tl-none'
                  }`}
                >
                  {/* Markdown or plain user text */}
                  {isUser ? (
                    <p className="whitespace-pre-wrap">{msg.text}</p>
                  ) : (
                    msg.text && <AgentMessageRenderer content={msg.text} />
                  )}

                  {/* Plan Card */}
                  {msg.plan && <PlanCard steps={msg.plan} />}

                  {/* Tool Call Rows */}
                  {msg.toolCalls && msg.toolCalls.length > 0 && (
                    <div className="my-2 border-t border-border-color pt-2">
                      <span className="text-[10px] font-mono uppercase tracking-wider text-text-body/60">
                        Tool Executions:
                      </span>
                      {msg.toolCalls.map((tc) => (
                        <ToolCallRow key={tc.id} tool={tc} />
                      ))}
                    </div>
                  )}

                  {/* Conflict & Alternatives Card */}
                  {msg.conflict && (
                    <AlternativesCard
                      conflict={msg.conflict}
                      onSelectAlternative={handleSelectAlternative}
                    />
                  )}

                  {/* Proposal Card */}
                  {msg.proposal && (
                    <ProposalCard
                      proposal={msg.proposal}
                      onSubmitForApproval={handleSubmitProposal}
                    />
                  )}
                </div>
              </div>
            );
          })}

          {/* Stream Interruption / Error Alert */}
          {streamError && (
            <div
              role="alert"
              className="flex items-center justify-between rounded-lg border border-red-300 bg-red-50 p-4 text-xs text-red-900"
            >
              <div className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-red-600" aria-hidden="true" />
                <span>Streaming interrupted: {streamError}</span>
              </div>
              <Button type="button" onClick={handleRetry} variant="outline" size="sm">
                <RotateCcw className="mr-1.5 h-3.5 w-3.5" aria-hidden="true" />
                Retry
              </Button>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* Suggested Prompt Chips */}
      <div className="border-t border-border-color bg-surface/80 px-4 py-2 sm:px-6">
        <div className="mx-auto max-w-4xl">
          <div className="flex items-center gap-1.5 text-xs text-text-body/70 pb-1.5">
            <Sparkles className="h-3.5 w-3.5 text-brand-orange" aria-hidden="true" />
            <span className="font-semibold">Suggested Scenarios:</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {PROMPT_SUGGESTIONS.map((chip, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => handleSendPrompt(chip)}
                disabled={isStreaming}
                className="rounded-full border border-border-color bg-surface px-3 py-1 text-xs text-text-body hover:border-brand-azure-strong hover:bg-brand-azure-strong/5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong transition text-left"
              >
                &ldquo;{chip}&rdquo;
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Prompt Input Form */}
      <div className="border-t border-border-color bg-surface p-4 sm:px-6">
        <div className="mx-auto max-w-4xl">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSendPrompt(inputPrompt);
            }}
            className="flex items-center gap-2"
          >
            <Input
              type="text"
              value={inputPrompt}
              onChange={(e) => setInputPrompt(e.target.value)}
              placeholder="e.g. Guest lecture next Thursday 3-5 PM, 80 people, speaker from Delhi needing room and parking..."
              disabled={isStreaming}
              className="flex-1 text-sm shadow-2xs"
              aria-label="Ask CampusFlow Agent"
            />
            <Button
              type="submit"
              variant="orange"
              disabled={!inputPrompt.trim() || isStreaming}
              className="font-bold text-text-heading shrink-0"
            >
              {isStreaming ? (
                'Planning...'
              ) : (
                <>
                  <Send className="mr-1.5 h-4 w-4" aria-hidden="true" />
                  Send
                </>
              )}
            </Button>
          </form>
        </div>
      </div>

      {/* Collapsible Reasoning & Tool Trace Side Panel */}
      <ReasoningSidePanel
        isOpen={isReasoningPanelOpen}
        onClose={() => setIsReasoningPanelOpen(false)}
        thoughtChain={activeThoughtChain}
        toolCalls={allToolCalls}
      />
    </div>
  );
}

