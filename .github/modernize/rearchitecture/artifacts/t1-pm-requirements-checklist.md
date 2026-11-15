# t1 — Requirements quality checklist

## Inventory coverage

- [x] The full hand-maintained app source was inventoried: 16 files across root configuration/docs, endpoints/services, and PWA assets.
- [x] Existing PWA behavior is distinguished from newly requested Android-only health/wearable behavior.
- [x] All six existing navigation destinations and Spanish/mobile behavior are covered.
- [x] Nutrition diary calculations, daily goals, water increments/undo, clear-today behavior, history, and streak are covered.
- [x] Recipe authoring, manual/Open Food Facts nutrition entry, recommendations, saved recipes, and calendar assignments are covered.
- [x] Calendar month/week random generation, breakfast option, category prerequisites, and replacement confirmation are covered.
- [x] Menstrual period dates, estimates, configuration, symptoms, private notes, and safety caveats are covered.
- [x] Photo selection, server submission, response handling, API key boundary, and failures are covered.
- [x] Settings/theme and local persistence/privacy are covered.
- [x] Health Connect and standard BLE Heart Rate GATT are explicitly marked as new requirements, with permissions, no-data/error, lifecycle, no-fabrication, and disclosure states.
- [x] Existing backend request/response contracts and error/rate-limit behavior are documented.

## Requirement ID Coverage

- [x] All 26 functional requirements use unique sequential `REQ-XXX` IDs.
- [x] REQ-001–REQ-020 describe existing product behaviors and preserved server boundaries.
- [x] REQ-021–REQ-026 describe newly requested native Android behaviors, not existing PWA capabilities.
- [x] Every requirement has observable outcomes or corresponding Given/When/Then scenarios.

## Testability and completeness

- [x] User stories are prioritized and independently testable.
- [x] Boundary/error cases are captured for validation, persistence, API errors, unavailable sources, permission denial, device loss, and background transitions.
- [x] Success criteria are user-facing and verifiable.
- [x] Known exclusions from the existing PWA (accounts, sync, fitness tracking, and past-day diary editing) are explicit.
- [x] No implementation design, framework choice, source code, or project scaffolding was introduced by this PM artifact.

## Evidence locations for downstream review

- PWA UI and behavior: `wwwroot/index.html`, `wwwroot/app.js`, `wwwroot/styles.css`, `wwwroot/manifest.webmanifest`
- Photo contract: `Endpoints/MealAnalysisEndpoints.cs`, `Services/OpenAiMealAnalysisService.cs`, `Services/IMealAnalysisService.cs`, `Program.cs`
- Food search contract: `Endpoints/OpenFoodFactsEndpoints.cs`, `Services/OpenFoodFactsService.cs`, `Services/IOpenFoodFactsService.cs`, `OpenFoodFacts.http`
- Current product/privacy constraints: `README.md`, `Nutria.csproj`

## Upstream Artifacts Consumed

- None — no dependency artifacts were provided.

## Evidence Mapping

- `t1-pm-spec.md#Functional Requirements` → this checklist's coverage of REQ-001–REQ-026.
- `t1-pm-spec.md#Existing API Contracts and Observable Service Boundaries` → this checklist's backend-contract and error-coverage checks.
