CREATE TABLE commentaries (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  breach_id UUID NOT NULL,
  trade_id UUID NOT NULL,
  content TEXT NOT NULL,
  generation_type VARCHAR(20) NOT NULL,
  prompt_version VARCHAR(20) NOT NULL,
  approved_by VARCHAR(100),
  approved_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_commentaries_breach_id ON commentaries(breach_id);
CREATE INDEX idx_commentaries_trade_id ON commentaries(trade_id);
CREATE INDEX idx_commentaries_generation_type ON commentaries(generation_type);
CREATE INDEX idx_commentaries_created_at ON commentaries(created_at);
