import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Car,
  CheckCircle2,
  QrCode,
} from 'lucide-react';
import { parkingApi } from '@/api/parking';
import {
  VisitorParkingRequest,
  VisitorParkingRequestSchema,
  ParkingPermitResponse,
} from '@/api/types';
import { GradientHeading } from '@/components/ui/GradientHeading';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Progress } from '@/components/ui/Progress';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/Dialog';

export function ParkingPage() {
  const queryClient = useQueryClient();
  const [allocatedPermit, setAllocatedPermit] = useState<ParkingPermitResponse | null>(null);

  const { data: zones = [], isLoading } = useQuery({
    queryKey: ['parking', 'zones'],
    queryFn: () => parkingApi.getZones(),
    refetchInterval: 15000, // Periodic live refresh simulation
  });

  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const defaultDate = tomorrow.toISOString().split('T')[0];

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<VisitorParkingRequest>({
    resolver: zodResolver(VisitorParkingRequestSchema),
    defaultValues: {
      visitDate: defaultDate,
      startTime: '10:00',
      endTime: '17:00',
      vehicleType: 'FOUR_WHEELER',
      zoneId: 'pk000001-0000-4000-8000-000000000001',
      purpose: 'Guest Lecture on Cloud Computing',
      visitorName: 'Prof. Amit Sengupta',
      vehicleNumber: 'RJ-14-CB-9021',
    },
  });

  const permitMutation = useMutation({
    mutationFn: (data: VisitorParkingRequest) => parkingApi.requestPermit(data),
    onSuccess: (res) => {
      setAllocatedPermit(res);
      queryClient.invalidateQueries({ queryKey: ['parking', 'zones'] });
      reset();
    },
  });

  const onSubmit = (data: VisitorParkingRequest) => {
    permitMutation.mutate(data);
  };

  return (
    <div className="min-h-screen bg-surface-alt py-8">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div>
          <GradientHeading as="h1" className="text-3xl font-extrabold sm:text-4xl">
            Campus Parking Management
          </GradientHeading>
          <p className="mt-2 text-sm text-text-body/80 sm:text-base">
            Live occupancy monitoring across campus parking bays and instant synthetic gate pass allocation for visitors.
          </p>
        </div>

        {/* Live Zones Grid */}
        <div className="mt-8">
          <h2 className="font-heading text-lg font-bold text-text-heading">
            Live Parking Zone Occupancy
          </h2>

          <div className="mt-4 grid grid-cols-1 gap-6 sm:grid-cols-3">
            {isLoading
              ? [1, 2, 3].map((i) => (
                  <div key={i} className="h-44 animate-pulse rounded-lg bg-surface shadow-sm" />
                ))
              : zones.map((zone) => {
                  const utilPct = Math.round((zone.occupiedSlots / zone.totalSlots) * 100);
                  const isNearFull = zone.status === 'NEAR_FULL';
                  return (
                    <Card key={zone.id} className="shadow-sm">
                      <CardContent className="p-6">
                        <div className="flex items-center justify-between">
                          <span className="font-mono text-sm font-bold text-brand-azure-strong">
                            {zone.code}
                          </span>
                          <Badge variant={isNearFull ? 'warning' : 'success'}>
                            {zone.status.replace('_', ' ')}
                          </Badge>
                        </div>

                        <h3 className="mt-2 font-heading text-base font-bold text-text-heading line-clamp-1">
                          {zone.name}
                        </h3>

                        <div className="mt-4 flex items-baseline justify-between text-xs">
                          <span className="font-bold text-lg text-text-heading">
                            {zone.availableSlots}{' '}
                            <span className="text-xs font-normal text-text-body/70">slots free</span>
                          </span>
                          <span className="text-text-body/60">
                            {zone.occupiedSlots}/{zone.totalSlots} filled
                          </span>
                        </div>

                        <div className="mt-2">
                          <Progress value={utilPct} className="h-2" />
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
          </div>
        </div>

        {/* Visitor Parking Request Form */}
        <div className="mt-12 max-w-2xl">
          <Card className="shadow-sm">
            <CardHeader>
              <div className="flex items-center gap-2 text-brand-azure-strong">
                <Car className="h-5 w-5" aria-hidden="true" />
                <CardTitle>Request Visitor Parking Pass</CardTitle>
              </div>
              <CardDescription>
                Allocate a guaranteed visitor parking slot and generate an entry barcode for the main security gate.
              </CardDescription>
            </CardHeader>

            <CardContent>
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div>
                    <Label htmlFor="visitorName">Visitor Full Name</Label>
                    <Input
                      id="visitorName"
                      placeholder="e.g. Dr. R. Sharma"
                      className="mt-1"
                      {...register('visitorName')}
                    />
                    {errors.visitorName && (
                      <p className="mt-1 text-xs text-red-600">{errors.visitorName.message}</p>
                    )}
                  </div>

                  <div>
                    <Label htmlFor="vehicleNumber">Vehicle Registration Number</Label>
                    <Input
                      id="vehicleNumber"
                      placeholder="e.g. RJ-14-CB-9021"
                      className="mt-1 font-mono uppercase"
                      {...register('vehicleNumber')}
                    />
                    {errors.vehicleNumber && (
                      <p className="mt-1 text-xs text-red-600">{errors.vehicleNumber.message}</p>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div>
                    <Label htmlFor="vehicleType">Vehicle Category</Label>
                    <select
                      id="vehicleType"
                      className="mt-1 flex h-9 w-full rounded-md border border-border-color bg-surface px-3 py-1 text-sm text-text-body shadow-sm focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong"
                      {...register('vehicleType')}
                    >
                      <option value="FOUR_WHEELER">Four Wheeler (Car / SUV)</option>
                      <option value="TWO_WHEELER">Two Wheeler (Motorcycle)</option>
                      <option value="BUS_VAN">Bus / Passenger Van</option>
                    </select>
                  </div>

                  <div>
                    <Label htmlFor="zoneId">Target Parking Zone</Label>
                    <select
                      id="zoneId"
                      className="mt-1 flex h-9 w-full rounded-md border border-border-color bg-surface px-3 py-1 text-sm text-text-body shadow-sm focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-azure-strong"
                      {...register('zoneId')}
                    >
                      {zones.map((z) => (
                        <option key={z.id} value={z.id}>
                          {z.name} ({z.availableSlots} free)
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                  <div>
                    <Label htmlFor="visitDate">Date of Visit</Label>
                    <Input id="visitDate" type="date" className="mt-1" {...register('visitDate')} />
                    {errors.visitDate && (
                      <p className="mt-1 text-xs text-red-600">{errors.visitDate.message}</p>
                    )}
                  </div>

                  <div>
                    <Label htmlFor="startTime">Entry Time</Label>
                    <Input id="startTime" type="time" className="mt-1" {...register('startTime')} />
                  </div>

                  <div>
                    <Label htmlFor="endTime">Exit Time</Label>
                    <Input id="endTime" type="time" className="mt-1" {...register('endTime')} />
                  </div>
                </div>

                <div>
                  <Label htmlFor="purpose">Purpose of Visit</Label>
                  <Input
                    id="purpose"
                    placeholder="e.g. Guest Lecture, Lab Inspection, Conference Keynote"
                    className="mt-1"
                    {...register('purpose')}
                  />
                  {errors.purpose && (
                    <p className="mt-1 text-xs text-red-600">{errors.purpose.message}</p>
                  )}
                </div>

                <Button
                  type="submit"
                  variant="orange"
                  className="w-full font-semibold"
                  disabled={permitMutation.isPending}
                >
                  {permitMutation.isPending ? 'Allocating Slot...' : 'Issue Visitor Parking Pass'}
                </Button>
              </form>
            </CardContent>
          </Card>
        </div>

        {/* Instant Allocation Result Dialog */}
        <Dialog open={Boolean(allocatedPermit)} onOpenChange={() => setAllocatedPermit(null)}>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-green-100 text-green-700">
                <CheckCircle2 className="h-6 w-6" aria-hidden="true" />
              </div>
              <DialogTitle className="text-center font-heading text-xl">
                Visitor Parking Permit Issued
              </DialogTitle>
              <DialogDescription className="text-center">
                This pass has been logged in security gate master records.
              </DialogDescription>
            </DialogHeader>

            {allocatedPermit && (
              <div className="mt-4 space-y-4">
                <div className="rounded-lg border-2 border-dashed border-brand-azure-strong bg-brand-azure-strong/5 p-4 text-center">
                  <span className="text-xs uppercase tracking-wider text-text-body/60">
                    Allocated Bay Slot
                  </span>
                  <p className="font-heading text-4xl font-extrabold text-brand-azure-strong">
                    {allocatedPermit.allocatedSlot}
                  </p>
                  <p className="mt-1 font-mono text-xs text-text-body/80">
                    Permit Ref: {allocatedPermit.permitCode}
                  </p>

                  <div className="mt-4 flex justify-center">
                    <div className="flex h-24 w-24 items-center justify-center rounded border bg-white p-2 shadow-2xs">
                      <QrCode className="h-20 w-20 text-text-heading" aria-label="Visitor Gate Barcode" />
                    </div>
                  </div>
                </div>

                <div className="text-center text-xs text-text-body/70">
                  Valid until {new Date(allocatedPermit.validUntil).toLocaleString()}. Present to security booth at Main Gate.
                </div>

                <Button
                  type="button"
                  onClick={() => setAllocatedPermit(null)}
                  variant="default"
                  className="w-full"
                >
                  Done
                </Button>
              </div>
            )}
          </DialogContent>
        </Dialog>
      </div>
    </div>
  );
}
