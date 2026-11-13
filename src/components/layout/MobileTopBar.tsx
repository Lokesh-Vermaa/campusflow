import { Link } from 'react-router-dom';
import { Menu, Plus } from 'lucide-react';
import { Logo } from '@/components/ui/Logo';
import { ThemeToggle } from '@/components/ui/ThemeToggle';

interface MobileTopBarProps {
  onOpenSidebar: () => void;
}

export function MobileTopBar({ onOpenSidebar }: MobileTopBarProps) {
  return (
    <header className="sticky top-0 z-30 flex h-14 w-full items-center justify-between border-b border-border-color bg-surface/95 px-4 backdrop-blur-sm md:hidden">
      <button
        type="button"
        onClick={onOpenSidebar}
        className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-border-color bg-surface text-text-heading hover:bg-surface-alt focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong"
        aria-label="Open sidebar"
      >
        <Menu className="h-5 w-5" aria-hidden="true" />
      </button>

      <Link to="/" className="inline-flex items-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong rounded-md">
        <Logo />
      </Link>

      <div className="flex items-center gap-2">
        <ThemeToggle />
        <Link
          to="/"
          className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-border-color bg-surface text-brand-azure-strong hover:bg-surface-alt focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong"
          aria-label="New chat"
        >
          <Plus className="h-5 w-5" aria-hidden="true" />
        </Link>
      </div>
    </header>
  );
}

