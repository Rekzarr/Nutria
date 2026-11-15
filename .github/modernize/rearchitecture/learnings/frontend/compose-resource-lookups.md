# Compose Resource Lookups

Use Compose-aware string resource resolution in UI and resolve strings before entering callbacks that are not composable.

## What Happened

In Nutria task t19.1 (2026-11-14), Android lint reported 67 `LocalContextGetResourceValueCall` errors in cycle and workout Compose screens. Replacing `LocalContext.current.getString()` with `stringResource` fixed UI reads; strings needed later in coroutine/event callbacks were first resolved during composition. Workout state-message helpers became composable, and the accessibility description was resolved before the semantics lambda.

## Takeaway

Prefer `stringResource(...)` for strings rendered by Compose. If a click handler, coroutine, or non-composable DSL lambda needs localized text, resolve the value in the composable scope and capture that value. Keep resource-based state-message helpers composable rather than hiding `Context.getString()` behind a non-composable helper.

## History
- 2026-11-14 (Nutria/t19.1): initial
