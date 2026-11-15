# Nutria architecture implementation guide

## Implementation Guide

This index is not the full contract. Do not implement from this file alone; follow the listed artifact paths and the source locations in the contracts.

### Global artifacts

- `unit_graph.yaml` — all implementation agents; identifies user-triggered pages and API routes, current anchors, API dependencies, shared references, and frozen signatures. Filter to the assigned unit.
- `migration_boundary.yaml` — all implementation agents; defines the Android-only new runtime scope and keeps existing backend/PWA files out of rewrite scope. Use `must_rewrite`; `source_anchors` are evidence, not rewrite targets.
- `wire_contracts.yaml` — agents touching photo analysis or ingredient lookup; filter by `unit`; retain frozen request, response, error, and upstream-provider boundaries.
- `shared_modules.yaml` — all client agents and API-host consumers; filter `used_by_units`; current browser runtime is a single responsibility-heavy shared script, not a native template.
- `cross_unit_state.yaml` — client/data agents; filter flows by `writer.unit` or `reader.unit`; reproduce locally shared state relationships and verify them end-to-end.
- `seams.yaml` — photo and recipe/ingredient agents; filter `cut_between`; these user-declared backend cuts are authoritative and the frozen side is not to be refactored in this phase.
- `project-structure.md` — all agents; describes current layers, entrypoints, file inventory, and user-visible domains.
- `tech-stack.md` — all agents; records existing runtime/dependencies and verified host limitations, without choosing the Android stack.
- `data-model.md` — all data and feature agents; documents the full current `nutria.v1` state shape and data-transfer implications.

### Per-unit contracts

For each assignment, read the matching behavior, bindings, and decomposition files. Filter the global files as directed above. `unit_decomposition.yaml` files are candidates only (`commit: false`).

| Unit / trigger | Exact unit artifacts | Global rows and completion evidence |
|---|---|---|
| `daily-overview` — PWA `#inicio` | `units/daily-overview/behavior.yaml`; `units/daily-overview/bindings.yaml`; `units/daily-overview/unit_decomposition.yaml` | Read shared client modules, meal/goals state flows, and migration boundary. Report the consumed files, daily totals/progress/water/meal behaviors preserved, and native/runtime evidence. |
| `food-diary` — PWA `#comida` | `units/food-diary/behavior.yaml`; `units/food-diary/bindings.yaml`; `units/food-diary/unit_decomposition.yaml` | Read shared client modules and manual-meal state flow; preserve unit conversions, history/removal, and totals; report verification evidence. |
| `recipe-library` — PWA `#recetas` | `units/recipe-library/behavior.yaml`; `units/recipe-library/bindings.yaml`; `units/recipe-library/unit_decomposition.yaml` | Read frozen ingredient-search contract/seam, shared client modules, and recipe-to-calendar flow. Report recipe nutrition, recommendations/filters, search/manual fallback, saving and diary actions. |
| `photo-meal` — PWA `#foto` | `units/photo-meal/behavior.yaml`; `units/photo-meal/bindings.yaml`; `units/photo-meal/unit_decomposition.yaml` | Read frozen photo contract/seam and photo-to-diary flow. Report supported formats/size, consent/submit flow, valid estimate handling, errors, and key remains server-side. |
| `meal-calendar` — PWA `#agua` (displayed as Calendario/Plan) | `units/meal-calendar/behavior.yaml`; `units/meal-calendar/bindings.yaml`; `units/meal-calendar/unit_decomposition.yaml` | Read recipe/calendar state flow and shared modules. Report month/week plan editing, optional breakfast, overwrite/missing-category branches, and persistence. |
| `cycle-tracker` — PWA `#ciclo` | `units/cycle-tracker/behavior.yaml`; `units/cycle-tracker/bindings.yaml`; `units/cycle-tracker/unit_decomposition.yaml` | Read `data-model.md` and migration boundary. Report private local periods/notes, date validation, estimates/disclaimers, and persistence; do not represent estimates as medical data. |
| `app-settings` — Ajustes dialog | `units/app-settings/behavior.yaml`; `units/app-settings/bindings.yaml`; `units/app-settings/unit_decomposition.yaml` | Read goals/theme state flows, shared UI modules, and migration boundary. Report all goals and four themes preserved, and local persistence. |
| `meal-analysis-api` — `POST /api/analyze-meal` | `units/meal-analysis-api/behavior.yaml`; `units/meal-analysis-api/bindings.yaml`; `units/meal-analysis-api/unit_decomposition.yaml` | Read its frozen wire contract, declared seam, shared ASP.NET host, and migration boundary. Report server route/provider behavior unchanged, credential boundary, and any approved divergence. |
| `ingredient-search-api` — `GET /api/ingredients/search` | `units/ingredient-search-api/behavior.yaml`; `units/ingredient-search-api/bindings.yaml`; `units/ingredient-search-api/unit_decomposition.yaml` | Read its frozen wire contract, declared seam, shared ASP.NET host, and migration boundary. Report route/search mapping/error behavior unchanged and any approved divergence. |

### Required completion evidence

Implementation agents report exact artifact paths read, each applicable `must_preserve` contract, unresolved/deferred behavior, and relevant build/test/device/runtime evidence. The inspected host lacks Android SDK, Gradle, and ADB, so no native compilation/device result may be inferred from this analysis.
