# t9 — Detailed plan coverage and feasibility findings

## Gate Verdict: FAIL

The plan covers all 18 plan items with 19 sequential, uniquely named tasks, and every plan item has at least one task reference. The gate nevertheless fails: the plan's Requirement Mapping table breaks the REQ-to-plan chain for six activity requirements, and three rewrite/conversion tasks omit required `[Source:]` annotations. Two additional HIGH findings prevent reliable execution against the known environment and exceed the one-agent/one-session task boundary.

## Findings

### CRITICAL — Activity requirement mappings point to the wrong plan items

`t8-teamlead-plan.md#Requirement-Mapping` maps the activity requirements to the wrong numbered steps:

| Requirement | Listed plan item(s) | Correct item(s) |
|---|---|---|
| REQ-021 | 3.3, 5.2 | 3.4, 5.2 |
| REQ-022 | 3.3, 4.5, 5.2 | 3.4, 4.5, 5.2 |
| REQ-023 | 3.4, 5.2 | 3.5, 5.2 |
| REQ-024 | 2.1, 3.4, 5.2 | 2.1, 3.5, 5.2 |
| REQ-025 | 2.2, 3.4, 5.2 | 2.2, 3.5, 5.2 |
| REQ-026 | 2.1, 3.4, 4.5, 5.3 | 2.1, 3.5, 4.5, 5.3 |

Plan item 3.3 is recipe authoring/lookup; 3.4 is Health Connect steps; and 3.5 is BLE/workouts. This conflicts with both the plan's own implementation steps and the correct upstream `t8-spec-to-plan.yaml` mappings. It breaks the explicit REQ → Plan → Task evidence chain required by the gate.

**Remediation:** Architect/teamlead correct the six rows in the plan's Requirement Mapping table, then regenerate/recheck the mapping checkpoint.

### CRITICAL — Rewrite tasks omit required source annotations

In rewrite mode, the plan-to-tasks gate requires `[Source:]` annotations on code-conversion tasks. The task lines for T003 (native navigation), T004 (moving persisted records to the new local-store boundary), and T005 (porting existing domain rules) have no source annotation. T007–T010 and T013–T016 do include source annotations; T011/T012 are newly requested Android capabilities and do not have a legacy implementation source.

**Remediation:** Add the specific existing PWA source files and/or authoritative source artifacts to T003–T005. Do not copy source into the plan; identify paths.

### HIGH — Toolchain prerequisite is both impossible to validate at that step and blocks all implementation

Plan Step 1.1/T001 requires a checked-in Gradle wrapper and a full build attempt before Step 1.2/T002 creates the Android project and wrapper. There is no `android/` project yet, so the requested build cannot run at T001. T002 also depends on T001 being READY, while the supplied environment evidence reports no Android SDK, Gradle, ADB, or Android workloads. If preparation remains blocked, the dependency chain therefore prevents the source implementation tasks from starting, despite the plan's prose saying code may continue with verification blocked.

**Remediation:** T001 should capture JDK/SDK/tool availability and its blocker only. Move wrapper creation and the first meaningful Gradle build to T002 or the first post-scaffold validation task. Permit implementation tasks to proceed when toolchain preparation is blocked, while keeping compile, instrumentation, and device evidence explicitly blocked and T017/T018 non-passing.

### HIGH — T004 exceeds a single independently executable task

T004 combines SQLCipher/Room integration, random key generation and Keystore wrapping, fail-closed recovery, backup exclusions, DataStore policy, schema/migrations across diary, water, recipes, calendar, cycle and workout data, transaction invariants, and privacy tests. This is a cross-cutting implementation batch spanning most of the data model, not a bounded one-agent/one-session task. It is also a prerequisite for nearly every feature, creating a substantial schedule and integration bottleneck.

**Remediation:** Keep T004 to a minimal, secure persistence/key/backup foundation and allocate feature-specific entities, DAOs, migrations, and transaction tests to their owning vertical feature tasks (or split T004 into independently verifiable bounded work). Preserve the architecture's encryption and privacy requirements; do not weaken them to reduce scope.

### MEDIUM — Several task story labels do not match the PM specification

Task story annotations drift after the early tasks: T009 is labeled `[US2]` although recipes are US3; T010 `[US2]` although recommendations are US5; T011 `[US3]` although Health Connect is US10; T012 `[US4]` although BLE workouts are US11; T013 `[US5]` although calendar planning is US6; T014 `[US6]` although cycle tracking is US7; T015 `[US7]` although photo analysis is US8; and T016 `[US8]` although settings are US9. T007 combines diary and water behaviors and may retain both US1/US2 labels if clarified.

**Remediation:** Align or remove the `[USx]` annotations so task ownership and acceptance evidence can be navigated from the actual story IDs in `t1-pm-spec.md`.

## Structural Coverage and Feasibility

- 18 plan steps found; all 18 are referenced by at least one task.
- 19 task IDs found; IDs are unique and sequential T001–T019.
- 26 requirement rows found in the plan's Requirement Mapping table.
- The t8 plan-to-tasks checkpoint reports 18/18 (100%) and no orphan tasks; the structural assertions confirm those counts.
- Feasibility is not a full pass: T001/T002 ordering conflicts with the absent Android project/toolchain and blocks code tasks when the known environment blocker persists; T004 is too broad for an independently executable task.
- Constitution ratification remains a disclosed governance TODO from upstream t3/t8; this review does not infer a ratification date or treat the plan date as ratification.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t7-teamlead.md` — verification tiers, exact Android environment blockers, and full-pass conditions.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead.md` — plan/checkpoint summary.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md` — reviewed steps, task dependencies, requirement mapping, task tags and source annotations.
- `.github/modernize/rearchitecture/artifacts/checkpoints/t8-plan-to-tasks.yaml` — existing plan item/task trace.
- `.github/modernize/rearchitecture/artifacts/checkpoints/t8-spec-to-plan.yaml` — upstream REQ mappings used to confirm the incorrect activity item numbers.
- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md` — requirement and user-story IDs.
- `.github/modernize/rearchitecture/artifacts/constitution.md` — implementation and evidence constraints.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — native target and platform boundaries.
- `.github/modernize/rearchitecture/artifacts/t5-ux.md` — destination and user-flow structure.
- `.github/modernize/rearchitecture/artifacts/t6-dba.md` — persistence model and privacy requirements.

## Evidence Mapping

- `t8-teamlead-plan.md#Implementation-Steps-3.3–3.5` and `t8-spec-to-plan.yaml#REQ-021–REQ-026` → correct plan-item IDs in the CRITICAL mapping table above.
- `t8-teamlead-plan.md#Task-Breakdown-T001–T005` → missing source annotations and toolchain/task-scope findings.
- `t8-teamlead-plan.md#Task-Breakdown-T007–T016` and `t1-pm-spec.md#User-Stories-1–11` → story-label drift.
- `t7-teamlead.md#Environment-Requirements-and-Observed-Availability` → environment premise used for feasibility assessment.
- `constitution.md#Principle-V` → traceability and honest blocked verification requirements.

## Test Results

- Command: PowerShell structural assertions against `t8-teamlead-plan.md` and `t8-plan-to-tasks.yaml`.
- Passed: 6 assertions — 18 plan items, 19 unique sequential task IDs, all plan items task-referenced, 26 requirement rows, and checkpoint coverage totals.
- Failed: 0 structural assertions.
- Skipped: YAML parser validation; no parser was available/used. Android build, app tests, and device validation are outside this review and remain blocked per the upstream environment evidence.
