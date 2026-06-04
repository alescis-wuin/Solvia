# UI-4 deferred hardening

## Objective

This pass closes the UI-4 deferred items around manual portfolio maintenance.

The goal is to keep Solvia append-oriented and historically reliable while making destructive-looking actions explicit, making long histories usable, and making corrections possible without mutating historical rows.

## Implemented scope

| Deferred item | Resolution |
| --- | --- |
| Confirmation modal before deactivation | Added reusable `Ui.confirm(...)` and wired it before account, asset and position deactivation. |
| Snapshot correction workflow | Account snapshots and position snapshots can now be corrected by creating a new snapshot at the same value date. The original row is not modified. |
| Cash-flow correction workflow | Cash-flow rows expose a correction action that prepares a compensating append-only flow. External capital flows use the opposite flow type; other flows use `CORRECTION`. |
| Event bus | Added `DesktopEventBus` and routed cross-screen refresh through event publication from the desktop composition root. |
| Filters for long histories | Added filters for account lists, account snapshot histories, snapshot-entry histories, cash-flow histories, assets, positions and position valuation histories. |
| Inline table actions | Added inline action columns for accounts, snapshots, cash flows, assets, positions and position snapshots. |

## Append-only correction model

### Account snapshots

A correction does not update or delete the previous snapshot. Instead, Solvia creates a new snapshot on the same value date.

The existing calculation model already selects the latest snapshot by value date and then by recorded date, so the newest correction becomes the effective value while the previous observation remains auditable.

### Position snapshots

Position valuation corrections follow the same rule: a corrected valuation creates a new position snapshot at the same value date.

### Cash flows

Cash flows are not edited or deleted in UI-4. The correction action prepares a new compensating flow:

| Original type | Prepared correction type |
| --- | --- |
| `DEPOSIT` | `WITHDRAWAL` |
| `WITHDRAWAL` | `DEPOSIT` |
| `TRANSFER_IN` | `TRANSFER_OUT` |
| `TRANSFER_OUT` | `TRANSFER_IN` |
| Other types | `CORRECTION` |

This avoids pretending that historical data was mutable while still letting the user neutralize an incorrect entry.

## Validation basis

The UI remains based on JavaFX standard controls:

- `Alert` for confirmation dialogs;
- `TableView` and action columns for row-level maintenance;
- `TextField` filters for long histories;
- `Platform.runLater(...)` for JavaFX thread marshalling after asynchronous backend calls.

A unit test was added for `DesktopEventBus` subscription, publication and unsubscription behavior.

## Remaining product decisions

- Whether a backend-level correction endpoint should later create a first-class `CorrectionBatch` record.
- Whether duplicate snapshot warnings should become a richer correction wizard.
- Whether financial correction flows should support a direct relationship to the corrected source row.
- Whether inline actions should move to a shared reusable `ActionTableCell` once the UI grows further.
