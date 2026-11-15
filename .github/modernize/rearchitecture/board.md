## User Input

> Implement the user's clarified request for the current workspace at D:\Proyectos\Nutri. The user explicitly chose to replace the PWA with a native Android app and confirmed that ALL existing Nutria capabilities must be preserved, not just fitness. They also selected Android Health Connect for steps/exercise data and a generic BLE heart-rate compatible band for workout tracking.
>
> Acceptance criteria:
> - Deliver a native Android implementation, not a browser-only mock/placeholder. Preserve the existing functionality: food/nutrition diary and macros/calorie goals, water logging/undo, recipes and recipe recommendations, meal calendar/menu planning, menstrual-cycle tracking and private notes, photo-based meal analysis, Open Food Facts ingredient lookup, and settings/theme. Keep the existing backend API/service contracts for photo analysis and food lookup unless a well-evidenced necessary change is made; never expose the OpenAI API key in the mobile client.
> - Add a meaningful daily step counter sourced from Android Health Connect with runtime permission requests and honest empty/unavailable/error states. Add BLE connection to a band exposing the standard Heart Rate GATT service, with explicit connect/disconnect and workout start/stop, real HR readings while connected, and no fabricated metrics. Clearly handle unsupported devices, permissions, Bluetooth-off, lost connections, and background/foreground lifecycle. Persist user records locally/private; disclose the Health Connect and BLE data flows.
> - Preserve user-visible behavior and Spanish/localization from the existing PWA. Avoid unrelated server changes and do not delete the existing implementation until parity is established; user's replacement intent means the Android app must be the delivered app target, not that source files must be destructively removed.
> - First inspect the whole current app, then produce architecture/migration artifacts if required by the workflow, implement the native app, and verify as much as this host supports. Existing repo has ASP.NET Core 10 PWA, no Android project; the environment check found only .NET SDK 11.0.100, no installed dotnet workloads, no ANDROID_HOME/ANDROID_SDK_ROOT, no gradle command, but Java 25 is installed. Do not claim Android compilation/runtime verification if the toolchain is unavailable. Try an appropriate minimal validation or explicitly report the blocker.
>
> This is a very broad rewrite. Use the coordinator/worker workflow and split it into safe, verifiable phases if needed. Do not misrepresent a WebView or a partial shell as a full native migration. If full parity cannot be delivered now, provide the strongest working implementation possible, identify concrete uncovered features and blockers, and do not report completion. Do not ask further questions; user has answered platform, wearable compatibility and parity scope.

**Project started**: 2026-11-14T17:33:41Z

## User Follow-up

> Nueva aceptación del usuario para el mismo trabajo: mantén también documentación del proyecto en español, con un nivel técnico accesible para alguien que no conoce Android. Incluye una guía de orientación (qué partes tiene la app y cómo se relacionan), requisitos y pasos de instalación/ejecución, configuración del backend/API existente, flujo de permisos/datos de Health Connect y Bluetooth BLE, pruebas en dispositivo y resolución de problemas. Mantén README o crea docs si procede, y actualiza la documentación cuando cambies arquitectura/comportamiento; evita documentación de planificación innecesaria y no afirmes como verificados pasos que el entorno no permita ejecutar.
>
> Nueva aceptación documental: registra también las decisiones de diseño en español. Usa ADRs breves (contexto/problema, decisión, alternativas consideradas, consecuencias/limitaciones y estado/fecha), incluyendo las decisiones ya confirmadas por el usuario: reemplazar la PWA por app Android nativa, preservar toda la funcionalidad existente, usar Health Connect para pasos/actividad y BLE estándar de frecuencia cardiaca para pulseras. Añade decisiones adicionales solo cuando estén respaldadas por el análisis/implementación, y actualízalas si cambian; evita presentar opciones tentativas como decisiones aprobadas.
>
> Aclaración del usuario: por «subagentes» se refiere a los usados durante el desarrollo (no agentes dentro de Nutria). Añade documentación en español sobre el flujo de trabajo: roles funcionales (coordinación, análisis, implementación, validación/revisión), qué tipo de tareas asignar, cómo delimitar/entregar una tarea, handoff de resultados, cuándo paralelizar y cómo verificar cambios/tests. Manténlo como guía portable del proyecto, no documentes identificadores internos, prompts privados ni configuración del entorno del asistente; distingue roles conceptuales de herramientas disponibles localmente.

## Tasks

### Phase: Analysis
- ✅ t1 [pm] Inventory all current Nutria features and observable behavior (17:35Z→17:39Z, 3m 34s)
- ✅ t2 [architect] Analyze current app architecture, APIs, and migration constraints (17:35Z→17:45Z, 9m 28s)

### Phase: Foundation
- ✅ t3 [teamlead] Establish native Android migration constitution and guardrails (17:47Z→17:49Z, 2m) [deps: t1, t2]

### Phase: Design
- ✅ t4 [architect] Design the native Android target architecture and preserved API contracts (17:50Z→17:54Z, 3m 29s) [deps: t1, t2, t3]
- ✅ t5 [ux] Specify Spanish native Android navigation and feature flows (17:55Z→17:58Z, 2m 22s) [deps: t1, t3, t4]
- ✅ t6 [dba] Design private local persistence and Health Connect/BLE data boundaries (17:55Z→18:01Z, 5m 46s) [deps: t1, t2, t3, t4]
- ✅ t7 [teamlead] Define parity, environment-aware runtime test strategy (17:55Z→17:59Z, 3m 56s) [deps: t1, t2, t3, t4]

### Phase: Plan
- ✅ t8 [teamlead] Create dependency-ordered feature implementation plan (18:02Z→18:09Z, 6m 30s) [deps: t4, t5, t6, t7]
- ✅ t9 [teamlead] Validate implementation plan coverage and feasibility (18:22Z→18:26Z, 4m 18s) [deps: t7, t8, t9.1]
- ✅ t9.1 [teamlead] Remediate plan-to-tasks findings before re-running the gate (18:15Z→18:20Z, 4m 47s) [evidence: t9-teamlead-validation.md]

### Phase: Target Environment Preparation
- ✅ t10 [devops] Prepare and verify the Android target toolchain (18:35Z→18:54Z, 18m 49s)

### Phase: Native Scaffold
- ✅ t11 [frontend] Scaffold the native Android app (18:55Z→19:05Z, 9m 43s) [deps: t10]

### Phase: Shared App Foundations
- ✅ t12 [dba] Establish encrypted private persistence (19:38Z→19:42Z, 4m 20s) [deps: t4, t6, t10, t11, t12.1, t14]
- ✅ t13 [frontend] Implement the Spanish navigation shell (19:43Z→19:48Z, 4m 37s) [deps: t4, t5, t10, t11, t12, t12.1, t14]
- ✅ t14 [frontend] Implement the typed Nutria API client (19:08Z→19:28Z, 20m 3s) [deps: t2, t4, t10, t11]
- ✅ t12.1 [frontend] Fix HTTP MockWebServer HTTPS test setup and verify the shared Gradle suite (19:32Z→19:36Z, 3m 58s) [deps: t10, t11, t14]

### Phase: Contributor Workflow Guide
- ✅ t36 [teamlead] Document a portable development workflow in Spanish (19:53Z→19:54Z, 1m 41s) [deps: t1, t3, t8, t9]

### Phase: Domain and Settings Foundations
- ✅ t15 [frontend] Implement shared domain rules and repository contracts (19:56Z→20:08Z, 11m 47s) [deps: t1, t4, t6, t7, t10, t11, t12, t14]

### Phase: Priority Feature Slices
- ✅ t16 [frontend] Implement diary, food entry, history and water (20:53Z→21:06Z, 12m 34s) [deps: t1, t4, t5, t7, t10, t11, t12, t13, t15, t19.1]
- ✅ t18 [frontend] Implement Health Connect steps (21:07Z→21:16Z, 8m 8s) [deps: t1, t4, t5, t6, t7, t10, t11, t13, t15, t16, t19.1]
- ✅ t19 [frontend] Implement BLE connection and explicit workouts (21:16Z→21:30Z, 12m 38s) [deps: t1, t4, t5, t6, t7, t10, t11, t12, t13, t15, t18, t19.1]
- 🔄 t20 [frontend] Implement cycle tracking and private journal (dispatched 21:31Z) [deps: t1, t4, t5, t6, t7, t10, t11, t12, t13, t15, t19, t19.1]
- ❌ failed[findings] t22 [frontend] Implement goals, themes and privacy disclosures [deps: t1, t4, t5, t6, t7, t10, t11, t12, t13, t15, t20, t19.1]
- ✅ t19.1 [frontend] Resolve shared Android lint blockers and verify a clean full Gradle suite (20:44Z→20:52Z, 8m 1s) [deps: t10, t11, t14, t15]

### Phase: Feature Integrations
- ⏳ t17 [frontend] Implement recipe authoring, lookup and diary integration [deps: t10, t11, t12, t13, t14, t15, t16]
- ⏳ t21 [frontend] Implement explicit photo meal analysis [deps: t10, t11, t12, t13, t14, t15, t16]
- ⏳ t23 [frontend] Implement daily summary, streak and clear-today [deps: t10, t11, t16, t18]

### Phase: Recipe-dependent Integrations
- ⏳ t24 [frontend] Implement recipe recommendations [deps: t10, t11, t17]
- ⏳ t25 [frontend] Implement meal calendar and menu generation [deps: t10, t11, t17]

### Phase: Dependency Security
- ⏳ t26 [frontend] Scan and remediate Android dependency vulnerabilities via skill(cve-remediation) [deps: t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22, t23, t24, t25]

### Phase: Project Documentation
- ⏳ t34 [architect] Record approved architecture decisions in Spanish ADRs [deps: t4, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22, t23, t24, t25, t26]
- ⏳ t35 [devops] Write accessible Spanish project, operations and contributor documentation [deps: t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22, t23, t24, t25, t26, t36]

### Phase: Implementation Review and Smoke Test
- ⏳ t27 [architect] Review architecture and verify full build/startup [deps: t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22, t23, t24, t25, t26, t34, t35]
- ⏳ t28 [security] Audit application security and data boundaries [deps: t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22, t23, t24, t25, t26, t34, t35]
- ⏳ t29 [ux] Review Spanish UX and accessibility [deps: t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22, t23, t24, t25, t26, t34, t35]

### Phase: Runtime Validation
- ⏳ t30 [tester] Run full Android runtime and parity validation [deps: t7, t10, t11, t16, t17, t18, t19, t20, t21, t22, t23, t24, t25, t26, t27, t28, t29, t34, t35]

### Phase: Platform Device Validation
- ⏳ t31 [tester] Validate Health Connect and BLE on real devices [deps: t10, t11, t18, t19, t30, t34, t35]

### Phase: Feature Parity Sign-off
- ⏳ t32 [pm] Sign off feature parity against all requirements [deps: t1, t10, t30, t31, t34, t35]

### Phase: Conformance Review
- ⏳ t33 [teamlead] Review conformance and issue the final verdict [deps: t7, t10, t30, t31, t32]
