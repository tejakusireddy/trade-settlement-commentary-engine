import { format } from 'date-fns';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { CheckCircle2 } from 'lucide-react';
import { commentaryApi } from '../../services/api';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { SkeletonLoader } from '../../components/ui/SkeletonLoader';

export default function ReportsPage() {
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'));

  const commentaryQuery = useQuery({
    queryKey: ['commentary-report', date],
    queryFn: () => commentaryApi.list(0, 100),
    refetchInterval: 30_000,
  });

  const commentaries = commentaryQuery.data?.data?.content ?? [];
  const aiCount = commentaries.filter((c) => c.generationType === 'AI').length;
  const templateCount = commentaries.filter((c) => c.generationType === 'TEMPLATE').length;

  return (
    <div className="space-y-6">
      <section className="flex items-center gap-3 rounded-xl border border-[#2A2D35] bg-[#111318] p-4">
        <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          className="rounded-lg border border-[#2A2D35] bg-[#0A0B0D] px-3 py-2 text-sm text-text-primary outline-none transition-colors hover:border-[#3A3D45]"
        />
        <button className="rounded-lg bg-primary px-3 py-2 text-sm font-medium text-black transition-opacity hover:opacity-90" type="button">
          Load Report
        </button>
        <button
          className="rounded-lg border border-[#2A2D35] bg-[#1A1D24] px-3 py-2 text-sm text-text-secondary transition-colors hover:text-text-primary"
          type="button"
        >
          Export
        </button>
      </section>

      <section className="rounded-xl border border-[#2A2D35] bg-[#111318] p-8">
        <header>
          <h2 className="text-sm font-semibold uppercase tracking-[0.2em] text-text-primary">
            Trade Settlement Breach Report
          </h2>
          <p className="mt-2 text-xs text-text-secondary">Report Date: {date}</p>
          <div className="mt-4 border-b border-[#2A2D35]" />
        </header>

        <div className="mt-5 grid grid-cols-3 gap-3">
          <div className="rounded-lg border border-[#2A2D35] bg-[#0A0B0D] p-4">
            <p className="text-xs text-text-secondary">Total Breaches</p>
            <p className="tabular-nums font-mono text-2xl text-text-primary">{commentaries.length}</p>
          </div>
          <div className="rounded-lg border border-[#2A2D35] bg-[#0A0B0D] p-4">
            <p className="text-xs text-text-secondary">AI Generated</p>
            <p className="tabular-nums font-mono text-2xl text-ai-accent">{aiCount}</p>
          </div>
          <div className="rounded-lg border border-[#2A2D35] bg-[#0A0B0D] p-4">
            <p className="text-xs text-text-secondary">Template Generated</p>
            <p className="tabular-nums font-mono text-2xl text-warning">{templateCount}</p>
          </div>
        </div>

        <div className="mt-6">
          {commentaryQuery.isLoading ? (
            <div className="space-y-3">
              <SkeletonLoader variant="row" />
              <SkeletonLoader variant="row" />
              <SkeletonLoader variant="row" />
            </div>
          ) : (
            commentaries.map((commentary) => (
              <article key={commentary.id} className="border-b border-[#2A2D35] py-6 last:border-0">
                <div className="mb-3 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <p className="font-mono text-xs text-primary">{commentary.id.slice(0, 8)}</p>
                    <p className="text-sm text-text-primary">Trade Commentary</p>
                  </div>
                  <StatusBadge status={commentary.generationType} />
                </div>
                <p className="leading-relaxed text-text-primary">{commentary.content}</p>
                <div className="mt-3 flex items-center gap-3 text-xs text-text-secondary">
                  <span>Prompt {commentary.promptVersion}</span>
                  <span className="tabular-nums font-mono">{format(new Date(commentary.createdAt), 'yyyy-MM-dd HH:mm')}</span>
                  {commentary.approvedBy ? (
                    <span className="inline-flex items-center gap-1 text-primary">
                      <CheckCircle2 className="h-3 w-3" />
                      Approved by {commentary.approvedBy}
                    </span>
                  ) : null}
                </div>
              </article>
            ))
          )}
        </div>
      </section>
    </div>
  );
}
