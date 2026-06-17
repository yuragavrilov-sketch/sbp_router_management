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

> **Routing-config history.** The rich routing-config catalog (upstreams,
> extraction-rules, terminals, tkb-pay, routing flags, routing-manifest
> compilation/publishing) was removed from the main line on 2026-06-16. A slim
> managed routing-config — groups/backends only — was re-introduced on 2026-06-17
> and is consumed by the router end-to-end (see **Managed routing-config**
> below). The richer content-router variant is preserved on the `sbp_router`
> `feature/sbp-rollout` branch. The old config-admin Flyway tables (V1/V2) are
> left in place but dormant. See ADR-0005 (amended).

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
| `KAFKA_ENABLED` | `false` locally, `true` in compose | Enables the `sbp-router-traffic` **and** `sbp-router-heartbeat` (fleet) consumers |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `TRAFFIC_RETENTION_DAYS` | `30` | Days of traffic to retain before scheduled purge |
| `SBP_HEARTBEAT_TOPIC` | `sbp-router-heartbeat` | Router fleet heartbeat topic |
| `SBP_ROUTING_CONFIG_TOPIC` | `sbp-router-routing-config` | Topic the managed routing-config is published to (gated by `KAFKA_ENABLED`) |
| `FLEET_TTL` | `45s` | After this with no heartbeat an instance is reported `STALE` |

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

## Managed routing-config

This service owns a slim managed routing-config — groups and their backends
only, not the old extraction/manifest catalog. The document is:

```json
{ "version": 1, "activeGroup": "default", "groups": { "default": { "backends": ["http://..."] } } }
```

It is stored in the Postgres table `routing_config` (Flyway `V5`); on replace the
`version` is set to `max(version) + 1`.

Internal admin API (`/internal/v1/sbp-router-management`, guarded by
`X-Internal-Admin-Key`):

- `GET /routing-config` — current config (`404` if none has been set yet).
- `PUT /routing-config` — replace config (`200` with the persisted version;
  `400` on an invalid config; `503` if publishing to Kafka fails).

On `PUT` the service validates the config, bumps the version, persists it, and
publishes the full config to the compacted Kafka topic `sbp-router-routing-config`
(gated by `KAFKA_ENABLED`). Routers consume that topic and rebuild their
registries from it.

## Fleet view

When `KAFKA_ENABLED=true`, a second consumer reads the `sbp-router-heartbeat`
topic (each router pod publishes presence + metrics) into an in-memory registry,
exposed at:

- `GET /internal/v1/sbp-router-management/routers` — the running router fleet:
  `{ total, up, routers: [ { instanceId, status (UP|STALE), startedAt,
  lastHeartbeat, activeGroup, groups, backends, routingConfigVersion, metrics } ] }`.
  `routingConfigVersion` is the routing-config version each router has applied —
  desired-vs-actual: this service owns the desired version (see **Managed
  routing-config**), routers report the version they have actually applied. An
  instance with no heartbeat for `FLEET_TTL` is reported `STALE`; long-silent
  instances are purged. The registry is in-memory only (derived state, not
  persisted).

> Note: the fleet consumer uses a single shared group, so the view is complete on
> a single-instance deployment. A multi-replica `sbp-router-management` would need
> a per-instance consumer group for each replica to see the whole fleet — a
> follow-up if this service is ever scaled out.

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
