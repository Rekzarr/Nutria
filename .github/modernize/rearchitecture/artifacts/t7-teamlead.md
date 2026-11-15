# t7 — Native Android parity and runtime validation strategy

## Summary

This is the binding validation design for the native Kotlin/Jetpack Compose client and the unchanged ASP.NET Core API seams. It covers the complete t1 scope, REQ-001–REQ-026. It is a test strategy, not test execution or evidence that the Android app exists or works.

**Current validation state:** no Android project/toolchain/device is available on this host. Android compilation, instrumentation, Health Connect, and physical BLE behavior are therefore **UNVERIFIED**. Do not interpret the existing `dotnet test` probe as product test coverage.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1-pm.md` — inventory index for the full PWA feature and new native scope.
- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md` — REQ-001–REQ-026, journeys, API contracts, and observable acceptance behavior.
- `.github/modernize/rearchitecture/artifacts/t1-pm-requirements-checklist.md` — inventory completeness and distinction between existing and new features.
- `.github/modernize/rearchitecture/artifacts/t2-architect.md` — current architecture, no existing native or test project, and host toolchain evidence.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — Kotlin/Compose architecture, target/min SDK, data/API boundaries, Android permission and lifecycle requirements.
- `.github/modernize/rearchitecture/artifacts/constitution.md` — native parity, frozen API contracts, local privacy, truthful Health Connect/BLE data, and verification gates.
- `.github/modernize/rearchitecture/artifacts/wire_contracts.yaml` — frozen photo, ingredient-search, and server-side provider contracts.
- `.github/modernize/rearchitecture/artifacts/tech-stack.md` — existing ASP.NET Core 10 host and observed environment limits.
- `.github/modernize/rearchitecture/artifacts/data-model.md` — browser-local `nutria.v1` state, no server persistence, and no established import path.
- `board.md` — design-phase sequencing and pending UX/data work.
- `team/teamlead/log.md` and `team/teamlead/inbox.md` — prior constitution decisions, coordinator instruction, and architect handoff.
- `learnings/teamlead/android-rewrite-guardrails.md` — preserve PWA/API and make Android verification claims only from real toolchain evidence.

`context.md` and `decisions.md` were not present in the rearchitecture base at preflight. No `*-fanout.md` topology fallback was present; t2's nine feature/API units are used to organize coverage.

## App Type and Scope

- **appType:** native Android Compose application with local Room/DataStore records and two user-initiated HTTP integrations; existing backend is ASP.NET Core minimal APIs. No browser UI is the delivered target.
- **Parity scope:** preserve the six Spanish PWA destinations and behaviors (REQ-001–REQ-020), then verify the new Health Connect and BLE behaviors (REQ-021–REQ-026). Keep the PWA untouched and available as the behavioral reference until parity and data-transfer implications are explicitly reviewed.
- **Auth behavior:** the inspected API has no authentication. Do not invent an auth flow or require a 401/403 case; verify the existing unauthenticated client/API contract and expected validation/provider errors.
- **No automatic data import:** tests must not imply that browser `nutria.v1` records move to Android. Confirm PWA storage is left intact and no import is claimed.

## Legacy Test Assets and Baseline

- **legacyTestAssets:** none found in the inspected source inventory; no Android app, test project, solution, JS package manifest, or CI test configuration was identified.
- **source:** discovered by t1/t2 full-source inventory and current workspace manifest scan.
- **migrationDecision:** N/A — there is no legacy test suite to rewrite or reuse. Use t1's behavior scenarios and source inventory as the parity baseline.
- **Canonical legacy test command:** none is defined by the project.
- **Baseline probe:** `dotnet test .\Nutria.csproj --no-restore --verbosity minimal` returned exit code `0` with no test output. This is not a test pass or evidence of any tests running; the project has no test project/test assets.
- The PWA's existing behavior is not to be revalidated by browser automation as the migration target. Compare native outcomes against t1 acceptance scenarios and retain the PWA as a non-destructive reference.

## Primary Validation Stack

### 1. Kotlin/domain unit tests — always required

- Use the Android project's pinned Kotlin/JUnit test stack and Gradle host-side unit-test task.
- Cover deterministic domain logic: nutrient scaling and units (including 28.3495 g/oz), macro/day totals, streak boundaries, water undo, recipe totals/per-serving values, recommendation filters/eligibility, calendar generation constraints, cycle date validation/estimates, Health Connect state mapping, BLE GATT measurement parsing, stale-reading/session-gap state transitions, and API error mapping.
- Inject a fixed `Clock`, local `ZoneId`, deterministic recommendation/menu selection, and fake platform/repository adapters. Include timezone/local-midnight and daylight-saving boundaries.
- Do not use fake Health Connect/BLE values as evidence of actual sensor/device integration.

### 2. Local persistence/repository integration — always required

- Test production Room entities/DAOs/repositories against an in-memory SQLite database using the same schema and migration chain; use isolated DataStore test storage for preferences.
- Verify create/read/update/delete and transactional relationships (especially recipe deletion with calendar assignments), day-keyed records, restart persistence, and schema migration behavior.
- Use a unique test database/profile per test. A process-relaunch persistence test must deliberately preserve its database; ordinary tests clean up their isolated state.
- Do not use Docker for the app's local Room/DataStore persistence tier. The app does not require a remote database or account service.

### 3. HTTP consumer-contract tests — always required; no production provider calls

- Run the native HTTP repository against a local mock HTTP server (e.g. MockWebServer if OkHttp is selected; otherwise a compatible local stub). Assert the actual method, path, UTF-8 query encoding, multipart part names/content types, optional fields, and DTO mapping.
- Ingredient search cases: trimmed 1–80 character query, `{ products: [...] }` shape, up to eight results, per-100-g nutrients, empty results, manual-entry fallback, and validation/provider/network/timeout errors.
- Photo cases: `POST /api/analyze-meal`, required `photo`, optional `name`, supported media/10 MiB client boundary, correct JSON mapping, finite/nonnegative result validation, and 400/413/429/502/503/504/network/timeout/invalid-response handling. Assert no diary write on failure and no automatic retry of the cost-bearing POST.
- Assert no request is sent before the user explicitly confirms photo analysis; ingredient requests occur only after a search action. Assert the mobile client contains no OpenAI key/configuration or provider call and does not upload diary, cycle, steps, BLE identity, or workout samples.
- For backend route contract coverage, use an in-process ASP.NET Core test host with stubbed `IMealAnalysisService` and `IOpenFoodFactsService` where the existing route registrations permit it. Do not call OpenAI/Open Food Facts in automated tests and do not change production endpoint behavior for test convenience. If a test host cannot exercise existing routes without modifying the frozen production seams, report that limitation rather than broadening backend scope.

### 4. Compose UI flow tests — required on an Android emulator

- Use AndroidX/Compose UI instrumentation with a clean app-private test profile and injected fake repositories/platform adapters for deterministic non-hardware journeys.
- Cover the complete user flow (setup → action → visible result → persisted read-back → cleanup), not just single-screen presence or endpoint probes.
- Keep Spanish strings, date/number formatting, six existing navigation destinations, narrow phone layouts, theme selection, accessibility labels, dialogs/confirmations, loading/error/empty states, and navigation/lifecycle behavior in scope.
- No Playwright tier applies to the native Android target. Node.js is not a prerequisite for Compose instrumentation.

### 5. Android platform/device validation — mandatory before full sign-off

- **Health Connect:** test provider installed/available and unavailable/update-required states; runtime `READ_STEPS` grant, denial and revocation; actual local-day aggregation; empty day; query error; refresh/foreground behavior; and disclosure. Production app remains read-only. Seed test steps through Health Connect UI or a separate test fixture with write permission that is not included in the production manifest; clean up fixture records after testing.
- **BLE:** use injectable fakes for deterministic state/error coverage, then connect a real generic compatible band advertising the standard Heart Rate service `0x180D` and notifying characteristic `0x2A37`. Verify a real received 8-bit/16-bit reading; no sample may be fabricated for the device test.
- Exercise BLE hardware unsupported, Bluetooth off, Android-version permission denial/revocation, scan/connect failure, missing service/characteristic, lost connection, no/stale samples, explicit connect/disconnect and workout start/stop, foreground notification, background/foreground transition, session interruption/gap, and stop/recovery behavior.
- Emulators may cover UI and permission/state machinery; they cannot substitute for proof of readings from a physical BLE band. Fakes validate state machines only.

## Critical User Journeys and PASS Criteria

1. **Daily nutrition and water (REQ-001–006):** Open Spanish home; verify default goals/empty state; add foods in grams, ounces, and portions; check nutrient totals/streak; add +150/+250 ml and undo only the last increment; review 30-day history; clear today after confirmation and verify recipes, calendar, cycle records, goals, and theme remain. Reopen/relaunch and verify persisted results.
2. **Recipes, search, and recommendations (REQ-007–010):** Create a multi-ingredient recipe, verify total/per-serving nutrition and validation; search via the frozen ingredient API, import then edit per-100-g nutrients or use manual fallback; filter/refresh/inspect/save a recommendation; add a serving to today's diary; delete an assigned recipe and verify calendar references are removed atomically.
3. **Calendar and menu generation (REQ-011–012):** Navigate a Monday-first month; manually assign/remove recipes, including any recipe category; enable/disable breakfast; generate a selected Monday–Sunday week and a month; verify category eligibility/no same-day duplicate where possible; confirm missing-category explanation and replace/cancel behavior; reopen and verify plans persist.
4. **Cycle history and private journal (REQ-013–016):** Create/edit/finalize/delete valid periods; reject future, malformed, reversed, overlapping, and invalid-length data without changing existing records; verify unknown and estimated phase/date states; save flow/pain/mood/note (including 160-character boundary); verify recent records and restart persistence. The Spanish estimate caveat must not imply diagnosis, fertility, pregnancy, or medical advice. Confirm no cycle record/note is sent over the network.
5. **Explicit photo analysis (REQ-017–019):** Select supported image, preview/remove it, and verify no request before explicit confirmation; submit once and verify multipart contract and estimate labeling; add only a valid returned meal. Exercise unsupported/oversized/invalid/network/timeout/rate/provider failures and prove no invalid diary write. Verify no client secret and no direct OpenAI call.
6. **Settings and localization (REQ-001, REQ-020):** Change all goals and each theme, save, cancel a preview, navigate/relaunch, and verify persistence/restoration. Verify Spanish labels and es-ES number/date formatting at narrow width and representative font scaling.
7. **Health Connect steps (REQ-021–022):** Request access only on user action; verify granted steps reflect actual Health Connect records for the user's local day and identify the source. Separately test denied/revoked, provider missing/update required, no records, read failure, refresh, timezone/day-boundary, and disclosure states. No-data is an explicit empty state, not a fabricated zero.
8. **BLE workout (REQ-023–026):** Explicitly connect a compatible band, verify the standard service/characteristic and actual timestamped notifications, then explicitly start/stop and persist a local session/sample. Exercise unsupported/off/permission/incompatible/connect/loss/stale/background paths and verify visible gaps, no fabricated continuity/calories/exercise minutes, local-only disclosure, and no Health Connect/API upload.

## Requirement-to-Evidence Coverage

| Requirement(s) | Required evidence |
|---|---|
| REQ-001 | Compose navigation/localization flow tests; Spanish labels, Monday-first calendar and mobile layout evidence. |
| REQ-002 | Diary/domain, Room, and Compose tests for daily intake/goals, remaining calories/macros, water progress, meal list/count, and streak. |
| REQ-003 | Unit/UI tests for positive amounts, gram/ounce/portion conversion, nutrient preview, validation, and diary write/read-back. |
| REQ-004 | History/repository/UI tests for 30-day grouping, deletion, and prior-day read-only behavior. |
| REQ-005 | Water persistence/UI tests for +150/+250 ml, glass marks, last-increment-only undo, and disabled undo. |
| REQ-006 | Confirmation/cancel and data-isolation tests proving clear-today leaves recipes, plans, cycle records, goals, and theme intact. |
| REQ-007 | Recipe calculation/repository/UI tests for serving count, ingredient completeness, per-recipe/per-serving nutrition, and invalid-save prevention. |
| REQ-008 | Mock HTTP contract and UI tests for query trimming/limits, response shape, per-100-g import, manual edits, empty results, and errors. |
| REQ-009 | Recipe book/diary integration and Room transaction tests for adding a serving and deleting calendar references. |
| REQ-010 | Deterministic recommendation tests for catalog count, filters, up-to-three suggestions, ≤550 kcal, saving, and estimate labeling. |
| REQ-011 | Calendar/Compose tests for Monday-first navigation, manual assign/remove, optional breakfast, persistence, and category-independent manual assignment. |
| REQ-012 | Menu-generation tests for month/week range, category constraints, duplicate avoidance, missing-category abort, and replacement confirmation. |
| REQ-013 | Cycle-record tests for valid dates, correction/finalization, ordering/duration, confirmation, and malformed/future/reversed/overlap rejection. |
| REQ-014 | Fixed-clock cycle estimate tests for 21–45/2–10 constraints, cycle day, unknown/estimated/late states, and next-start calculation. |
| REQ-015 | Local persistence/UI tests for flow, 0–10 pain, mood, 160-character note boundary, recent seven days, and privacy disclosure. |
| REQ-016 | Exact-language review and UI assertion for estimate, non-diagnosis, non-fertility/pregnancy, and professional-advice caveats. |
| REQ-017 | Photo picker/preview/confirmation flow, supported media/size, successful diary result, and approximate-result labeling. |
| REQ-018 | Multipart consumer-contract tests and error-no-write tests for response validation, size/type, rate/provider/network/timeout failures. |
| REQ-019 | No-request-before-confirmation/network inspection and client secret/provider-call scan; verify server remains key owner. |
| REQ-020 | Goal/theme tests for validation, four named themes, cancel restoration, and persistence after relaunch. |
| REQ-021 | Health Connect runtime permission test and actual local-day step record/source verification on a compatible device. |
| REQ-022 | Health Connect denied/revoked, unavailable/update, empty, query-error, recovery, disclosure, and no-upload evidence. |
| REQ-023 | BLE tests for explicit connect/disconnect and unsupported, off, permission, incompatible, connection failure, and loss states. |
| REQ-024 | Workout tests for explicit start/stop and actual received HR notifications only; assert no fabricated calories/exercise data. |
| REQ-025 | Stale/lost/background/foreground/service interruption and recovery tests showing timestamped gaps without false continuity. |
| REQ-026 | Disclosure and network-boundary evidence that BLE/workout/cycle/diary records remain local and untransmitted. |

Every REQ must have at least one named automated test or a recorded platform/device check. A source scan or mock test alone cannot count as real Health Connect/BLE integration evidence.

## External Dependency and Test Data Strategy

| Boundary | Test implementation | Production/test isolation rule |
|---|---|---|
| Room/DataStore | Real Room in-memory database and isolated DataStore test storage; production schema/migrations. | No Docker; no shared test profile. Keep persistence choices aligned with t6. |
| Health Connect | Fake adapter for state mapping; real provider with isolated synthetic test records for platform integration. | App requests only the required read permission; no production `WRITE_STEPS`; clean up synthetic records. |
| BLE/GATT | Fake transport/state stream for automated state tests; compatible physical band for real notification test. | Only received GATT notification values count as real data; redact band identifiers from reports. |
| Nutria API client | Local HTTP stub/mock server with captured request assertions and deterministic responses/errors. | No real OpenAI/Open Food Facts call in automated tests; no key in client or fixture. |
| Existing ASP.NET endpoints | In-process host with stub implementations of existing service interfaces, if achievable without production changes. | Keep routes/DTOs frozen; do not make test-only production behavior or access external provider services. |
| Clock/randomness | Inject fixed local clock/time zone and deterministic selection source. | No dependence on host wall-clock date or nondeterministic recipe fixtures. |
| Test identities/data | Synthetic values and unique identifiers; no real user, cycle, or health records. | Fresh profile per test, teardown/DB close, except explicit relaunch-persistence tests. No logs/screenshots with personal data. |

## Environment Requirements and Observed Availability

| Capability | Required for planned validation | Observed on this host |
|---|---|---|
| Android build/JVM tests | Compatible JDK (t4's baseline is JDK 17), checked-in Gradle wrapper, Android SDK Platform/build tools for configured API 36 target. | Java 25.0.5 only; no `gradle` command/wrapper, no Android SDK, no `sdkmanager`, and no `ANDROID_HOME`/`ANDROID_SDK_ROOT`; common SDK directories not found. **Unavailable.** |
| Emulator/instrumentation | Android SDK/ADB and emulator image; API 28 minimum plus API 33/34/36 behavior coverage where supported. | No `adb` or `emulator`; no SDK. **Unavailable.** |
| Health Connect device validation | Health Connect provider on Android 9–13 / Android 14+ system provider, granted test data, runtime permission UI. | No Android device/emulator/provider available. **Unavailable.** |
| BLE validation | Android BLE-capable physical device and compatible standard Heart Rate GATT band. | No Android device or band evidence provided. **Unavailable.** |
| Android UI tests | AndroidX/Compose test dependencies, emulator/device. | No Android project or test dependencies yet. **Unavailable.** |
| Docker | Not required for local Room/DataStore or mocked HTTP tests; no Docker-based database is in the target design. | `docker` not found; no container tier is selected. |
| Node/Playwright | Not applicable: native Compose target has no browser UI. | `node`, `npm`, `npx` not found; this does not block Android-native test design. |
| Existing backend runtime tests | .NET 10 SDK/runtime if new endpoint tests are added/run for the existing `net10.0` server. | .NET SDK/runtime 11.0.100/11.0 found; .NET 10 runtime absent (also 3.1.32 present). **Unavailable without environment preparation.** |
| External network | Not required for deterministic app/API tests because boundaries are stubbed. | Not evaluated or needed for this strategy. |

The Android target from t4 is minSdk 28 / compileSdk 36 / targetSdk 36. Prepare the compatible JDK, SDK platform/build tools, wrapper and emulator/device before execution; do not assume Java 25 is a valid Android Gradle runtime. Health Connect and BLE device evidence is a separate prerequisite from a successful compile.

## Commands and Execution Rules

Once `android/` and its checked-in wrapper exist, run from that directory:

1. Full app build gate: `.\gradlew.bat clean build` (all configured app modules; no narrowed build).
2. Canonical host-side test command: `.\gradlew.bat testDebugUnitTest`.
3. Required emulator instrumentation: `.\gradlew.bat connectedDebugAndroidTest` with an explicitly recorded emulator/API level.
4. Required real-device checks: use the same instrumented/manual flow on compatible Health Connect/BLE devices; record OS/API, provider state, band standard service/characteristic verification, test cases and actual results.

If a test-only backend contract project is added without modifying production behavior, run `dotnet test tests/Nutria.Api.Tests/Nutria.Api.Tests.csproj` under .NET 10. Report it as a separate backend boundary tier. Every implementation task must run relevant module-level tests and record pass/fail/skip counts; an unrun or failing tier is not deferred as if it passed.

For each command, record its exact text, working directory, exit code, passed/failed/skipped counts, runtime/SDK versions, device/emulator API, and generated report path. Exit code other than zero is FAIL regardless of partial test counts. Startup failure is fail-fast for that runtime tier. Test code defects may be fixed and rerun; production defects go to the owning role, not the tester.

## Per-Capability Fallback Matrix and Known Gaps

| Capability axis | Primary | Fallback/behavior if unavailable | Mandatory known gap |
|---|---|---|---|
| Local persistence/infrastructure | Room/DataStore integration tests using the real local implementation and in-memory/isolated storage. | No Docker fallback needed; use the same SQLite-backed Room implementation, not a substitute remote DB. | No containerized infrastructure fidelity is lost because none is used by this architecture. |
| Native UI | Compose instrumentation on Android emulator/device. | Host-side unit/repository tests can continue while SDK/device is missing; they do not replace UI/runtime testing. | Build/UI behavior remains UNVERIFIED until Android tools and emulator/device are available. |
| Health Connect | Real provider permission + real step records on Android devices. | Fake adapter verifies app state mapping only. | Fake results do not prove permission integration, provider behavior, local-day aggregation, or actual records. |
| BLE | Physical compatible band and Android device. | Fake GATT adapter verifies parser/state transitions only. | No proof of hardware compatibility, notification delivery, background behavior, or real HR readings. |
| HTTP dependencies | Local stub/mock server; in-process backend host with fake services where possible. | Mocked contract tests remain valid for client request behavior. | Does not prove external OpenAI/Open Food Facts availability; real provider calls are intentionally not part of routine tests. |
| Backend runtime | .NET 10 test host/runtime for server-side endpoint integration if created. | Native client consumer-contract tests can run against a mock while backend runtime is absent. | Server route runtime remains UNVERIFIED; source inspection/mock-client success cannot be labeled server integration pass. |
| Browser tooling | Not applicable to the native app; no Playwright is selected. | No browser fallback is permitted as proof of native parity. | The retained PWA is reference-only and cannot stand in for native Compose evidence. |

Unavailable Android tooling blocks all Android build and runtime claims; it does not justify replacing the app with a WebView or declaring a partial test pass. Record each unavailable axis separately and notify the coordinator if execution reaches a blocked gate.

## Acceptance and Final Conformance Review

Full parity PASS requires all of the following:

- The full Android build exits `0`; unit, persistence, HTTP-contract, and Compose instrumentation suites have no unexplained failures and report explicit counts.
- All REQ-001–REQ-026 rows above have evidence. PWA behavior remains intact until parity/data-transfer review is complete.
- Health Connect and BLE platform evidence includes real permission/device/source behavior; fake-only coverage is labeled as such.
- Local/private storage and explicit API data flows match the approved constitution and t6 design. No OpenAI key or direct provider call exists in the client; photo upload requires explicit action; Health Connect/BLE/personal records are not transmitted.
- Toolchain, command, exit-code, actual device/API level, permission state, and gaps are recorded; no missing artifact or unverified requirement is treated as PASS.

The final reviewer must compare the executed tests to this strategy per capability axis, verify each critical journey and all REQ mappings, check that fallbacks have a reproducible blocker for that specific axis, and report gaps separately. Any missing required Android/device evidence or any HIGH/CRITICAL finding is FAIL; there is no conditional pass.

## Evidence Mapping

- `t1-pm-spec.md#REQ-001–REQ-020` → critical journeys 1–6 and coverage table for existing PWA parity.
- `t1-pm-spec.md#REQ-021–REQ-026` → critical journeys 7–8, platform validation, and truthful-data/privacy evidence.
- `t1-pm-requirements-checklist.md#Inventory coverage` → full feature and state matrix; no existing capability dropped from test scope.
- `t2-architect.md#Summary/Validation and findings` → no current Android project/test setup and known host limitations reflected in environment gates.
- `t4-architect.md#Target structure and boundaries` → unit, Room/DataStore, API-adapter, and Compose test layers.
- `t4-architect.md#Daily steps` and `#BLE heart-rate band and workout lifecycle` → real platform/device requirements, permission/loss/stale/background tests, and fake-vs-real evidence distinction.
- `wire_contracts.yaml#native-client-photo-analysis` and `#native-client-ingredient-search` → local HTTP consumer-contract tests and preserved request/response/error behavior.
- `constitution.md#Core Principles I–V` → parity, server-side secret, private local data, truthful sensor data, and binary evidence gate.
- `tech-stack.md#Runtime configuration and limits` → observed ASP.NET/.NET and Android toolchain blockers.
- `data-model.md#Persistence boundary` → local-only test data, no presumed browser import, and PWA retention.

## Verification Performed for This Strategy

- Environment probe: `docker`, `node`, `npm`, `npx`, `gradle`, `adb`, `sdkmanager`, and `emulator` not found; Java 25.0.5 and .NET SDK/runtime 11.0 found; .NET 10 runtime not found; Android SDK environment variables and common SDK paths absent.
- Legacy test probe: `dotnet test .\Nutria.csproj --no-restore --verbosity minimal` returned `0` without test output; no test project or test suite exists, so this is not test coverage.
- Strategy completeness: all 26 requirements are mapped; this artifact makes no Android build, app-runtime, Health Connect, BLE, or backend-runtime pass claim.
- Tests/build: not applicable to this documentation-only task; product execution is blocked by the missing Android project/toolchain and compatible test devices.
