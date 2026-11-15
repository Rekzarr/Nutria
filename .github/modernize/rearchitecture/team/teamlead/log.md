## [t3] Established native Android migration constitution
- Codebase/domain discoveries: The delivered client must be native Android, but the
  current app remains an ASP.NET Core 10.0 host with a Spanish PWA and two frozen API
  seams; localStorage data has no known export/import path. The available host evidence
  does not establish Android build readiness.
- Wrong assumptions and corrections: There is no existing constitution, context.md,
  decisions.md, or teamlead log/inbox. The architect must select the Android language,
  API target, and build tool; this task did not assume Kotlin or a specific SDK version.
- Debugging dead-ends and what actually worked: The task-template path referenced by
  the constitution skill was unavailable in the installed skill set; the available
  plan/spec templates already expose constitution alignment points.
- Techniques/patterns worth reusing for future tasks: Trace guardrails to REQ IDs and
  architecture boundaries; defer unselected versions; treat missing Android SDK,
  Gradle, and ADB as explicit verification blockers rather than inferred success.
- Learnings consumed: none.

## [t7] Defined parity and runtime validation strategy
- Codebase/domain discoveries: t1's complete inventory defines 26 requirements across
  six existing Spanish destinations plus Health Connect/BLE; no legacy tests, Android
  project, or canonical test command exists. The current Android host requirements
  include physical Health Connect/BLE evidence beyond unit or emulator tests.
- Wrong assumptions and corrections: `dotnet test` returned 0 but produced no test
  output; with no test project this is not a baseline pass. Docker and Node/Playwright
  are not prerequisites for native Compose/Room validation, but their absence does not
  excuse skipping Android instrumentation or real-device evidence.
- Debugging dead-ends and what actually worked: environment capability probes and
  source inventory established separate blockers: no Android SDK/Gradle/ADB/emulator;
  .NET 10 runtime absent; Java 25 is not evidence that the t4 JDK 17 baseline is met.
- Techniques/patterns worth reusing for future tasks: map each REQ to named flow
  evidence; separate mocked adapter/state tests from real sensor-device proof; preserve
  local test data isolation and mock external API boundaries.
- Learnings consumed: [teamlead/android-rewrite-guardrails]

## [t8] Created dependency-ordered native Android implementation plan
- Codebase/domain discoveries: t1 defines 26 requirements and t4–t7 provide complete architecture, UX, persistence and validation inputs. There is no project topology/fanout artifact; feature packages and Spanish flows are sufficient to organize 18 plan items and 19 executable tasks.
- Wrong assumptions and corrections: An initial structural assertion counted 17 rather than 18 plan items; the phase headings and checkpoint were corrected before final validation. Python/PyYAML is unavailable, so YAML syntax was not parser-validated.
- Debugging dead-ends and what actually worked: A Python YAML parse command could not launch on this host. PowerShell structural/traceability assertions confirmed exact REQ-001–REQ-026 coverage, plan-to-task coverage, sequential task IDs and links.
- Techniques/patterns worth reusing for future tasks: Put toolchain readiness ahead of native implementation while allowing code progress to remain explicitly unverified if preparation is blocked; separate Health Connect state-machine tests from provider evidence and fake BLE tests from physical-band evidence; keep requirement checkpoints synchronized with task count.
- Learnings consumed: [teamlead/android-rewrite-guardrails, teamlead/native-android-validation-strategy]

## [t9] Reviewed plan coverage and feasibility
- Codebase/domain discoveries: t8's 18 plan items and 19 sequential tasks are structurally covered, but its final requirement table misnumbers all REQ-021–REQ-026 activity items; the upstream spec-to-plan checkpoint has the correct IDs.
- Wrong assumptions and corrections: 100% structural coverage does not establish a passing plan-to-tasks gate; rewrite source annotations and dependency feasibility must be reviewed independently.
- Debugging dead-ends and what actually worked: One PowerShell check had a malformed expression and was discarded; a corrected deterministic script verified 18 items, 19 unique sequential task IDs, all item references, 26 REQ rows, and the checkpoint totals.
- Techniques/patterns worth reusing for future tasks: Compare each REQ row's item IDs to the numbered implementation steps and upstream checkpoint; check that a toolchain blocker does not deadlock source work; bound foundational tasks to one independently executable session.
- Learnings consumed: [teamlead/android-rewrite-guardrails, teamlead/native-android-plan-order, teamlead/native-android-validation-strategy]

## [t9.1] Remediated plan-to-tasks findings
- Codebase/domain discoveries: Activity mappings must align with the explicit steps 3.4 (Health Connect) and 3.5 (BLE); the t1 story IDs place recipes at US3/US4, recommendations at US5, and newly requested activity flows at US10/US11.
- Wrong assumptions and corrections: A prior T001 build requirement was impossible before `android/` and its wrapper existed, and its READY dependency would have blocked source work under the known host limitations. The wrapper/first build now belongs after scaffold in T002; T001 records readiness only.
- Debugging dead-ends and what actually worked: `context.md` and `decisions.md` are absent; the existing constitution/design artifacts and coordinator inbox supply the applicable constraints. PowerShell structural assertions validated updated plan/checkpoint links; no Android build or independent gate rerun was performed.
- Techniques/patterns worth reusing for future tasks: Keep secure DB/key/backup/migration bootstrap small, and let each feature task own its entities/DAOs/migrations/transactions; mark stale gate checkpoints pending rather than retaining a PASS after inputs change.
- Learnings consumed: [teamlead/android-rewrite-guardrails, teamlead/native-android-plan-order, teamlead/native-android-validation-strategy]

## [t9] Reran plan coverage and feasibility gate
- Codebase/domain discoveries: The remediation corrected the six Health Connect/BLE requirement mappings, added legacy source paths to conversion tasks, and kept the toolchain/device blockers separate from source implementation.
- Wrong assumptions and corrections: A full structural count and a PASS for this planning gate do not establish native app implementation, build readiness, sensor integration, or feature parity.
- Debugging dead-ends and what actually worked: An initial PowerShell row-matching expression over-escaped CR/LF characters; using line-oriented matching without that character class produced a deterministic 13-check run with zero failures. YAML parsing and product tests were not performed.
- Techniques/patterns worth reusing for future tasks: Revalidate the actual corrected plan and both checkpoint layers; keep old finding evidence, record the independent rerun separately, and state environmental blockers without turning them into a false implementation pass.
- Learnings consumed: [teamlead/android-rewrite-guardrails, teamlead/native-android-plan-order, teamlead/native-android-validation-strategy]

## [t10] Generated execute and validate DAG
- Codebase/domain discoveries: The approved t8 plan maps 26 requirements into one Android module, shared persistence/domain/API foundations, and seven vertical feature slices. No topology exists; keep the DAG ungrouped.
- Wrong assumptions and corrections: The current plan permits source work when toolchain readiness is blocked, but the DAG catalog requires every target-stack task to depend on target-env-prep and says BLOCKED readiness leaves those tasks not ready. The generated graph follows the DAG hard gate and keeps the requested Android target; the known missing SDK/Gradle/device evidence must not be hidden.
- Debugging dead-ends and what actually worked: `context.md` and `decisions.md` are absent; t9's rerun confirms the corrected plan is PASS. The DAG skill explicitly returns JSON without writing an artifact file.
- Techniques/patterns worth reusing for future tasks: Preserve plan feature dependencies, add CVE remediation because the rewrite creates Android dependency manifests, place architecture/security/UX review and smoke verification before runtime validation, then require physical Health Connect/BLE evidence before parity/conformance sign-off.
- Learnings consumed: [teamlead/android-rewrite-guardrails, teamlead/native-android-plan-order, teamlead/native-android-validation-strategy]

## [t36] Documented portable contributor workflow
- Codebase/domain discoveries: The requested “subagentes” are development collaborators, not Nutria runtime components; the guide describes conceptual responsibilities without binding them to a particular local tool.
- Wrong assumptions and corrections: A structural PASS for this documentation does not validate Android compilation, device integrations, or feature parity.
- Debugging dead-ends and what actually worked: Initial PowerShell heading checks treated CRLF endings as a failed `$` anchor; matching optional carriage returns made the ten checks deterministic.
- Techniques/patterns worth reusing for future tasks: Make task scope, acceptance criteria, dependencies, handoff evidence, and test omissions explicit; parallelize only independent work with separate file ownership.
- Learnings consumed: [teamlead/native-android-validation-strategy]

## [t36] Refined and revalidated the portable contributor guide
- Codebase/domain discoveries: The requested guide should stay focused on collaboration and verification rather than embedding app-specific runtime behavior; conceptual roles and locally available tools must remain distinct.
- Wrong assumptions and corrections: The prior guide was correct about workflow but too tied to Nutria's platform features for a reusable contributor guide; rewrote its examples and validation section to be project-agnostic.
- Debugging dead-ends and what actually worked: The first content checks failed because wrapped Markdown lines split phrases and CRLF affected heading anchors. Normalizing whitespace for prose checks and allowing optional carriage returns made all 12 assertions pass.
- Techniques/patterns worth reusing for future tasks: Use portable role descriptions, observable task boundaries, explicit handoff evidence and environment-aware verification; keep project requirements in project-specific design documents.
- Learnings consumed: [teamlead/android-rewrite-guardrails, teamlead/native-android-plan-order, teamlead/native-android-validation-strategy]
