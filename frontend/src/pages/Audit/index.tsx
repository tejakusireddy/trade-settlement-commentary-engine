import { clsx } from 'clsx';
import { format, parseISO } from 'date-fns';
import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { commentaryApi } from '../../services/api';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { SkeletonLoader } from '../../components/ui/SkeletonLoader';
import { useAuth } from '../../auth/AuthContext';

type AuditFilter = 'ALL' | 'AI' | 'TEMPLATE' | 'APPROVED';

export default function AuditPage() {
  const { sessionKey } = useAuth();
  const [filter, setFilter] = useState<AuditFilter>('ALL');
  const { data: paged, isLoading, isError } = useQuery({
    queryKey: ['audit-commentaries', sessionKey],
    queryFn: () => commentaryApi.list(0, 200),
    refetchInterval: 30000,
  });

  const events = useMemo(
    () =>
      [...(paged?.content ?? [])].sort(
        (a, b) => parseISO(b.createdAt).getTime() - parseISO(a.createdAt).getTime(),
      ),
    [paged?.content],
  );

  const filtered = useMemo(
    () =>
      events.filter((event) => {
        if (filter === 'ALL') return true;
        if (filter === 'AI') return event.generationType === 'AI';
        if (filter === 'TEMPLATE') return event.generationType === 'TEMPLATE';
        return Boolean(event.approvedBy);
      }),
    [events, filter],
  );

  return (
    <div className="space-y-6">
      <div className="rounded-lg border border-border-subtle bg-bg-surface p-4">
        <div className="flex items-center gap-2">
          {(['ALL', 'AI', 'TEMPLATE', 'APPROVED'] as const).map((current) => (
            <button
              key={current}
              type="button"
              onClick={() => setFilter(current)}
              className={clsx(
                'rounded-lg border px-3 py-2 text-xs transition-colors',
                filter === current
                  ? 'border-primary/20 bg-primary/10 text-primary'
                  : 'border-border-subtle bg-bg-raised text-text-secondary hover:text-text-primary',
              )}
            >
              {current}
            </button>
          ))}
        </div>
      </div>

      <section className="rounded-lg border border-border-subtle bg-bg-surface p-5">
        {isLoading ? (
          <div className="space-y-3">
            <SkeletonLoader variant="row" />
            <SkeletonLoader variant="row" />
            <SkeletonLoader variant="row" />
          </div>
        ) : isError ? (
          <p className="text-sm text-danger">Unable to load audit timeline.</p>
        ) : (
          <div className="space-y-4">
            {filtered.map((entry) => (
              <div key={entry.id} className="flex gap-3">
                <div className="mt-1 h-3 w-3 rounded-full bg-ai-accent" />
                <div className="flex-1 border-b border-border-subtle pb-4 last:border-0">
                  <div className="mb-1 flex items-center justify-between">
                    <p className="font-mono text-xs tabular-nums text-text-secondary">
                      {format(parseISO(entry.createdAt), 'yyyy-MM-dd HH:mm:ss')}
                    </p>
                    <StatusBadge status={entry.generationType} />
                  </div>
                  <p className="text-sm text-text-primary">
                    {entry.generationType === 'AI'
                      ? `AI Commentary generated for breach ${entry.breachId.slice(0, 8)}`
                      : `Template commentary generated for breach ${entry.breachId.slice(0, 8)}`}
                  </p>
                  <div className="mt-1 flex items-center gap-2 text-xs text-text-secondary">
                    <span>Prompt {entry.promptVersion}</span>
                    {entry.approvedBy ? (
                      <span className="rounded-full border border-primary/20 bg-primary/10 px-2 py-0.5 text-primary">
                        Commentary approved by {entry.approvedBy}
                      </span>
                    ) : null}
                    {entry.generationType === 'AI' ? (
                      <span className="font-mono tabular-nums">$--</span>
                    ) : null}
                  </div>
                </div>
              </div>
            ))}
            {!filtered.length ? (
              <p className="text-sm text-text-secondary">No audit events found for this filter.</p>
            ) : null}
          </div>
        )}
      </section>
    </div>
  );
}
