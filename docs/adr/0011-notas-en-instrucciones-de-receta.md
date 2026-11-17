# ADR-0011: Notas dentro de los pasos de preparación

- **Estado:** Aceptada
- **Fecha:** 2026-11-17
- **Ámbito:** Formulario nativo de creación y edición de recetas

## Contexto

El formulario ya guardaba las instrucciones de cada receta como líneas ordenadas en `SavedRecipe.instructions` y en Room. La etiqueta «Preparación (un paso por línea)» no dejaba claro que se podían documentar observaciones junto a los pasos. En móvil, el rótulo de cada ingrediente compartía fila con dos botones; el ancho restante podía reducirse a una letra por línea.

## Decisión

- Presentar un campo multilínea «Pasos y notas de preparación» con la indicación de escribir una línea por paso o nota.
- Conservar cada línea no vacía, recortada y en orden, en el campo existente `instructions`; al mostrar la receta, sus líneas se enumeran.
- Mantener notas y pasos en el mismo campo: son parte de la secuencia de preparación y no requieren una propiedad nueva ni una migración de base de datos.
- Dar ancho completo al formulario y al rótulo numerado de cada ingrediente; colocar los botones de acción en una fila separada para que no reduzcan el ancho del texto.

## Alternativas consideradas

- Añadir una propiedad `notes` separada: descartada porque duplicaría el contenido de preparación y requeriría cambios en el modelo, la persistencia, la edición y la pantalla de detalle.
- Usar una única descripción sin saltos de línea: descartada porque dificulta seguir pasos individuales y pierde el orden visual.

## Consecuencias

- La persona puede incluir indicaciones como tiempos, temperaturas o variaciones dentro de la secuencia de preparación.
- Las recetas ya guardadas conservan sus instrucciones; no se requiere migración ni se pierde información.
- Las líneas se muestran numeradas, incluidas las notas, para mantener el orden introducido.
