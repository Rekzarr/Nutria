# t20 — Ciclo y registro privado

## Summary

The native Spanish cycle slice is implemented and its serialized host build/test gate passes. It includes validated period history, configurable calendar estimates, and a private daily journal backed by encrypted Room. Existing entries for today are reloaded before the journal editor becomes active, so editing does not silently replace saved values. This completes T014 only; it is not an app-parity or device-validation verdict.

## Deliverables

- [t20-frontend-details.md](./t20-frontend-details.md) — implementation, source anchoring, verification evidence, and remaining device limitation.
- [t20-frontend-batch-report.yaml](./t20-frontend-batch-report.yaml) — structured task/requirement traceability and test results.
- [checkpoints/tasks-to-impl.yaml](./checkpoints/tasks-to-impl.yaml) — updated shared implementation checkpoint with T014 complete.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md` — REQ-013–REQ-016 behavior and safety acceptance criteria.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md`, `t5-ux.md`, `t6-dba.md`, and `t7-teamlead.md` — cycle architecture, Spanish flows, privacy boundary, and validation expectations.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md` — Plan 4.3 / T014 task scope and traceability.
- `.github/modernize/rearchitecture/artifacts/t10-devops.md` and `checkpoints/t8-environment.md` — JDK/SDK activation and unavailable device/emulator evidence.
- `.github/modernize/rearchitecture/artifacts/t11-frontend.md`, `t12-dba.md`, `t13-frontend.md`, `t15-frontend.md`, `t19-frontend.md`, and `t19.1-frontend.md` — native scaffold, encrypted Room, navigation/domain contracts, earlier verification blocker, and serialized lint/build remediation.
- `.github/modernize/rearchitecture/artifacts/constitution.md` and `board.md` — migration/privacy guardrails and task sequencing.
- `.github/modernize/rearchitecture/learnings/frontend/private-cycle-feature-storage.md`, `compose-resource-lookups.md`, and `shared-domain-rules-and-repository-contracts.md` — sensitive form-state boundary, Compose localization, and repository/domain conventions.
- `wwwroot/app.js`, `wwwroot/index.html`, and `wwwroot/styles.css` — source behavior and cycle form/history/estimate flows.
- `context.md` and `decisions.md` were absent; no `*-fanout.md` topology fallback was present.

## Evidence Mapping

- `t1-pm-spec.md#REQ-013–REQ-016` and `t8-teamlead-plan.md#Plan-4.3/T014` → `CycleScreen.kt`, `CycleRoomRepository.kt`, cycle entities/migration, and parser/domain tests; exact host evidence is in [t20-frontend-details.md](./t20-frontend-details.md#test-results).
- `t5-ux.md#Ciclo-estimates-and-private-journal` and `t6-dba.md#Proposed-local-model` → Spanish estimate caveat, local-only encrypted records, strict note boundary, recent-entry limit, and journal restoration covered by the new Compose test source.
- `t19.1-frontend.md#Test-Results` → serialized clean Gradle run now passes; emulator/device runtime remains unavailable and is not claimed.
