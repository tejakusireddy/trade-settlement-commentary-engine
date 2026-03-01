CREATE TABLE settlement_breaches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  trade_id UUID NOT NULL,
  instrument VARCHAR(100) NOT NULL,
  counterparty VARCHAR(100) NOT NULL,
  breach_type VARCHAR(10) NOT NULL,
  breach_reason VARCHAR(50) NOT NULL,
  detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  days_overdue INT NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING_COMMENTARY'
);

CREATE INDEX idx_breaches_trade_id ON settlement_breaches(trade_id);
CREATE INDEX idx_breaches_status ON settlement_breaches(status);
CREATE INDEX idx_breaches_breach_type ON settlement_breaches(breach_type);
CREATE INDEX idx_breaches_detected_at ON settlement_breaches(detected_at);
