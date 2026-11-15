# Native Navigation History and Spanish Locale

Nutria's Compose shell keeps localized destination labels in Android resources and formats local dates/numbers with an explicit `es-ES` locale.

## What Happened

For Nutria task t13 (2026-11-14), the PWA's six-tab mobile navigation was replaced with the UX-approved labeled seven-destination native drawer. `AppDestination` owns stable route keys and resource-backed Spanish labels; `NavigationHistory` reuses an existing route instead of adding a duplicate; `SpanishFormats` handles numeric `dd/MM/yyyy` dates and display numbers with `es-ES`. A legacy `registrar` route remains an alias for `Comida`. Feature placeholders explicitly state that sections are not yet available so the staged shell is not mistaken for delivered parity.

## Takeaway

Keep route identity independent from localized display text, and centralize navigation history transitions so drawer/deep-link actions cannot create duplicate destination entries. Preserve legacy entry aliases where the old client exposed them. Use deterministic locale formatting in domain/UI helpers, follow the UX-specified presentation exactly (`dd/MM/yyyy` for these dates), and test it with fixed dates and values. When feature tasks add nested forms/details, extend the back-stack model deliberately rather than adding ad hoc screen booleans.

## History
- 2026-11-14 (Nutria/t13): initial
- 2026-11-14 (Nutria/t13): aligned the app-bar date display with the UX specification's `dd/MM/yyyy` presentation and verified it with a fixed-date unit test.
