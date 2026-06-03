# Phase 4 calculation engine analysis

## Objective

Phase 4 turns the stored historical data into read-only financial calculations for
charts and summaries. The implementation must stay deterministic and testable
because later UI and import features will depend on these results.

## Scope mapping

| Phase item | Implementation |
| --- | --- |
| 4.1 Value at a date | `NetWorthCalculator.valueAt(...)` |
| 4.2 Total net worth | `NetWorthValuation.total` |
| 4.3 By account | `AccountValuation` values in `NetWorthValuation` |
| 4.4 By asset class | `AssetTypeValuation` allocation values |
| 4.5 Gross change in EUR and % | `PerformanceSummary.grossChange` and `grossChangePercentage` |
| 4.6 Flow-adjusted gain | `PerformanceSummary.flowAdjustedGain` |
| 4.7 Time series | `NetWorthCalculator.series(...)` |
| 4.8 Custom aggregation | `TimeBucket` and `AggregationMode` |
| 4.9 Last known value fill | latest snapshot at or before the requested date |
| 4.10 Reference tests | domain tests and MVC endpoint tests |

## Calculation rules

A value at a date is calculated from the latest known snapshot on or before that
date. Account balance snapshots are classified as `FIAT_CURRENCY`. Position
snapshots are classified through their linked asset type.

All returned values are converted to the requested reporting currency. Direct FX
rates are used when available; inverse rates are used as a fallback. Missing FX
rates fail the calculation explicitly instead of silently returning a misleading
number.

Gross change is:

```text
end value - start value
```

Flow-adjusted gain is:

```text
end value - start value - net external flows
```

External capital flows are deposits, withdrawals, transfer-in and transfer-out.
Interest, dividends, fees, taxes, cashback and corrections are not treated as
external capital flows in this V1 calculation model.

## Time series

The first supported aggregation modes are:

- `LAST_KNOWN`: value at the end of each bucket;
- `AVERAGE`: arithmetic average of daily values inside each bucket.

Buckets use compact API syntax such as `1d`, `2d`, `1w` or `1m`.

## API mapping

New read-only endpoints:

- `GET /api/net-worth`;
- `GET /api/net-worth/series`;
- `GET /api/performance`;
- `GET /api/allocation`.

## Deferred work

The current implementation deliberately does not include full TWR, IRR/MWR or
Modified Dietz. Those require more precise flow timing rules, benchmark datasets
and careful UX explanations. The current flow-adjusted gain is a pragmatic V1
metric that avoids the most misleading interpretation of deposits and withdrawals.
