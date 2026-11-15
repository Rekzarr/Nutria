# t16 — Diario, alimentos, historial y agua

## Estado

La porción asignada está implementada: el formulario de alimentos y la vista previa aplican gramos, onzas y porciones; las comidas y eventos de agua se guardan localmente; el historial cubre 30 días y es de solo lectura para fechas anteriores; y se puede deshacer únicamente la última toma de agua del día. La persistencia nutricional ahora sigue el contrato de unidades escaladas del diseño DBA mediante migración Room v5→v6. La verificación serializada completa del host pasó (59 pruebas JVM, build y lint); Android instrumentado quedó empaquetado pero no ejecutado por falta de dispositivo/emulador. Se mantienen los límites de alcance de T007: el resumen Inicio/macros/objetivos y la acción de vaciar el día pertenecen a T008. La PWA sigue intacta; este resultado no demuestra paridad total.

## Entregables

- [Informe estructurado de implementación y resultados](./t16-frontend-batch-report.yaml) — trazabilidad y evidencia de verificación.
- [Informe de implementación detallado](./checkpoints/t16-implementation.yaml) — alcance, persistencia, pruebas y límites de dispositivo.
- [Checkpoint de T007](./checkpoints/t16-tasks-to-impl.yaml) — mapeo de tarea a archivos y pruebas.
- `README.md` — documentación en español del estado parcial, la retención de PWA y la validación real pendiente.
- `docs/adr/0001-agua-por-eventos-room.md` — decisión aceptada para almacenar tomas como eventos y deshacer la última.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1-pm.md` — alcance del producto.
- `.github/modernize/rearchitecture/artifacts/t1-pm-spec.md` — REQ-003, REQ-004 y REQ-005.
- `.github/modernize/rearchitecture/artifacts/t4-architect.md` — límites de aplicación nativa y persistencia.
- `.github/modernize/rearchitecture/artifacts/t5-ux.md` — flujos de Comida, historial y controles de agua.
- `.github/modernize/rearchitecture/artifacts/t7-teamlead.md` — estrategia de pruebas para reglas, Room y Compose.
- `.github/modernize/rearchitecture/artifacts/t8-teamlead-plan.md` — T007 y límites respecto de T008.
- `.github/modernize/rearchitecture/artifacts/t10-devops.md` y `.github/modernize/rearchitecture/artifacts/checkpoints/t8-environment.md` — toolchain Android disponible y limitaciones de dispositivo.
- `.github/modernize/rearchitecture/artifacts/t11-frontend.md` — scaffold Compose y convención de validación.
- `.github/modernize/rearchitecture/artifacts/t12-dba.md` — base cifrada, migraciones explícitas y almacenamiento privado.
- `.github/modernize/rearchitecture/artifacts/t6-dba.md` — modelo local y precisión entera escalada de nutrientes.
- `.github/modernize/rearchitecture/artifacts/t13-frontend.md` — navegación española y shell nativo.
- `.github/modernize/rearchitecture/artifacts/t15-frontend.md` — modelos de alimento, comida y agua, y contratos de repositorio.
- `.github/modernize/rearchitecture/artifacts/t19.1-frontend.md` — remediación lint compartida y comando Gradle serializado.

## Evidence Mapping

- `t1-pm-spec.md#REQ-003` + `t5-ux.md#Comida—entrada-e-historial` → `FoodEntryValidation.kt`, `DiaryScreen.kt`, `FoodEntryValidatorTest.kt` → validación, conversión y vista previa probadas; la suite JVM completa pasó.
- `t1-pm-spec.md#REQ-004` + `t8-teamlead-plan.md#T007` → `DiaryLocalStore.kt`, `DiaryViewModel.kt`, `DiaryScreen.kt` → consulta de 30 días y edición exclusiva de hoy; APK de instrumentación empaquetado, ejecución en dispositivo pendiente.
- `t1-pm-spec.md#REQ-005` + `t5-ux.md#Inicio` → `DiaryLocalStore.kt`, `DiaryViewModel.kt`, `DiaryScreen.kt` → eventos +150/+250 ml y undo transaccional de la última toma; pruebas instrumentadas compiladas pero no ejecutadas.
- `t6-dba.md#Proposed-local-model` → `MealRecordEntity`, `MIGRATION_5_6` y esquema Room v6 → nutrientes guardados como enteros escalados; JVM prueba la conversión/round-trip y continuidad de migraciones. La prueba SQL de conservar filas v5 está empaquetada en AndroidTest pero no ejecutada.
- `t7-teamlead.md#Local-persistence/repository-integration` → `checkpoints/t16-implementation.yaml#test_results` → 59 pruebas JVM pasan y el build/lint serializado pasa; la persistencia SQL y el flujo Compose requieren ejecutar AndroidTest en un dispositivo compatible.

## Test Results

- Command: `.\gradlew.bat clean test build assembleDebugAndroidTest --no-daemon --no-parallel --max-workers=1` (desde `android/`, con JDK 17.0.16 y Android SDK 36 activados en el mismo proceso PowerShell).
- Passed: 59 pruebas JVM; compilaciones debug/release; lint y `check`; compilación/empaquetado del APK de AndroidTest.
- Failed: 0 pruebas JVM.
- Skipped: 0 pruebas JVM.
- Build: `BUILD SUCCESSFUL`, código 0; `lintDebug` sin errores (22 avisos y 1 sugerencia informativa). Se observaron advertencias Kotlin/AGP no bloqueantes.
- Instrumentación: las fuentes de pruebas y el APK AndroidTest se compilaron/empaquetaron; no se ejecutaron. `adb devices -l` no mostró dispositivos y `emulator -accel-check` devolvió 6 por ausencia del controlador de hipervisor.
- API: no se modificó ni produjo ningún backend o endpoint.

## Hallazgos y límites

- La PWA no fue eliminada ni modificada funcionalmente.
- Una diferencia con el comportamiento legado es que este slice sigue la UX aprobada y deja el historial pasado como solo lectura, aunque el PWA antiguo permitía borrar comidas anteriores. La discrepancia fue comunicada a UX.
- La verificación de build del proyecto está verde en este host, pero la persistencia/Compose instrumentados y la aplicación en dispositivo siguen sin verificarse. No se presenta la aplicación como paridad completa.
