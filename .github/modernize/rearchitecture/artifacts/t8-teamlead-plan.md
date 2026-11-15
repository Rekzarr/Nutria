# Nutria Native Android Implementation Plan

**Date**: 2026-11-14 | **Spec**: `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md`

## Summary

Deliver Nutria as a real native Kotlin/Jetpack Compose Android app in a new `android/` root while preserving the existing ASP.NET Core API and PWA source. The plan covers all 26 functional requirements: full Spanish PWA parity (REQ-001–REQ-020), Health Connect step reads (REQ-021–REQ-022), and an explicit BLE heart-rate/workout flow (REQ-023–REQ-026). User records remain local/private; the existing photo and ingredient-search contracts stay frozen; no OpenAI key or fabricated fitness metrics may enter the client.

Work begins with an Android toolchain readiness check, then a secure local data foundation and API adapters, followed by independently verifiable vertical feature slices; a blocked readiness check does not block source implementation. Full completion additionally requires Compose journeys, Android platform/device evidence for Health Connect and a physical BLE band, and a final parity/privacy review. If the current host remains without a compatible JDK/Android SDK/Gradle/device, implementation can still be developed and host-side tests authored, but native build/runtime and device gates remain **BLOCKED/UNVERIFIED**; this is not a parity pass.

## Technical Context

**Language/Version**: Kotlin; native Android application; exact Kotlin and Android Gradle Plugin pins to be selected during scaffold, compatible with the architect's JDK 17 build baseline.
**Primary Dependencies**: Jetpack Compose; one installable Android app module; AndroidX Health Connect client `1.1.0` baseline; Room + SQLCipher; Android Keystore; DataStore; Android BLE GATT APIs; typed HTTP client.
**Storage**: Encrypted, versioned Room database for personal records; Keystore-wrapped random per-install key; DataStore only for non-health preferences; no automatic backup, sync, or browser `nutria.v1` import. Health Connect step aggregates remain transient; workout persistence contains observed events and actual received HR samples only.
**Testing**: Kotlin/domain unit tests; Room/DataStore integration and migration tests; local HTTP consumer-contract tests; Compose instrumentation; Health Connect provider tests and physical BLE band validation. Full build command: `.\gradlew.bat clean build` from `android/`.
**Target Platform**: `minSdk 28`, `compileSdk 36`, `targetSdk 36`; JDK 17 build baseline. Pin compatible tooling and verify current release target requirements before publishing.
**Performance Goals**: No numeric latency target was specified. Keep domain calculations local, use lifecycle-aware UI state, and expose loading/error state for HTTP and platform operations. Do not present stale HR readings as live.
**Constraints**: Native UI only (no WebView completion); retain the PWA and backend; preserve Spanish labels, formats, estimate cautions, and observable behavior; keep the two API contracts and OpenAI secret boundary; explicitly disclose external data flows; no invented Health Connect/BLE data; no Android build/runtime claim without actual toolchain/device evidence.

## Constitution Check

| Principle | Result | Plan alignment |
|---|---|---|
| I. Native Android Delivery and Complete Parity | PASS | Adds `android/` Compose client and covers REQ-001–REQ-026. PWA source remains intact; parity is a final gate, not presumed. |
| II. Frozen Backend Contracts and Server-Side Secrets | PASS | Uses the existing photo-analysis and ingredient-search routes; HTTP consumer tests cover request/response/error behavior and client secret boundary. No unrelated server changes are planned. |
| III. Private Local Records and Disclosed Data Flows | PASS | Uses local encrypted Room + DataStore boundaries, disables backup, avoids automatic import/sync, and makes Health Connect/BLE/API disclosures part of the flows. |
| IV. Health and Wearable Data Must Be Truthful | PASS | Health Connect reads actual local-day data with distinct empty/error/permission states; BLE shows received samples and visible gaps only. |
| V. Incremental, Requirement-Traceable Quality Gates | PASS | Features are sequenced as vertical slices with REQ mapping, tests, and explicit toolchain/device prerequisites. |

Governance note: the constitution remains version 1.0.0 with a `TODO(RATIFICATION_DATE)` marker in its source artifact. No owner-supplied formal ratification date was present; this plan is dated 2026-11-14 but does not infer a constitution ratification event. The coordinator/project owner should resolve the constitution marker before treating it as formally ratified. The t3 template/tooling limitation for the unavailable task-breakdown template remains documented in the constitution's Sync Impact Report.

## Applied Guidelines

- **Guideline lookup**: `skills/guidelines/` was checked. The only available domain guideline is Struts-to-Spring, which does not apply to a Kotlin/Compose rewrite. No transformation guideline was applied.
- **Architecture source**: `.github/modernize/rearchitecture/artifacts/t4-architect.md` governs package boundaries, min/compile/target SDK, Health Connect/BLE behavior, and frozen API seams.
- **Data source**: `.github/modernize/rearchitecture/artifacts/t6-dba.md` governs encrypted persistence, key handling, schema, backup exclusion, and local-only data boundaries.
- **Validation source**: `.github/modernize/rearchitecture/artifacts/t7-teamlead.md` governs required test layers, device evidence, and exact build/test commands.

## Implementation Steps

### Phase 1 — Setup

#### Step 1.1: Inspect Android build-environment readiness
- **Requirements**: REQ-001 (native application delivery and executable target)
- **Design inputs**: t4 Platform and build target; t7 Environment Requirements and Observed Availability; constitution Principle V.
- **Description**: Inspect and record command-resolution evidence for the architect's JDK 17 build baseline, Android SDK Platform/build tools 36, Gradle, ADB, and emulator. A Gradle wrapper does not exist until the app is scaffolded in Step 1.2, so do not require or attempt a project build here. Current evidence is Java 25.0.5 only, without Gradle, Android SDK, SDK environment variables, ADB, or emulator. If readiness is blocked, record exact evidence and keep build/runtime gates blocked; this finding does not block scaffold or source tasks.

#### Step 1.2: Create the native app structure and Spanish navigation shell
- **Requirements**: REQ-001
- **Design inputs**: t4 Target structure and boundaries; t5 Information architecture and shared interaction rules.
- **Description**: Create a single installable Compose app module under `android/`, define the app namespace/manifest/resources and test source sets, and add the pinned Gradle wrapper/build configuration. Attempt the first meaningful full build only after the wrapper exists; record exact toolchain blockers without gating source implementation. Implement the single-activity navigation shell with labeled destinations `Inicio`, `Comida`, `Recetas`, `Foto`, `Ciclo`, `Plan`, `Actividad`, plus `Ajustes` from the app bar; preserve narrow-screen drawer behavior, Spanish locale formatting, back-stack rules, TalkBack names, and accessible touch targets.

### Phase 2 — Foundational capabilities

#### Step 2.1: Implement the private persistence and recovery boundary
- **Requirements**: REQ-002–REQ-007, REQ-009, REQ-011, REQ-013–REQ-015, REQ-020, REQ-024–REQ-026
- **Design inputs**: t6 Proposed local model, transaction boundaries, encryption/key lifecycle, backup/retention, schema migration; t4 Local privacy boundary.
- **Description**: Establish only the shared secure-storage foundation: Room/approved SQLCipher database initialization, a random per-install database key wrapped by Android Keystore, a DataStore boundary for non-health preferences, migration infrastructure, `allowBackup=false`, legacy backup exclusions, and API 31+ data-extraction exclusions. Fail closed on database/key errors; do not silently reset, import `nutria.v1`, or store sensitive records in DataStore. Keep feature schemas, DAOs, feature migrations, and transaction invariants inside their owning vertical tasks (T007–T009, T012–T014, T016), with feature-specific persistence tests there; this keeps the security foundation independently executable without weakening encryption or privacy requirements.

#### Step 2.2: Implement shared domain rules and repository contracts
- **Requirements**: REQ-002–REQ-007, REQ-009–REQ-016, REQ-020–REQ-025
- **Design inputs**: t4 Dependency direction and feature ownership; t6 Proposed local model; t1 acceptance scenarios.
- **Description**: Add independently testable domain models/use cases and repository interfaces for local-date handling, goals, nutrient conversion/rounding, streaks, recipe totals, recommendation filters, calendar generation, cycle validation/estimates, Health Connect result states, and BLE reading freshness/session gaps. Inject `Clock`/`ZoneId` and deterministic selection. Preserve exact ounce factor, date/overlap rules, no-data-versus-zero distinction, and no fabricated metrics.

#### Step 2.3: Implement typed Nutria API client and contract test harness
- **Requirements**: REQ-008, REQ-017–REQ-019
- **Design inputs**: t4 Nutria API — frozen server seam; t1 Existing API Contracts; `wire_contracts.yaml`.
- **Description**: Add the typed Android HTTP client and test harness for only `GET /api/ingredients/search?query=...` and `POST /api/analyze-meal`. Enforce trimming/limits, UTF-8 query encoding, multipart part names and supported media/size, DTO validation, finite/nonnegative response values, timeout/error mapping, and no automatic retry of photo POST. Use a configured service base URL without credentials; assert no OpenAI key/provider call or personal-record upload. Do not modify the existing production API.

### Phase 3 — Priority 1 user stories

#### Step 3.1: Implement food diary, units, history, water and undo
- **Requirements**: REQ-003–REQ-005
- **Design inputs**: t1 US2 and diary requirements; t4 `feature/diary`; t5 Comida flow; t6 meal and water tables.
- **Description**: Deliver the vertical `Comida` flow for manual entry, validation, live amount-based nutrient preview, grams/ounces/portions, today-only add/remove, latest 30-day grouped read-only history, +150/+250 ml, 250-ml glass marks, and last-event-only undo. Persist via the local repository, preserve values after failures, and show a durable Spanish persistence error rather than false success. Add unit, repository, and Compose tests.

#### Step 3.2: Implement Inicio summary, streak and clear-today isolation
- **Requirements**: REQ-001, REQ-002, REQ-006
- **Design inputs**: t1 US1; t4 `feature/home`; t5 Inicio flow; t6 clear-today transaction.
- **Description**: Deliver the Spanish daily overview using persisted diary/goals/water data: totals, remaining calories floored at zero, macros, progress capped visually at 100%, water, meal count/list, empty state, and consecutive-day streak. Add confirmed clear-today behavior that deletes only today's meals and water while retaining recipes, plans, cycle data, goals, and theme. Include state/persistence/UI tests.

#### Step 3.3: Implement recipe authoring, lookup and diary integration
- **Requirements**: REQ-007–REQ-009
- **Design inputs**: t1 US3–US4; t4 `feature/recipes` and frozen ingredient seam; t5 Recetas flow; t6 recipe/ingredient and calendar FK rules.
- **Description**: Deliver saved recipe CRUD, ingredient validation and nutrition totals/per-serving calculation, editable Open Food Facts results through the existing lookup route with manual-entry fallback, meal/dinner category changes, one-serving diary integration, and confirmed deletion that transactionally removes calendar assignments. Keep diary entries as nutrition snapshots. Add domain/Room/HTTP/Compose tests, including no-result and error branches.

#### Step 3.4: Implement Health Connect daily steps
- **Requirements**: REQ-021–REQ-022
- **Design inputs**: t1 US10; t4 Daily steps; t5 Actividad/Steps; t6 Health Connect boundary.
- **Description**: Implement explicit user-triggered `READ_STEPS` permission flow, provider status/update handling, permission recheck, actual `StepsRecord.COUNT_TOTAL` aggregation over the user's local day, refresh timestamp, and foreground refresh. Distinguish provider unavailable/update-required, denied/revoked, no records, read failure, and actual result; never convert absent data to zero or persist/upload step totals. Add fake-adapter/domain tests and a device test plan/evidence checklist.

#### Step 3.5: Implement BLE band connection and explicit workouts
- **Requirements**: REQ-023–REQ-026
- **Design inputs**: t1 US11; t4 BLE heart-rate band and workout lifecycle; t5 Actividad/Band and workout; t6 workout/session/sample model and disclosure.
- **Description**: Implement explicit scan/select/connect/disconnect for a compatible standard Heart Rate GATT Service (`0x180D`) and Measurement characteristic (`0x2A37`), Android-version Bluetooth permission paths, Bluetooth/hardware checks, and measurement parsing. Separate connected from workout-active state; start only by user action, collect actual timestamped notifications, mark stale after the designed threshold, persist interruptions/gaps, and provide foreground-service notification/stop and lifecycle recovery. Do not persist band address or invent HR/calories/exercise minutes. Add parser/state/persistence tests and Spanish disclosures.

### Phase 4 — Priority 2 user stories

#### Step 4.1: Implement recommendations
- **Requirements**: REQ-010
- **Design inputs**: t1 US5 and 11-item catalog; t4 `feature/recipes`; t5 Recetas/Ideas.
- **Description**: Port the 11 local recommendations and implement category, time, and complexity filters, up to three unsaved suggestions, refresh, detail/instructions, save-to-recipe behavior, no-match state, <=550 kcal eligibility, and adjacent estimate language. Add deterministic unit and Compose tests; do not call a new server route.

#### Step 4.2: Implement meal calendar and menu generation
- **Requirements**: REQ-011–REQ-012
- **Design inputs**: t1 US6; t4 `feature/calendar`; t5 Plan; t6 calendar assignments and recipe deletion cascade.
- **Description**: Deliver a Monday-first monthly view with previous/next navigation, selected-day editing, manual assignment/removal for meal/dinner and optional breakfast, and persistent assignments. Generate a complete selected week or month using category rules, avoid same-day repeats where alternatives exist, explain missing categories without partial writes, and confirm replacement. Add calendar/domain/transaction/Compose tests.

#### Step 4.3: Implement cycle history, estimates and private journal
- **Requirements**: REQ-013–REQ-016
- **Design inputs**: t1 US7; t4 `feature/cycle`; t5 Ciclo; t6 encrypted cycle records and privacy controls.
- **Description**: Deliver period create/correct/finalize/delete with confirmation, overlap/date validation, configurable averages and local-date estimates, unknown/late states, seven-day flow/pain/mood/note journal, and Spanish on-device privacy text. Retain all estimate/non-diagnosis/fertility/pregnancy/professional-advice caveats; do not expose notes in notifications or network traffic. Add fixed-clock/unit, Room, and Compose tests.

#### Step 4.4: Implement explicit photo analysis
- **Requirements**: REQ-017–REQ-019
- **Design inputs**: t1 US8 and frozen photo contract; t4 Nutria API photo mapping; t5 Foto; t6 remote-data disclosure.
- **Description**: Deliver image selection, preview/remove, JPG/PNG/WebP and 10 MiB checks, 60-character dish-name limit, explicit consent/submit, busy/error/retry-safe UI, and insertion of a clearly approximate meal only after a fully valid response. Call only the existing `POST /api/analyze-meal`; preserve server-owned OpenAI credential and never cache/upload before user confirmation. Add mock HTTP/Compose tests for all documented response and error cases, including no diary write on failure.

#### Step 4.5: Implement goals, themes and privacy/settings disclosures
- **Requirements**: REQ-020, with disclosure support for REQ-022 and REQ-026
- **Design inputs**: t1 US9; t4 `feature/settings`; t5 Ajustes; t6 DataStore/local-data disclosure.
- **Description**: Deliver positive goal editing (water >=50 ml in 50-ml increments), the four named themes, saved selection across restart, and preview-cancel restoration. Provide Spanish privacy/data-flow explanations for local records, Health Connect, BLE, photo analysis, ingredient search, lack of account/sync, no automatic PWA import, backup/uninstall implications, and configurable service URL without an API-key field. Add preference/UI tests.

### Phase 5 — Integration and release gates

#### Step 5.1: Run complete parity and contract test journeys
- **Requirements**: REQ-001–REQ-026
- **Design inputs**: t7 Primary Validation Stack, Critical User Journeys, Requirement-to-Evidence Coverage, and Acceptance/Final Conformance Review.
- **Description**: Run all host-side Kotlin/domain, Room/DataStore, API consumer-contract, and Compose UI tests with isolated profiles and synthetic data. Execute full `.\gradlew.bat clean build`, `.\gradlew.bat testDebugUnitTest`, and `.\gradlew.bat connectedDebugAndroidTest` from `android/` where prerequisites exist. Capture exact command, working directory, versions, exit code, and pass/fail/skip counts. Never substitute WebView/Playwright or claim an unexecuted tier.

#### Step 5.2: Validate Health Connect and BLE on compatible devices
- **Requirements**: REQ-021–REQ-026
- **Design inputs**: t7 Android platform/device validation; t4 Health Connect and BLE gates; t6 risk gates.
- **Description**: On suitable Android devices, verify Health Connect provider, permission grant/denial/revocation, actual local-day aggregate, empty/error/refresh behavior, and disclosures. On a BLE-capable device with a physical compatible band, verify the standard service/characteristic, actual 8-/16-bit notifications, connect/workout controls, Bluetooth-off/permission/loss/stale/background/foreground interruptions, persistence and visible gaps. Label fake tests as fake; do not substitute them for device proof.

#### Step 5.3: Perform final parity, privacy and legacy-retention review
- **Requirements**: REQ-001–REQ-026
- **Design inputs**: t1 full requirement inventory; constitution Principles I–V; t7 final conformance criteria; migration boundary.
- **Description**: Compare evidence against all REQ rows and critical journeys; verify the PWA/API files remain intact, no unapproved backend changes occurred, no automatic browser-data import is claimed, no OpenAI secret/provider call or health/wearable upload exists in the app, and all Spanish/error/privacy states are represented. Binary verdict only: PASS if every required evidence gate passes, otherwise FAIL with the exact uncovered requirements, test/device blockers, and responsible follow-up owner.

## Task Breakdown

Tasks are vertical feature deliverables and run in the stated dependency order. A task marked `[P]` may run concurrently with other ready tasks after its listed prerequisites. Toolchain readiness is evidence, not a prerequisite for source implementation: a blocked T001 does not block T002 or later source work. Record blockers and leave Android build/runtime/device gates blocked until their exact evidence exists. Feature-specific entities, DAOs, migrations, and transaction tests belong to their owning vertical task rather than the shared security foundation. There is no topology file, so no G-group labels are used.

### Phase 1 — Setup

- [ ] T001 [Plan:1.1] Inspect and record JDK 17, Android SDK Platform/build tools 36, Gradle, ADB, and emulator availability/resolution in `.github/modernize/rearchitecture/artifacts/checkpoints/t8-environment.md`; if unavailable, record exact blockers and activation requirements. Do not require a Gradle wrapper or attempt a project build before `android/` exists.
- [ ] T002 [Plan:1.2] Create the single-module native Compose app, pinned Gradle wrapper/build configuration, manifest, app-private resources, and test source sets under `android/`; attempt the first full build only after the wrapper exists and record any exact toolchain blocker. Depends on: T001 (inspection completion only; a BLOCKED readiness result does not prevent scaffolding or source work).
- [ ] T003 [Plan:1.2] Implement Spanish single-activity navigation, the labeled seven-destination drawer and Ajustes app-bar entry, responsive behavior, accessibility labels, and back-stack semantics in `android/app/src/main/java/<application-namespace>/app/` and `android/app/src/main/res/`; add navigation/locale Compose tests. [Source: wwwroot/index.html, wwwroot/app.js, wwwroot/styles.css] Depends on: T002.

### Phase 2 — Foundational capabilities

- [ ] T004 [Plan:2.1] Implement only the shared encrypted Room/SQLCipher database provider, Keystore-wrapped per-install key lifecycle, DataStore access boundary, migration framework, backup exclusions and fail-closed recovery in `android/app/src/main/java/<application-namespace>/data/local/` and `AndroidManifest.xml`; add focused database-open/key/backup/migration-bootstrap tests, not feature schemas. [Source: wwwroot/app.js] Depends on: T002.
- [ ] T005 [Plan:2.2] Implement shared domain models/use cases/repository interfaces for nutrition, local dates, recipe/calendar/cycle rules, Health Connect result states and BLE freshness/gaps under `android/app/src/main/java/<application-namespace>/core/`; add fixed-clock, time-zone and deterministic unit tests. [Source: wwwroot/app.js] Depends on: T004.
- [ ] T006 [Plan:2.3] Implement typed Nutria API repositories and local HTTP contract tests for the unchanged photo and ingredient routes under `android/app/src/main/java/<application-namespace>/data/remote/` and `android/app/src/test/`; assert method/path/multipart/query/error mappings, no pre-consent photo request, no secret/provider call, and no personal-record upload. [Source: Endpoints/MealAnalysisEndpoints.cs, Endpoints/OpenFoodFactsEndpoints.cs, Services/IMealAnalysisService.cs, Services/IOpenFoodFactsService.cs] Depends on: T002.

### Phase 3 — Priority 1 user stories

#### US1/US2 — Daily nutrition, hydration and food entry

- [ ] T007 [US1] [US2] [Plan:3.1,2.1] Implement food entry/conversions, nutrient preview, today diary add/remove, read-only 30-day history, water +150/+250 ml and last-event undo in `android/app/src/main/java/<application-namespace>/feature/diary/`; own the meal/water entities, DAOs and feature migrations; add domain, Room and Compose tests for unit conversion, validation, history and undo. [Source: wwwroot/app.js, wwwroot/index.html, wwwroot/styles.css] Depends on: T003, T004, T005.
- [ ] T008 [US1] [Plan:3.2,2.1] Implement Inicio totals/macros/water/streak/empty state and confirmed clear-today isolation in `android/app/src/main/java/<application-namespace>/feature/home/`; implement and test the repository transaction that clears only today's meals and water while retaining all other records, plus summary/relaunch Compose tests. [Source: wwwroot/app.js, wwwroot/index.html, wwwroot/styles.css] Depends on: T007.

#### US3/US4 — Recipe library and ingredient lookup

- [ ] T009 [US3] [US4] [Plan:3.3,2.1] Implement recipe authoring/calculation, saved recipe detail/category/edit/delete, ingredient lookup/import/manual fallback, and diary snapshot integration in `android/app/src/main/java/<application-namespace>/feature/recipes/`; own recipe/ingredient entities, DAOs and feature migrations; test recipe calculation and contract mapping. [Source: wwwroot/app.js, wwwroot/index.html, wwwroot/styles.css, Endpoints/OpenFoodFactsEndpoints.cs, Services/OpenFoodFactsService.cs] Depends on: T003, T004, T005, T006.
- [ ] T010 [US5] [Plan:4.1] Port the 11 local recommendation records and implement filter/refresh/detail/save/no-match behavior in `android/app/src/main/java/<application-namespace>/feature/recipes/`; test all filter dimensions, <=550 kcal eligibility, at-most-three suggestions, saved exclusion and estimate labels. [Source: wwwroot/app.js, wwwroot/index.html] Depends on: T009.

#### US10 — Health Connect steps

- [ ] T011 [P] [US10] [Plan:3.4] Implement Health Connect provider/permission state handling, explicit `READ_STEPS` request, local-day aggregate, refresh timestamp, foreground refresh, Spanish disclosure and honest empty/error UI under `android/app/src/main/java/<application-namespace>/feature/activity/`; test fake states and schedule actual-provider device evidence. Depends on: T003, T005.

#### US11 — BLE workout

- [ ] T012 [P] [US11] [Plan:3.5,2.1] Implement explicit BLE scan/select/connect/disconnect and workout start/stop, standard Heart Rate GATT parsing, versioned permissions, foreground service, actual-sample persistence, stale/gap/lifecycle states and Spanish disclosure under `android/app/src/main/java/<application-namespace>/feature/activity/`; own the workout/session/sample entities, DAOs and feature migrations; test parser, fake state machine, local sample/event persistence and notification behavior. Depends on: T003, T004, T005.

### Phase 4 — Priority 2 user stories

#### US6 — Meal calendar

- [ ] T013 [US6] [Plan:2.1,3.3,4.2] Implement Monday-first month navigation, optional breakfast, manual recipe assignment/removal and complete week/month menu generation in `android/app/src/main/java/<application-namespace>/feature/calendar/`; own the calendar-assignment entity/DAO/migration and test category constraints, duplicate avoidance, missing-category abort, overwrite confirmation, persistence, and transactional recipe-deletion cleanup. [Source: wwwroot/app.js, wwwroot/index.html, wwwroot/styles.css] Depends on: T009.

#### US7 — Cycle and private journal

- [ ] T014 [P] [US7] [Plan:4.3,2.1] Implement period history/correction/finalization/delete, cycle estimates/settings and local daily journal in `android/app/src/main/java/<application-namespace>/feature/cycle/`; own the encrypted period/journal entities, DAOs and feature migrations; test valid/invalid/overlapping dates, boundaries, fixed-clock estimates, privacy and Spanish safety language. [Source: wwwroot/app.js, wwwroot/index.html, wwwroot/styles.css] Depends on: T003, T004, T005.

#### US8 — Photo meal analysis

- [ ] T015 [P] [US8] [Plan:4.4] Implement photo selection/preview/removal, constraints, explicit Spanish consent, user-triggered multipart analysis, validated approximate diary result and actionable no-write failures in `android/app/src/main/java/<application-namespace>/feature/photo/`; test all API and Compose paths. [Source: wwwroot/app.js, wwwroot/index.html, Endpoints/MealAnalysisEndpoints.cs, Services/IMealAnalysisService.cs] Depends on: T003, T004, T005, T006.

#### US9 — Settings and disclosures

- [ ] T016 [P] [US9] [Plan:4.5,2.1] Implement goals, four themes with cancel restoration, settings-owned DataStore values and complete Spanish local/API/Health Connect/BLE/privacy disclosures in `android/app/src/main/java/<application-namespace>/feature/settings/`; test validation, save/cancel/restart and network-boundary text. [Source: wwwroot/app.js, wwwroot/index.html, wwwroot/styles.css] Depends on: T003, T004.

### Phase 5 — Integration and sign-off

- [ ] T017 [Plan:5.1] Run full Android build, all host-side/domain/Room/API contract tests, Compose journeys and full acceptance trace for REQ-001–REQ-026; record exact commands, tool versions and pass/fail/skip counts in `.github/modernize/rearchitecture/artifacts/checkpoints/t8-implementation-tests.md`. Depends on: T007–T016 and completion of T001/T002; if build/test prerequisites are unavailable, record each exact blocked tier and evidence without blocking source implementation or substituting a browser shell.
- [ ] T018 [Plan:5.2] Execute Health Connect permission/provider/real-record checks and physical compatible BLE band/lifecycle checks; record device/API/provider/band capability and actual evidence (redact device identifiers) in `.github/modernize/rearchitecture/artifacts/checkpoints/t8-device-evidence.md`. Depends on: T011, T012, T017 and compatible devices.
- [ ] T019 [Plan:5.3] Perform binary parity/privacy/legacy-retention conformance review for every REQ and test/device gate; publish PASS only with complete evidence, else FAIL with uncovered rows and blockers in `.github/modernize/rearchitecture/artifacts/t8-final-conformance.md`. Depends on: T017, T018.

## Project Structure

```text
android/
  app/
    src/main/
      AndroidManifest.xml
      java/<application-namespace>/
        app/                 # Application, activity, navigation, DI
        core/model/          # Nutrition, dates, validation, common state
        core/data/           # Repository interfaces and mappings
        data/local/          # Room, SQLCipher, DAOs, migrations, DataStore
        data/remote/         # Nutria API client, DTOs, error mapping
        feature/home/
        feature/diary/
        feature/recipes/
        feature/photo/
        feature/calendar/
        feature/cycle/
        feature/activity/
        feature/settings/
      res/                   # Spanish strings, icons, theme resources
    src/test/                # Domain, parser, repository, migration tests
    src/androidTest/         # Compose and platform integration tests
  gradle/                    # Checked-in wrapper and pinned build config
Program.cs, Endpoints/, Services/, wwwroot/   # Preserve unchanged until parity gate
```

There is no project-topology or `*-fanout.md` artifact. The implementation is therefore organized by user-visible feature slices from t1/t4, without invented G-group labels or Gradle subprojects.

## Testing Strategy

- **appType**: Native Android Compose app with local Room/DataStore persistence and two user-triggered HTTP integrations; existing ASP.NET Core backend/PWA remains.
- **Critical user journeys**:
  1. Daily nutrition/water: default goals, food in all units, diary/history/streak, water undo, confirmed clear-today isolation, restart persistence.
  2. Recipes/planning: create and calculate recipe, ingredient search/manual fallback, recommendations, add to diary, calendar assignments and full week/month generation.
  3. Cycle/private notes: valid and invalid period changes, estimate/unknown states and caveats, journal persistence, no network transmission.
  4. Explicit photo flow: select/preview, prove no request before confirmation, submit once, valid estimated entry and no-write error cases.
  5. Activity: actual Health Connect local-day steps and all access/provider states; compatible physical BLE band with real samples and visible loss/stale/background gaps.
- **primaryValidationStack**: Kotlin/JUnit unit tests; real Room in-memory persistence/migration tests and isolated DataStore; local mock HTTP consumer-contract tests; Compose instrumentation; physical Health Connect/BLE validation.
- **Commands** (once `android/` and prerequisites exist): `.\gradlew.bat clean build`; `.\gradlew.bat testDebugUnitTest`; `.\gradlew.bat connectedDebugAndroidTest`, all from `android/`. Optional backend test host is separate and must not alter production endpoint behavior; .NET 10 runtime was unavailable in the recorded environment.
- **fallbackMatrix**:
  - Android build/UI: primary is the full Gradle build and Compose instrumentation on an emulator/device. Host-side Kotlin/domain tests can run only if a compatible JDK and Gradle wrapper are available; they do not prove Android compilation or UI behavior.
  - Health Connect: fake adapter for state mapping, followed by real provider + real records on a compatible Android device. Fakes do not prove permission integration or record aggregation.
  - BLE: fake GATT/state tests, followed by physical standard Heart Rate GATT band validation. Fakes do not prove real notification delivery or lifecycle continuity.
  - HTTP: local stub/mock server for deterministic client contract tests; no production OpenAI/Open Food Facts calls in automated test fixtures.
- **Environment requirements**: JDK 17-compatible build runtime; Android SDK Platform/build tools 36; Gradle wrapper; ADB/emulator for instrumentation; Health Connect provider/device and seeded test records; BLE-capable device and standard Heart Rate GATT band for actual reading evidence. Current host lacks Android SDK, Gradle, ADB, emulator and Android device/band; these are explicit blockers. Java 25.0.5 alone is not the selected build baseline.
- **knownGaps**: Until tools/devices are available, native build, Compose instrumentation, Health Connect provider/permission integration, physical BLE readings/background behavior, and full parity are UNVERIFIED. Existing PWA behavior is a reference, not a substitute Android test. There is no legacy test suite or test project; a `dotnet test` exit code with no tests is not a passing baseline.
- **Test data strategy**: Synthetic values and unique identifiers; fresh app-private profile per test; isolated in-memory Room/DataStore fixtures; preserve database only for explicit restart tests; stub remote APIs; no real user, cycle, health, band identifier, OpenAI key, or external provider calls.
- **Acceptance criteria**: Every REQ-001–REQ-026 has at least one named automated test or actual platform/device check. Full PASS requires a successful full Gradle build and relevant automated suites, all parity journeys, actual Health Connect source behavior, real compatible BLE samples and interruption evidence, local/private data boundaries, preserved backend contract, and PWA retention. Any unavailable required evidence or HIGH/CRITICAL finding is FAIL; no conditional verdict.
- **Validation review expectations**: t9/final reviewer matches executed evidence to the exact t7 matrix, distinguishes fake from real platform results, verifies all requirements and privacy contracts, confirms no unsupported Android claims, and reports blocked axes separately.

## Requirement Mapping

| REQ ID | Description | Plan Items | Implementation Evidence |
|---|---|---|---|
| REQ-001 | Spanish navigation/localization | 1.1, 1.2, 3.2, 5.1, 5.3 | `android/app/src/main/.../app/`, `res/`, navigation and Compose tests |
| REQ-002 | Daily summary and streak | 2.1, 2.2, 3.2 | Diary/water persistence in T007; `feature/home/` summary/streak in T008 |
| REQ-003 | Food calculation and diary | 2.2, 3.1 | `core/model/`, `feature/diary/`, T007 persistence and conversion/domain/UI tests |
| REQ-004 | Diary history and deletion | 2.1, 3.1 | T007 meal entity/DAO/repository and history screens/tests |
| REQ-005 | Water logging and undo | 2.1, 2.2, 3.1 | T007 water-event entity/DAO/use case and undo tests |
| REQ-006 | Clear today only | 2.1, 3.2 | T008 transactional repository operation and isolation tests |
| REQ-007 | Recipe authoring/calculation | 2.1, 2.2, 3.3 | T009 recipe/ingredient entities, domain rules, authoring flow/tests |
| REQ-008 | Ingredient lookup contract | 2.3, 3.3 | Typed `GET /api/ingredients/search` adapter and mock contract tests |
| REQ-009 | Recipe book and diary integration | 2.1, 3.3 | T009 recipe screens/repository and diary snapshot; T013 assignment-deletion transaction tests |
| REQ-010 | Recipe recommendations | 2.2, 4.1 | Local 11-item catalog, filters/save flow and deterministic tests |
| REQ-011 | Meal calendar | 2.1, 4.2 | T013 calendar assignment entity/DAO, Monday-first UI and persistence tests |
| REQ-012 | Random menu generation | 2.2, 4.2 | Week/month generation use case and replacement/category tests |
| REQ-013 | Menstrual period history | 2.1, 4.3 | T014 encrypted period records and validation/UI tests |
| REQ-014 | Cycle estimates/configuration | 2.2, 4.3 | Cycle domain use cases and fixed-clock tests |
| REQ-015 | Private daily cycle journal | 2.1, 4.3 | T014 encrypted journal DAO/UI, seven-day and privacy tests |
| REQ-016 | Cycle safety language | 4.3, 5.1 | Spanish estimate/caveat strings and Compose assertions |
| REQ-017 | Photo-based meal analysis | 2.3, 4.4 | `feature/photo/` picker, consent, valid response flow and tests |
| REQ-018 | Photo endpoint/errors | 2.3, 4.4 | Typed multipart client, error mapping, no-write mock tests |
| REQ-019 | Server-side AI secret boundary | 2.3, 4.4, 5.3 | Client config/secret scan, explicit consent, no direct provider call |
| REQ-020 | Settings and theme | 2.1, 4.5 | T016 settings-owned DataStore preferences, theme/goal UI and restart/cancel tests |
| REQ-021 | Health Connect permission and steps | 3.4, 5.2 | `READ_STEPS` flow, actual local-day aggregate and device evidence |
| REQ-022 | Health Connect states/disclosure | 3.4, 4.5, 5.2 | Provider/permission/no-data/error UI and no-upload evidence |
| REQ-023 | Explicit BLE connection | 3.5, 5.2 | GATT adapter, explicit connect/disconnect and failure-state tests |
| REQ-024 | Explicit workout/real readings | 2.1, 3.5, 5.2 | T012 session/sample persistence and actual notification evidence |
| REQ-025 | BLE lifecycle integrity | 2.2, 3.5, 5.2 | Stale/gap state tests and device background/foreground evidence |
| REQ-026 | BLE disclosure/local privacy | 2.1, 3.5, 4.5, 5.3 | T012 local encrypted records, disclosure UI, network-boundary evidence |

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md` — REQ-001–REQ-026, user stories, API contracts, edge cases, privacy and success criteria.
- `.github/modernize/rearchitecture/artifacts/constitution.md` — migration principles and implementation constraints.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — native architecture, package boundaries, platform targets, API seams, and Health Connect/BLE behavior.
- `.github/modernize/rearchitecture/artifacts/t5-ux.md` — Spanish navigation, accessibility and user flows.
- `.github/modernize/rearchitecture/artifacts/t6-dba.md` — local schema, encryption/key lifecycle, backup, transactions and data disclosure.
- `.github/modernize/rearchitecture/artifacts/t7-teamlead.md` — test strategy, environment limits, commands, platform validation and binary sign-off.
- `.github/modernize/rearchitecture/artifacts/wire_contracts.yaml` — existing API contract reference consulted from architecture artifacts; routes remain unchanged.
- `.github/modernize/rearchitecture/artifacts/project-structure.md` and `architecture_index.md` — current PWA/server boundary and feature unit names.
- `.github/modernize/rearchitecture/board.md`, `team/teamlead/log.md`, `team/teamlead/inbox.md` — project phase state, prior decisions and ratification-date follow-up.
- `.github/modernize/rearchitecture/learnings/teamlead/android-rewrite-guardrails.md` and `native-android-validation-strategy.md` — PWA/API retention and verification constraints.

## Evidence Mapping

- `t1-pm-spec.md#REQ-001–REQ-026` → implementation steps 1.1–5.3 and the complete Requirement Mapping table.
- `constitution.md#Principles-I–V` → Constitution Check and phase gates; PWA/API preservation, privacy, truthful activity data and evidence requirements.
- `t4-architect.md#Target-structure-and-boundaries` → Project Structure and steps 1.2, 2.1–2.3.
- `t4-architect.md#Nutria-API—frozen-server-seam` → step 2.3 and 4.4 contract-preservation tasks.
- `t4-architect.md#Daily-steps` and `#BLE-heart-rate-band-and-workout-lifecycle` → steps 3.4, 3.5 and 5.2.
- `t5-ux.md#Information-architecture` and `#Feature-flows` → navigation, all Spanish feature flows and accessibility requirements in steps 1.2 and 3.1–4.5.
- `t6-dba.md#Proposed-local-model`, `#Encryption-key-lifecycle-and-recovery`, and `#Health-Connect-and-BLE-boundary` → steps 2.1–2.2 and evidence for REQ-021–REQ-026.
- `t7-teamlead.md#Primary-Validation-Stack`, `#Critical-User-Journeys`, and `#Requirement-to-Evidence-Coverage` → Testing Strategy and steps 5.1–5.3.
- `project-structure.md#Migration-boundary-facts` and `migration_boundary.yaml#implementation_rule` → retain ASP.NET/PWA source; add a separate `android/` target.

## Test Results

- Command: PowerShell structural and traceability assertions over the plan and checkpoint files.
- Passed: 18 assertions across three final validation runs, including exact REQ-001–REQ-026 coverage, 18 plan items, 19 sequential tasks, valid plan references, checkpoint/task coverage, and artifact links.
- Failed: 0 after correction; an initial assertion exposed an off-by-one plan-item count, corrected from 17 to 18 before final validation.
- Skipped: YAML parser validation because Python/PyYAML is unavailable; Android build, app tests and device tests are not applicable to this planning task and remain blocked by missing Android tooling/devices.

## Open Items

- Formal constitution ratification date remains unprovided; request the coordinator/project owner to resolve the existing marker without inferring approval.
- Native build/runtime and actual Health Connect/BLE evidence remain blocked until the compatible toolchain and devices listed above are available.
- No `context.md`, `decisions.md`, project-topology file, or `*-fanout.md` was present; available constitution and design artifacts were authoritative.
