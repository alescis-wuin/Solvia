# UI-2 desktop design system and accessibility pass

## Objective

UI-2 introduces a small JavaFX design system so the desktop UI stops duplicating visual structure and status handling inside each screen.

The goal is not a full visual redesign yet. The goal is to create stable primitives before the dashboard and future asset or position screens grow further.

## Research basis

JavaFX nodes expose accessibility-related properties such as accessible role, accessible text and accessible help. JavaFX controls also support CSS style classes and pseudo-class driven states such as focused, hover, disabled and related interaction states.

WCAG 2.2 was used as a practical checklist for visible focus, readable labels, target sizing, and clear error/status messaging. Solvia is a desktop app, not a web page, but the same usability constraints apply to financial data entry.

## Implemented UI primitives

| Component | Purpose |
| --- | --- |
| `Ui` | Small helper for labels, help text, field labels, tooltips and style-class assignment. |
| `SectionCard` | Reusable titled section wrapper. |
| `MetricCard` | Reusable metric block for dashboard-style values. |
| `StateMessage` | Reusable status component for loading, empty, success, warning and error messages. |

## CSS changes

The Solvia stylesheet now defines design tokens and reusable classes for:

- section cards;
- metric cards;
- state messages;
- field labels;
- body/help text;
- disabled control opacity;
- visible focus outlines;
- table headers;
- backend status states.

## Applied views

### Accounts

`AccountsView` now uses:

- `SectionCard` for the account creation form;
- `SectionCard` for the accounts table;
- `StateMessage` for loading, empty, success and error states;
- labels linked to controls through `Ui.fieldLabel(...)`;
- tooltips on inputs and the create action.

### Entries

`EntriesView` now uses:

- `SectionCard` for account snapshots;
- `SectionCard` for cash flows;
- `StateMessage` for validation, loading, success and backend errors;
- labels linked to controls;
- tooltips on all data-entry inputs;
- clearer amount validation feedback.

### Dashboard

The dashboard already had cards, metric styling and state labels from UI-1. It now benefits from the extended CSS tokens. A deeper dashboard refactor should be done in UI-3 because UI-3 is specifically about making the dashboard the central patrimonial page.

## Deferred work

- Refactor `DashboardView` into `MetricCard`, `SectionCard` and `StateMessage` during UI-3.
- Add JavaFX smoke tests.
- Add a small form layout helper to avoid repeated `GridPane` code.
- Add input-specific validation components for currency codes and amounts.
- Add keyboard traversal and screen-reader checks on a packaged build.
