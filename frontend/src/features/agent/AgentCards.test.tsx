import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { PlanCard } from './PlanCard';
import { ToolCallRow } from './ToolCallRow';
import { AgentPlanStep, AgentToolCall } from '@/api/types';

describe('Agent PlanCard and ToolCallRow Components', () => {
  it('renders plan steps with correct status indicators', () => {
    const steps: AgentPlanStep[] = [
      { id: 1, title: 'Analyze capacity requirements', status: 'done' },
      { id: 2, title: 'Cross-check timetables', status: 'running' },
      { id: 3, title: 'Allocate parking bay', status: 'pending' },
    ];

    render(<PlanCard steps={steps} />);

    expect(screen.getByText(/execution plan & sub-tasks/i)).toBeInTheDocument();
    expect(screen.getByText(/1\. Analyze capacity requirements/i)).toBeInTheDocument();
    expect(screen.getByText(/2\. Cross-check timetables/i)).toBeInTheDocument();
    expect(screen.getByText(/3\. Allocate parking bay/i)).toBeInTheDocument();
    expect(screen.getByText(/in progress\.\.\./i)).toBeInTheDocument();
  });

  it('renders tool call and expands details on click', async () => {
    const user = userEvent.setup();
    const tool: AgentToolCall = {
      id: 'tc-test-1',
      toolName: 'queryTimetableDatabase',
      args: { date: '2026-11-19', minCapacity: 80 },
      resultSummary: 'Scanned 25 rooms. Found 1 conflict.',
      durationMs: 145,
      status: 'success',
    };

    render(<ToolCallRow tool={tool} />);

    expect(screen.getByText('queryTimetableDatabase')).toBeInTheDocument();
    expect(screen.getByText('145ms')).toBeInTheDocument();
    expect(screen.getByText(/success/i)).toBeInTheDocument();

    // Details initially collapsed
    expect(screen.queryByText(/tool arguments:/i)).toBeNull();

    // Click to expand
    await user.click(screen.getByText('queryTimetableDatabase'));
    expect(screen.getByText(/tool arguments:/i)).toBeInTheDocument();
    expect(screen.getByText(/scanned 25 rooms\. found 1 conflict\./i)).toBeInTheDocument();
  });
});
