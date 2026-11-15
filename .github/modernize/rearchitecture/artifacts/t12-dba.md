# t12 — Encrypted private persistence foundation

## Status

**COMPLETE for the shared persistence foundation.** The full clean Gradle build and JVM suite now pass after the API-client test fixture remediation in t12.1. Android instrumentation is compiled and packaged but remains unexecuted because this host has no connected device and its emulator cannot start; this is a documented runtime-validation limitation, not a build/test failure.

## Summary

- Added Room 2.8.5 with SQLCipher Android 4.20.0 Room 2 integration; API 36 dependency metadata validation passed.
- Generated a per-install 256-bit random DB passphrase, wrapped it with AES-GCM under a non-exportable Android Keystore AES key, and atomically stored only the encrypted envelope in `noBackupFilesDir`.
- Added an app-scoped Room provider that authenticates/opens on `Dispatchers.IO`, fails without deleting or recreating data, registers explicit migrations with no destructive fallback, and clears its in-memory passphrase on close.
- Added only a bootstrap metadata entity; feature-owned tables, DAOs, migrations, and transaction invariants remain with their assigned vertical tasks.
- Added a typed DataStore boundary for non-health display/connection preferences only; goals and user/health records are not included.
- Kept `allowBackup=false` and added explicit legacy plus API 31+ exclusions for cloud backup and device transfer, including root, files, databases, preferences, and external files.
- The database provider is not yet wired into application startup; vertical feature/repository tasks will consume it.

## Deliverables

- [t12-dba-batch-report.yaml](./t12-dba-batch-report.yaml) — implementation file inventory, traceability, and validation status.
- [checkpoints/tasks-to-impl.yaml](./checkpoints/tasks-to-impl.yaml) — T004 traceability and successful host validation state.
- `android/app/schemas/com.nutria.app.data.local.NutriaDatabase/1.json` — Room-exported initial bootstrap schema.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — app-private storage boundary, one-module package structure, and Room/DataStore ownership.
- `.github/modernize/rearchitecture/artifacts/t6-dba.md` — encryption/key lifecycle, fail-closed rules, schema scope, sensitive data boundaries, and backup exclusions.
- `.github/modernize/rearchitecture/artifacts/t10-devops.md` — JDK 17 / API 36 readiness and exact build activation.
- `.github/modernize/rearchitecture/artifacts/checkpoints/t8-environment.md` — target SDK packages are build-ready; emulator/device runtime remains blocked.
- `.github/modernize/rearchitecture/artifacts/t11-frontend.md` — existing Gradle/Compose scaffold and Kotlin/AGP versions to preserve.
- `.github/modernize/rearchitecture/artifacts/t12.1-frontend.md` — shared-test fixture remediation and prior successful suite evidence.
- `.github/modernize/rearchitecture/artifacts/t14-frontend.md` — API-client loopback/HTTPS policy and unit-test scope; no change to production transport policy.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md#T004` — shared encrypted storage only; no feature schemas; required tests.
- `.github/modernize/rearchitecture/learnings/dba/android-private-health-persistence.md` — keep Health Connect ephemeral, isolate non-health preferences, and exclude data transfer.

## Evidence Mapping

- `t8-teamlead-plan.md#T004` → `data/local/EncryptedDatabaseProvider.kt`, `DatabaseKeyVault.kt`, `NutriaDatabase.kt`, `PreferencesStore.kt`, manifest backup XML, and the focused JVM/instrumentation tests.
- `t6-dba.md#Encryption,-key-lifecycle,-and-recovery` → AES-GCM envelope under the Keystore alias, database/WAL/journal orphan detection, zeroization, and fail-closed exceptions; tested by `EncryptedDatabaseInstrumentedTest` (compiled and packaged, runtime not yet executed).
- `t6-dba.md#Backup,-retention,-and-deletion` → `allowBackup=false`, `backup_rules.xml`, `data_extraction_rules.xml`, and `BackupExclusionInstrumentedTest` (compiled and packaged, runtime not yet executed).
- `t6-dba.md#Proposed-local-model` → Room schema version 1 with only `storage_metadata`; exported schema is present under `android/app/schemas/`. No feature entity or health-data cache was introduced.
- `t4-architect.md#Target-structure-and-boundaries` plus `t11-frontend.md#Implementation-Notes` → implementation uses the existing Kotlin 2.2.10 / AGP 9.5.0 / API 36 scaffold; the full clean debug/release build and instrumentation package now pass.
- `t10-devops.md#Target-Environment-Preparation` and `checkpoints/t8-environment.md#Emulator-and-device-validation` → commands used JDK 17.0.16; instrumentation was package-verified only because no emulator/device is available.

## Test Results

- Command: `Set-Location android; .\gradlew.bat clean build assembleDebugAndroidTest --no-daemon` (JDK 17.0.16 and Android SDK 36 activated in the same PowerShell process).
- Result: `BUILD SUCCESSFUL`, exit code 0; 131 actionable tasks, including debug/release builds, lint/check, and instrumentation APK compilation/package.
- Passed: 20 JVM unit tests (1 persistence migration-registry test plus the shared navigation, scaffold, and API-client tests).
- Failed: 0.
- Skipped: 0 JVM unit tests.
- Android instrumentation: test sources compile and package; runtime not executed because `adb devices -l` reports no connected devices and the prepared emulator is blocked by missing host acceleration support (per t10).
- Build notices: AGP's `android.disallowKotlinSourceSets=false` compatibility opt-out is experimental; SQLCipher and other native libraries were packaged without symbol stripping. Neither notice failed the build.

## Remaining Validation

The shared encrypted Room/SQLCipher bootstrap, Keystore-wrapped key envelope, non-health DataStore boundary, and backup exclusion configuration are implemented. Feature-owned entities/DAOs and application wiring remain with downstream vertical tasks. Keystore encryption/reopen and backup-rule assertions still require instrumentation execution on a supported emulator or physical Android device; no such runtime pass is claimed here.

## References

- [SQLCipher for Android and Room integration](https://github.com/sqlcipher/sqlcipher-android)
- [SQLCipher Android Maven metadata](https://repo.maven.apache.org/maven2/net/zetetic/sqlcipher-android/maven-metadata.xml)
- [Android Room persistence](https://developer.android.com/training/data-storage/room)
- [Android Auto Backup and data extraction rules](https://developer.android.com/identity/data/autobackup)
- [Android Keystore system](https://developer.android.com/privacy-and-security/keystore)
- [KSP / AGP built-in Kotlin source-set compatibility issue](https://github.com/google/ksp/issues/2729)
