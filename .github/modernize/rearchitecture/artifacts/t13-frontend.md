# t13 — Spanish native navigation shell

## Status

**Complete for the navigation-shell task only.** The app now has a native Compose drawer with the approved Spanish destinations and accessible Ajustes entry point. This does not claim feature parity: unimplemented destination content remains explicitly marked as unavailable, and Android UI/runtime behavior was not exercised on a device.

## Summary

- Implemented the native single-activity Compose shell with a labeled drawer for `Inicio`, `Comida`, `Recetas`, `Foto`, `Ciclo`, `Plan` and `Actividad`; `Ajustes` remains accessible from the app bar.
- Added selected-destination state, localized labels, accessible menu naming, back-stack de-duplication, the legacy `registrar` → `Comida` route alias, and honest unavailable-screen messaging.
- Formatted the app-bar date as `dd/MM/yyyy` and display numbers with `es-ES`, matching the UX specification.
- Kept the PWA and backend intact. Feature workflows remain owned by their later implementation tasks.

## Deliverables

- [Structured implementation and verification report](./t13-frontend-batch-report.yaml) — task traceability and exact host build/test evidence.
- [Implementation traceability checkpoint](./checkpoints/t13-implementation.yaml) — T003 status, upstream evidence and device limitation.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — native Compose boundary and the app/navigation architecture.
- `.github/modernize/rearchitecture/artifacts/t5-ux.md` — ordered Spanish destinations, drawer behavior, app-bar Ajustes, back-stack rules, accessibility and `dd/MM/yyyy` formatting.
- `.github/modernize/rearchitecture/artifacts/t10-devops.md` and `checkpoints/t8-environment.md` — verified JDK/SDK activation and emulator/device limitation.
- `.github/modernize/rearchitecture/artifacts/t11-frontend.md` — scaffold namespace, Compose and test configuration.
- `.github/modernize/rearchitecture/artifacts/t12-dba.md` and `t12.1-frontend.md` — current shared persistence/build baseline and API test-fixture remediation.
- `.github/modernize/rearchitecture/artifacts/t14-frontend.md` — preserved typed API-client boundary; no shell API changes.
- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md#REQ-001` and `t8-teamlead-plan.md#T003` — navigation and localization requirement and assigned scope.
- `.github/modernize/rearchitecture/board.md` — phase/dependency context; `context.md` and `decisions.md` were not present.

## Evidence Mapping

- `t8-teamlead-plan.md#T003` and `t5-ux.md#Information architecture` → `AppDestination.kt` and `MainActivity.kt`: all seven primary destinations appear in order; Ajustes remains a separate app-bar action.
- `t5-ux.md#Shared layout, content, and interaction rules` → `MainActivity.kt` and `strings.xml`: labeled drawer, selected state, accessible menu action, saved navigation history, and clear staged-availability copy.
- `t5-ux.md#Shared layout, content, and interaction rules` → `SpanishFormats.kt` and `NavigationAndLocaleTest.kt`: numeric Spanish `dd/MM/yyyy` date and `es-ES` number formatting, covered by fixed-value JVM tests.
- `t1-pm-spec.md#REQ-001` → route definitions and tests: Spanish destination labels, `registrar` alias, unknown-route fallback, and back-stack behavior.
- `t10-devops.md#Target-Environment-Preparation` and `checkpoints/t8-environment.md#Exact activation instructions` → the recorded Gradle verification used JDK 17.0.16 and the prepared Android SDK.

## Test Results

- Command: From `android/`, after activating JDK 17.0.16 and Android SDK 36 per t10: `.\gradlew.bat test build assembleDebugAndroidTest --no-daemon`.
- Passed: 20 JVM unit tests; debug and release builds, lint/check, and debug instrumentation APK compilation/package.
- Failed: 0.
- Skipped: 0 JVM tests.
- Android UI/instrumentation: 2 navigation test methods are present and the instrumentation APK packaged successfully; 0 methods were executed because `adb devices -l` reported no connected device and the prepared emulator is blocked by missing host acceleration support.
- No backend/API endpoint behavior changed; this task did not make live service requests.

## Remaining Validation and Scope

- The Compose screen journeys still require execution on a supported emulator or physical Android device.
- Destination feature behavior (diary, water, recipes, calendar, cycle/journal, settings, photo analysis, Health Connect and BLE) is not delivered by this shell and remains assigned to downstream tasks.
- This task's completion is not an app-parity or migration-completion verdict.
