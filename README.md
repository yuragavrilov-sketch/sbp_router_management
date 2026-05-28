# sbp-router-management

Administrative service for SBP routing configuration catalog and routing-manifest compilation.

## Scope

The service exposes an internal admin API for the draft→publish lifecycle of SBP
routing configuration and compiles all `ACTIVE` routing configuration into a
versioned, checksummed routing manifest. Data is stored in Postgres schema
`sbp_router_management` with Flyway migrations.

`sbp-router` currently loads its routing configuration statically from YAML /
Config Server. Consumption of routing manifests produced by this service is a
future increment (see ADR-0005). This service owns the administrative source of
truth for routing configuration; `sbp-router` is not changed by this service.

## Stack

- Java 21
- Spring Boot 4.0.6
- Spring Cloud 2025.1.1
- Spring MVC, Validation, Actuator
- Spring Cloud Config Client and Vault Config
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
| `CONFIG_SERVER_URL` | `http://pay-config:8080` | Config Server URL |
| `CONFIG_SERVER_LABEL` | `${pay.environment}` | Config Server Git label |
| `VAULT_ENABLED` | `false` locally, `true` in test/prod | Enables Vault Config |
| `VAULT_KV_BACKEND` | `pay` | Vault KV backend |
| `VAULT_KV_CONTEXTS` | `${PAY_ENVIRONMENT}/sbp-router-management-db-password` | Vault application contexts |
| `SBP_ROUTER_MANAGEMENT_DB_URL` | `jdbc:postgresql://localhost:5432/pay_admin?currentSchema=sbp_router_management` | Local fallback DB URL |
| `SBP_ROUTER_MANAGEMENT_DB_USERNAME` | `pay_admin` | Local fallback DB user |
| `SBP_ROUTER_MANAGEMENT_DB_PASSWORD` | empty | Local fallback DB password; test/compose loaded from Vault |
| `SBP_ROUTER_MANAGEMENT_SERVICE_NAME` | `SBP Router Management` | Display name for this service |

Vault secret path (compose contour): `pay/compose/sbp-router-management-db-password`.

## Run

Local standalone run keeps Config Server and Vault disabled and uses local
defaults or `SBP_ROUTER_MANAGEMENT_DB_*` environment overrides:

```powershell
mvn spring-boot:run
```

## Routing config entities

The service manages five configuration entity types via draft→publish lifecycle:

- **Upstreams** — target HTTP endpoints for SBP GCSvc routing.
- **Extraction rules** — per message-type field-binding rules that extract
  routing fields from GCSvc XML.
- **Terminal routing config** — singleton configuration for terminal-level routing
  field names and TKB Pay prefix.
- **TKB Pay list entries** — allow-list of `rcvTspId` values identifying TKB Pay
  transactions.
- **Routing flags** — key/value configuration flags.

Each entity supports `list`, `create`, and `patch` operations. Create and patch
operate on drafts. `GET /internal/v1/sbp-router-management/routing-config/pending-changes`
returns all `DRAFT` and `removal=true` entries across all entity types.

`POST /internal/v1/sbp-router-management/routing-config/discard-drafts` discards
all pending drafts and removals, restoring the active state.

## Routing manifests

`POST /internal/v1/sbp-router-management/routing-manifests` compiles all `ACTIVE`
routing configuration into a versioned, checksummed routing manifest.

The compiler validates consistency before publishing:

- All extraction-rule field bindings must be valid.
- A routing manifest is rejected with `ROUTING_MANIFEST_CONFLICT` if the active
  configuration is inconsistent.
- A manifest with the same checksum (identical content) as an existing manifest
  is rejected as a duplicate.

Read endpoints:

- `GET /internal/v1/sbp-router-management/routing-manifests/latest`
- `GET /internal/v1/sbp-router-management/routing-manifests/{manifestId}`

Full test-profile startup is owned by `../infra/run-test.ps1`: non-secret
database config is loaded from Config Server branch `test`, and
`spring.datasource.password` is loaded from Vault path
`pay/test/sbp-router-management-db-password`.

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
