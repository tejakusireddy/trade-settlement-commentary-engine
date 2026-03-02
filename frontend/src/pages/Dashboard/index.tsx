import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { clsx } from 'clsx';
import { Cpu, ArrowLeftRight, AlertTriangle, FileText } from 'lucide-react';
import { format, isSameDay, isToday, parseISO, subDays } from 'date-fns';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useNavigate } from 'react-router-dom';
import { MetricCard } from '../../components/ui/MetricCard';
import { SkeletonLoader } from '../../components/ui/SkeletonLoader';
import { DataTable, type DataTableColumn } from '../../components/ui/DataTable';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { CustomTooltip } from '../../components/ui/CustomTooltip';
import { breachApi, commentaryApi, tradeApi } from '../../services/api';
import { useAuth } from '../../auth/AuthContext';
import type { Breach } from '../../types';

export default function DashboardPage() {
  const navigate = useNavigate();
  const { isAdmin, sessionKey } = useAuth();

  const { data: trades, isLoading: tradesLoading, isError: tradesError } = useQuery({
    queryKey: ['trades', sessionKey],
    queryFn: () => tradeApi.list(),
    refetchInterval: 30_000,
    staleTime: 10_000,
  });

  const { data: breaches, isLoading: breachesLoading, isError: breachesError } = useQuery({
    queryKey: ['breaches', sessionKey],
    queryFn: () => breachApi.list(),
    refetchInterval: 15_000,
    staleTime: 5_000,
  });

  const { data: aiCost, isLoading: aiLoading, isError: aiError } = useQuery({
    queryKey: ['ai-cost', sessionKey],
    queryFn: () => commentaryApi.getDailyCost(),
    refetchInterval: 60_000,
    enabled: isAdmin,
  });

  const safeTrades = trades ?? [];
  const safeBreaches = breaches ?? [];

  const chartData = useMemo(
    () =>
      Array.from({ length: 14 }, (_, i) => {
        const date = subDays(new Date(), 13 - i);
        const dateStr = format(date, 'MMM dd');
        const count =
          safeBreaches.filter((breach) => isSameDay(parseISO(breach.detectedAt), date)).length ?? 0;
        return { date: dateStr, count };
      }),
    [safeBreaches],
  );

  const totalTrades = safeTrades.length ?? 0;
  const breachedToday = safeBreaches.filter((breach) => isToday(parseISO(breach.detectedAt))).length ?? 0;
  const pendingCommentary =
    safeBreaches.filter((breach) => breach.status === 'PENDING_COMMENTARY').length ?? 0;
  const dailyCost = aiCost?.dailyCostUsd ?? 0;
  const dailyCap = aiCost?.dailyCapUsd ?? 10;
  const usagePercent = dailyCap > 0 ? Math.min((dailyCost / dailyCap) * 100, 100) : 0;
  const circuitStatus = aiCost?.circuitBreakerStatus ?? 'CLOSED';

  const recentBreaches = useMemo(
    () =>
      [...safeBreaches]
        .sort((a, b) => parseISO(b.detectedAt).getTime() - parseISO(a.detectedAt).getTime())
        .slice(0, 10),
    [safeBreaches],
  );
  const breachColumns: DataTableColumn<Breach>[] = [
    {
      header: 'Trade ID',
      accessor: 'tradeId',
      render: (row) => <span className="tabular-nums font-mono text-xs text-info">{row.tradeId}</span>,
    },
    { header: 'Instrument', accessor: 'instrument' },
    { header: 'Counterparty', accessor: 'counterparty', className: 'text-text-secondary' },
    {
      header: 'Type',
      accessor: 'breachType',
      render: (row) => <StatusBadge status={row.breachType} />,
    },
    {
      header: 'Days Overdue',
      accessor: 'daysOverdue',
      render: (row) => (
        <span className={clsx('tabular-nums font-mono', row.breachType === 'T5' ? 'text-danger' : 'text-text-primary')}>
          {row.daysOverdue}
        </span>
      ),
    },
    {
      header: 'Status',
      accessor: 'status',
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      header: 'Detected',
      accessor: 'detectedAt',
      render: (row) => (
        <span className="tabular-nums font-mono text-xs text-text-secondary">
          {format(new Date(row.detectedAt), 'MMM dd HH:mm')}
        </span>
      ),
    },
  ];

  if (tradesLoading || breachesLoading || (isAdmin && aiLoading)) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-4 gap-4">
          <SkeletonLoader variant="card" />
          <SkeletonLoader variant="card" />
          <SkeletonLoader variant="card" />
          <SkeletonLoader variant="card" />
        </div>
        <SkeletonLoader variant="card" className="h-72" />
        <SkeletonLoader variant="card" className="h-72" />
      </div>
    );
  }

  if (tradesError || breachesError) {
    return (
      <div className="rounded-lg border border-danger/30 bg-bg-surface p-6 text-sm text-danger">
        Failed to load dashboard data. Please verify service availability and refresh.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-4 gap-4">
        <MetricCard
          title="Total Trades"
          value={totalTrades.toLocaleString()}
          subtitle="All records"
          icon={ArrowLeftRight}
          color="primary"
        />
        <MetricCard
          title="Breached Today"
          value={breachedToday.toLocaleString()}
          subtitle="Current day"
          icon={AlertTriangle}
          color="danger"
        />
        <MetricCard
          title="Pending Commentary"
          value={pendingCommentary.toLocaleString()}
          subtitle="Needs narrative"
          icon={FileText}
          color="warning"
        />
        <MetricCard
          title="AI Cost Today"
          value={isAdmin ? `$${dailyCost.toFixed(2)}` : 'Restricted'}
          subtitle={isAdmin ? `Cap $${dailyCap.toFixed(2)}` : 'Admin only'}
          icon={Cpu}
          color="ai"
        />
      </div>

      <div className="grid grid-cols-3 gap-4">
        <section className="col-span-2 rounded-lg border border-border-subtle bg-bg-surface p-5">
          <div className="mb-4">
            <h3 className="text-base font-semibold text-text-primary">Settlement Breach Activity</h3>
            <p className="text-xs text-text-secondary">Last 14 days</p>
          </div>
          <div className="h-[240px] min-h-[240px] min-w-0">
            <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={220}>
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="breachGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#F85149" stopOpacity={0.05} />
                    <stop offset="95%" stopColor="#F85149" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke="#21262D" strokeDasharray="3 3" />
                <XAxis dataKey="date" tick={{ fill: '#7D8590', fontSize: 11, fontFamily: 'IBM Plex Mono' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: '#7D8590', fontSize: 11, fontFamily: 'IBM Plex Mono' }} axisLine={false} tickLine={false} />
                <Tooltip content={<CustomTooltip />} />
                <Area type="monotone" dataKey="count" stroke="#F85149" strokeWidth={2} fill="url(#breachGradient)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="rounded-lg border border-border-subtle bg-bg-surface p-5">
          <h3 className="mb-4 text-base font-semibold text-text-primary">AI System Status</h3>
          {isAdmin ? (
            <>
              <div className="mb-4 flex flex-col items-center">
                <div
                  className={clsx(
                    'mb-3 flex h-20 w-20 items-center justify-center rounded-full',
                    circuitStatus === 'OPEN'
                      ? 'bg-danger/20 text-danger'
                      : circuitStatus === 'HALF_OPEN'
                        ? 'bg-warning/20 text-warning'
                        : 'bg-primary/20 text-primary',
                  )}
                >
                  <Cpu className="h-8 w-8" />
                </div>
                <p className="mb-2 text-sm font-semibold text-text-primary">{circuitStatus}</p>
                <StatusBadge status={circuitStatus} />
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between text-xs text-text-secondary">
                  <span>Daily Cost</span>
                  <span className="tabular-nums font-mono text-text-primary">
                    ${dailyCost.toFixed(2)} / ${dailyCap.toFixed(2)}
                  </span>
                </div>
                <div className="h-2 overflow-hidden rounded bg-bg-raised">
                  <div
                    className={clsx(
                      'h-full',
                      usagePercent < 50 ? 'bg-primary' : usagePercent < 80 ? 'bg-warning' : 'bg-danger',
                    )}
                    style={{ width: `${usagePercent}%` }}
                  />
                </div>
                <p className="text-xs text-text-tertiary">Model: claude-sonnet-4-6</p>
                <p className="text-xs text-text-tertiary">Cap: $10.00</p>
                {aiError ? (
                  <p className="text-xs text-warning">AI endpoint unavailable or unauthorized.</p>
                ) : null}
              </div>
            </>
          ) : (
            <div className="rounded-lg border border-border-subtle bg-bg-base p-4 text-sm text-text-secondary">
              AI telemetry is restricted to admin users.
            </div>
          )}
        </section>
      </div>

      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-semibold text-text-primary">Recent Breaches</h3>
          <button
            type="button"
            className="text-sm text-info transition-colors hover:text-primary"
            onClick={() => navigate('/breaches')}
          >
            View All →
          </button>
        </div>
        <DataTable
          columns={breachColumns}
          data={recentBreaches}
          emptyMessage="No recent breaches"
          onRowClick={(row) => navigate(`/breaches/${row.id}`)}
        />
      </section>
    </div>
  );
}
