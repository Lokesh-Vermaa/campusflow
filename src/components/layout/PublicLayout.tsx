import type { ReactNode } from 'react';
import { SkipLink } from '@/components/ui/SkipLink';
import { ThemeToggle } from '@/components/ui/ThemeToggle';

interface PublicLayoutProps {
  children: ReactNode;
}

export function PublicLayout({ children }: PublicLayoutProps) {
  return (
    <div className="relative flex min-h-screen flex-col bg-surface-alt">
      <SkipLink />
      {/* Top right theme toggle */}
      <div className="absolute top-4 right-4 z-20">
        <ThemeToggle />
      </div>
      <main id="main-content" className="flex-1">
        {children}
      </main>
    </div>
  );
}
