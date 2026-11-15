# Shared Domain Rules and Repository Contracts

Nutria feature rules should remain pure Kotlin and use typed local dates, explicit state results, and repository interfaces at platform/storage boundaries.

## What Happened

For Nutria task t15 (2026-11-14), shared domain code was added under `core/model/` and storage/platform contracts under `core/data/`. Exact ounce conversion, PWA-compatible rounding, local-day DST ranges, streak/recommendation/calendar/cycle rules, Health Connect no-data/permission states, and actual BLE reading freshness were tested without Android services or Room. Deterministic random inputs keep recommendation/menu tests repeatable.

## Takeaway

Keep calculation/value types in `core/model`, keep Android/Room/HTTP access behind `core/data` interfaces, and keep health data transient unless an explicitly local workout repository is used. Use `LocalDate` for calendar-day identity and `ZoneId` only when converting a day to instants. Preserve unrounded nutrient values until the intended display or recipe-calculation boundary. A mock reading or fake state is unit-test evidence only, never proof of device integration.

## History
- 2026-11-14 (Nutria/t15): initial
