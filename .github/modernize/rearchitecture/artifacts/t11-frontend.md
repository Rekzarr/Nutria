# t11 — Native Android scaffold

## Summary

Created the planned single-module Kotlin/Jetpack Compose Android project in `android/` with a pinned Gradle wrapper, JDK 17 / Android API 36 build settings, app manifest/resources, and unit/instrumentation test source sets. The starter `MainActivity` is native Compose, not a WebView. This completes scaffold task T002 only; it is not the navigation shell, feature parity, or a completed migration.

## Deliverables

- [Structured batch report](./t11-frontend-batch-report.yaml) — files, plan trace, upstream use, and test evidence.
- [Implementation traceability checkpoint](./checkpoints/tasks-to-impl.yaml) — T002 implementation and REQ-001 trace status.
- `android/app/build/outputs/apk/debug/app-debug.apk` — debug build output (generated, not a source deliverable).
- `android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` — instrumentation APK; compiled/packaged but not run.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t10-devops.md` — JDK/SDK readiness and exact activation block.
- `.github/modernize/rearchitecture/artifacts/checkpoints/t8-environment.md` — installed Android API 36/build tools, Gradle-wrapper expectation, and emulator acceleration blocker.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md#T002` — scaffold scope, target SDKs, test source sets, and full build command.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md#Target structure and boundaries` — one app module, native Compose, package boundary, and min/compile/target SDK.
- `.github/modernize/rearchitecture/artifacts/constitution.md#Principles I–V` — native-only delivery, preservation boundaries, and truthful verification claims.
- `.github/modernize/rearchitecture/learnings/devops/android-toolchain-activation.md` — explicit JDK 17 and Android SDK shell activation.
- `.github/modernize/rearchitecture/learnings/architect/native-android-api-seam.md` — Android-client scope; leave backend routes and PWA intact.
- `.github/modernize/rearchitecture/learnings/teamlead/native-android-plan-order.md` and `native-android-validation-strategy.md` — separate source/build/device evidence and do not infer platform success.

## Evidence Mapping

- `t8-teamlead-plan.md#T002` → `android/` single-module Compose app, manifest, resources, test source sets, pinned wrapper, and successful `clean build`.
- `t4-architect.md#Target structure and boundaries` → `android/app/build.gradle.kts` targets minSdk 28 / compileSdk 36 / targetSdk 36 and Java 17; package is `com.nutria.app`.
- `t10-devops.md#Target Environment Preparation` and `checkpoints/t8-environment.md#Exact activation instructions` → the build used the explicit user-local JDK 17 / Android SDK activation and verified Android SDK Platform 36 and Build Tools 36.0.0.
- `constitution.md#Principles I–V` → only native Compose is scaffolded; no PWA/backend source was removed or rewritten, and build evidence is separated from emulator/device validation.

## Implementation Notes

- Pinned Android Gradle Plugin 9.5.0 with Gradle 9.6.0; wrapper distribution checksum is configured and the wrapper JAR SHA-256 matches Gradle's published 9.6.0 checksum.
- AGP 9.5.0 supplies built-in Kotlin (KGP 2.2.10); the Compose compiler plugin is pinned to 2.2.10 and the module intentionally does not apply the now-unneeded `org.jetbrains.kotlin.android` plugin.
- Compose BOM 2026.06.01 is pinned for the API 36 target. Compose 1.12 / BOM 2026.08 moved to compileSdk 37, so it is not used with the prepared API 36 SDK.
- No OpenAI key, backend request, Health Connect permission, BLE permission, health data, or network capability is added in this scaffold.
- The starter activity only renders the app name. Spanish navigation is assigned to T003; all feature implementations remain pending.
- The host has no ready emulator/device: `adb devices -l` listed none and `emulator -accel-check` returned 6 (`Android Emulator hypervisor driver is not installed`). Instrumentation was compiled and packaged but not run.

## Test Results

- Command: `cd android; .\\gradlew.bat clean build assembleDebugAndroidTest`
- Passed: 1 unit test (`ScaffoldTest.nativeApplicationNamespaceIsStable`); full debug/release build; Android instrumentation source compilation and APK packaging.
- Failed: 0.
- Skipped: 0 executed tests.
- Instrumentation runtime: not run; no connected device and emulator acceleration is unavailable (exit 6). This is an environment limitation, not a passing device test.
- Build notices: AndroidX graphics path native library could not be stripped and was packaged as-is; Gradle build still succeeded.

## Remaining Work / Risks

T003 must implement and validate the Spanish navigation shell. Local encrypted records, diary, water, recipes, calendar, cycle/journal, settings, Health Connect, BLE workouts, photo analysis, and ingredient lookup are not implemented by this scaffold. Emulator and real-device validation remain pending. The legacy PWA and ASP.NET Core APIs remain the required preserved source boundary.
