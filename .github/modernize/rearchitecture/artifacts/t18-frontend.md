# t18 — Implement Health Connect steps

## Summary

Completed the read-only daily step slice and cleared the earlier shared-lint blocker. The implementation checks provider availability and `READ_STEPS` before a DST-aware local-day aggregate, requests permission only from a user action, refreshes on foreground return, and presents distinct Spanish states. Step totals remain transient UI state: Nutria does not write, persist, or upload them. Corrected the Health Connect Play Store onboarding deep link and added fall-back DST coverage. This completes the Health Connect source/build slice, not real-device provider validation or application-wide parity.

## Deliverables

- [t18-frontend-report.yaml](./t18-frontend-report.yaml) — requirement traceability, test results, and remaining device-validation boundary.
- [checkpoints/t18-tasks-to-impl.yaml](./checkpoints/t18-tasks-to-impl.yaml) — task-to-requirement implementation checkpoint.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1-pm.md` — product scope.
- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md` — REQ-021 and REQ-022 acceptance requirements.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — Health Connect data boundary and aggregate behavior.
- `.github/modernize/rearchitecture/artifacts/t5-ux.md` — Spanish Activity card, states, and recovery actions.
- `.github/modernize/rearchitecture/artifacts/t6-dba.md` — no persistence/upload boundary.
- `.github/modernize/rearchitecture/artifacts/t7-teamlead.md` — verification strategy.
- `.github/modernize/rearchitecture/artifacts/t10-devops.md` and `checkpoints/t8-environment.md` — Android build activation and device limitation.
- `.github/modernize/rearchitecture/artifacts/t11-frontend.md` — native app scaffold.
- `.github/modernize/rearchitecture/artifacts/t13-frontend.md` — Spanish navigation and resource conventions.
- `.github/modernize/rearchitecture/artifacts/t15-frontend.md` — typed Health Connect states and local-day rules.
- `.github/modernize/rearchitecture/artifacts/t16-frontend.md` — current shared app/build state before retry.
- `.github/modernize/rearchitecture/artifacts/t19.1-frontend.md` — shared lint remediation and serial verification procedure.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md` — implementation task T011 and feature-slice scope.

## Evidence Mapping

- `t1-pm-spec.md#REQ-021` → `HealthConnectStepsDataSource.kt`, `HealthConnectStepsFeature.kt`, and Home/Activity cards show actual positive Health Connect daily aggregates with source and refresh time.
- `t1-pm-spec.md#REQ-022` → `HealthConnectPermissionState.kt`, Spanish strings, rationale activity, and explicit permission launch distinguish permission, provider, no-data, and read-error conditions.
- `t4-architect.md#Daily steps — Android Health Connect` and official Android steps guide → `StepsRecord.COUNT_TOTAL` with no `DataOrigin` filter, provider/permission checks, and DST-aware local-day bounds; no background read or persistence path.
- `t5-ux.md#Actividad — Health Connect and BLE / Steps` → distinct Spanish card states, recovery buttons, live-region status text, and data-flow disclosure.
- `t6-dba.md#Health Connect and BLE boundary / Steps` → the Health Connect result is held only in Compose state; no Room, DataStore, or API health-data model is introduced.
- `t7-teamlead.md#Primary Validation Stack` → complete JVM suite and AndroidTest APK packaging passed; device execution remains separate.
- `checkpoints/t8-environment.md#Emulator and device validation` → current `adb devices -l` is empty and `emulator -accel-check` exits 6, so provider/device behavior remains unverified.
- `t15-frontend.md#Evidence Mapping (REQ-021–REQ-022)` → typed result states and 23-/25-hour DST day-boundary test assertions.
- `t19.1-frontend.md#Test Results` → serialized full Gradle verification completed after shared lint remediation.
- `t8-teamlead-plan.md#Step 3.4 / T011` → Health Connect slice and requirement traceability.

## Test Results

- Command: `.\gradlew.bat clean test build assembleDebugAndroidTest --no-daemon --no-parallel --max-workers=1` (from `android/`, with JDK 17.0.16 and Android SDK 36 activated in the same PowerShell process).
- Passed: 59 JVM tests; debug and release builds; `lint`, `check`, and Android instrumentation APK compilation/package.
- Failed: 0. Skipped JVM tests: 0. Android instrumentation executions: 0 (not run).
- Lint: 0 errors, 22 warnings, 1 hint; the full Gradle command completed successfully. Remaining warnings do not block this slice.
- Structural checks: manifest declares `android.permission.health.READ_STEPS` and no `WRITE_STEPS`; Android resources parse and contain no duplicate names.
- Device check: `adb devices -l` listed no devices; `emulator -accel-check` exited 6 because the Android Emulator hypervisor driver is unavailable. Health Connect permission UI, provider aggregation, and live-device behavior therefore remain unverified.

## Scope and Remaining Validation

No backend route, OpenAI key handling, PWA source, or server API contract changed. The README retains the PWA during migration and labels Health Connect device validation as pending. Do not treat JVM fakes or instrumentation APK packaging as proof of provider/device behavior or overall feature parity.
