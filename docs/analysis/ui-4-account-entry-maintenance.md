# UI-4 account and entry maintenance

## Objective

UI-4 completes the daily manual-maintenance workflow around accounts and entries.

UI-1 made backend state explicit, UI-2 introduced reusable UI primitives, UI-3 rebuilt the patrimonial dashboard, and the existing asset/position work already covers investment-like data. The remaining UI-4 gap was the bank-like workflow: maintain accounts, inspect latest balances, review manual account entries and prevent common duplicate mistakes.

## Implemented scope

| Area | Implementation |
| --- | --- |
| Account DTOs | Added desktop `AccountUpdateDto`, `AccountSnapshotDto` and `CashFlowDto`. |
| Desktop API client | Added account update/deactivate calls and account snapshot / cash-flow history reads. |
| Accounts screen | `AccountsView` now supports create, update, soft deactivation, clear selection, detail panel and snapshot history. |
| Latest balance | Selecting an account loads snapshots and displays the latest known balance in the detail panel. |
| Entry histories | `EntriesView` now shows snapshot history and cash-flow history for the selected accounts. |
| Duplicate prevention | Account snapshot creation is blocked when the selected account already has a snapshot on the same date. |
| Flow duplicate prevention | Cash-flow creation is blocked when account, date, type, amount, currency and label are identical. |
| Flow sign rule | Cash-flow amounts are now strictly positive in the desktop UI; the flow type determines direction. |
| Dashboard refresh | Account snapshots and cash flows trigger a dashboard refresh through `setOnPortfolioDataChanged(...)`. |
| Cross-screen refresh | Account create/update/deactivation refreshes entries, assets/positions and dashboard data. |

## User workflow

### Maintain accounts

The user can select a row in the account table. The form is populated from the selected account and exposes:

- create a new account;
- update the selected account;
- deactivate the selected account;
- clear the selection and prepare a new account.

Deactivation keeps historical data intact because the backend uses the existing soft-deactivation endpoint.

### Inspect account values

Selecting an account loads:

```text
GET /api/accounts/{accountId}/snapshots
```

The detail panel shows the latest known balance, its date, the account status and the number of snapshots. The snapshot table keeps the historical entries visible so the user can verify whether the dashboard is based on recent data.

### Enter account snapshots

The snapshot form now checks the selected account history before submitting. If a snapshot already exists for the same date, the UI blocks the request and asks the user to avoid creating a confusing duplicate.

### Enter cash flows

The cash-flow form now uses a clear sign rule:

```text
amount > 0
direction = cash-flow type
```

This matches the current V1 calculation engine, which treats deposits and transfer-in as external capital additions, and withdrawals and transfer-out as external capital removals. Interest, dividends, fees, taxes, cashback and corrections remain non-external performance-context flows in V1.

The UI also checks exact duplicate cash flows for the selected account before submitting.

## Technical notes

The backend already exposed all required endpoints for this pass:

- `PUT /api/accounts/{id}`;
- `DELETE /api/accounts/{id}`;
- `GET /api/accounts/{accountId}/snapshots`;
- `GET /api/accounts/{accountId}/cash-flows`.

No backend schema or controller change was required.

JavaFX standard controls remain sufficient for UI-4:

- `TableView` for account, snapshot and cash-flow histories;
- `ComboBox` for account selection;
- `TextField` and `DatePicker` for controlled manual data entry;
- `StateMessage` and `EmptyState` for feedback.

All asynchronous backend responses still marshal UI mutations back onto the JavaFX application thread through `Platform.runLater(...)`.

## Deferred work

- Confirmation modal before account deactivation.
- Snapshot correction workflow instead of simple duplicate blocking.
- Cash-flow editing/deactivation once the backend exposes non-destructive correction semantics.
- Shared event bus instead of direct callbacks once more screens mutate portfolio data.
- Filter controls for long snapshot and cash-flow histories.
- Inline table actions after the first maintenance workflow is stable.
