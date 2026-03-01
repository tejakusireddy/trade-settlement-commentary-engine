import { clsx } from 'clsx';
import { format } from 'date-fns';
import { AlertTriangle, X } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { breachApi, commentaryApi } from '../../services/api';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { SkeletonLoader } from '../../components/ui/SkeletonLoader';
import type { Breach } from '../../types';

type TypeFilter = 'ALL' | 'T2' | 'T3' | 'T5';
type DateRangeFilter = '7d' | '30d' | '90d';

function toArray<T>(data: T[] | { content?: T[] } | undefined): T[] {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  return data.content ?? [];
}

const reasonLabel: Record<string, string> = {
  MISSING_ASSIGNMENT: 'Missing Assignment',
  FAILED_ALLOCATION: 'Failed Allocation',
  COUNTERPARTY_FAILURE: 'Counterparty Failure',
  INSUFFICIENT_FUNDS: 'Insufficient Funds',
  SYSTEM_ERROR: 'System Error',
};

export default function BreachesPage() {
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('ALL');
  const [dateRange, setDateRange] = useState<DateRangeFilter>('30d');
  const [selectedBreach, setSelectedBreach] = useState<Breach | null>(null);

  const breachesQuery = useQuery({
    queryKey: ['breaches', 'cards'],
    queryFn: breachApi.list,
    refetchInterval: 15_000,
  });

  const commentaryMutation = useMutation({
    mutationFn: (breachId: string) => commentaryApi.getByBreachId(breachId),
  });

  const breaches = toArray(breachesQuery.data?.data);

  const filteredBreaches = useMemo(() => {
    const now = Date.now();
    const windowMs = dateRange === '7d' ? 7 * 86400000 : dateRange === '30d' ? 30 * 86400000 : 90 * 86400000;
    return breaches.filter((b) => {
      const inType = typeFilter === 'ALL' ? true : b.breachType === typeFilter;
      const inRange = now - new Date(b.detectedAt).getTime() <= windowMs;
      return inType && inRange;
    });
  }, [breaches, dateRange, typeFilter]);

  const openCommentaryModal = (breach: Breach) => {
    setSelectedBreach(breach);
    commentaryMutation.mutate(breach.id);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between rounded-xl border border-[#2A2D35] bg-[#111318] p-4">
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2">
            {(['ALL', 'T2', 'T3', 'T5'] as const).map((type) => (
              <button
                key={type}
                type="button"
                className={clsx(
                  'rounded-lg px-3 py-1.5 text-xs font-medium transition-colors',
                  typeFilter === type ? 'bg-primary text-black' : 'bg-[#1A1D24] text-text-secondary hover:text-text-primary',
                )}
                onClick={() => setTypeFilter(type)}
              >
                {type}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2">
            {(['7d', '30d', '90d'] as const).map((range) => (
              <button
                key={range}
                type="button"
                className={clsx(
                  'rounded-lg px-3 py-1.5 text-xs font-medium transition-colors',
                  dateRange === range ? 'bg-primary text-black' : 'bg-[#1A1D24] text-text-secondary hover:text-text-primary',
                )}
                onClick={() => setDateRange(range)}
              >
                {range}
              </button>
            ))}
          </div>
        </div>

        <p className="text-sm text-text-secondary">{filteredBreaches.length} breaches found</p>
      </div>

      {breachesQuery.isLoading ? (
        <div className="space-y-3">
          <SkeletonLoader variant="row" />
          <SkeletonLoader variant="row" />
          <SkeletonLoader variant="row" />
        </div>
      ) : (
        <div className="space-y-3">
          {filteredBreaches.map((breach) => (
            <div
              key={breach.id}
              className="cursor-pointer rounded-xl border border-[#2A2D35] bg-[#111318] p-5 transition-all hover:border-[#3A3D45]"
              onClick={() => setSelectedBreach(breach)}
            >
              <div className="flex items-start gap-4">
                <span
                  className={clsx(
                    'mt-1 block h-16 w-1 rounded',
                    breach.breachType === 'T5' ? 'bg-danger' : breach.breachType === 'T3' ? 'bg-warning' : 'bg-info',
                  )}
                />

                <div className="flex-1 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <p className="font-mono text-sm text-primary">{breach.tradeId}</p>
                      <StatusBadge status={breach.breachType} />
                    </div>
                    <StatusBadge status={breach.status} />
                  </div>

                  <div className="flex items-center gap-3">
                    <p className="font-semibold text-text-primary">{breach.instrument}</p>
                    <p className="text-sm text-text-secondary">{breach.counterparty}</p>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="tabular-nums font-mono text-sm text-danger">
                      {breach.daysOverdue} days overdue
                    </span>
                    <span className="rounded-full border border-[#2A2D35] bg-[#1A1D24] px-2 py-0.5 text-xs text-text-secondary">
                      {reasonLabel[breach.breachReason] ?? breach.breachReason}
                    </span>
                    <span className="text-xs text-text-tertiary">
                      Detected {format(new Date(breach.detectedAt), 'MMM dd, yyyy HH:mm')}
                    </span>
                  </div>
                </div>

                {breach.status === 'PENDING_COMMENTARY' ? (
                  <button
                    type="button"
                    className="rounded-lg border border-ai-accent/20 bg-ai-accent/10 px-3 py-2 text-xs font-medium text-ai-accent transition-colors hover:bg-ai-accent hover:text-black"
                    onClick={(e) => {
                      e.stopPropagation();
                      openCommentaryModal(breach);
                    }}
                  >
                    Generate Commentary
                  </button>
                ) : null}
              </div>
            </div>
          ))}
        </div>
      )}

      {selectedBreach ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
          <div className="w-[600px] rounded-2xl border border-[#2A2D35] bg-[#111318] p-5 shadow-card">
            <div className="mb-4 flex items-start justify-between">
              <div>
                <h3 className="text-base font-semibold text-text-primary">Commentary</h3>
                <p className="text-xs text-text-secondary">
                  {selectedBreach.instrument} · {selectedBreach.counterparty} · {selectedBreach.breachType}
                </p>
              </div>
              <button
                className="rounded-lg p-1 text-text-secondary transition-colors hover:bg-[#1A1D24] hover:text-text-primary"
                type="button"
                onClick={() => setSelectedBreach(null)}
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="min-h-36 rounded-lg border border-[#2A2D35] bg-[#0A0B0D] p-4">
              {commentaryMutation.isPending ? (
                <div className="flex items-center gap-2 text-ai-accent">
                  <span className="h-2 w-2 animate-pulse rounded-full bg-ai-accent" />
                  <span className="text-sm">Generating with Claude AI...</span>
                </div>
              ) : commentaryMutation.data?.data?.content ? (
                <p className="leading-relaxed text-text-primary">{commentaryMutation.data.data.content}</p>
              ) : (
                <div className="flex items-center gap-2 text-text-secondary">
                  <AlertTriangle className="h-4 w-4" />
                  <span className="text-sm">No commentary available yet.</span>
                </div>
              )}
            </div>

            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                className="rounded-lg border border-[#2A2D35] bg-[#1A1D24] px-3 py-2 text-xs text-text-secondary transition-colors hover:text-text-primary"
                onClick={() => setSelectedBreach(null)}
              >
                Close
              </button>
              <button
                type="button"
                className="rounded-lg border border-primary/20 bg-primary/10 px-3 py-2 text-xs font-medium text-primary transition-colors hover:bg-primary hover:text-black"
              >
                Approve
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
