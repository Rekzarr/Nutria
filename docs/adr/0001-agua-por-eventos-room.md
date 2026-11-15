# ADR-0001: Registrar las tomas de agua como eventos locales

- **Estado:** Aceptada
- **Fecha:** 2026-11-14
- **Ámbito:** Aplicación Android nativa, registro de agua

## Contexto

La experiencia acordada permite añadir 150 ml o 250 ml y deshacer únicamente la última toma. Guardar solo el total diario no permite identificar de forma fiable qué incremento debe deshacerse después de reiniciar la aplicación. Los registros personales deben permanecer en el almacenamiento local cifrado.

## Decisión

Guardar cada incremento como un evento asociado al día local y a un orden secuencial. Calcular el total diario a partir de esos eventos. La operación «Deshacer» elimina, en una transacción, solo el evento más reciente de ese día. No se envían al backend los datos de agua.

## Consecuencias

- El último incremento puede deshacerse sin reconstruirlo a partir de un total agregado.
- La asignación de secuencia y la eliminación se serializan en la capa de persistencia para evitar que dos operaciones concurrentes deshagan o reordenen eventos de forma incorrecta.
- El historial sigue separado por día local; el límite del historial de comidas no convierte los eventos de agua en datos remotos.
- La compilación de pruebas instrumentadas no equivale a verificar esta decisión en un dispositivo. La verificación en emulador o dispositivo sigue pendiente.

## Evidencia

- UX: `t5-ux.md`, sección «Inicio» — incrementos de +150/+250 ml y deshacer la última toma.
- Requisitos: `t1-pm-spec.md`, REQ-005 — registro de agua y deshacer.
- Implementación: tarea T007, `DiaryLocalStore.kt` y `DiaryLocalStoreInstrumentedTest.kt`.
