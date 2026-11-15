# Cycle Estimate Source Branches

The native cycle estimate presentation must preserve the PWA's order for ongoing, late, due-today, and future-start states.

## What Happened

In Nutria task t20 (2026-11-14), `CycleRules` correctly exposed both `isOngoing` and `daysLate`, but the first Compose estimate rendering checked lateness before ongoing status. A period that was still open could therefore be labeled late even though the PWA prioritizes an ongoing period and shows the expected-start estimate. The UI now checks ongoing first, then late, expected today, and future dates with singular/plural countdown text.

## Takeaway

When porting cycle estimates, preserve `renderCycle()` branch precedence as well as calendar calculations. Render the ongoing state before late-day counts; distinguish a date due today from future dates; keep singular/plural day copy accurate. A correct domain result can still be presented incorrectly if the target UI reorders its states.

## History
- 2026-11-14 (Nutria/t20 retry): preserve estimate presentation precedence and due-date branches.
