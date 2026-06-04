# UI-5 fine-grained backend series

## Objective

This pass lets the dashboard use the selected time step as a real backend bucket instead of silently falling back to daily buckets.

Before this change, the desktop UI could display presets such as `1s`, `1min` and `1h`, but those values were converted to `1d` before calling `/api/net-worth/series`. The UI therefore refreshed, but the generated series still had daily points.

## Implemented scope

| Area | Implementation |
| --- | --- |
| Domain calculator | Added temporal series generation with `LocalDateTime` points. |
| Bucket units | Added `SECONDS`, `MINUTES` and `HOURS` to `TimeBucketUnit`. |
| Backend parser | `/api/net-worth/series` now parses `s`, `min`, `h`, `d`, `w`, `m`, `y` units. |
| API response | Series points now return `LocalDateTime` values. |
| Point limit | Added `maxPoints` with a backend cap of 100000 points. |
| Desktop DTO | `SeriesPointDto.valueDate` now uses `LocalDateTime`. |
| Desktop chart | Axis and tooltip rendering now include time when needed. |
| UI presets | Fine-grained presets now send real buckets such as `1s`, `5min`, `1h`. |
| Tests | Added backend tests for hourly and second-level series buckets. |

## Behavior

The dashboard now sends the selected bucket directly:

| UI step | API bucket |
| --- | --- |
| `1s` | `1s` |
| `5s` | `5s` |
| `1min` | `1min` |
| `1h` | `1h` |
| `1d` | `1d` |
| `1w` | `1w` |
| `1m` | `1m` |
| `1y` | `1y` |

The backend returns temporal points such as:

```json
{
  "valueDate": "2026-06-10T00:00:01",
  "value": {
    "amount": 2300,
    "currencyCode": "EUR"
  }
}
```

## Data-model limitation

This change enables real temporal sampling, but not yet true intraday valuation changes. Existing Solvia V1 snapshots and cash flows are still stored by `LocalDate`, not by precise timestamp.

Consequence:

- `1s` produces second-level points;
- the values only change when the effective valuation date changes;
- true intraday changes will require timestamped snapshots and database columns in a later phase.

## Safety limit

Dense series can become very large. For example, `1s` over 31 days would request more than 2.6 million points. The backend therefore caps series generation at 100000 points and returns a validation error when the request is too dense.

For now, `1s` is practical over one day or less. Wider periods should use minute, hour or day steps.

## Validation

Added backend tests:

- `returnsHourlyNetWorthSeries`;
- `returnsSecondNetWorthSeriesWhenPointLimitAllowsIt`.

Updated desktop test expectations so fine-grained steps are treated as real backend buckets instead of daily fallbacks.
