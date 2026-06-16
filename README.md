# sbp-router-management

Administrative service for SBP traffic observability: it ingests the proxied
GCSvc traffic published by `sbp-router` and exposes it for querying.

## Scope

`sbp-router` publishes the raw request and response of every proxied GCSvc
transaction to the `sbp-router-traffic` Kafka topic. This service consumes that
topic, correlates each request/response pair into a transaction row in Postgres
schema `sbp_router_management` (Flyway-managed), applies time-based retention,
and exposes an internal admin API to list/search transactions, fetch a single
transaction (with raw XML), and read aggregate stats.

> **Routing-config-admin removed (flat-proxy sync, 2026-06-16).** `sbp-router`
> was reduced to a flat single-backend proxy that no longer consumes routing
> manifests, so this service's former routing-config catalog (upstreams,
> extraction-rules, terminals, tkb-pay, routing flags) and routing-manifest
> compilation/publishing were removed from the main line. They remain in git
> history; the richer content-router variant is preserved on the `sbp_router`
> `feature/sbp-rollout` branch. The config-admin Flyway tables (V1/V2) are left
> in place but dormant.

## Stack

- Java 21
- Spring Boot 4.0.6
- Spring Cloud 2025.1.1
- Spring MVC, Validation, Actuator
- Spring Cloud Config Client and Vault Config
- Spring for Apache Kafka (traffic consumer)
- Postgres (schema `sbp_router_management`), Flyway
- springdoc-openapi

## Base package

`ru.copperside.sbprouter.management`

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8087` | HTTP port |
| `PAY_ENVIRONMENT` | `local` | PAY_ALL environment label |
| `CONFIG_SERVER_ENABLED` | `false` locally, `true` in test/prod | Enables Spring Cloud Config |
| `CONFIG_SERVER_URL` | `http://pay-payconfig-server:8080` | Config Server URL |
| `CONFIG_SERVER_LABEL` | `${pay.environment}` | Config Server Git label |
| `VAULT_ENABLED` | `false` locally, `true` in test/prod | Enables Vault Config |
| `VAULT_KV_BACKEND` | `pay` | Vault KV backend |
| `VAULT_KV_CONTEXTS` | `${PAY_ENVIRONMENT}/sbp-router-management-db-password` | Vault application contexts |
| `SBP_ROUTER_MANAGEMENT_DB_URL` | `jdbc:postgresql://localhost:5432/pay_admin?currentSchema=sbp_router_management` | Local fallback DB URL |
| `SBP_ROUTER_MANAGEMENT_DB_USERNAME` | `pay_admin` | Local fallback DB user |
| `SBP_ROUTER_MANAGEMENT_DB_PASSWORD` | empty | Local fallback DB password; test/compose loaded from Vault |
| `SBP_ROUTER_MANAGEMENT_SERVICE_NAME` | `SBP Router Management` | Display name for this service |
| `KAFKA_ENABLED` | `false` locally, `true` in compose | Enables the `sbp-router-traffic` consumer |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `TRAFFIC_RETENTION_DAYS` | `30` | Days of traffic to retain before scheduled purge |

Vault secret path (compose contour): `pay/compose/sbp-router-management-db-password`.

## Run

Local standalone run keeps Config Server and Vault disabled and uses local
defaults or `SBP_ROUTER_MANAGEMENT_DB_*` environment overrides:

```powershell
mvn spring-boot:run
```

## Traffic ingest & query

The Kafka consumer (gated by `KAFKA_ENABLED`, at-least-once with manual ack)
reads `request`/`response` events from `sbp-router-traffic`, keyed by SBP
`correlationId` (or `txId` fallback). Each pair is correlated by upsert into one
transaction row (`PENDING` → `RESPONDED`, latency from the request/response
timestamps). A scheduled job purges transactions older than
`TRAFFIC_RETENTION_DAYS`. Only headers + raw XML are stored; payload bodies are
not parsed.

Internal admin API (`/internal/v1/sbp-router-management`, guarded by
`X-Internal-Admin-Key`):

- `GET /traffic/transactions` — list/search (filters, paging); list rows omit raw XML.
- `GET /traffic/transactions/{correlationId}` — single transaction with raw request/response XML.
- `GET /traffic/stats` — aggregate stats (throughput, latency, outcomes) over a window.

Health: [http://localhost:8087/actuator/health](http://localhost:8087/actuator/health)

### Docker Compose contour

The full local contour is owned by `../infra/docker-compose.yaml`.
The compose service name is `pay-sbp-router-management`.

```powershell
cd ..\infra
docker compose up -d --build pay-sbp-router-management
```

The container uses `SPRING_PROFILES_ACTIVE=compose`, Config Server label
`compose`, Postgres at `postgres:5432`, and Vault secret
`pay/compose/sbp-router-management-db-password`.

OpenAPI:

- [http://localhost:8087/v3/api-docs](http://localhost:8087/v3/api-docs)
- [http://localhost:8087/swagger-ui.html](http://localhost:8087/swagger-ui.html)

Contract: [../contracts/sbp-router-management](../contracts/sbp-router-management)

## Tests

```powershell
mvn clean verify
```
