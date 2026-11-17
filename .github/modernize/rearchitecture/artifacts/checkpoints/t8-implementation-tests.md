# T8 — Android implementation and test evidence

Date: 2026-11-15

## Host verification

Command, from `android/`:

```powershell
.\gradlew.bat test build assembleDebugAndroidTest --no-daemon --no-parallel --max-workers=1
```

Result: exit code 0. The debug and release builds, `lint`, JVM tests, and Android test APK assembly completed successfully. The JVM test reports contain 74 tests, 0 failures, and 0 skipped.

## Physical-device instrumentation

Command, from `android/`:

```powershell
.\gradlew.bat connectedDebugAndroidTest --no-daemon --no-parallel --max-workers=1
```

Result: exit code 0 on OPPO CPH2197 / Android 13. The connected test report contains 30 tests, 0 failures, 0 errors, and 0 skipped. Coverage includes native activity launch, navigation, Compose UI flows, encrypted Room, migrations, diary, recipes, calendar, cycle, settings, weekly activity selection, Health Connect content states, and date-filtered workout persistence.

The test fixtures do not access a real Health Connect provider or a BLE band. The provider read and permission were separately verified manually on the device; BLE GATT connection, foreground workout notification behavior, and physical band loss/recovery remain unverified.
