# ADR 0004: Calendario de comidas nativo

- Estado: aceptada
- Fecha: 2026-11-15

## Contexto

La PWA permite consultar un calendario mensual, editar las recetas de cada día, habilitar un turno de desayuno y generar menús aleatorios para un mes o una semana de lunes a domingo. La base nativa ya guarda recetas en Room cifrada; el plan debe seguir siendo local y las referencias a recetas no deben quedar huérfanas.

## Decisión

- Guardar cada asignación en `calendar_assignments`, con fecha ISO local (`yyyy-MM-dd`), turno y referencia a receta.
- Usar la clave primaria `(local_date, slot)`: puede haber una receta por turno y fecha, pero una misma receta puede elegirse en más de un turno.
- Referenciar `saved_recipes(id)` con `ON DELETE CASCADE`. Eliminar una receta elimina sus asignaciones; editarla actualiza su fila sin reemplazarla para no disparar la cascada.
- Reemplazar un periodo borrando e insertando sus asignaciones dentro de una transacción Room. La operación valida que no haya fechas fuera del intervalo ni turnos duplicados y no toca otros periodos.
- Reutilizar `CalendarRules` para el inicio de semana, categorías, elección aleatoria, control de repetición diaria y confirmación de reemplazo. El desayuno es opcional; al desactivarlo no se muestra en el calendario ni se incluye en nuevos menús.
- Mantener la preferencia del desayuno en DataStore, separada de comidas y planes. Las recetas y asignaciones permanecen en la base cifrada.
- Usar una cuadrícula mensual y un editor por día adaptados a Compose; la edición solo se persiste al confirmar el día.
- Mantener la PWA y sus datos aparte. No se importa automáticamente `localStorage` al almacenamiento Android.

## Consecuencias

- Se añade la migración no destructiva Room 7→8.
- La eliminación en cascada y el reemplazo transaccional mantienen coherentes las recetas y los planes.
- La prueba de persistencia del calendario se compila como prueba instrumentada, pero necesita un dispositivo o emulador para ejecutarse.
- La generación aleatoria no implica recomendaciones nutricionales; reutiliza las categorías asignadas a las recetas y permite cualquier receta en el desayuno.
