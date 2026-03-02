# Trade Settlement Commentary Engine — Master Architecture Document

> **Reference this document in every Cursor prompt.**
> Every decision here is final unless explicitly revised.

---

## Mission Statement

Automate the manual ops workflow of writing management commentary for trade settlement breaches (T+2/T+3/T+5). When trades breach settlement deadlines, this system detects the breach, classifies the reason, and uses Claude AI to generate professional management-ready commentary — the same narrative an ops analyst would write manually, produced in seconds.

---

## Non-Functional Requirements

| Requirement | Decision |
|-------------|----------|
| Breach detection latency | Near real-time (< 5 seconds from trade event to breach record) |
| Commentary generation SLA | < 10 seconds under normal load |
| Claude API fallback | Template-based commentary if Claude is down (circuit breaker) |
| Duplicate trade events | Idempotency keys, silent deduplication via Redis (24h TTL) |
| Daily AI cost cap | Hard stop via Redis accumulator, falls back to templates |
| Kafka consumer failure | Dead letter queue after 3 retries, manual replay |
| Data retention | 7 years (financial compliance standard) |
| Auth | OAuth2/OIDC, JWT, RBAC with 3 roles |

---

## Tech Stack

| Layer | Technology | Version | Why |
|-------|-----------|---------|-----|
| Language | Java | 21 | Virtual threads, records, pattern matching |
| Framework | Spring Boot | 3.x | JPMorgan standard |
| Messaging | Apache Kafka | 3.x | Exactly-once, replayable |
| Schema | Avro + Schema Registry | Latest | Type safety, schema evolution |
| Primary DB | PostgreSQL | 16 | ACID, financial data |
| Search + Audit | ElasticSearch | 8.x | Full-text, audit trail |
| Cache | Redis | 7 | Rate limiting, idempotency, cost tracking |
| Auth | Keycloak | Latest | OAuth2/OIDC |
| Resilience | Resilience4j | Latest | Circuit breaker, retry, bulkhead |
| AI | Claude API | claude-sonnet-4-6 | Commentary generation |
| Build | Maven | Multi-module | JPMorgan standard |
| Testing | JUnit 5 + Testcontainers | Latest | Real infra in tests |
| Observability | Micrometer + Prometheus + Grafana | Latest | Production metrics |
| Containers | Docker + Docker Compose | Latest | Local dev + deployment |
| Frontend | React + TypeScript + Vite | Latest | Modern, fast |
| UI Components | shadcn/ui + Tailwind CSS | Latest | Customized dark theme |
| State Management | Zustand | Latest | Lightweight |
| Charts | Recharts | Latest | Custom dark styled |

---

## Architecture Overview

```
Frontend (React + TS + Vite + Tailwind)
        ↓
API Gateway Service (Spring Boot 3, Java 21)
        ↓ JWT via Keycloak
   ┌────┴────┬──────────────┐
Trade    Breach        Commentary
Ingest   Detector      Service
Service  Service       (Claude API)
   └────┬────┴──────────────┘
        ↓
   Kafka (4 topics + DLQ)
        ↓
   PostgreSQL + ElasticSearch + Redis
        ↓
   Prometheus + Grafana (observability)
```

---

## Microservices

### 1. trade-ingest-service
- **Responsibility:** Accept trade events via REST, validate, publish to Kafka
- **Port:** 8082
- **Kafka:** Produces to `trade.events`
- **Key patterns:** Idempotency key validation (Redis), Avro schema validation, fail-fast on bad input
- **Endpoints:**
  - `POST /api/v1/trades` — ingest single trade
  - `POST /api/v1/trades/batch` — ingest batch
  - `GET /api/v1/trades/{id}` — get trade by ID

### 2. breach-detector-service
- **Responsibility:** Consume trade events, apply settlement calendar logic, detect T+2/T+3/T+5 breaches
- **Port:** 8083
- **Kafka:** Consumes `trade.events`, `commentary.completed`, `commentary.approved`; produces to `trade.breaches`
- **Key patterns:** Settlement calendar (weekends/holidays excluded), breach reason classification, DLQ after 3 retries
- **Breach reasons:** `MISSING_ASSIGNMENT`, `FAILED_ALLOCATION`, `COUNTERPARTY_FAILURE`, `INSUFFICIENT_FUNDS`, `SYSTEM_ERROR`

### 3. commentary-service
- **Responsibility:** Consume breach events, generate AI commentary via Claude API, store with full audit
- **Port:** 8084
- **Kafka:** Consumes `trade.breaches`, produces to `commentary.completed` and `commentary.approved`
- **Key patterns:**
  - Resilience4j circuit breaker around Claude API
  - Fallback to structured template commentary
  - Every Claude call logged (tokens in/out, cost, latency, model, prompt version)
  - Daily cost cap via Redis accumulator
  - Prompt versioning

### 4. api-gateway-service
- **Responsibility:** Single entry point, JWT validation, RBAC, rate limiting, routing
- **Port:** 8080
- **Key patterns:** JWT validation per request, role-based access, Redis sliding window rate limiting, request/response audit logging

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `trade.events` | trade-ingest-service | breach-detector-service | Raw trade ingestion |
| `trade.breaches` | breach-detector-service | commentary-service | Detected breaches |
| `commentary.requests` | commentary-service internal | commentary-service internal | Commentary generation queue |
| `commentary.completed` | commentary-service | breach-detector-service | Commentary generated (breach workflow transition) |
| `commentary.approved` | commentary-service | breach-detector-service | Commentary approved (breach workflow transition) |
| `trade.dlq` | Any service | Manual consumer | Dead letter queue |

**Kafka Config:**
- Exactly-once semantics on all producers
- Manual offset commits on all consumers (commit only after successful DB write)
- Avro schemas for all messages
- 3 partitions per topic (scalable)
- Replication factor: 3 (production), 1 (local dev)

---

## Database Schema

```sql
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
    breach_type VARCHAR(10) NOT NULL,      -- T2, T3, T5
    breach_reason VARCHAR(50) NOT NULL,    -- MISSING_ASSIGNMENT etc
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    days_overdue INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_COMMENTARY'
);

-- commentaries: Generated commentary
CREATE TABLE commentaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    breach_id UUID NOT NULL REFERENCES settlement_breaches(id),
    content TEXT NOT NULL,
    generation_type VARCHAR(20) NOT NULL,  -- AI, TEMPLATE
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
```

---

## Resilience Patterns

### Circuit Breaker (Resilience4j) — Claude API
```
Failure threshold:     5 failures in 30 seconds → OPEN
Wait duration:         60 seconds before HALF-OPEN
Success threshold:     3 successes → CLOSED
Fallback:              Template-based commentary
```

### Idempotency
- Every trade event carries `idempotency_key` (UUID generated by client)
- Redis stores processed keys with 24h TTL
- Duplicate events: 200 OK, silently acknowledged, not reprocessed

### Dead Letter Queue
- Kafka message fails processing: retry 3 times with exponential backoff
- After 3 failures: publish to `trade.dlq` with full error context + original payload
- Separate DLQ consumer for monitoring and manual replay

### Cost Circuit Breaker
- Daily Claude API spend tracked in Redis key: `ai:cost:daily:{date}`
- Hard cap: $10.00/day (configurable via env var)
- If cap hit: all commentary requests fall back to templates
- Redis key TTL: resets at midnight UTC
- Alert fires to ops team via log + metric

### Backpressure
- Kafka consumers: manual offset commit
- Only commit after: successful DB write + ElasticSearch index + downstream publish
- Consumer group lag monitored via Prometheus

---

## RBAC Roles

| Role | Permissions |
|------|------------|
| `ops-user` | View trades, view breaches, generate commentary, edit commentary |
| `compliance-officer` | All ops-user permissions + approve commentary + export reports |
| `admin` | All permissions + configure cost caps + view AI usage dashboard |

---

## Frontend Architecture

### Design Language: Dark Financial Terminal
Inspired by Bloomberg Terminal + Citadel internal dashboards. Data-dense, precise, premium.

### Runtime Flow
- Frontend calls API Gateway only (`VITE_API_BASE_URL=http://localhost:8080`).
- Frontend must not call service ports directly in normal runtime flow.

### Color System
```css
--bg-base:          #0A0B0D;   /* near black background */
--bg-surface:       #111318;   /* cards, panels */
--bg-raised:        #1A1D24;   /* elevated elements */
--border:           #2A2D35;   /* subtle borders */

--primary:          #00D4AA;   /* teal — success, settled */
--danger:           #FF4D4D;   /* red — breach, error */
--warning:          #FFB547;   /* amber — T+3 warning */
--info:             #4D9EFF;   /* blue — informational */
--ai-accent:        #7C6EF8;   /* purple — Claude AI elements only */

--text-primary:     #F0F2F5;   /* near white */
--text-secondary:   #8B92A5;   /* muted labels */
--text-tertiary:    #4A5060;   /* disabled/placeholder */
```

### Typography
- UI labels: **Inter**
- Numbers/financial data: **JetBrains Mono** (alignment in tables)
- Headers: **Inter Bold**

### Pages
1. **Dashboard** — KPI cards, live breach activity chart, recent breaches table
2. **Trades** — Full trade blotter, filterable, sortable, ElasticSearch powered
3. **Breach Management** — Active breaches grouped by severity, generate/edit/approve commentary
4. **Commentary Report** — Date-picker, management-ready report, PDF export, AI vs Template badge
5. **AI Usage & Cost** — Daily spend chart, token breakdown, circuit breaker status, cost cap progress
6. **Audit Trail** — ElasticSearch powered, every event timestamped

### Frontend Project Structure
```
frontend/
├── src/
│   ├── components/
│   │   ├── ui/           (shadcn base, heavily customized)
│   │   ├── charts/       (Recharts wrappers, dark themed)
│   │   ├── tables/       (Trade blotter, breach table)
│   │   └── layout/       (Sidebar, header, page wrapper)
│   ├── pages/
│   │   ├── Dashboard/
│   │   ├── Trades/
│   │   ├── Breaches/
│   │   ├── Reports/
│   │   ├── AiUsage/
│   │   └── Audit/
│   ├── hooks/            (useBreaches, useTrades, useCommentary)
│   ├── services/         (Typed API clients)
│   ├── store/            (Zustand global state)
│   ├── types/            (TypeScript interfaces mirroring backend models)
│   └── utils/
```

### UI Principles
- **Tables:** Dense, monospaced numbers, subtle row hover, no unnecessary whitespace
- **Status badges:** Pill-shaped, color + text (never color alone)
- **Charts:** Dark background, teal/red/amber lines, Recharts only
- **AI elements:** Purple `#7C6EF8` accent on anything Claude-powered
- **Loading states:** Skeleton loaders, never spinners
- **No frontend without TypeScript types** — every API response typed

---

## Monorepo Project Structure

```
trade-settlement-commentary-engine/
├── services/
│   ├── trade-ingest-service/
│   │   ├── src/
│   │   ├── pom.xml
│   │   └── Dockerfile
│   ├── breach-detector-service/
│   │   ├── src/
│   │   ├── pom.xml
│   │   └── Dockerfile
│   ├── commentary-service/
│   │   ├── src/
│   │   ├── pom.xml
│   │   └── Dockerfile
│   └── api-gateway-service/
│       ├── src/
│       ├── pom.xml
│       └── Dockerfile
├── shared/
│   ├── common-models/       (shared DTOs, enums, exceptions)
│   ├── kafka-schemas/       (Avro .avsc files)
│   └── security-config/     (shared JWT config)
├── frontend/
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
├── infrastructure/
│   ├── docker-compose.yml
│   ├── docker-compose.monitoring.yml
│   ├── keycloak/
│   │   └── realm-export.json
│   └── kafka/
│       └── create-topics.sh
├── docs/
│   ├── architecture.md      (this file)
│   └── adr/                 (Architecture Decision Records)
│       ├── ADR-001-java21.md
│       ├── ADR-002-kafka-exactly-once.md
│       └── ADR-003-claude-circuit-breaker.md
├── pom.xml                  (parent Maven POM)
└── README.md
```

---

## Build Order (Strict — Do Not Deviate)

| Step | What | Why |
|------|------|-----|
| 1 | Docker Compose infrastructure | Foundation everything runs on |
| 2 | Parent Maven POM + shared modules | Contract everything depends on |
| 3 | Avro schemas | Message contracts between services |
| 4 | trade-ingest-service | Get data flowing |
| 5 | breach-detector-service | Core business logic |
| 6 | commentary-service | AI layer |
| 7 | api-gateway-service | Expose everything |
| 8 | Frontend shell + routing | UI foundation |
| 9 | Frontend pages (Dashboard first) | Page by page |
| 10 | Observability (Prometheus + Grafana) | Production readiness |

---

## Commit Convention

```
feat(service-name): description
fix(service-name): description
chore(infra): description
test(service-name): description
docs: description
```

Examples:
- `feat(trade-ingest): add idempotency key validation with Redis`
- `feat(breach-detector): implement T+5 settlement calendar logic`
- `feat(commentary): add Resilience4j circuit breaker around Claude API`
- `chore(infra): add docker-compose with Kafka, Postgres, Redis, ElasticSearch`

---

## Environment Variables (All Services)

```bash
# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_SCHEMA_REGISTRY_URL=http://localhost:8081

# PostgreSQL
POSTGRES_URL=jdbc:postgresql://localhost:5433/trade_settlement
POSTGRES_USERNAME=trade_user
POSTGRES_PASSWORD=<secret>

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# ElasticSearch
ELASTICSEARCH_HOST=localhost
ELASTICSEARCH_PORT=9200

# Keycloak
KEYCLOAK_URL=http://localhost:8180
KEYCLOAK_REALM=trade-settlement
KEYCLOAK_CLIENT_ID=trade-api

# Claude API
ANTHROPIC_API_KEY=<secret>
CLAUDE_MODEL=claude-sonnet-4-6
CLAUDE_DAILY_COST_CAP_USD=10.00

# Service ports
TRADE_INGEST_PORT=8082
BREACH_DETECTOR_PORT=8083
COMMENTARY_PORT=8084
API_GATEWAY_PORT=8080

# Frontend ingress
VITE_API_BASE_URL=http://localhost:8080
```

---

## Key Architectural Decisions (ADRs)

### ADR-001: Java 21
Virtual threads (Project Loom) for high-throughput Kafka consumers without reactive complexity. Records for immutable DTOs. Pattern matching for cleaner code.

### ADR-002: Kafka Exactly-Once Semantics
Financial data cannot be double-processed. Idempotent producers + transactional consumers. Higher latency tradeoff accepted.

### ADR-003: Circuit Breaker on Claude API
Claude is a third-party dependency. Template fallback ensures the system never blocks on AI unavailability. Commentary may be less eloquent but ops workflow never stops.

### ADR-004: Redis for Idempotency Keys
24h TTL, O(1) lookup, horizontal scaling. Preferred over DB-based idempotency for performance at ingestion layer.

### ADR-005: Monorepo
Single repo for all services. Shared modules, unified CI/CD, easier cross-service refactoring. Trade-off: larger clone size, acceptable for this scale.

### ADR-006: Avro over JSON for Kafka
Schema enforcement at publish time. Schema evolution without breaking consumers. Required for production financial systems.

### ADR-007: shadcn/ui with heavy customization
Not using shadcn defaults. Every component restyled to dark financial terminal aesthetic. shadcn chosen for accessibility primitives and TypeScript support.

---

*Last updated: 2026-02-28*
*Owner: tejakusireddy*
*Repo: github.com/tejakusireddy/trade-settlement-commentary-engine*
