import { clsx } from 'clsx';
import { format, parseISO, subDays } from 'date-fns';
import { AlertTriangle, X } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { breachApi, commentaryApi } from '../../services/api';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { SkeletonLoader } from '../../components/ui/SkeletonLoader';
import { useAuth } from '../../auth/AuthContext';

type TypeFilter = 'ALL' | 'T2' | 'T3' | 'T5';
type DateRangeFilter = '7d' | '30d' | '90d';

const reasonLabel: Record<string, string> = {
  MISSING_ASSIGNMENT: 'Missing Assignment',
  FAILED_ALLOCATION: 'Failed Allocation',
  COUNTERPARTY_FAILURE: 'Counterparty Failure',
  INSUFFICIENT_FUNDS: 'Insufficient Funds',
  SYSTEM_ERROR: 'System Error',
};

export default function BreachesPage() {
  const queryClient = useQueryClient();
  const { canApprove, username, sessionKey } = useAuth();
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('ALL');
  const [dateRange, setDateRange] = useState<DateRangeFilter>('30d');
  const [selectedBreachId, setSelectedBreachId] = useState<string | null>(null);

  const { data: breaches = [], isLoading, isError } = useQuery({
    queryKey: ['breaches', sessionKey],
    queryFn: () => breachApi.list(),
    refetchInterval: 15_000,
  });

  const selectedBreach = useMemo(
    () => breaches.find((breach) => breach.id === selectedBreachId) ?? null,
    [breaches, selectedBreachId],
  );

  const { data: commentary, isLoading: commentaryLoading } = useQuery({
    queryKey: ['commentary', sessionKey, selectedBreachId],
    queryFn: () => commentaryApi.getByBreachId(selectedBreachId as string),
    enabled: !!selectedBreachId,
  });

  const approveMutation = useMutation({
    mutationFn: () => commentaryApi.approve(commentary?.id as string, username),
    onSuccess: () => {
      toast.success('Commentary approved');
      void queryClient.invalidateQueries({ queryKey: ['commentary', sessionKey, selectedBreachId] });
      void queryClient.invalidateQueries({ queryKey: ['commentaries', sessionKey] });
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to approve commentary');
    },
  });

  const filteredBreaches = useMemo(() => {
    const lowerBound = subDays(
      new Date(),
      dateRange === '7d' ? 7 : dateRange === '30d' ? 30 : 90,
    );
    return breaches.filter((b) => {
      const inType = typeFilter === 'ALL' ? true : b.breachType === typeFilter;
      const inRange = parseISO(b.detectedAt).getTime() >= lowerBound.getTime();
      return inType && inRange;
    });
  }, [breaches, dateRange, typeFilter]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between rounded-lg border border-border-subtle bg-bg-surface p-4">
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2">
            {(['ALL', 'T2', 'T3', 'T5'] as const).map((type) => (
              <button
                key={type}
                type="button"
                className={clsx(
                  'rounded-lg px-3 py-1.5 text-xs font-medium transition-colors',
                  typeFilter === type ? 'bg-primary/10 text-primary' : 'bg-bg-raised text-text-secondary hover:text-text-primary',
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
                  dateRange === range ? 'bg-primary/10 text-primary' : 'bg-bg-raised text-text-secondary hover:text-text-primary',
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

      {isLoading ? (
        <div className="space-y-3">
          <SkeletonLoader variant="row" />
          <SkeletonLoader variant="row" />
          <SkeletonLoader variant="row" />
        </div>
      ) : isError ? (
        <div className="rounded-lg border border-danger/30 bg-bg-surface p-4 text-sm text-danger">
          Failed to load breach data.
        </div>
      ) : (
        <div className="space-y-3">
          {filteredBreaches.map((breach) => (
            <div
              key={breach.id}
              className="cursor-pointer border-b border-border-subtle bg-bg-surface p-4 transition-colors hover:bg-bg-raised"
              onClick={() => setSelectedBreachId(breach.id)}
            >
              <div className="flex items-start gap-4">
                <span
                  className={clsx(
                    'mt-1 block h-12 w-[3px]',
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
                    <span className="rounded-full bg-bg-raised px-2 py-0.5 text-xs text-text-secondary">
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
                    className="inline-flex items-center gap-1 text-sm text-ai-accent transition-colors hover:text-text-primary"
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedBreachId(breach.id);
                    }}
                  >
                    Generate Commentary →
                  </button>
                ) : null}
              </div>
            </div>
          ))}
        </div>
      )}

      {selectedBreach ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm">
          <div className="w-[600px] rounded-xl border border-border-subtle bg-bg-surface p-5">
            <div className="mb-4 flex items-start justify-between border-b border-border-subtle pb-4">
              <div>
                <h3 className="text-base font-semibold text-text-primary">Commentary</h3>
                <p className="text-xs text-text-secondary">
                  {selectedBreach.instrument} · {selectedBreach.counterparty} · {selectedBreach.breachType}
                </p>
              </div>
              <button
                className="rounded p-1 text-text-secondary transition-colors hover:bg-bg-raised hover:text-text-primary"
                type="button"
                onClick={() => setSelectedBreachId(null)}
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="relative my-4 min-h-36 pl-4">
              {commentary?.generationType ? (
                <div className="absolute right-3 top-3">
                  <StatusBadge status={commentary.generationType} size="sm" />
                </div>
              ) : null}

              {commentaryLoading ? (
                <div className="flex items-center gap-2 text-ai-accent">
                  <span className="h-2 w-2 animate-pulse rounded-full bg-ai-accent" />
                  <span className="text-sm">Fetching AI commentary...</span>
                </div>
              ) : commentary?.content ? (
                <div className={clsx('border-l-[3px] border-l-ai-accent py-1 pl-4')}>
                  <p className="text-sm leading-[1.7] text-text-primary">{commentary.content}</p>
                  <p className="mt-2 text-[11px] text-text-secondary">
                    Generated by Claude AI · {commentary.promptVersion} · $0.003
                  </p>
                </div>
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
                className="rounded-lg border border-border-subtle bg-bg-raised px-3 py-2 text-xs text-text-secondary transition-colors hover:text-text-primary"
                onClick={() => setSelectedBreachId(null)}
              >
                Close
              </button>
              {commentary && !commentary.approvedBy && canApprove ? (
                <button
                  type="button"
                  disabled={approveMutation.isPending}
                  onClick={() => approveMutation.mutate()}
                  className="rounded-lg border border-primary/20 bg-primary/10 px-3 py-2 text-xs font-medium text-primary transition-colors hover:bg-primary hover:text-black disabled:opacity-50"
                >
                  {approveMutation.isPending ? 'Approving...' : 'Approve'}
                </button>
              ) : commentary && !commentary.approvedBy ? (
                <span className="rounded-lg border border-border-subtle bg-bg-raised px-3 py-2 text-xs text-text-secondary">
                  Approval requires compliance-officer or admin role
                </span>
              ) : (
                <span className="rounded-lg border border-primary/20 bg-primary/10 px-3 py-2 text-xs font-medium text-primary">
                  Approved
                </span>
              )}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
