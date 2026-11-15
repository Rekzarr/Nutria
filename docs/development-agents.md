# Guía de subagentes para el desarrollo de Nutria

Esta guía describe roles de trabajo que pueden realizarse con subagentes o por una persona. No son funciones de Nutria ni dependen de una herramienta concreta.

## Roles

| Rol | Responsabilidad | Entregable esperado |
|---|---|---|
| Coordinación | Aclarar el objetivo, dividir el trabajo, fijar dependencias y asignar propietarios de archivos. | Tareas acotadas, orden de ejecución y resumen de estado. |
| Análisis | Leer la implementación actual, identificar contratos, casos límite y funciones que deben conservarse. | Evidencia con rutas de archivos, contratos y dudas/bloqueos. |
| Diseño | Proponer navegación, modelos y límites entre interfaz, lógica y almacenamiento. No cambia código salvo que se le asigne explícitamente. | Decisiones registradas en un ADR cuando afecten arquitectura o comportamiento. |
| Implementación | Resolver una tarea concreta en sus archivos asignados, siguiendo los contratos existentes. | Cambios de código, pruebas enfocadas y lista de limitaciones. |
| Validación | Ejecutar las comprobaciones pertinentes en un entorno disponible y contrastar el resultado con los requisitos. | Comandos ejecutados, resultados y condiciones que no pudieron probarse. |
| Revisión | Revisar cambios de otros con foco en errores funcionales, seguridad, privacidad y compatibilidad. | Hallazgos accionables con archivo y evidencia; no reescribe el alcance por su cuenta. |

## Flujo recomendado

1. Expresar el resultado observable, el alcance, las exclusiones y la evidencia de aceptación.
2. Analizar contratos y estado existente antes de diseñar o editar. Consultar el README y los ADR aplicables.
3. Dividir por límites funcionales y dependencias reales. Solo paralelizar tareas que no editen los mismos archivos ni dependan de resultados aún no disponibles.
4. Asignar a cada implementador una tarea, archivos permitidos, comportamiento que conservar, restricciones y comandos de validación.
5. Entregar resultados con rutas cambiadas, decisiones, pruebas ejecutadas y no ejecutadas, bloqueos y riesgos abiertos.
6. Integrar en orden de dependencia; resolver conflictos de propiedad de archivos en coordinación, no sobrescribiendo cambios ajenos.
7. Validar el conjunto integrado y actualizar README/ADR cuando cambien instrucciones de uso, comportamiento o decisiones aceptadas.

## Cuándo no delegar

- Búsquedas puntuales, cambios pequeños de un solo archivo y tareas que dependen de una traza continua del mismo flujo suelen ser más eficientes de resolver de forma directa.
- No crear tareas paralelas “por si acaso”, no pedir revisiones redundantes y no presentar una propuesta de diseño como una decisión aceptada.

## Evidencia y límites

- El rol de validación no debe declarar un build o una prueba como correctos si no se ejecutaron.
- Distinguir las pruebas JVM, las pruebas instrumentadas, el build del APK y la comprobación en un teléfono; no son equivalentes.
- Para Health Connect y BLE, registrar modelo/versión del dispositivo, permisos concedidos, pulsera utilizada y resultado observado, evitando incluir identificadores personales innecesarios.
- Las herramientas, modelos e identificadores de agentes dependen del entorno de desarrollo y no forman parte del contrato del proyecto.
