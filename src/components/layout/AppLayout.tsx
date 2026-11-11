import { useState, type ReactNode } from 'react';
import { SkipLink } from '@/components/ui/SkipLink';
import { AnnouncementStripe } from '@/components/layout/AnnouncementStripe';
import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';
import { FloatingDock } from '@/components/layout/FloatingDock';
import { BottomNav } from '@/components/layout/BottomNav';
import { DEFAULT_ANNOUNCEMENT } from '@/lib/constants';

interface AppLayoutProps {
  children: ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const [announcementVisible, setAnnouncementVisible] = useState(true);

  return (
    <div className="flex min-h-screen flex-col">
      <SkipLink />
      {announcementVisible && (
        <AnnouncementStripe
          message={DEFAULT_ANNOUNCEMENT.message}
          linkText={DEFAULT_ANNOUNCEMENT.linkText}
          linkHref={DEFAULT_ANNOUNCEMENT.linkHref}
          onDismiss={() => setAnnouncementVisible(false)}
        />
      )}
      <Header variant="solid" />
      <main id="main-content" className="flex-1 pb-20 md:pb-0">
        {children}
      </main>
      <Footer />
      <FloatingDock />
      <BottomNav />
    </div>
  );
}
