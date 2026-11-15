# t14 — Typed Nutria API client

## Summary

Implemented the native Android typed client for only the two frozen Nutria backend routes. Ingredient lookup uses the existing UTF-8 query contract; photo analysis sends the selected image only when called, with bounded multipart input, Spanish error mapping, and no automatic retry. The client does not persist payloads, call OpenAI/Open Food Facts directly, or contain provider credentials.

## Deliverables

- [Batch report](./t14-frontend-batch-report.yaml) — implementation file inventory, API contract traceability, test evidence, and known validation boundary.
- [Task checkpoint](./checkpoints/t14-tasks-to-impl.yaml) — T006-to-source and requirement trace.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t2-architect.md` — current client/backend boundary and preserved PWA/API scope.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — native data/remote package boundary, HTTPS/base URL rule, timeout and photo error policy.
- `.github/modernize/rearchitecture/artifacts/wire_contracts.yaml` — frozen photo-analysis and ingredient-search paths, fields, limits, and error contracts.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md#Step-2.3` and `#T006` — typed API client plus local HTTP contract-test scope.
- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md#REQ-008` and `#REQ-017–REQ-019` — lookup, explicit photo-analysis call, error, and server-only secret requirements.
- `.github/modernize/rearchitecture/artifacts/t10-devops.md` and `.github/modernize/rearchitecture/artifacts/checkpoints/t8-environment.md` — JDK 17 / Android SDK activation and emulator limitation.
- `.github/modernize/rearchitecture/artifacts/t11-frontend.md` — native project package and build configuration baseline.
- `.github/modernize/rearchitecture/learnings/frontend/android-native-scaffold-build-baseline.md` — same-shell JDK 17 activation and pinned wrapper usage.

## Evidence Mapping

- `wire_contracts.yaml#native-client-ingredient-search` and `t8-teamlead-plan.md#T006` → `NutriaApiClient.searchIngredients`, `IngredientProduct`, and UTF-8/query/empty/error assertions in `NutriaApiClientTest`.
- `wire_contracts.yaml#native-client-photo-analysis` and `t4-architect.md#Nutria-API—frozen-server-seam` → `NutriaApiClient.analyzeMeal`, multipart/media/size/name validation, finite nonnegative response checks, no-retry behavior, and error mapping tests.
- `t1-pm-spec.md#REQ-019` and `wire_contracts.yaml#server-to-openai-responses` → no provider URL/key or personal-record fields in the client; request assertions check the expected photo/name parts and absence of authorization or unrelated records.
- `t10-devops.md#Target-Environment-Preparation` → `test build assembleDebugAndroidTest` completed using the documented JDK 17 and Android SDK environment.

## Test Results

- Command: `cd android; .\gradlew.bat test build assembleDebugAndroidTest --no-daemon`
- Passed: 19 unit tests (10 API-client contract tests plus navigation, scaffold, and migration tests); Android debug/release build, lint/check, and instrumentation APK compilation/package.
- Failed: 0.
- Skipped: 0 unit tests.
- Instrumentation runtime: not run; the prepared host still has no connected device and emulator acceleration is unavailable per t10.
- Network test scope: local MockWebServer only; no live OpenAI, Open Food Facts, or production Nutria request was made.

## Notes

- Added the Android `INTERNET` permission and pinned OkHttp, Gson, and coroutines dependencies; release URLs must be HTTPS. HTTP is accepted only when explicitly enabled for a loopback debug host.
- Server `ProblemDetails.title` is preserved when present; Spanish fallbacks distinguish invalid input, oversized images, rate limits, upstream/provider errors, service unavailability, timeouts, network failures, invalid payloads, and other HTTP errors.
- The shared checkpoint `.github/modernize/rearchitecture/artifacts/checkpoints/tasks-to-impl.yaml` still contains an earlier T004 snapshot that marks these API tests failed; its owner/coordinator was notified that it is stale. This task's current evidence is in the task-scoped checkpoint linked above. No `board.md` or other task-owned status was edited.
- Gradle reports the existing `android.disallowKotlinSourceSets=false` migration option as experimental; the standard project build passed with the shared setting.
