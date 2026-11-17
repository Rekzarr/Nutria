# ADR-0012: Importación local de recetas JSON

- **Estado:** Aceptada
- **Fecha:** 2026-11-17
- **Ámbito:** Recetas de la aplicación Android nativa

## Contexto

La receta solicitada se compartió mediante capturas y no se pudo leer directamente desde Lenus. Se necesita una forma reproducible de cargarla sin mantener un servidor ni enviar datos personales a terceros. Las recetas existentes ya se almacenan en una base Room cifrada mediante `RecipeRepository`.

## Decisión

- Añadir una importación de un solo archivo JSON mediante el selector de documentos de Android.
- Usar el formato versionado `nutria.recipe.v1`, limitar el archivo a 128 KiB y validar estructura, categoría, ingredientes, cantidades, nutrientes y pasos antes de guardarlo.
- Guardar con `RecipeRepository.saveRecipe`, conservando el cifrado y las transacciones existentes; no escribir directamente en Room ni añadir permisos de almacenamiento.
- Rechazar una receta cuyo nombre ya exista, sin sobrescribir ni crear silenciosamente otra copia.
- No realizar llamadas de red durante la importación. Los enlaces de origen nutricional pueden incluirse en el archivo para consulta, pero no se conservan en el modelo local.

## Formato `nutria.recipe.v1`

- La raíz contiene `format` y `recipe`.
- `recipe` requiere `name`, `category` (`MEAL` o `DINNER`), `servings`, `ingredients` e `instructions`.
- `prepMinutes` y `complexity` son opcionales; la dificultad admite `EASY`, `MEDIUM`, `ADVANCED` o `null`.
- Cada ingrediente requiere `name`, `edibleGrams` y `nutritionPer100g`, con `caloriesKcal`, `proteinGrams`, `carbohydratesGrams` y `fatGrams`.
- Los campos opcionales de atribución o contexto (por ejemplo, `nutritionSource`, `nutritionSourceUrl` o `titleNote`) se ignoran al importar; no se guardan en la base local.
- Las instrucciones son una lista de textos y se conservan en orden. Los valores nutricionales se expresan por 100 g.

## Alternativas consideradas

- Añadir datos a mano desde el formulario: descartada porque es más propensa a errores y no deja un archivo reutilizable.
- Enviar el JSON a un backend: descartada porque la aplicación personal debe funcionar sin hosting ni sincronización.
- Aceptar cualquier JSON: descartada porque formatos ambiguos o incompletos podrían producir datos nutricionales incorrectos.

## Consecuencias

- La importación funciona sin conexión y los datos permanecen en la base cifrada del dispositivo.
- El archivo debe respetar el esquema y los códigos de categoría/dificultad documentados en el parser.
- La validación no certifica que los datos nutricionales sean exactos; las estimaciones deben revisarse contra los envases.
- Las recetas importadas no conservan metadatos de fuente después de guardarse; la atribución puede consultarse en el archivo JSON original.
