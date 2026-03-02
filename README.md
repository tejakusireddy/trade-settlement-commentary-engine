# Trade Settlement Breach & Commentary Engine

Automates the highest-friction post-trade operations loop: detect settlement breaches in real time and generate management-ready commentary with full auditability and cost controls.

![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-6DB33F?logo=springboot&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-Event%20Streaming-231F20?logo=apachekafka&logoColor=white)
![Claude AI](https://img.shields.io/badge/Claude-claude--sonnet--4--6-5A67D8)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

---

## The Problem

Settlement failures are expensive, noisy, and operationally manual. In equity and fixed-income workflows, trades that miss expected settlement deadlines (T+2/T+3/T+5) trigger escalations, management reporting, and often regulatory scrutiny. Every breach needs context: what failed, who owns resolution, how much exposure exists, and what remediation is underway.

In most operations teams, this is still a human pipeline: analysts pull trade context from multiple systems, classify root cause, write commentary for leadership, and keep repeating that process under deadline pressure. The bottleneck is not breach detection alone; it is the commentary and governance layer that follows every detected exception.

For high-volume books, each exception can consume 15-30 minutes of analyst time across investigation, drafting, review, and escalation. That creates queueing risk during volatile periods, exactly when management and compliance stakeholders need faster and more consistent signal quality.

This platform replaces that loop end-to-end. It ingests trade flow, detects breaches with settlement-calendar logic, classifies breach reasons, generates professional management commentary using Claude (`claude-sonnet-4-6`), and records every AI call with token/cost/latency telemetry. The result is faster incident understanding, cleaner reporting, and operational throughput without sacrificing control.

---

## Architecture Diagram

```text
┌───────────────────────────────────────────── Frontend (React + TypeScript + Vite) :5173 ─────────────────────────────────────────────┐
│                                      Dark terminal UI for Trades, Breaches, Reports, AI Usage, Audit                              │
└──────────────────────────────────────────────────────────────────────┬───────────────────────────────────────────────────────────────┘
                                                                       │ HTTPS / JWT
                                                                       ▼
┌──────────────────────────────────────────────────────────── API Gateway Service :8080 ────────────────────────────────────────────────┐
│ JWT validation (Keycloak JWKS) · RBAC (ops-user/compliance-officer/admin) · Redis sliding-window rate limit · X-Request-ID · audit │
└───────────────────────────────┬──────────────────────────────────────┬───────────────────────────────────────┬─────────────────────────┘
                                │                                      │                                       │
                                ▼                                      ▼                                       ▼
            ┌──────────────────────────────┐          ┌──────────────────────────────┐         ┌──────────────────────────────────┐
            │ trade-ingest-service :8082   │          │ breach-detector-service :8083 │         │ commentary-service :8084         │
            │ REST ingest + idempotency    │          │ settlement breach detection   │         │ Claude generation + approvals    │
            │ PostgreSQL writes            │          │ workflow status transitions    │         │ AI audit + cost controls         │
            └──────────────┬───────────────┘          └──────────────┬───────────────┘         └──────────────┬───────────────────┘
                           │                                          │                                        │
                           │ trade.events                             │ trade.breaches                         │ commentary.completed
                           │                                          │                                        │ commentary.approved
                           ▼                                          ▼                                        ▼
                    ┌──────────────────────────────────────────────────── Apache Kafka + Schema Registry ─────────────────────────────────┐
                    │ topics: trade.events · trade.breaches · commentary.completed · commentary.approved · commentary.requests · trade.dlq │
                    └────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

                    ┌───────────────────────────────┐  ┌─────────────────────┐  ┌──────────────────────────┐  ┌──────────────────────────┐
                    │ PostgreSQL 16 (shared state)  │  │ Redis 7             │  │ Elasticsearch 8 + Kibana │  │ Prometheus + Grafana     │
                    │ trades/breaches/commentaries  │  │ idempotency/rate/cost │ │ audit/search sink target  │  │ metrics, SLO telemetry   │
                    └───────────────────────────────┘  └─────────────────────┘  └──────────────────────────┘  └──────────────────────────┘

                    ┌───────────────────────────────┐                                ┌──────────────────────────────────────────────────────┐
                    │ Keycloak (OIDC/OAuth2) :8180  │                                │ Anthropic Claude API (claude-sonnet-4-6)           │
                    │ realm: trade-settlement        │                                │ circuit-breaker guarded + template fallback         │
                    └───────────────────────────────┘                                └──────────────────────────────────────────────────────┘
```

---

## Pipeline Flow

1. Trade arrives via `POST /api/v1/trades` at `trade-ingest-service`.
2. Idempotency key is checked in Redis (`24h TTL`) to suppress duplicates.
3. Trade is persisted to PostgreSQL (`trades` table).
4. `TradeEvent` is published to `trade.events` (Avro, idempotent producer config).
5. `breach-detector-service` consumes `trade.events` with manual offset commit.
6. Settlement calendar logic computes overdue days and classifies T+2/T+3/T+5 breach.
7. `BreachEvent` is published to `trade.breaches`.
8. `commentary-service` consumes breach, checks daily AI spend cap in Redis.
9. Claude API is called with structured breach context prompt (`claude-sonnet-4-6`).
10. Commentary + AI usage audit (tokens, cost, latency, model, prompt version) are persisted.
11. Commentary becomes available via REST and, once approved, breach status transitions to `COMMENTARY_APPROVED`.

---

## Engineering Highlights

- **Financial workflow awareness**: distinguishes detection from post-detection governance (commentary, approval, auditability).
- **Operational safety**: AI is wrapped in circuit breaker + fallback + hard cost cap, so model instability cannot halt breach handling.
- **Typed event contracts**: Avro schemas + registry-compatible records across all service boundaries.
- **Backpressure-ready consumers**: manual offset commits with explicit retry/DLQ behavior.
- **Security boundary discipline**: frontend is gateway-only; auth, RBAC, and rate limits are not duplicated across services.
- **Production-grade local parity**: Docker Compose stack includes Kafka, Schema Registry, PostgreSQL, Redis, Keycloak, Elasticsearch, Kibana.

---

## Services

| Service | Port | Responsibility | Key Patterns |
|---|---:|---|---|
| `trade-ingest-service` | 8082 | Ingest trade events, validate payloads, persist trades | Redis idempotency, Avro producer, Flyway + JPA |
| `breach-detector-service` | 8083 | Detect settlement breaches and maintain breach workflow status | Settlement calendar logic, manual Kafka commits, DLQ with retries/backoff |
| `commentary-service` | 8084 | Generate and approve management commentary | Claude integration, Resilience4j circuit breaker, template fallback, AI cost cap + audit |
| `api-gateway-service` | 8080 | Authn/authz, rate-limit, route requests, log/audit boundaries | Keycloak JWT validation, RBAC, Redis sliding-window limit, request correlation |

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `trade.events` | `trade-ingest-service` | `breach-detector-service` | Raw trade ingestion stream |
| `trade.breaches` | `breach-detector-service` | `commentary-service` | Detected settlement breaches |
| `commentary.completed` | `commentary-service` | `breach-detector-service` | Commentary generated status transition |
| `commentary.approved` | `commentary-service` | `breach-detector-service` | Commentary approved status transition |
| `commentary.requests` | internal/reserved | internal/reserved | Queue contract kept for extensibility |
| `trade.dlq` | all services | ops/manual replay | Dead-letter events with full failure context |

---

## Data Model (Shared PostgreSQL)

| Table | Purpose | Notable Indexes / Constraints |
|---|---|---|
| `trades` | Canonical trade record | unique `trade_id`, unique `idempotency_key`, status/date indexes |
| `settlement_breaches` | Detected settlement breaches | indexes on `trade_id`, `status`, `detected_at` |
| `commentaries` | AI/template commentary and approvals | index on `breach_id`, generation metadata |
| `ai_usage_audit` | Per-call AI telemetry | index on `created_at`, stores tokens/cost/latency/model/prompt version |

Migration governance:
- One shared schema across services.
- One global Flyway version namespace across the monorepo.
- Current baseline includes early schema versions (`V1`-`V4`) and later non-overlapping versions for cross-service evolution.

---

## Resilience & Production Readiness

### AI safety and continuity
- Claude calls are guarded by Resilience4j (`sliding-window-size=10`, `failure-rate-threshold=50`, `wait-duration-in-open-state=60s`, `permitted-number-of-calls-in-half-open-state=3`).
- If AI path fails or cost cap is exceeded, commentary generation falls back to deterministic templates.
- Outcome: breach workflow is never blocked by external model availability.

### Idempotency and duplicate suppression
- Trade ingestion enforces idempotency on `idempotency_key` with:
  - Redis fast-path lookup (`O(1)`)
  - PostgreSQL unique constraint on stored key
- Duplicate requests return success semantics without double-processing side effects.

### Kafka delivery discipline
- Consumers run with `enable-auto-commit=false` and `AckMode.MANUAL_IMMEDIATE`.
- Offsets commit only after successful domain processing.
- Retry envelope: 3 attempts with exponential backoff (`200ms`, `400ms`, `800ms`) before DLQ.

### Rate limiting and auth boundary
- Gateway enforces JWT validation against Keycloak and role-based authorization at endpoint level.
- Sliding-window rate limiting (`100 req/min/user`) implemented with Redis sorted sets.
- Request IDs are propagated and included in structured audit logs.

### Cost controls for AI operations
- Daily Claude spend tracked in Redis accumulator (`CLAUDE_DAILY_COST_CAP_USD`, default `$10.00`).
- Once cap is reached, generation shifts to template path automatically.
- Every AI call is persisted with token + latency + cost attributes (`ai_usage_audit`).

### Shared DB migration governance
- Services share a single PostgreSQL schema and a global Flyway history.
- Migration versions must be globally coordinated across services to avoid checksum/version collisions.

---

## AI-Generated Management Commentary (actual output)

> Trade ID relates to a TSLA equity position traded on 1 February 2026 with counterparty Morgan Stanley, which remains unsettled at T+5 and is currently 17 business days overdue. The breach has been attributed to a missing assignment, indicating that the requisite trade allocation or novation documentation has not been completed, preventing settlement from progressing. Immediate escalation to the Morgan Stanley relationship team is recommended to resolve the outstanding assignment, alongside a review of potential buy-in exposure and any regulatory reporting obligations arising from the extended settlement failure.

---

## Local Development (10-minute setup)

### Prerequisites
- Java 21
- Maven 3.9+
- Node.js 20+
- Docker + Docker Compose

### 1) Start infrastructure

```bash
docker compose -f infrastructure/docker-compose.yml up -d
bash infrastructure/kafka/create-topics.sh
```

### 2) Build backend modules

```bash
mvn clean install -DskipTests
```

### 3) Start backend services

Run each in its own terminal.

```bash
# Terminal 1
cd services/trade-ingest-service
mvn spring-boot:run

# Terminal 2
cd services/breach-detector-service
mvn spring-boot:run

# Terminal 3 (loads Anthropic env vars if present)
cd services/commentary-service
set -a; source ../../.env; set +a
mvn spring-boot:run

# Terminal 4
cd services/api-gateway-service
mvn spring-boot:run
```

### 4) Start frontend

```bash
cd frontend
npm install
npm run dev
```

Set frontend env (`frontend/.env`):

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=trade-settlement
VITE_KEYCLOAK_CLIENT_ID=trade-api
```

### 5) Ingest a sample trade and observe breach + commentary

```bash
# Get admin token from local Keycloak
TOKEN=$(curl -sS -X POST 'http://localhost:8180/realms/trade-settlement/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'client_id=trade-api' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'username=admin-test' \
  --data-urlencode 'password=admin-pass' | jq -r '.access_token')

# Ingest intentionally overdue trade
TRADE_ID=$(uuidgen | tr 'A-Z' 'a-z')
IDEMPOTENCY_KEY=$(uuidgen | tr 'A-Z' 'a-z')

curl -sS -X POST 'http://localhost:8080/api/v1/trades' \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{
    \"tradeId\": \"${TRADE_ID}\",
    \"instrument\": \"TSLA\",
    \"tradeDate\": \"2026-02-01\",
    \"expectedSettlementDate\": \"2026-02-03\",
    \"counterparty\": \"Morgan Stanley\",
    \"quantity\": 500,
    \"price\": 215.40,
    \"currency\": \"USD\",
    \"idempotencyKey\": \"${IDEMPOTENCY_KEY}\"
  }" | jq

# Poll breaches
curl -sS -H "Authorization: Bearer ${TOKEN}" \
  'http://localhost:8080/api/v1/breaches?page=0&size=20' | jq

# Poll commentaries
curl -sS -H "Authorization: Bearer ${TOKEN}" \
  'http://localhost:8080/api/v1/commentaries?page=0&size=20' | jq
```

---

## Testing & Verification

- Unit tests across services (`mvn -q test`) for controllers, domain services, and security filters.
- Testcontainers-backed integration profile available for infrastructure-realistic checks (`mvn -q verify -Pintegration-tests`).
- Frontend production compile gate (`cd frontend && npm run -s build`).
- Release verification script (`bash scripts/release-verify.sh`) to run default non-Docker release gates.

---

## Project Structure

```text
trade-settlement-commentary-engine/
├── services/
│   ├── trade-ingest-service/          # Trade ingest API, idempotency, trade.events producer
│   ├── breach-detector-service/       # Breach detection engine, workflow transitions, DLQ routing
│   ├── commentary-service/            # Claude integration, commentary APIs, AI usage audit
│   └── api-gateway-service/           # JWT/RBAC/rate-limit boundary, request routing, audit logging
├── shared/
│   ├── common-models/                 # Shared DTOs, enums, API envelope contracts
│   └── kafka-schemas/                 # Avro schemas + generated specific records
├── frontend/                          # React/TypeScript terminal-style operations UI
├── infrastructure/
│   ├── docker-compose.yml             # Local infra: Kafka, Schema Registry, Postgres, Redis, Keycloak, Elastic, etc.
│   ├── kafka/create-topics.sh         # Local topic bootstrap script
│   └── keycloak/realm-export.json     # Realm, client, role, and test-user bootstrap
├── scripts/
│   └── release-verify.sh              # Unit + frontend build release gate script
├── ARCHITECTURE.md                    # Master architecture and operating decisions
├── README.md
└── pom.xml                            # Parent Maven multi-module build
```

---

## Technology Decisions (ADR-style)

### 1) Java 21 as the baseline runtime
Java 21 gives a modern, stable platform for low-latency service code, concise domain models (records), and concurrency primitives that can scale consumers and I/O-heavy paths cleanly.

### 2) Kafka + Avro over direct service coupling
Breach detection and commentary generation are decoupled by replayable, typed event contracts. This gives backpressure tolerance, failure isolation, and long-lived schema evolution under change.

### 3) Hexagonal service boundaries
Domain logic is isolated from delivery infrastructure (HTTP/Kafka/DB), which keeps testing cheap and prevents framework concerns from leaking into core operations logic.

### 4) Shared PostgreSQL with global Flyway namespace
A single source of truth for trade/breach/commentary state simplifies consistency and reporting, while a global migration version namespace enforces cross-service DB governance discipline.

### 5) Fallback-first AI design
AI generation is explicitly treated as a dependency, not a prerequisite. Template fallback guarantees workflow continuity when model APIs fail, circuit opens, or cost limits are hit.

### 6) Redis as operational control plane
Redis backs idempotency, rate limiting, and cost controls because those concerns are latency-sensitive and stateful across requests and services.

### 7) Gateway-first security model
All frontend traffic terminates at the API gateway. Authorization, role checks, rate limits, and request tracing live at that boundary to reduce policy drift across services.

---

## What’s Next (Production Roadmap)

### Step 1: Breach reason enrichment from settlement reject codes
- Extend breach events with settlement reject metadata.
- Maintain reject-code → `BreachReason` mapping in externalized config (versioned, testable).
- Preserve source-system timestamps and original reject artifacts for audit chain integrity.

### Step 2: Settlement adapter for external feeds (DTCC/Euroclear/internal)
- Add adapter service for post-trade status feeds.
- Normalize external reject/status payloads into canonical internal events.
- Support replay, reconciliation windows, and feed-health monitoring.

### Additional production extensions
- Kubernetes deployment with service-level autoscaling and controlled rollout policy.
- Multi-region Kafka replication and failover strategy.
- WebSocket push channel for real-time breach/commentary state updates in UI.

---

## Author Note

The AI cost circuit breaker in this system is not academic. It is intentionally opinionated because uncontrolled agent workloads can become an operational incident, not just a billing issue. After seeing an unmonitored AI workflow generate a ~$47K cloud surprise in production, this architecture treats model usage as a first-class risk surface: hard caps, fallback paths, and full-call auditability are mandatory controls, not optional features.
