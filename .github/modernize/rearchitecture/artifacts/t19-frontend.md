# T19 — BLE connection and explicit workouts

## Status

**Complete for the BLE/workout source and host build slice.** The previous shared-lint failure was resolved in t19.1; this retry adds bounded GATT setup and closes scanning/connection safely when permissions are lost. The native app now offers a user-started session using actual standard Heart Rate GATT notifications, with encrypted local records and Spanish lifecycle/error states. This is not a full app-parity or device-validation verdict.

## Implementation

- Discovery, device selection, GATT connect/disconnect, and workout start/stop are distinct explicit actions. Compatibility is confirmed only after finding Heart Rate Service `0x180D`, Measurement `0x2A37`, and enabling its `0x2902` notification descriptor.
- A 20-second scan and 30-second connection/notification-setup timeout return the UI to an actionable failure state. Permission loss stops scans, closes GATT, interrupts an open session and clears the displayed reading. Bluetooth-off, link loss, user disconnect, and process recovery remain explicit interruption events.
- Android 12+ uses runtime `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`; Android 11 and older use the legacy Bluetooth declarations plus runtime fine-location permission required for scanning. BLE hardware is optional and checked at runtime.
- An ongoing connected-device foreground service and stop notification are active only after the user starts a workout. The screen shows only parsed, timestamped notifications, marks samples stale after 15 seconds, and preserves observed gaps; it does not calculate calories, exercise minutes, or missing values.
- Sessions, actual received samples and observed events remain in encrypted Room. No band address or raw scan packet is persisted; the BLE/workout flow uses no Nutria API or Health Connect write.
- The PWA and server/API contracts remain unchanged. `README.md` now documents the BLE flow and clearly labels physical-device validation as pending.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1-pm.md` and `t1-pm-spec.md` — product scope and REQ-023–REQ-026 acceptance criteria.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — standard GATT UUIDs, platform permission paths, foreground service, stale/gap rules, and local-only boundary.
- `.github/modernize/rearchitecture/artifacts/t5-ux.md` — Spanish activity, permission, connection, workout, recovery, disclosure, and history states.
- `.github/modernize/rearchitecture/artifacts/t6-dba.md` — workout/session/sample model, encrypted persistence and no remote transfer.
- `.github/modernize/rearchitecture/artifacts/t7-teamlead.md` — required host/device validation and test distinctions.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md` — Step 3.5 / T012 scope and traceability.
- `.github/modernize/rearchitecture/artifacts/t10-devops.md` and `checkpoints/t8-environment.md` — JDK 17 / SDK 36 activation and unavailable emulator/device evidence.
- `.github/modernize/rearchitecture/artifacts/t11-frontend.md`, `t12-dba.md`, `t13-frontend.md`, and `t15-frontend.md` — native app, encrypted Room, navigation, and shared domain/repository contracts.
- `.github/modernize/rearchitecture/artifacts/t18-frontend.md` — integrated Activity screen and Health Connect separation.
- `.github/modernize/rearchitecture/artifacts/t19.1-frontend.md` — shared lint remediation and serialized build procedure.

## Evidence Mapping

- `t1-pm-spec.md#REQ-023` and `t4-architect.md#BLE heart-rate band and workout lifecycle` → `WorkoutBleService.kt` checks BLE support, adapter and runtime permission; only promotes the GATT link after standard service/characteristic discovery and notification subscription. Scan/connect failures and timeouts are recoverable states.
- `t1-pm-spec.md#REQ-024` and `t5-ux.md#Band and workout` → `WorkoutFeatureScreen.kt` separates connection from explicit start/stop; `ActivityRules.kt` parses actual 8-/16-bit HR Measurement values and bounds freshness; `WorkoutRulesTest.kt` verifies malformed values and stale/future readings are not treated as fresh.
- `t1-pm-spec.md#REQ-025` → service event recording, explicit reconnection, 15-second stale state, foreground stop notification, and Room recovery preserve only observed interruption times and samples.
- `t1-pm-spec.md#REQ-026` and `t6-dba.md#Health Connect and BLE boundary` → `WorkoutRoomRepository.kt`, database schema/migrations, Spanish disclosure resources and README describe private local encrypted records; this feature does not call the API or write Health Connect data.
- `t7-teamlead.md#BLE workout (REQ-023–026)` → 61 JVM tests and AndroidTest APK packaging passed; device-dependent checks remain outstanding as required by the strategy.
- Official platform references consulted: [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions) and [foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types). Their `connectedDevice` manifest/runtime prerequisites are represented in `AndroidManifest.xml` and `WorkoutBleService.kt`.

## Changed Files

- Updated `android/app/src/main/java/com/nutria/app/feature/activity/WorkoutBleService.kt` for connection setup timeout and permission/Bluetooth-loss cleanup.
- Updated `android/app/src/test/java/com/nutria/app/feature/activity/WorkoutRulesTest.kt` to assert distinct states, 8-/16-bit parsing, malformed values, no-reading, and stale/future behavior.
- Updated `README.md` with user-facing BLE behavior, permission boundary, local-data disclosure, and explicit device-validation caveat.
- The previous feature implementation and persistence files remain as listed above; no backend route, OpenAI key flow, or PWA source was changed.

## Test Results

- Command: `.\gradlew.bat clean test build assembleDebugAndroidTest --no-daemon --no-parallel --max-workers=1` (from `android/`; OpenJDK 17.0.16 and Android SDK 36 activated in the same PowerShell process).
- Exit code: `0` — `BUILD SUCCESSFUL`; debug/release build, `lint`, `check`, and AndroidTest APK compilation/package completed.
- Passed: 61 JVM unit tests across 14 XML reports, including four `WorkoutRulesTest` methods.
- Failed: 0.
- Skipped: 0 JVM tests.
- Lint: 0 errors; 22 warnings and 1 hint (including existing target/version notices).
- Instrumented tests: 0 executed. The workout persistence instrumentation source compiled and APK packaged but was not run.
- Device evidence: `adb devices -l` listed no devices; `emulator -accel-check` returned 6 (`Android Emulator hypervisor driver is not installed`). No compatible physical BLE band was available.

## Remaining Validation / Issues

- Real Android permission prompts, band discovery, service/characteristic behavior, notification controls, and foreground/background/process recovery remain **unverified on a device**; retain them for t31 device validation. The successful host build and pure Kotlin tests do not establish BLE device behavior or overall feature parity.
- No API endpoint change was made; project-root endpoint contract scripts were absent, and the task did not implement a web backend.
