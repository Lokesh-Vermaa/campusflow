import { useState, useCallback, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { SkipLink } from '@/components/ui/SkipLink';
import { Sidebar, RecentChat } from '@/components/layout/Sidebar';
import { MobileTopBar } from '@/components/layout/MobileTopBar';
import { conversationsApi } from '@/api/conversations';
import { cn } from '@/lib/cn';

interface AppLayoutProps {
  children: ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const handleToggleCollapse = useCallback(() => setIsCollapsed((prev) => !prev), []);
  const handleCloseMobile = useCallback(() => setMobileOpen(false), []);
  const handleOpenMobile = useCallback(() => setMobileOpen(true), []);

  // Fetch conversations for the recent chats list
  const { data: convsData } = useQuery({
    queryKey: ['conversations'],
    queryFn: () => conversationsApi.list(),
    staleTime: 30_000,
  });

  const recentChats: RecentChat[] =
    convsData?.items.map((c) => ({ id: c.id, title: c.title, updatedAt: c.updatedAt })) ?? [];

  // Rename conversation
  const renameMutation = useMutation({
    mutationFn: ({ id, title }: { id: string; title: string }) => conversationsApi.rename(id, title),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['conversations'] }),
  });

  // Delete conversation
  const deleteMutation = useMutation({
    mutationFn: (id: string) => conversationsApi.delete(id),
    onSuccess: (_data, deletedId) => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      // If viewing deleted conversation, go home
      if (window.location.pathname === `/c/${deletedId}`) {
        navigate('/');
      }
    },
  });

  const handleRename = useCallback(
    (id: string, title: string) => renameMutation.mutate({ id, title }),
    [renameMutation],
  );

  const handleDelete = useCallback((id: string) => deleteMutation.mutate(id), [deleteMutation]);

  return (
    <div className="flex min-h-screen bg-surface">
      <SkipLink />

      {/* Left Sidebar (Desktop + Mobile Drawer) */}
      <Sidebar
        isCollapsed={isCollapsed}
        onToggleCollapse={handleToggleCollapse}
        mobileOpen={mobileOpen}
        onCloseMobile={handleCloseMobile}
        recentChats={recentChats}
        onRenameChat={handleRename}
        onDeleteChat={handleDelete}
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

        {/* Main Content — full height for chat pages */}
        <main
          id="main-content"
          className="flex flex-1 flex-col w-full bg-surface"
          style={{ minHeight: 'calc(100vh - 48px)' }}
        >
          {children}
        </main>
      </div>
    </div>
  );
}
