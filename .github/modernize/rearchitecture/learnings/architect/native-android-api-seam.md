# Native Android client with frozen server APIs

When replacing a browser client with native Android while preserving server APIs, scope the new runtime to the Android client and keep the existing backend routes as explicit frozen seams.

## What Happened

In Nutria task t2 (2026-11-14), the current ASP.NET Core host exposed two independently consumed contracts: multipart photo analysis and ingredient lookup. The PWA kept diary, recipe, cycle, settings, and calendar records in one browser-local `nutria.v1` value; no Android runtime or local-data transfer path existed. The user required all behaviors in native Android and specifically retained the API contracts while keeping the legacy PWA until parity.

## Takeaway

Record each explicitly preserved endpoint as a declared seam, map the native request/response semantics at the client boundary, and keep server files outside rewrite scope unless evidence requires a change. Treat old browser data transfer as a separate design question; identical feature behavior does not automatically migrate browser-local records. Do not select an Android language or toolchain in analysis unless the user/design phase has done so.

## History
- 2026-11-14 (Nutria/t2): initial
- 2026-11-14 (Nutria/t4): added native client boundary, Health Connect aggregation, and BLE workout lifecycle decisions

## Additional Takeaway

For the design phase, a native Kotlin/Compose client can remain a single Android app module with feature packages and UI/domain/data boundaries. Use Health Connect's step aggregate without a hardcoded `DataOrigin` filter, request read-only steps access, and model unavailable, denied, no-record, and error states separately. Keep GATT connection/workout lifecycle in a service-owned adapter; only timestamp actual Heart Rate Measurement notifications and persist interruptions as gaps. Verify current platform/JDK requirements rather than assuming an installed Java version implies an Android build.
