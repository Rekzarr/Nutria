# ADR 0005: Recomendaciones de recetas en Android

- Estado: aceptada
- Fecha: 2026-11-15

## Contexto

La PWA ofrece una selección aleatoria de hasta tres recetas sugeridas desde un catálogo incluido con la aplicación. Aplica filtros de categoría, tiempo y complejidad; excluye recetas ya guardadas por identificador o nombre, y limita la selección a 550 kcal por ración. El catálogo y las preferencias no requieren una API.

## Decisión

- Mantener el catálogo como datos estáticos del cliente Android, conservando las 11 recetas, sus ingredientes, nutrientes estimados e instrucciones.
- Reutilizar `RecommendationRules` para filtros, exclusiones, límite de calorías y selección aleatoria; mantener `Random` inyectable en las reglas para pruebas reproducibles.
- Mostrar hasta tres recomendaciones en la sección Recetas y permitir desplegar ingredientes y pasos.
- Al guardar, convertir la recomendación a `SavedRecipe` y persistirla por el repositorio de recetas existente; una recomendación guardada deja de aparecer.
- Guardar categoría, tiempo máximo y complejidad en DataStore, que solo contiene preferencias de interfaz. Los filtros tienen valores predeterminados compatibles con la PWA.
- Tratar cantidades y macronutrientes como estimaciones por ración, no como valores garantizados de producto o consejo nutricional.

## Consecuencias

- Las sugerencias funcionan sin conexión y no envían datos al servidor.
- El catálogo queda duplicado en los clientes web y Android hasta que se decida una fuente compartida; la prueba de catálogo comprueba IDs únicos y el límite de calorías.
- El alta de una sugerencia usa el mismo esquema cifrado Room que una receta creada manualmente.
