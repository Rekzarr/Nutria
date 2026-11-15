# Native Android Plan Ordering

For Nutria's native rewrite, sequence toolchain readiness and privacy-safe foundations before vertical feature slices, and keep real platform/device evidence as a separate final gate.

## What Happened

Task t9 verified full structural coverage but found incorrect activity requirement item IDs, missing source annotations, and a dependency that could block all source work when Android tooling remained unavailable. Task t9.1 corrected the trace and kept the shared encrypted-storage/key/backup/migration bootstrap small; feature entities, DAOs, migrations, and transaction tests now live with the owning vertical slices. The plan explicitly treats toolchain readiness as validation evidence rather than a source-work prerequisite.

Task t8 planned Nutria's 26-requirement PWA-to-Android rewrite from the t1 inventory and t4–t7 architecture, UX, data and validation artifacts. The plan uses one native app module, shared persistence/domain/API foundations, then independent feature slices and a separate parity/device sign-off. The missing Android SDK/Gradle/device evidence remains a blocker, not a basis for a WebView substitution or inferred pass. An initial item-count check was corrected from 17 to 18.

## Takeaway

Compare each requirement table's plan-item IDs against the numbered implementation steps and upstream trace checkpoint; a 100% coverage percentage alone does not prove a correct chain. Do not make source implementation depend on a toolchain being ready when verification can remain explicitly blocked.

When no project topology exists, organize by named user-facing feature modules and do not invent G-group labels. Keep each REQ mapped through plan items to tasks; treat fakes as state-machine evidence only, and reserve Health Connect provider and physical BLE behavior for actual device validation. Track Android toolchain preparation separately from source work so unavailable native verification remains explicit. Keep the shared persistence task to secure database/key/backup plumbing; let feature tasks own their schema and transaction tests so the data-layer foundation does not become an oversized single-session bottleneck.

## History
- 2026-11-14 (Nutria/t8): initial
- 2026-11-14 (Nutria/t9): added traceability and blocker-order review findings
- 2026-11-14 (Nutria/t9.1): bounded shared persistence foundation and assigned schemas to vertical feature tasks
