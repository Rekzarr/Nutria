## [t2] Analyzed existing PWA and native migration boundary
- Codebase/domain discoveries: The current client is one 1,969-line JavaScript runtime and one 501-line HTML shell; browser state is stored as a single `nutria.v1` localStorage object. The ASP.NET host owns two external-service routes, with the OpenAI key read only server-side.
- Wrong assumptions and corrections: The PWA has a manifest but no service-worker registration/file was found, so offline caching cannot be inferred. There is no existing Android project or target language choice.
- Debugging dead-ends and what actually worked: Python, Node, yq, Ruby, and YAML-aware PowerShell commands were unavailable; structural artifact gates were run with PowerShell regex/source-path checks instead.
- Techniques/patterns worth reusing for future tasks: Keep UI and API route artifacts separate; register monolithic frontend files as shared modules instead of duplicating source anchors; treat explicit user API-preservation requirements as declared frozen seams.
- Learnings consumed: [(none)]

## [t4] Designed native Android target architecture and preserved API contracts
- Codebase/domain discoveries: Existing APIs remain the only remote Nutria client seams; all personal records, including activity/workout records, belong in app-private local persistence. Health Connect has different availability across Android versions and step-source attribution changed in 2026; aggregate without hardcoded origin filters.
- Wrong assumptions and corrections: Kotlin/Compose selection is a design-phase decision, not a fact from the source app. Android tooling is still absent; JDK 25 does not establish a working Android Gradle toolchain.
- Debugging dead-ends and what actually worked: Official docs confirmed Health Connect 1.1.0+ requires compileSdk 36, Health Connect is only available from Android 9 despite SDK support from API 26, BLE permissions differ at API 31, and Android 14 requires foreground-service type declaration.
- Techniques/patterns worth reusing for future tasks: Preserve endpoints with typed client adapters and explicit Spanish error mappings; keep external APIs out of screens; persist actual timestamped BLE notifications with interruption markers; do not model permission denial/no data as zero.
- Learnings consumed: [architect/native-android-api-seam]
