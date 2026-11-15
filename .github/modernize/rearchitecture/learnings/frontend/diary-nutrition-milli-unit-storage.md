# Diary Nutrition Milli-Unit Storage

Persist diary nutrition as nonnegative integer milli-units, with an explicit Room migration from the earlier REAL columns.

## What Happened

In Nutria t16 (2026-11-14), review against the DBA model found that meal rows stored Kotlin `Double` values in SQLite `REAL` columns even though the approved data model uses milli-kcal and milli-grams. Meal records now round at the persistence boundary to the nearest thousandth, Room schema v6 stores `Long` values, and migration 5→6 copies existing meal rows while scaling each value by 1,000. Domain calculations remain unrounded until persistence/display boundaries. JVM tests cover conversion and round-trip; the SQL migration test is instrumented and was compiled/packaged but not executed on a device.

## Takeaway

Follow the DBA's stored-unit contract at the Room boundary: keep UI/domain nutrition in grams/kcal, persist milli-units as `Long`, convert explicitly in entity mappers, and append a non-destructive migration that preserves prior records. A successful host build is not evidence that an instrumented migration ran.

## History
- 2026-11-14 (Nutria/t16): initial
