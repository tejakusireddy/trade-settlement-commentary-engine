import { clsx } from 'clsx';

type Variant = 'text' | 'card' | 'row';

interface SkeletonLoaderProps {
  variant?: Variant;
  className?: string;
}

export function SkeletonLoader({ variant = 'text', className }: SkeletonLoaderProps) {
  return (
    <div
      className={clsx(
        'animate-pulse rounded bg-bg-raised',
        variant === 'text' && 'h-4 w-full',
        variant === 'card' && 'h-32 w-full',
        variant === 'row' && 'h-12 w-full',
        className,
      )}
    />
  );
}
