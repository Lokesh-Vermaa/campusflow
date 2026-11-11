import { X } from 'lucide-react';

interface AnnouncementStripeProps {
  message: string;
  linkText: string;
  linkHref: string;
  onDismiss?: () => void;
}

export function AnnouncementStripe({
  message,
  linkText,
  linkHref,
  onDismiss,
}: AnnouncementStripeProps) {
  return (
    <div
      role="banner"
      className="flex w-full items-center justify-center gap-3 bg-brand-periwinkle-deep px-4 py-2 text-sm text-white"
    >
      <p className="flex-1 text-center">
        {message}{' '}
        <a
          href={linkHref}
          className="font-bold underline underline-offset-2 hover:no-underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
        >
          {linkText}
        </a>
      </p>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          aria-label="Dismiss announcement"
          className="ml-auto shrink-0 rounded p-0.5 hover:bg-white/20 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
        >
          <X className="h-4 w-4" aria-hidden="true" />
        </button>
      )}
    </div>
  );
}
