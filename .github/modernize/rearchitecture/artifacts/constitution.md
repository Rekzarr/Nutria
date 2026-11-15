<!--
Sync Impact Report
- Version change: no prior constitution → 1.0.0 (initial project constitution).
- Modified principles: none; this is the first constitution.
- Added sections: five core principles, target-stack guardrails, additional constraints,
  delivery and verification, and governance.
- Removed sections: none.
- Templates requiring updates:
  - creating-implementation-plan/templates/plan-template.md — ✅ reviewed; its Constitution
    Check already provides an alignment gate.
  - feature-inventory/templates/spec-template.md — ✅ reviewed; its governed-by reference
    already points to constitution.md and the existing requirements match this scope.
  - breaking-down-tasks/templates/tasks-template.md — ⚠ pending; this skill/template was
    not available in the installed skill set.
  - Relevant workflow SKILL.md files — ✅ reviewed; no generic guidance changes required.
- Follow-up TODO: RATIFICATION_DATE is unknown; coordinator/project owner must set it when
  the constitution is formally ratified. Android implementation/tool versions remain for
  the architect to select and verify in t4.
-->
# Nutria Constitution

## Migration Mode

**Mode**: REWRITE

**Justification**: Health Connect and standard BLE heart-rate access require a native
Android runtime; a browser PWA or WebView shell cannot satisfy the clarified target.
Implement the Android client in a new `android/` root while retaining the existing
ASP.NET Core service contracts and PWA sources until functional parity and data-transfer
implications have been reviewed.

## Core Principles

### I. Native Android Delivery and Complete Parity

The delivered user-facing target MUST be a real native Android application; a WebView,
browser-only interface, or partial shell MUST NOT be represented as the completed
migration. The app MUST preserve every existing capability in t1's REQ-001–REQ-020,
including the Spanish nutrition diary, macros and goals, water logging and undo, recipes
and recommendations, calendar/menu planning, cycle records and private notes, photo
analysis, ingredient lookup, settings, and themes. It MUST also implement the new
Android capabilities in REQ-021–REQ-026. The existing PWA MUST remain intact until
parity is demonstrated and any local-data transfer implications are addressed.

### II. Frozen Backend Contracts and Server-Side Secrets

The Android client MUST use the existing `POST /api/analyze-meal` and
`GET /api/ingredients/search` contracts documented in `wire_contracts.yaml`.
Implementations MUST preserve request/response semantics, supported inputs, and
user-visible validation/error behavior. Backend changes are out of scope unless the
architect records concrete evidence that a narrow change is necessary and the
coordinator approves it. The OpenAI API key MUST remain in backend configuration and
MUST NOT be bundled, stored, or sent by the mobile app; meal photos go to the server
only after an explicit user action.

### III. Private Local Records and Disclosed Data Flows

Diary entries, water events, recipes, calendar plans, cycle records and notes, goals,
preferences, and workout records MUST remain local/private by default. The migration
MUST NOT silently add accounts, synchronization, uploads, backups, or remote storage,
or imply that browser-local records have automatically transferred to Android.
Before use, the app MUST clearly disclose what data Health Connect and the BLE band
provide, what is stored, and whether anything leaves the device. Any future transfer
or sharing requires an explicit user-facing disclosure and consent.

### IV. Health and Wearable Data Must Be Truthful

Daily steps MUST come from actual Health Connect records for the user's local day, with
runtime permission handling and distinct denied/revoked, unavailable, no-data, and
read-error states. Heart rate MUST come only from received readings from a connected
compatible band exposing the standard Heart Rate GATT service. Connection, workout,
and disconnect controls MUST be explicit. The app MUST distinguish unsupported
hardware, Bluetooth-off, permission denial, incompatible devices, connection loss, and
missing/stale readings. It MUST NOT fabricate steps, heart rate, calories, exercise
minutes, or continuity across an interruption. Background/foreground transitions and
recovery/stop states MUST be visible. Cycle phases/dates and nutrition analyses MUST
remain clearly identified as estimates, not diagnoses or verified values.

### V. Incremental, Requirement-Traceable Quality Gates

Implementation MUST proceed in independently verifiable feature slices, with every
plan item and implementation task traced to one or more REQ identifiers. Each slice
MUST preserve the corresponding existing observable behavior and Spanish/localized
guidance. Tests MUST cover normal paths, persistence, invalid input, errors, permission
and device states, and contract boundaries relevant to the slice. Android build,
emulator, and device results MUST be reported only when actually run. The current
host evidence is limited to .NET SDK 11.0.100 and Java 25.0.5; Android SDK, Gradle,
and ADB are absent, so no Android compilation or runtime result may be inferred until
the toolchain is prepared.

## Target Technology Stack

| Component | Target Version | Notes |
|-----------|---------------|-------|
| Client platform | Native Android; exact OS/API target pending t4 | Native UI and Android platform integrations are mandatory; WebView-only is prohibited. |
| Client language/framework | Pending architect decision in t4 | Select only after evaluating Android platform/API compatibility and toolchain availability. |
| Build tool | Pending architect decision in t4 | Provide a reproducible build configuration and wrapper where supported. |
| Local persistence | Private on-device storage; technology pending t6 | Preserve personal records locally; do not introduce server persistence or sync by implication. |
| Existing backend | ASP.NET Core 10.0 | Preserve the existing photo-analysis and ingredient-search routes unless a documented, approved necessity is established. |
| Current host evidence | .NET SDK 11.0.100; Java 25.0.5 | No Android SDK, Gradle, or ADB was found; these installed versions do not prove Android build readiness. |

## Additional Constraints

- The PWA's six destinations and mobile labels, Spanish text, number/date formatting,
  estimates and cautions, and observable validation/error states are part of parity.
- No source file under `wwwroot/` or existing backend implementation may be removed
  merely to make the Android app the delivery target.
- The discovered browser `nutria.v1` data has no established export/import path.
  Migration MUST NOT claim automatic transfer or silently discard the original data.
- Health Connect and BLE integrations MUST request only needed access and MUST explain
  their local data flow. No remote storage of health data is permitted without a
  separate explicit consent and approved design.
- Cycle tracking MUST retain the existing calendar-estimate caveat and MUST NOT
  present phases as confirmation of ovulation, fertility, pregnancy, or medical advice.
- Exact Android framework, API levels, persistence library, and build commands are
  intentionally deferred to the architect and data-design tasks; this constitution
  does not invent versions not selected by the user or verified by the environment.

## Delivery and Verification

- The architect's design MUST respect this constitution and the frozen boundaries in
  the t2 migration and wire-contract artifacts.
- The implementation plan MUST include all 26 requirements and identify dependencies,
  parity evidence, testing levels, host/device prerequisites, and honest fallback limits.
- Native compilation and platform integration require a prepared Android toolchain.
  When unavailable, the responsible task MUST record the exact blocker and MUST NOT
  mark Android build or runtime verification as passing.
- Health Connect and BLE require compatible-device validation for permission,
  unavailable/empty/error states, real readings, disconnection, and lifecycle gaps.
  Mocks may support automated tests but MUST NOT be reported as real sensor data.
- The PWA may be considered for later retirement only after the parity gate passes and
  its browser-local data-transfer implications have been explicitly reviewed.

## Governance

This constitution governs migration planning and implementation guardrails. It MUST be
checked during plan review and implementation conformance review. Every violation is
CRITICAL and blocks the affected gate; a missing design, implementation, or validation
artifact is not evidence of compliance. Amendments require a written rationale, owner
approval, an ISO-date update, and a semantic-version increment: MAJOR for incompatible
principle changes, MINOR for added or materially expanded principles, and PATCH for
clarifications that do not change obligations. The coordinator MUST communicate
approved changes to affected roles. Explicit user acceptance requirements remain the
product scope authority; technical decisions may refine but MUST NOT weaken them.

**Version**: 1.0.0 | **Ratified**: TODO(RATIFICATION_DATE): not supplied | **Last Amended**: 2026-11-14

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1-pm.md` — inventory index for the
  requirements and parity baseline.
- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md` — REQ-001–REQ-026,
  observable behavior, boundaries, and success criteria.
- `.github/modernize/rearchitecture/artifacts/t1-pm-requirements-checklist.md` —
  confirmation that all existing and new capability areas are represented.
- `.github/modernize/rearchitecture/artifacts/t2-architect.md` — architecture
  inventory index and Android migration constraints.
- `.github/modernize/rearchitecture/artifacts/migration_boundary.yaml` — new Android
  client scope, preserved backend/PWA, and phased migration rules.
- `.github/modernize/rearchitecture/artifacts/wire_contracts.yaml` — frozen photo and
  ingredient lookup routes and server-side provider/secret boundaries.
- `.github/modernize/rearchitecture/artifacts/tech-stack.md` — observed stack and
  verified host toolchain limitations.
- `.github/modernize/rearchitecture/artifacts/data-model.md` — current browser-local
  records and absence of an established transfer path.
- `README.md` and `Nutria.csproj` — current Spanish PWA description and ASP.NET Core
  10.0 service target.

## Evidence Mapping

- `t1-pm-spec.md#REQ-001–REQ-020` → Principle I and V; all existing user-visible
  capabilities remain required parity scope.
- `t1-pm-spec.md#REQ-021–REQ-026` → Principles III and IV; Health Connect, BLE,
  lifecycle, permission, and disclosure requirements.
- `t2 migration_boundary.yaml#implementation_rule` → Principles I and V; create a
  native Android target while preserving the existing backend and PWA during parity.
- `t2 wire_contracts.yaml#native-client-photo-analysis` and
  `#native-client-ingredient-search` → Principle II; preserve both client API seams.
- `t2 wire_contracts.yaml#server-to-openai-responses` → Principle II; keep the API
  key and provider call server-side.
- `t2 tech-stack.md#Runtime configuration and limits` → Target Technology Stack and
  Principle V; record actual host limitations and prohibit inferred Android build
  success.
- `t2 data-model.md#Persistence boundary` → Principle III and Additional Constraints;
  no automatic migration or remote persistence is established.

## Test Results

- Command: PowerShell validation of required headings, version/date fields, upstream
  references, and unresolved template placeholders (executed after writing).
- Passed: 5 checks.
- Failed: 0.
- Skipped: 0 product tests; this task changes governance documentation only.
