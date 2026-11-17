# ADR-0008: Historial semanal de actividad

- **Estado:** Aceptada
- **Fecha:** 2026-11-15
- **Ámbito:** Selector de actividad, pasos y entrenamientos

## Contexto

La pantalla Actividad mostraba únicamente el agregado de pasos de hoy y una lista limitada de entrenamientos recientes. Se necesita consultar actividad de días anteriores, con navegación semanal y un límite de aproximadamente un mes.

## Decisión

- Añadir a la misma pantalla un selector de semanas de lunes a domingo con flechas anterior/siguiente y selección de un día.
- Permitir fechas desde hoy hasta 29 días antes (30 días naturales contando hoy). Deshabilitar fechas futuras y las anteriores al límite; la primera semana puede ser parcial.
- Consultar para el día seleccionado el agregado de pasos de Health Connect al entrar, cambiar de fecha, volver a primer plano o pedir actualización. No guardar ni importar una copia local de los pasos.
- Mostrar solo los entrenamientos que Nutria registró con una pulsera BLE y guardó en su base Room cifrada, filtrados por la fecha local seleccionada. No importar sesiones de Google Fit ni escribir entrenamientos en Health Connect.
- Mantener separado el control del entrenamiento actual del historial del día seleccionado; cambiar la fecha no altera ni retrofecha una sesión.

## Alternativas consideradas

- Guardar diariamente copias locales de pasos: descartado porque duplicaría datos de salud y contradiría el diseño de lectura temporal de Health Connect.
- Importar entrenamientos de terceros: descartado en este alcance; requeriría permisos y un contrato de datos de Health Connect diferente.
- Mostrar un calendario mensual: descartado a favor del control semanal solicitado, que hace explícita la navegación y el límite de fechas.

## Consecuencias

- Health Connect sigue siendo la fuente de verdad de los pasos. La pantalla informa ausencia de datos y errores, nunca estima un total.
- Las consultas de días previos requieren el permiso de lectura de pasos de Health Connect y respetan el límite visible de 30 días.
- Los entrenamientos se consultan desde Room por fecha local, sin limitar la historia diaria a la cantidad de sesiones recientes del feed general.
- La base de datos no cambia de esquema; no se necesita migración Room.
