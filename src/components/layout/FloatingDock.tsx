import { Link } from 'react-router-dom';
import { MessageCircle, PlusCircle, Search } from 'lucide-react';
import * as Tooltip from '@radix-ui/react-tooltip';
import { cn } from '@/lib/cn';

const dockItems = [
  {
    label: 'Ask Agent',
    sublabel: 'AI Assistant',
    href: '/agent',
    Icon: MessageCircle,
  },
  {
    label: 'New Booking',
    sublabel: 'Reserve Space',
    href: '/wizard',
    Icon: PlusCircle,
  },
  {
    label: 'Find a Room',
    sublabel: 'Quick Search',
    href: '/finder',
    Icon: Search,
  },
];

export function FloatingDock() {
  return (
    <Tooltip.Provider delayDuration={200}>
      <div
        className="fixed right-0 top-1/2 z-30 hidden -translate-y-1/2 flex-col overflow-hidden rounded-l-2xl bg-brand-azure-strong shadow-xl md:flex"
        role="complementary"
        aria-label="Quick actions"
      >
        {dockItems.map(({ label, sublabel, href, Icon }, idx) => (
          <Tooltip.Root key={label}>
            <Tooltip.Trigger asChild>
              <Link
                to={href}
                className={cn(
                  'flex flex-col items-center gap-1 px-3 py-4 text-white transition-colors hover:bg-brand-azure focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-white',
                  idx !== dockItems.length - 1 && 'border-b border-white/20',
                )}
                aria-label={`${label}: ${sublabel}`}
              >
                <Icon className="h-5 w-5" aria-hidden="true" />
                <span className="text-center text-[10px] font-semibold leading-tight">{label}</span>
              </Link>
            </Tooltip.Trigger>
            <Tooltip.Portal>
              <Tooltip.Content
                side="left"
                sideOffset={8}
                className="rounded bg-text-heading px-2.5 py-1.5 text-xs text-white shadow-md"
              >
                {sublabel}
                <Tooltip.Arrow className="fill-text-heading" />
              </Tooltip.Content>
            </Tooltip.Portal>
          </Tooltip.Root>
        ))}
      </div>
    </Tooltip.Provider>
  );
}
