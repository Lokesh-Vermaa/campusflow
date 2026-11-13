import type { ReactNode } from 'react';
import { SkipLink } from '@/components/ui/SkipLink';

interface PublicLayoutProps {
  children: ReactNode;
}

export function PublicLayout({ children }: PublicLayoutProps) {
  return (
    <div className="flex min-h-screen flex-col bg-surface-alt">
      <SkipLink />
      <main id="main-content" className="flex-1">
        {children}
      </main>
    </div>
  );
}
