import { useState, useEffect, useRef, useMemo } from 'react';
import { NavLink, Link, useNavigate, useLocation } from 'react-router-dom';
import {
  MessageSquare,
  Search,
  Building2,
  Home,
  Car,
  Calendar,
  LayoutDashboard,
  CheckSquare,
  UploadCloud,
  Database,
  ShieldAlert,
  BarChart3,
  Plus,
  PanelLeftClose,
  PanelLeft,
  MoreVertical,
  Pencil,
  Trash2,
  LogOut,
  HelpCircle,
  X,
} from 'lucide-react';
import { useAuth } from '@/features/auth/useAuth';
import { Logo } from '@/components/ui/Logo';
import { Avatar, AvatarFallback } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Tooltip, TooltipTrigger, TooltipContent, TooltipProvider } from '@/components/ui/Tooltip';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuLabel,
} from '@/components/ui/DropdownMenu';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from '@/components/ui/AlertDialog';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/cn';

export interface RecentChat {
  id: string;
  title: string;
  updatedAt: string;
}

interface SidebarProps {
  isCollapsed: boolean;
  onToggleCollapse: () => void;
  mobileOpen: boolean;
  onCloseMobile: () => void;
  recentChats?: RecentChat[];
  onRenameChat?: (id: string, newTitle: string) => void;
  onDeleteChat?: (id: string) => void;
  pendingApprovalsCount?: number;
}

export function Sidebar({
  isCollapsed,
  onToggleCollapse,
  mobileOpen,
  onCloseMobile,
  recentChats = [],
  onRenameChat,
  onDeleteChat,
  pendingApprovalsCount,
}: SidebarProps) {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [searchQuery, setSearchQuery] = useState('');
  const [renamingChat, setRenamingChat] = useState<RecentChat | null>(null);
  const [newTitle, setNewTitle] = useState('');
  const [deletingChat, setDeletingChat] = useState<RecentChat | null>(null);
  const [helpOpen, setHelpOpen] = useState(false);

  const sidebarRef = useRef<HTMLElement>(null);
  const triggerRef = useRef<HTMLElement | null>(null);
  const prevPathRef = useRef(location.pathname);

  // Close mobile drawer on route change (skip on initial mount)
  useEffect(() => {
    if (location.pathname !== prevPathRef.current) {
      prevPathRef.current = location.pathname;
      onCloseMobile();
    }
  }, [location.pathname, onCloseMobile]);

  // Focus trap and Escape key listener for mobile drawer
  useEffect(() => {
    if (!mobileOpen) return;

    // Save previous active element to restore later
    triggerRef.current = document.activeElement as HTMLElement;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onCloseMobile();
        return;
      }

      if (e.key === 'Tab' && sidebarRef.current) {
        const focusable = sidebarRef.current.querySelectorAll<HTMLElement>(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
        );
        if (focusable.length === 0) return;

        const first = focusable[0];
        const last = focusable[focusable.length - 1];

        if (e.shiftKey && document.activeElement === first) {
          last.focus();
          e.preventDefault();
        } else if (!e.shiftKey && document.activeElement === last) {
          first.focus();
          e.preventDefault();
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    // Initial focus on the close button or first focusable
    const timer = setTimeout(() => {
      const closeBtn = sidebarRef.current?.querySelector<HTMLElement>('button[aria-label="Close sidebar"]');
      closeBtn?.focus();
    }, 50);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      clearTimeout(timer);
      triggerRef.current?.focus();
    };
  }, [mobileOpen, onCloseMobile]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  // Group recent chats
  const filteredChats = useMemo(() => {
    if (!searchQuery.trim()) return recentChats;
    const query = searchQuery.toLowerCase();
    return recentChats.filter((c) => c.title.toLowerCase().includes(query));
  }, [recentChats, searchQuery]);

  const groupedChats = useMemo(() => {
    const today: RecentChat[] = [];
    const yesterday: RecentChat[] = [];
    const prev7Days: RecentChat[] = [];
    const older: RecentChat[] = [];

    const now = new Date();
    const oneDay = 24 * 60 * 60 * 1000;

    for (const chat of filteredChats) {
      const date = new Date(chat.updatedAt);
      const diffDays = Math.floor((now.getTime() - date.getTime()) / oneDay);

      if (diffDays === 0) {
        today.push(chat);
      } else if (diffDays === 1) {
        yesterday.push(chat);
      } else if (diffDays <= 7) {
        prev7Days.push(chat);
      } else {
        older.push(chat);
      }
    }

    return { today, yesterday, prev7Days, older };
  }, [filteredChats]);

  // Initials for user avatar
  const initials = useMemo(() => {
    if (!user?.name) return 'U';
    const parts = user.name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }, [user?.name]);

  const sidebarContent = (
    <div className="flex h-full flex-col justify-between overflow-hidden bg-surface-alt text-text-body">
      {/* Top Section */}
      <div className="flex flex-1 flex-col overflow-y-auto">
        {/* A. Brand Row */}
        <div className="flex h-14 shrink-0 items-center justify-between border-b border-border-color px-3">
          <Link
            to="/"
            className={cn(
              'inline-flex items-center gap-2 overflow-hidden rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong',
              isCollapsed && 'justify-center w-full',
            )}
            aria-label="JKLU CampusFlow Home"
          >
            {isCollapsed ? (
              <span className="font-heading text-lg font-bold text-brand-azure-strong">
                J<span className="text-brand-orange">.</span>
              </span>
            ) : (
              <Logo />
            )}
          </Link>

          {/* Desktop collapse toggle */}
          <button
            type="button"
            onClick={onToggleCollapse}
            className="hidden h-8 w-8 items-center justify-center rounded-md text-text-body/70 hover:bg-black/5 hover:text-text-heading focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong md:inline-flex"
            aria-label={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {isCollapsed ? (
              <PanelLeft className="h-4 w-4" aria-hidden="true" />
            ) : (
              <PanelLeftClose className="h-4 w-4" aria-hidden="true" />
            )}
          </button>

          {/* Mobile close button */}
          <button
            type="button"
            onClick={onCloseMobile}
            className="inline-flex h-8 w-8 items-center justify-center rounded-md text-text-body/70 hover:bg-black/5 hover:text-text-heading focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong md:hidden"
            aria-label="Close sidebar"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        {/* B. New Chat & Search */}
        <div className="p-3 space-y-2">
          {isCollapsed ? (
            <Tooltip delayDuration={200}>
              <TooltipTrigger asChild>
                <Link
                  to="/"
                  className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-azure-strong text-white shadow-xs transition hover:bg-brand-azure-strong/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong focus-visible:ring-offset-2"
                  aria-label="New chat"
                >
                  <Plus className="h-4 w-4" aria-hidden="true" />
                </Link>
              </TooltipTrigger>
              <TooltipContent side="right">New chat</TooltipContent>
            </Tooltip>
          ) : (
            <>
              <Link
                to="/"
                className="flex h-9 w-full items-center justify-center gap-2 rounded-lg bg-brand-azure-strong px-3 text-xs font-semibold text-white shadow-xs transition hover:bg-brand-azure-strong/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong focus-visible:ring-offset-2"
              >
                <Plus className="h-4 w-4" aria-hidden="true" />
                <span>New chat</span>
              </Link>

              <div className="relative">
                <Search className="pointer-events-none absolute left-2.5 top-2.5 h-3.5 w-3.5 text-text-body/50" aria-hidden="true" />
                <input
                  type="text"
                  placeholder="Search chats..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="h-8 w-full rounded-md border border-border-color bg-surface pl-8 pr-2.5 text-xs text-text-heading placeholder:text-text-body/40 focus:border-brand-azure-strong focus:outline-none focus:ring-1 focus:ring-brand-azure-strong"
                  aria-label="Search chats"
                />
              </div>
            </>
          )}
        </div>

        {/* C. Workspace Nav Group */}
        <nav className="px-2 py-1" aria-label="Workspace">
          {!isCollapsed && (
            <div className="px-2 pb-1 text-[11px] font-semibold uppercase tracking-wider text-text-body/50">
              Workspace
            </div>
          )}

          <div className="space-y-0.5">
            {[
              { label: 'Chat', href: '/', icon: MessageSquare },
              { label: 'Find a Room', href: '/rooms', icon: Search },
              { label: 'Venues & Labs', href: '/venues', icon: Building2 },
              ...(hasRole(['STAFF', 'ADMIN'])
                ? [{ label: 'Guest House', href: '/guest-house', icon: Home }]
                : []),
              { label: 'Parking', href: '/parking', icon: Car },
              { label: 'My Bookings', href: '/bookings', icon: Calendar },
              { label: 'Overview', href: '/overview', icon: LayoutDashboard },
            ].map((item) => {
              const Icon = item.icon;
              const linkNode = (
                <NavLink
                  key={item.href}
                  to={item.href}
                  end={item.href === '/'}
                  className={({ isActive }) =>
                    cn(
                      'group flex items-center rounded-md px-2.5 py-2 text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong',
                      isCollapsed ? 'justify-center' : 'gap-2.5',
                      isActive
                        ? 'bg-brand-azure-strong/10 text-brand-azure-strong font-semibold'
                        : 'text-text-body/80 hover:bg-black/5 hover:text-text-heading',
                    )
                  }
                >
                  <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />
                  {!isCollapsed && <span>{item.label}</span>}
                </NavLink>
              );

              if (isCollapsed) {
                return (
                  <Tooltip key={item.href} delayDuration={200}>
                    <TooltipTrigger asChild>{linkNode}</TooltipTrigger>
                    <TooltipContent side="right">{item.label}</TooltipContent>
                  </Tooltip>
                );
              }
              return linkNode;
            })}
          </div>
        </nav>

        {/* D. Admin Group (Hidden for non-admin) */}
        {hasRole('ADMIN') && (
          <nav className="mt-3 px-2 py-1 border-t border-border-color/60 pt-2" aria-label="Admin">
            {!isCollapsed && (
              <div className="flex items-center justify-between px-2 pb-1 text-[11px] font-semibold uppercase tracking-wider text-text-body/50">
                <span>Admin</span>
              </div>
            )}

            <div className="space-y-0.5">
              {[
                {
                  label: 'Approvals',
                  href: '/admin/approvals',
                  icon: CheckSquare,
                  badgeCount:
                    pendingApprovalsCount !== undefined && pendingApprovalsCount > 0
                      ? pendingApprovalsCount
                      : undefined,
                },
                { label: 'Timetable Upload', href: '/admin/timetable', icon: UploadCloud },
                { label: 'Master Data', href: '/admin/master-data', icon: Database },
                { label: 'Audit Log', href: '/admin/audit', icon: ShieldAlert },
                { label: 'Reports', href: '/admin/reports', icon: BarChart3 },
              ].map((item) => {
                const Icon = item.icon;
                const linkNode = (
                  <NavLink
                    key={item.href}
                    to={item.href}
                    className={({ isActive }) =>
                      cn(
                        'group flex items-center rounded-md px-2.5 py-2 text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong',
                        isCollapsed ? 'justify-center' : 'justify-between',
                        isActive
                          ? 'bg-brand-azure-strong/10 text-brand-azure-strong font-semibold'
                          : 'text-text-body/80 hover:bg-black/5 hover:text-text-heading',
                      )
                    }
                  >
                    <div className="flex items-center gap-2.5">
                      <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />
                      {!isCollapsed && <span>{item.label}</span>}
                    </div>

                    {!isCollapsed && item.badgeCount !== undefined && (
                      <span className="inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-amber-500/20 px-1.5 text-[10px] font-bold text-amber-800">
                        {item.badgeCount}
                      </span>
                    )}
                  </NavLink>
                );

                if (isCollapsed) {
                  return (
                    <Tooltip key={item.href} delayDuration={200}>
                      <TooltipTrigger asChild>{linkNode}</TooltipTrigger>
                      <TooltipContent side="right">
                        {item.label} {item.badgeCount ? `(${item.badgeCount})` : ''}
                      </TooltipContent>
                    </Tooltip>
                  );
                }
                return linkNode;
              })}
            </div>
          </nav>
        )}

        {/* E. Recent Chats List */}
        {!isCollapsed && (
          <div className="mt-3 flex-1 px-2 border-t border-border-color/60 pt-2">
            <div className="px-2 pb-1.5 text-[11px] font-semibold uppercase tracking-wider text-text-body/50">
              Recent Chats
            </div>

            {filteredChats.length === 0 ? (
              <div className="px-2 py-4 text-center text-xs text-text-body/50">
                {searchQuery ? 'No matching chats' : 'No recent chats'}
              </div>
            ) : (
              <div className="space-y-3">
                {[
                  { label: 'Today', items: groupedChats.today },
                  { label: 'Yesterday', items: groupedChats.yesterday },
                  { label: 'Previous 7 days', items: groupedChats.prev7Days },
                  { label: 'Older', items: groupedChats.older },
                ]
                  .filter((group) => group.items.length > 0)
                  .map((group) => (
                    <div key={group.label} className="space-y-0.5">
                      <div className="px-2 text-[10px] font-medium text-text-body/40">
                        {group.label}
                      </div>
                      {group.items.map((chat) => (
                        <div
                          key={chat.id}
                          className="group relative flex items-center justify-between rounded-md px-2 py-1.5 text-xs text-text-body/80 hover:bg-black/5 hover:text-text-heading"
                        >
                          <NavLink
                            to={`/c/${chat.id}`}
                            className={({ isActive }) =>
                              cn(
                                'flex-1 truncate pr-6 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand-azure-strong rounded-xs',
                                isActive && 'font-semibold text-brand-azure-strong',
                              )
                            }
                          >
                            {chat.title}
                          </NavLink>

                          {/* Hover menu for Rename / Delete */}
                          <div className="absolute right-1 opacity-0 group-hover:opacity-100 transition-opacity">
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <button
                                  type="button"
                                  className="flex h-6 w-6 items-center justify-center rounded hover:bg-black/10 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brand-azure-strong"
                                  aria-label={`Options for ${chat.title}`}
                                >
                                  <MoreVertical className="h-3.5 w-3.5 text-text-body/60" aria-hidden="true" />
                                </button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="w-36">
                                <DropdownMenuItem
                                  onClick={() => {
                                    setRenamingChat(chat);
                                    setNewTitle(chat.title);
                                  }}
                                >
                                  <Pencil className="mr-2 h-3.5 w-3.5" aria-hidden="true" />
                                  <span>Rename</span>
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem
                                  onClick={() => setDeletingChat(chat)}
                                  className="text-red-600 focus:text-red-700 focus:bg-red-50"
                                >
                                  <Trash2 className="mr-2 h-3.5 w-3.5" aria-hidden="true" />
                                  <span>Delete</span>
                                </DropdownMenuItem>
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </div>
                        </div>
                      ))}
                    </div>
                  ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* F. Footer: User profile, role badge & menu */}
      <div className="shrink-0 border-t border-border-color p-2">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              className={cn(
                'flex w-full items-center rounded-lg p-1.5 transition-colors hover:bg-black/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong',
                isCollapsed ? 'justify-center' : 'justify-between',
              )}
              aria-label="User account menu"
            >
              <div className="flex items-center gap-2.5 overflow-hidden">
                <Avatar className="h-8 w-8 shrink-0">
                  <AvatarFallback className="bg-brand-azure-strong text-xs font-bold text-white">
                    {initials}
                  </AvatarFallback>
                </Avatar>

                {!isCollapsed && (
                  <div className="flex flex-col text-left truncate">
                    <span className="truncate text-xs font-semibold text-text-heading">
                      {user?.name || 'JKLU User'}
                    </span>
                    <span className="truncate text-[10px] text-text-body/60">
                      {user?.email || 'user@demo.jklu.test'}
                    </span>
                  </div>
                )}
              </div>

              {!isCollapsed && (
                <Badge
                  variant="outline"
                  className={cn(
                    'shrink-0 text-[10px] font-bold',
                    user?.role === 'ADMIN'
                      ? 'border-brand-orange/40 bg-brand-orange/20 text-brand-orange-ink'
                      : user?.role === 'STAFF'
                        ? 'border-brand-azure-strong/30 bg-brand-azure-strong/10 text-brand-azure-strong'
                        : 'border-brand-periwinkle-deep/30 bg-brand-periwinkle-deep/10 text-brand-periwinkle-deep',
                  )}
                >
                  {user?.role || 'STUDENT'}
                </Badge>
              )}
            </button>
          </DropdownMenuTrigger>

          <DropdownMenuContent align={isCollapsed ? 'center' : 'start'} side="top" className="w-56 mb-1">
            <DropdownMenuLabel className="font-normal">
              <div className="flex flex-col space-y-1">
                <p className="text-xs font-medium text-text-heading">{user?.name}</p>
                <p className="text-[11px] text-text-body/60 truncate">{user?.email}</p>
                <p className="text-[10px] text-text-body/50">Dept: {user?.department || 'General'}</p>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => setHelpOpen(true)}>
              <HelpCircle className="mr-2 h-4 w-4 text-text-body/70" aria-hidden="true" />
              <span>Help & Guidelines</span>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={handleLogout} className="text-red-600 focus:text-red-700 focus:bg-red-50">
              <LogOut className="mr-2 h-4 w-4" aria-hidden="true" />
              <span>Sign out</span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {/* Rename Chat Dialog */}
      <Dialog open={Boolean(renamingChat)} onOpenChange={(open) => !open && setRenamingChat(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Rename Chat</DialogTitle>
          </DialogHeader>
          <div className="py-2">
            <Input
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              placeholder="Enter new conversation title"
              maxLength={100}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && newTitle.trim() && renamingChat) {
                  onRenameChat?.(renamingChat.id, newTitle.trim());
                  setRenamingChat(null);
                }
              }}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" size="sm" onClick={() => setRenamingChat(null)}>
              Cancel
            </Button>
            <Button
              variant="default"
              size="sm"
              disabled={!newTitle.trim()}
              onClick={() => {
                if (renamingChat && newTitle.trim()) {
                  onRenameChat?.(renamingChat.id, newTitle.trim());
                  setRenamingChat(null);
                }
              }}
            >
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Chat Confirmation Alert */}
      <AlertDialog open={Boolean(deletingChat)} onOpenChange={(open) => !open && setDeletingChat(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete conversation?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently remove &ldquo;{deletingChat?.title}&rdquo;. This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setDeletingChat(null)}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="bg-red-600 hover:bg-red-700 text-white"
              onClick={() => {
                if (deletingChat) {
                  onDeleteChat?.(deletingChat.id);
                  setDeletingChat(null);
                }
              }}
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Help Dialog */}
      <Dialog open={helpOpen} onOpenChange={setHelpOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>JKLU CampusFlow Help</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 text-xs text-text-body/80 py-2">
            <p>
              <strong>CampusFlow</strong> is an AI-agent-powered venue, guest house, and parking coordination system built for the JKLU community.
            </p>
            <div className="space-y-1.5 rounded-md border border-border-color bg-surface-alt p-3">
              <p className="font-semibold text-text-heading">Role Guidelines:</p>
              <ul className="list-disc pl-4 space-y-1">
                <li><strong>STUDENT:</strong> Search classrooms with timetable heatmaps, request event spaces, chat with AI Agent.</li>
                <li><strong>STAFF:</strong> Student abilities + reserve Amphitheatres, academic labs, and MDC Guest House rooms.</li>
                <li><strong>ADMIN:</strong> Review approval queue, upload semester timetables, manage master data & audit logs.</li>
              </ul>
            </div>
            <p>
              Every booking requires admin review before final reservation. Deterministic schedule clashing is evaluated in real time.
            </p>
          </div>
          <DialogFooter>
            <Button variant="default" size="sm" onClick={() => setHelpOpen(false)}>
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );

  return (
    <TooltipProvider>
      {/* Desktop Persistent Sidebar */}
      <aside
        ref={sidebarRef}
        aria-label="Sidebar navigation"
        className={cn(
          'fixed inset-y-0 left-0 z-20 hidden border-r border-border-color bg-surface-alt transition-all duration-200 md:block',
          isCollapsed ? 'w-[68px]' : 'w-[260px]',
        )}
      >
        {sidebarContent}
      </aside>

      {/* Mobile Off-Canvas Drawer with Focus Trap & Backdrop */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 md:hidden" role="dialog" aria-modal="true" aria-label="Navigation drawer">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-black/40 backdrop-blur-xs transition-opacity"
            onClick={onCloseMobile}
            aria-hidden="true"
          />

          {/* Drawer content */}
          <aside
            ref={sidebarRef}
            aria-label="Mobile navigation drawer"
            className="fixed inset-y-0 left-0 z-50 w-72 max-w-[85vw] border-r border-border-color bg-surface-alt shadow-xl animate-in slide-in-from-left duration-200"
          >
            {sidebarContent}
          </aside>
        </div>
      )}
    </TooltipProvider>
  );
}
