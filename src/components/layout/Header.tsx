import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Bell,
  Search,
  Menu,
  X,
  ChevronDown,
  User as UserIcon,
  LogOut,
  HelpCircle,
  Shield,
} from 'lucide-react';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';
import { Logo } from '@/components/ui/Logo';
import { cn } from '@/lib/cn';
import { NAV_ITEMS } from '@/lib/constants';
import { useAuth } from '@/features/auth/useAuth';

interface HeaderProps {
  variant: 'transparent' | 'solid';
}

export function Header({ variant }: HeaderProps) {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const isTransparent = variant === 'transparent';

  const navTextClass = isTransparent
    ? 'text-white hover:text-white/80'
    : 'text-text-heading hover:text-brand-azure-strong';
  const headerBg = isTransparent ? 'bg-transparent' : 'bg-surface shadow-sm';

  // Filter nav items based on user role
  const navItems = NAV_ITEMS.filter((item) => {
    if (item.label === 'ADMIN') {
      return hasRole('ADMIN');
    }
    return true;
  });

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <>
      <header
        className={cn(
          'sticky top-0 z-40 w-full transition-all duration-300',
          headerBg,
        )}
      >
        {/* Orange left tab */}
        <div className="absolute left-0 top-0 h-full w-1.5 bg-brand-orange" aria-hidden="true" />

        <div className="pl-4">
          {/* Utility row */}
          <div
            className={cn(
              'hidden items-center justify-end gap-2 border-b px-4 py-1 text-sm md:flex',
              isTransparent ? 'border-white/20' : 'border-border-color',
            )}
          >
            <a
              href="/help"
              className={cn('flex items-center gap-1 px-2 py-1 font-medium', navTextClass)}
            >
              <HelpCircle className="h-3.5 w-3.5" aria-hidden="true" />
              Help
            </a>

            <button
              type="button"
              aria-label="Notifications"
              className={cn('rounded p-1.5 focus-visible:outline focus-visible:outline-2', navTextClass)}
            >
              <Bell className="h-4 w-4" aria-hidden="true" />
            </button>

            {/* Profile dropdown */}
            <DropdownMenu.Root>
              <DropdownMenu.Trigger asChild>
                <button
                  type="button"
                  className={cn(
                    'flex items-center gap-1.5 rounded px-2 py-1 font-medium focus-visible:outline focus-visible:outline-2',
                    navTextClass,
                  )}
                  aria-label="Profile menu"
                >
                  <UserIcon className="h-4 w-4" aria-hidden="true" />
                  <span className="hidden lg:inline">{user?.name || 'My Account'}</span>
                  {user && (
                    <span className="rounded bg-brand-azure/20 px-1.5 py-0.2 text-[10px] font-bold text-brand-azure-strong">
                      {user.role}
                    </span>
                  )}
                  <ChevronDown className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
              </DropdownMenu.Trigger>
              <DropdownMenu.Portal>
                <DropdownMenu.Content
                  className="z-50 min-w-[180px] rounded-md border border-border-color bg-surface p-1 shadow-lg"
                  sideOffset={5}
                >
                  {user && (
                    <div className="border-b border-border-color px-3 py-2 text-xs">
                      <p className="font-semibold text-text-heading">{user.name}</p>
                      <p className="truncate text-text-body/60">{user.email}</p>
                    </div>
                  )}
                  {hasRole('ADMIN') && (
                    <DropdownMenu.Item asChild>
                      <Link
                        to="/admin/approvals"
                        className="flex cursor-pointer items-center gap-2 rounded px-3 py-2 text-sm text-text-body outline-none hover:bg-surface-alt focus:bg-surface-alt"
                      >
                        <Shield className="h-4 w-4 text-brand-azure-strong" aria-hidden="true" />
                        Admin Queue
                      </Link>
                    </DropdownMenu.Item>
                  )}
                  <DropdownMenu.Separator className="my-1 h-px bg-border-color" />
                  <DropdownMenu.Item asChild>
                    <button
                      type="button"
                      onClick={handleLogout}
                      className="flex w-full cursor-pointer items-center gap-2 rounded px-3 py-2 text-sm text-red-600 outline-none hover:bg-red-50 focus:bg-red-50"
                    >
                      <LogOut className="h-4 w-4" aria-hidden="true" />
                      Sign Out
                    </button>
                  </DropdownMenu.Item>
                </DropdownMenu.Content>
              </DropdownMenu.Portal>
            </DropdownMenu.Root>

            <button
              type="button"
              aria-label="Search"
              className={cn('rounded p-1.5 focus-visible:outline focus-visible:outline-2', navTextClass)}
            >
              <Search className="h-4 w-4" aria-hidden="true" />
            </button>

            <Link
              to="/wizard"
              className="rounded bg-brand-orange px-3 py-1 text-xs font-bold tracking-wide text-text-heading hover:bg-brand-orange-ink hover:text-white focus-visible:outline focus-visible:outline-2"
            >
              NEW BOOKING
            </Link>
          </div>

          {/* Main nav row */}
          <div className="flex items-center gap-4 px-4 py-3">
            <Link to="/dashboard" aria-label="Go to homepage">
              <Logo variant={isTransparent ? 'light' : 'color'} />
            </Link>

            {/* Desktop nav */}
            <nav aria-label="Main navigation" className="ml-6 hidden items-center gap-1 md:flex">
              {navItems.map((item) => (
                <Link
                  key={item.label}
                  to={item.href}
                  className={cn(
                    'rounded px-2.5 py-1.5 text-xs font-semibold tracking-nav transition-colors focus-visible:outline focus-visible:outline-2',
                    navTextClass,
                  )}
                >
                  {item.label}
                </Link>
              ))}
            </nav>

            {/* Mobile hamburger */}
            <button
              type="button"
              className={cn('ml-auto rounded p-2 md:hidden', navTextClass)}
              aria-label={mobileOpen ? 'Close menu' : 'Open menu'}
              aria-expanded={mobileOpen}
              aria-controls="mobile-nav"
              onClick={() => setMobileOpen((prev) => !prev)}
            >
              {mobileOpen ? (
                <X className="h-5 w-5" aria-hidden="true" />
              ) : (
                <Menu className="h-5 w-5" aria-hidden="true" />
              )}
            </button>
          </div>
        </div>
      </header>

      {/* Mobile slide-in nav */}
      {mobileOpen && (
        <div id="mobile-nav" className="fixed inset-0 z-30 md:hidden">
          <button
            type="button"
            aria-label="Close menu backdrop"
            tabIndex={-1}
            className="fixed inset-0 h-full w-full bg-black/50 border-0 p-0 cursor-default"
            onClick={() => setMobileOpen(false)}
          />
          <nav
            aria-label="Mobile navigation"
            className="relative ml-auto h-full w-72 bg-surface shadow-xl z-10"
          >
            <div className="flex items-center justify-between border-b border-border-color p-4">
              <Logo variant="color" />
              <button
                type="button"
                aria-label="Close menu"
                onClick={() => setMobileOpen(false)}
                className="rounded p-1.5 text-text-body hover:bg-surface-alt"
              >
                <X className="h-5 w-5" aria-hidden="true" />
              </button>
            </div>
            <div className="flex flex-col p-4">
              {NAV_ITEMS.map((item) => (
                <Link
                  key={item.label}
                  to={item.href}
                  className="rounded px-3 py-2.5 text-sm font-semibold tracking-nav text-text-body hover:bg-surface-alt"
                  onClick={() => setMobileOpen(false)}
                >
                  {item.label}
                </Link>
              ))}
              <div className="mt-4 border-t border-border-color pt-4">
                <Link
                  to="/bookings/new"
                  className="block w-full rounded bg-brand-orange px-4 py-2 text-center text-sm font-bold text-text-heading"
                  onClick={() => setMobileOpen(false)}
                >
                  NEW BOOKING
                </Link>
              </div>
            </div>
          </nav>
        </div>
      )}
    </>
  );
}
