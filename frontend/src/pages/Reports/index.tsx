import { format } from 'date-fns';
import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { CheckCircle2 } from 'lucide-react';
import { breachApi, commentaryApi } from '../../services/api';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { SkeletonLoader } from '../../components/ui/SkeletonLoader';
import { useAuth } from '../../auth/AuthContext';

export default function ReportsPage() {
  const { sessionKey } = useAuth();
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [page, setPage] = useState(0);

  const { data: commentaries, isLoading, isError } = useQuery({
    queryKey: ['commentaries', sessionKey, page],
    queryFn: () => commentaryApi.list(page, 20),
    refetchInterval: 30_000,
  });

  const { data: breaches = [] } = useQuery({
    queryKey: ['breaches', sessionKey],
    queryFn: () => breachApi.list(),
    refetchInterval: 30_000,
  });

  const breachById = useMemo(
    () => new Map(breaches.map((breach) => [breach.id, breach])),
    [breaches],
  );

  const aiCount = (commentaries?.content ?? []).filter((c) => c.generationType === 'AI').length;
  const templateCount = (commentaries?.content ?? []).filter((c) => c.generationType === 'TEMPLATE').length;

  return (
    <div className="space-y-6">
      <section className="no-print flex items-center gap-3 rounded-lg border border-border-subtle bg-bg-surface p-4">
        <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          className="rounded-lg border border-border-subtle bg-bg-base px-3 py-2 text-sm text-text-primary outline-none transition-colors hover:border-border-focus"
        />
        <button className="rounded-lg bg-primary px-3 py-2 text-sm font-medium text-black transition-opacity hover:opacity-90" type="button">
          Load Report
        </button>
        <button
          onClick={() => window.print()}
          className="rounded-lg border border-border-subtle bg-bg-raised px-3 py-2 text-sm text-text-secondary transition-colors hover:text-text-primary"
          type="button"
        >
          Export to PDF
        </button>
      </section>

      <section className="print-area rounded-lg border border-border-subtle bg-bg-surface p-8">
        <header>
          <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-text-primary">
            Trade Settlement Breach Report
          </h2>
          <p className="mt-2 text-xs text-text-secondary">Report Date: {date}</p>
          <div className="mt-4 border-b border-border-subtle" />
        </header>

        <div className="mt-5 grid grid-cols-3 gap-3">
          <div className="rounded-lg border border-border-subtle bg-bg-base p-4">
            <p className="text-xs text-text-secondary">Total Breaches</p>
            <p className="tabular-nums font-mono text-2xl text-text-primary">{commentaries?.content.length ?? 0}</p>
          </div>
          <div className="rounded-lg border border-border-subtle bg-bg-base p-4">
            <p className="text-xs text-text-secondary">AI Generated</p>
            <p className="tabular-nums font-mono text-2xl text-ai-accent">{aiCount}</p>
          </div>
          <div className="rounded-lg border border-border-subtle bg-bg-base p-4">
            <p className="text-xs text-text-secondary">Template Generated</p>
            <p className="tabular-nums font-mono text-2xl text-warning">{templateCount}</p>
          </div>
        </div>

        <div className="mt-6">
          {isLoading ? (
            <div className="space-y-3">
              <SkeletonLoader variant="row" />
              <SkeletonLoader variant="row" />
              <SkeletonLoader variant="row" />
            </div>
          ) : isError ? (
            <div className="text-sm text-danger">Failed to load management report data.</div>
          ) : (
            (commentaries?.content ?? []).map((commentary) => {
              const breach = breachById.get(commentary.breachId);
              return (
              <article key={commentary.id} className="border-b border-border-subtle py-6 last:border-0">
                <div className="mb-3 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <p className="font-mono text-xs text-primary">
                      Trade {breach?.tradeId ?? commentary.breachId.slice(0, 8)}
                    </p>
                    <p className="text-sm text-text-primary">
                      {breach?.instrument ?? 'Unknown Instrument'} · {breach?.counterparty ?? 'Unknown Counterparty'}
                    </p>
                  </div>
                  <StatusBadge status={commentary.generationType} />
                </div>
                <p className="leading-relaxed text-text-primary">{commentary.content}</p>
                <div className="mt-3 flex items-center gap-3 text-xs text-text-secondary">
                  <span>Prompt {commentary.promptVersion}</span>
                  <span className="tabular-nums font-mono">{format(new Date(commentary.createdAt), 'yyyy-MM-dd HH:mm')}</span>
                  {commentary.approvedBy ? (
                    <span className="inline-flex items-center gap-1 rounded-full border border-primary/20 bg-primary/10 px-2 py-0.5 text-primary">
                      <CheckCircle2 className="h-3 w-3" />
                      ✓ Approved by {commentary.approvedBy}
                    </span>
                  ) : null}
                </div>
              </article>
            );
            })
          )}
        </div>
        <div className="no-print mt-5 flex justify-end gap-2">
          <button
            type="button"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="rounded-lg border border-border-subtle bg-bg-raised px-3 py-2 text-sm text-text-secondary transition-colors hover:text-text-primary"
            disabled={page === 0}
          >
            Previous
          </button>
          <button
            type="button"
            onClick={() => setPage((p) => p + 1)}
            className="rounded-lg border border-border-subtle bg-bg-raised px-3 py-2 text-sm text-text-secondary transition-colors hover:text-text-primary"
            disabled={(commentaries?.content.length ?? 0) < 20}
          >
            Next
          </button>
        </div>
      </section>
    </div>
  );
}
