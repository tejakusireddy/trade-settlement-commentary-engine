import { clsx } from 'clsx';
import { format, isBefore, parseISO } from 'date-fns';
import { Eye, Plus, Search, X } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { SkeletonLoader } from '../../components/ui/SkeletonLoader';
import { breachApi, tradeApi } from '../../services/api';
import { useAuth } from '../../auth/AuthContext';
import type { CreateTradeRequest, Trade } from '../../types';

type TradeStatusFilter = 'ALL' | 'PENDING' | 'SETTLED' | 'BREACHED';

const defaultFormState: CreateTradeRequest = {
  tradeId: '',
  instrument: '',
  tradeDate: '',
  expectedSettlementDate: '',
  counterparty: '',
  quantity: 0,
  price: 0,
  currency: 'USD',
  idempotencyKey: crypto.randomUUID(),
};

export default function TradesPage() {
  const queryClient = useQueryClient();
  const { sessionKey } = useAuth();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<TradeStatusFilter>('ALL');
  const [showNewTradeModal, setShowNewTradeModal] = useState(false);
  const [selectedTrade, setSelectedTrade] = useState<Trade | null>(null);
  const [form, setForm] = useState<CreateTradeRequest>(defaultFormState);

  const {
    data: trades = [],
    isLoading: tradesLoading,
    isError: tradesError,
  } = useQuery({
    queryKey: ['trades', sessionKey],
    queryFn: () => tradeApi.list(),
    refetchInterval: 30000,
  });

  const { data: breaches = [] } = useQuery({
    queryKey: ['breaches', sessionKey],
    queryFn: () => breachApi.list(),
    refetchInterval: 15000,
  });

  const createTradeMutation = useMutation({
    mutationFn: (payload: CreateTradeRequest) => tradeApi.create(payload),
    onSuccess: () => {
      toast.success('Trade ingested successfully');
      setShowNewTradeModal(false);
      setForm({ ...defaultFormState, idempotencyKey: crypto.randomUUID() });
      void queryClient.invalidateQueries({ queryKey: ['trades', sessionKey] });
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : 'Failed to create trade');
    },
  });

  const filteredTrades = useMemo(() => {
    const needle = search.trim().toLowerCase();
    return [...trades]
      .filter((trade) => {
        const textMatch =
          !needle ||
          trade.tradeId.toLowerCase().includes(needle) ||
          trade.instrument.toLowerCase().includes(needle) ||
          trade.counterparty.toLowerCase().includes(needle);
        const statusMatch = statusFilter === 'ALL' || trade.status === statusFilter;
        return textMatch && statusMatch;
      })
      .sort((a, b) => parseISO(b.createdAt).getTime() - parseISO(a.createdAt).getTime());
  }, [search, statusFilter, trades]);

  const relatedBreaches = useMemo(
    () =>
      selectedTrade
        ? breaches.filter(
            (breach) =>
              breach.tradeId === selectedTrade.tradeId || breach.tradeId === selectedTrade.id,
          )
        : [],
    [breaches, selectedTrade],
  );

  const submitNewTrade = () => {
    createTradeMutation.mutate(form);
  };

  const controlsClass =
    'rounded-lg border border-border-subtle bg-bg-base px-3 py-2 text-sm text-text-primary outline-none placeholder:text-text-tertiary transition-colors hover:border-border-focus';

  return (
    <div className="relative h-full space-y-4">
      <div className="flex items-center justify-between rounded-lg border border-border-subtle bg-bg-surface p-4">
        <div className="relative w-[420px]">
          <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-text-tertiary" />
          <input
            className={clsx(controlsClass, 'w-full pl-9')}
            placeholder="Search trade ID, instrument, counterparty..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        <div className="flex items-center gap-2">
          {(['ALL', 'PENDING', 'SETTLED', 'BREACHED'] as const).map((status) => (
            <button
              key={status}
              type="button"
              onClick={() => setStatusFilter(status)}
              className={clsx(
                'rounded-lg border px-3 py-2 text-xs transition-colors',
                statusFilter === status
                  ? 'border-primary/20 bg-primary/10 text-primary'
                  : 'border-border-subtle bg-bg-raised text-text-secondary hover:text-text-primary',
              )}
            >
              {status}
            </button>
          ))}
          <button
            type="button"
            onClick={() => setShowNewTradeModal(true)}
            className="flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-black transition-colors hover:bg-primary/90"
          >
            <Plus className="h-4 w-4" />
            New Trade
          </button>
        </div>
      </div>

      <div className="overflow-hidden rounded-lg border border-border-subtle bg-bg-surface">
        <div className="grid grid-cols-9 border-b border-border-subtle px-4 py-3 text-[11px] uppercase tracking-[0.12em] text-text-secondary">
          <span>Trade ID</span>
          <span>Instrument</span>
          <span>Counterparty</span>
          <span>Trade Date</span>
          <span>Settlement Date</span>
          <span className="text-right">Quantity</span>
          <span className="text-right">Price</span>
          <span>Status</span>
          <span className="text-center">Actions</span>
        </div>

        {tradesLoading ? (
          <div className="space-y-2 p-4">
            <SkeletonLoader variant="row" />
            <SkeletonLoader variant="row" />
            <SkeletonLoader variant="row" />
          </div>
        ) : tradesError ? (
          <div className="p-6 text-sm text-danger">Unable to load trades right now.</div>
        ) : !filteredTrades.length ? (
          <div className="p-6 text-sm text-text-secondary">No trades match the current filters.</div>
        ) : (
          filteredTrades.map((trade) => {
            const settlementOverdue =
              trade.status !== 'SETTLED' && isBefore(parseISO(trade.expectedSettlementDate), new Date());
            return (
              <div
                key={trade.id}
                className={clsx(
                  'grid cursor-pointer grid-cols-9 items-center border-b border-border-subtle px-4 py-2 text-[13px] transition-colors hover:bg-bg-raised last:border-0',
                  selectedTrade?.id === trade.id && 'border-l-2 border-l-border-focus bg-bg-raised',
                )}
                onClick={() => setSelectedTrade(trade)}
              >
                <button
                  type="button"
                  className="w-fit font-mono text-xs text-info transition-colors hover:text-text-primary"
                >
                  {trade.tradeId}
                </button>
                <span className="font-medium text-text-primary">{trade.instrument}</span>
                <span className="text-text-secondary">{trade.counterparty}</span>
                <span className="font-mono tabular-nums text-text-primary">
                  {format(parseISO(trade.tradeDate), 'MMM dd, yyyy')}
                </span>
                <span
                  className={clsx(
                    'font-mono tabular-nums',
                    settlementOverdue ? 'text-warning' : 'text-text-primary',
                  )}
                >
                  {format(parseISO(trade.expectedSettlementDate), 'MMM dd, yyyy')}
                </span>
                <span className="font-mono tabular-nums text-right text-text-primary">
                  {trade.quantity.toLocaleString()}
                </span>
                <span className="font-mono tabular-nums text-right text-text-primary">
                  {new Intl.NumberFormat('en-US', { style: 'currency', currency: trade.currency }).format(
                    trade.price,
                  )}
                </span>
                <div>
                  <StatusBadge status={trade.status} />
                </div>
                <div className="flex justify-center">
                  <button
                    type="button"
                    className="rounded border border-border-subtle bg-bg-base p-1.5 text-text-secondary transition-colors hover:text-text-primary"
                  >
                    <Eye className="h-4 w-4" />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {showNewTradeModal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm">
          <div className="w-[560px] rounded-xl border border-border-subtle bg-bg-surface p-5">
            <div className="mb-4 flex items-center justify-between border-b border-border-subtle pb-4">
              <h3 className="text-lg font-semibold text-text-primary">Ingest New Trade</h3>
              <button
                type="button"
                onClick={() => setShowNewTradeModal(false)}
                className="rounded p-1 text-text-secondary transition-colors hover:bg-bg-raised hover:text-text-primary"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <input className={controlsClass} placeholder="Trade ID" value={form.tradeId} onChange={(e) => setForm((v) => ({ ...v, tradeId: e.target.value }))} />
              <input className={controlsClass} placeholder="Instrument" value={form.instrument} onChange={(e) => setForm((v) => ({ ...v, instrument: e.target.value }))} />
              <input className={controlsClass} placeholder="Counterparty" value={form.counterparty} onChange={(e) => setForm((v) => ({ ...v, counterparty: e.target.value }))} />
              <select className={controlsClass} value={form.currency} onChange={(e) => setForm((v) => ({ ...v, currency: e.target.value }))}>
                {['USD', 'EUR', 'GBP', 'JPY'].map((currency) => (
                  <option key={currency} value={currency}>
                    {currency}
                  </option>
                ))}
              </select>
              <input className={controlsClass} type="date" value={form.tradeDate} onChange={(e) => setForm((v) => ({ ...v, tradeDate: e.target.value }))} />
              <input className={controlsClass} type="date" value={form.expectedSettlementDate} onChange={(e) => setForm((v) => ({ ...v, expectedSettlementDate: e.target.value }))} />
              <input className={controlsClass} type="number" min={0} placeholder="Quantity" value={form.quantity || ''} onChange={(e) => setForm((v) => ({ ...v, quantity: Number(e.target.value) }))} />
              <input className={controlsClass} type="number" min={0} step="0.01" placeholder="Price" value={form.price || ''} onChange={(e) => setForm((v) => ({ ...v, price: Number(e.target.value) }))} />
            </div>
            <div className="mt-3 rounded-lg border border-border-subtle bg-bg-base px-3 py-2 text-xs text-text-secondary">
              Idempotency Key: <span className="font-mono text-text-primary">{form.idempotencyKey}</span>
            </div>
            <button
              type="button"
              onClick={submitNewTrade}
              disabled={createTradeMutation.isPending}
              className="mt-4 w-full rounded-lg bg-primary py-2 font-medium text-black transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {createTradeMutation.isPending ? 'Submitting...' : 'Submit Trade'}
            </button>
          </div>
        </div>
      ) : null}

      {selectedTrade ? (
        <div className="absolute right-0 top-0 h-full w-96 border-l border-border-subtle bg-bg-surface p-5">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-base font-semibold text-text-primary">Trade Detail</h3>
            <button
              type="button"
              onClick={() => setSelectedTrade(null)}
              className="rounded p-1 text-text-secondary transition-colors hover:bg-bg-raised hover:text-text-primary"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="space-y-3">
            {[
              ['Trade ID', selectedTrade.tradeId],
              ['Instrument', selectedTrade.instrument],
              ['Counterparty', selectedTrade.counterparty],
              ['Trade Date', format(parseISO(selectedTrade.tradeDate), 'MMM dd, yyyy')],
              ['Settlement Date', format(parseISO(selectedTrade.expectedSettlementDate), 'MMM dd, yyyy')],
              ['Quantity', selectedTrade.quantity.toLocaleString()],
              [
                'Price',
                new Intl.NumberFormat('en-US', {
                  style: 'currency',
                  currency: selectedTrade.currency,
                }).format(selectedTrade.price),
              ],
              ['Status', selectedTrade.status],
            ].map(([label, value]) => (
              <div key={label as string}>
                <p className="text-xs uppercase text-text-secondary">{label}</p>
                <p className="font-mono text-sm tabular-nums text-text-primary">{value}</p>
              </div>
            ))}
          </div>

          <div className="mt-6">
            <h4 className="mb-2 text-xs uppercase tracking-wider text-text-secondary">Related Breaches</h4>
            <div className="space-y-2">
              {relatedBreaches.length ? (
                relatedBreaches.map((breach) => (
                  <div key={breach.id} className="rounded-lg border border-border-subtle bg-bg-base p-2">
                    <div className="mb-1 flex items-center justify-between">
                      <StatusBadge status={breach.breachType} size="sm" />
                      <StatusBadge status={breach.status} size="sm" />
                    </div>
                    <p className="text-xs text-text-secondary">{breach.counterparty}</p>
                  </div>
                ))
              ) : (
                <p className="text-xs text-text-tertiary">No breaches linked to this trade.</p>
              )}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
