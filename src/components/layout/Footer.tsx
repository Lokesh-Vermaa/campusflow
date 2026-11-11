import { Link } from 'react-router-dom';
import { Logo } from '@/components/ui/Logo';

const campusLinks = [
  { label: 'Academic Calendar', href: '/calendar' },
  { label: 'Campus Map', href: '/map' },
  { label: 'Venue Directory', href: '/venues' },
  { label: 'Parking Info', href: '/parking' },
  { label: 'Guest House', href: '/guest-house' },
];

const helpLinks = [
  { label: 'How to Book', href: '/help/booking' },
  { label: 'Cancellation Policy', href: '/help/cancellation' },
  { label: 'FAQs', href: '/help/faqs' },
];

export function Footer() {
  return (
    <footer className="border-t border-border-color bg-surface-alt py-10">
      <div className="mx-auto max-w-6xl px-4">
        <div className="grid grid-cols-1 gap-8 md:grid-cols-3">
          {/* Brand column */}
          <div>
            <Logo variant="color" />
            <p className="mt-3 text-sm text-text-body">
              Campus venue, guest-house and parking management system for JKLU.
            </p>
          </div>

          {/* Campus links */}
          <div>
            <h3 className="mb-3 text-sm font-semibold uppercase tracking-nav text-text-heading">
              Campus
            </h3>
            <ul className="space-y-2">
              {campusLinks.map((link) => (
                <li key={link.href}>
                  <Link
                    to={link.href}
                    className="text-sm text-text-body hover:text-brand-azure-strong hover:underline"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Help & Contact */}
          <div>
            <h3 className="mb-3 text-sm font-semibold uppercase tracking-nav text-text-heading">
              Help &amp; Contact
            </h3>
            <ul className="space-y-2">
              {helpLinks.map((link) => (
                <li key={link.href}>
                  <Link
                    to={link.href}
                    className="text-sm text-text-body hover:text-brand-azure-strong hover:underline"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
              <li>
                <a
                  href="mailto:campusflow@jklu.edu.in"
                  rel="noopener noreferrer"
                  className="text-sm text-brand-azure-strong hover:underline"
                >
                  campusflow@jklu.edu.in
                </a>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-8 border-t border-border-color pt-6 text-center text-xs text-text-body">
          <p>&copy; {new Date().getFullYear()} JKLU CampusFlow. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
}
