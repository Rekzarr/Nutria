## [t5] Specified native Spanish navigation and feature flows
- Project discoveries: PWA exposes six destinations and client behaviors already enumerated in t1; Activity is a new top-level feature rather than a replacement.
- Decision: phone navigation uses a labeled drawer with all seven destinations; settings stays globally reachable from the app bar. This avoids compressing a seven-destination bottom bar and preserves old labels.
- Cautions: distinguish no Health Connect records from zero, and distinguish BLE connection from explicit workout state; keep gaps/stale readings visible. Photo submission is explicitly user-triggered; cycle estimates remain caveated.
- Corrections: the PM inventory did not establish editing an already saved recipe, so the UX spec does not invent that action; localized recommendation filter labels and listed the existing ingredient units. Python was unavailable on the host, so the final artifact check used PowerShell.
- Missing context files: `context.md`, `decisions.md`, and `team/ux/log.md` were not present at preflight; used task prompt and dependency artifacts as authoritative inputs. No prior UX learnings directory/files existed.
- Learnings consumed: (none)
