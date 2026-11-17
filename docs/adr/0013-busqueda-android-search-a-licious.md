# ADR-0013: Búsqueda Android mediante Search-a-licious

- **Estado:** Aceptada
- **Fecha:** 2026-11-17
- **Ámbito:** Búsqueda de ingredientes en Android nativo

## Contexto

La integración Android usaba `https://world.openfoodfacts.org/cgi/search.pl`. Esa búsqueda de texto está heredada y devuelve HTTP 503; la guía oficial de Open Food Facts recomienda Search-a-licious para búsquedas de texto completo. Su endpoint `POST /search` devuelve los productos en `hits`, distinto del formato antiguo `products`.

## Decisión

- Enviar la consulta directamente desde Android a `https://search.openfoodfacts.org/search` mediante `POST`, con `q`, idiomas español e inglés, un máximo de ocho resultados y solo los campos necesarios.
- Leer productos desde `hits`, conservando el filtro de nutrientes completos y la conversión de kJ a kcal. Aceptar `brands` tanto como texto como lista.
- No añadir un backend, cuenta, clave ni almacenamiento de las consultas. La solicitud se hace únicamente cuando la persona pulsa «Buscar».
- No migrar la PWA a esta API por ahora: en la prueba desde `http://localhost:5173`, Search-a-licious rechazó el origen mediante CORS. La PWA sigue usando el endpoint anterior y puede seguir mostrando HTTP 503; la entrada manual de nutrientes permanece disponible.

## Alternativas consideradas

- Mantener `/cgi/search.pl`: descartada porque el endpoint heredado está devolviendo 503.
- Usar `/api/v2/search`: descartada porque la API v2 no admite búsqueda de texto completo.
- Añadir un proxy en Nutria para la PWA: descartado porque requeriría mantener un backend, contrario a la preferencia del proyecto.

## Consecuencias

- La búsqueda nativa no depende del endpoint Perl legado y se mantiene sin coste de hosting de Nutria.
- La búsqueda necesita conexión a Internet y sigue dependiendo de la disponibilidad de Open Food Facts.
- La limitación CORS deja una diferencia temporal entre Android y la PWA hasta que la API permita el origen web o el proyecto decida mantener un proxy.

## Referencias

- [Guía oficial de Search-a-licious](https://openfoodfacts.github.io/search-a-licious/users/ref-openapi/)
- [Búsqueda de productos API v2](https://openfoodfacts.github.io/documentation/docs/Product-Opener/v2/search/get-search/)
