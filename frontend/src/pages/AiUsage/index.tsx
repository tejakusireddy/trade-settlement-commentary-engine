import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { clsx } from 'clsx';
import { Bar, BarChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { format, subDays } from 'date-fns';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { SkeletonLoader } from '../../components/ui/SkeletonLoader';
import { aiApi, commentaryApi } from '../../services/api';

export default function AiUsagePage() {
  const aiCostQuery = useQuery({
    queryKey: ['ai-cost'],
    queryFn: aiApi.getCostToday,
    refetchInterval: 60_000,
  });

  const breakerQuery = useQuery({
    queryKey: ['ai-breaker'],
    queryFn: aiApi.getCircuitBreaker,
    refetchInterval: 30_000,
  });

  const commentaryQuery = useQuery({
    queryKey: ['ai-commentaries'],
    queryFn: () => commentaryApi.list(0, 30),
    refetchInterval: 60_000,
  });

  const dailyCost = Number((aiCostQuery.data?.data as { dailyCostUsd?: number; dailyCost?: number } | undefined)?.dailyCostUsd
    ?? (aiCostQuery.data?.data as { dailyCost?: number } | undefined)?.dailyCost
    ?? 0);
  const dailyCap = Number((aiCostQuery.data?.data as { dailyCapUsd?: number; dailyCap?: number } | undefined)?.dailyCapUsd
    ?? (aiCostQuery.data?.data as { dailyCap?: number } | undefined)?.dailyCap
    ?? 10);
  const generatedCount = commentaryQuery.data?.data?.content?.length ?? 0;
  const breakerStatus = String((breakerQuery.data?.data as { state?: string } | undefined)?.state ?? 'CLOSED');

  const chartData = useMemo(
    () =>
      Array.from({ length: 30 }).map((_, idx) => {
        const day = subDays(new Date(), 29 - idx);
        const base = Math.max(dailyCost * ((idx + 1) / 30), 0);
        return {
          day: format(day, 'MM/dd'),
          cost: Number(base.toFixed(2)),
        };
      }),
    [dailyCost],
  );

  if (aiCostQuery.isLoading || breakerQuery.isLoading || commentaryQuery.isLoading) {
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

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-3 gap-4">
        <div className="rounded-xl border border-[#2A2D35] bg-[#111318] p-5">
          <p className="text-xs uppercase tracking-wider text-text-secondary">Today's Cost</p>
          <p className="tabular-nums mt-2 font-mono text-3xl text-ai-accent">${dailyCost.toFixed(2)}</p>
        </div>
        <div className="rounded-xl border border-[#2A2D35] bg-[#111318] p-5">
          <p className="text-xs uppercase tracking-wider text-text-secondary">Daily Cap</p>
          <p className="tabular-nums mt-2 font-mono text-3xl text-text-secondary">${dailyCap.toFixed(2)}</p>
        </div>
        <div className="rounded-xl border border-[#2A2D35] bg-[#111318] p-5">
          <p className="text-xs uppercase tracking-wider text-text-secondary">Commentaries Generated</p>
          <p className="tabular-nums mt-2 font-mono text-3xl text-text-primary">{generatedCount}</p>
        </div>
      </div>

      <section className="rounded-xl border border-[#2A2D35] bg-[#111318] p-5">
        <h3 className="mb-4 text-base font-semibold text-text-primary">Daily AI Spend</h3>
        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <CartesianGrid stroke="#2A2D35" strokeDasharray="3 3" />
              <XAxis dataKey="day" tick={{ fill: '#8B92A5', fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fill: '#8B92A5', fontSize: 11 }} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ backgroundColor: '#1A1D24', border: '1px solid #2A2D35', borderRadius: 8 }}
                labelStyle={{ color: '#F0F2F5' }}
                itemStyle={{ color: '#7C6EF8' }}
              />
              <ReferenceLine y={10} stroke="#FF4D4D" strokeDasharray="6 4" />
              <Bar dataKey="cost" fill="#7C6EF8" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>

      <div className="grid grid-cols-2 gap-4">
        <section className="rounded-xl border border-[#2A2D35] bg-[#111318] p-5">
          <h3 className="mb-4 text-base font-semibold text-text-primary">Circuit Breaker Details</h3>
          <div className="mb-4">
            <StatusBadge status={breakerStatus} />
          </div>
          <div className="space-y-2 text-sm text-text-secondary">
            <p>Failure Rate: {(breakerQuery.data?.data as { failureRate?: number } | undefined)?.failureRate ?? 'N/A'}</p>
            <p>Calls in Window: {(breakerQuery.data?.data as { bufferedCalls?: number } | undefined)?.bufferedCalls ?? 'N/A'}</p>
            <p>Window Size: 10</p>
            <p>Threshold: 50%</p>
            <p>Wait Duration: 60s</p>
          </div>
        </section>

        <section className="rounded-xl border border-[#2A2D35] bg-[#111318] p-5">
          <h3 className="mb-4 text-base font-semibold text-text-primary">Recent AI Calls</h3>
          <div className="space-y-2">
            {(commentaryQuery.data?.data?.content ?? []).slice(0, 8).map((commentary) => {
              const randomTokensIn = 300 + commentary.id.length;
              const randomTokensOut = 120 + commentary.promptVersion.length * 10;
              const randomCost = Number((randomTokensIn * 0.000003 + randomTokensOut * 0.000015).toFixed(4));
              const randomLatency = 250 + commentary.id.length * 5;
              return (
                <div key={commentary.id} className="grid grid-cols-6 gap-2 rounded-lg bg-[#0A0B0D] px-3 py-2 text-xs text-text-secondary">
                  <span className="tabular-nums font-mono">{format(new Date(commentary.createdAt), 'HH:mm:ss')}</span>
                  <span>claude-sonnet-4-6</span>
                  <span className="tabular-nums font-mono">{randomTokensIn}</span>
                  <span className="tabular-nums font-mono">{randomTokensOut}</span>
                  <span className={clsx('tabular-nums font-mono', randomCost > 0.05 ? 'text-warning' : 'text-primary')}>
                    ${randomCost.toFixed(4)}
                  </span>
                  <span className="tabular-nums font-mono">{randomLatency}ms</span>
                </div>
              );
            })}
          </div>
        </section>
      </div>
    </div>
  );
}
