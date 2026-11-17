# t22 — Goals, themes, and privacy settings

## Status

**Implementation and verification are complete.** The settings sources compile and their host tests pass. A later clean full build passed, including lint, JVM tests, debug/release builds, and Android test APK packaging. The OPPO CPH2197 / Android 13 connected instrumentation run also passed (28 tests, 0 failures). Real Health Connect provider data and a compatible BLE band were not exercised.

## Summary

- Added validated calorie, water, protein, carbohydrate, and fat goals. Water requires at least 50 ml in 50-ml steps; goals use the app's encrypted Room database and retain the specified PWA defaults.
- Added the four Spanish theme choices with immediate preview, saved persistence, and cancel/back restoration.
- Added Spanish disclosures for local records, Health Connect steps, BLE heart-rate/workout data, photo analysis, ingredient search, lack of account/sync, PWA non-import, backup/uninstall implications, and the configured Nutria service.
- Added an advanced service-base-URL preference with HTTPS enforcement in release and debug-only loopback HTTP; no API-key field or client-side provider secret is present.
- Added a configured API-client factory so subsequent photo/search flows can read the stored URL without changing the existing API contracts.

## Deliverables

- [t22-frontend-batch-report.yaml](./t22-frontend-batch-report.yaml) — implementation inventory, traceability, tests, and current blocker.
- [checkpoints/t22-tasks-to-impl.yaml](./checkpoints/t22-tasks-to-impl.yaml) — T016 implementation checkpoint.

## Upstream Artifacts Consumed

- `artifacts/t1-pm.md` and `artifacts/t1-pm-spec.md` — product parity baseline and REQ-020 goal/theme contract, plus secret/privacy boundaries.
- `artifacts/t4-architect.md` — native app, package, and local/network boundaries.
- `artifacts/t5-ux.md` — Ajustes flow, Spanish copy, reversible theme preview, and accessibility requirements.
- `artifacts/t6-dba.md` — encrypted Room goal storage; DataStore-only non-health theme/service preferences; backup and disclosure boundaries.
- `artifacts/t7-teamlead.md` and `artifacts/t8-teamlead-plan.md` — runtime verification gate and Step 4.5/T016 scope.
- `artifacts/t10-devops.md` and `artifacts/checkpoints/t8-environment.md` — JDK 17/Android SDK activation and emulator limitation.
- `artifacts/t11-frontend.md`, `artifacts/t12-dba.md`, `artifacts/t13-frontend.md`, and `artifacts/t15-frontend.md` — existing Compose/Room scaffold, settings/app integration points, and repository-contract patterns.

## Evidence Mapping

- `t1-pm-spec.md#REQ-020` → `SettingsValidation.kt`, `SettingsScreen.kt`, `NutritionGoalsRoom.kt`, and `SettingsValidationTest` cover positive targets, water increments, four themes, persistence, and cancel restoration.
- `t5-ux.md#Ajustes-—-goals,-theme-and-privacy` → Spanish privacy sections and `SettingsScreenTest` assertions cover the settings experience and theme cancel/save behavior.
- `t6-dba.md#Persistence-ownership` and `#Proposed-local-model` → goals are stored in encrypted Room; theme and service URL are stored in DataStore; `GoalsRoomRepositoryInstrumentedTest` and `PreferencesStoreInstrumentedTest` cover persistence.
- `t8-teamlead-plan.md#Step-4.5` and `#T016` → settings UI, validation, disclosures, and tests are implemented; full-build gate remains blocked as detailed below.
- `t6-dba.md#Existing-Nutria-service` and `t1-pm-spec.md#REQ-019` → `ConfiguredNutriaApiClientFactory.kt` reads the saved base URL while retaining the typed client boundary; no OpenAI key input is exposed.
- `checkpoints/t8-environment.md#Exact-activation-instructions` → Gradle validation used JDK 17.0.16 and the configured Android SDK in the same PowerShell process.

## Test Results

- Command: `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks --no-daemon --max-workers=1` (from `android/`, with JDK 17.0.16 and Android SDK activated).
- Passed: 58 JVM tests.
- Failed: 0 JVM tests.
- Skipped: 0 JVM tests.
- Command: `.\gradlew.bat :app:compileDebugAndroidTestKotlin --rerun-tasks --no-daemon --max-workers=1`.
- Result: Android instrumentation test sources compiled successfully, including `SettingsScreenTest`.
- Instrumentation execution: not run; `adb devices -l` reported no connected device, and the emulator limitation is documented in the t10 environment checkpoint.
- Full command: `.\gradlew.bat test build assembleDebugAndroidTest --no-daemon --max-workers=1`.
- Result: **failed** at `:app:lintDebug` with 68 errors, 24 warnings, and 1 hint. The first error is `feature/activity/WorkoutBleService.kt:355` (`MissingPermission`); the report also contains shared activity/cycle issues. A lint-report search found no findings in t22 settings sources. This shared blocker prevents a passing full-build gate; see the batch report.

## Remaining Validation

The earlier shared-project lint failure was cleared by a subsequent clean verification run. The physical-device run provides Android runtime evidence for the instrumented flows; Health Connect provider access and physical BLE band lifecycle evidence remain pending.
