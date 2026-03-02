import { clsx } from 'clsx';

interface StatusBadgeProps {
  status: string;
  size?: 'sm' | 'md';
}

const styles: Record<string, string> = {
  PENDING: 'bg-[#D29922]/15 text-[#D29922]',
  SETTLED: 'bg-[#3FB950]/15 text-[#3FB950]',
  BREACHED: 'bg-[#F85149]/15 text-[#F85149]',
  FAILED: 'bg-[#F85149]/15 text-[#F85149]',
  T2: 'bg-[#388BFD]/15 text-[#388BFD]',
  T3: 'bg-[#D29922]/15 text-[#D29922]',
  T5: 'bg-[#F85149]/15 text-[#F85149]',
  PENDING_COMMENTARY: 'bg-[#D29922]/15 text-[#D29922]',
  COMMENTARY_GENERATED: 'bg-[#388BFD]/15 text-[#388BFD]',
  COMMENTARY_APPROVED: 'bg-[#3FB950]/15 text-[#3FB950]',
  AI: 'bg-[#BC8CFF]/15 text-[#BC8CFF]',
  TEMPLATE: 'bg-[#7D8590]/15 text-[#7D8590]',
  CLOSED: 'bg-[#3FB950]/15 text-[#3FB950]',
  OPEN: 'bg-[#F85149]/15 text-[#F85149]',
  HALF_OPEN: 'bg-[#D29922]/15 text-[#D29922]',
};

export function StatusBadge({ status, size = 'md' }: StatusBadgeProps) {
  const normalized = status.toUpperCase();
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-full font-medium',
        size === 'sm' ? 'px-1.5 py-0.5 text-[10px]' : 'px-1.5 py-0.5 text-[11px]',
        styles[normalized] ?? 'bg-[#7D8590]/15 text-[#7D8590]',
      )}
    >
      {normalized}
    </span>
  );
}
