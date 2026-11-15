# Android private health persistence

Use encrypted Room storage for local health-related records, keep Health Connect reads ephemeral, and persist BLE readings only as timestamped samples tied to explicit workouts.

## What Happened

For Nutria t6, the current `nutria.v1` browser-local model had no Android schema or transfer path. The target design uses SQLCipher-backed Room and an Android Keystore-wrapped random database key, while Health Connect remains the source of steps and BLE measurements are stored only when actually received. Android backup documentation notes that `allowBackup=false` alone may not disable device-to-device transfer on some Android 12+ devices, so explicit extraction rules and device validation are required. No PWA import or backend data store is introduced.

For Nutria t12, implement the bootstrap with SQLCipher Android 4.20.0's Room 2 `SupportOpenHelperFactory`, Room 2.8.5, and a 256-bit random passphrase wrapped with AES-GCM under a non-exportable Keystore key; keep the wrapped envelope in `noBackupFilesDir`. The key byte array must remain alive while Room can open connections and is zeroed when the app-scoped database provider closes. The initial Room schema contains only a storage bootstrap marker; feature tables and migrations remain owned by vertical feature tasks. AGP 9.5/KSP 2.2.10 rejects KSP-generated Kotlin source roots unless `android.disallowKotlinSourceSets=false` is set; keep the exception documented until the KSP/AGP integration supports the built-in Kotlin check directly.

The first t12 full-suite run was blocked by the parallel API client's loopback MockWebServer setup, not by persistence tests. After t12.1 explicitly opted into loopback-only cleartext for tests, the complete clean build and 20 JVM tests passed without changing the release HTTPS policy. Instrumentation APK packaging is not a substitute for running the Keystore/SQLCipher and backup-rule tests on an Android device.

## Takeaway

For sensitive Android records, distinguish encrypted database data from app-private preferences; keep provider-owned health records out of local persistence when a read-on-demand aggregate suffices; never turn missing steps or sensor gaps into numeric values. Exclude both cloud backup and device transfer, and document the resulting uninstall/key-loss consequence. Keep domain dates as local-date keys and event timestamps as instants. If implementing SQLCipher with Room 2, persist the envelope atomically in no-backup storage, fail closed whenever DB state exists without a recoverable envelope, and never rely on destructive Room migration fallbacks.

## History
- 2026-11-14 (Nutria/t6): initial
- 2026-11-14 (Nutria/t12): added encrypted Room/Keystore bootstrap and AGP/KSP compatibility notes
- 2026-11-14 (Nutria/t12 retry): recorded that loopback-only API test remediation cleared the full Gradle gate; instrumentation still needs a device
