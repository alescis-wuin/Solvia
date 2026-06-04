# UI-2 dashboard component refactor

## Result

The dashboard refactor that was previously blocked has been applied in smaller steps.

`DashboardView` is now a thin orchestrator. It delegates rendering and layout to focused components:

| Component | Responsibility |
| --- | --- |
| `DashboardFilters` | Date range, bucket, aggregation and refresh action. |
| `DashboardMetrics` | Net worth, gross change and flow-adjusted gain. |
| `DashboardChartCard` | Net-worth history line chart. |
| `DashboardAllocationCard` | Allocation table by asset type. |
| `StateMessage` | Loading, empty, success, warning and error states. |

## Behavior changes

- Business data is not reloaded on every periodic backend readiness check.
- The dashboard reloads automatically only when the backend becomes ready or when no data has been loaded yet.
- Manual refresh remains available from `DashboardFilters`.
- Invalid periods are rejected before calling the backend.
- Backend unavailable states clear metrics, chart and allocation consistently.

## Accessibility and maintainability

- Dashboard controls use `Ui.fieldLabel(...)` and `Ui.tooltip(...)`.
- The chart keeps a stable `accessibleText` but avoids a fragile role enum that may vary between JavaFX versions.
- The allocation table has accessible text and standardized column setup.
- Dashboard-specific rendering code is isolated, making UI-3 dashboard improvements smaller and safer.

## Remaining work

- Add JavaFX smoke tests for desktop startup and dashboard construction.
- Add visual QA on the packaged application.
- Add richer chart tooltips and date-aware X axis labels in UI-3.
