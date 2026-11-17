# ADR-0009: Búsqueda directa en Open Food Facts

- **Estado:** Aceptada
- **Fecha:** 2026-11-17
- **Ámbito:** Búsqueda de ingredientes en Android, PWA y backend

## Contexto

La búsqueda de ingredientes dependía del endpoint Nutria `GET /api/ingredients/search`, que solo actuaba como intermediario hacia Open Food Facts. Mantener ese proxy obliga a desplegar y operar el backend incluso para una consulta pública, sin aportar una credencial privada ni una ventaja de coste. El análisis de fotos sí necesita un servidor porque la clave de OpenAI no debe estar en la aplicación móvil ni en el navegador.

## Decisión

- Android y la PWA consultan directamente `https://world.openfoodfacts.org/cgi/search.pl` cuando la persona pulsa el botón de búsqueda.
- El cliente solicita como máximo ocho productos en español y solo conserva resultados con nombre e información nutricional completa. Usa kcal cuando están disponibles o convierte kJ; el usuario debe comprobar siempre el envase.
- Android identifica el cliente con `User-Agent: Nutria/1.0`. La PWA usa `fetch` desde el navegador y no intenta establecer un `User-Agent` personalizado.
- Se retiran del backend el endpoint, el servicio, su registro HTTP y el ejemplo `.http`. El backend conserva únicamente el análisis de fotos en este ámbito.
- Se conserva la atribución a Open Food Facts y a su licencia ODbL en la interfaz web; Android informa del origen y pide verificar los valores.
- La URL de servicio guardada en Android queda asociada al análisis de fotos, no a la búsqueda.
- Esta decisión sustituye el contrato anterior de búsqueda detrás de Nutria documentado en los artefactos de modernización; estos reflejan el diseño previo a esta ADR.

## Alternativas consideradas

- Mantener el proxy de Open Food Facts en Nutria: descartado porque duplica una consulta pública y exige mantener recursos de backend para la búsqueda.
- Eliminar la búsqueda y exigir captura manual: descartado porque se perdería una función útil que puede funcionar sin servidor.
- Enviar fotos directamente a OpenAI desde Android/PWA: descartado porque expondría una clave reutilizable y no ofrece una forma segura de guardarla en un cliente distribuido.

## Consecuencias

- La búsqueda funciona sin desplegar Nutria y no genera peticiones al backend.
- Las búsquedas dependen de la disponibilidad y los límites de Open Food Facts; no son autocompletado y solo se ejecutan por acción explícita.
- La PWA requiere que el navegador permita CORS hacia Open Food Facts. Android y la PWA necesitan conexión a Internet para buscar; el registro manual sigue disponible si falla el servicio.
- El análisis fotográfico sigue necesitando el backend y `OPENAI_API_KEY` del lado servidor. La escala a cero reduce recursos ociosos, pero no garantiza coste cero.
