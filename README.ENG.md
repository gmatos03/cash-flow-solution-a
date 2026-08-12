# Cash Flow Control — Solution A (Event Sourcing + CQRS)

*[Leia em português](README.md)*

Companion source code for Appendix D and Appendix F of the *Cash-Flow Architecture Report*
(`Architecture1_Equivalent_Solutions.docx`). Four independent Spring Boot 3.2 / Java 17
microservices implement Solution A's write side, read side, projection, and daily
reporting, wired up to run entirely on **localhost** against Kafka, PostgreSQL, and Redis.

In AWS (Appendix D) these run on ECS/EKS behind an ALB, with Amazon MSK, Aurora
PostgreSQL, and ElastiCache for Redis. Locally, `docker-compose.yml` stands up the same
three pieces of infrastructure so the services behave identically on a laptop.

| Service                 | Port | Role                                                              | Public API |
|--------------------------|------|--------------------------------------------------------------------|------------|
| `command-service`        | 8081 | Validates entries, appends events, publishes to Kafka              | Yes (`/commands`) |
| `query-service`          | 8082 | Serves balances/statements/reports, Redis cache-aside               | Yes (`/queries`) |
| `event-handler-service`  | 8083 | Consumes Kafka, projects the read model, invalidates cache          | No (internal + `/actuator`) |
| `reporting-service`      | 8084 | Builds the daily cash-flow log (scheduled + on-demand)               | Yes (`/reports/run`) |

Each service is a fully independent Maven module — there is no shared JAR between them,
matching how they'd be built and deployed separately in Appendix D's ECS/EKS clusters.

## 1. Prerequisites

- Java 17+ (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Docker + Docker Compose

> This code was authored and reviewed in a sandboxed environment without Maven/Docker
> access, so it has not been compiled here. Everything follows standard Spring Boot 3.2 /
> Spring Kafka 3.1 / jjwt 0.12 APIs, but please run `mvn clean verify` after checking it
> out and open an issue-for-yourself on anything that doesn't build.

## 2. Start the infrastructure

```bash
docker compose up -d
docker compose ps   # wait until postgres, redis and kafka all report healthy
```

This creates a `cashflow` Postgres database (user/password `cashflow`/`cashflow`), a
single-node Kafka broker on `localhost:9092`, and Redis on `localhost:6379`.

## 3. Build and run the services

From the repository root, `mvn clean install` builds all four modules. Each one also runs
independently:

```bash
cd command-service   && mvn spring-boot:run    # http://localhost:8081
cd query-service     && mvn spring-boot:run    # http://localhost:8082
cd event-handler-service && mvn spring-boot:run # http://localhost:8083
cd reporting-service  && mvn spring-boot:run    # http://localhost:8084
```

The first service you start against a fresh database applies the Flyway migration
(`V1__init.sql`) that creates `event_store`, `accounts`, `ledger_entries`, and
`daily_cash_flow_log`, and seeds two demo accounts (`acc-10293847`, `acc-55510023`). The
other three services point at the same schema-history table, so their own copies of the
migration are safe no-ops (see the comment at the top of each `V1__init.sql`).

Each service also auto-creates the Kafka topics it needs (`ledger.credit-debit.events`,
`ledger.credit-debit.events.dlq`) on startup via a `NewTopic` bean.

## 4. Authenticate

Every public endpoint except `/auth/token` and `/actuator/health` requires a JWT. This is
a **local-development-only** substitute for the Amazon Cognito user pool described in
Appendix D/F — see the class comment on `JwtService` in each service. Demo credentials:
`demo` / `demo123`.

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")
```

(`command-service` and `query-service` each issue their own token against their own copy
of the demo user store — either works against either service, since both are signed with
the same shared local secret in `application.yml`.)

## 5. Exercise the write path

```bash
curl -s -X POST http://localhost:8081/commands/entries \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "acc-10293847",
    "amount": 1250.00,
    "currency": "USD",
    "type": "CREDIT",
    "channel": "WEB",
    "description": "Wire transfer received",
    "idempotencyKey": "'"$(uuidgen)"'"
  }' | python3 -m json.tool
```

You should get back `202 Accepted` with an `entryId`. Watch `event-handler-service`'s logs
— within milliseconds it should log that it projected the entry and evicted the balance
cache. Re-sending the exact same request body (same `idempotencyKey`) returns `409
Conflict` with the original `entryId` instead of creating a duplicate.

## 6. Exercise the read path

```bash
curl -s http://localhost:8082/queries/accounts/acc-10293847/balance \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

curl -s "http://localhost:8082/queries/accounts/acc-10293847/statement?from=2026-08-01&to=2026-08-10&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

The first balance read after a new entry reports `"source": "db"`; every read for the next
300 seconds (or until the next projection) reports `"source": "cache"`.

## 7. Build the daily cash-flow log

```bash
curl -s -X POST "http://localhost:8084/reports/run?date=$(date -u +%F)" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

curl -s "http://localhost:8082/queries/accounts/acc-10293847/daily-log/$(date -u +%F)" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

`reporting-service` also runs this automatically every day at 23:00 UTC
(`app.reporting.cron` in its `application.yml`). Either way, look under
`reporting-service/reports/{accountId}/{date}/` for the rendered `report.csv` and
`report.pdf` — this replaces the S3 report bucket used in the AWS deployment.

## 8. Run the tests

```bash
mvn test          # from the repo root, runs all four modules' JUnit 5 / Mockito suites
```

Each service has unit tests for its service layer (Mockito mocks for repositories, Kafka,
and Redis) and at least one MockMvc test for its controller(s), covering the happy path
plus the validation / conflict / not-found error cases documented in Appendix F.

## 9. API documentation (Swagger / OpenAPI)

`command-service`, `query-service`, and `reporting-service` each publish live, browsable
API docs via springdoc-openapi. `event-handler-service` has no public API (Kafka consumer
only), so it's excluded.

| Service            | Swagger UI                                  | Raw spec                          |
|---------------------|----------------------------------------------|------------------------------------|
| `command-service`   | http://localhost:8081/swagger-ui.html         | http://localhost:8081/v3/api-docs |
| `query-service`     | http://localhost:8082/swagger-ui.html         | http://localhost:8082/v3/api-docs |
| `reporting-service` | http://localhost:8084/swagger-ui.html         | http://localhost:8084/v3/api-docs |

`/swagger-ui/**` and `/v3/api-docs/**` are permitted without a token in `SecurityConfig`,
but the endpoints they describe are not — click "Authorize" in the UI, paste a bearer
token from `POST /auth/token`, and you can exercise the real API from the browser.

Each service also ships a hand-written, version-controlled copy of its spec at
`<service>/docs/openapi.yaml`, useful for generating clients or importing into Postman
without a running instance.

## 10. Project layout

```
cash-flow-solution-a/
  docker-compose.yml          # Postgres, Redis, Kafka (KRaft, single node)
  pom.xml                     # aggregator (reactor) pom
  command-service/
    docs/openapi.yaml         # static OpenAPI 3.0 spec
    src/main/java/.../{config,security,web,domain,service,repository,kafka}
    src/main/resources/{application.yml, db/migration/V1__init.sql}
    src/test/java/...
  query-service/      (same shape, read side, + docs/openapi.yaml)
  event-handler-service/ (same shape, Kafka consumer, no security/web-facing controller)
  reporting-service/  (same shape, + scheduler + PDF/CSV rendering, + docs/openapi.yaml)
```

## 11. What's simplified for local use (and why)

- **Auth**: a local HS256 JWT issuer (`/auth/token`) stands in for Amazon Cognito. Swapping
  in a real OAuth2 resource-server config against Cognito's JWKS endpoint is a drop-in
  replacement for `JwtService`/`JwtAuthenticationFilter` — nothing else changes.
- **Reporting delivery**: files are written to `./reports/...` on disk and a log line
  stands in for the SNS/SES notification in Appendix F.7, instead of S3 + email.
  `ReportRenderer.writeToLocalStorage` is the only place that would change.
- **Schema migrations**: each service ships an identical copy of `V1__init.sql` so any one
  of them can bootstrap a fresh database; Appendix F.6 describes running this as a
  dedicated one-off deploy step instead in a real environment.
- **Kafka topic sizing**: 3 partitions locally instead of the 12 documented in Appendix
  F.5, since a laptop-scale demo doesn't need that much parallelism.
