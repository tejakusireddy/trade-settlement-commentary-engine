CREATE TABLE ai_usage_audit (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  commentary_id UUID NOT NULL REFERENCES commentaries(id),
  model VARCHAR(100) NOT NULL,
  tokens_input INT NOT NULL,
  tokens_output INT NOT NULL,
  cost_usd DECIMAL(10,6) NOT NULL,
  latency_ms BIGINT NOT NULL,
  prompt_version VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_audit_commentary_id ON ai_usage_audit(commentary_id);
CREATE INDEX idx_ai_audit_created_at ON ai_usage_audit(created_at);
