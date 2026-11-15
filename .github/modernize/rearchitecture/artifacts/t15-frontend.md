# t15 — Shared domain rules and repository contracts

## Status

**COMPLETE for the shared domain foundation.** Pure Kotlin rules, immutable shared models, and data/platform repository interfaces now define the contracts for the following feature slices. This does not claim that the UI, persistence adapters, Health Connect integration, or BLE runtime flow are implemented.

## Deliverables

- [t15-frontend-batch-report.yaml](./t15-frontend-batch-report.yaml) — implementation and source-trace report.
- [checkpoints/t15-tasks-to-impl.yaml](./checkpoints/t15-tasks-to-impl.yaml) — T005 traceability and verification checkpoint.

## Implementation

- Added `core/model/` types for nutrition and goals, food amounts, meal/water records, recipes, calendar assignments, cycle records, steps states, heart-rate readings, and workout lifecycle events.
- Added pure rules for exact 28.3495 g/oz conversion, proportional nutrients, PWA-compatible display/recipe rounding, local-day boundaries across DST, care streaks, recipe totals, recommendation eligibility/selection, Monday-first week/month generation, cycle validation/estimates, Health Connect result states, standard Heart Rate Measurement parsing, stale readings, and observed workout gaps.
- Added `core/data/RepositoryContracts.kt` interfaces for local diary/goals/recipes/calendar/cycle records, transient Health Connect reads, BLE transport/readings, workout records, and non-health display preferences. The interfaces document transactional boundaries and prohibit implicit health-data persistence or fabricated readings.
- Kept Android/Room dependencies out of the domain package. No backend route, API client contract, PWA source, or app UI was changed.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1-pm.md` and `t1-pm-spec.md` — current behavior, REQ acceptance rules, defaults, and user-visible edge cases.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — package/dependency boundaries, local-date semantics, nutrition conversion, Health Connect/BLE result states and lifecycle.
- `.github/modernize/rearchitecture/artifacts/t6-dba.md` — data ownership, transaction boundaries, no-data distinction, workout event/sample constraints, and no Health Connect cache.
- `.github/modernize/rearchitecture/artifacts/t7-teamlead.md` — required deterministic domain test coverage and separation of fakes from device evidence.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md` — Plan 2.2 / T005 scope and REQ traceability.
- `.github/modernize/rearchitecture/artifacts/t10-devops.md` and `checkpoints/t8-environment.md` — JDK 17 / Android SDK activation and separate device-runtime blocker.
- `.github/modernize/rearchitecture/artifacts/t11-frontend.md`, `t12-dba.md`, and `t14-frontend.md` — existing Compose/build, encrypted persistence foundation, and frozen API boundary that this task leaves unchanged.
- `.github/modernize/rearchitecture/artifacts/constitution.md` — native parity, local privacy, and truthful sensor-data requirements.
- `.github/modernize/rearchitecture/learnings/frontend/android-native-scaffold-build-baseline.md`, `native-navigation-history-and-spanish-locale.md`, and `typed-nutria-api-client.md` — build activation, locale conventions, and client/server boundary.

## Evidence Mapping

- `t8-teamlead-plan.md#Step 2.2 / T005` → `android/app/src/main/java/com/nutria/app/core/model/` and `core/data/`, with fixed-date, timezone, deterministic-selection, and sensor-state tests.
- `t1-pm-spec.md#REQ-003` and `t4-architect.md#State and contract ownership by feature` → `NutritionRules`, `Nutrients`, and `FoodAmount`; tests assert the exact ounce factor, portions, validation, and rounding behavior.
- `t1-pm-spec.md#REQ-002` → `StreakRules.careStreak`, tested for current-day counting and stopping at the first empty day.
- `t1-pm-spec.md#REQ-007, REQ-010` → recipe totals and recommendation filter/selection rules; tests verify per-serving values, filters, saved-name/ID exclusion, the 550-kcal rounded threshold, and the three-result cap.
- `t1-pm-spec.md#REQ-011–REQ-012` → `CalendarRules`, tested for Monday-first weeks, month length, required meal/dinner categories, explicit replacement confirmation, and no duplicate recipe where an alternative exists.
- `t1-pm-spec.md#REQ-013–REQ-016` and `t6-dba.md#Constraints and transaction boundaries` → cycle settings/date/overlap validation, calendar estimate phases, unknown state, and 160-character note bounds; cycle records remain repository-owned local data.
- `t1-pm-spec.md#REQ-021–REQ-022` and `t6-dba.md#Health Connect and BLE boundary` → `HealthConnectStepsState` distinguishes positive actual results, no data, permission required/denied/revoked, provider unavailable/update required, and read error; zero cannot be represented as an available reading.
- `t1-pm-spec.md#REQ-023–REQ-026` and `t4-architect.md#BLE heart-rate band and workout lifecycle` → BLE connection state, standard 0x2A37 measurement parsing, 15-second stale threshold, actual timestamped readings, and explicit observed session gaps; unit tests use constructed values only and are not device evidence.
- `t6-dba.md#Constraints and transaction boundaries` → repository contracts require clear-today isolation, transactional recipe/calendar deletion and range replacement, Health Connect non-persistence, and workout readings/events tied to explicit sessions.

## Test Results

- Command: `.\gradlew.bat test build assembleDebugAndroidTest --no-daemon` (from `android/`, with the t10 JDK 17 / SDK activation in the same PowerShell process).
- Passed: 35 JVM unit tests total, including 15 new domain tests; debug and release builds, Android lint/check, and instrumentation APK compilation/package.
- Failed: 0.
- Skipped: 0 JVM unit tests.
- Android instrumentation/runtime: not executed; t10 records no connected device and an emulator acceleration blocker. APK packaging is not reported as an instrumentation pass.
- Build notice: AGP reports the existing `android.disallowKotlinSourceSets=false` compatibility setting as experimental; build and lint completed successfully.

## Handoff / Remaining Work

The upcoming feature tasks can implement Room/Health Connect/BLE/UI adapters against these contracts. They remain responsible for feature-specific schema migrations, Spanish UI/error presentation, permission requests, lifecycle behavior, actual sensor/device verification, and persisted-record integration tests. The new rules alone do not satisfy application parity or device validation.
