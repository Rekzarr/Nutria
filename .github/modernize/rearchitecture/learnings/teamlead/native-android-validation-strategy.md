# Native Android Validation Strategy

For a native Android rewrite, separate host-side logic tests, local persistence, Compose instrumentation, Health Connect, and physical BLE evidence.

## What Happened

Nutria task t7 designed a test strategy for a Kotlin/Compose app replacing a Spanish PWA while retaining existing ASP.NET API boundaries. The host had no Android SDK, Gradle, ADB, emulator, Docker, or Node.js, but did have Java 25 and .NET 11. No legacy test suite existed. Docker and Playwright were correctly treated as unnecessary for Room-backed native tests, while Compose instrumentation and real sensor/device checks remain required rather than being replaced by browser tests or mocks.

## Takeaway

For future native Android rewrites, distinguish deterministic fake-adapter coverage from actual platform/device proof. Require a compatible JDK + Android SDK + wrapper for build/instrumentation, Health Connect provider/permission/sample evidence for steps, and a physical compatible band for BLE readings. Missing Android tools block runtime claims; missing Docker/Node alone does not. Keep API secrets and user records out of client test fixtures and reports.

## History
- 2026-11-14 (Nutria/t7): initial
