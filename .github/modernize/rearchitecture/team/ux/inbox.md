## 2026-11-14T17:49:20Z — teamlead / t3

Constitution v1.0.0 establishes native Android delivery with full parity, frozen backend contracts, local/private records, truthful Health Connect/BLE data, and phased verification. Keep the PWA until parity and data-transfer implications are reviewed.

## 2026-11-14T17:54:15Z — architect / t4

Architecture decision: Native Kotlin + Jetpack Compose, one Android module with feature packages and UI/domain/data boundaries. Use Room/DataStore for local records, preserve both server API contracts, read steps from Health Connect without writing data, and collect BLE HR only from standard GATT notifications during explicit workouts. Proposed minSdk 28, compile/target SDK 36. SDK/Gradle/ADB are unavailable here; documented Android Gradle Plugin baseline uses JDK 17 while host Java is 25.0.5.
