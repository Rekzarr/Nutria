# Android Native Scaffold Build Baseline

Nutria's native Android scaffold uses a single Kotlin/Compose app module with AGP 9.5 and a checksum-pinned Gradle wrapper.

## What Happened

In Nutria task t11 (2026-11-14), official Android and Gradle documentation verified AGP 9.5.0 with Gradle 9.6.0, JDK 17, and Build Tools 36.0.0. AGP's published metadata showed built-in Kotlin/KGP 2.2.10, so the scaffold applies the matching Compose compiler plugin but not the superseded `org.jetbrains.kotlin.android` plugin. It pins Compose BOM 2026.06.01 to stay with the API 36 target; the later Compose 1.12 / August BOM requires API 37.

## Takeaway

Keep the wrapper's Gradle ZIP checksum and verify the wrapper JAR against Gradle's published checksum. On this host, activate the user-local JDK 17 and Android SDK in the same shell before building. For AGP 9+, use built-in Kotlin and align the Compose compiler plugin with AGP's KGP; choose a Compose BOM compatible with the project's compileSdk rather than blindly using the newest BOM.

## History
- 2026-11-14 (Nutria/t11): initial
