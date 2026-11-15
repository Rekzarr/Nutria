# Existing client data model

## Persistence boundary

All personal records and preferences are serialized as one JSON value in browser `localStorage` under `nutria.v1` (`wwwroot/app.js:3,249-302`). There is no server-side database or account/sync model. Cycle notes are explicitly described by the UI as remaining on the device. The client does not persist the meal photo itself.

The following describes the existing browser state as implemented; it does not prescribe a target schema.

## State inventory

- **`goals`** — `calories`, `water`, `protein`, `carbs`, `fat`; defaults are 2,000 kcal, 2,000 ml, 120 g, 250 g, and 65 g.
- **`days`** — keyed by local `YYYY-MM-DD`; each day has cumulative `water`, ordered `waterEntries` for undo, and `meals`.
- **Meal entry** — `id`, `name`, `calories`, `protein`, `carbs`, `fat`, `source` (`food`, `recipe`, or `photo`), and `createdAt`. Nutrient values are per logged portion/amount.
- **`recipes`** — saved recipes with `id`, optional `recommendationId`, `name`, `category` (`meal` or `dinner`), `servings`, recipe-level nutrients, ingredient list, and optional `prepMinutes`, `complexity`, and `instructions`.
- **Ingredient** — `name`, human-entered `amount` and `unit`, edible weight `grams`, and `nutrition` values per 100 g (`calories`, `protein`, `carbs`, `fat`). Non-gram units require a separate gram equivalent for nutrition calculation.
- **`periods`** — records with `id`, start date, and optional end date.
- **`cycleSettings`** — configured `cycleLength` and `periodLength` (validated by the UI to 21–45 and 2–10 days, respectively).
- **`cycleDays`** — date-keyed daily values for `flow`, `pain` (0–10), `mood`, and optional private `notes` (UI limit: 160 characters).
- **`recommendationSettings`** — `maxPrepMinutes`, `complexity`, and `category`.
- **`theme`** — one of `sage`, `ocean`, `lavender`, or `coral`.
- **`calendar`** — `thirdMealEnabled`, `viewMonth`, and `months`; monthly assignments map a date and meal slot (`meal`, `dinner`, optional `breakfast`) to a saved recipe ID.

## Relationships and behavior

- A day contains multiple meal entries and water events. Daily overview totals are computed from the current day's meal nutrients and water values.
- A recipe contains ingredient rows; recipe totals are calculated by summing each ingredient's per-100-g nutrients scaled by edible grams. Serving values divide recipe totals when adding one serving to a day's diary.
- Calendar slots reference saved recipe IDs; deleting a recipe removes its assignments from every stored month.
- Cycle-day notes and period records are keyed/related by local calendar dates; cycle phases and next-start dates are derived from the latest past period and configured averages, not persisted as medical conclusions.
- Recommendations are a static catalog embedded in `app.js`, not persisted until the user saves one as a recipe.
- Open Food Facts search results and meal photo analysis responses are transient; selecting/confirming them can populate local recipe or meal values.

## Privacy and migration evidence

Local browser storage means there is no account-based synchronization and data visibility is bounded by the browser origin/profile. The inspected app does not show encryption, export/restore, or an import path. Moving to Android therefore does not automatically transfer existing `nutria.v1` records; any transfer/import behavior is an open design question. Health Connect and BLE records do not exist in this current model.
