# ADR 0003: Apply local-first security defaults

## Status

Accepted

## Context

Solvia handles financial and patrimonial data. Even the V1 development version
must avoid unsafe defaults.

## Decision

The backend binds to `127.0.0.1` by default. The project avoids telemetry and
treats exports, backups and later synchronization as explicit user actions.

## Consequences

Local development remains simple, but any future remote or multi-user deployment
will require a separate security decision covering authentication, authorization,
secret storage and backup encryption.
