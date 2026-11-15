# Existing project structure

## Classification

Full-stack ASP.NET Core web application: one server process hosts the REST API and static Spanish PWA from the same origin. It is not a monorepo and has no separate Android application or database.

## Runtime and layers

- **Web host:** `Program.cs` configures Kestrel/form limits, dependency injection, OpenAPI in Development, rate limiting, error handling, and static-file fallback.
- **HTTP API:** two ASP.NET Core minimal-API route groups under `Endpoints/`.
- **External-service layer:** typed `HttpClient` services under `Services/`; the photo endpoint calls OpenAI and the ingredient endpoint calls Open Food Facts.
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
| `Endpoints/OpenFoodFactsEndpoints.cs` | `GET /api/ingredients/search` contract and query validation |
| `Services/OpenAiMealAnalysisService.cs` | Server-side OpenAI Responses API integration |
| `Services/OpenFoodFactsService.cs` | Open Food Facts query, retry, and product mapping |
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
