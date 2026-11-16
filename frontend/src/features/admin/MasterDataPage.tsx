import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Layers,
  Building,
  Home,
  Car,
  Search,
  Plus,
  Edit,
  PowerOff,
  FileSpreadsheet,
  Download,
} from 'lucide-react';
import { adminApi } from '@/api/admin';
import { PageHeader } from '@/components/layout/PageHeader';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';

type MasterTab = 'rooms' | 'blocks' | 'guesthouse' | 'parking';

interface MasterRoom {
  id: string;
  roomNumber: string;
  name?: string;
  block?: string;
  floor?: number;
  capacity?: number;
  type?: string;
  active?: boolean;
  bookableOutside?: boolean;
  features?: string[];
}

interface MasterBlock {
  id: string;
  name: string;
  code?: string;
  active?: boolean;
}

interface MasterGuestRoom {
  id: string;
  roomNumber: string;
  type?: string;
  roomType?: string;
  capacity?: number;
  pricePerNight?: number;
  ratePerNight?: number;
  active?: boolean;
}

interface MasterParking {
  id: string;
  name: string;
  code?: string;
  totalSlots?: number;
  active?: boolean;
}

export function MasterDataPage() {
  const [activeTab, setActiveTab] = useState<MasterTab>('rooms');
  const [searchQuery, setSearchQuery] = useState('');
  const [showCsvModal, setShowCsvModal] = useState(false);
  const [overrides, setOverrides] = useState<Record<string, boolean>>({});

  const toggleRoomBookable = (id: string) => {
    setOverrides((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  // Live queries from PostgreSQL backend
  const { data: liveRooms = [] } = useQuery<MasterRoom[]>({
    queryKey: ['admin', 'master', 'rooms'],
    queryFn: () => adminApi.getMasterData('rooms') as Promise<MasterRoom[]>,
  });
  const { data: liveBlocks = [] } = useQuery<MasterBlock[]>({
    queryKey: ['admin', 'master', 'blocks'],
    queryFn: () => adminApi.getMasterData('blocks') as Promise<MasterBlock[]>,
  });
  const { data: liveGuestHouse = [] } = useQuery<MasterGuestRoom[]>({
    queryKey: ['admin', 'master', 'guesthouse'],
    queryFn: () => adminApi.getMasterData('guesthouse') as Promise<MasterGuestRoom[]>,
  });
  const { data: liveParking = [] } = useQuery<MasterParking[]>({
    queryKey: ['admin', 'master', 'parking'],
    queryFn: () => adminApi.getMasterData('parking') as Promise<MasterParking[]>,
  });

  const rooms: MasterRoom[] = liveRooms;
  const blocks: MasterBlock[] = liveBlocks;
  const guestHouse: MasterGuestRoom[] = liveGuestHouse;
  const parking: MasterParking[] = liveParking;

  const filteredRooms = rooms.filter(
    (r: MasterRoom) =>
      (r.roomNumber || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (r.block || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (r.type || '').toLowerCase().includes(searchQuery.toLowerCase()),
  );

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <PageHeader
        title="Campus Master Data Management"
        description="Maintain institutional registries for academic blocks, classrooms, specialized labs, guest suites, and parking zones."
        actions={
          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setShowCsvModal(true)}
            >
              <FileSpreadsheet className="mr-2 h-4 w-4" aria-hidden="true" />
              CSV Import Preview
            </Button>
            <Button type="button" variant="orange" size="sm" className="font-semibold">
              <Plus className="mr-1.5 h-4 w-4" aria-hidden="true" />
              Add Record
            </Button>
          </div>
        }
      />

        {/* Tab Navigation */}
        <div className="mt-8 flex flex-wrap gap-2 border-b border-border-color pb-3">
          {[
            { id: 'rooms', label: `Rooms & Labs (${rooms.length})`, icon: Building },
            { id: 'blocks', label: `Academic Blocks (${blocks.length})`, icon: Layers },
            { id: 'guesthouse', label: `MDC Guest House (${guestHouse.length})`, icon: Home },
            { id: 'parking', label: `Parking Zones (${parking.length})`, icon: Car },
          ].map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => {
                  setActiveTab(tab.id as MasterTab);
                  setSearchQuery('');
                }}
                className={`inline-flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition ${
                  isActive
                    ? 'bg-brand-azure-strong text-white shadow-sm'
                    : 'bg-surface text-text-body hover:bg-surface-alt border border-border-color'
                }`}
              >
                <Icon className="h-4 w-4" aria-hidden="true" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        {/* Search Bar */}
        <div className="mt-6 max-w-md">
          <div className="relative">
            <Search
              className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-text-body/50"
              aria-hidden="true"
            />
            <Input
              type="search"
              placeholder={`Search ${activeTab}...`}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 text-xs"
            />
          </div>
        </div>

        {/* Tab Content: Rooms */}
        {activeTab === 'rooms' && (
          <div className="mt-6 overflow-x-auto rounded-lg border border-border-color bg-surface shadow-sm">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-border-color bg-surface-alt font-semibold text-text-heading">
                <tr>
                  <th className="px-4 py-3">Room No.</th>
                  <th className="px-4 py-3">Academic Block</th>
                  <th className="px-4 py-3">Floor</th>
                  <th className="px-4 py-3">Capacity</th>
                  <th className="px-4 py-3">Category</th>
                  <th className="px-4 py-3">Installed Features</th>
                  <th className="px-4 py-3">Publicly Bookable</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border-color">
                {filteredRooms.map((room) => (
                  <tr key={room.id} className="hover:bg-surface-alt/50">
                    <td className="px-4 py-3 font-mono font-bold text-text-heading">
                      {room.roomNumber}
                    </td>
                    <td className="px-4 py-3">{room.block}</td>
                    <td className="px-4 py-3">{room.floor}</td>
                    <td className="px-4 py-3 font-semibold">{room.capacity} seats</td>
                    <td className="px-4 py-3">
                      <Badge variant="secondary" className="text-[10px]">
                        {room.type}
                      </Badge>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1">
                        {Array.isArray(room.features) && room.features.map((f: string) => (
                          <span
                            key={f}
                            className="rounded bg-surface-alt px-1.5 py-0.5 text-[10px] text-text-body/70 border border-border-color"
                          >
                            {f}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        onClick={() => toggleRoomBookable(room.id)}
                        className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold transition ${
                          (overrides[room.id] !== undefined ? overrides[room.id] : (room.active !== false && room.bookableOutside !== false))
                            ? 'bg-green-100 text-green-800'
                            : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {(overrides[room.id] !== undefined ? overrides[room.id] : (room.active !== false && room.bookableOutside !== false))
                          ? 'Active (Open)'
                          : 'Deactivated'}
                      </button>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          type="button"
                          className="rounded p-1 text-brand-azure-strong hover:bg-brand-azure/10"
                          title="Edit Room"
                          aria-label={`Edit Room ${room.roomNumber}`}
                        >
                          <Edit className="h-3.5 w-3.5" aria-hidden="true" />
                        </button>
                        <button
                          type="button"
                          onClick={() => toggleRoomBookable(room.id)}
                          className="rounded p-1 text-red-600 hover:bg-red-50"
                          title="Toggle Status"
                          aria-label={`Toggle status for Room ${room.roomNumber}`}
                        >
                          <PowerOff className="h-3.5 w-3.5" aria-hidden="true" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Tab Content: Blocks */}
        {activeTab === 'blocks' && (
          <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
            {blocks.map((b: MasterBlock) => (
              <Card key={b.id} className="shadow-sm">
                <CardContent className="p-6">
                  <span className="font-mono text-xs text-text-body/60">Code: {b.code || b.id.slice(0, 8)}</span>
                  <h3 className="mt-2 font-heading text-lg font-bold text-text-heading">{b.name}</h3>
                  <p className="mt-1 text-xs text-text-body/70">
                    Host to academic classrooms, departments, and computer labs.
                  </p>
                  <div className="mt-4 flex items-center justify-between">
                    <Badge variant={b.active !== false ? 'success' : 'destructive'} className="text-[10px]">
                      {b.active !== false ? 'Active' : 'Inactive'}
                    </Badge>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        {/* Tab Content: Guest House */}
        {activeTab === 'guesthouse' && (
          <div className="mt-6 overflow-x-auto rounded-lg border border-border-color bg-surface shadow-sm">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-border-color bg-surface-alt font-semibold text-text-heading">
                <tr>
                  <th className="px-4 py-3">Room No.</th>
                  <th className="px-4 py-3">Category</th>
                  <th className="px-4 py-3">Capacity</th>
                  <th className="px-4 py-3">Nightly Rate</th>
                  <th className="px-4 py-3">Operational State</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border-color">
                {guestHouse.map((gh: MasterGuestRoom) => (
                  <tr key={gh.id} className="hover:bg-surface-alt/50">
                    <td className="px-4 py-3 font-mono font-bold text-text-heading">{gh.roomNumber}</td>
                    <td className="px-4 py-3">{gh.type || gh.roomType || 'STANDARD'}</td>
                    <td className="px-4 py-3">{gh.capacity ?? 2} Persons</td>
                    <td className="px-4 py-3 font-semibold">₹{gh.pricePerNight || gh.ratePerNight || 1200}</td>
                    <td className="px-4 py-3">
                      <Badge variant={gh.active !== false ? 'success' : 'destructive'} className="text-[10px]">
                        {gh.active !== false ? 'Operational' : 'Maintenance'}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Tab Content: Parking */}
        {activeTab === 'parking' && (
          <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
            {parking.map((pz: MasterParking) => (
              <Card key={pz.id} className="shadow-sm">
                <CardContent className="p-6">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-sm font-bold text-brand-azure-strong">{pz.code}</span>
                    <Badge variant={pz.active !== false ? 'success' : 'destructive'}>
                      {pz.active !== false ? 'Active Zone' : 'Closed'}
                    </Badge>
                  </div>
                  <h3 className="mt-2 font-heading text-base font-bold text-text-heading">{pz.name}</h3>
                  <div className="mt-4 grid grid-cols-2 gap-2 text-xs">
                    <div>
                      <span className="text-text-body/60">Total Capacity:</span>
                      <span className="block font-bold">{pz.totalSlots || 50} bays</span>
                    </div>
                    <div>
                      <span className="text-text-body/60">Hourly Rate:</span>
                      <span className="block font-bold">Free (Permit Req.)</span>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        {/* CSV Import Preview Modal Mockup */}
        {showCsvModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
            <div className="w-full max-w-xl rounded-lg bg-surface p-6 shadow-xl">
              <div className="flex items-center justify-between border-b border-border-color pb-3">
                <h3 className="font-heading font-bold text-lg text-text-heading">
                  Master Data CSV Batch Import Preview
                </h3>
                <button
                  type="button"
                  onClick={() => setShowCsvModal(false)}
                  className="rounded p-1 text-text-body hover:bg-surface-alt"
                >
                  ✕
                </button>
              </div>

              <div className="mt-4 space-y-3 text-xs text-text-body/80">
                <p>
                  Download the CSV schema template, populate room attributes, and batch upload.
                </p>
                <div className="rounded border border-border-color bg-surface-alt p-3 font-mono text-[11px]">
                  roomNumber,block,floor,capacity,type,features,bookableOutside<br />
                  A-101,Block A,1,60,CLASSROOM,&quot;projector,ac,smartboard&quot;,true<br />
                  B-205,Block B,2,35,LAB,&quot;fpga_boards,cadence_eda&quot;,false
                </div>
              </div>

              <div className="mt-6 flex justify-end gap-3">
                <Button variant="outline" size="sm" onClick={() => setShowCsvModal(false)}>
                  Cancel
                </Button>
                <Button variant="default" size="sm" onClick={() => setShowCsvModal(false)}>
                  <Download className="mr-1.5 h-3.5 w-3.5" aria-hidden="true" />
                  Download CSV Template
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
  );
}
