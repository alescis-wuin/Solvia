# Solvia

Solvia is a local-first wealth tracking application for personal finance data.

## Product direction

Solvia tracks account balances, savings products, listed investments, crypto-assets,
non-listed assets, cashback and other gains through immutable historical entries.

The application intentionally separates:

- entered values;
- money flows;
- calculated valuations and performance metrics.

This prevents deposits, withdrawals, dividends and transfers from being confused with
actual investment performance.

## V1 scope

The first version focuses on manual data entry and reliable calculations:

- accounts and envelopes;
- assets and positions;
- account and position snapshots;
- important cash flows;
- net-worth history;
- period-based charts;
- custom aggregation steps;
- CSV/JSON export and backup foundations;
- local-first security defaults.

Out of scope for V1: bank synchronization, order placement, investment advice,
complete tax automation, mobile applications, multi-user cloud hosting and real-time
market data.

## Technical baseline

| Area | Decision |
| --- | --- |
| Language | Java 25 |
| Backend | Spring Boot 4 |
| Desktop | JavaFX |
| Database | PostgreSQL |
| Migrations | Flyway |
| Persistence access | Spring JDBC `JdbcClient` |
| API style | REST, OpenAPI later |
| Build | Maven multi-module |
| Security stance | Local-first, no external telemetry |

## Repository layout

```text
Solvia/
├─ solvia-domain/
├─ solvia-application/
├─ solvia-infrastructure-persistence/
├─ solvia-backend/
├─ solvia-desktop/
├─ docs/
│  ├─ adr/
│  └─ analysis/
├─ compose.yaml
└─ pom.xml
```

## Build

```bash
mvn clean verify
```

## Run backend

```bash
mvn -pl solvia-backend spring-boot:run
```

The backend binds to `127.0.0.1` by default.

## Run desktop shell

```bash
mvn -pl solvia-desktop javafx:run
```

## Local PostgreSQL

```bash
docker compose up -d postgres
```

Default local credentials are development-only and must not be reused outside local
developer machines.
