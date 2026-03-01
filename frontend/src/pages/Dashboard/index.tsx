import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { clsx } from 'clsx';
import { Cpu, ArrowLeftRight, AlertTriangle, FileText } from 'lucide-react';
import { format, subDays } from 'date-fns';
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
import { aiApi, breachApi, tradeApi } from '../../services/api';
import type { Breach } from '../../types';

function toArray<T>(data: T[] | { content?: T[] } | undefined): T[] {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  return data.content ?? [];
}

export default function DashboardPage() {
  const navigate = useNavigate();

  const tradesQuery = useQuery({
    queryKey: ['trades'],
    queryFn: tradeApi.get,
    refetchInterval: 30_000,
  });

  const breachesQuery = useQuery({
    queryKey: ['breaches'],
    queryFn: breachApi.list,
    refetchInterval: 15_000,
  });

  const aiCostQuery = useQuery({
    queryKey: ['aiCostToday'],
    queryFn: aiApi.getCostToday,
    refetchInterval: 60_000,
  });

  const trades = toArray(tradesQuery.data?.data);
  const breaches = toArray(breachesQuery.data?.data);

  const chartData = useMemo(() => {
    const byDate = new Map<string, number>();
    for (let i = 13; i >= 0; i -= 1) {
      const date = format(subDays(new Date(), i), 'yyyy-MM-dd');
      byDate.set(date, 0);
    }
    breaches.forEach((b) => {
      const date = format(new Date(b.detectedAt), 'yyyy-MM-dd');
      if (byDate.has(date)) {
        byDate.set(date, (byDate.get(date) ?? 0) + 1);
      }
    });
    return Array.from(byDate.entries()).map(([date, count]) => ({
      date: format(new Date(date), 'MMM dd'),
      count,
    }));
  }, [breaches]);

  const totalTrades = trades.length;
  const breachedToday = breaches.filter(
    (b) => format(new Date(b.detectedAt), 'yyyy-MM-dd') === format(new Date(), 'yyyy-MM-dd'),
  ).length;
  const pendingCommentary = breaches.filter((b) => b.status === 'PENDING_COMMENTARY').length;
  const dailyCost = Number((aiCostQuery.data?.data as { dailyCost?: number; dailyCostUsd?: number } | undefined)?.dailyCostUsd
    ?? (aiCostQuery.data?.data as { dailyCost?: number } | undefined)?.dailyCost
    ?? 0);
  const dailyCap = Number((aiCostQuery.data?.data as { dailyCap?: number; dailyCapUsd?: number } | undefined)?.dailyCapUsd
    ?? (aiCostQuery.data?.data as { dailyCap?: number } | undefined)?.dailyCap
    ?? 10);
  const usagePercent = dailyCap > 0 ? Math.min((dailyCost / dailyCap) * 100, 100) : 0;
  const circuitStatus = String((aiCostQuery.data?.data as { circuitBreakerStatus?: string } | undefined)?.circuitBreakerStatus ?? 'CLOSED');

  const recentBreaches = breaches.slice(0, 10);
  const breachColumns: DataTableColumn<Breach>[] = [
    {
      header: 'Trade ID',
      accessor: 'tradeId',
      render: (row) => <span className="tabular-nums font-mono text-xs text-primary">{row.tradeId}</span>,
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

  if (tradesQuery.isLoading || breachesQuery.isLoading || aiCostQuery.isLoading) {
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
          value={`$${dailyCost.toFixed(2)}`}
          subtitle={`Cap $${dailyCap.toFixed(2)}`}
          icon={Cpu}
          color="ai"
        />
      </div>

      <div className="grid grid-cols-3 gap-4">
        <section className="col-span-2 rounded-xl border border-[#2A2D35] bg-[#111318] p-5 shadow-card">
          <div className="mb-4">
            <h3 className="text-base font-semibold text-text-primary">Settlement Breach Activity</h3>
            <p className="text-xs text-text-secondary">Last 14 days</p>
          </div>
          <div className="h-[240px]">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="breachGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#FF4D4D" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#FF4D4D" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke="#2A2D35" strokeDasharray="3 3" />
                <XAxis dataKey="date" tick={{ fill: '#8B92A5', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: '#8B92A5', fontSize: 11 }} axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#1A1D24', border: '1px solid #2A2D35', borderRadius: 8 }}
                  labelStyle={{ color: '#F0F2F5' }}
                  itemStyle={{ color: '#FF4D4D' }}
                />
                <Area type="monotone" dataKey="count" stroke="#FF4D4D" strokeWidth={2} fill="url(#breachGradient)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="rounded-xl border border-[#2A2D35] bg-[#111318] p-5 shadow-card">
          <h3 className="mb-4 text-base font-semibold text-text-primary">AI System Status</h3>
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
            <div className="h-2 overflow-hidden rounded bg-[#1A1D24]">
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
          </div>
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
