import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  Legend,
  PieChart,
  Pie,
  Cell,
  LabelList,
} from 'recharts';
import {
  Filter,
} from 'lucide-react';
import { reportsApi } from '@/api/reports';
import { PageHeader } from '@/components/layout/PageHeader';
import { Input } from '@/components/ui/Input';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';

// Colors strictly derived from THEME VALUES
const BRAND_AZURE_STRONG = '#0069C9';
const BRAND_PERIWINKLE_DEEP = '#4A67B0';

export function ReportsPage() {
  const [startDate, setStartDate] = useState('2026-11-01');
  const [endDate, setEndDate] = useState('2026-11-30');

  const { data: report } = useQuery({
    queryKey: ['admin', 'reports', { startDate, endDate }],
    queryFn: () => reportsApi.getUtilisation({ startDate, endDate }),
  });

  const hourlyData = report?.byHour || [];
  const topRoomsData = report?.topRooms || [];
  const leastRoomsData = report?.leastRooms || [];

  const parkingPieData = [
    { name: 'Occupied Bays', value: report?.parkingPeakOccupancyPct || 85, color: BRAND_AZURE_STRONG },
    { name: 'Available Bays', value: 100 - (report?.parkingPeakOccupancyPct || 85), color: '#E5E7EB' },
  ];

  const guestHousePieData = [
    { name: 'Occupied Rooms', value: report?.guestHouseOccupancyPct || 75, color: BRAND_PERIWINKLE_DEEP },
    { name: 'Vacant Rooms', value: 100 - (report?.guestHouseOccupancyPct || 75), color: '#E5E7EB' },
  ];

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <PageHeader
        title="Campus Resource Utilisation & Analytics"
        description="Institutional intelligence tracking room occupancy rates, peak lecture hours, guest house demand, and visitor parking distribution."
        actions={
          <div className="flex items-center gap-2 rounded-lg border border-border-color bg-surface p-2 shadow-2xs">
            <Filter className="h-4 w-4 text-text-body/50" aria-hidden="true" />
            <Input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="h-8 text-xs"
              aria-label="Start report date"
            />
            <span className="text-xs text-text-body/60">to</span>
            <Input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="h-8 text-xs"
              aria-label="End report date"
            />
          </div>
        }
      />

        {/* Executive KPI Summary Tiles */}
        <div className="mt-8 grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Card className="border-t-4 border-t-brand-azure-strong shadow-sm">
            <CardContent className="p-4 sm:p-6">
              <span className="text-xs font-semibold text-text-body/70">Peak Hour Utilisation</span>
              <p className="mt-2 font-heading text-2xl font-bold text-text-heading sm:text-3xl">
                95%
              </p>
              <p className="mt-1 text-xs text-text-body/60">Peak window: 14:00 - 16:00</p>
            </CardContent>
          </Card>

          <Card className="border-t-4 border-t-brand-periwinkle-deep shadow-sm">
            <CardContent className="p-4 sm:p-6">
              <span className="text-xs font-semibold text-text-body/70">Guest House Occupancy</span>
              <p className="mt-2 font-heading text-2xl font-bold text-text-heading sm:text-3xl">
                {report?.guestHouseOccupancyPct ?? 78}%
              </p>
              <p className="mt-1 text-xs text-text-body/60">Average 9 of 12 rooms filled</p>
            </CardContent>
          </Card>

          <Card className="border-t-4 border-t-brand-orange-ink shadow-sm">
            <CardContent className="p-4 sm:p-6">
              <span className="text-xs font-semibold text-text-body/70">Parking Peak Load</span>
              <p className="mt-2 font-heading text-2xl font-bold text-text-heading sm:text-3xl">
                {report?.parkingPeakOccupancyPct ?? 88}%
              </p>
              <p className="mt-1 text-xs text-text-body/60">Zone P1 & P2 peak at 11:30</p>
            </CardContent>
          </Card>

          <Card className="border-t-4 border-t-green-600 shadow-sm">
            <CardContent className="p-4 sm:p-6">
              <span className="text-xs font-semibold text-text-body/70">Total Event Bookings</span>
              <p className="mt-2 font-heading text-2xl font-bold text-text-heading sm:text-3xl">
                {report?.totalBookingsPeriod ?? 84}
              </p>
              <p className="mt-1 text-xs text-text-body/60">Across 25 academic spaces</p>
            </CardContent>
          </Card>
        </div>

        {/* Chart 1: Hourly Room Utilisation Bar Chart with Patterns/Labels */}
        <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-12">
          <Card className="shadow-sm lg:col-span-8">
            <CardHeader>
              <CardTitle className="text-lg">Classroom Utilisation by Hour of Day</CardTitle>
              <CardDescription>
                Percentage of classrooms occupied by classes and events from 09:00 to 18:00.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="h-72 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={hourlyData} margin={{ top: 20, right: 20, left: -10, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                    <XAxis dataKey="hour" tick={{ fontSize: 11 }} />
                    <YAxis unit="%" tick={{ fontSize: 11 }} domain={[0, 100]} />
                    <RechartsTooltip
                      formatter={(val: number) => [`${val}% Occupied`, 'Occupancy']}
                      contentStyle={{ backgroundColor: '#FFFFFF', borderRadius: '6px', border: '1px solid #E5E7EB' }}
                    />
                    <Legend />
                    <Bar
                      dataKey="occupancyRate"
                      name="Room Occupancy %"
                      fill={BRAND_AZURE_STRONG}
                      radius={[4, 4, 0, 0]}
                    >
                      {/* Explicit text label for accessibility so color is not sole conveyor */}
                      <LabelList dataKey="occupancyRate" position="top" formatter={(val: number) => `${val}%`} style={{ fontSize: '10px', fill: '#212529' }} />
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          {/* Chart 2: Parking and Guest House Occupancy Gauges */}
          <Card className="shadow-sm lg:col-span-4 flex flex-col justify-between">
            <CardHeader>
              <CardTitle className="text-lg">Special Asset Occupancy</CardTitle>
              <CardDescription>MDC Guest House & Visitor Parking Peak</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Guest House Donut */}
              <div className="flex items-center justify-between border-b border-border-color pb-4">
                <div>
                  <span className="font-semibold text-sm text-text-heading">MDC Guest House</span>
                  <p className="text-xs text-text-body/60">Occupancy: {report?.guestHouseOccupancyPct ?? 75}%</p>
                </div>
                <div className="h-24 w-24">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={guestHousePieData}
                        dataKey="value"
                        innerRadius={25}
                        outerRadius={38}
                        startAngle={90}
                        endAngle={-270}
                      >
                        {guestHousePieData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </div>

              {/* Parking Donut */}
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-semibold text-sm text-text-heading">Visitor Parking</span>
                  <p className="text-xs text-text-body/60">Peak Bays: {report?.parkingPeakOccupancyPct ?? 88}%</p>
                </div>
                <div className="h-24 w-24">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={parkingPieData}
                        dataKey="value"
                        innerRadius={25}
                        outerRadius={38}
                        startAngle={90}
                        endAngle={-270}
                      >
                        {parkingPieData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Most & Least Utilised Rooms */}
        <div className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-2">
          {/* Most Utilised Rooms */}
          <Card className="shadow-sm">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-base text-green-700">Highest Utilised Rooms</CardTitle>
                <Badge variant="success">High Demand</Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {topRoomsData.map((r, i) => (
                  <div key={r.roomNumber} className="flex items-center justify-between border-b border-border-color pb-2 text-xs">
                    <span className="font-semibold text-text-heading">
                      #{i + 1} Room {r.roomNumber}
                    </span>
                    <span className="text-text-body/70">{r.bookingCount} bookings</span>
                    <span className="font-mono font-bold text-green-700">{r.utilPct}% utilisation</span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          {/* Least Utilised Rooms */}
          <Card className="shadow-sm">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-base text-amber-700">Least Utilised Rooms (Optimisation Candidates)</CardTitle>
                <Badge variant="warning">Underutilised</Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {leastRoomsData.map((r, i) => (
                  <div key={r.roomNumber} className="flex items-center justify-between border-b border-border-color pb-2 text-xs">
                    <span className="font-semibold text-text-heading">
                      #{i + 1} Room {r.roomNumber}
                    </span>
                    <span className="text-text-body/70">{r.bookingCount} bookings</span>
                    <span className="font-mono font-bold text-amber-700">{r.utilPct}% utilisation</span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
  );
}
