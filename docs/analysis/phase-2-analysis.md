# Phase 2 database analysis

## Objective

Phase 2 turns the pure domain model into durable PostgreSQL storage while keeping
the domain independent from persistence concerns.

## Scope mapping

| Phase item | Implementation |
| --- | --- |
| 2.1 Flyway migrations | `V1__create_initial_schema.sql` |
| 2.2 Accounts, assets, positions | `accounts`, `assets`, `positions` |
| 2.3 Snapshots and flows | `account_balance_snapshots`, `position_snapshots`, `cash_flows` |
| 2.4 Prices and FX rates | `market_prices`, `fx_rates` |
| 2.5 Import and audit | `import_batches`, `audit_logs` |
| 2.6 Repositories | JDBC repositories in `solvia-infrastructure-persistence` |
| 2.7 PostgreSQL integration tests | Testcontainers-backed repository tests |

## Design notes

The schema deliberately stores amounts as `numeric(38, 18)` instead of floating
point values. This preserves exactness for money, quantities, prices and exchange
rates.

Java enums are persisted as text with check constraints instead of PostgreSQL enum
types. This keeps migrations explicit when supported enum values evolve.

Snapshots and cash flows are append-oriented. Updates are not introduced in phase
2 because corrections should later be represented through additional records and
audit entries rather than destructive mutation.

## Deferred work

- full calculation queries;
- import deduplication logic;
- audit integration in service use cases;
- backup and restore tables or file format;
- schema indexes driven by production query plans.
