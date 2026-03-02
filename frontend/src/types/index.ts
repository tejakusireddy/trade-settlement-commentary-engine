export type TradeStatus = 'PENDING' | 'SETTLED' | 'BREACHED' | 'FAILED';
export type BreachType = 'T2' | 'T3' | 'T5';
export type BreachReason =
  | 'MISSING_ASSIGNMENT'
  | 'FAILED_ALLOCATION'
  | 'COUNTERPARTY_FAILURE'
  | 'INSUFFICIENT_FUNDS'
  | 'SYSTEM_ERROR';
export type GenerationType = 'AI' | 'TEMPLATE';
export type CircuitBreakerStatus = 'CLOSED' | 'OPEN' | 'HALF_OPEN';

export interface Trade {
  id: string;
  tradeId: string;
  stableTradeId?: string;
  instrument: string;
  tradeDate: string;
  expectedSettlementDate: string;
  counterparty: string;
  quantity: number;
  price: number;
  currency: string;
  status: TradeStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTradeRequest {
  tradeId: string;
  instrument: string;
  tradeDate: string;
  expectedSettlementDate: string;
  counterparty: string;
  quantity: number;
  price: number;
  currency: string;
  idempotencyKey: string;
}

export interface Breach {
  id: string;
  tradeId: string;
  instrument: string;
  counterparty: string;
  breachType: BreachType;
  breachReason: BreachReason;
  daysOverdue: number;
  detectedAt: string;
  status: string;
}

export interface Commentary {
  id: string;
  breachId: string;
  content: string;
  generationType: GenerationType;
  promptVersion: string;
  approvedBy: string | null;
  approvedAt: string | null;
  createdAt: string;
}

export interface AiCostSummary {
  dailyCostUsd: number;
  dailyCapUsd: number;
  percentUsed: number;
  circuitBreakerStatus: CircuitBreakerStatus;
}

export interface AiUsageDailyPoint {
  day: string;
  costUsd: number;
  callCount: number;
  tokensInput: number;
  tokensOutput: number;
}

export interface AiUsageCall {
  commentaryId: string;
  model: string;
  tokensInput: number;
  tokensOutput: number;
  costUsd: number;
  latencyMs: number;
  promptVersion: string;
  createdAt: string;
}

export interface AiUsageHistory {
  daily: AiUsageDailyPoint[];
  recentCalls: AiUsageCall[];
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}
