# ADR 0001: Use a layered multi-module architecture

## Status

Accepted

## Context

Solvia must support a backend, a JavaFX desktop application and a later web
client. The domain model contains the most durable rules and must not be coupled
to UI, persistence or transport details.

## Decision

Use a Maven multi-module layout with these initial modules:

- `solvia-domain`;
- `solvia-application`;
- `solvia-backend`;
- `solvia-desktop`.

The domain module remains framework-free. The application layer coordinates use
cases. The backend exposes HTTP adapters. The desktop module owns JavaFX UI code.

## Consequences

This introduces a little more ceremony at the start, but protects the domain
model from Spring, JavaFX and database dependencies.
