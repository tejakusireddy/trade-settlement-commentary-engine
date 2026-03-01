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
  primary: { text: 'text-primary', bg: 'bg-primary/10' },
  danger: { text: 'text-danger', bg: 'bg-danger/10' },
  warning: { text: 'text-warning', bg: 'bg-warning/10' },
  ai: { text: 'text-ai-accent', bg: 'bg-ai-accent/10' },
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
    <div className="rounded-xl border border-[#2A2D35] bg-[#111318] p-5 shadow-card transition-colors duration-200 hover:border-[#3A3D45]">
      <div className="mb-4 flex items-start justify-between">
        <p className="text-xs uppercase tracking-wider text-text-secondary">{title}</p>
        <span className={clsx('rounded-lg p-2', palette.bg)}>
          <Icon className={clsx('h-4 w-4', palette.text)} />
        </span>
      </div>

      <p className={clsx('tabular-nums font-mono text-3xl font-bold', palette.text)}>{value}</p>
      {subtitle ? <p className="mt-1 text-xs text-text-secondary">{subtitle}</p> : null}

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
