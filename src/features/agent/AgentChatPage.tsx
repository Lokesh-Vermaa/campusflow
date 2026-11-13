import { useState, useRef, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Send, Square, RotateCcw, AlertCircle, ChevronDown, Copy, Check, Bot } from 'lucide-react';
import { streamAgentChat } from '@/api/agent';
import { conversationsApi } from '@/api/conversations';
import { AgentChatMessage, AgentProposal, AgentToolCall, Conversation } from '@/api/types';
import { bookingsApi } from '@/api/bookings';
import { useAuth } from '@/features/auth/useAuth';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/Button';
import { AgentMessageRenderer } from './AgentMessageRenderer';
import { AgentWorkAccordion } from './AgentWorkAccordion';
import { AlternativesCard } from './AlternativesCard';
import { ProposalCard } from './ProposalCard';
import { ReasoningSidePanel } from './ReasoningSidePanel';
import { cn } from '@/lib/cn';

// ─── Suggestion cards ─────────────────────────────────────────────────────────

const STUDENT_SUGGESTIONS = [
  { label: 'Find a free classroom for 60 students', prompt: 'Find a free classroom for 60 students this Thursday 3–4 PM' },
  { label: 'Book amphitheatre for a club event', prompt: 'I want to book the amphitheatre for a club cultural event next Friday evening' },
  { label: 'Show my pending bookings', prompt: 'Show all my current pending booking requests' },
  { label: 'Check seminar hall availability', prompt: 'Is the seminar hall available this Saturday 10 AM to 1 PM?' },
];

const STAFF_SUGGESTIONS = [
  { label: 'Plan a guest lecture: room + guest house', prompt: 'Plan a guest lecture for next Thursday 3–5 PM, 80 people. Speaker from Delhi needs a guest house room for one night.' },
  { label: 'Book the amphitheatre for Friday', prompt: 'Book the amphitheatre for a departmental event next Friday 4–6 PM' },
  { label: 'Find a free lab for 30 students', prompt: 'Find a free computer lab for 30 students this Wednesday 2–4 PM' },
  { label: 'Arrange visitor parking', prompt: 'Allocate visitor parking for a delegation arriving Monday morning' },
];

const ADMIN_SUGGESTIONS = [
  { label: 'Show pending approvals summary', prompt: 'Give me a summary of all pending booking approvals' },
  { label: 'Venue utilisation this week', prompt: 'Show venue utilisation report for this week' },
  { label: 'Find clashing bookings', prompt: 'Are there any resource conflicts in bookings this week?' },
  { label: 'Multi-resource booking plan', prompt: 'Plan a multi-resource booking: seminar hall + guest house + parking for a 2-day conference next week' },
];

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 17) return 'Good afternoon';
  return 'Good evening';
}

// ─── Composer component ───────────────────────────────────────────────────────

interface ComposerProps {
  value: string;
  onChange: (v: string) => void;
  onSend: (text: string) => void;
  onStop: () => void;
  isStreaming: boolean;
  disabled?: boolean;
  autoFocusOnMount?: boolean;
}

const MAX_CHARS = 2000;

function Composer({ value, onChange, onSend, onStop, isStreaming, disabled, autoFocusOnMount }: ComposerProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (autoFocusOnMount && textareaRef.current) {
      textareaRef.current.focus();
    }
  }, [autoFocusOnMount]);

  // Auto-grow textarea
  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    const lineH = 24;
    const maxH = lineH * 8;
    el.style.height = `${Math.min(el.scrollHeight, maxH)}px`;
    el.style.overflowY = el.scrollHeight > maxH ? 'auto' : 'hidden';
  }, [value]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (!isStreaming && value.trim()) {
        onSend(value.trim());
      }
    }
  };

  const remaining = MAX_CHARS - value.length;
  const nearLimit = remaining <= 200;

  return (
    <div
      className={cn(
        'relative rounded-2xl border bg-surface shadow-sm transition focus-within:ring-2 focus-within:ring-brand-azure-strong/40',
        'border-border-color',
      )}
    >
      <textarea
        ref={textareaRef}
        rows={1}
        value={value}
        onChange={(e) => {
          if (e.target.value.length <= MAX_CHARS) onChange(e.target.value);
        }}
        onKeyDown={handleKeyDown}
        placeholder="Ask CampusFlow to find, book or plan..."
        disabled={disabled || isStreaming}
        aria-label="Message CampusFlow"
        className={cn(
          'block w-full resize-none rounded-2xl bg-transparent px-4 pb-10 pt-3.5 text-sm text-text-body',
          'placeholder:text-text-body/40 focus:outline-none disabled:opacity-50',
          'min-h-[52px]',
        )}
        style={{ overflowY: 'hidden' }}
      />

      {/* Bottom row: char counter + send/stop */}
      <div className="absolute bottom-2.5 left-4 right-3 flex items-center justify-between">
        {nearLimit ? (
          <span className={cn('text-[11px] tabular-nums', remaining <= 50 ? 'text-red-500' : 'text-text-body/50')}>
            {remaining} left
          </span>
        ) : (
          <span />
        )}

        {isStreaming ? (
          <button
            type="button"
            onClick={onStop}
            aria-label="Stop streaming"
            className="flex h-8 w-8 items-center justify-center rounded-lg bg-red-500 text-white transition hover:bg-red-600 focus-visible:outline focus-visible:outline-2 focus-visible:outline-red-500"
          >
            <Square className="h-3.5 w-3.5 fill-current" />
          </button>
        ) : (
          <button
            type="button"
            onClick={() => { if (value.trim()) onSend(value.trim()); }}
            disabled={!value.trim() || disabled}
            aria-label="Send message"
            className={cn(
              'flex h-8 w-8 items-center justify-center rounded-lg transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong',
              value.trim() && !disabled
                ? 'bg-brand-azure-strong text-white hover:bg-brand-azure-strong/90'
                : 'bg-brand-azure-strong/20 text-brand-azure-strong/40 cursor-not-allowed',
            )}
          >
            <Send className="h-3.5 w-3.5" />
          </button>
        )}
      </div>
    </div>
  );
}

// ─── Message list item ────────────────────────────────────────────────────────

interface MessageItemProps {
  msg: AgentChatMessage;
  isLast: boolean;
  isStreaming: boolean;
  onRetry: () => void;
  onSelectAlternative: (name: string) => void;
  onSubmitProposal: (p: AgentProposal) => Promise<void>;
  onViewTrace: () => void;
}

function MessageItem({ msg, isLast, isStreaming, onRetry, onSelectAlternative, onSubmitProposal, onViewTrace }: MessageItemProps) {
  const isUser = msg.sender === 'user';
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    if (!msg.text) return;
    await navigator.clipboard.writeText(msg.text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (isUser) {
    return (
      <div className="flex justify-end">
        <div className="max-w-[75%] rounded-2xl rounded-tr-sm bg-surface-alt px-4 py-2.5 text-sm text-text-body shadow-xs border border-border-color">
          <p className="whitespace-pre-wrap break-words">{msg.text}</p>
        </div>
      </div>
    );
  }

  // Assistant message — no bubble, left-aligned with small bot mark
  return (
    <div className="flex gap-3">
      {/* Bot icon */}
      <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-brand-azure-strong/10 text-brand-azure-strong">
        <Bot className="h-4 w-4" />
      </div>

      <div className="min-w-0 flex-1 space-y-3">
        {/* Markdown text */}
        {msg.text && (
          <div className="text-sm text-text-body leading-relaxed">
            <AgentMessageRenderer content={msg.text} />
            {/* Streaming pulse */}
            {isLast && isStreaming && (
              <span className="inline-block ml-0.5 h-4 w-0.5 animate-pulse bg-text-body align-middle" aria-hidden="true" />
            )}
          </div>
        )}

        {/* Inline work accordion (plan + tool calls) */}
        {(msg.plan || msg.toolCalls) && (
          <AgentWorkAccordion
            plan={msg.plan}
            toolCalls={msg.toolCalls}
            isStreaming={isLast && isStreaming}
            onViewTrace={onViewTrace}
          />
        )}

        {/* Conflict / Alternatives */}
        {msg.conflict && (
          <AlternativesCard conflict={msg.conflict} onSelectAlternative={onSelectAlternative} />
        )}

        {/* Proposal */}
        {msg.proposal && (
          <ProposalCard proposal={msg.proposal} onSubmitForApproval={onSubmitProposal} />
        )}

        {/* Copy + Retry (only on finished assistant messages) */}
        {!isStreaming && (
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={handleCopy}
              aria-label="Copy response"
              className="flex items-center gap-1 rounded px-1.5 py-0.5 text-[11px] text-text-body/50 hover:bg-surface-alt hover:text-text-body transition"
            >
              {copied ? <Check className="h-3 w-3 text-green-600" /> : <Copy className="h-3 w-3" />}
              {copied ? 'Copied' : 'Copy'}
            </button>
            {isLast && (
              <button
                type="button"
                onClick={onRetry}
                aria-label="Retry response"
                className="flex items-center gap-1 rounded px-1.5 py-0.5 text-[11px] text-text-body/50 hover:bg-surface-alt hover:text-text-body transition"
              >
                <RotateCcw className="h-3 w-3" />
                Retry
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Main ChatPage ────────────────────────────────────────────────────────────

export function AgentChatPage() {
  const { conversationId } = useParams<{ conversationId?: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const [inputValue, setInputValue] = useState('');
  const [messages, setMessages] = useState<AgentChatMessage[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamError, setStreamError] = useState<string | null>(null);
  const [allToolCalls, setAllToolCalls] = useState<AgentToolCall[]>([]);
  const [isReasoningPanelOpen, setIsReasoningPanelOpen] = useState(false);
  const [activeConvId, setActiveConvId] = useState<string | undefined>(conversationId);
  const [showScrollBtn, setShowScrollBtn] = useState(false);

  const abortRef = useRef<AbortController | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  // Sync route param → local state
  useEffect(() => {
    setActiveConvId(conversationId);
    if (!conversationId) {
      setMessages([]);
      setStreamError(null);
    }
  }, [conversationId]);

  // Load conversation from sidebar if navigated to /c/:id
  useQuery({
    queryKey: ['conversation', conversationId],
    queryFn: () => conversationsApi.get(conversationId!),
    enabled: Boolean(conversationId),
    retry: false,
  });

  // Scroll to bottom when messages change
  useEffect(() => {
    if (!showScrollBtn) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, showScrollBtn]);

  // Scroll-to-bottom visibility
  const handleScroll = useCallback(() => {
    const el = scrollContainerRef.current;
    if (!el) return;
    const distFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    setShowScrollBtn(distFromBottom > 120);
  }, []);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    setShowScrollBtn(false);
  };

  // Create conversation mutation
  const createConv = useMutation({
    mutationFn: () => conversationsApi.create(),
    onSuccess: (conv: Conversation) => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      setActiveConvId(conv.id);
      navigate(`/c/${conv.id}`, { replace: true });
    },
  });

  const handleSend = async (text: string) => {
    if (!text.trim() || isStreaming) return;

    setStreamError(null);
    setInputValue('');

    // Create conversation if none exists
    let convId = activeConvId;
    if (!convId) {
      try {
        const conv = await createConv.mutateAsync();
        convId = conv.id;
        // Update conv title from first message
        await conversationsApi.rename(conv.id, text.slice(0, 60));
        queryClient.invalidateQueries({ queryKey: ['conversations'] });
      } catch {
        // proceed without persisting conversation
      }
    }

    const userMsg: AgentChatMessage = {
      id: crypto.randomUUID(),
      sender: 'user',
      timestamp: new Date().toISOString(),
      text,
    };
    setMessages((prev) => [...prev, userMsg]);
    setIsStreaming(true);

    const abortCtrl = new AbortController();
    abortRef.current = abortCtrl;

    streamAgentChat(
      text,
      {
        onChunk: (assistantMsg) => {
          setMessages((prev) => {
            const idx = prev.findIndex((m) => m.id === assistantMsg.id);
            if (idx !== -1) {
              const updated = [...prev];
              updated[idx] = assistantMsg;
              return updated;
            }
            return [...prev, assistantMsg];
          });
          if (assistantMsg.toolCalls) setAllToolCalls(assistantMsg.toolCalls);
        },
        onDone: () => setIsStreaming(false),
        onError: (err) => {
          setIsStreaming(false);
          setStreamError(err.message);
        },
      },
      abortCtrl.signal,
    );
  };

  const handleStop = () => {
    abortRef.current?.abort();
    setIsStreaming(false);
  };

  const handleRetry = () => {
    const lastUser = [...messages].reverse().find((m) => m.sender === 'user');
    if (lastUser?.text) {
      setMessages((prev) => prev.slice(0, -1)); // remove last assistant message
      handleSend(lastUser.text);
    }
  };

  const handleSelectAlternative = (name: string) => handleSend(`Switch the venue to ${name} and proceed.`);

  const handleSubmitProposal = async (proposal: AgentProposal) => {
    await bookingsApi.create({
      title: proposal.title,
      description: 'Agent-coordinated multi-resource allocation',
      resourceType: 'MULTI_RESOURCE',
      resourceId: proposal.roomId,
      bookingDate: proposal.date,
      startTime: proposal.time.split(' - ')[0],
      endTime: proposal.time.split(' - ')[1],
      expectedAttendees: proposal.capacity,
    });
  };

  const firstName = user?.name?.split(' ')[0] ?? 'there';
  const suggestions =
    user?.role === 'ADMIN' ? ADMIN_SUGGESTIONS : user?.role === 'STAFF' ? STAFF_SUGGESTIONS : STUDENT_SUGGESTIONS;

  const hasMessages = messages.length > 0;

  return (
    <div className="flex flex-col h-full relative">
      {/* ── Empty state: greeting + suggestion cards ── */}
      {!hasMessages && (
        <div className="flex flex-1 flex-col items-center justify-center px-4 py-16 text-center">
          <h1 className="font-heading text-3xl font-bold text-text-heading mb-1 tracking-tight">
            {getGreeting()}, {firstName}
          </h1>
          <p className="text-text-body/60 text-base mb-10">What would you like to arrange on campus today?</p>

          {/* Composer centred */}
          <div className="w-full max-w-2xl mb-8">
            <Composer
              value={inputValue}
              onChange={setInputValue}
              onSend={handleSend}
              onStop={handleStop}
              isStreaming={isStreaming}
              autoFocusOnMount
            />
            <p className="mt-2 text-center text-[11px] text-text-body/40">
              CampusFlow can make mistakes. All bookings need admin approval.
            </p>
          </div>

          {/* Suggestion cards 2×2 */}
          <div
            className="w-full max-w-2xl grid grid-cols-1 sm:grid-cols-2 gap-3"
            aria-label="Suggestion prompts"
          >
            {suggestions.map((s) => (
              <button
                key={s.prompt}
                type="button"
                onClick={() => handleSend(s.prompt)}
                disabled={isStreaming}
                className={cn(
                  'rounded-xl border border-border-color bg-surface px-4 py-3 text-left text-sm text-text-body',
                  'hover:border-brand-azure-strong/40 hover:bg-brand-azure-strong/5 hover:shadow-sm',
                  'focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong',
                  'transition disabled:opacity-40 disabled:cursor-not-allowed',
                )}
              >
                {s.label}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* ── Active conversation: message thread ── */}
      {hasMessages && (
        <>
          {/* Scrollable messages area */}
          <div
            ref={scrollContainerRef}
            onScroll={handleScroll}
            className="flex-1 overflow-y-auto px-4 py-6"
          >
            <div className="mx-auto max-w-2xl space-y-6" aria-live="polite" aria-label="Conversation">
              {messages.map((msg, i) => (
                <MessageItem
                  key={msg.id}
                  msg={msg}
                  isLast={i === messages.length - 1}
                  isStreaming={isStreaming && i === messages.length - 1}
                  onRetry={handleRetry}
                  onSelectAlternative={handleSelectAlternative}
                  onSubmitProposal={handleSubmitProposal}
                  onViewTrace={() => setIsReasoningPanelOpen(true)}
                />
              ))}

              {/* Stream error */}
              {streamError && (
                <div
                  role="alert"
                  className="flex items-center justify-between rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
                >
                  <span className="flex items-center gap-2">
                    <AlertCircle className="h-4 w-4 shrink-0 text-red-500" aria-hidden="true" />
                    {streamError}
                  </span>
                  <Button type="button" variant="outline" size="sm" onClick={handleRetry}>
                    <RotateCcw className="mr-1.5 h-3.5 w-3.5" />
                    Retry
                  </Button>
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>
          </div>

          {/* Scroll-to-bottom button */}
          {showScrollBtn && (
            <button
              type="button"
              onClick={scrollToBottom}
              aria-label="Scroll to latest message"
              className="absolute bottom-28 left-1/2 -translate-x-1/2 flex h-8 w-8 items-center justify-center rounded-full bg-surface border border-border-color shadow-md text-text-body hover:bg-surface-alt transition z-10"
            >
              <ChevronDown className="h-4 w-4" />
            </button>
          )}

          {/* Pinned composer */}
          <div className="sticky bottom-0 bg-surface border-t border-border-color px-4 py-3">
            <div className="mx-auto max-w-2xl space-y-2">
              <Composer
                value={inputValue}
                onChange={setInputValue}
                onSend={handleSend}
                onStop={handleStop}
                isStreaming={isStreaming}
              />
              <div className="flex items-center justify-between">
                <p className="text-[11px] text-text-body/40">
                  CampusFlow can make mistakes. All bookings need admin approval.
                </p>
                {allToolCalls.length > 0 && (
                  <button
                    type="button"
                    onClick={() => setIsReasoningPanelOpen(true)}
                    className="text-[11px] text-brand-azure-strong hover:underline"
                  >
                    View trace ({allToolCalls.length} tool{allToolCalls.length !== 1 ? 's' : ''})
                  </button>
                )}
              </div>
            </div>
          </div>
        </>
      )}

      {/* Reasoning trace side panel */}
      <ReasoningSidePanel
        isOpen={isReasoningPanelOpen}
        onClose={() => setIsReasoningPanelOpen(false)}
        thoughtChain={[]}
        toolCalls={allToolCalls}
      />
    </div>
  );
}
