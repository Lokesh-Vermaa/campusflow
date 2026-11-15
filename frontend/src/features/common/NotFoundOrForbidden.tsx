import { Link } from 'react-router-dom';
import { ShieldAlert, ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export function NotFoundOrForbidden() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center px-4 py-16 text-center">
      <div className="mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-surface-alt text-brand-azure-strong">
        <ShieldAlert className="h-10 w-10 text-brand-azure-strong" aria-hidden="true" />
      </div>

      <h1 className="font-heading text-3xl font-bold tracking-tight text-text-heading sm:text-4xl">
        Page Not Found or Access Restricted
      </h1>

      <p className="mt-4 max-w-md text-base text-text-body/80">
        The requested resource does not exist, has been moved, or your account does not possess the
        required administrative credentials to access it.
      </p>

      <div className="mt-8 flex flex-wrap justify-center gap-4">
        <Button asChild variant="default">
          <Link to="/">
            <ArrowLeft className="mr-2 h-4 w-4" aria-hidden="true" />
            Return to Chat
          </Link>
        </Button>
        <Button asChild variant="outline">
          <Link to="/rooms">Find an Available Room</Link>
        </Button>
      </div>
    </div>
  );
}

