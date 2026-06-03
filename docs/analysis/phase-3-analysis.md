# Phase 3 API backend analysis

## Objective

Phase 3 exposes the V1 manual data-entry model through a local REST API. The API
must allow desktop and future web clients to create and read the data needed for
accounts, assets, snapshots and cash flows.

## Scope mapping

| Phase item | Implementation |
| --- | --- |
| 3.1 CRUD accounts | `AccountController` |
| 3.2 CRUD assets | `AssetController` |
| 3.3 Account snapshots | `AccountSnapshotController` |
| 3.4 Position snapshots | `PositionSnapshotController` |
| 3.5 Cash flows | `CashFlowController` |
| 3.6 OpenAPI | static `/api/openapi.yaml` |
| 3.7 Errors and validation | DTO constraints and `ApiExceptionHandler` |
| 3.8 API tests | standalone MVC controller tests |

## Additional implementation

A `PositionController` is included even though it is not explicitly listed in
phase 3. It is required for a complete API because position snapshots need a
position identifier, and positions link accounts to assets.

## Design notes

The API uses request/response DTOs instead of exposing persistence rows directly.
Records are mapped explicitly to the domain model and repositories. Deletes are
implemented as soft deactivation for accounts, assets and positions because the
application stores historical financial data.

The OpenAPI file is served by the backend from classpath resources. This provides
a contract without coupling phase 3 to a generator dependency.

## Deferred work

- pagination and filtering;
- optimistic concurrency;
- authenticated remote mode;
- generated OpenAPI from annotations;
- import/export API endpoints;
- calculation endpoints from phase 4.
