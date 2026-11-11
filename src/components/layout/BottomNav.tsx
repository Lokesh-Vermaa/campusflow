import { Link, useLocation } from 'react-router-dom';
import { Home, Search, Calendar, User, MessageCircle } from 'lucide-react';
import { cn } from '@/lib/cn';

const navItems = [
  { label: 'Home', href: '/dashboard', Icon: Home },
  { label: 'Find Room', href: '/find-room', Icon: Search },
  { label: 'Bookings', href: '/my-bookings', Icon: Calendar },
  { label: 'Profile', href: '/profile', Icon: User },
];

export function BottomNav() {
  const { pathname } = useLocation();

  return (
    <nav
      aria-label="Mobile bottom navigation"
      className="fixed bottom-0 left-0 right-0 z-30 flex items-end border-t border-border-color bg-surface pb-safe md:hidden"
    >
      {/* Left nav items */}
      {navItems.slice(0, 2).map(({ label, href, Icon }) => {
        const active = pathname.startsWith(href);
        return (
          <Link
            key={href}
            to={href}
            aria-current={active ? 'page' : undefined}
            className={cn(
              'flex flex-1 flex-col items-center gap-0.5 py-2 text-xs font-medium transition-colors',
              active ? 'text-brand-azure-strong' : 'text-text-body hover:text-brand-azure-strong',
            )}
          >
            <Icon className="h-5 w-5" aria-hidden="true" />
            <span>{label}</span>
          </Link>
        );
      })}

      {/* Centre floating agent button */}
      <div className="relative -top-5 flex flex-col items-center">
        <Link
          to="/agent"
          aria-label="Open AI Agent"
          className="flex h-14 w-14 items-center justify-center rounded-full bg-brand-azure-strong text-white shadow-lg hover:bg-brand-azure focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-azure-strong"
        >
          <MessageCircle className="h-6 w-6" aria-hidden="true" />
        </Link>
        <span className="mt-1 text-xs font-medium text-text-body">Agent</span>
      </div>

      {/* Right nav items */}
      {navItems.slice(2).map(({ label, href, Icon }) => {
        const active = pathname.startsWith(href);
        return (
          <Link
            key={href}
            to={href}
            aria-current={active ? 'page' : undefined}
            className={cn(
              'flex flex-1 flex-col items-center gap-0.5 py-2 text-xs font-medium transition-colors',
              active ? 'text-brand-azure-strong' : 'text-text-body hover:text-brand-azure-strong',
            )}
          >
            <Icon className="h-5 w-5" aria-hidden="true" />
            <span>{label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
