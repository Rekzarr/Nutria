# Android navigation drawer for seven Nutria destinations

For Nutria's phone UX, a labeled drawer keeps the six existing Spanish destinations plus Actividad accessible without overcrowding or scrolling a seven-item bottom bar.

## What Happened
During project Nutria task t5, the native app needed to retain Inicio, Comida, Recetas, Foto, Ciclo and Plan while adding Actividad. A persistent labeled drawer makes all seven destinations discoverable on narrow phones; Ajustes remains a global app-bar action.

## Takeaway
When a mobile rewrite adds a top-level feature beyond an already dense navigation set, preserve original destination labels and expose the new destination as a peer. Do not silently hide existing flows behind an unlabeled “More” item or shrink a crowded bottom navigation.

## History
- 2026-11-14 (Nutria/t5): initial
