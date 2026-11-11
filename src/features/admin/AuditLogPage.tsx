import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  FileText,
  Search,
  Download,
  ChevronDown,
  ChevronRight,
} from 'lucide-react';
import { auditApi, AuditSearchParams } from '@/api/audit';
import { GradientHeading } from '@/components/ui/GradientHeading';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';

const EVENT_TYPES = [
  'ALL',
  'AGENT_REASONING',
  'TOOL_CALL',
  'API_CALL',
  'APPROVAL',
  'REJECTION',
  'LOGIN',
  'DATA_CHANGE',
];

export function AuditLogPage() {
  const [actorSearch, setActorSearch] = useState('');
  const [selectedEventType, setSelectedEventType] = useState('ALL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [expandedRowId, setExpandedRowId] = useState<string | null>(null);

  const searchParams: AuditSearchParams = {
    actor: actorSearch || undefined,
    eventType: selectedEventType === 'ALL' ? undefined : selectedEventType,
    startDate: startDate || undefined,
    endDate: endDate || undefined,
    page: currentPage,
    pageSize: 15,
  };

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'audit', searchParams],
    queryFn: () => auditApi.getLogs(searchParams),
  });

  const auditEntries = data?.items || [];
  const pagination = data?.pagination;

  // Export current list to CSV
  const handleExportCsv = () => {
    if (auditEntries.length === 0) return;

    const headers = ['ID', 'Timestamp', 'Actor', 'Role', 'EventType', 'Action', 'IPAddress'];
    const rows = auditEntries.map((e) => [
      e.id,
      e.timestamp,
      `"${e.actor.replace(/"/g, '""')}"`,
      e.actorRole,
      e.eventType,
      `"${e.action.replace(/"/g, '""')}"`,
      e.ipAddress || '',
    ]);

    const csvContent =
      'data:text/csv;charset=utf-8,' +
      [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `jklu_audit_logs_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const toggleRow = (id: string) => {
    setExpandedRowId((prev) => (prev === id ? null : id));
  };

  return (
    <div className="min-h-screen bg-surface-alt py-8">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div>
            <GradientHeading as="h1" className="text-3xl font-extrabold sm:text-4xl">
              Compliance & Security Audit Log
            </GradientHeading>
            <p className="mt-2 text-sm text-text-body/80 sm:text-base">
              Immutable, auditable ledger tracking AI agent reasoning, deterministic tool calls, administrative approvals, and user logins.
            </p>
          </div>

          <Button
            type="button"
            onClick={handleExportCsv}
            variant="outline"
            size="sm"
            disabled={auditEntries.length === 0}
          >
            <Download className="mr-2 h-4 w-4" aria-hidden="true" />
            Export CSV
          </Button>
        </div>

        {/* Filter Controls */}
        <Card className="mt-6 shadow-sm">
          <CardContent className="p-5">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div>
                <label htmlFor="audit-actor" className="text-xs font-semibold text-text-body/70">
                  Search Actor Name
                </label>
                <div className="relative mt-1">
                  <Search
                    className="pointer-events-none absolute left-3 top-2.5 h-3.5 w-3.5 text-text-body/50"
                    aria-hidden="true"
                  />
                  <Input
                    id="audit-actor"
                    type="search"
                    placeholder="e.g. Aarav, Rajesh, Admin"
                    value={actorSearch}
                    onChange={(e) => {
                      setActorSearch(e.target.value);
                      setCurrentPage(1);
                    }}
                    className="pl-8 text-xs"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="audit-event" className="text-xs font-semibold text-text-body/70">
                  Filter by Event Type
                </label>
                <select
                  id="audit-event"
                  value={selectedEventType}
                  onChange={(e) => {
                    setSelectedEventType(e.target.value);
                    setCurrentPage(1);
                  }}
                  className="mt-1 flex h-9 w-full rounded-md border border-border-color bg-surface px-3 py-1 text-xs text-text-body shadow-sm focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong"
                >
                  {EVENT_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {type.replace(/_/g, ' ')}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="audit-start" className="text-xs font-semibold text-text-body/70">
                  From Date
                </label>
                <Input
                  id="audit-start"
                  type="date"
                  value={startDate}
                  onChange={(e) => {
                    setStartDate(e.target.value);
                    setCurrentPage(1);
                  }}
                  className="mt-1 text-xs"
                />
              </div>

              <div>
                <label htmlFor="audit-end" className="text-xs font-semibold text-text-body/70">
                  To Date
                </label>
                <Input
                  id="audit-end"
                  type="date"
                  value={endDate}
                  onChange={(e) => {
                    setEndDate(e.target.value);
                    setCurrentPage(1);
                  }}
                  className="mt-1 text-xs"
                />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Audit Log Table */}
        <div className="mt-6 overflow-hidden rounded-lg border border-border-color bg-surface shadow-sm">
          {isLoading ? (
            <div className="p-8 text-center text-xs text-text-body/60">Loading audit records...</div>
          ) : auditEntries.length === 0 ? (
            <div className="p-12 text-center">
              <FileText className="mx-auto h-8 w-8 text-text-body/40" aria-hidden="true" />
              <p className="mt-2 text-sm font-semibold text-text-heading">No audit logs found</p>
              <p className="text-xs text-text-body/60">Try adjusting your filters.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="border-b border-border-color bg-surface-alt font-semibold text-text-heading">
                  <tr>
                    <th className="w-8 px-3 py-3"></th>
                    <th className="px-4 py-3">Timestamp</th>
                    <th className="px-4 py-3">Actor & Role</th>
                    <th className="px-4 py-3">Event Type</th>
                    <th className="px-4 py-3">Action Recorded</th>
                    <th className="px-4 py-3">IP Address</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border-color">
                  {auditEntries.map((entry) => {
                    const isExpanded = expandedRowId === entry.id;
                    return (
                      <React.Fragment key={entry.id}>
                        <tr
                          onClick={() => toggleRow(entry.id)}
                          className="cursor-pointer hover:bg-surface-alt/60 transition-colors"
                        >
                          <td className="px-3 py-3 text-text-body/50">
                            {isExpanded ? (
                              <ChevronDown className="h-4 w-4" aria-hidden="true" />
                            ) : (
                              <ChevronRight className="h-4 w-4" aria-hidden="true" />
                            )}
                          </td>
                          <td className="whitespace-nowrap px-4 py-3 font-mono text-[11px] text-text-body/80">
                            {new Date(entry.timestamp).toLocaleString()}
                          </td>
                          <td className="px-4 py-3">
                            <span className="font-semibold text-text-heading">{entry.actor}</span>
                            <span className="ml-1.5 rounded bg-surface-alt px-1.5 py-0.2 text-[10px] text-text-body/70 border border-border-color">
                              {entry.actorRole}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            <Badge
                              variant={
                                entry.eventType === 'APPROVAL'
                                  ? 'success'
                                  : entry.eventType === 'REJECTION'
                                    ? 'destructive'
                                    : entry.eventType === 'AGENT_REASONING'
                                      ? 'default'
                                      : entry.eventType === 'TOOL_CALL'
                                        ? 'warning'
                                        : 'secondary'
                              }
                              className="text-[10px]"
                            >
                              {entry.eventType}
                            </Badge>
                          </td>
                          <td className="px-4 py-3 font-medium text-text-heading max-w-md truncate">
                            {entry.action}
                          </td>
                          <td className="px-4 py-3 font-mono text-[11px] text-text-body/60">
                            {entry.ipAddress || '127.0.0.1'}
                          </td>
                        </tr>

                        {/* Expandable JSON Details Row Requirement */}
                        {isExpanded && (
                          <tr className="bg-surface-alt/80">
                            <td colSpan={6} className="px-6 py-4">
                              <div className="rounded border border-border-color bg-surface p-4">
                                <div className="flex items-center justify-between border-b border-border-color pb-2">
                                  <span className="font-mono text-xs font-bold text-brand-azure-strong">
                                    Log Entry ID: {entry.id}
                                  </span>
                                  <span className="text-[11px] text-text-body/60">
                                    Full Structured Audit Payload
                                  </span>
                                </div>
                                <pre className="mt-3 overflow-x-auto font-mono text-xs text-text-body">
                                  {JSON.stringify(entry.details, null, 2)}
                                </pre>
                              </div>
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Pagination */}
        {pagination && pagination.totalPages > 1 && (
          <div className="mt-6 flex items-center justify-between border-t border-border-color pt-4 text-xs">
            <span className="text-text-body/70">
              Page {pagination.page} of {pagination.totalPages} ({pagination.totalItems} entries)
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={pagination.page <= 1}
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={pagination.page >= pagination.totalPages}
                onClick={() => setCurrentPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
