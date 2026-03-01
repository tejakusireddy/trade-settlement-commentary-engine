import { AlertTriangle } from 'lucide-react';
import { Component, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

export class PageErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error: unknown) {
    console.error('Page render error', error);
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    return (
      <div className="rounded-xl border border-danger/40 bg-[#111318] p-6">
        <div className="mb-3 flex items-center gap-2 text-danger">
          <AlertTriangle className="h-4 w-4" />
          <h2 className="text-sm font-semibold">Page failed to load</h2>
        </div>
        <p className="text-sm leading-relaxed text-text-secondary">
          An unexpected rendering error occurred. Refresh the page or check console logs for details.
        </p>
      </div>
    );
  }
}
