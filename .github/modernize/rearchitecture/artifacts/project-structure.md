# Existing project structure

> **Snapshot note:** the inventory and file counts below originated in the initial
> architecture assessment. ADR-0009 and the current project files supersede its
> ingredient-search backend references; the repository now also contains `android/`.

## Classification

Full-stack ASP.NET Core web application: one server process hosts the photo-analysis REST API and static Spanish PWA from the same origin. The PWA and Android client query Open Food Facts directly for ingredient search. The repository also contains the native Android app.

## Runtime and layers

- **Web host:** `Program.cs` configures Kestrel/form limits, dependency injection, OpenAPI in Development, rate limiting, error handling, and static-file fallback.
- **HTTP API:** ASP.NET Core minimal API under `Endpoints/`; only the photo-analysis route remains.
- **External-service layer:** typed `HttpClient` service under `Services/` for the photo endpoint's OpenAI call. Ingredient search is performed by Android and PWA clients directly against Open Food Facts.
- **Client:** `wwwroot/index.html` contains the six hash-addressed tab panels and dialogs; `app.js` owns state, calculations, rendering, event handlers, and API calls; `styles.css` provides responsive presentation and four themes.
- **Persistence:** browser `localStorage` only. No server database, account system, repository layer, or cross-device synchronization was found.

## Entrypoints and key files

| File | Role |
|---|---|
| `Program.cs` | ASP.NET Core host, middleware, DI, rate limits, API registration, and static-file hosting |
| `wwwroot/index.html` | PWA shell, Spanish navigation, six feature panels, and settings/menu/calendar dialogs |
| `wwwroot/app.js` | Client state and behavior; `nutria.v1` is the persisted browser key |
| `wwwroot/styles.css` | Responsive visual system, theme variables, and page/component styles |
| `wwwroot/manifest.webmanifest` | Standalone-installable web app metadata and Spanish locale |
| `Endpoints/MealAnalysisEndpoints.cs` | `POST /api/analyze-meal` contract and request validation |
| `Services/OpenAiMealAnalysisService.cs` | Server-side OpenAI Responses API integration |
| `android/app/src/main/java/com/nutria/app/data/remote/OpenFoodFactsClient.kt` | Android's direct Open Food Facts query, filtering, and nutrient mapping |
| `Dockerfile` | .NET 10 multi-stage image prepared for local/container deployment; not deployed |
| `Nutria.csproj` | `net10.0`, nullable/implicit usings, warnings-as-errors, and OpenAPI package references |

No service-worker registration or service-worker file was found; the manifest establishes install metadata but does not itself establish offline caching.

## Existing source inventory

Count is from the project root, excluding `.github`, `bin`, and `obj`; it includes documentation and the HTTP example.

| Area | Files | Contents |
|---|---:|---|
| Root | 4 | project file, host entrypoint, README, HTTP request example |
| `Endpoints/` | 2 | minimal API route modules |
| `Services/` | 5 | two service interfaces, two service implementations, one provider exception |
| `wwwroot/` | 5 | HTML, JavaScript, CSS, manifest, icon |
| **Total** | **16** | C# server plus browser client |

The supplied project profile reports 2,453 LOC across C# and JavaScript. No source project or existing runtime boundary for Android is present.

## Functional areas visible in the client

- Daily overview, calorie/macronutrient goals, water entries and undo, streak, meal list and 30-day meal history.
- Manual food entry with gram, ounce, and serving conversions.
- Recipe authoring, nutrient calculation, ingredient search, curated recommendations, and recipe-to-diary actions.
- Photo meal estimate, preview, upload, and adding the returned estimate to the day.
- Monthly recipe calendar, editable meal slots, optional breakfast, and random week/month generation.
- Menstrual cycle estimates, period history, configurable cycle/period length, and daily flow/pain/mood/private notes.
- Settings for daily goals and four color themes.

## Migration boundary facts

The user requires a native Android deliverable with parity, while preserving the existing photo-analysis and ingredient-search server contracts. The smallest new runtime is an Android client calling the existing ASP.NET Core endpoints; the existing PWA and backend can remain in the repository. This finding does not select an Android language, UI toolkit, module layout, or persistence technology; those are design-phase choices.
