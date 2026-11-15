# Settings Storage and Theme Preview

Nutria goals are sensitive local records in encrypted Room; display theme and service URL are non-health DataStore preferences, and theme preview must remain reversible until explicit save.

## What Happened

For Nutria task t22, the more specific DBA contract resolved a potential ambiguity in the plan: calorie/macronutrient/water goals are stored in the SQLCipher-backed Room database, while theme and service base URL use the settings preference repository. The settings screen previews themes immediately, restores the stored selection on cancel/back, and saves only after explicit confirmation. The Coral accent was adjusted to `#B45846` so white foreground contrast exceeds 4.5:1.

## Takeaway

Keep personal goals out of preference storage; add them as feature-owned encrypted Room data and append a non-destructive schema migration. Keep only non-health display/connection preferences in DataStore. For reversible settings, distinguish selected preview state from saved state and restore the saved value on cancel/disposal. Validate service URLs before persistence and construct the existing typed client from that setting; never expose or store a provider API key in the app.

## History
- 2026-11-14 (Nutria/t22): initial
