## [t11] Scaffold the native Android app
- Codebase/domain discoveries: t10's user-local JDK 17, Android SDK Platform 36, and Build Tools 36.0.0 are usable when explicitly activated in the same shell; the emulator remains unavailable because the hypervisor driver is absent.
- Wrong assumptions and corrections: the default shell still resolved Java 25; setting/prepending the recorded `JAVA_HOME\bin` was necessary for Gradle. No project-global Gradle is needed once the checked-in wrapper exists.
- Debugging dead-ends and what actually worked: downloaded Gradle 9.6.0 from the official distribution URL, verified its published SHA-256 before use, then generated the wrapper and verified the wrapper-JAR SHA-256 against Gradle's release page.
- Techniques/patterns worth reusing: AGP 9.5 built-in Kotlin means do not apply `org.jetbrains.kotlin.android`; match Compose compiler plugin 2.2.10 to the built-in KGP 2.2.10. Keep Android test compilation/package evidence distinct from instrumented execution.
- Learnings consumed: [devops/android-toolchain-activation, architect/native-android-api-seam, teamlead/native-android-plan-order, teamlead/native-android-validation-strategy]

## [t13] Implement the Spanish native navigation shell
- Codebase/domain discoveries: the PWA exposes six mobile tabs and a `registrar` legacy alias; the UX spec intentionally replaces the crowded mobile tabbar with seven text-labeled drawer destinations and keeps Ajustes in the app bar.
- Wrong assumptions and corrections: `Modifier.weight` cannot be imported as a public top-level extension in this Compose version; the Column scope resolves it directly. Material3 `TopAppBar` requires its experimental API opt-in.
- Debugging dead-ends and what actually worked: after correcting the initial Compose compile errors, a clean Gradle run compiled and packaged the app and instrumentation APKs, but nine shared NutriaApiClientTest cases failed. A later filtered test was blocked before execution by concurrent t12 persistence sources with unresolved Room/SQLCipher/DataStore dependencies. The new legacy-alias test was added afterward and remains unverified.
- Techniques/patterns worth reusing: route identity should be separate from Spanish labels; reuse an existing route in the history rather than pushing a duplicate; explicitly format local presentation with `es-ES`; mark unimplemented sections honestly during staged migration.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline]

## [t14] Implement the typed Nutria API client
- Codebase/domain discoveries: only `POST /api/analyze-meal` and `GET /api/ingredients/search` cross the backend boundary; nutrition, cycle, steps, and BLE data remain local.
- Wrong assumptions and corrections: MockWebServer's URL helper used a canonical reverse-DNS name (`activation.easeus.com`) in this host, not a local loopback name; tests now build a `127.0.0.1` URL explicitly. Contract tests also caught and corrected the missing `/api` route prefix.
- Debugging dead-ends and what actually worked: the first shared `clean` build overlapped other Gradle workers and corrupted output cleanup; after the KSP migration option was configured, a non-clean standard build/test run passed 19 tests. The emulator/device limitation does not affect local HTTP contract verification.
- Techniques/patterns worth reusing: use OkHttp multipart/query builders; disable connection-failure retries for cost-bearing photo POSTs; close responses; cancel calls with coroutine cancellation; preserve server ProblemDetails titles and validate complete finite nonnegative nutrition before returning success.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline]

## [t12.1] Verify loopback MockWebServer HTTPS policy and shared Gradle suite
- Codebase/domain discoveries: the current MockWebServer fixture already forces `127.0.0.1` and explicitly opts into debug cleartext; that prevents host reverse-DNS names from tripping the client's HTTPS guard. The prior t12 failure report was stale relative to the fixture and t14's passing report.
- Wrong assumptions and corrections: a cached Gradle result alone is insufficient evidence; `--rerun-tasks` executed the complete unit/build/package suite and confirmed the current source.
- Debugging dead-ends and what actually worked: this shell starts at the repo root, so the wrapper must be invoked after `Set-Location android`; the first command without that directory did not run Gradle.
- Techniques/patterns worth reusing: keep HTTP transport test-only and loopback-gated; test that the opt-in still rejects non-loopback HTTP; report instrumented APK packaging separately from device execution.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline, frontend/typed-nutria-api-client]

## [t13] Complete the Spanish native navigation shell
- Codebase/domain discoveries: the agreed phone navigation is a labeled seven-destination Compose drawer with Ajustes kept in the app bar; feature workflows are intentionally separate vertical tasks and must remain clearly unavailable until implemented.
- Wrong assumptions and corrections: the prior task report's shared-suite failures were stale; after t12.1 and the current persistence/API sources landed, the complete Gradle test/build/package command passed.
- Debugging dead-ends and what actually worked: update the app-bar date presentation to the UX-required `dd/MM/yyyy`; verify with a fixed-date JVM test and run the complete `test build assembleDebugAndroidTest` task sequence using t10's explicit JDK 17/SDK activation.
- Techniques/patterns worth reusing: keep instrumentation APK packaging distinct from actual device execution; `adb devices -l` confirmed this host still has no device, so the two Compose UI methods remain unexecuted.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline, frontend/native-navigation-history-and-spanish-locale]

## [t15] Implement shared domain rules and repository contracts
- Codebase/domain discoveries: current PWA calculations live in `wwwroot/app.js`; core equivalents now cover local-date/DST handling, nutrition conversion, recipe/recommendation/calendar behavior, cycle estimates, Health Connect result states, BLE measurement parsing/freshness, and observed workout gaps. Persistence/platform adapters are still future vertical work.
- Wrong assumptions and corrections: Android health/workout states need typed distinctions, not nullable numbers/connected booleans; `NoData`, permission/provider/read errors, stale readings, and gaps now have explicit representations. Kotlin's tie-to-even rounding differs from the PWA's positive-value whole-number display, so PWA-style rounding is explicit.
- Debugging dead-ends and what actually worked: activate the documented JDK 17/SDK in the same PowerShell process, run `.\gradlew.bat test build assembleDebugAndroidTest --no-daemon`, and count XML test reports rather than infer a test count from Gradle task output.
- Techniques/patterns worth reusing: inject Kotlin `Random` for repeatable but shuffle-based selections; keep local day identity as `LocalDate`, resolve DST-aware ranges only at the data boundary, and keep `core/model` free of Android/Room types.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline, frontend/native-navigation-history-and-spanish-locale, frontend/typed-nutria-api-client]

## [t20] Implement cycle tracking and private journal
- Codebase/domain discoveries: the existing `CycleRules` already captures legacy inclusive overlap checks and calendar-only estimates; the cycle screen can reuse it while storing `LocalDate` keys in encrypted Room.
- Wrong assumptions and corrections: another feature slice advanced the shared database to v2 for diary tables while t20 was starting; preserve that migration and insert cycle tables as v2→v3, then keep later concurrent migrations in the same continuous chain. Do not treat an initially observed DB version as stable during parallel slices.
- Debugging dead-ends and what actually worked: structural checks confirmed every cycle string resource exists, cycle migration v2→v3 is in the explicit v1→v5 chain, and the cycle repository has no network-client dependency. Full Gradle verification was blocked first by other in-flight source compilation failures and then by shared KSP/resource output contention.
- Techniques/patterns worth reusing: keep reproductive-health form state out of Android saved-instance-state; persist only after explicit user save; use strict `dd/MM/uuuu` parsing and exclude the edited record from overlap checks. Extend the cycle repository for a bounded recent-entry query instead of loading the entire journal.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline, frontend/native-navigation-history-and-spanish-locale, frontend/shared-domain-rules-and-repository-contracts]

## [t18] Implement Health Connect steps
- Codebase/domain discoveries: the native shell can share one Health Connect card between Inicio and Actividad; provider state and aggregate results belong behind `HealthConnectStepsRepository`, while permission rationale and the current total stay in memory.
- Wrong assumptions and corrections: a missing/zero aggregate must not be presented as a fabricated daily total; `StepsRecord.COUNT_TOTAL` over zone-aware consecutive local-day starts handles DST without persisting timestamps or totals.
- Debugging dead-ends and what actually worked: shared builds collided early; a serial JVM rerun produced 58 passing tests and the Android instrumentation APK packaged. The full `build` task remains blocked at `:app:lintDebug` by cross-task findings, first reported in t19 `WorkoutBleService.kt:355`.
- Techniques/patterns worth reusing: check provider and `READ_STEPS` before aggregating, request permission only from a user action, refresh on `ON_RESUME`, and distinguish no-data, permission, provider, and read-error states. Keep device-provider behavior explicitly unverified until tested on a real provider/device.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline, frontend/shared-domain-rules-and-repository-contracts, frontend/native-navigation-history-and-spanish-locale]

## [t16] Implement diary, food entry, history and water
- Codebase/domain discoveries: food amount conversion and PWA storage behavior live in `wwwroot/app.js`; the approved native UX requires past history to be read-only even though the PWA exposes deletion there. The shared encrypted Room schema is concurrently extended by multiple feature slices, so migrations must be checked against the current chain, not an earlier version snapshot.
- Wrong assumptions and corrections: AndroidX Lifecycle 2.11.0 requires compile SDK 37; the app remains on compile SDK 36 and uses Lifecycle 2.10.0. Instrumentation APK packaging is not device execution.
- Debugging dead-ends and what actually worked: `test build assembleDebugAndroidTest` passed the 58-test JVM suite, compiled production and instrumentation code, and packaged APKs, but the shared build ended at `lintDebug` with 68 errors/24 warnings in other components. Filtering the report found no `feature/diary` lint result. The host has no connected device/emulator, so instrumentation remains unexecuted.
- Techniques/patterns worth reusing: store undoable water increments as individually ordered local events and delete only the maximum sequence for the selected local day in a transaction. Keep water and meal records behind the existing encrypted Room provider; perform writes before success feedback and retain form state on persistence errors. Update Spanish README documentation and a concise ADR without describing the partial native app as full parity.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline, frontend/native-navigation-history-and-spanish-locale, frontend/shared-domain-rules-and-repository-contracts]

## [t22] Implement goals, themes, and privacy settings
- Codebase/domain discoveries: t6 owns nutrition goals in encrypted Room while display theme and service base URL belong in non-health DataStore preferences; expose the configured typed-client factory for photo/ingredient consumers without adding a provider key.
- Wrong assumptions and corrections: the plan's phrase “settings preferences” does not override the more specific DBA storage decision; the settings and repository contract keep goals separate from DataStore.
- Debugging dead-ends and what actually worked: Gradle cache status was not sufficient, so rerun the JVM suite with `--rerun-tasks` (58 tests passed) and compile the Compose instrumentation sources separately. The complete task command still fails on shared `:app:lintDebug` issues in Activity/cycle sources; the t22 files had no lint findings.
- Techniques/patterns worth reusing: use `SettingsPreferencesRepository` for theme/URL, Room for goals, validate service URLs before saving, and restore the persisted theme on cancel/back. Darken the Coral primary/swatch to `#B45846` to keep white-text contrast above 4.5:1.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline, frontend/native-navigation-history-and-spanish-locale, frontend/shared-domain-rules-and-repository-contracts, frontend/typed-nutria-api-client, frontend/private-cycle-feature-storage]

## [t19.1] Resolve shared Compose resource lookup lint errors and run the serialized full Gradle gate
- Codebase/domain discoveries: the 67 blocking lint errors were all `LocalContextGetResourceValueCall` findings in `CycleScreen.kt` and `WorkoutFeatureScreen.kt`; current resource reads should use Compose-aware resolution.
- Wrong assumptions and corrections: merely changing the obvious `Text` calls is insufficient when callbacks, coroutines, or semantics lambdas need a string; resolve strings in the composable scope and capture them, or use composable message helpers.
- Debugging dead-ends and what actually worked: a clean `test build assembleDebugAndroidTest` invocation with `--no-parallel --max-workers=1` passed the 58-test JVM suite, debug/release build, lint, and instrumentation APK packaging. The emulator remains unavailable (`-accel-check` exit 6), so APK packaging is not device-test evidence.
- Techniques/patterns worth reusing: pre-resolve localized callback messages and map typed validation errors to those values; resolve accessibility descriptions before non-composable semantics lambdas. Leave valid lint enabled and preserve the typed API client's HTTPS/loopback-debug boundary.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline, devops/android-toolchain-activation, architect/native-android-api-seam]

## [t16] Align diary persistence with scaled nutrition contract and reverify
- Codebase/domain discoveries: DBA data model specifies milli-kcal/milli-grams, while the existing diary table used REAL columns; Room v6 now stores `Long` milli-units and migration 5→6 scales existing rows.
- Wrong assumptions and corrections: prior t16 green JVM test counts did not prove the host build was green; after t19.1, the clean serialized suite completed successfully. AndroidTest migration/Compose code compiled but could not be executed without an emulator/device.
- Debugging dead-ends and what actually worked: `.\gradlew.bat clean test build assembleDebugAndroidTest --no-daemon --no-parallel --max-workers=1` using JDK 17.0.16 and SDK 36 passed; 59 JVM tests, lint with 0 errors, debug/release and AndroidTest packaging.
- Techniques/patterns worth reusing: keep calculations in domain doubles, convert only at Room mapping boundaries, append a forward migration preserving old values, and distinguish migration test compilation from runtime execution.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline, frontend/compose-resource-lookups, frontend/diary-water-event-storage, frontend/native-navigation-history-and-spanish-locale, frontend/shared-domain-rules-and-repository-contracts, frontend/typed-nutria-api-client]
- Learnings written: [frontend/diary-nutrition-milli-unit-storage]

## [t18 retry] Reverify Health Connect steps after shared lint remediation
- Codebase/domain discoveries: Health Connect daily aggregates need local-zone midnight boundaries and no `DataOrigin` filter so current device-attributed records remain eligible; the fall-back DST day is 25 hours as well as spring-forward's 23 hours.
- Wrong assumptions and corrections: the provider-update Play Store link requires a URL-encoded `healthconnect://onboarding` value with both slashes; the former single-slash value did not match Android's official example.
- Debugging dead-ends and what actually worked: the prior full Gradle result was stale/blocked by shared lint; rerunning the exact clean serialized suite after t19.1 passed all 59 JVM tests, lint, debug/release builds, and AndroidTest packaging.
- Techniques/patterns worth reusing: request only READ_STEPS from a user action, re-check permission on each read and `ON_RESUME`, keep totals transient, and record provider/device runtime as unverified when `adb` has no device and emulator acceleration is unavailable.
- Learnings consumed: [frontend/health-connect-steps-read-boundary, frontend/shared-domain-rules-and-repository-contracts, frontend/android-native-scaffold-build-baseline]
- Learnings written: [frontend/health-connect-steps-read-boundary]

## [t19 retry] Complete BLE workouts and verify after shared lint remediation
- Codebase/domain discoveries: GATT setup should not stay indefinitely in `Connecting`; a bounded connection/notification-subscription wait makes failures actionable. Permission loss and Bluetooth-off must stop scans, close the GATT transport, and interrupt any open session before UI recovery.
- Wrong assumptions and corrections: APK packaging and green host builds do not verify Bluetooth permissions, standard-band interoperability, or foreground/background behavior; the host still has no device and the emulator acceleration check exits 6.
- Debugging dead-ends and what actually worked: t19's prior full build was blocked only by shared Compose resource lint. After t19.1 remediation, the serialized clean Gradle command passed; report XMLs confirmed 61 JVM tests and no skipped/failed tests.
- Techniques/patterns worth reusing: keep service/CCCD success as the compatibility boundary; cancel timeouts on GATT teardown or successful subscription; persist only timestamped received readings and observed events. Keep platform-device evidence separate from pure domain tests and test APK packaging.
- Learnings consumed: [frontend/android-native-scaffold-build-baseline, frontend/compose-resource-lookups, frontend/shared-domain-rules-and-repository-contracts]
- Learnings written: [frontend/ble-workout-lifecycle]

## [t20 retry] Verify cycle tracking and private journal
- Codebase/domain discoveries: cycle periods and journal notes are in the shared SQLCipher Room schema, now at v6 after the diary precision migration; the cycle feature remains on its explicit v2→v3 migration.
- Wrong assumptions and corrections: the original implementation had not yet passed a full Gradle gate; t19.1 resolved shared lint issues, and a clean serialized rerun passed. Source review also confirmed the PWA repopulates today's journal inputs from its saved record, so the native editor now restores persisted values before enabling edits.
- Debugging dead-ends and what actually worked: run the full wrapper command with JDK 17 / SDK 36 in the same shell and `--no-parallel --max-workers=1`; parse XML test reports rather than infer counts from Gradle output. Current evidence: 61 JVM tests pass, lint has 0 errors, and AndroidTest packaging passes.
- Techniques/patterns worth reusing: keep cycle form state outside `rememberSaveable`; wait for the bounded daily-entry query before allowing edits; keep API/network dependencies out of the cycle repository. Device execution remains distinct from compiled instrumentation.
- Learnings consumed: [frontend/private-cycle-feature-storage, frontend/compose-resource-lookups, frontend/shared-domain-rules-and-repository-contracts]
- Learnings written: [frontend/private-cycle-feature-storage]
