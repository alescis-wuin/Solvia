# Desktop accessibility refresh

## Backend launch correction

The backend must be started from the root workspace with the Maven reactor enabled:

```bash
mvn -pl solvia-backend -am spring-boot:run
```

The previous command without `-am` can fail when sibling modules have not been installed in the local Maven repository.

PostgreSQL must also be running:

```bash
docker compose up -d postgres
```

## Desktop theme

The desktop module now uses AtlantaFX as the theme baseline and adds a Solvia dark stylesheet for higher contrast and larger controls.

## Typography

The application tries to load Luciole from `fonts/Luciole/` and Hack from `fonts/Hack/`. If those resources are absent or named differently, JavaFX falls back to system fonts declared in CSS.

## Accessibility goals

The UI refresh targets:

- dark background with bright foreground text;
- stronger borders between surfaces;
- larger text and controls;
- visible focus indicators;
- larger buttons;
- monospaced rendering for financial values and tabular data;
- tooltips for navigation and data-entry controls.

## Remaining work

The current refresh adds the theme foundation. The next pass should continue with per-view refinements:

- semantic CSS classes on every form section;
- richer tooltips on all inputs;
- more compact but clearer dashboard grouping;
- optional font filename discovery if the bundled font names differ from the known candidates;
- smoke testing with `mvn -pl solvia-desktop -am javafx:run`.
