# t5 — Spanish native Android navigation and feature flows

## UX direction

The Android client opens on **Inicio**, retains every existing Spanish destination, and adds **Actividad** as a peer destination. On phones, a clearly labeled navigation drawer lists all seven destinations; do not compress seven icons into a crowded bottom bar or bury an existing feature behind an unlabeled “Más” item. Keep the current destination title in the app bar, a visible menu control, and **Ajustes** available from the app bar on every destination. Use the same labels and ordering wherever the drawer is shown.

The experience is Spanish-first and local-first: there is no account, cloud sync, or assumed import from the old browser PWA. Preserve the existing PWA and its local data; do not imply that records have moved to Android. Use familiar native back behavior, with an unsaved-changes confirmation only when leaving a form with entered data. Forms and longer workflows use full screens; use dialogs for consequential confirmations and compact selections, and bottom sheets only for short contextual choices.

## Information architecture

```text
Nutria
├── Inicio       Daily summary, diary snapshot, water, Health Connect step card
├── Comida       Add food, today’s diary, 30-day history
├── Recetas      Recipe book, recommendations, create/edit recipe
├── Foto         Select, preview, confirm and analyze a meal photo
├── Ciclo        Period history, estimates, daily journal
├── Plan         Monday-first monthly calendar and menu generation
├── Actividad    Steps, band connection, active/recent workouts and disclosure
└── Ajustes      Goals, theme, privacy and data-flow explanations, service URL
```

| Destination | Spanish surface | Main actions |
|---|---|---|
| Inicio | `Inicio` | Review intake, goals, water, meals and step status; go to food logging or activity. |
| Comida | `Comida` | Add food; switch between today and recent history; remove a meal or clear today. |
| Recetas | `Recetas` | Switch between `Mis recetas` and `Ideas`; add, inspect or delete recipes. |
| Foto | `Foto` | Choose/replace/remove a photo, enter an optional dish name, then explicitly submit for analysis. |
| Ciclo | `Ciclo` | Review estimates/history, record or correct a period, configure averages, add today’s private note. |
| Plan | `Plan` | Navigate months, select a date, edit meal slots, generate a week or month. |
| Actividad | `Actividad` | Read steps, connect/disconnect a compatible band, start/stop a workout, review actual samples and gaps. |
| Ajustes | `Ajustes` | Edit daily goals and theme; inspect local storage, Health Connect, BLE, photo and ingredient-search data flows. |

The phone drawer must show a text label and a distinct selected state for every destination; menu and selected-state changes are accessible to TalkBack. Opening a destination from Inicio or an action deep-link must not create a duplicate screen in the back stack. Android back first dismisses an open dialog/sheet, then returns from a nested form/detail to its parent, then follows normal destination history.

## Shared layout, content, and interaction rules

- Each screen has one clear title, vertically scrollable content when needed, and a primary action placed consistently at the end of its task (sticky only when it remains reachable and does not cover content). Keep the app bar, drawer, keyboard, and system insets from obscuring inputs or actions.
- Use Spanish labels, accessible names, hints, validation, confirmations, empty states, and error messages. Format dates using the device’s Spanish locale (`dd/MM/yyyy` presentation) and numbers with the Spanish decimal convention; calculate day boundaries using the local calendar day. Do not mix units silently.
- Use persistent inline status/error text for states that require recovery; brief snackbars may confirm routine saves but must not be the only indication of a consequential result. Disable invalid or unavailable actions with a nearby explanation. Keep entered values when a network/platform request fails.
- Do not communicate status by color alone. Maintain readable contrast in all four existing themes, support font scaling and screen readers, give actionable controls at least 48 dp touch targets, and provide logical focus order and visible keyboard focus. After dismissing a dialog, return focus to its opener; announce changed totals and connection/workout state without repeatedly interrupting live readings.
- Confirm destructive operations and explain their exact scope before applying them. A canceled confirmation leaves the original data unchanged. The labels “estimado/a” and equivalent cautions remain adjacent to recommendation, photo, and menstrual-cycle estimates.

## Feature flows

### Inicio — daily overview

1. Show today’s calorie intake, remaining calories (never below zero), calorie goal, protein/carbohydrate/fat versus goals, water amount/progress and meal count/list. Progress indicators cap visually at 100%; the numeric values remain accurate.
2. Show an empty-meals message and the configured default goals on a fresh profile. Offer direct actions to `Añadir comida`, `Añadir agua` and open `Actividad`.
3. Water controls add exactly `+150 ml` or `+250 ml`; show the total and one mark per 250 ml (up to eight marks). `Deshacer` reverses only the last water increment and is disabled when nothing remains to undo.
4. Include the consecutive-day streak using the existing rule (at least one meal or nonzero water). A gap day resets the count.
5. The step card names **Health Connect** as its source and reflects the same honest states as Actividad below. It must not render missing data as `0 pasos`. Tapping the card opens Actividad.
6. `Vaciar hoy` opens a confirmation that explicitly says it removes only today’s meals and water. Recipes, plans, cycle records, goals and theme remain. Cancel leaves all records untouched.

### Comida — entry and history

1. `Añadir comida` opens a labeled form for food name, quantity, unit (`g`, `oz`, `porción`), grams per portion when relevant, and calories/protein/carbohydrates/fat per 100 g.
2. Reject a blank name, nonpositive quantity/portion weight, or negative/incomplete nutrient value inline. Show an amount-based nutrient preview before `Añadir al diario`; convert ounces using 28.3495 g/oz and portions as quantity × grams per portion.
3. On success, add the meal to today and immediately refresh Inicio totals; provide a brief confirmation. Allow removal of an individual meal with confirmation.
4. Show the latest 30 calendar days grouped by date, labeling today/yesterday where appropriate and omitting dates without meals. Today is editable; other dates are visibly read-only, with no misleading add/clear controls.
5. If local persistence fails, show a durable Spanish “No se pudo guardar en este dispositivo” state and do not imply success.

### Recetas — saved recipes, recommendations, authoring

1. Provide obvious `Mis recetas` and `Ideas` sections without replacing either when navigating between them. Saved recipes group by meal/dinner category and expose per-serving nutrition; expanding a recipe reveals ingredients and saved instructions.
2. `Nueva receta` collects name, category (`comida`/`cena`), positive integer servings, ingredients and optional instructions. Each ingredient supports its name, quantity and unit (`g`, `ml`, `unidad`, `cucharada`, `cucharadita`, `pizca`, `al gusto`), edible gram equivalent when its unit does not establish grams, and nutrients per 100 g. Recalculate total and per-serving nutrition as values change; keep `Guardar` disabled until required values are complete.
3. An ingredient lookup field accepts a trimmed 1–80-character query. `Buscar` displays up to eight products, distinguishes a successful empty result from an error, and labels returned nutrition **por 100 g**. Selecting a result fills editable values, not the ingredient’s edible weight. Explain that package values should be checked. On API/network/rate-limit errors preserve the ingredient form and offer retry or manual entry.
4. A saved recipe offers `Añadir una porción a Comida`, category change, and delete. Confirm deletion and explain that its calendar assignments are removed too.
5. `Ideas` exposes Spanish filters for category (`Comidas`, `Cenas`, `Todas`), maximum time (15/30/45/60 min), and complexity (`Cualquiera`, `Sencilla`, `Media`, `Avanzada`), plus refresh. Show at most three eligible, unsaved ideas at a time. Expanded cards show ingredients, instructions, prep time, complexity, and an adjacent estimate disclaimer; save moves the idea into Mis recetas. A no-match state explains the filters and does not offer a nonfunctional refresh.

### Foto — explicit photo-analysis consent

1. Select an image from the device and show its preview, filename if available, `Quitar` and optional dish name. Accept JPG/JPEG, PNG or WebP up to 10 MiB. Keep the existing PWA’s 60-character dish-name limit.
2. Before network transmission, show this concise disclosure: **“Al analizar, se enviarán la foto y el nombre opcional al servicio Nutria para obtener una estimación con OpenAI. La foto no se envía hasta que pulses «Analizar».”** Provide `Cancelar` and an explicit `Analizar` action. Never expose API credentials or imply that a key is required from the user.
3. On submit, lock duplicate submits and show a busy state that can wait for the backend response. Do not automatically retry the cost-bearing request. On success, show the returned values as an estimate and add a photo-sourced meal to today only when all values are valid.
4. On unsupported/oversized/missing image, timeout, network, rate limit, configuration/provider, server validation, or invalid-result failure, explain the state in Spanish, retain the user’s selection where possible, and offer a valid recovery. No error state creates a diary meal.

### Plan — menu calendar

1. Show a Monday-first month grid, current date, previous/next month controls and clear selected-day treatment. Use a day editor on phones rather than squeezing slot controls into each calendar cell.
2. Selecting a day opens its meal and dinner slots (and breakfast only when enabled); each slot can be assigned or cleared using any saved recipe. Show each assignment and serving without requiring the user to infer it from color.
3. Provide `Generar semana` for the Monday–Sunday week containing the selected date and `Generar mes` for the displayed month. Ask for confirmation before replacing any existing assignment; cancellation preserves the current plan.
4. Generation uses meal/dinner recipe categories and any recipe for optional breakfast, avoids a same-day repeat when eligible alternatives exist, and either creates a complete target plan or explains which required category lacks recipes. With no recipes, show a useful empty state and disable generation.

### Ciclo — estimates and private journal

1. If no period start is recorded, display an unknown state, `Registrar inicio` and no next-period date. Otherwise show cycle day, approximate phase and expected next start/late days, each marked as **estimación**.
2. Explain next to the estimate that dates/phases do not confirm ovulation, fertility or pregnancy and are not medical advice. Do not imply diagnosis or certainty.
3. Period history is newest first, includes known duration and offers edit/correct date, `Finalizar hoy` for an open period, and delete after confirmation. Validate local dates and reject future/malformed dates, end-before-start and overlaps with an actionable message.
4. Cycle settings accept cycle average 21–45 days and period average 2–10 days, with period shorter than cycle. Preserve the existing values after invalid input.
5. A clearly private `Registro de hoy` form offers flow, pain 0–10, mood and optional note ≤160 characters; show up to seven recent recorded days. State plainly: **“Tus registros de ciclo y notas se guardan en este dispositivo; Nutria no los envía a un servicio.”** Do not display cycle content in notifications or public summaries.

### Actividad — Health Connect and BLE

Keep the step-reading card and band/workout card visually separate. Explain in plain Spanish that step access is read from Health Connect on this device and is not uploaded by Nutria; a compatible band sends only received heart-rate notifications over Bluetooth to the app, where the workout data is stored locally. No diary, cycle, step or BLE data is sent to the Nutria API. Explain that availability depends on Health Connect/provider/device support.

#### Steps

1. Initial state: `Conectar con Health Connect` or `Permitir acceso a pasos`. Explain the requested step read before opening Android’s permission UI; ask only when the user requests this feature.
2. Granted with records: show the actual local-day total, `Fuente: Health Connect`, and last refresh time; `Actualizar` rechecks permission and records. Refresh on return to foreground as well as on explicit action.
3. Granted with no records: show **“No hay datos de pasos para hoy en Health Connect”**, not zero. Offer `Actualizar` and explain that Nutria cannot create step data.
4. Denied/revoked: show **“Acceso a pasos no concedido”** and `Volver a permitir` / `Abrir ajustes` as appropriate. Do not continue to query as if permission remained granted.
5. Provider missing/outdated: say Health Connect must be installed or updated and provide the platform-appropriate setup route. Query/read failure is an error with retry, not an empty or zero result.

#### Band and workout

1. Disclose the local Bluetooth data flow before the first connection. `Conectar pulsera` is an explicit action; show scanning progress and a cancel action, then let the user select a discovered device. Do not auto-connect or claim compatibility based on its name; confirm the standard service before offering workout start. Request only the Bluetooth permissions needed by the OS, at the point of use.
2. Map outcomes to distinct Spanish states and recovery actions: device lacks BLE support; Bluetooth off (offer Android’s enable/settings action); permission denied (explain and request/open settings); scanning/connection failed (retry); connected band lacks the standard heart-rate service (identify it as incompatible); connection lost (show interruption and allow user-directed reconnect/stop).
3. Once a standard Heart Rate GATT band is confirmed connected, show its connection state and `Iniciar entrenamiento`. Connection alone is not an active workout. The session begins only on this button; a session cannot start when disconnected, incompatible, or permissions are unavailable.
4. During a workout, show elapsed session time and the most recently received real heart-rate value with its timestamp. If no reading arrives, show **“Esperando una lectura de la pulsera”**; if a reading becomes stale, label it stale with time. Never invent heart rate, calories, exercise minutes or workout continuity.
5. `Finalizar entrenamiento` is persistent and explicit. Confirm only if it prevents accidental loss; report the actual saved session and sample count without interpreting gaps as readings. `Desconectar` while active ends the session with a visible interruption/end marker before closing the connection.
6. While backgrounded, use the platform’s ongoing workout notification to identify an active session and provide `Detener`. On foreground return, reconcile current permission/connection state and show any gap or service interruption; do not imply continuous heart rate during a gap. If support is lost (Bluetooth disabled, permission revoked, process/connection failure), surface the concrete reason and safe stop/reconnect option.
7. Recent sessions distinguish actual samples, start/end/interruption times and gaps; empty state says no workouts have been recorded. Do not display calorie or exercise totals that the product does not calculate.

### Ajustes — goals, theme and privacy

1. Keep the existing positive calorie/protein/carbohydrate/fat goals and water goal ≥50 ml in 50-ml increments. Show validation inline; saving applies to Inicio immediately and remains after restart.
2. Show all existing theme names: `Verde natural`, `Azul océano`, `Lavanda`, `Coral`. Previewing a theme is reversible; `Cancelar`/back restores the last saved theme, while `Guardar` persists the selection.
3. Provide a `Privacidad y datos` section that says personal records (food, water, recipes, plans, cycle notes, settings and workout data) stay in app-private local storage; there is no account or sync. Health Connect is read-only for steps; BLE readings are received from the connected band; neither is uploaded. Show photo-analysis and ingredient-search disclosures separately, because those user-triggered requests go to the configured Nutria backend and the photo is then processed through OpenAI.
4. State that existing browser PWA records are **not imported automatically**. Do not claim secure deletion, backup, export, encryption, or transfer behavior until separately designed and verified.
5. If a backend base URL is user-configurable, label it clearly as advanced connection settings; do not request, store or display an OpenAI key.

## UX-to-requirement traceability

| UX flow/section | Requirements |
|---|---|
| Navigation, drawer, Spanish labels and shared interaction | REQ-001 |
| Inicio, daily totals, streak, water, clear-today scope | REQ-002, REQ-005, REQ-006 |
| Comida, conversions, history/editability | REQ-003, REQ-004 |
| Recetas authoring, lookup, saved recipes and recommendations | REQ-007, REQ-008, REQ-009, REQ-010 |
| Plan month/week, slots and replacement behavior | REQ-011, REQ-012 |
| Ciclo history, estimates, journal and caveats | REQ-013, REQ-014, REQ-015, REQ-016 |
| Foto disclosure, selection, submission and errors | REQ-017, REQ-018, REQ-019 |
| Ajustes goals/theme/data-flow disclosures | REQ-020, REQ-022, REQ-026 |
| Actividad steps and states | REQ-021, REQ-022 |
| Actividad band, workout and lifecycle states | REQ-023, REQ-024, REQ-025, REQ-026 |

## Accessibility and localization acceptance checks

- Every screen and drawer entry has a meaningful Spanish TalkBack name; state changes such as permission result, workout start/stop, save and validation failure are announced once and have visible equivalents.
- All seven destinations are reachable from the navigation control on a narrow phone screen without horizontal scrolling or icon-only ambiguity. Keyboard focus can reach every control; dynamic text does not clip the primary action.
- Error text is associated with its field/action; date, numeric and unit fields have labels beyond placeholder text; dangerous actions name the scope of deletion.
- Month headers, selected dates, week boundaries, step-source timestamp, workout elapsed time and sample timestamps are exposed with meaningful screen-reader context.
- Theme previews meet contrast in normal/selected/error/disabled states and never use hue alone to convey category or status.
- Locale presentation must not alter stored quantities or turn an absent sensor record into a numeric zero.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1-pm.md` — inventory index and evidence that this is a Spanish local-first PWA with six destinations and new Android health features.
- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md` — REQ-001–REQ-026, stories, validation, edge/error states and parity behaviors used for flow details and traceability.
- `.github/modernize/rearchitecture/artifacts/constitution.md` — native delivery, PWA retention, frozen API contracts, local privacy and honest platform-data requirements.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — Compose target, seven-feature access requirement, existing API seams, platform permission/lifecycle states and local-only data boundary.
- `.github/modernize/rearchitecture/artifacts/wire_contracts.yaml` — photo and ingredient request/response semantics and provider/key boundary.

## Evidence Mapping

- `t1-pm-spec.md#REQ-001` → Information architecture and drawer acceptance: preserve six Spanish labels/mobile variants and add Activity without displacing them.
- `t1-pm-spec.md#REQ-002–REQ-006` → Inicio and Comida flows: totals, local-day behavior, conversions/history, water undo and isolated clear-today confirmation.
- `t1-pm-spec.md#REQ-007–REQ-012` → Recetas and Plan flows: authoring/lookup/recommendation, meal slots, generation prerequisites and replacement confirmation.
- `t1-pm-spec.md#REQ-013–REQ-016` → Ciclo flow: period validation, estimates, private journal and safety language.
- `t1-pm-spec.md#REQ-017–REQ-019` and `wire_contracts.yaml#native-client-photo-analysis` → Foto explicit consent, supported image/name constraints, errors and no client secret.
- `t1-pm-spec.md#REQ-020` → Ajustes persistence and reversible four-theme preview.
- `t1-pm-spec.md#REQ-021–REQ-022` → Actividad steps permission, truthful empty/error states, source and data disclosure.
- `t1-pm-spec.md#REQ-023–REQ-026` → Actividad BLE connect/workout/lifecycle states, real readings and local-data disclosure.
- `constitution.md#Principles I–V` and `t4-architect.md#Migration and delivery gates` → No WebView substitution, no presumed browser-data import, no health upload, no fabricated sensor metrics and no unsupported verification claim.

## Test Results

- Command: PowerShell assertions for required artifact sections, explicit REQ-001–REQ-026 coverage, upstream references, and deliverable paths.
- Passed: 4.
- Failed: 0.
- Skipped: implementation, Android build, accessibility runtime and device tests; UX specification does not implement the client, and Android toolchain/device availability is outside this task.
