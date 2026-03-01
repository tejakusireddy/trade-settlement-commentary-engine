import { clsx } from 'clsx';

interface StatusBadgeProps {
  status: string;
  size?: 'sm' | 'md';
}

const styles: Record<string, string> = {
  PENDING: 'bg-warning/10 text-warning border border-warning/20',
  SETTLED: 'bg-primary/10 text-primary border border-primary/20',
  BREACHED: 'bg-danger/10 text-danger border border-danger/20',
  FAILED: 'bg-danger/10 text-danger border border-danger/20',
  T2: 'bg-info/10 text-info border border-info/20',
  T3: 'bg-warning/10 text-warning border border-warning/20',
  T5: 'bg-danger/10 text-danger border border-danger/20',
  AI: 'bg-ai-accent/10 text-ai-accent border border-ai-accent/20',
  TEMPLATE: 'bg-[#2A2D35] text-text-secondary border border-[#2A2D35]',
  CLOSED: 'bg-primary/10 text-primary border border-primary/20',
  OPEN: 'bg-danger/10 text-danger border border-danger/20',
  HALF_OPEN: 'bg-warning/10 text-warning border border-warning/20',
};

export function StatusBadge({ status, size = 'md' }: StatusBadgeProps) {
  const normalized = status.toUpperCase();
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-full font-medium',
        size === 'sm' ? 'px-2 py-0.5 text-[10px]' : 'px-2 py-0.5 text-xs',
        styles[normalized] ?? 'bg-[#2A2D35] text-text-secondary border border-[#2A2D35]',
      )}
    >
      {normalized}
    </span>
  );
}
