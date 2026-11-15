# Nutria — existing feature inventory and Android parity baseline

**Task**: t1 — Inventory all current Nutria features and observable behavior  
**Status**: Ready for architecture, UX, data, and planning phases  
**Discovery scope**: 16 hand-maintained files in the current app: 4 root files, 2 endpoint files, 5 service files, and 5 `wwwroot` assets. Generated `bin/` and `obj/` outputs were excluded.

## Scope Baseline

- **Discovery method**: inspected the project README and project file; read all endpoint and service source; read the complete HTML, JavaScript, CSS, PWA manifest, and app icon; cross-checked visible controls against client event handlers and service request/response behavior.
- **Total items discovered**: 16 hand-maintained application files.
- **Items in scope**: 16/16. All contribute to user-visible behavior, API contracts, privacy/data flow, or the current app shell.
- **Current product baseline**: Spanish-first, responsive ASP.NET Core 10 PWA, with six navigation destinations. Nutrition and personal records are kept in browser-local storage; there is no account or cross-device synchronization.
- **Migration scope**: deliver a native Android app that preserves all existing product capabilities and Spanish behavior. Retaining the PWA sources during parity work is allowed and expected; a browser-only wrapper is not a native implementation.
- **New-request boundary**: daily steps from Android Health Connect and live workout heart rate from a standard BLE Heart Rate Service band are newly requested capabilities and are absent from the current PWA. Do not claim them as existing behavior.

## User Scenarios & Testing

### User Story 1 — Review today's nutrition and hydration (Priority: P1)

As a person keeping a food diary, I want one daily summary of my calories, macronutrients, water, and logged meals so I can review today's progress.

**Why this priority**: It is the core diary outcome and aggregates the principal food and water actions.

**Independent Test**: Add food entries in different units, log and undo water, and verify today's summary updates and persists after restarting the app.

**Acceptance Scenarios**

1. **Given** a fresh local profile, **When** I open the home summary, **Then** it shows zero intake, default daily goals (2,000 kcal, 2,000 ml, 120 g protein, 250 g carbohydrates, 65 g fat), and an empty-meals state.
2. **Given** I have added meals and water today, **When** I return to the summary, **Then** totals, remaining calories, macro progress, water progress, meal count, and meal details reflect those records.
3. **Given** I have records on consecutive days, **When** I view the summary, **Then** the care streak counts consecutive days with at least one meal or nonzero water; a day with neither ends the streak.
4. **Given** I have food or water records today, **When** I choose to clear today's record and confirm, **Then** today's food and water are removed, while recipes, calendar plans, cycle records, goals, and theme remain.
5. **Given** I cancel the clear-today confirmation, **When** I return to the summary, **Then** existing records remain unchanged.

### User Story 2 — Log food by weight or measure (Priority: P1)

As a person tracking nutrition, I want to calculate and record food from nutrition values per 100 g and the amount I ate.

**Independent Test**: Log one item in grams, one in ounces, and one in portions; compare the displayed nutrients with the amount-based calculation and remove an entry.

**Acceptance Scenarios**

1. **Given** valid name, amount, and per-100-g calorie/macronutrient values, **When** I submit the food form, **Then** a meal is added to today's diary and contributes proportionally to daily totals.
2. **Given** I choose ounces, **When** I enter an amount, **Then** the app converts using 1 oz = 28.3495 g and previews the resulting calories and macros.
3. **Given** I choose portions, **When** I provide grams per portion, **Then** the calculation uses amount × portion weight.
4. **Given** incomplete required values, **When** I try to submit, **Then** the form prevents an invalid entry; a valid entry can be removed individually.
5. **Given** entries exist within the previous 30 calendar days, **When** I open food history, **Then** meals are grouped by date with today/yesterday labels where applicable; days without meals are omitted.
6. **Given** a date other than today, **When** I view its history, **Then** its meals are view-only in the current PWA; adding or clearing diary entries applies to today.

### User Story 3 — Create and reuse recipes (Priority: P1)

As a person planning meals, I want to save recipes whose nutrition is calculated from their ingredients and add a serving to my diary.

**Independent Test**: Create a recipe using multiple ingredients and servings, verify its per-recipe and per-serving nutrients, add a serving to today's diary, and delete the recipe.

**Acceptance Scenarios**

1. **Given** a recipe name, meal/dinner category, positive integer serving count, and at least one complete ingredient, **When** I save it, **Then** the recipe is listed under its category with per-serving calories/macros and ingredient details.
2. **Given** an ingredient measured in grams, **When** I change its amount, **Then** its edible gram weight follows the amount; for a non-gram unit (ml, unit, tablespoon, teaspoon, pinch, or to taste), I must supply a gram equivalent to calculate nutrition.
3. **Given** ingredient values per 100 g and edible weight, **When** a value changes, **Then** recipe totals are recalculated as the sum of each ingredient's proportional values; saving remains disabled until all required values and servings are valid.
4. **Given** a saved recipe, **When** I choose “add” on it, **Then** one serving's nutrients are added to today's diary.
5. **Given** a recipe is assigned in calendar plans, **When** I delete that recipe, **Then** its calendar assignments are removed as well.
6. **Given** recipe category is changed, **When** I use random menu generation later, **Then** the category affects meal/dinner random selection; it does not prevent manual assignment to any calendar slot.

### User Story 4 — Look up ingredient nutrition in Open Food Facts (Priority: P1)

As a person building a recipe, I want to search a product by ingredient name, review its nutrition, and import the per-100-g values without losing the ability to correct them.

**Independent Test**: Search for an ingredient, select a result, verify its nutrient fields populate, then adjust values manually or enter values when there are no usable results.

**Acceptance Scenarios**

1. **Given** a non-empty query up to 80 characters, **When** I search, **Then** Nutria returns at most eight products with a name, optional brand, and complete calories/protein/carbohydrate/fat values per 100 g.
2. **Given** a result is selected, **When** I review the ingredient, **Then** the per-100-g nutrient inputs are populated and remain editable, with a reminder to check the package and serving.
3. **Given** the query is empty or longer than 80 characters, **When** it reaches the service, **Then** the API returns a localized 400 validation problem.
4. **Given** there are no products with complete nutrient values, **When** the response succeeds, **Then** the user can enter values manually and no product is silently imported.
5. **Given** the backend, upstream food database, or network fails, **When** the search is attempted, **Then** the user sees a localized connection/service error and can retry; a timeout or invalid upstream response is not represented as a successful search.

### User Story 5 — Discover, inspect, and save a recommended recipe (Priority: P2)

As a person looking for meal ideas, I want to filter and refresh recipe recommendations, inspect ingredients and steps, and save an idea to my recipe book.

**Independent Test**: Change recommendation filters, inspect a recommendation, save it, and verify it becomes a user recipe and is no longer recommended.

**Acceptance Scenarios**

1. **Given** recommendations are available, **When** I set category (all/meals/dinners), maximum preparation time (15/30/45/60 minutes), and complexity (any/easy/medium/advanced), **Then** the displayed suggestions match all filters.
2. **Given** eligible recommendations remain, **When** I refresh, **Then** up to three suggestions are shown, with randomized alternatives where possible.
3. **Given** a suggestion is displayed, **When** I expand it, **Then** I can see its ingredient list, preparation instructions, time, complexity, estimated macros, and a per-serving calorie estimate no greater than 550 kcal.
4. **Given** I save a suggestion, **When** it is added to my recipe book, **Then** its ingredients, instructions, category, preparation time, complexity, and nutrition are retained locally and it is excluded from further suggestions.
5. **Given** no eligible suggestion remains for my filters, **When** I view recommendations, **Then** I see an empty-state explanation and the refresh action is unavailable.
6. **Given** recommendations show estimated values, **When** I use them, **Then** the interface identifies nutrition as an estimate that may vary with ingredients and portions.

### User Story 6 — Plan meals on the monthly calendar (Priority: P2)

As a person organizing meals, I want to assign saved recipes to calendar dates and generate a random weekly or monthly proposal.

**Independent Test**: Navigate months, assign recipes to visible slots, generate a week from a selected date, and verify plans persist and can be edited.

**Acceptance Scenarios**

1. **Given** recipes exist, **When** I open the calendar, **Then** it shows the selected month Monday-first, with controls to move between months and highlight today.
2. **Given** a date and saved recipes, **When** I edit its meal and dinner slots, **Then** any saved recipe can be assigned or cleared; plans persist after closing and reopening the app.
3. **Given** I enable breakfast, **When** I edit a day or generate a menu, **Then** a breakfast slot appears, its calendar assignments persist, and any saved recipe is eligible for breakfast.
4. **Given** at least one recipe is categorized as a meal and one as a dinner, **When** I generate a month or a week containing the chosen date, **Then** the app assigns recipes to each active slot for all target dates; a week is Monday through Sunday.
5. **Given** the target period already has assignments, **When** I generate a menu, **Then** the app asks before replacing those assignments; cancelling preserves the existing plan.
6. **Given** any required meal/dinner category has no recipe, **When** I generate a random menu, **Then** the app explains which category needs a recipe and does not create a partial menu.
7. **Given** no recipes exist, **When** I open the calendar, **Then** the app shows an empty state and disables random-menu generation.
8. **Given** a mobile-sized screen, **When** I select a calendar day, **Then** a day-edit dialog lets me review and change its slots; wider layouts expose slot selectors in the month grid.

### User Story 7 — Track a menstrual cycle and private daily notes (Priority: P2)

As a person tracking menstrual health, I want to record period dates and daily symptoms, and see an explicitly approximate calendar-based phase estimate.

**Independent Test**: Record a past/current period, adjust valid averages, add a daily symptom note, and verify phase/estimate/history output and privacy messaging.

**Acceptance Scenarios**

1. **Given** there is no period start date, **When** I open cycle tracking, **Then** the app shows an unknown phase, asks me to record a start date, and shows no estimated next date.
2. **Given** I record a valid current or past start and optional end date, **When** dates are valid and do not overlap another recorded period, **Then** the period is stored and shown in descending start-date order with its duration when known.
3. **Given** I provide a future or malformed date, an end before its start, or an end in the future, **When** I submit, **Then** the record is rejected with a localized explanation.
4. **Given** an overlapping period is already recorded, **When** I submit a conflicting period, **Then** it is rejected and the existing period remains unchanged.
5. **Given** I enter cycle length from 21–45 days and period length from 2–10 days, with period length less than cycle length, **When** I save, **Then** the updated averages drive the estimate; invalid values are rejected.
6. **Given** a period record has no end date, **When** I select “finalize today,” **Then** today is saved as its end; deletion requires confirmation.
7. **Given** a period start and averages are available, **When** I view the status, **Then** the app shows the cycle day and one of the approximate menstrual, follicular, ovulatory-window, luteal, or possible-late states and the next-start estimate or days late.
8. **Given** I record today's flow, pain from 0–10, mood, and optional note (maximum 160 characters), **When** I save, **Then** the data is kept on this device and appears in a list of up to seven most recent logged days.
9. **Given** a user sees a phase or date estimate, **When** they interpret it, **Then** the app states that it does not confirm ovulation, fertility, or pregnancy and is not professional medical guidance.

### User Story 8 — Analyze a meal photo (Priority: P2)

As a person who has eaten a meal away from home, I want to submit a meal photo for a nutrition estimate and optionally name the dish.

**Independent Test**: Select a supported image, submit it, verify the displayed estimate and diary entry, and test validation/network errors.

**Acceptance Scenarios**

1. **Given** a JPG, PNG, or WebP up to 10 MB, **When** I select it, **Then** I see a preview, may remove it, and can optionally enter a dish name of up to 60 characters in the current client.
2. **Given** an image is selected, **When** I have not pressed analyze, **Then** the photo is not sent for analysis; on explicit submit the app shows a busy state and sends the image plus optional name to Nutria's existing analysis contract.
3. **Given** analysis succeeds with finite, nonnegative calorie and macro values, **When** the response is accepted, **Then** the app adds a photo-sourced meal to today's diary and labels the result as an estimate to verify.
4. **Given** no image, an unsupported media type, an oversized image, an invalid server response, timeout, rate limit, missing server key, or network/provider error, **When** analysis is attempted, **Then** the user receives an actionable error and invalid estimates are not added.
5. **Given** the uploaded image does not clearly show food, **When** the analysis provider returns its documented best-visible-name/zero-value estimate, **Then** the result remains visibly an estimate rather than being presented as verified nutrition.

### User Story 9 — Configure goals and appearance (Priority: P2)

As a person using the app, I want to adjust my daily targets and choose an appearance color that remains selected.

**Independent Test**: Change each daily target and theme, save, reopen the settings, and verify the chosen values and theme are still active.

**Acceptance Scenarios**

1. **Given** the settings dialog is open, **When** I enter valid positive targets for calories, water, protein, carbohydrates, and fat, **Then** the summary uses the new targets and progress values.
2. **Given** I preview a theme but cancel/close settings, **When** the dialog closes, **Then** the last saved theme is restored.
3. **Given** I save one of Verde natural, Azul océano, Lavanda, or Coral, **When** I revisit the app, **Then** that theme remains selected and applied.

### User Story 10 — Review steps from Health Connect (Priority: P1; newly requested)

As an Android user, I want to review my daily steps from Health Connect with clear permission and availability states.

**Independent Test**: Exercise permission granted, denied, unavailable-provider, empty-day, and query-error states on a compatible Android device; confirm displayed values come from Health Connect.

**Acceptance Scenarios**

1. **Given** Health Connect is available but the app has not been granted step/activity read access, **When** I request steps, **Then** Android's runtime permission flow is invoked before data is displayed.
2. **Given** read access is granted and records exist for the local calendar day, **When** I refresh the step summary, **Then** it shows that day's actual Health Connect step count and the data source.
3. **Given** access is denied/revoked, Health Connect is unavailable, the day has no records, or reading fails, **When** I view steps, **Then** the app distinguishes denied, unavailable, no-data, and error states and does not substitute a fabricated count.
4. **Given** I inspect the data disclosure, **When** I grant or use access, **Then** the app explains that step/activity data is read from Health Connect and whether any of it leaves the device; no upload or sharing is implied without an explicit user-facing disclosure and consent.

### User Story 11 — Track a workout with a BLE heart-rate band (Priority: P1; newly requested)

As an Android user with a compatible heart-rate band, I want to explicitly connect it and start/stop a workout that shows only live readings actually received from the band.

**Independent Test**: On a BLE-capable device and standard Heart Rate GATT Service band, verify permission/setup, connect/disconnect, workout start/stop, live HR notifications, and interruptions from Bluetooth or connection loss.

**Acceptance Scenarios**

1. **Given** Bluetooth is available and permitted, **When** I choose connect, **Then** the app searches for and connects to a compatible band exposing the standard Heart Rate GATT service; it does not claim a connection before a compatible device connects.
2. **Given** a compatible band is connected, **When** I start a workout, **Then** the app enters a visible active-workout state and displays heart-rate values only when actual band readings are received.
3. **Given** a workout is active, **When** I stop it or disconnect the band, **Then** the workout ends visibly; the user can explicitly reconnect/start a later session.
4. **Given** Bluetooth is off, the device lacks BLE support, the required permission is denied, the band is unsupported, the connection is lost, or no readings arrive, **When** I attempt to connect or continue a workout, **Then** the app presents the specific state, does not fabricate heart rate or exercise metrics, and offers only valid recovery actions.
5. **Given** the app enters background, returns to foreground, or its process/connection is interrupted, **When** a workout's connection state changes, **Then** the UI reports the interruption and never implies continuous readings for a gap; any persisted workout record accurately marks available readings and interruption/stop state.
6. **Given** I connect a band or start a workout, **When** I read the data disclosure, **Then** the app explains that live heart-rate data is read from the paired BLE device and states where workout/readings are stored and whether any data is transmitted.

### Edge Cases and Observable Failure States

- Corrupt or unavailable browser storage resets the current PWA to defaults; saving failure shows a “could not save on this device” toast. Native migration must not falsely claim successful persistence when local storage fails.
- Water undo is disabled at zero and removes only the most recent water increment. The UI shows at most eight filled glass marks, one per 250 ml, while the numeric amount remains the total.
- Visual progress bars are capped between 0% and 100%; calorie remainder is floored at zero even if intake exceeds its goal.
- Meal and recipe values are estimated/user-entered, not clinically validated. Recommendation and photo nutrition carries estimate cautions.
- Current PWA offers no account, cloud sync, account recovery, notifications, or past-day diary editing. Do not silently add remote storage for menstrual notes or diary records during migration.
- Existing photo endpoint uses an upload cap of 10 MiB plus multipart overhead; the client accepts JPG/PNG/WebP. A photo name is limited to 60 characters by the client and 80 by the endpoint.
- Open Food Facts search may yield an empty product list because products missing required macro/calorie data are omitted.
- Generated menus replace plans only after confirmation and require at least one recipe in each active meal/dinner category.
- Existing cycle information is calendar estimation only and is explicitly not medical advice.
- Android-specific Health Connect and BLE permission/device/lifecycle cases are listed in REQ-021–REQ-026; they must not be hidden behind a generic zero/connected state.

## Functional Requirements

### Existing diary, water, recipes, calendar, cycle, settings, and integrations

- **REQ-001 — Navigation/localization**: The app MUST provide the six existing destinations—Resumen (shown as Inicio on mobile), Comida, Recetas, Foto, Ciclo, and Calendario (shown as Plan on mobile)—with Spanish user-facing labels and date/number formatting; mobile navigation MUST remain usable on narrow screens.
- **REQ-002 — Daily summary and streak**: The app MUST show today's consumed calories, remaining calories, calorie goal, protein/carbohydrate/fat consumed versus goals, water versus goal, meal count, and meal list; it MUST calculate a streak as consecutive calendar days having at least one meal or nonzero water.
- **REQ-003 — Food calculation and diary**: The app MUST accept a food name, positive quantity, and nonnegative nutrition values per 100 g; support grams, ounces converted at 28.3495 g/oz, and portions multiplied by an entered grams-per-portion value; preview and add proportional kcal/protein/carbohydrate/fat to today's diary.
- **REQ-004 — Diary history and deletion**: The app MUST list meals grouped by date for the latest 30 calendar days, allow individual meal removal, and clearly distinguish today's editable diary from prior-day read-only history.
- **REQ-005 — Water logging and undo**: The app MUST provide +150 ml and +250 ml actions, update daily amount/progress and 250-ml glass marks, and undo only the last recorded increment; it MUST disable undo when no water remains to undo.
- **REQ-006 — Clear today**: The app MUST require confirmation before deleting today's meals and water and MUST leave recipes, calendar plans, cycle information, goals, and theme unchanged.
- **REQ-007 — Recipe authoring**: The app MUST let a person create a named meal or dinner recipe with a positive integer serving count and one or more named ingredients, quantities, units, gram-equivalent edible weights, and nutrition per 100 g; it MUST calculate total and per-serving nutrition and prevent saving incomplete recipes.
- **REQ-008 — Recipe lookup**: The app MUST preserve the existing Open Food Facts lookup contract: `GET /api/ingredients/search?query={query}`, trimmed query length 1–80 characters, JSON `{ products: [...] }`, up to eight products with `code`, `name`, `brand`, and `calories`, `protein`, `carbs`, `fat` per 100 g. Selecting a result imports editable nutrient values; users can enter values manually.
- **REQ-009 — Recipe book and diary integration**: The app MUST list saved recipes grouped by meal/dinner with per-serving nutrients, ingredient detail, and any saved instructions; allow adding one serving to today's diary, changing the meal/dinner generation category, and deleting a recipe while removing its calendar assignments.
- **REQ-010 — Recipe recommendations**: The app MUST preserve the current catalog of 11 locally defined recommendations and show up to three unsaved recommendations at a time; users can filter meal/dinner/all, maximum prep time 15/30/45/60 minutes, and complexity any/easy/medium/advanced, refresh suggestions, inspect ingredients/instructions, and save a suggestion. Eligible suggestions MUST not exceed 550 kcal per serving and must display estimates as estimates.
- **REQ-011 — Meal calendar**: The app MUST show a Monday-first monthly calendar with previous/next month navigation, optional breakfast, manual recipe assignment/removal for each active date/slot, and persistent month plans; manual assignment can use any saved recipe regardless of generation category.
- **REQ-012 — Random menu generation**: The app MUST generate a complete menu for the open month or a Monday–Sunday week containing a selected date, using categorized recipes for meal and dinner and any recipe for optional breakfast; it MUST avoid repeating a recipe in the same day when eligible alternatives exist, explain missing required categories, and confirm before replacing existing assignments.
- **REQ-013 — Menstrual period history**: The app MUST let a person record valid current/past period start and optional end dates, correct forgotten start dates by entering the actual date, list periods and known durations, finalize a period today, and delete a record only after confirmation; it MUST reject future/malformed dates, end-before-start, and overlapping periods.
- **REQ-014 — Cycle estimates and configuration**: The app MUST accept an integer cycle average from 21–45 days and period average from 2–10 days, with period length less than cycle length; from the latest valid recorded start, show cycle day, approximate phase (menstrual, follicular, estimated ovulatory window, luteal, possible late), next expected start or late days, and an unknown state if no start is recorded.
- **REQ-015 — Private daily cycle journal**: The app MUST let users record today's flow (none/spotting/light/medium/heavy), pain (0–10), mood (good/stable/tired/sensitive/irritable), and an optional note up to 160 characters; show up to seven most recent daily records and provide a clear on-device privacy statement.
- **REQ-016 — Cycle safety language**: The app MUST label phase/date outputs as estimates and disclose they do not confirm ovulation, fertility, or pregnancy and do not replace professional advice.
- **REQ-017 — Photo-based meal analysis**: The app MUST accept JPG/PNG/WebP images up to 10 MiB and an optional dish name; show a preview/removal action, and submit only after explicit user action; on a valid response add the estimated meal to today's diary and identify the result as approximate.
- **REQ-018 — Photo endpoint contract and failures**: The mobile client MUST retain the existing `POST /api/analyze-meal` multipart contract (`photo` required, `name` optional) and response fields `name`, `calories`, `protein`, `carbs`, `fat`; handle validation, payload-size, rate-limit, provider, configuration, network, invalid-result, and timeout errors without creating an invalid diary entry.
- **REQ-019 — Server-side AI secret boundary**: The OpenAI API key MUST remain in backend configuration and MUST never be stored, bundled, or transmitted from the mobile client; the app MUST explain that the photo is sent to the Nutria service/OpenAI only after analysis is requested.
- **REQ-020 — Settings/theme**: The app MUST allow positive calorie/protein/carbohydrate/fat goals and a water goal of at least 50 ml in 50-ml increments, plus the four existing themes (Verde natural, Azul océano, Lavanda, Coral); saved values and theme MUST remain active after app restart, while cancelling theme changes restores the saved selection.

### Newly requested native Android capabilities (not present in the PWA)

- **REQ-021 — Health Connect permission and actual daily steps**: The Android app MUST request runtime read permission before reading daily step/activity records from Health Connect and MUST identify Health Connect as the data source; it MUST display actual records for the user's local day, not estimate or fabricate a step count.
- **REQ-022 — Honest Health Connect states and disclosure**: The app MUST distinguish permission denied/revoked, provider unavailable, no records for the day, and read failure; each state MUST give an accurate explanation/recovery action. The app MUST disclose what Health Connect data is read, where it is stored, and whether it leaves the device.
- **REQ-023 — Explicit BLE connection**: The Android app MUST allow the user to explicitly connect and disconnect a generic BLE heart-rate band exposing the standard Heart Rate GATT service and MUST distinguish Bluetooth disabled, unsupported BLE, missing/denied permission, incompatible band, connection failure, and lost connection.
- **REQ-024 — Explicit workout and real readings**: The Android app MUST provide explicit workout start/stop controls and display heart rate only from actual readings received while a compatible band is connected; it MUST NOT fabricate heart rate, calories, exercise minutes, or any other metric.
- **REQ-025 — BLE lifecycle integrity**: The Android app MUST visibly represent missing/stale readings and connection loss, including app background/foreground transitions; persisted workout information MUST not imply data continuity for time without readings, and the app MUST provide accurate recovery and stop states.
- **REQ-026 — BLE data disclosure and local privacy**: The app MUST explain that heart-rate data is read from the connected BLE band, state where workout/readings are persisted and whether they are transmitted, and keep diary, recipe, cycle notes, and workout records private/local unless the user receives an explicit disclosure and gives consent for any transfer.

## Existing API Contracts and Observable Service Boundaries

These are migration compatibility requirements, not permission to redesign the server:

| Contract | Request | Successful response | Observable error behavior |
|---|---|---|---|
| `POST /api/analyze-meal` | `multipart/form-data`: required file field `photo`; optional text `name`; JPG/JPEG, PNG, WebP; maximum image 10 MB; endpoint rejects name longer than 80 characters (current PWA field limits it to 60). | JSON fields `name`, `calories`, `protein`, `carbs`, `fat`, where calories are kcal and macros are grams per visible serving. | 400 invalid/missing form or unsupported media/name; 413 oversize; 429 rate limit (10 per IP/min); 502 provider/network/invalid response; 503 key/configuration/provider unavailable; 504 timeout. Errors are Problem Details with localized `title`. |
| `GET /api/ingredients/search?query={query}` | URL-encoded ingredient name; server trims and accepts 1–80 characters; service requests Spanish-localized Open Food Facts results and filters for usable nutrients. | JSON `{ "products": [...] }`, at most 8 products; each has `code`, `name`, `brand`, `calories`, `protein`, `carbs`, `fat`; nutrients are per 100 g. Calories may be derived from kJ if kcal is absent. | 400 invalid query; 429 rate limit (20 per IP/min); 502 upstream/network/invalid JSON; 504 timeout. Empty usable result is a successful empty list. |

The current photo flow sends the image to the Nutria backend only after pressing analyze; the backend then sends it to OpenAI. The API key is configured server-side, not in browser code. The current ingredient flow sends the entered search query to Nutria; the server queries Open Food Facts, and the UI links to Open Food Facts/ODbL and asks users to check package values.

## Data, Privacy, and Current Product Boundaries

- Existing daily foods/water, recipes, menstrual period dates, cycle settings, daily flow/pain/mood/notes, recommendations filters, goals, theme, and calendar plans persist in browser `localStorage` under `nutria.v1`.
- The existing product has no account or cross-device synchronization. It describes cycle records as remaining on the device and shows a separate privacy disclosure before photo upload.
- The PWA has no current Health Connect, pedometer, BLE device, heart-rate stream, workout, exercise-calorie, or fitness-history capability.
- No OpenAI API key may be placed in the Android app. Preserve the existing backend route/request/response contracts unless the architect records evidence that a change is necessary.
- Do not imply that cycle estimates are medical diagnoses or that photo/recommendation nutrition values are verified.

## Key Entities

- **Daily diary**: local date, meals, nutrient amounts, meal source, timestamps, water total and undoable water increments.
- **Food entry**: name, quantity/measurement, calculated kcal/protein/carbohydrate/fat, source (manual, recipe, photo).
- **Recipe**: name, meal/dinner category, serving count, ingredients and their per-100-g nutrition/edible gram weight, calculated recipe totals, optional preparation metadata/instructions.
- **Recommendation**: catalog recipe, category, preparation time, complexity, ingredient nutrition, instructions, estimate status.
- **Calendar plan**: month, date, and meal/dinner/optional breakfast recipe assignments.
- **Menstrual period**: start date and optional end date.
- **Cycle settings and daily journal**: average cycle/period length; local date, flow, pain, mood, private note.
- **Health Connect step record**: local day, actual step total, source/permission/provider state, read time.
- **BLE workout session**: explicit start/stop/interruption state, connected device identity as appropriate, and timestamped received heart-rate readings; no derived metrics without a defined/validated source.
- **Application preferences**: nutrition/water targets, theme, recommendation filters, optional breakfast, calendar month.

## Success Criteria

- **SC-001**: All 20 existing functional requirements (REQ-001–REQ-020) are represented by a native Android user flow and can be tested independently before declaring feature parity.
- **SC-002**: For supported valid food measurements, the preview and stored totals match the specified conversion and per-100-g calculation within 0.1 unit before display rounding.
- **SC-003**: A user can log a supported meal, undo a water entry, and see both changes in the daily summary without losing them after an app restart.
- **SC-004**: A user can create a multi-ingredient recipe, review its per-serving nutrition, assign it manually in the calendar, and generate a Monday–Sunday weekly proposal when required categories exist.
- **SC-005**: On supported Android devices, granted Health Connect permission yields only the actual daily step total; denied, unavailable, empty, and error test cases never display an invented number.
- **SC-006**: On a compatible standard Heart Rate GATT device, every displayed heart-rate sample has a received-device reading; zero readings during an interruption are not represented as continuous data or as a fabricated value.
- **SC-007**: Cycle dates/notes and other personal records remain on-device by default; no account, sync, Health Connect upload, or BLE upload is represented without explicit disclosure and consent.
- **SC-008**: All current Spanish names, key guidance/cautions, validation/error states, and mobile-friendly interaction paths remain available in the Android product.
- **SC-009**: Photo analysis and ingredient lookup retain their existing observable HTTP contracts; the mobile package contains no OpenAI API secret.

## Assumptions

- “Private/local” means user-created diary, recipe, cycle, and workout records are stored on-device by default; any later backup or synchronization would require its own explicit disclosure and consent.
- Health Connect remains the selected source for daily step/activity data; the Android app does not infer steps from a phone sensor if the user's clarified request requires Health Connect.
- Generic heart-rate band interoperability means standard BLE Heart Rate GATT Service support, not vendor-specific fitness metrics.
- The new target is an Android app and may use native Android UI and platform permissions, but implementation technology and architecture are left to the architect.
- This inventory defines required parity, not a medical, nutrition-label, or fitness accuracy certification.

## Upstream Artifacts Consumed

- None — no dependency artifacts were provided. Source files and task input were read directly.

## Evidence Mapping

- `wwwroot/index.html` / `wwwroot/app.js` / `wwwroot/styles.css` → REQ-001–REQ-007 and REQ-009–REQ-020: navigation, controls, calculations, state changes, validation, history, responsive behavior, and privacy text.
- `Endpoints/MealAnalysisEndpoints.cs` / `Services/OpenAiMealAnalysisService.cs` / `Services/IMealAnalysisService.cs` / `Program.cs` → REQ-017–REQ-019 and the `POST /api/analyze-meal` contract: multipart fields, supported formats/limits, response values, error states, and per-IP rate limit.
- `Endpoints/OpenFoodFactsEndpoints.cs` / `Services/OpenFoodFactsService.cs` / `Services/IOpenFoodFactsService.cs` / `OpenFoodFacts.http` → REQ-008 and the `GET /api/ingredients/search` contract: query validation, response shape, Spanish lookup, usable nutrient filtering, and errors.
- `README.md` / `Nutria.csproj` / `wwwroot/manifest.webmanifest` → current PWA target, local persistence, deployment/configuration/privacy claims, Spanish language, and supported product boundaries.
- User clarification in task assignment → REQ-021–REQ-026 and Android migration scope: Health Connect, BLE Heart Rate GATT, explicit workout controls, honest failure/lifecycle behavior, local/private records, and data disclosures.
