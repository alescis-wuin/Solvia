# ADR 0004: Use PostgreSQL migrations and explicit JDBC repositories

## Status

Accepted

## Context

Solvia stores financial history. The database schema must make monetary values,
quantities, dates, immutable observations and audit events explicit.

The domain module is intentionally framework-free. Persistence must therefore
adapt domain records to SQL rows without adding infrastructure annotations to the
domain model.

## Decision

Use PostgreSQL as the relational database, Flyway SQL migrations for schema
versioning and Spring JDBC `JdbcClient` for explicit repository implementations.

The schema uses UUID identifiers, `numeric` columns for monetary values,
quantities, prices and FX rates, and textual enum values constrained by check
constraints.

## Consequences

SQL remains visible and reviewable. Repository code is more verbose than generated
ORM repositories, but persistence decisions stay outside the domain model and are
easier to test against real PostgreSQL containers.
