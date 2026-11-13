import type { ReactNode } from 'react';
import { SkipLink } from '@/components/ui/SkipLink';

interface AppLayoutProps {
  children: ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  return (
    <div className="flex min-h-screen bg-surface">
      <SkipLink />
      <main id="main-content" className="flex-1 w-full">
        {children}
      </main>
    </div>
  );
}
