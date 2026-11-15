# BLE Workout Lifecycle

Nutria's BLE workout adapter owns explicit connection/session lifecycle, bounded setup, and local-only actual readings.

## What Happened

In Nutria task t19 (2026-11-14), a native BLE workflow was completed around the standard Heart Rate GATT Service, Measurement characteristic, and CCCD. A 20-second scan and 30-second GATT/subscription setup timeout keep pending states bounded. Permission loss and Bluetooth-off now stop scanning, close GATT, mark the active session interrupted, and clear the live reading. Host tests validate parsing and freshness, but no band or Android device was available to validate GATT behavior.

## Takeaway

Keep the GATT adapter and active-workout foreground service lifecycle in one owner. Mark a band compatible only after notification subscription succeeds; cancel setup timeouts on success and all teardown paths. On revocation or transport loss, persist only the observed interruption and actual timestamped readings. Treat JVM tests and APK packaging as host evidence, not proof of device behavior.

## History
- 2026-11-14 (Nutria/t19): initial
