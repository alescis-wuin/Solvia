# UI-5 dashboard visual simplification

## Objective

UI-5 reduces dashboard visual noise and introduces a more precise time-step selector.

The dashboard is now focused on portfolio value, chart, allocation and account contribution. Technical backend information has been moved to a dedicated system page.

## Implemented scope

| Area | Implementation |
| --- | --- |
| Time-step selector | Added `DashboardTimeStep` and `DashboardTimeStepSelector`. |
| Fine-grained presets | Added second, minute and hour presets as UI-level choices with V1 daily fallback. |
| Native V1 buckets | Kept day, multi-day, week, month, quarter, semester and year presets mapped to backend bucket syntax. |
| Dashboard shortcuts | Replaced text shortcut card with a hamburger `MenuButton`. |
| SVG icons | Added SVGPath-based menu icons styled through CSS. |
| Backend status | Moved backend/PostgreSQL status controls to `SystemView`. |
| Refresh success noise | Removed the persistent “Dashboard actualisé” success message after successful refresh. |
| Visual tuning | Increased base font size, adjusted contrast, accent colors, button sizes and dashboard-specific spacing. |

## Time-step behavior

Solvia V1 stores snapshot value dates as dates, not instants. For that reason, the dashboard can expose fine-grained UI presets such as seconds, minutes and hours, but their current backend query fallback remains `1d`.

This keeps the UI ready for future intraday data without pretending that V1 can calculate true second-level patrimonial series.

Examples:

| Display preset | API bucket in V1 |
| --- | --- |
| `1s` | `1d` |
| `1min` | `1d` |
| `1h` | `1d` |
| `2d` | `2d` |
| `1w` | `1w` |
| `1m` | `1m` |
| `1y` | `12m` |

## Research basis

JavaFX `ComboBox` remains appropriate for selecting a value from a finite set of predefined steps. JavaFX `MenuButton` provides a popup menu suitable for compact dashboard actions. JavaFX `SVGPath` allows icons to be styled from CSS using SVG path content.

## Deferred work

- True intraday storage based on timestamps instead of `LocalDate` snapshots.
- Backend bucket parsing for seconds, minutes and hours once the domain model supports instants.
- Optional adaptive chart renderer if JavaFX `LineChart` becomes too limited for dense time series.
- Visual QA screenshots after desktop packaging is introduced.
