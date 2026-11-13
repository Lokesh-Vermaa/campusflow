import { useState, useCallback, type ReactNode } from 'react';
import { SkipLink } from '@/components/ui/SkipLink';
import { Sidebar } from '@/components/layout/Sidebar';
import { MobileTopBar } from '@/components/layout/MobileTopBar';
import { cn } from '@/lib/cn';

interface AppLayoutProps {
  children: ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleToggleCollapse = useCallback(() => {
    setIsCollapsed((prev) => !prev);
  }, []);

  const handleCloseMobile = useCallback(() => {
    setMobileOpen(false);
  }, []);

  const handleOpenMobile = useCallback(() => {
    setMobileOpen(true);
  }, []);

  return (
    <div className="flex min-h-screen bg-surface">
      <SkipLink />

      {/* Left Sidebar (Desktop + Mobile Drawer) */}
      <Sidebar
        isCollapsed={isCollapsed}
        onToggleCollapse={handleToggleCollapse}
        mobileOpen={mobileOpen}
        onCloseMobile={handleCloseMobile}
      />

      {/* Main Content Area */}
      <div
        className={cn(
          'flex flex-1 flex-col transition-all duration-200 min-w-0',
          isCollapsed ? 'md:ml-[68px]' : 'md:ml-[260px]',
        )}
      >
        {/* Mobile Slim Top Bar */}
        <MobileTopBar onOpenSidebar={handleOpenMobile} />

        {/* Main Content */}
        <main id="main-content" className="flex-1 w-full bg-surface">
          {children}
        </main>
      </div>
    </div>
  );
}
