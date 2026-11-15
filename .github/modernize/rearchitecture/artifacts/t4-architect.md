# t4 — Native Android target architecture and preserved API contracts

## Decision

Build the delivered client as a **native Android application in Kotlin with Jetpack Compose** under a new `android/` root. Use one installable Android app module with feature-oriented packages and explicit UI, domain, and data boundaries; do not use a WebView, reuse the PWA as the delivered interface, or change the ASP.NET Core host to accommodate the client.

The app is the local source of truth for personal Nutria records. It calls only the two existing Nutria APIs for user-initiated meal analysis and ingredient lookup. Health Connect and the BLE band are separate Android platform data sources, not backend integrations. Keep the existing PWA and backend source intact until parity and existing browser-data implications have been reviewed.

## Target structure and boundaries

```text
android/
  app/
    src/main/
      AndroidManifest.xml
      java/<application-namespace>/
        app/                 Application, single activity, navigation, DI
        core/model/          Shared value types, local-date and nutrition rules
        core/data/            Repository interfaces and common mappings
        data/local/           Room database, DAOs, migrations, DataStore
        data/remote/          Nutria API client, DTOs, error mapping
        feature/home/         Daily summary, streak, steps entry point
        feature/diary/        Food entry, meals, history, water and undo
        feature/recipes/      Recipe authoring, lookup, recommendations
        feature/photo/        Explicit image selection, preview, analysis
        feature/calendar/     Month/week plan and assignment flows
        feature/cycle/        Period records, estimates, private journal
        feature/activity/     Health Connect steps, BLE device, workouts
        feature/settings/     Goals, theme and privacy/data disclosure
      res/                    Spanish strings, icons, theme resources
    src/test/                 Pure Kotlin/domain and repository tests
    src/androidTest/          Device/framework integration tests
  gradle/                     Wrapper and pinned build configuration
```

These are package boundaries, not a commitment to many Gradle subprojects. Keep the initial build as one app module: the existing product is small, and a multi-module build would add dependency/version/build complexity without an existing independently shipped component. Split a package into a Gradle module only when build/test ownership or reuse justifies it.

### Dependency direction

```text
Compose screens → screen ViewModels → domain use cases/repository interfaces
                                          ↑
                 Room/DataStore, Nutria API, Health Connect, BLE adapters
```

- **UI:** single-activity Compose UI; Spanish resources; screen-level ViewModels expose immutable `StateFlow` UI state and accept user actions. Collect UI state lifecycle-aware. UI never calls Room, HTTP, Health Connect, or Bluetooth APIs directly.
- **Domain:** explicit models/use cases for diary calculations, recipes, calendar generation, cycle estimates, and activity states. Keep nutrition, serving/unit conversion, calendar assignment, validation, and estimated-cycle logic independently testable. Use local calendar dates for day-keyed records; do not silently shift an entry at UTC midnight.
- **Data:** repository interfaces separate the domain from Room/DataStore, the two Nutria endpoints, Health Connect, and BLE. Room is the record store; DataStore is suitable for small preferences such as goals, theme, and server base URL. Use database transactions where one operation updates related records (e.g. deleting a recipe and its calendar assignments).
- **Feature packages:** preserve all t1 REQ-001–REQ-020 surfaces and behavior. Keep the six existing Spanish destinations/labels and their mobile variants; add activity/workout access without replacing or hiding any existing destination. The UX task owns the exact navigation composition.

### Local privacy boundary

Meals, water events, recipes, calendar plans, cycle records/notes, goals, settings, workout sessions, and received HR samples stay in app-private local storage. Do not upload Health Connect data, BLE identity/readings, cycle data, meals, or recipes. Disable automatic cloud/device backup for these records unless a separately approved and clearly disclosed export/restore design is introduced. Use platform-private storage and have the data-design task decide whether/how sensitive records (especially cycle notes and HR samples) are encrypted at rest, key lifecycle, and schema details. Do not claim that the old browser `nutria.v1` value is imported: no export/import path was found. Leave the PWA intact; a future opt-in import is a separate, explicit task.

## Platform and build target

| Choice | Design target | Reason / constraint |
|---|---|---|
| Client language/UI | Kotlin + Jetpack Compose | Native Android UI and platform API access; current Android guidance recommends Compose and layered repositories. |
| Android minimum | `minSdk 28` | Health Connect's Android app/provider is available from Android 9 (API 28); on Android 13 and lower the provider app must be installed, while Android 14+ includes the framework module. |
| Compile/target | `compileSdk 36`, `targetSdk 36` | Health Connect 1.1.0+ requires compile SDK 36. API 36 is a current supported Android API target at design time; recheck current target-SDK distribution requirements before release. |
| Health Connect client | Stable `androidx.health.connect:connect-client:1.1.0` baseline | Use the stable API baseline; do not adopt alpha-only features for daily steps. Pin versions and verify AGP compatibility in the build-preparation task. |
| Persistence | Room for records; DataStore for preferences | Relational records and recipe/calendar references need queryable persistence; small preferences remain separate. DBA task t6 owns schema, encryption, migrations, and backup exclusions. |
| HTTP | Typed repository over an Android HTTP client (OkHttp/Retrofit or equivalent) | Keep multipart and query encoding, DTO mapping, timeout, and error policy out of UI/domain. Pin library versions during implementation. |
| Build | Gradle wrapper checked into `android/`; Android Gradle Plugin and Kotlin versions pinned | Reproducible project build; no machine-global Gradle dependency. |

These choices do **not** establish that the current host can build Android. The recorded host has Java 25.0.5 but no Android SDK, Gradle command/wrapper, `ANDROID_HOME`/`ANDROID_SDK_ROOT`, or ADB. Current Android Gradle Plugin 9.5 documentation lists Gradle 9.6.0 and JDK 17 as its default toolchain; do not assume installed Java 25 is the build runtime. Native build/emulator verification remains blocked until a compatible JDK, SDK platforms/build tools, Gradle wrapper/dependencies, and emulator or Android device are available. Do not report a passing Android build from .NET validation.

## External and platform contracts

### Nutria API — frozen server seam

The base URL is supplied by build/environment configuration or a user-visible connection setting; never embed an API credential. Production requests must use HTTPS. Permit plain HTTP only for an explicitly configured local debug backend if required; do not weaken release network security. The inspected server has no authentication/authorization, so deployment of these routes beyond a trusted environment requires a separate security decision; do not invent client-side auth or alter the frozen routes in this task.

| User action | Native request and response mapping | Failure/side-effect policy |
|---|---|---|
| User confirms a selected meal photo for analysis | `POST {baseUrl}/api/analyze-meal`, `multipart/form-data`: required file part `photo` (JPG/JPEG, PNG, or WebP; maximum 10 MiB), optional text part `name` (maximum 80 characters). Decode JSON `name`, `calories`, `protein`, `carbs`, `fat`. | Before confirmation, explain in Spanish that the selected image and optional dish name are sent to the configured Nutria service and analyzed through OpenAI; do not upload before explicit confirmation. Validate file/type/size locally. Map server `ProblemDetails.title` and HTTP states 400, 413, 429, 502, 503, 504 plus network/timeout/invalid JSON into Spanish actionable states. Do not automatically retry this cost-bearing POST. Add a diary meal only after a successful complete finite-valued response; never cache the image or commit an invalid estimate. Client timeout must allow for the server's 90-second upstream timeout. |
| User submits an ingredient query | `GET {baseUrl}/api/ingredients/search?query={UTF-8 URL-encoded trimmed query}`; trim and enforce 1–80 characters. Decode `{ products: [...] }`; each result has `code`, `name`, `brand`, `calories`, `protein`, `carbs`, `fat` **per 100 g** (up to 8). | Distinguish empty result list from errors; preserve `ProblemDetails`/400, 429, 502, and 504 handling. Import selected nutrition per 100 g without replacing the recipe ingredient's edible-weight amount. Search is read-only and can be repeated after an explicit user action; retain manual entry when service/results are unavailable. |

The Android client talks to the existing Nutria host, not directly to OpenAI or Open Food Facts. The OpenAI key remains server-only. Preserve the server's Spanish Open Food Facts query, filtering, retries, rate limits, and safe provider error mapping without editing endpoint/service code. Do not send diary, cycle, step, or wearable records to these routes.

### Daily steps — Android Health Connect

Use the Health Connect Jetpack client and `StepsRecord.COUNT_TOTAL` aggregate. Query the user's current local day from local midnight to the next local midnight, with no `DataOrigin` filter so the returned total can include available device and provider records (including the post-June-2026 device-specific step source). Show a refresh timestamp and “Health Connect” as the source; step data can be delayed/batched, so refresh on explicit request and when the activity becomes foregrounded rather than claiming a live sensor reading.

Request only `android.permission.health.READ_STEPS` when the user asks to enable/view steps; do not request `WRITE_STEPS` or write workouts/steps to Health Connect. Check provider status and granted permissions every time before reading (permissions can be revoked). Implement distinct UI states: provider unavailable; provider update/install required; permission not granted/denied/revoked; no records for the local day; read/query error; and actual result. No records is not a fabricated numeric zero. Explain in Spanish that steps are read locally from Health Connect and are not uploaded by Nutria. Declare the Health Connect provider package visibility query and the required permission-rationale/privacy-policy activity/alias. On Android 13 and earlier guide installation/update of the provider; on Android 14+ use the framework provider. Do not promise phone step capture merely from installing Nutria: Health Connect's device counter availability depends on OS/SDK extension and Health Connect permission/data availability.

### BLE heart-rate band and workout lifecycle

Use Android's BLE GATT client APIs in a dedicated `WorkoutBleService` (or equivalently lifecycle-owned service adapter) plus a repository-facing event/state stream. Discover/select a device only after the user presses Connect. Support a generic device only when its GATT server exposes the standard Heart Rate service `0x180D` and Heart Rate Measurement characteristic `0x2A37`; connect with `BluetoothGatt`, discover services, enable characteristic notifications and the CCCD, and parse the flags-defined 8-bit/16-bit value. Never treat a generic advertising name or a connected GATT link as proof that a usable heart-rate characteristic exists.

Use the least permissions needed: `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` for Android 12+ with runtime requests; legacy Bluetooth declarations plus runtime location permission for scanning on Android 11 and lower (maxSdkVersion 30). Do not derive location; document any `neverForLocation` use and validate that it does not prevent target band discovery. Check BLE hardware support, adapter state, and permission before scan/connect; Bluetooth-off opens the system enable/settings action rather than silently toggling it. Distinguish unsupported hardware, permission denied, Bluetooth off, scan/connect failure, missing service/characteristic, lost connection, and no/stale measurement.

Connection and workout are separate explicit user actions. **Starting** a workout starts the foreground service from the visible app, records a local session start, and shows an ongoing notification with Stop; on Android 14+ declare/use `connectedDevice` foreground service type and `FOREGROUND_SERVICE_CONNECTED_DEVICE`, satisfying platform prerequisites before start. The service owns GATT and streams actual timestamped notifications to the UI and local store while the user navigates or the app is backgrounded. Stop ends the session and closes/unregisters GATT; Disconnect is explicit and stops an active session with an interruption/end marker rather than leaving a phantom workout.

Persist only actual received HR values with timestamps and explicit start/stop/interruption markers. While connected, mark the latest value stale if no new notification has arrived for 15 seconds; show the timestamp rather than presenting stale data as live. On disconnection, Bluetooth disable, service/process death, permission loss, or app return, display a gap/stale state and preserve the gap in the session. Validate the stale threshold against compatible bands during device testing. Do not auto-fill missing samples, auto-start, claim continuous readings, or derive calories/exercise minutes. If reconnection is offered, the user confirms it and the session retains a visible gap. Never use Health Connect to supplement a missing BLE reading.

## State and contract ownership by feature

| Package / flow | Owns | Existing behavior and state that must remain |
|---|---|---|
| `feature/home` | Local-day totals, progress, streak, overview | Summary values derive from diary/water; streak stays consecutive days with meal or nonzero water; show actual Health Connect steps or the honest state above. |
| `feature/diary` | Manual food conversion, meal records/history, water events | 28.3495 g/oz conversion, portions × gram equivalent, nutrient calculation, 30-day history, today-only edits, last-water-entry undo, confirmed clear-today isolation. |
| `feature/recipes` | Ingredients, per-serving nutrition, saved recipes and local recommendations | 11 fixed recommendations; up to three suggestions, existing filters and ≤550 kcal eligibility; delete a recipe and its calendar assignments transactionally; call the frozen search route. |
| `feature/photo` | URI selection/preview, API call, pending result | Explicit submit, file/name constraints, approximate-result disclosure, no entry on errors; call frozen photo endpoint only. |
| `feature/calendar` | Local month assignment and generation | Monday-first, optional breakfast, week/month generation, category constraints, avoid same-day duplicate where possible, missing-category and replacement confirmation behavior. |
| `feature/cycle` | Period history/configuration, estimate rules, private journal | Valid date/overlap rules, 21–45 cycle and 2–10 period settings, estimates/caveats, local private notes ≤160 chars; not diagnosis/fertility guidance. |
| `feature/settings` | Goals, four themes, privacy disclosures, API base URL | Existing goal validation and theme persistence/cancel semantics; user-facing descriptions for both platform data flows. |
| `feature/activity` | Health Connect read state and local workout session/sample state | Steps are read-only Health Connect data; HR is only device notifications received through an active BLE session; no server write or fabricated metric. |

## Migration and delivery gates

1. Add the new native target alongside—not in place of—`Program.cs`, `Endpoints/`, `Services/`, and `wwwroot/`. Do not modify the existing API contracts or remove legacy assets.
2. Implement all t1 REQ-001–REQ-020 as native Compose user flows; keep new activity work separately traceable to REQ-021–REQ-026. A native shell with incomplete feature packages is not parity.
3. Keep browser `nutria.v1` data untouched. Do not imply auto-migration; decide any explicit transfer separately with disclosure, validation, and user consent.
4. Verify frozen API contract mapping with request/response/error tests and a backend integration probe; verify Health Connect provider/permission/empty/error cases and BLE permission/off/unsupported/incompatible/lost/stale/background paths on suitable Android devices. Fake adapters may test state machines but must never be reported as real sensor data.
5. Run a full Gradle build from `android/` and Android unit/instrumentation tests only after toolchain preparation. Report actual build/device evidence; SDK/toolchain absence is a blocker, not a reason to downgrade into a WebView or claim a native pass.

## Risks and required decisions

- **Android toolchain unavailable (blocker for compile/runtime proof):** install/activate a compatible JDK, Android SDK (platform 36/build tools), wrapper-compatible Gradle, and ADB/emulator or real devices before claiming native validation.
- **Backend endpoints are unauthenticated:** existing API contract is preserved as instructed, but a publicly reachable endpoint accepts photo and food-search requests subject only to IP rate limits. Require trusted HTTPS deployment and separate security review before public launch; no local personal records are sent by this design.
- **Browser data has no transfer path:** the existing PWA profile may contain unique local records. Retain the old app and data; any import/export is explicitly out of current scope and cannot be presumed.
- **BLE vendors vary:** standard-service support is the interoperability criterion; manufacturer names/proprietary characteristics are not accepted as a fallback. Validate notification CCCD behavior and 8-/16-bit measurements on multiple real bands.
- **Health Connect availability/source:** provider install state, permission revocation, day-boundary/time-zone changes, and delayed step batches must map to honest UI states. Do not hardcode a device DataOrigin.
- **Sensitive local data:** the data-design task must make encryption/backup/migration choices explicit for cycle notes and HR samples before implementation rather than assuming app sandbox alone is the complete privacy model.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md` — REQ-001–REQ-026 and observable behavior for preservation and new activity requirements.
- `.github/modernize/rearchitecture/artifacts/t1-pm-requirements-checklist.md` — scope completeness across existing PWA, API failures, and new Android states.
- `.github/modernize/rearchitecture/artifacts/t2-architect.md` — current architecture inventory and toolchain limitation summary.
- `.github/modernize/rearchitecture/artifacts/migration_boundary.yaml` — `android/` is new runtime scope; backend is copied as-is and PWA remains.
- `.github/modernize/rearchitecture/artifacts/wire_contracts.yaml` — frozen photo/ingredient API and server-side provider/secret contracts.
- `.github/modernize/rearchitecture/artifacts/seams.yaml` — declared bridge mapping and frozen-side rules.
- `.github/modernize/rearchitecture/artifacts/data-model.md` — current `nutria.v1` state shape and no established browser-to-Android transfer.
- `.github/modernize/rearchitecture/artifacts/tech-stack.md` — ASP.NET Core 10 host, request timeouts/limits, and verified host limitations.
- `.github/modernize/rearchitecture/artifacts/constitution.md` — native parity, privacy, truthful Health Connect/BLE, and incremental evidence guardrails.
- `board.md` and `team/architect/inbox.md` — task sequencing, t1 handoff, and t3 constitution notice.
- `team/architect/log.md` and `learnings/architect/native-android-api-seam.md` — prior architecture findings and API-seam learning.
- Android Developers documentation (consulted 2026-11-14): [Health Connect setup](https://developer.android.com/health-and-fitness/health-connect/get-started), [Health Connect steps](https://developer.android.com/health-and-fitness/health-connect/features/steps), [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions), [GATT client](https://developer.android.com/develop/connectivity/bluetooth/ble/connect-gatt-server), [foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types), [architecture recommendations](https://developer.android.com/topic/architecture/recommendations), [AGP 9.5 compatibility](https://developer.android.com/build/releases/agp-9-5-0-release-notes).
- Bluetooth SIG Heart Rate Service specification: [Heart Rate Service](https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/HRS_v1.0/out/en/index-en.html).

## Evidence Mapping

- `t1-pm-spec.md#REQ-001–REQ-020` → “Target structure and boundaries”, “State and contract ownership by feature”, and the two frozen API request mappings.
- `t1-pm-spec.md#REQ-021–REQ-022` → “Daily steps — Android Health Connect” permission, aggregate, state, and disclosure boundaries.
- `t1-pm-spec.md#REQ-023–REQ-026` → “BLE heart-rate band and workout lifecycle” connection, permissions, actual readings, session interruptions, and disclosure.
- `migration_boundary.yaml#must_rewrite/copy_as_is/legacy_allowed_to_remain` → “Decision” and “Migration and delivery gates”; new native app only, backend and PWA retained.
- `wire_contracts.yaml#native-client-photo-analysis` and `seams.yaml#MealAnalysisEndpoints.cs:28` → photo API table row and no-retry/no-invalid-diary-commit rules.
- `wire_contracts.yaml#native-client-ingredient-search` and `seams.yaml#OpenFoodFactsEndpoints.cs:27` → lookup API table row and per-100-g/manual-fallback rules.
- `wire_contracts.yaml#server-to-openai-responses/server-to-openfoodfacts-search` → server remains the sole provider integration/key holder; no provider call or credential in Android.
- `data-model.md#Persistence boundary` → local-only record ownership, no assumed `nutria.v1` import, and PWA retention.
- `tech-stack.md#Runtime configuration and limits` and Android Developers toolchain docs → explicitly blocked native build/runtime claims until Android SDK/JDK/Gradle/ADB are present.
- `constitution.md#Principles I–V` → native parity, frozen API contracts, local privacy, truthful sensor state, and phased verification gates.

## Verification

- Method: cross-checked the native target boundary and each feature/API flow against t1 requirements, t2 wire contracts/seams/migration boundary, and t3 constitution; verified platform-specific requirements against current official Android/Bluetooth SIG documentation.
- Tests/build: not applicable to this documentation-only design task. No Android project/toolchain exists on the inspected host; no Android compile or runtime test is claimed.
- Open decisions handed off: t5 owns the exact Spanish navigation/flow composition; t6 owns local schema, encryption, key lifecycle, backup exclusion, and migration policy details; t7 owns the execution-ready test matrix and device/toolchain gates.
