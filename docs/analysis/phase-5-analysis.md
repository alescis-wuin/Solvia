# Phase 5 JavaFX MVP analysis

## Objective

Phase 5 turns the local backend into a usable desktop MVP. The goal is to provide the first daily workflow for consulting net worth, creating accounts and entering manual financial events.

## Scope mapping

| Phase item | Implementation |
| --- | --- |
| 5.1 Shell application | `SolviaDesktopApplication` and `SolviaShell` |
| 5.2 Sidebar navigation | `SolviaShell` sidebar pages |
| 5.3 Dashboard screen | `DashboardView` |
| 5.4 Accounts list | `AccountsView` table |
| 5.5 Account form | `AccountsView` creation form |
| 5.6 Snapshot form | `EntriesView` account snapshot form |
| 5.7 Cash-flow form | `EntriesView` cash-flow form |
| 5.8 Net-worth chart | JavaFX `LineChart` in `DashboardView` |
| 5.9 Period selector | `DatePicker` controls in `DashboardView` |
| 5.10 Aggregation selector | bucket and aggregation `ComboBox` controls |
| 5.11 Allocation view | allocation table in `DashboardView` |

## Architecture

The desktop module remains a client of the local REST API. It does not access the PostgreSQL database directly and does not duplicate backend calculation logic.

Implemented layers:

- `api`: Java records and asynchronous `SolviaApiClient` based on `java.net.http`;
- `ui`: JavaFX screens and formatting helpers;
- `SolviaDesktopApplication`: composition root.

## Runtime assumptions

The MVP expects the backend to be running on `http://127.0.0.1:8080`.

Backend:

```bash
mvn -pl solvia-backend spring-boot:run
```

Desktop:

```bash
mvn -pl solvia-desktop javafx:run
```

## Design notes

The UI uses standard JavaFX controls only: `BorderPane`, `VBox`, `GridPane`, `HBox`, `TableView`, `DatePicker`, `ComboBox` and `LineChart`.

The HTTP client is asynchronous so backend requests do not block the JavaFX application thread. UI updates are marshalled back through `Platform.runLater`.

## Deferred work

Deferred items: backend process supervision, remote authentication, JavaFX integration tests, advanced styling, asset and position creation forms, offline cache and jlink/jpackage packaging.
