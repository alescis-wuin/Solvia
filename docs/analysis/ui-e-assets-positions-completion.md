# UI-E assets and positions completion

## Objective

UI-E completes the asset and position workflow before moving to UI-4.x.

UI-D introduced the first desktop screen for creating assets, positions and position snapshots. UI-E extends it so the workflow is usable for everyday maintenance: edit, deactivate, inspect details, review position valuation history and avoid duplicate active positions.

## Implemented scope

| Area | Implementation |
| --- | --- |
| Asset editing | Added update DTO/client method and UI action for updating the selected asset. |
| Asset deactivation | Added client delete call and UI action that deactivates the selected asset through the existing backend soft-delete endpoint. |
| Position editing | Added update DTO/client method and UI action for updating selected position quantity. |
| Position deactivation | Added client delete call and UI action that deactivates the selected position. |
| Snapshot history | Added `positionSnapshots(...)` client method and a snapshot history table for the selected position. |
| Duplicate detection | UI blocks creation when an active position already exists for the selected account and asset. |
| Detail panels | Added selected asset and selected position details. |
| Dashboard refresh | `AssetsPositionsView` exposes `setOnPortfolioDataChanged(...)`; the desktop app wires it to `DashboardView.refresh()`. |
| Contextual help | Asset type selector now displays business help text for each asset type. |

## User workflow

### Maintain an asset

The user can select an asset from the table. Selection fills the asset form and the detail panel.

Available actions:

- create a new asset;
- update the selected asset;
- deactivate the selected asset;
- clear the selection and start a new asset.

Deactivation preserves history because the backend uses soft deactivation instead of destructive deletion.

### Maintain a position

The user can select a position from the table. Selection fills the position form, detail panel and snapshot position selector.

Available actions:

- create a new position;
- update selected position quantity;
- deactivate selected position;
- clear the selection and start a new position.

Before creating a position, the UI checks whether an active position already exists for the same account and asset.

### Review and update valuation history

Selecting a position loads its valuation history through:

```text
GET /api/positions/{positionId}/snapshots
```

Saving a new valuation snapshot refreshes:

- the local asset/position screen;
- the selected position history;
- the dashboard, through the callback wired in `SolviaDesktopApplication`.

## Technical notes

The backend already had the necessary REST endpoints for update, soft deactivation and position snapshots. UI-E therefore changes only the desktop client and desktop views.

The screen keeps JavaFX standard controls:

- `TableView` for assets, positions and snapshot history;
- `ComboBox` for account, asset and position selection;
- `TextField` / `DatePicker` for data entry;
- `StateMessage` and `EmptyState` for feedback.

All backend responses still update JavaFX state via `Platform.runLater(...)`.

## Deferred work

- Dedicated modal confirmation before deactivation.
- Inline table actions instead of selection-driven buttons.
- Better responsive behavior when the window is narrow.
- Position snapshot editing or correction workflow.
- Advanced duplicate handling for inactive positions that should be reactivated rather than recreated.
- Event bus instead of direct dashboard refresh callback when more screens start changing portfolio data.
