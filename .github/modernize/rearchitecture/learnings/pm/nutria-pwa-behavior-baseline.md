# Nutria PWA behavior baseline

Nutria's PWA keeps diary, recipe, menstrual, and calendar records in browser-local storage and currently has no fitness/Health Connect/BLE capability.

## What Happened

For project Nutria task t1, the full hand-maintained ASP.NET Core/PWA source was inspected. The UI behaviors live mostly in `wwwroot/app.js`; the exact Spanish controls are in `wwwroot/index.html`; API behavior is defined in two minimal endpoint files and their corresponding services. Recommendations are bundled local data, and meal/dinner category is used by random menu generation rather than manual calendar assignment. There is no Android source or existing step/workout integration in the PWA.

## Takeaway

For Nutria parity work, read the HTML, JavaScript, endpoint, and service together. Keep the two existing API contracts stable. Mark Health Connect and standard BLE heart-rate features as new product scope, preserve local/private storage for personal records, and do not overstate the current product as having fitness support.

## History
- 2026-11-14 (Nutria/t1): initial
