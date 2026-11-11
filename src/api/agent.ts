import { AgentChatMessage } from './types';

export interface AgentStreamCallbacks {
  onChunk: (message: AgentChatMessage) => void;
  onDone: () => void;
  onError: (err: Error) => void;
}

/**
 * Scripted realistic mock streaming session that models SSE events:
 * 1. Plan step creation
 * 2. Tool call: checkRoomAvailability (simulating temporary failure + retry)
 * 3. Timetable clash detection on C-204
 * 4. Alternatives suggestion card
 * 5. Multi-resource bundle proposal (Room A-201 + MDC-101 + Parking P1)
 */
export async function streamAgentChat(
  userQuery: string,
  callbacks: AgentStreamCallbacks,
  abortSignal?: AbortSignal,
): Promise<void> {
  const messageId = crypto.randomUUID();

  const delay = (ms: number) =>
    new Promise((resolve, reject) => {
      const timer = setTimeout(resolve, ms);
      if (abortSignal) {
        abortSignal.addEventListener('abort', () => {
          clearTimeout(timer);
          reject(new Error('Streaming interrupted by user'));
        });
      }
    });

  try {
    // 1. Initial Plan Card
    await delay(300);
    callbacks.onChunk({
      id: messageId,
      sender: 'assistant',
      timestamp: new Date().toISOString(),
      text: "I am analyzing your event request. Let me coordinate the required academic classrooms, guest accommodations, and visitor parking.",
      plan: [
        { id: 1, title: 'Analyze capacity and audio-visual equipment requirements', status: 'done' },
        { id: 2, title: 'Cross-check 8 semester timetables for Thursday 15:00-17:00 slots', status: 'running' },
        { id: 3, title: 'Verify MDC Guest House room vacancy for visiting speaker', status: 'pending' },
        { id: 4, title: 'Allocate Main Gate Visitor Parking permit', status: 'pending' },
        { id: 5, title: 'Synthesize multi-resource proposal for administrative approval', status: 'pending' },
      ],
    });

    // 2. Simulated Tool Failure with Automatic Retry
    await delay(600);
    callbacks.onChunk({
      id: messageId,
      sender: 'assistant',
      timestamp: new Date().toISOString(),
      text: "Querying room timetable databases...",
      plan: [
        { id: 1, title: 'Analyze capacity and audio-visual equipment requirements', status: 'done' },
        { id: 2, title: 'Cross-check 8 semester timetables for Thursday 15:00-17:00 slots', status: 'running' },
        { id: 3, title: 'Verify MDC Guest House room vacancy for visiting speaker', status: 'pending' },
        { id: 4, title: 'Allocate Main Gate Visitor Parking permit', status: 'pending' },
        { id: 5, title: 'Synthesize multi-resource proposal for administrative approval', status: 'pending' },
      ],
      toolCalls: [
        {
          id: 'tc-1',
          toolName: 'queryTimetableDatabase',
          args: { date: '2026-11-19', time: '15:00-17:00', minCapacity: 80 },
          resultSummary: 'Database connection timeout on replica 2. Retrying with primary shard...',
          durationMs: 240,
          status: 'retrying',
          retryCount: 1,
        },
      ],
    });

    // 3. Successful Retry of Tool Call & Conflict Detection
    await delay(800);
    callbacks.onChunk({
      id: messageId,
      sender: 'assistant',
      timestamp: new Date().toISOString(),
      text: "Timetable check finished on primary shard. I discovered a schedule clash with a scheduled lecture in **Room C-204**.",
      plan: [
        { id: 1, title: 'Analyze capacity and audio-visual equipment requirements', status: 'done' },
        { id: 2, title: 'Cross-check 8 semester timetables for Thursday 15:00-17:00 slots', status: 'done' },
        { id: 3, title: 'Verify MDC Guest House room vacancy for visiting speaker', status: 'running' },
        { id: 4, title: 'Allocate Main Gate Visitor Parking permit', status: 'pending' },
        { id: 5, title: 'Synthesize multi-resource proposal for administrative approval', status: 'pending' },
      ],
      toolCalls: [
        {
          id: 'tc-1',
          toolName: 'queryTimetableDatabase',
          args: { date: '2026-11-19', time: '15:00-17:00', minCapacity: 80 },
          resultSummary: 'Recovered on primary replica. Timetable scanned: 1 clash detected.',
          durationMs: 310,
          status: 'success',
          retryCount: 1,
        },
      ],
      conflict: {
        resourceName: 'Room C-204 (Management Block)',
        reason:
          'Room C-204 is occupied by B.Tech CSE Sem 3 Data Structures from 15:00 to 16:00. Scheduling here would displace 65 enrolled students.',
        clashingSlot: '15:00-16:00',
        alternatives: [
          {
            id: 'a0000004-0000-4000-8000-000000000004',
            name: 'Room A-201 (Computing Block)',
            capacity: 80,
            features: ['Projector', 'AC', 'Smartboard', 'Recording'],
          },
          {
            id: 'a0000007-0000-4000-8000-000000000007',
            name: 'Room A-301 (Grand Seminar Hall)',
            capacity: 120,
            features: ['Dual Projectors', 'Podium', 'Mic', 'AC'],
          },
        ],
      },
    });

    // 4. Multi-Resource Synthesis & Final Proposal
    await delay(1000);
    callbacks.onChunk({
      id: messageId,
      sender: 'assistant',
      timestamp: new Date().toISOString(),
      text: "### Campus Multi-Resource Proposal Prepared\n\nI have reconciled your request without clashes:\n- Selected **Room A-201** (80 capacity, fully free).\n- Reserved **MDC-101 (Deluxe)** at the Guest House.\n- Issued Visitor Parking Pass for Main Gate **Zone P1**.\n\nPlease review the unified proposal below and click **'Submit for Admin Approval'**.",
      plan: [
        { id: 1, title: 'Analyze capacity and audio-visual equipment requirements', status: 'done' },
        { id: 2, title: 'Cross-check 8 semester timetables for Thursday 15:00-17:00 slots', status: 'done' },
        { id: 3, title: 'Verify MDC Guest House room vacancy for visiting speaker', status: 'done' },
        { id: 4, title: 'Allocate Main Gate Visitor Parking permit', status: 'done' },
        { id: 5, title: 'Synthesize multi-resource proposal for administrative approval', status: 'done' },
      ],
      toolCalls: [
        {
          id: 'tc-1',
          toolName: 'queryTimetableDatabase',
          args: { date: '2026-11-19', time: '15:00-17:00' },
          resultSummary: 'Success (auto-recovered from replica)',
          durationMs: 310,
          status: 'success',
          retryCount: 1,
        },
        {
          id: 'tc-2',
          toolName: 'reserveGuestHouseRoom',
          args: { roomType: 'DELUXE', checkIn: '2026-11-18', checkOut: '2026-11-20' },
          resultSummary: 'MDC-101 allocated for visiting keynote speaker',
          durationMs: 95,
          status: 'success',
        },
        {
          id: 'tc-3',
          toolName: 'generateVisitorParkingPermit',
          args: { zone: 'P1', vehicleType: 'FOUR_WHEELER' },
          resultSummary: 'Permit JKLU-PRK-842 allocated for Bay P1-04',
          durationMs: 70,
          status: 'success',
        },
      ],
      proposal: {
        title: userQuery.includes('Guest lecture')
          ? 'Distinguished Guest Lecture & Delhi Speaker Accommodation'
          : 'Coordinated Campus Event Package',
        roomName: 'Room A-201 (Computing Block)',
        roomId: 'a0000004-0000-4000-8000-000000000004',
        date: '2026-11-19',
        time: '15:00 - 17:00',
        capacity: 80,
        guestHouse: 'MDC-101 (Deluxe Room, 18-20 Nov)',
        parking: 'Zone P1 (Main Gate, Bay P1-04)',
        requiresAdminApproval: true,
        status: 'DRAFT',
      },
    });

    callbacks.onDone();
  } catch (err: unknown) {
    callbacks.onError(err instanceof Error ? err : new Error('Stream execution interrupted'));
  }
}

