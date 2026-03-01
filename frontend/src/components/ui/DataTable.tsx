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
      <div className="rounded-xl border border-[#2A2D35] bg-[#111318] p-4">
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
      <div className="rounded-xl border border-[#2A2D35] bg-[#111318] p-10 text-center">
        <Database className="mx-auto mb-2 h-5 w-5 text-text-tertiary" />
        <p className="text-sm text-text-secondary">{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div className="w-full overflow-hidden rounded-xl border border-[#2A2D35] bg-[#111318]">
      <div className="grid border-b border-[#2A2D35] bg-[#0A0B0D] px-4 py-3">
        <div
          className="grid gap-3"
          style={{ gridTemplateColumns: `repeat(${columns.length}, minmax(0, 1fr))` }}
        >
          {columns.map((column, index) => (
            <div key={`${column.header}-${index}`} className="text-xs uppercase tracking-wider text-text-secondary">
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
              'border-b border-[#1A1D24] px-4 py-3 transition-colors last:border-0',
              onRowClick ? 'cursor-pointer hover:bg-[#1A1D24]' : '',
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
                  className={clsx('text-sm text-text-primary', column.className)}
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
