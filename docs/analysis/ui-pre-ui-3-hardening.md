# Pre UI-3 desktop hardening pass

## Objective

This pass closes the remaining UI-1/UI-2 follow-ups before starting UI-3.

## Integrated items

| Item | Implementation |
| --- | --- |
| JavaFX smoke tests | Conditional `JavaFxSmokeTest` starts JavaFX when available and constructs core desktop views. |
| Better empty states | Added reusable `EmptyState` and applied it to accounts, entries, chart and allocation areas. |
| Optional backend start | Added explicit `BackendLauncher` and `Démarrer backend` action in `BackendStatusBanner`. |
| Chart tooltips | Dashboard chart points now expose date and value tooltips. |
| Real date X axis | Dashboard chart now uses `CategoryAxis` with formatted real dates instead of day offsets. |
| Specialized amount/currency validation | Added `InputValidation`, unit tests, and form integration for accounts and entries. |

## Notes

The backend start is deliberately explicit. Solvia does not silently spawn backend processes during desktop startup. The launch action attempts:

```text
mvn -pl solvia-backend -am spring-boot:run
```

from the detected project root. This is a development-time convenience and should later be replaced by packaged-process supervision after jlink/jpackage work.

## Tests

Added tests:

- `InputValidationTest`;
- `JavaFxSmokeTest`.

The JavaFX smoke test is conditional because CI environments may not expose a graphical runtime. It skips instead of failing when JavaFX cannot start.

## Remaining UI-3 candidates

- More readable date axis for dense periods.
- Better chart interaction beyond point tooltips.
- Dedicated form layout component to reduce repeated `GridPane` code.
- Packaged backend supervision instead of Maven-based development launcher.
