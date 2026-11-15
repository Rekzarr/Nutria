# t10 — Android target toolchain preparation

## Summary

**Build toolchain: READY for the Android scaffold and subsequent builds, provided the activation block in the checkpoint is used.** Installed a user-local JDK 17 and Android SDK Platform 36 / Build Tools 36.0.0, and configured user-scoped Android/JDK environment variables. No Android project or wrapper exists yet, as expected before scaffold task t11, so no Gradle build was attempted.

The Android emulator package and API 36 images are installed and an AVD was created, but the emulator cannot be verified running: `emulator -accel-check` exits 6 because no hypervisor driver is installed; the headless start did not reach readiness within approximately 360 seconds. Android instrumentation remains blocked pending host virtualization setup. This is separate from build-toolchain readiness and does not block source implementation under the approved plan.

## Deliverables

- [checkpoints/t8-environment.md](./checkpoints/t8-environment.md) — full target-environment preparation status, exact activation commands, installed package evidence, blockers, and verification results.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — Kotlin/Compose target, JDK 17 baseline, min/compile/target SDK, and pinned-build requirements.
- `.github/modernize/rearchitecture/artifacts/t7-teamlead.md` — required SDK/API 36 and emulator/device validation prerequisites.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md` — T001 scope: inspect and record toolchain readiness only; do not require a wrapper or build before scaffold.
- `.github/modernize/rearchitecture/artifacts/t9-teamlead-validation.md` — confirms missing toolchain must be an explicit blocker, not a WebView substitution or inferred pass.
- `.github/modernize/rearchitecture/artifacts/constitution.md` — no Android build/runtime claims absent actual evidence.
- `.github/modernize/rearchitecture/board.md` — phase and workflow status.

## Evidence Mapping

- `t4-architect.md#Platform-and-build-target` → JDK 17, API 36 and Build Tools 36.0.0 were prepared; Gradle 9.6.0 / AGP 9.5.0 compatibility and downstream command are recorded in `checkpoints/t8-environment.md`.
- `t7-teamlead.md#Environment-Requirements-and-Observed-Availability` → SDK, ADB, emulator and host-device evidence is recorded, including the emulator acceleration blocker.
- `t8-teamlead-plan.md#Task-Breakdown-T001` → environment-only check completed; no pre-scaffold Gradle wrapper/build was attempted.
- `constitution.md#Principle-V` → no application build, test, or runtime pass is claimed.

## Test Results

- Command: Environment/toolchain checks listed in `checkpoints/t8-environment.md`; no Android project build/test command was applicable before t11.
- Passed: 7 readiness checks — JDK 17 activation, Android CLI/SDK package inventory, API 36 platform and build tools presence, ADB resolution, emulator binary resolution, AVD creation, and user-scoped environment configuration.
- Failed: 1 platform capability — emulator acceleration check returned 6; headless emulator startup did not reach readiness within approximately 360 seconds.
- Skipped: 2 — Gradle build (no `android/` project or wrapper exists until t11) and connected instrumentation (emulator unavailable).
- Failure details: `emulator -accel-check` reports `Android Emulator hypervisor driver is not installed on this machine`; `adb devices -l` showed no devices after the launch attempt.
