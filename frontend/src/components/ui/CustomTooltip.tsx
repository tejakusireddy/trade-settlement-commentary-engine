interface CustomTooltipProps {
  active?: boolean;
  payload?: Array<{ value: number }>;
  label?: string;
}

export function CustomTooltip({ active, payload, label }: CustomTooltipProps) {
  if (active && payload?.length) {
    return (
      <div className="rounded-lg border border-border-subtle bg-bg-raised px-3 py-2 text-xs">
        <p className="text-text-secondary">{label}</p>
        <p className="font-mono font-medium text-danger tabular-nums">
          {payload[0].value} breaches
        </p>
      </div>
    );
  }
  return null;
}
