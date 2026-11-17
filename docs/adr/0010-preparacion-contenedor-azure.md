# ADR-0010: Preparar el backend como contenedor, sin desplegarlo

- **Estado:** Aceptada
- **Fecha:** 2026-11-17
- **Ámbito:** Empaquetado y despliegue futuro del backend de análisis fotográfico

## Contexto

La búsqueda de Open Food Facts se ha trasladado a los clientes. El único flujo que conserva dependencia de Nutria es el análisis de fotos, que requiere mantener la clave de OpenAI fuera del APK y del navegador. Se busca poder desplegarlo en una plataforma con escala a cero, sin mantener un servidor permanentemente encendido ni crear recursos desde esta tarea.

## Decisión

- Añadir una imagen Docker multi-stage basada en .NET 10, ejecutada como usuario no root en el puerto 8080.
- Documentar Container Apps Consumption como una opción futura de despliegue con mínimo de réplicas cero, dejando claro que no garantiza coste cero.
- No crear ni desplegar recursos de Azure.
- No publicar el endpoint de fotos mientras carezca de autenticación y cuotas por usuario. Un secreto estático dentro de la app móvil no se considera autenticación suficiente.

## Alternativas consideradas

- Mantener el backend activo todo el tiempo: descartado por el objetivo de reducir costes cuando la app no se usa.
- Incluir `OPENAI_API_KEY` en el cliente móvil: descartado porque el APK puede inspeccionarse y reutilizarse.
- Desplegar inmediatamente el endpoint anónimo con límite por IP: descartado porque un tercero podría consumir el presupuesto de OpenAI.

## Consecuencias

- El contenedor puede compilarse y ejecutarse localmente; el análisis de fotos no queda disponible sin configurar `OPENAI_API_KEY`.
- Antes de un ingreso público se necesitan autenticación adecuada a clientes móviles, cuotas por identidad y revisión de precios/alertas.
- La PWA y Android pueden seguir utilizando búsqueda de ingredientes sin desplegar este contenedor.
