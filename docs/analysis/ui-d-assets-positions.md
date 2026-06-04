# UI-D assets and positions workflow

## Objective

UI-D completes the first desktop workflow for investment-like data.

Before this step, the backend already exposed assets, positions and position snapshots, and UI-3 displayed allocation and valued accounts. The missing part was the desktop workflow that lets the user create and value these investment positions without leaving the application.

## Implemented scope

| Area | Implementation |
| --- | --- |
| Desktop DTOs | Added asset, position and position-snapshot DTOs in `ApiDtos`. |
| Desktop API client | Added methods for listing/creating assets, listing/creating positions and creating position snapshots. |
| UI screen | Added `AssetsPositionsView`. |
| Navigation | Added the `Actifs & positions` page to the desktop shell. |
| Dashboard shortcut | Added a dashboard shortcut to the new page. |
| Tests | Updated JavaFX smoke test to instantiate `AssetsPositionsView`. |

## User workflows

### Create an asset

The user can create an asset with:

- name;
- asset type;
- currency code;
- optional symbol.

The type selector follows the backend enum values and the table renders business labels through `DesktopFormatters.assetType(...)`.

### Create a position

The user can link an asset to an account with an initial quantity.

The view loads all accounts, all assets and all positions. Positions are loaded per account through the existing backend endpoint:

```text
GET /api/accounts/{accountId}/positions
```

### Create a position snapshot

The user can create a valuation snapshot for an existing position with:

- position;
- value date;
- observed quantity;
- total market value;
- market value currency.

The UI sends `OBSERVED` confidence for now, which matches the manual V1 workflow.

## UI behavior

- The screen is disabled while the backend is unavailable.
- Data is refreshed after asset, position and snapshot creation.
- Empty states distinguish missing backend, missing accounts and missing assets.
- Numeric and currency inputs reuse `InputValidation`.
- Tables use simple row models instead of embedding nodes in cells.

## Research basis

JavaFX `TableView` is intended to visualize rows of data split into columns, and supports table columns, resizing and sorting. This makes it a reasonable V1 control for assets and positions.

JavaFX `ComboBox` is appropriate for choosing existing domain objects such as accounts, assets and positions.

JavaFX `Platform.runLater(...)` is used because backend calls finish on background threads while UI mutation must be marshalled back to the JavaFX application thread.

WCAG 2.2 criteria relevant to this workflow include labels/instructions, target size, focus appearance and error prevention for financial data.

## Deferred work

- Editing/deactivating assets and positions from the desktop screen.
- Position snapshot history table.
- Duplicate detection before creating a position with the same account and asset.
- Richer helper text explaining each asset type.
- Dedicated asset and position detail screens.
- Dashboard refresh event after position snapshot creation.
