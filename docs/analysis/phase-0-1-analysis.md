# Phase 0 and Phase 1 analysis

## Context

The product brief describes Solvia as a wealth tracking application, not as a
simple spreadsheet of balances. The central design constraint is historical
correctness: values, flows and calculations must remain separate.

## Phase 0 assessment

The repository initially contained only a root `pom.xml`, a minimal `README.md`
and IDE metadata. The `ai` branch existed, while the uppercase `AI` ref did not.

The initial root POM declared a single-module Maven project with Java 26 source
and target values. That was not sufficient for the requested V1 foundation
because it provided no domain boundary, no backend launcher, no desktop launcher,
no database development service and no CI workflow.

## Phase 0 implementation strategy

The project is now structured as a Maven multi-module workspace:

- `solvia-domain`: pure domain model and domain tests;
- `solvia-application`: use-case boundary, intentionally thin at this stage;
- `solvia-backend`: empty Spring Boot HTTP backend, bound locally by default;
- `solvia-desktop`: empty JavaFX desktop shell.

Java 25 is used as the Maven release target because it is the long-term baseline
identified for the project, while still allowing developers to run the project
on newer compatible JDKs.

## Phase 1 assessment

The domain model must represent both bank-like products and investment-like
products. The important split is:

- an `Account` holds cash, positions or value snapshots;
- an `Asset` describes what is owned or tracked;
- a `Position` links an account to an asset quantity;
- snapshots capture observed value at a date;
- cash flows capture external movements, income, fees and transfers;
- market prices and FX rates are reference data for later calculations.

## Phase 1 implementation strategy

The first domain layer intentionally avoids persistence annotations and framework
dependencies. This keeps the model testable and reusable by the backend, desktop
and future web-facing use cases.

Implemented building blocks:

- monetary primitives: `MoneyAmount`, `CurrencyCode`, `Percentage`;
- classification enums: account, asset, envelope and flow types;
- core entities: `Account`, `Asset`, `Position`;
- historical entities: account snapshots, position snapshots and cash flows;
- reference data: market prices and FX rates;
- validation helpers and unit tests.

## Deferred work

The following items remain intentionally deferred to later phases:

- database schema and Flyway migrations;
- REST DTOs and input validation adapters;
- calculation engine for net worth and performance;
- JavaFX forms and charts;
- CSV/JSON import/export;
- encrypted backups.
