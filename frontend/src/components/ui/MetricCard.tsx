import { clsx } from 'clsx';
import { TrendingDown, TrendingUp, type LucideIcon } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  trend?: number;
  icon: LucideIcon;
  color?: 'primary' | 'danger' | 'warning' | 'ai';
}

const colorMap = {
  primary: { line: 'border-t-info/30', value: 'text-text-primary' },
  danger: { line: 'border-t-danger/30', value: 'text-text-primary' },
  warning: { line: 'border-t-warning/30', value: 'text-text-primary' },
  ai: { line: 'border-t-ai-accent/30', value: 'text-text-primary' },
} as const;

export function MetricCard({
  title,
  value,
  subtitle,
  trend,
  icon: Icon,
  color = 'primary',
}: MetricCardProps) {
  const palette = colorMap[color];
  return (
    <div className={clsx('rounded-lg border border-border-subtle border-t-2 bg-bg-surface p-5', palette.line)}>
      <div className="mb-4 flex items-start justify-between">
        <p className="text-[11px] uppercase tracking-[0.12em] text-text-secondary">{title}</p>
        <Icon className="h-4 w-4 text-text-tertiary" />
      </div>

      <p className={clsx('tabular-nums font-mono text-[28px] font-medium', palette.value)}>{value}</p>
      {subtitle ? <p className="mt-1 text-[11px] text-text-secondary">{subtitle}</p> : null}

      {typeof trend === 'number' ? (
        <div
          className={clsx(
            'mt-3 inline-flex items-center gap-1 text-xs',
            trend >= 0 ? 'text-primary' : 'text-danger',
          )}
        >
          {trend >= 0 ? <TrendingUp className="h-3 w-3" /> : <TrendingDown className="h-3 w-3" />}
          <span className="tabular-nums font-mono">{Math.abs(trend).toFixed(1)}%</span>
        </div>
      ) : null}
    </div>
  );
}
