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
