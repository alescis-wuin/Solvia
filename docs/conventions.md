# Solvia conventions

## Language

Code, package names, documentation filenames and commit messages are written in
English.

## Commit messages

Commit titles use:

```text
<branch type> (<feature>) : <title>
```

Example:

```text
feature (domain-model) : add monetary value objects
```

A commit body explains the intent and the main decisions. Footers are used for
phase references or breaking changes.

## Java packages

The root package is:

```text
fr.seynax.solvia
```

Domain code must not depend on Spring, JavaFX, PostgreSQL or any infrastructure
library.

## Money

Money is represented with `BigDecimal` in Java and must be stored with exact
numeric database types once persistence is introduced. Floating point types are
not allowed for money, percentages, quantities, prices or FX rates.

## Privacy

Financial data is treated as sensitive personal data. Solvia defaults to local
execution, explicit exports and no telemetry.
