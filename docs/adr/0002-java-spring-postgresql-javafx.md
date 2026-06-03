# ADR 0002: Use Java 25, Spring Boot 4, PostgreSQL and JavaFX

## Status

Accepted

## Context

The project needs a stable Java baseline, a reusable HTTP backend, exact storage
for financial values and a desktop UI for the first usable version.

## Decision

Use:

- Java 25 as the source and runtime baseline;
- Spring Boot 4 for the backend;
- PostgreSQL for persistence in later phases;
- JavaFX for the desktop application.

## Consequences

The project can evolve toward a web client without replacing the backend.
PostgreSQL supports exact numeric types for monetary data. JavaFX keeps the first
desktop version simple while the API matures.
