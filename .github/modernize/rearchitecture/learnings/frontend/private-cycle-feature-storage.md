# Private Cycle Feature Storage

Nutria cycle dates and daily reproductive-health notes belong in encrypted Room and must not enter network, notification, or saved-instance-state flows.

## What Happened

For Nutria task t20 (2026-11-14), the cycle screen reused the pure `CycleRules` model and added a `CycleRepository` adapter over the app's encrypted Room provider. Period dates are strict `dd/MM/uuuu` local dates; the journal keeps the seven newest saved entries and stores notes only after an explicit save. The Compose form uses ordinary in-memory state rather than `rememberSaveable`, so reproductive-health form values are not copied into Activity saved-instance-state bundles. A parallel diary slice had already moved the database from v1 to v2, so the cycle schema was added as v2→v3 while preserving later migrations in the shared chain. In the t20 retry, source review showed the PWA repopulates today's journal form from the saved day record. The native form now waits for its first repository emission, hydrates those ephemeral fields, and only then enables journal controls; a Compose test source checks that editing the note preserves the previously saved flow, pain, and mood.

## Takeaway

Keep cycle rules in pure Kotlin and persist cycle settings, periods, and daily entries only through the encrypted Room repository. Use ISO local-date strings for storage and strict Spanish `dd/MM/uuuu` at the UI boundary. Validate create/correction/finalization against every other period, excluding the record being edited. Never save note text in `rememberSaveable`, notifications, logs, network requests, or settings storage. Hydrate an existing current-day entry from the first bounded repository emission before enabling edit controls; this mirrors PWA behavior while avoiding accidental replacement with defaults. During parallel feature work, inspect the current database version and migration chain before adding a migration; append to the current chain rather than assuming the version from an earlier read.

## History
- 2026-11-14 (Nutria/t20): initial
- 2026-11-14 (Nutria/t20 retry): restore saved today's values before editing; keep sensitive form state ephemeral.
