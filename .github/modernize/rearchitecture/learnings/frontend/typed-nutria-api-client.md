# Typed Nutria API client

The Android client uses a small typed OkHttp boundary for only the frozen Nutria photo and ingredient routes.

## What Happened

In Nutria task t14 (2026-11-14), a cancellation-aware OkHttp callback API was wrapped as suspending typed results; the client validates query/image constraints, maps Spanish HTTP errors, requires HTTPS except explicitly enabled loopback debug, uses a 110-second call timeout, and disables automatic retries. Local MockWebServer tests use an explicit `127.0.0.1` URL because this host's reverse-DNS canonical hostname resolved loopback to `activation.easeus.com`; do not trust `MockWebServer.url()` host text blindly in this environment. In t12.1, the shared fixture and full forced Gradle rerun passed, and a regression assertion confirmed the debug opt-in does not allow remote cleartext hosts.

## Takeaway

Keep the mobile API boundary narrow: call only the existing Nutria routes, keep all provider credentials/server integrations server-side, send no local personal records, and make costly photo upload caller-triggered with retry disabled. For Android Gradle builds in this project, use the checked-in wrapper and activate JDK 17; the shared KSP migration flag is currently required by AGP 9.5 built-in Kotlin. For MockWebServer, build the test base URL using `127.0.0.1`, opt into the client's local-debug cleartext policy only in the test fixture, and retain an assertion that the same flag rejects non-loopback hosts.

## History
- 2026-11-14 (Nutria/t14): initial
- 2026-11-14 (Nutria/t12.1): documented loopback-only MockWebServer setup and full shared-suite verification
