# Android Rewrite Guardrails

For Nutria's native rewrite, retain the current backend/API and PWA until parity is proven.

## What Happened

Task t3 created the initial migration constitution for Nutria. The requirements demand a
native Android client with full PWA parity, while retaining the photo-analysis and food
lookup contracts, local/private user records, and truthful Health Connect/BLE behavior.
No Android project or verified Android toolchain is present.

## Takeaway

Trace Android implementation and parity evidence to t1 requirements. Do not assume a
language, Android API level, or storage library before architecture/data design. Never
claim a build/device result unless the Android SDK and required tooling were actually
used. Keep browser-local records' migration implications explicit and preserve the
OpenAI credential boundary on the server.

## History
- 2026-11-14 (Nutria/t3): initial
