# t2 — Current application architecture and Android migration constraints

## Summary

Nutria is a small full-stack ASP.NET Core 10 host with a plain JavaScript Spanish PWA and two server-side integration endpoints. User records are currently browser-local under `localStorage:nutria.v1`; there is no existing Android app, server database, authentication, synchronization, Health Connect, or BLE integration. The new native app must implement all six PWA tabs plus settings and new Health Connect/BLE workflows; the existing backend routes are preserved seams and the PWA remains until parity is established.

## Deliverables

- [architecture_index.md](./architecture_index.md) — implementation navigation guide for all nine units.
- [project-structure.md](./project-structure.md) — existing project layers, entrypoints, inventory, and features.
- [tech-stack.md](./tech-stack.md) — observed dependencies, configuration, and host toolchain evidence.
- [data-model.md](./data-model.md) — existing localStorage state model and transfer implications.
- [unit_graph.yaml](./unit_graph.yaml) — six UI tabs, settings dialog, and two API route units.
- [migration_boundary.yaml](./migration_boundary.yaml) — phased native-client scope; current backend and PWA remain.
- [wire_contracts.yaml](./wire_contracts.yaml) — frozen client API and existing upstream provider contracts.
- [shared_modules.yaml](./shared_modules.yaml) — shared browser runtime/shell/styles and ASP.NET host.
- [cross_unit_state.yaml](./cross_unit_state.yaml) — current client-local state relationships.
- [seams.yaml](./seams.yaml) — declared photo and ingredient-search server boundaries.
- [units/daily-overview/behavior.yaml](./units/daily-overview/behavior.yaml) — daily summary, water, and meals.
- [units/daily-overview/bindings.yaml](./units/daily-overview/bindings.yaml) — daily overview wiring.
- [units/daily-overview/unit_decomposition.yaml](./units/daily-overview/unit_decomposition.yaml) — daily overview split candidates.
- [units/food-diary/behavior.yaml](./units/food-diary/behavior.yaml) — manual food calculator and history behavior.
- [units/food-diary/bindings.yaml](./units/food-diary/bindings.yaml) — food diary wiring.
- [units/food-diary/unit_decomposition.yaml](./units/food-diary/unit_decomposition.yaml) — food diary split candidates.
- [units/recipe-library/behavior.yaml](./units/recipe-library/behavior.yaml) — recipes, recommendations, ingredient lookup, and diary interaction.
- [units/recipe-library/bindings.yaml](./units/recipe-library/bindings.yaml) — recipe library wiring.
- [units/recipe-library/unit_decomposition.yaml](./units/recipe-library/unit_decomposition.yaml) — optional authoring/recommendation split candidate.
- [units/photo-meal/behavior.yaml](./units/photo-meal/behavior.yaml) — image selection, analysis request, and diary result.
- [units/photo-meal/bindings.yaml](./units/photo-meal/bindings.yaml) — photo flow wiring.
- [units/photo-meal/unit_decomposition.yaml](./units/photo-meal/unit_decomposition.yaml) — photo flow split candidates.
- [units/meal-calendar/behavior.yaml](./units/meal-calendar/behavior.yaml) — monthly calendar, assignments, and random menu generation.
- [units/meal-calendar/bindings.yaml](./units/meal-calendar/bindings.yaml) — meal calendar wiring.
- [units/meal-calendar/unit_decomposition.yaml](./units/meal-calendar/unit_decomposition.yaml) — calendar split candidates.
- [units/cycle-tracker/behavior.yaml](./units/cycle-tracker/behavior.yaml) — estimates, period records, and private daily notes.
- [units/cycle-tracker/bindings.yaml](./units/cycle-tracker/bindings.yaml) — cycle tracker wiring.
- [units/cycle-tracker/unit_decomposition.yaml](./units/cycle-tracker/unit_decomposition.yaml) — cycle tracker split candidates.
- [units/app-settings/behavior.yaml](./units/app-settings/behavior.yaml) — goals and theme settings.
- [units/app-settings/bindings.yaml](./units/app-settings/bindings.yaml) — settings dialog wiring.
- [units/app-settings/unit_decomposition.yaml](./units/app-settings/unit_decomposition.yaml) — settings split candidates.
- [units/meal-analysis-api/behavior.yaml](./units/meal-analysis-api/behavior.yaml) — upload validation and server-side OpenAI behavior.
- [units/meal-analysis-api/bindings.yaml](./units/meal-analysis-api/bindings.yaml) — analysis API route, DI, and runtime configuration.
- [units/meal-analysis-api/unit_decomposition.yaml](./units/meal-analysis-api/unit_decomposition.yaml) — analysis API split candidates.
- [units/ingredient-search-api/behavior.yaml](./units/ingredient-search-api/behavior.yaml) — query validation and Open Food Facts mapping.
- [units/ingredient-search-api/bindings.yaml](./units/ingredient-search-api/bindings.yaml) — ingredient API route, DI, and runtime configuration.
- [units/ingredient-search-api/unit_decomposition.yaml](./units/ingredient-search-api/unit_decomposition.yaml) — ingredient API split candidates.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/project-profile.yaml` — supplied project scope, LOC/languages, and rewrite intent.
- `board.md` — task sequencing; confirms `t1` feature inventory is separately in progress.
- User task acceptance criteria — declared Android target, parity, Health Connect/BLE, API compatibility, and retention boundaries.
- No dependency-role artifact was provided; `t1` output was not present when this analysis was prepared.

## Evidence Mapping

- `project-profile.yaml#project.structure` and inspected root files → `project-structure.md`, `tech-stack.md`.
- `project-profile.yaml#assessment.change_type` plus user acceptance criteria → `migration_boundary.yaml`.
- `Endpoints/MealAnalysisEndpoints.cs` and `OpenFoodFactsEndpoints.cs` → frozen client contracts in `wire_contracts.yaml` and declared seams in `seams.yaml`.
- `wwwroot/app.js#STORAGE_KEY/DEFAULTS/loadState/saveState` → `data-model.md`, `shared_modules.yaml`, and `cross_unit_state.yaml`.
- `wwwroot/index.html#tab-panels/dialogs` and `app.js#render/attachEvents` → nine UI/API units and their per-unit artifacts.
- Host environment commands (`dotnet --list-sdks`, `dotnet workload list`, `java --version`, PATH/env checks) → `tech-stack.md` Android build limitation.

## Validation and findings

- Source inventory: 16 project files excluding `.github`, `bin`, and `obj`; project profile reports 2,453 LOC.
- Unit completeness: 9 units in `unit_graph.yaml`; matching per-unit behavior, bindings, and decomposition files are required.
- Structural artifact gates: 9/9/9 behavior/bindings/decomposition files; no duplicate source anchors, invalid referenced source locations, out-of-registry shared fields, or forbidden `TBD` entries; all nine units are present in the implementation guide.
- YAML parser: not available in the host (Python, Node, yq, Ruby, and `ConvertFrom-Yaml` were not found); artifacts received structural/source-anchor checks but no formal parser run.
- Android build/runtime validation: not performed; no Android SDK, Gradle, or ADB was available on the inspected host.
- No source files or backend behavior were changed by this analysis.
