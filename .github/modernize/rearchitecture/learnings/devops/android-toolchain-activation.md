# Android Toolchain Activation

For Nutria's native build on this Windows host, activate the installed user-local JDK 17 and Android SDK explicitly before invoking the future Gradle wrapper.

## What Happened

Task t10 installed JDK 17 and Android SDK API 36 locally. The machine PATH continued to expose Java 25; setting `JAVA_HOME` without prepending its `bin` did not change `java` command resolution. Android SDK packages were installed with Google's current Android CLI because the legacy `sdkmanager` was absent. The emulator AVD was provisioned but could not boot because no hypervisor driver was installed.

## Takeaway

For Gradle on this host, set `JAVA_HOME` to `C:\Users\ADRIAN\.jdk\jdk-17.0.16` and prepend its `bin` directory to PATH in the same shell; set both Android SDK environment variables to `C:\Users\ADRIAN\AppData\Local\Android\Sdk`. Use the checked-in wrapper after scaffold. Keep emulator/instrumentation status separate from SDK/build readiness; verify `emulator -accel-check` before expecting connected tests to run.

## History
- 2026-11-14 (Nutria/t10): initial
