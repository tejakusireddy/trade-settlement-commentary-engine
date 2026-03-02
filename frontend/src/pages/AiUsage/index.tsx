import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { clsx } from 'clsx';
import { Bar, BarChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { format, parseISO, subDays } from 'date-fns';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { SkeletonLoader } from '../../components/ui/SkeletonLoader';
import { commentaryApi } from '../../services/api';
import { useAuth } from '../../auth/AuthContext';

export default function AiUsagePage() {
  const { isAdmin, sessionKey } = useAuth();

  const { data: costData, isLoading: costLoading, isError: costError } = useQuery({
    queryKey: ['ai-cost', sessionKey],
    queryFn: () => commentaryApi.getDailyCost(),
    refetchInterval: 30_000,
    enabled: isAdmin,
  });

  const { data: breakerData, isLoading: breakerLoading, isError: breakerError } = useQuery({
    queryKey: ['ai-breaker', sessionKey],
    queryFn: () => commentaryApi.getCircuitBreaker(),
    refetchInterval: 30_000,
    enabled: isAdmin,
  });

  const { data: commentaryPage, isLoading: commentaryLoading, isError: commentaryError } = useQuery({
    queryKey: ['ai-commentaries', sessionKey],
    queryFn: () => commentaryApi.list(0, 30),
    refetchInterval: 60_000,
    enabled: isAdmin,
  });

  if (!isAdmin) {
    return (
      <div className="rounded-lg border border-border-subtle bg-bg-surface p-6 text-sm text-text-secondary">
        AI usage and circuit-breaker telemetry are visible to admin users only.
      </div>
    );
  }

  const commentaries = commentaryPage?.content ?? [];
  const dailyCost = costData?.dailyCostUsd ?? 0;
  const dailyCap = costData?.dailyCapUsd ?? 10;
  const generatedCount = commentaries.length;
  const breakerStatus = String(
    (breakerData as { state?: string; status?: string; circuitBreakerStatus?: string } | undefined)
      ?.state ??
      (breakerData as { status?: string } | undefined)?.status ??
      (breakerData as { circuitBreakerStatus?: string } | undefined)?.circuitBreakerStatus ??
      costData?.circuitBreakerStatus ??
      'CLOSED',
  );
  const usagePercent = dailyCap > 0 ? Math.min((dailyCost / dailyCap) * 100, 100) : 0;

  const chartData = useMemo(
    () =>
      Array.from({ length: 30 }).map((_, idx) => {
        const day = subDays(new Date(), 29 - idx);
        const dayCommentaries = commentaries.filter(
          (commentary) => format(parseISO(commentary.createdAt), 'yyyy-MM-dd') === format(day, 'yyyy-MM-dd'),
        );
        const estimated = Number(((dayCommentaries.length / Math.max(commentaries.length, 1)) * dailyCost).toFixed(2));
        return {
          day: format(day, 'MM/dd'),
          cost: estimated,
        };
      }),
    [commentaries, dailyCost],
  );

  if (costLoading || breakerLoading || commentaryLoading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-3 gap-4">
          <SkeletonLoader variant="card" />
          <SkeletonLoader variant="card" />
          <SkeletonLoader variant="card" />
        </div>
        <SkeletonLoader variant="card" className="h-72" />
      </div>
    );
  }

  if (costError || breakerError || commentaryError) {
    return (
      <div className="rounded-lg border border-danger/30 bg-bg-surface p-6 text-sm text-danger">
        Failed to load AI usage telemetry.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-3 gap-4">
        <div className="rounded-lg border border-border-subtle bg-bg-surface p-5">
          <p className="text-xs uppercase tracking-wider text-text-secondary">Today's Cost</p>
          <p className="tabular-nums mt-2 font-mono text-3xl text-ai-accent">${dailyCost.toFixed(2)}</p>
        </div>
        <div className="rounded-lg border border-border-subtle bg-bg-surface p-5">
          <p className="text-xs uppercase tracking-wider text-text-secondary">Daily Cap</p>
          <p className="tabular-nums mt-2 font-mono text-3xl text-text-secondary">${dailyCap.toFixed(2)}</p>
        </div>
        <div className="rounded-lg border border-border-subtle bg-bg-surface p-5">
          <p className="text-xs uppercase tracking-wider text-text-secondary">Commentaries Generated</p>
          <p className="tabular-nums mt-2 font-mono text-3xl text-text-primary">{generatedCount}</p>
        </div>
      </div>

      <section className="rounded-lg border border-border-subtle bg-bg-surface p-5">
        <h3 className="mb-4 text-base font-semibold text-text-primary">Daily AI Spend</h3>
        <div className="mb-4">
          <div className="mb-1 flex items-center justify-between text-xs text-text-secondary">
            <span>Usage</span>
            <span className="font-mono tabular-nums">
              ${dailyCost.toFixed(2)} / ${dailyCap.toFixed(2)}
            </span>
          </div>
          <div className="h-2 overflow-hidden rounded bg-bg-raised">
            <div
              className={clsx(
                'h-full transition-all',
                usagePercent < 50 ? 'bg-primary' : usagePercent < 80 ? 'bg-warning' : 'bg-danger',
              )}
              style={{ width: `${usagePercent}%` }}
            />
          </div>
        </div>
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <CartesianGrid stroke="#21262D" strokeDasharray="3 3" />
              <XAxis dataKey="day" tick={{ fill: '#7D8590', fontSize: 11, fontFamily: 'IBM Plex Mono' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fill: '#7D8590', fontSize: 11, fontFamily: 'IBM Plex Mono' }} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ backgroundColor: '#161B22', border: '1px solid #21262D', borderRadius: 8 }}
                labelStyle={{ color: '#E6EDF3' }}
                itemStyle={{ color: '#BC8CFF' }}
                formatter={(value) => `$${Number(value ?? 0).toFixed(2)}`}
              />
              <ReferenceLine y={dailyCap} stroke="#F85149" strokeDasharray="6 4" />
              <Bar dataKey="cost" fill="#BC8CFF" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>

      <div className="grid grid-cols-2 gap-4">
        <section className="rounded-lg border border-border-subtle bg-bg-surface p-5">
          <h3 className="mb-4 text-base font-semibold text-text-primary">Circuit Breaker Details</h3>
          <div className="mb-4">
            <StatusBadge status={breakerStatus} />
          </div>
          <div className="space-y-2 text-sm text-text-secondary">
            <p>Failure Rate: {(breakerData as { failureRate?: number } | undefined)?.failureRate ?? 'N/A'}</p>
            <p>Calls in Window: {(breakerData as { bufferedCalls?: number } | undefined)?.bufferedCalls ?? 'N/A'}</p>
            <p>Window Size: 10</p>
            <p>Threshold: 50%</p>
            <p>Wait Duration: 60s</p>
          </div>
        </section>

        <section className="rounded-lg border border-border-subtle bg-bg-surface p-5">
          <h3 className="mb-4 text-base font-semibold text-text-primary">Recent AI Calls</h3>
          <div className="space-y-2">
            {commentaries.slice(0, 8).map((commentary) => (
              <div
                key={commentary.id}
                className="grid grid-cols-6 gap-2 rounded-lg bg-bg-base px-3 py-2 text-xs text-text-secondary"
              >
                <span className="tabular-nums font-mono">{format(parseISO(commentary.createdAt), 'HH:mm:ss')}</span>
                <span>claude-sonnet-4-6</span>
                <span className="tabular-nums font-mono">--</span>
                <span className="tabular-nums font-mono">--</span>
                <span className="tabular-nums font-mono text-text-primary">--</span>
                <span className="tabular-nums font-mono">--</span>
              </div>
            ))}
            {!commentaries.length ? (
              <div className="rounded-lg bg-bg-base px-3 py-2 text-xs text-text-tertiary">
                No AI calls recorded yet.
              </div>
            ) : null}
          </div>
        </section>
      </div>
    </div>
  );
}
