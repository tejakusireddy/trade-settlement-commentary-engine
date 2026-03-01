-- trades: Core trade record
CREATE TABLE trades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_id VARCHAR(100) UNIQUE NOT NULL,
    instrument VARCHAR(100) NOT NULL,
    trade_date DATE NOT NULL,
    settlement_date DATE,
    expected_settlement_date DATE NOT NULL,
    counterparty VARCHAR(100) NOT NULL,
    quantity DECIMAL(20,6) NOT NULL,
    price DECIMAL(20,6) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    idempotency_key VARCHAR(255) UNIQUE NOT NULL
);

-- settlement_breaches: Detected breaches
CREATE TABLE settlement_breaches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_id UUID NOT NULL REFERENCES trades(id),
    breach_type VARCHAR(10) NOT NULL,
    breach_reason VARCHAR(50) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    days_overdue INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_COMMENTARY'
);

-- commentaries: Generated commentary
CREATE TABLE commentaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    breach_id UUID NOT NULL REFERENCES settlement_breaches(id),
    content TEXT NOT NULL,
    generation_type VARCHAR(20) NOT NULL,
    prompt_version VARCHAR(20) NOT NULL,
    approved_by VARCHAR(100),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ai_usage_audit: Full AI cost audit
CREATE TABLE ai_usage_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    commentary_id UUID NOT NULL REFERENCES commentaries(id),
    model VARCHAR(100) NOT NULL,
    tokens_input INT NOT NULL,
    tokens_output INT NOT NULL,
    cost_usd DECIMAL(10,6) NOT NULL,
    latency_ms INT NOT NULL,
    prompt_version VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_trades_status ON trades(status);
CREATE INDEX idx_trades_trade_date ON trades(trade_date);
CREATE INDEX idx_breaches_status ON settlement_breaches(status);
CREATE INDEX idx_breaches_detected_at ON settlement_breaches(detected_at);
CREATE INDEX idx_commentaries_breach_id ON commentaries(breach_id);
CREATE INDEX idx_ai_audit_created_at ON ai_usage_audit(created_at);
