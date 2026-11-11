import { useState, useRef } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  Upload,
  FileSpreadsheet,
  AlertTriangle,
  CheckCircle2,
  AlertCircle,
  FileText,
  Send,
  Trash2,
} from 'lucide-react';
import { adminApi, TimetableUploadResponse } from '@/api/admin';
import { GradientHeading } from '@/components/ui/GradientHeading';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/Dialog';

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ALLOWED_TYPES = [
  'text/csv',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/pdf',
];

export function TimetableUploadPage() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [parseResult, setParseResult] = useState<TimetableUploadResponse | null>(null);
  const [publishDialogOpen, setPublishDialogOpen] = useState(false);
  const [publishSuccess, setPublishSuccess] = useState<string | null>(null);

  // Editable row overrides in client
  const [editableRows, setEditableRows] = useState<Record<number, { roomNumber: string }>>({});

  const uploadMutation = useMutation({
    mutationFn: (file: File) => adminApi.uploadTimetable(file),
    onSuccess: (data) => {
      setParseResult(data);
    },
  });

  const publishMutation = useMutation({
    mutationFn: () =>
      adminApi.publishTimetable('Spring Semester 2027', parseResult?.preview || []),
    onSuccess: (res) => {
      setPublishDialogOpen(false);
      setPublishSuccess(res.message);
      setParseResult(null);
      setSelectedFile(null);
    },
  });

  const handleFile = (file: File) => {
    setValidationError(null);
    setPublishSuccess(null);

    // Validate size
    if (file.size > MAX_FILE_SIZE) {
      setValidationError('File size exceeds the 10 MB maximum allowed limit.');
      return;
    }

    // Validate file type
    const isExtensionMatch = /\.(csv|xlsx|xls|pdf)$/i.test(file.name);
    if (!ALLOWED_TYPES.includes(file.type) && !isExtensionMatch) {
      setValidationError('Only CSV, Excel (.xlsx/.xls), and PDF timetable documents are supported.');
      return;
    }

    setSelectedFile(file);
    uploadMutation.mutate(file);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  const updateRowRoom = (rowNum: number, newRoom: string) => {
    setEditableRows((prev) => ({
      ...prev,
      [rowNum]: { roomNumber: newRoom },
    }));
  };

  return (
    <div className="min-h-screen bg-surface-alt py-8">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div>
          <GradientHeading as="h1" className="text-3xl font-extrabold sm:text-4xl">
            Semester Timetable Upload & Synchronization
          </GradientHeading>
          <p className="mt-2 text-sm text-text-body/80 sm:text-base">
            Upload institutional semester schedules in CSV, Excel, or PDF. The client parser validates room existence and detects timetable clashes before publishing.
          </p>
        </div>

        {/* Success Message */}
        {publishSuccess && (
          <div className="mt-6 rounded-lg border border-green-200 bg-green-50 p-4 text-green-900 flex items-center gap-2">
            <CheckCircle2 className="h-5 w-5 text-green-700 shrink-0" aria-hidden="true" />
            <div>
              <p className="font-semibold">Timetable Published Successfully!</p>
              <p className="text-xs text-green-800">{publishSuccess}</p>
            </div>
          </div>
        )}

        {/* Drag & Drop Upload Card */}
        <Card className="mt-6 shadow-sm">
          <CardContent className="p-8">
            <div
              onDrop={handleDrop}
              onDragOver={(e) => e.preventDefault()}
              className="flex flex-col items-center justify-center rounded-xl border-2 border-dashed border-border-color bg-surface-alt/50 p-8 text-center transition hover:border-brand-azure-strong"
            >
              <FileSpreadsheet className="h-12 w-12 text-brand-azure-strong" aria-hidden="true" />
              <h3 className="mt-4 font-heading font-bold text-text-heading text-lg">
                Drag and drop your timetable file here
              </h3>
              <p className="mt-1 text-xs text-text-body/70 sm:text-sm">
                Supported formats: CSV, XLSX, XLS, PDF (Max 10 MB per file)
              </p>

              <input
                ref={fileInputRef}
                type="file"
                accept=".csv,.xlsx,.xls,.pdf"
                className="hidden"
                onChange={(e) => {
                  if (e.target.files && e.target.files[0]) {
                    handleFile(e.target.files[0]);
                  }
                }}
              />

              <Button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                variant="default"
                size="sm"
                className="mt-4 font-semibold"
                disabled={uploadMutation.isPending}
              >
                <Upload className="mr-2 h-4 w-4" aria-hidden="true" />
                {uploadMutation.isPending ? 'Parsing Document...' : 'Browse Local Files'}
              </Button>
            </div>

            {/* Validation Error */}
            {validationError && (
              <div role="alert" className="mt-4 flex items-center gap-2 rounded-md border border-red-200 bg-red-50 p-3 text-xs text-red-900">
                <AlertTriangle className="h-4 w-4 text-red-600 shrink-0" aria-hidden="true" />
                <span>{validationError}</span>
              </div>
            )}

            {selectedFile && !uploadMutation.isPending && (
              <div className="mt-4 flex items-center justify-between rounded-md border border-border-color bg-surface p-3 text-xs">
                <div className="flex items-center gap-2">
                  <FileText className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
                  <span className="font-semibold text-text-heading">{selectedFile.name}</span>
                  <span className="text-text-body/60">({(selectedFile.size / 1024).toFixed(1)} KB)</span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setSelectedFile(null);
                    setParseResult(null);
                  }}
                  className="text-red-600 hover:text-red-700"
                  aria-label="Remove uploaded file"
                >
                  <Trash2 className="h-4 w-4" aria-hidden="true" />
                </button>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Parsed Preview Table & Row Validation Errors */}
        {parseResult && (
          <div className="mt-8 space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 className="font-heading text-xl font-bold text-text-heading">
                  Parsed Timetable Preview ({parseResult.totalRows} Entries)
                </h2>
                <div className="mt-1 flex items-center gap-3 text-xs">
                  <span className="text-green-700 font-medium">
                    ✓ {parseResult.validRows} valid rows ready
                  </span>
                  {parseResult.errorRows.length > 0 && (
                    <span className="text-red-700 font-medium">
                      ⚠ {parseResult.errorRows.length} validation errors flagged
                    </span>
                  )}
                </div>
              </div>

              <Button
                type="button"
                onClick={() => setPublishDialogOpen(true)}
                variant="orange"
                className="font-bold"
              >
                <Send className="mr-2 h-4 w-4" aria-hidden="true" />
                Approve & Publish Timetable
              </Button>
            </div>

            {/* Flagged Error Rows Callout */}
            {parseResult.errorRows.length > 0 && (
              <div className="rounded-lg border border-red-300 bg-red-50 p-4">
                <h3 className="font-semibold text-sm text-red-900 flex items-center gap-1.5">
                  <AlertCircle className="h-4 w-4 text-red-700" aria-hidden="true" />
                  Per-Row Validation Errors (Editable in preview table):
                </h3>
                <ul className="mt-2 space-y-1 text-xs text-red-800">
                  {parseResult.errorRows.map((err) => (
                    <li key={err.rowNumber}>
                      • <strong>Row #{err.rowNumber} ({err.course}):</strong> {err.error}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Editable Preview Table */}
            <div className="overflow-x-auto rounded-lg border border-border-color bg-surface shadow-sm">
              <table className="w-full text-left text-xs">
                <thead className="border-b border-border-color bg-surface-alt font-semibold text-text-heading">
                  <tr>
                    <th className="px-4 py-3">Row</th>
                    <th className="px-4 py-3">Semester</th>
                    <th className="px-4 py-3">Course Code</th>
                    <th className="px-4 py-3">Course Name</th>
                    <th className="px-4 py-3">Instructor</th>
                    <th className="px-4 py-3">Room Assigned</th>
                    <th className="px-4 py-3">Day</th>
                    <th className="px-4 py-3">Slot Time</th>
                    <th className="px-4 py-3">Validation Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border-color">
                  {parseResult.preview.map((row) => {
                    const rowError = parseResult.errorRows.find((e) => e.rowNumber === row.rowNumber);
                    const currentRoom = editableRows[row.rowNumber]?.roomNumber || row.roomNumber;
                    return (
                      <tr key={row.rowNumber} className={rowError ? 'bg-red-50/40' : ''}>
                        <td className="px-4 py-3 font-mono text-text-body/60">#{row.rowNumber}</td>
                        <td className="px-4 py-3 font-medium text-text-heading">{row.semester}</td>
                        <td className="px-4 py-3 font-mono font-bold text-brand-azure-strong">{row.courseCode}</td>
                        <td className="px-4 py-3">{row.courseName}</td>
                        <td className="px-4 py-3 text-text-body/80">{row.instructor}</td>
                        <td className="px-4 py-3">
                          <input
                            type="text"
                            value={currentRoom}
                            onChange={(e) => updateRowRoom(row.rowNumber, e.target.value)}
                            className="w-20 rounded border border-border-color px-2 py-1 font-mono text-xs focus:border-brand-azure-strong"
                          />
                        </td>
                        <td className="px-4 py-3">{row.dayOfWeek}</td>
                        <td className="px-4 py-3 font-mono">{row.startTime} - {row.endTime}</td>
                        <td className="px-4 py-3">
                          {rowError ? (
                            <Badge variant="destructive" className="text-[10px]">
                              Error: {rowError.error}
                            </Badge>
                          ) : (
                            <Badge variant="success" className="text-[10px]">
                              Verified
                            </Badge>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Publish Confirmation Dialog */}
        <Dialog open={publishDialogOpen} onOpenChange={setPublishDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Publish Timetable & Overwrite Models?</DialogTitle>
              <DialogDescription>
                Publishing will update classroom availability across the entire campus. Existing approved bookings that clash with the new timetable will be flagged for review.
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="outline" onClick={() => setPublishDialogOpen(false)}>
                Cancel
              </Button>
              <Button
                variant="orange"
                onClick={() => publishMutation.mutate()}
                disabled={publishMutation.isPending}
              >
                {publishMutation.isPending ? 'Publishing...' : 'Confirm & Publish'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </div>
  );
}
