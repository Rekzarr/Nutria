# Health Connect Steps Read Boundary

Read Health Connect daily step aggregates at the Android data boundary, using the local calendar day and transient UI state only.

## What Happened

For Nutria task t18 (2026-11-14), the app added Health Connect 1.1.0 read-only daily steps using `StepsRecord.COUNT_TOTAL`. The adapter checks provider availability and `READ_STEPS`, then queries from the current local day's `atStartOfDay(zone)` to the next local day's start, which preserves 23/25-hour DST days. No `DataOrigin` filter is applied so valid device and other-source records can contribute. Totals remain only in Compose state; permission, no-data, provider, and read errors stay distinct. The retry after t19.1 cleared the shared lint blocker and verified the full serial build; it also corrected the Play Store setup URI to the official `healthconnect://onboarding` deep link.

## Takeaway

Keep Health Connect API calls behind an Android data source and the shared repository contract. Request only the record permission the user selected, never add a write permission, and do not persist or upload health totals unless a new product/privacy decision explicitly allows it. Use zone-aware local-day boundaries, and do not interpret a missing aggregate as zero. For provider update routing, preserve both slashes in `healthconnect://onboarding` when URL-encoding the Play Store `url` parameter. A fake or compiled UI test is not evidence of real provider behavior; verify permission and provider flows on a device when available.

## History
- 2026-11-14 (Nutria/t18): initial
- 2026-11-14 (Nutria/t18 retry): verified serial full build after shared lint fix; corrected Health Connect onboarding deep link and added fall-back DST coverage.
