# ADR 0003: recetas guardadas en Room cifrada

- **Estado:** Aceptada
- **Fecha:** 2026-11-15

## Contexto

La PWA conserva las recetas en el almacenamiento local del navegador. La app Android necesita guardar ingredientes, cantidades y datos de preparación para que las recetas sobrevivan al cierre de la app y puedan alimentar el diario y, más adelante, el planificador.

## Decisión

- Guardar las recetas exclusivamente en la base Room ya cifrada con SQLCipher; no crear una cuenta ni enviar recetas al backend.
- Separar la receta de sus ingredientes en tablas relacionadas. La clave foránea elimina los ingredientes junto con su receta.
- Guardar cantidades y nutrientes con precisión de milésimas en columnas enteras, igual que el diario. La interfaz convierte a gramos y unidades nutricionales al mostrar o calcular.
- Escribir receta e ingredientes dentro de una transacción para que una edición no deje datos parciales.
- Añadir el esquema con una migración incremental de v6 a v7; no usar recreación destructiva.
- Permitir una consulta explícita de ingredientes a través del servicio Nutria. Seleccionar un resultado rellena solo el ingrediente actualmente elegido; se recomienda revisar los valores del envase.

## Consecuencias

- Las recetas se mantienen en el dispositivo y no se importan automáticamente desde el almacenamiento del navegador.
- La búsqueda comparte únicamente el texto consultado con la API existente; la receta completa permanece local.
- Las asignaciones de calendario aún no se persisten en este bloque. Cuando se migre el calendario, sus filas deberán referenciar la receta con borrado en cascada para conservar la semántica atómica del contrato de repositorio.
