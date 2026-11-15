## 2026-11-14T17:58:14Z — ux / t5

UX specification is ready at `.github/modernize/rearchitecture/artifacts/t5-ux.md`; it defines seven-destination phone navigation, Spanish feature flows, accessibility expectations, and truthful Health Connect/BLE states.

## 2026-11-14T19:29:43Z — dba / t12

CRITICAL: The shared full Gradle suite failed 9 of 18 tests in `NutriaApiClientTest` because HTTP MockWebServer URLs were rejected by HTTPS policy. Please fix the test setup without weakening release HTTPS policy and rerun the full suite. t12 remains blocked until the full suite passes.

## 2026-11-14T19:29:43Z — frontend / t13

The navigation shell is implemented, but t13 reports verification blocked by the shared API-client test failures and concurrent t12 persistence sources with unresolved dependencies. Do not treat t13 as complete until sources are reconciled and the full suite is green.

## 2026-11-14T19:29:43Z — coordinator / user follow-up

New acceptance: keep project documentation in accessible Spanish for readers unfamiliar with Android, including architecture orientation, installation/execution, existing backend/API configuration, Health Connect and BLE permission/data flows, device testing, and troubleshooting. Update documentation when behavior or architecture changes; do not claim unverified steps. Avoid unnecessary planning docs.

## 2026-11-14T20:42:00Z — coordinator / shared build remediation

The priority feature reports t16, t18, t19, t20, and t22 record cross-task Gradle failures. t16/t18/t19/t22 have unresolved HIGH/CRITICAL build findings and must be re-dispatched to clean PASS after remediation; t20 has no completed test verdict and also needs a clean rerun. The t19 lint report identifies widespread `LocalContextGetResourceValueCall` issues, first at `WorkoutBleService.kt:355`, with another report first at `CycleScreen.kt`; do not suppress lint or weaken release HTTPS policy. t20 also observed Gradle/KSP resource-output contention during concurrent builds. Use a serialized verification sequence after fixing shared lint issues.
