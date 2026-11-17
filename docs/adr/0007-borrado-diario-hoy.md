# ADR 0007: Borrado confirmado del diario de hoy

- Estado: aceptada
- Fecha: 2026-11-15

## Contexto

El requisito REQ-006 pide poder borrar las comidas y el agua del día actual con confirmación. El repositorio ya ofrece una eliminación atómica acotada a una fecha.

## Decisión

- La acción se presenta en «Agua de hoy», accesible desde «Inicio», y solo se habilita cuando hay comidas o agua registradas y el almacenamiento está disponible.
- Antes de borrar, se muestra un diálogo que explica qué se eliminará, que la acción no se puede deshacer y que recetas, planes y otros días no se modificarán.
- La interfaz invoca `DiaryRepository.clearMealsAndWater` para la fecha actual. Room elimina comidas y eventos de agua dentro de una única transacción.
- Los errores se presentan en el estado del diario y no se informa de éxito si la transacción falla.

## Consecuencias

- Se puede vaciar el diario del día sin borrar objetivos, recetas, planificaciones ni datos históricos.
- La prueba instrumentada cubre cancelar, confirmar y conservar datos de ayer; requiere dispositivo/emulador para ejecutarse.
