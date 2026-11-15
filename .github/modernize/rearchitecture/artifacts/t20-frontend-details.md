# t20 — Implementation and verification details

## Status

**Complete for T014 and the host verification gate.** The native Spanish Ciclo destination contains period history/correction, date/overlap validation, finalization and confirmed deletion, configurable cycle averages and calendar estimates, and a private daily flow/pain/mood/note journal. Records use the existing SQLCipher-encrypted Room database. Today's saved journal entry is loaded before its editor becomes interactive, preserving existing values when a person returns to edit. This does not claim full app parity or Android device-runtime verification.

## Implementation

- Added the Ciclo Compose screen with unknown-state and estimate presentation, period create/correct/finalize/delete flows, 21–45/2–10-day settings validation, and up to seven recent daily journal records.
- Accepted only strict `dd/MM/aaaa` input; invalid, future, reversed, and overlapping periods receive actionable Spanish errors. Existing records are excluded from overlap checks when correcting/finalizing that same record.
- Kept estimates explicitly approximate and adjacent to the required non-diagnosis, non-fertility/pregnancy, and professional-guidance caveats.
- Added flow, pain 0–10, mood, and optional ≤160-character note entry for today. Notes are not included in Compose saved-instance-state; only an explicit save writes them to the local repository. No notification or API integration was added.
- Reloaded today's persisted values into the ephemeral journal form before enabling its controls; added a Compose test source that edits one field and verifies previously stored fields remain unchanged.
- Added `CycleRoomRepository` and cycle tables with a versioned v2→v3 migration. The existing v1→v2 diary, v3→v4 workout, v4→v5 goals, and v5→v6 scaled-nutrition migrations are preserved in the current v1→v6 chain.
- Connected the destination through the existing `NutriaApplication` shared encrypted database provider. Existing PWA and backend sources/contracts were not changed.
- Added strict Spanish date parser unit coverage, an encrypted-Room persistence/reopen instrumentation test, and Compose tests for the unknown estimate/caveat and saving a period.
- Updated `README.md` in Spanish with the native cycle slice, local encrypted storage, journal restoration behavior, and medical-estimate boundary. PWA documentation and source remain intact.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t19-frontend.md` and `t19.1-frontend.md` — original shared compile/lint blockers and the successful serialized remediation/gate.
- `.github/modernize/rearchitecture/learnings/frontend/private-cycle-feature-storage.md`, `compose-resource-lookups.md`, and `shared-domain-rules-and-repository-contracts.md` — encrypted storage/form-state constraints, Compose localization, and repository boundaries.
- `wwwroot/app.js`, `wwwroot/index.html`, and `wwwroot/styles.css` — cycle status branches, input validation, persistence actions, form defaults, and responsive source presentation.
- `.github/modernize/rearchitecture/artifacts/t1-pm.md` — inventory index and preservation scope.
- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md` — REQ-013–REQ-016 behavior and acceptance scenarios.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — `feature/cycle` boundary, local-date rules, and estimate/data ownership.
- `.github/modernize/rearchitecture/artifacts/t5-ux.md` — Spanish Ciclo flow, privacy disclosure, accessibility, and caveat language.
- `.github/modernize/rearchitecture/artifacts/t6-dba.md` — encrypted cycle tables, note constraints, local-only storage, and Room migration rules.
- `.github/modernize/rearchitecture/artifacts/t7-teamlead.md` — required cycle validation, persistence, UI, and privacy evidence.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md` — Plan 4.3 / T014 scope and traceability.
- `.github/modernize/rearchitecture/artifacts/t10-devops.md` and `checkpoints/t8-environment.md` — JDK 17 / Android SDK activation and separate device-runtime limitation.
- `.github/modernize/rearchitecture/artifacts/t11-frontend.md` — native Compose scaffold and build/test source sets.
- `.github/modernize/rearchitecture/artifacts/t12-dba.md` — encrypted Room/SQLCipher provider and fail-closed persistence foundation.
- `.github/modernize/rearchitecture/artifacts/t13-frontend.md` — Spanish navigation route and shell behavior.
- `.github/modernize/rearchitecture/artifacts/t15-frontend.md` — pure cycle domain rules and `CycleRepository` contract.
- `.github/modernize/rearchitecture/artifacts/constitution.md` — local privacy, estimate caveats, and truthful verification requirements.
- `.github/modernize/rearchitecture/board.md` — task sequencing and concurrently active feature slices.
- `.github/modernize/rearchitecture/context.md` and `decisions.md` were not present; no `*-fanout.md` topology fallback was present.

## Evidence Mapping

- `wwwroot/app.js#getCycleStatus/renderCycle/cycle event handlers` and `wwwroot/index.html#ciclo` → preserves unknown/approximate cycle status, local date boundaries, validation, period/journal presentation, current-day values and explicit local writes; the native form restores saved daily values before allowing edits.
- `t5-ux.md#Ciclo-estimates-and-private-journal` → Compose resources show the nearby estimate disclaimer and private local-storage disclosure; the UI test source covers the unknown state/caveat and today-journal editing path.
- `t1-pm-spec.md#REQ-013` and `#User-Story-7` → `feature/cycle/CycleScreen.kt`: descending history, date correction, finalization, confirmed deletion, and localized rejection for invalid/overlapping ranges; `CycleScreenTest.kt` covers adding a valid record.
- `t1-pm-spec.md#REQ-014` and `t5-ux.md#Ciclo` → `CycleRules` usage and estimate/settings cards: default/updated averages, unknown state, cycle day, expected start/late status, and estimate caveat.
- `t1-pm-spec.md#REQ-015` and `t6-dba.md#Proposed-local-model` → `CycleDayEntity`, `CycleDao`, `CycleRoomRepository`, and `JournalCard`: flow, pain, mood, ≤160-character private note, and seven newest entries; instrumentation test verifies saved values reopen from the encrypted database.
- `t1-pm-spec.md#REQ-016` and `t5-ux.md#Ciclo-estimates-and-private-journal` → `cycle_estimate_disclaimer` adjacent to phase/date results and `cycle_private_disclosure`; Compose assertion checks the safety language.
- `t6-dba.md#Encryption,-key-lifecycle,-and-recovery` and `t12-dba.md#Implementation` → versioned cycle schema is added to the existing SQLCipher-backed Room provider without destructive migration fallback.
- `t15-frontend.md#CycleRepository` → local repository adapter implements the cycle settings, period, and journal contracts; static boundary check found no HTTP/API client reference in the cycle repository.

## Source-Anchored Rewrite

- Source files read: `wwwroot/app.js` (`getCycleStatus`, `renderCycle`, cycle settings/journal/period submit handlers, period shortcut); `wwwroot/index.html#ciclo`; `wwwroot/styles.css` cycle selectors and narrow-screen rules.
- Preserved behavior: unknown state without a period; current-cycle day/ongoing, late, menstrual, follicular, estimated ovulatory-window and luteal branches; expected-start/late presentation; period/journal empty and populated states; default/current-day journal values; seven newest date-bounded journal entries; validated settings; local save side effects; required/optional date handling; future/reversed/overlap rejection; and confirmed period deletion.
- The native slice additionally follows the explicit REQ/UX requirements for correction, finalization, strict Spanish date input, encrypted Room storage, and local-only private records.

## Test Results

- Command: `Set-Location android; .\gradlew.bat clean test build assembleDebugAndroidTest --no-daemon --no-parallel --max-workers=1` (OpenJDK 17.0.16 and Android SDK 36 activated in the same PowerShell process).
- Result: `BUILD SUCCESSFUL`, Gradle exit code 0; debug and release builds, `lint`, `check`, and AndroidTest APK compilation/package completed.
- Passed: 61 JVM unit tests across 14 XML reports, including cycle rule/date-input coverage.
- Failed: 0.
- Skipped: 0 JVM unit tests.
- Lint: 0 errors, 22 warnings, 1 hint; no lint rule was disabled or suppressed for this task.
- Android instrumentation: the APK packaged successfully and all three `CycleScreenTest` methods compiled; 0 methods were executed because `adb devices -l` listed no devices and `emulator -accel-check` returned 6 (hypervisor driver unavailable).
- Instrumented Room encryption/reopen and Compose rendering/interaction therefore remain unverified at runtime on this host.

## Remaining Validation / Handoff

- Run the instrumentation tests on a supported emulator/device to verify Room persistence/reopen and Compose behavior; this host has no device and cannot start its emulator. Device-dependent behavior must remain labeled unverified.
- The UI is Spanish and native, but final accessibility review remains assigned to t29. Cycle estimates are calendar calculations only, not medical predictions.
- No migration/import from browser-local PWA data is implemented or claimed; retain the PWA until parity is established.
