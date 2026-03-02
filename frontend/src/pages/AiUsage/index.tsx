import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { clsx } from 'clsx';
import { Bar, BarChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { format, parseISO } from 'date-fns';
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

  const { data: usageHistory, isLoading: usageLoading, isError: usageError } = useQuery({
    queryKey: ['ai-usage-history', sessionKey],
    queryFn: () => commentaryApi.getUsageHistory(30, 20),
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

  const usageDaily = usageHistory?.daily ?? [];
  const recentCalls = usageHistory?.recentCalls ?? [];
  const dailyCost = costData?.dailyCostUsd ?? 0;
  const dailyCap = costData?.dailyCapUsd ?? 10;
  const generatedCount = usageDaily.reduce((total, day) => total + day.callCount, 0);
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
    () => usageDaily.map((day) => ({
      day: format(parseISO(day.day), 'MM/dd'),
      cost: Number(day.costUsd ?? 0),
    })),
    [usageDaily],
  );

  if (costLoading || breakerLoading || usageLoading) {
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

  if (costError || breakerError || usageError) {
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
          <p className="text-xs uppercase tracking-wider text-text-secondary">Calls (30d)</p>
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
        <div className="h-64 min-h-[256px] min-w-0">
          <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
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
            {recentCalls.slice(0, 8).map((call) => (
              <div
                key={call.commentaryId}
                className="grid grid-cols-6 gap-2 rounded-lg bg-bg-base px-3 py-2 text-xs text-text-secondary"
              >
                <span className="tabular-nums font-mono">{format(parseISO(call.createdAt), 'HH:mm:ss')}</span>
                <span>{call.model}</span>
                <span className="tabular-nums font-mono">{call.tokensInput}</span>
                <span className="tabular-nums font-mono">{call.tokensOutput}</span>
                <span className="tabular-nums font-mono text-text-primary">${Number(call.costUsd).toFixed(4)}</span>
                <span className="tabular-nums font-mono">{call.latencyMs} ms</span>
              </div>
            ))}
            {!recentCalls.length ? (
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
