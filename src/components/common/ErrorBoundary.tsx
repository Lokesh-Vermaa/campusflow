import { Component, ErrorInfo, ReactNode } from 'react';
import { AlertCircle, RefreshCw, Home } from 'lucide-react';
import { Button } from '@/components/ui/Button';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  requestId: string;
  errorMessage: string;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      hasError: false,
      requestId: '',
      errorMessage: '',
    };
  }

  static getDerivedStateFromError(error: Error): State {
    const errorWithReq = error as { requestId?: string };
    const requestId = errorWithReq.requestId || `err-${crypto.randomUUID().slice(0, 8)}`;
    return {
      hasError: true,
      requestId,
      errorMessage: error.message || 'An unexpected application error occurred.',
    };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    if (import.meta.env.DEV) {
      console.error('ErrorBoundary caught error:', error, errorInfo);
    }
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div
          role="alert"
          aria-live="assertive"
          className="flex min-h-[60vh] flex-col items-center justify-center p-6 text-center"
        >
          <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-red-50 text-red-600">
            <AlertCircle className="h-8 w-8" aria-hidden="true" />
          </div>

          <h2 className="font-heading text-2xl font-bold text-text-heading">
            Something went wrong
          </h2>

          <p className="mt-2 max-w-md text-sm text-text-body/80">
            We encountered an unexpected error while loading this page. Our technical team has been
            notified.
          </p>

          <div className="mt-4 rounded-md border border-border-color bg-surface-alt px-4 py-2 font-mono text-xs text-text-body/70">
            Reference ID: <span className="font-semibold text-text-heading">{this.state.requestId}</span>
          </div>

          <div className="mt-6 flex flex-wrap gap-3">
            <Button onClick={this.handleReload} variant="default">
              <RefreshCw className="mr-2 h-4 w-4" aria-hidden="true" />
              Try Again
            </Button>
            <Button onClick={() => (window.location.href = '/dashboard')} variant="outline">
              <Home className="mr-2 h-4 w-4" aria-hidden="true" />
              Go to Dashboard
            </Button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
