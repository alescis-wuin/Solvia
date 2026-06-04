# UI-1 backend readiness and desktop state analysis

## Objective

UI-1 makes the desktop application explicit about the local backend state before loading financial data.

The implemented user-facing states are:

| State | Meaning | Desktop behavior |
| --- | --- | --- |
| `CHECKING` | The desktop is probing `/api/readiness`. | Data views remain disabled until the result is known. |
| `CONNECTED` | Backend and PostgreSQL schema are reachable. | Views can load accounts, entries and dashboard data. |
| `DEGRADED` | Backend is reachable but PostgreSQL or migrations are not ready. | Views remain disabled and the banner explains the issue. |
| `UNAVAILABLE` | The desktop cannot connect to the backend URL. | Views remain disabled and the banner suggests starting the backend or fixing the URL. |
| `ERROR` | Local URL validation or unexpected state failed. | Views remain disabled and the banner shows the error. |

## Backend changes

`/api/readiness` now returns a structured response containing:

- global status;
- service name;
- timestamp;
- backend status;
- database status;
- user-readable message.

The readiness check validates the JDBC connection and triggers retryable Flyway migration through `DatabaseMigrationInitializer` when needed. Hikari no longer hard-fails immediately if PostgreSQL is late, so the backend can start and report a degraded state instead of leaving the desktop with only a connection failure.

## Desktop changes

Implemented desktop pieces:

- `BackendConnectionState`: normalized desktop state enum;
- `BackendStatusSnapshot`: immutable status snapshot consumed by views;
- `SolviaApiException`: HTTP/API error wrapper that extracts readable backend messages;
- `SolviaApiClient.readiness()`: timeout-bound readiness probe;
- `SolviaDesktopPreferences`: persisted backend URL preference;
- `BackendStatusBanner`: visible global banner with URL field, retry and apply actions;
- `SolviaShell`: hosts the status banner;
- `DashboardView`, `AccountsView`, `EntriesView`: disable loading/actions while backend is not ready.

## UX decisions

- The backend URL defaults to `http://127.0.0.1:8080`.
- Users can change the URL from the banner without restarting the application.
- Data views do not call account, dashboard or entry endpoints until readiness is connected.
- API and connection errors are converted into concise user-facing messages.
- The banner refreshes automatically and exposes a manual retry button.

## Remaining work

- Add JavaFX smoke tests.
- Add generated OpenAPI or contract tests so `/api/readiness` and the static OpenAPI file cannot drift.
- Avoid repeated business-data refreshes on every periodic readiness check; views should refresh automatically only on a disconnected-to-connected transition.
- Add richer empty-state components rather than status labels only.
- Add an optional backend auto-start strategy after the desktop packaging phase.
