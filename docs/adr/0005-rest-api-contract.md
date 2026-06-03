# ADR 0005: Use explicit REST adapters and a static OpenAPI contract

## Status

Accepted

## Context

Phase 3 must expose the V1 manual entry use cases through HTTP while the product
is still evolving. The domain and persistence layers are intentionally explicit,
and the first API should follow that style.

## Decision

Implement REST controllers manually in the backend module. Use Jakarta validation
on request DTOs, central error handling and repository-backed adapters.

Publish an OpenAPI YAML document from the backend instead of introducing an
OpenAPI generation dependency at this stage.

## Consequences

The API contract is reviewable and stable in source control. The contract must be
kept in sync with controllers until a later phase decides whether generated
OpenAPI documentation is worth the additional dependency.
