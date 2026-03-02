import { clsx } from 'clsx';
import { Database } from 'lucide-react';
import type { ReactNode } from 'react';
import { SkeletonLoader } from './SkeletonLoader';

export interface DataTableColumn<T> {
  header: string;
  accessor: keyof T;
  render?: (item: T) => ReactNode;
  className?: string;
}

interface DataTableProps<T> {
  columns: DataTableColumn<T>[];
  data: T[];
  loading?: boolean;
  onRowClick?: (row: T) => void;
  emptyMessage?: string;
}

export function DataTable<T>({
  columns,
  data,
  loading,
  onRowClick,
  emptyMessage = 'No data available',
}: DataTableProps<T>) {
  if (loading) {
    return (
      <div className="rounded-lg border border-border-subtle bg-bg-surface p-4">
        <div className="space-y-3">
          <SkeletonLoader variant="row" />
          <SkeletonLoader variant="row" />
          <SkeletonLoader variant="row" />
        </div>
      </div>
    );
  }

  if (!data.length) {
    return (
      <div className="rounded-lg border border-border-subtle bg-bg-surface p-10 text-center">
        <Database className="mx-auto mb-2 h-5 w-5 text-text-tertiary" />
        <p className="text-sm text-text-secondary">{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div className="w-full overflow-hidden rounded-lg border border-border-subtle bg-bg-surface">
      <div className="grid border-b border-border-subtle px-4 py-3">
        <div
          className="grid gap-3"
          style={{ gridTemplateColumns: `repeat(${columns.length}, minmax(0, 1fr))` }}
        >
          {columns.map((column, index) => (
            <div key={`${column.header}-${index}`} className="text-[11px] uppercase tracking-[0.12em] text-text-secondary">
              {column.header}
            </div>
          ))}
        </div>
      </div>

      <div>
        {data.map((row, idx) => (
          <div
            key={idx}
            className={clsx(
              'min-h-10 border-b border-border-subtle px-4 py-2.5 transition-colors last:border-0',
              onRowClick ? 'cursor-pointer hover:bg-bg-raised' : '',
            )}
            onClick={() => onRowClick?.(row)}
          >
            <div
              className="grid gap-3"
              style={{ gridTemplateColumns: `repeat(${columns.length}, minmax(0, 1fr))` }}
            >
              {columns.map((column, colIdx) => (
                <div
                  key={`${String(column.accessor)}-${colIdx}`}
                  className={clsx('text-[13px] text-text-primary', column.className)}
                >
                  {column.render
                    ? column.render(row)
                    : String((row as Record<string, unknown>)[String(column.accessor)] ?? '')}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
