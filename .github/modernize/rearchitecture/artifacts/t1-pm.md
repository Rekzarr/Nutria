# t1 — Nutria feature inventory

## Summary

Inspected the complete hand-maintained application source: 16 files across the ASP.NET Core endpoints/services, the Spanish PWA UI and its client behavior, and the project README/contracts. The detailed inventory defines the existing behavior to preserve and distinguishes newly requested Android health/wearable capabilities from what exists today.

## Deliverables

- [t1-pm-spec.md](./t1-pm-spec.md) — product scope, user journeys, 26 testable requirements, current API contracts, data-flow boundaries, and measurable outcomes.
- [t1-pm-requirements-checklist.md](./t1-pm-requirements-checklist.md) — requirements coverage and handoff checklist.

## Upstream Artifacts Consumed

- None — no dependency artifacts were provided. Read the task assignment, board, and project profile.

## Evidence Mapping

- None — no dependency artifacts were provided. Source evidence is cited in the detailed inventory.

## Verification

- Method: read all 16 hand-maintained application files, including all of `wwwroot/app.js`, `wwwroot/index.html`, and the backend endpoint/service source.
- Tests: not run; this is a documentation-only inventory task and the repository contains no test project in the inspected source set.
- The PWA currently has no Health Connect, step-count, BLE, heart-rate, or workout feature. These are new requirements, not existing behavior.
