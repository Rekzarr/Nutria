# Android Target Environment Preparation

**Task:** t10 (devops) — Prepare and verify the Android target toolchain  
**Captured:** 2026-11-14T18:52:47Z  
**Workspace:** `D:\Proyectos\Nutri`

## Target Environment Preparation

**Status: READY** for Android source scaffolding and future command-line builds when the activation instructions below are used. This status means the target JDK and Android compile SDK/build tools resolve correctly; it does **not** mean an application build, emulator run, instrumentation suite, or physical-device validation passed. The emulator/device tier is separately **BLOCKED**.

### Requested target versions

- Kotlin + Jetpack Compose, one native Android app module (t4).
- `minSdk 28`, `compileSdk 36`, `targetSdk 36`.
- JDK 17 build baseline.
- Current Android Gradle Plugin compatibility reference: AGP 9.5.0 uses Gradle 9.6.0, SDK Build Tools 36.0.0 and JDK 17.
- Android CLI used for SDK provisioning. Project-specific Gradle wrapper and dependency pins are assigned to scaffold task t11.

### Preparation actions

- Installed Microsoft OpenJDK 17 through the available trusted JDK installer: `C:\Users\ADRIAN\.jdk\jdk-17.0.16`.
- Verified the Android CLI package from the official Google `Google.AndroidCLI` winget package metadata. Winget verified the installer SHA-256 before install; the CLI was invoked with `--no-metrics` for subsequent operations.
- Installed the Android SDK under `C:\Users\ADRIAN\AppData\Local\Android\Sdk`:
  - `platforms/android-36` — 2.0.0
  - `build-tools/36.0.0` — 36.0.0
  - `platform-tools` — 37.0.1
  - `emulator` — 37.2.13
  - `system-images/android-36/google_apis/x86_64` — 7.0.0
  - `system-images/android-36/google_apis_playstore/x86_64` — 7.0.0 (fetched by AVD provisioning)
- Created `medium_phone` AVD successfully.
- Set user-scoped `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, and PATH entries for SDK `platform-tools` and `emulator`.

### Installed versions and command resolution

| Capability | Evidence |
|---|---|
| JDK 17 | `C:\Users\ADRIAN\.jdk\jdk-17.0.16\bin\java.exe -version` → OpenJDK 17.0.16 LTS |
| Android SDK | `C:\Users\ADRIAN\AppData\Local\Android\Sdk\platforms\android-36\android.jar` exists |
| Build Tools | `...\build-tools\36.0.0\aapt2.exe` exists |
| ADB | `...\platform-tools\adb.exe version` → 1.0.41 / Platform-Tools 37.0.1 |
| Emulator | `...\emulator\emulator.exe -version` → 37.2.13.0 |
| Android CLI | Google Android CLI package; runtime reported 1.1.16919734 |
| Gradle | No machine-global `gradle` command. This is intentional: app build uses the checked-in wrapper to be created by t11. |
| Wrapper/project | `D:\Proyectos\Nutri\android` and `android\gradlew.bat` do not yet exist. |
| Physical device / band | None connected or provided. |

The original shell's machine PATH resolves Java 25.0.5.21, so do not run the Android Gradle wrapper in that environment without activating JDK 17. User-scoped `JAVA_HOME` is also set to the JDK 17 path; the explicit activation block below prepends it to PATH and was verified to resolve `java.exe` from that JDK.

### Exact activation instructions

In the PowerShell terminal that will run Android builds (or after restarting it to load user-scoped variables), use:

```powershell
$env:JAVA_HOME = 'C:\Users\ADRIAN\.jdk\jdk-17.0.16'
$env:ANDROID_HOME = 'C:\Users\ADRIAN\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$androidCli = 'C:\Users\ADRIAN\AppData\Local\Microsoft\WinGet\Packages\Google.AndroidCLI_Microsoft.Winget.Source_8wekyb3d8bbwe'
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$androidCli;$env:PATH"
java -version
```

Verify the first line reports OpenJDK `17.0.16`. After t11 creates the project and pinned wrapper, run from `D:\Proyectos\Nutri\android`:

```powershell
.\gradlew.bat clean build
```

The intended wrapper distribution is Gradle 9.6.0 to match AGP 9.5.0 compatibility guidance. This is a downstream project configuration, not an installed global Gradle command, and has not yet been built or verified.

### Emulator and device validation

- AVD creation succeeded, but the host cannot use it currently: `emulator -accel-check` returned exit code `6` and stated `Android Emulator hypervisor driver is not installed on this machine`.
- `android emulator start --headless medium_phone` did not report readiness after approximately 360 seconds; the attempt was stopped. A subsequent `adb devices -l` listed no devices.
- BIOS virtualization is enabled on the Intel CPU, but an emulator hypervisor driver is absent. No driver installation or Windows virtualization feature change was attempted because it would require elevated host changes and could require a reboot.
- No physical Android device, Health Connect provider, or compatible BLE band is present. Emulator success would not replace physical Health Connect/BLE validation.

### Missing tools and downstream implications

- A checked-in `gradlew.bat` is not present until t11; there is no global Gradle installation. This is expected and supports reproducible project builds via the wrapper.
- `sdkmanager` is not installed; SDK packages were installed and enumerated using Google's current Android CLI. The Android CLI executable can be resolved in a new shell via the winget user PATH or the absolute path above.
- No app source project exists yet, so a native build, unit tests, instrumentation tests, or app startup could not be attempted. Do not claim such validation.
- t11 and source work may proceed. Build verification can start only after scaffold adds the Gradle wrapper. Compose instrumentation and emulator flows remain blocked until a hypervisor driver or supported Windows hypervisor is enabled and `emulator -accel-check` passes. Physical Health Connect/BLE acceptance remains a later device gate.

## Verification Evidence

### Passed (7 readiness checks)

1. JDK 17 resolves with the explicit activation block.
2. Android CLI installation succeeded; winget installer hash verified.
3. API 36 platform package and `android.jar` are present.
4. Build Tools 36.0.0 and `aapt2.exe` are present.
5. ADB resolves and reports Platform-Tools 37.0.1.
6. Emulator executable resolves and reports 37.2.13.0.
7. `medium_phone` AVD creation succeeded and SDK package inventory confirms the installed target.

### Failed / blocked (1 platform capability)

- Command: `C:\Users\ADRIAN\AppData\Local\Android\Sdk\emulator\emulator.exe -accel-check`
- Result: exit code `6`, Android Emulator hypervisor driver not installed.
- Follow-up: `android emulator start --headless medium_phone` did not reach readiness in approximately 360 seconds; stopped. `adb devices -l` showed no device.

### Skipped (2 checks)

- Full Gradle build: no `android/` project or wrapper exists until t11, per T001 scope.
- Connected instrumentation: no booted emulator/device.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — native target and JDK/SDK baseline.
- `.github/modernize/rearchitecture/artifacts/t7-teamlead.md` — emulator and platform/device requirements.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md` — T001 preparation-only boundary and post-scaffold build command.
- `.github/modernize/rearchitecture/artifacts/t9-teamlead-validation.md` — honest blocker reporting and no inferred Android build result.
- `.github/modernize/rearchitecture/artifacts/constitution.md` — Android build/runtime claims require actual verification.
- `.github/modernize/rearchitecture/board.md` — current task sequencing.

## Evidence Mapping

- `t4-architect.md#Platform-and-build-target` → installed JDK 17, Android API 36 and Build Tools 36.0.0; wrapper version and exact build activation are documented above.
- `t7-teamlead.md#Environment-Requirements-and-Observed-Availability` → installed SDK/ADB/AVD evidence and emulator acceleration/device blockers.
- `t8-teamlead-plan.md#Task-Breakdown-T001` → readiness inspection completed without attempting a pre-scaffold build.
- `constitution.md#Principle-V` → no native build/runtime pass is inferred or claimed.

## References

- Android Gradle Plugin compatibility: https://developer.android.com/build/releases/gradle-plugin
- Android CLI downloads: https://developer.android.com/tools/agents/android-cli/download
- Android CLI SDK install: https://developer.android.com/tools/agents/android-cli/commands/sdk_install
- Android CLI emulator commands: https://developer.android.com/tools/agents/android-cli/commands/emulator
