# Diary water event storage

Store water increments as individually ordered local events when users must undo only the latest entry.

## What Happened

For Nutria t16, the native diary needed +150/+250 ml actions and undo of only the latest increment for a local day. A cumulative daily total cannot identify the increment to reverse, so the Room model stores each event with a day key and sequence; add/undo operations serialize sequence allocation and removal. The diary uses the existing encrypted Room provider and explicit migration chain. Lifecycle 2.11.0 required compile SDK 37, so the app kept compile SDK 36 and used Lifecycle 2.10.0.

## Takeaway

Represent undoable additions as event rows, derive the total, and perform ordering/removal transactionally. Keep the date key local and test the day boundary and undo isolation. Check AndroidX dependency metadata against the project's pinned `compileSdk` before accepting a new version.

## History

- 2026-11-14 (Nutria/t16): initial
