import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AgentWorkAccordion } from './AgentWorkAccordion';
import { AgentPlanStep, AgentToolCall } from '@/api/types';

const mockPlan: AgentPlanStep[] = [
  { id: 1, title: 'Analyze requirements', status: 'done' },
  { id: 2, title: 'Cross-check timetables', status: 'done' },
];

const mockToolCalls: AgentToolCall[] = [
  {
    id: 'tc-1',
    toolName: 'queryTimetableDatabase',
    args: { date: '2026-11-19', time: '15:00-17:00' },
    resultSummary: 'Success, 1 clash detected',
    durationMs: 310,
    status: 'success',
  },
  {
    id: 'tc-2',
    toolName: 'reserveGuestHouseRoom',
    args: { roomType: 'DELUXE' },
    resultSummary: 'MDC-101 allocated',
    durationMs: 95,
    status: 'success',
  },
];

describe('AgentWorkAccordion', () => {
  it('shows plan heading and tool call row when streaming (expanded)', () => {
    render(
      <AgentWorkAccordion
        plan={mockPlan}
        toolCalls={mockToolCalls}
        isStreaming={true}
      />,
    );
    // PlanCard renders "Execution Plan & Sub-tasks" header
    expect(screen.getByText(/Execution Plan/i)).toBeInTheDocument();
    // ToolCallRow renders tool name
    expect(screen.getByText('queryTimetableDatabase')).toBeInTheDocument();
  });

  it('shows "Working…" label while streaming', () => {
    render(
      <AgentWorkAccordion
        plan={mockPlan}
        toolCalls={mockToolCalls}
        isStreaming={true}
      />,
    );
    expect(screen.getByText(/Working/i)).toBeInTheDocument();
  });

  it('shows tool count label when not streaming (expanded)', () => {
    render(
      <AgentWorkAccordion
        plan={mockPlan}
        toolCalls={mockToolCalls}
        isStreaming={false}
      />,
    );
    // "Used 2 tools" in the header
    expect(screen.getByText(/Used 2 tools/i)).toBeInTheDocument();
  });

  it('calls onViewTrace when Full trace button clicked', async () => {
    const user = userEvent.setup();
    const onViewTrace = vi.fn();
    render(
      <AgentWorkAccordion
        plan={mockPlan}
        toolCalls={mockToolCalls}
        isStreaming={false}
        onViewTrace={onViewTrace}
      />,
    );
    // Full trace button in the expanded header
    const traceBtn = screen.getByRole('button', { name: /View full reasoning trace/i });
    await user.click(traceBtn);
    expect(onViewTrace).toHaveBeenCalledOnce();
  });

  it('renders nothing when no plan or toolCalls provided', () => {
    const { container } = render(
      <AgentWorkAccordion isStreaming={false} />,
    );
    expect(container.firstChild).toBeNull();
  });

  it('collapses and shows summary row when collapse button clicked', async () => {
    const user = userEvent.setup();
    render(
      <AgentWorkAccordion
        plan={mockPlan}
        toolCalls={mockToolCalls}
        isStreaming={false}
      />,
    );
    // Click collapse
    const collapseBtn = screen.getByRole('button', { name: /Collapse work details/i });
    await user.click(collapseBtn);
    // After collapse, the PlanCard heading should be gone and we see the summary
    expect(screen.queryByText(/Execution Plan/i)).not.toBeInTheDocument();
    expect(screen.getByText(/Used 2 tools/i)).toBeInTheDocument();
  });
});
