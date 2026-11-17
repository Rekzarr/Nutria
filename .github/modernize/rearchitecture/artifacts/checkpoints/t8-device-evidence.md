# T8 — Android device evidence

Date: 2026-11-15

## Device and run

- Device model: OPPO CPH2197
- Android version: 13
- ADB connection: authorized device
- Command: `.\gradlew.bat connectedDebugAndroidTest --no-daemon --no-parallel --max-workers=1`
- Result: exit code 0; 30 tests passed, 0 failed, 0 errors, 0 skipped

## Evidence boundary

The connected tests exercised Android activity startup, navigation and Compose flows on this device, plus encrypted Room persistence, migrations, settings, cycle, diary, recipe/calendar, Health Connect UI states, weekly navigation bounds, and date-filtered workout persistence.

A manual check granted Nutria only the Health Connect steps read permission. Nutria displayed a non-zero total for today with Health Connect as the source; pressing Refresh completed another read successfully. Selecting previous dates queried Health Connect for those dates and displayed its no-data state where no records were available. The personal step total is intentionally not recorded here.

No compatible heart-rate band was paired, so BLE scanning, GATT notifications, background notification behavior, connection loss, and recovery were not validated with hardware. Those remain open device-evidence requirements. No device serial or unique ADB identifier is recorded.
