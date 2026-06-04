# UI-3 patrimonial dashboard

## Objective

UI-3 turns the desktop dashboard into the main patrimonial home page instead of a simple chart screen.

The dashboard must let the user understand, in one view:

- current net worth;
- gross change;
- net external flows;
- flow-adjusted gain;
- period and aggregation settings;
- data density and valuation date;
- allocation by asset class;
- account values contributing to the total;
- quick access to daily data-entry workflows.

## Research basis

The implementation keeps JavaFX standard controls because the V1 desktop client is still deliberately simple and local-first.

JavaFX `LineChart` remains suitable for the first time-series visualization. `Tooltip` is used on chart points because precise financial values must be available without overloading the chart surface.

WCAG 2.2 was used as a practical checklist. The relevant concerns for this phase are visible focus, readable labels, target size, status messages and error prevention for financial data.

## Implemented changes

| Area | Implementation |
| --- | --- |
| Metrics | `DashboardMetrics` now shows four cards: net worth, gross change, net external flow and flow-adjusted gain. |
| Quality | `DashboardQualityCard` summarizes period, valuation date, series point count, valued account count, allocation row count and aggregation mode. |
| Shortcuts | `DashboardQuickLinksCard` links the dashboard to data entry, accounts and refresh. |
| Account values | `DashboardAccountsCard` lists valued accounts sorted by value. |
| Allocation | `DashboardAllocationCard` now uses business labels instead of raw enum names. |
| Navigation | `SolviaShell` exposes `showPage(...)` so dashboard shortcuts can open existing pages. |
| Styling | `solvia-dashboard.css` isolates dashboard-specific layout rules. |
| Formatting | `DesktopFormatters` now includes date, period, bucket, aggregation, count and asset-type helpers. |

## Layout

The dashboard now follows this structure:

```text
Filters
Quick links
KPI cards
Status message
Main chart | Quality / Allocation / Accounts
```

The chart remains visually dominant. The right column contains data-quality and breakdown cards so the user can quickly check whether the displayed numbers are trustworthy.

## Product reasoning

A financial dashboard should not only display a line and a total. It must answer three questions:

1. What is my patrimonial value?
2. Did the value move because of performance or because I added/removed capital?
3. Are the displayed data dense and recent enough to be trusted?

UI-3 addresses these questions without adding new backend endpoints.

## Deferred work

- Add a dedicated period comparison card.
- Add explicit stale-data warnings once snapshots expose freshness thresholds.
- Add position and asset shortcuts after the UI-D asset/position screens exist.
- Replace the simple JavaFX chart with a richer charting layer only if JavaFX charts become limiting.
- Add screenshot-based visual QA after packaging is introduced.
