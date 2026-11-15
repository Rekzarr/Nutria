# Existing technology stack

## Server

- **Framework/runtime target:** ASP.NET Core Web SDK, `net10.0`, declared in `Nutria.csproj`.
- **Language:** C# with nullable reference types, implicit usings, and warnings treated as errors.
- **HTTP style:** ASP.NET Core minimal API route extensions; no MVC controllers were found.
- **DI/networking:** framework dependency injection and typed `HttpClient` registrations.
- **OpenAPI:** `Microsoft.AspNetCore.OpenApi` 10.0.0 and `Microsoft.OpenApi` 2.7.5; the OpenAPI document route is mapped only in Development.
- **External integrations:** OpenAI Responses API and Open Food Facts public search API.
- **Server persistence/authentication:** none found. User records are not persisted by the server; no user authentication or authorization middleware is configured.

## Browser client

- Plain HTML, CSS, and JavaScript; no `package.json`, JavaScript framework, package manager, or client build step found.
- Uses browser APIs including `localStorage`, `fetch`, `FormData`, `FileReader`, native `<dialog>`, `structuredClone`, `crypto.randomUUID`, and browser history.
- Spanish (`es-ES`) formatting and Spanish UI text are embedded in the client.
- `manifest.webmanifest` requests standalone display and portrait orientation. No service-worker code/registration was found, so offline asset caching is not evidenced.
- Google Fonts are requested remotely by the HTML; the system font fallbacks remain in CSS.

## Runtime configuration and limits

- `OPENAI_API_KEY` is read by the server-side analysis service; it is not part of the browser request contract.
- `OPENAI_MODEL` is optional and defaults to `gpt-4.1-mini`.
- Analysis `HttpClient` timeout is 90 seconds; ingredient lookup timeout is 12 seconds.
- Request/form body ceiling is 10 MiB plus 64 KiB; the photo file limit is 10 MiB.
- Fixed-window rate limits are 10 analysis requests/minute/IP and 20 ingredient searches/minute/IP.
- Project target is .NET 10. The inspected host has .NET SDK 11.0.100 only. Android-related host evidence: Java 25.0.5 is installed; no `ANDROID_HOME` or `ANDROID_SDK_ROOT`, Gradle, or `adb` command is available; `dotnet workload list` reports no installed workloads. These facts do not establish an Android build toolchain.

## Not present in the current stack

No Android project, Android SDK/NDK, Gradle wrapper, Health Connect integration, Bluetooth LE code, Room/SQLite database, server-side user database, synchronization service, or client test/build manifest was found in the inspected source inventory.
