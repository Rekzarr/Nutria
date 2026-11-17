# ADR 0006: Resumen diario y racha de cuidado

- Estado: aceptada
- Fecha: 2026-11-15

## Contexto

La PWA muestra el consumo de hoy, el agua respecto al objetivo, las comidas registradas y una racha diaria. La lógica de dominio para calcular la racha ya existía en Android, pero no recibía el historial real del almacenamiento ni se mostraba en Inicio.

## Decisión

- Room agrupa comidas y eventos de agua por fecha y expone únicamente los conteos/totales diarios necesarios para el cálculo, sin cargar todas las comidas históricas al inicio.
- El `DiaryViewModel` combina ese resumen con los datos del día actual y usa `StreakRules`: cuenta hacia atrás desde hoy y corta al primer día sin comidas ni agua.
- Inicio presenta calorías, macronutrientes y agua respecto a sus objetivos con barras limitadas visualmente al 100 %, además de la racha, el número de comidas y su nombre/calorías.
- Los datos diarios siguen en Room cifrada. No se crea una tabla nueva ni se envía el historial a la red.

## Consecuencias

- El resumen continúa reactivo ante cambios guardados en comidas o agua.
- El agregado consulta el historial por fecha, pero devuelve solo una fila por fecha con actividad.
- La prueba de ViewModel con Room se compila como instrumentada; requiere dispositivo/emulador para ejecutarse.
