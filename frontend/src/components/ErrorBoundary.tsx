import { AlertTriangle } from 'lucide-react';
import { Component, type ReactNode } from 'react';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error: unknown) {
    console.error('Error boundary caught render error', error);
  }

  private handleRetry = () => {
    this.setState({ hasError: false });
  };

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="w-full max-w-md rounded-lg border border-danger/30 bg-bg-surface p-6 text-center">
          <div className="mb-3 inline-flex rounded-full bg-danger/10 p-2 text-danger">
            <AlertTriangle className="h-5 w-5" />
          </div>
          <h2 className="mb-2 text-lg font-semibold text-text-primary">Something went wrong</h2>
          <p className="mb-5 text-sm text-text-secondary">
            A rendering error occurred while loading this page.
          </p>
          <button
            type="button"
            onClick={this.handleRetry}
            className="rounded-lg border border-primary/20 bg-primary/10 px-4 py-2 text-sm font-medium text-primary transition-colors hover:bg-primary hover:text-black"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }
}
