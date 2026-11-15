# Guía portable de trabajo para personas y subagentes colaboradores

## Propósito

Esta guía describe un flujo de trabajo de desarrollo que puede aplicar un
equipo de personas, subagentes u otras herramientas disponibles localmente. Los
roles son responsabilidades conceptuales: no presuponen una plataforma,
proveedor, configuración o herramienta determinada. Cada equipo debe aclarar
qué herramientas y capacidades tiene a su disposición; ninguna herramienta
sustituye los criterios de aceptación ni la evidencia.

## Responsabilidades funcionales

| Rol conceptual | Responsabilidad | Entrega habitual |
|---|---|---|
| **Coordinación** | Aclara el objetivo y las prioridades, asigna responsables, registra dependencias, resuelve conflictos y mantiene visible el estado del trabajo. | Tareas acotadas, orden de trabajo, decisiones aprobadas y estado integrado. |
| **Análisis** | Examina el código y la documentación pertinentes; distingue observaciones verificables de inferencias, preguntas y limitaciones. | Alcance afectado, contratos o comportamientos relevantes, riesgos con evidencia y dependencias. |
| **Implementación** | Realiza el cambio asignado dentro de sus límites, respeta las decisiones vigentes y deja el resultado listo para revisar. | Cambios identificables y resumen de su relación con los criterios de aceptación. |
| **Validación y revisión** | Contrasta el resultado con los criterios, inspecciona el cambio y ejecuta comprobaciones adecuadas; informa también lo que no pudo validar. | Evidencia reproducible, defectos concretos, límites de cobertura y veredicto claro. |

Una persona puede asumir varios roles si el equipo es pequeño. Para cambios de
riesgo o revisiones importantes, conviene que alguien distinto compruebe el
resultado cuando sea posible. Los roles no implican permisos especiales ni
requieren que el equipo use subagentes.

## Seleccionar y delimitar tareas

Antes de iniciar una tarea, quien la coordina deja claros:

1. **Resultado esperado:** qué cambio o evidencia se entregará.
2. **Límites:** qué incluye y qué queda expresamente fuera.
3. **Criterios de aceptación:** condiciones observables para considerar el
   resultado correcto, incluidos los errores o estados vacíos importantes.
4. **Contexto y dependencias:** archivos, contratos, decisiones vigentes y
   resultados previos que se deben consultar.
5. **Restricciones:** límites de datos, compatibilidad o cambios permitidos.
6. **Validación y entrega:** comprobaciones esperadas y formato o ubicación del
   resultado.

Una tarea debe poder ejecutarse y revisarse como una unidad coherente. Si reúne
resultados independientes, afecta a varias áreas sin una frontera clara o exige
decisiones todavía pendientes, se divide y se declaran las dependencias entre
las partes. Cuando sea posible, se asigna por flujo funcional completo y
verificable, en vez de separar cambios que solo tienen sentido al integrarse.

Quien recibe la tarea lee sus referencias y confirma el alcance antes de
ampliarlo. Si descubre un conflicto, una dependencia ausente o una ambigüedad
que cambia el resultado, lo comunica a coordinación. Puede avanzar en partes
independientes si eso no compromete la integración.

## Entrega y handoff

Quien implementa acompaña el cambio con un resumen que indique:

- qué cambió y dónde;
- qué criterio de aceptación cubre cada parte relevante;
- decisiones nuevas y su justificación, distinguiéndolas de las ya aprobadas;
- comandos exactos ejecutados y resultados de pruebas o comprobaciones;
- fallos, pruebas omitidas o validaciones no disponibles, con su causa;
- riesgos, dependencias pendientes y siguiente paso necesario.

Quien valida registra evidencia que otra persona pueda repetir y clasifica cada
comprobación como **aprobada**, **fallida** o **no ejecutada**. Una comprobación
no ejecutada no equivale a una aprobada. Ante un fallo, describe el escenario
mínimo para reproducirlo, el resultado esperado y el observado, y el área que
debe atenderlo; no lo oculta bajo una descripción genérica.

Coordinación integra las entregas, revisa conflictos y mantiene el estado
consolidado. No da por cubierto un criterio solo porque una tarea haya
terminado: debe enlazarlo con el cambio y con evidencia apropiada. El trabajo
adicional se registra como seguimiento cuando excede el alcance acordado.

## Cuándo paralelizar

El trabajo en paralelo ayuda cuando hay varias tareas concretas, sus
dependencias están disponibles, cada una puede generar evidencia propia y no
hay edición simultánea de los mismos archivos o contratos. Antes de empezar,
se acuerdan responsables, límites de cada área, interfaces de integración y
quién resolverá conflictos.

Es mejor mantener el trabajo secuencial cuando una tarea necesita el resultado
de otra para decidir cómo proceder, varias tareas tocarían los mismos
componentes, se investiga un único recorrido de extremo a extremo o falta una
decisión común. Paralelizar no elimina la integración, la revisión ni las
pruebas; si el coste de coordinación supera el beneficio, se elige una tarea
secuencial y acotada.

## Verificar cambios y pruebas

Quien implementa ejecuta las comprobaciones pertinentes que estén disponibles
y registra el comando, el resultado y cualquier limitación del entorno. Quien
valida selecciona evidencia acorde al cambio, por ejemplo:

- **Reglas o lógica:** pruebas deterministas de casos normales, límites,
  entradas no válidas y errores relevantes.
- **Almacenamiento:** comprobación de escritura, lectura, actualización,
  persistencia tras reiniciar y limpieza o aislamiento de datos de prueba.
- **Interfaz:** ejecución de los flujos principales, estados de error y
  accesibilidad; que el proyecto compile no demuestra que la interacción sea
  correcta.
- **Contratos entre componentes o servicios:** validación de formatos,
  respuestas, errores y compatibilidad en ambos lados del límite.
- **Integraciones con el entorno:** pruebas controladas ayudan a validar la
  lógica, pero no sustituyen las comprobaciones que requieran el servicio,
  dispositivo o entorno real. Se debe indicar qué parte quedó sin verificar.
- **Cambios de documentación o configuración:** revisión de enlaces,
  consistencia, comandos y afirmaciones frente al contenido real del proyecto.

La validación contrasta los criterios de aceptación con los resultados y
registra diferencias, fallos y cobertura pendiente. Si falta una herramienta,
servicio o entorno necesario, se ejecutan las comprobaciones que sí sean
posibles y se especifica qué falta para completar la verificación. No se
presenta una prueba parcial como completa, ni se afirma compatibilidad o
correctitud que no esté respaldada por evidencia.

## Lista breve de cierre

- [ ] El resultado responde a criterios de aceptación explícitos.
- [ ] Se respetaron el alcance, las dependencias y las decisiones aprobadas.
- [ ] Los cambios y las comprobaciones se pueden identificar y revisar.
- [ ] Se informaron los comandos ejecutados, los resultados y los fallos.
- [ ] Lo no ejecutado y sus motivos figuran como pendiente, no como aprobado.
- [ ] El handoff indica riesgos y el siguiente paso, si lo hay.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1-pm.md` — ejemplo de alcance
  explícito, comportamiento de referencia y evidencia de análisis.
- `.github/modernize/rearchitecture/artifacts/constitution.md` — referencia
  para límites y decisiones aprobadas que deben orientar la implementación.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead.md` — organización
  de tareas, dependencias y validación de un trabajo mayor.
- `.github/modernize/rearchitecture/artifacts/t9-teamlead.md` — distinción
  entre una revisión estructural aprobada y evidencia de ejecución del producto.
- `.github/modernize/rearchitecture/learnings/teamlead/native-android-validation-strategy.md`
  — diferencia entre pruebas controladas y validación que requiere el entorno
  real.

## Evidence Mapping

- `t1-pm.md#Summary` y `constitution.md#Core-Principles` → alcance, contexto,
  restricciones y aceptación descritos en “Seleccionar y delimitar tareas”.
- `t8-teamlead.md#Implementation-Steps` y `#Testing-Strategy` → dependencias,
  responsabilidades y selección de comprobaciones descritas en esta guía.
- `t9-teamlead.md#Verdict` y `#Feasibility-and-Remaining-Blockers` → obligación
  de distinguir la aprobación de un plan de la evidencia de ejecución.
- `native-android-validation-strategy.md#Takeaway` → limitaciones y necesidad
  de evidencia de entorno real incorporadas en “Verificar cambios y pruebas”.

## Test Results

- Command: PowerShell: comprobar estructura Markdown, roles, límites de tareas,
  handoff, paralelización, verificación, trazabilidad y ausencia de referencias
  a herramientas privadas o detalles de Nutria runtime.
- Passed: 12 comprobaciones estructurales y de alcance.
- Failed: 0.
- Skipped: pruebas de producto no aplicables; este entregable es una guía de
  proceso.
