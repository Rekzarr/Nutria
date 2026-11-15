# ADR-0002: Aplicación nativa Android y fuentes de actividad

- **Estado:** Aceptada
- **Fecha:** 2026-11-14
- **Ámbito:** Plataforma móvil y seguimiento de actividad

## Contexto

La aplicación existente es una PWA. El usuario decidió sustituirla por una app Android nativa y conservar toda su funcionalidad. También pidió un contador de pasos y conexión con pulseras para registrar entrenamientos. No existe una API web universal que permita leer pasos en segundo plano ni conectarse de forma uniforme a todas las pulseras y fabricantes.

## Decisión

- Crear una aplicación Android nativa y migrar las funciones de Nutria antes de considerar retirada la PWA existente.
- Leer pasos desde Health Connect, solicitando permiso de lectura y sin inventar un valor cuando no haya acceso o datos.
- Admitir pulseras que publiquen el servicio BLE estándar de frecuencia cardiaca; requerir una acción explícita de la persona para conectar y para iniciar un entrenamiento.
- Mantener las claves de proveedores externos y las llamadas a OpenAI/Open Food Facts en el servidor existente; el cliente Android consume la API de Nutria.
- Mantener privados y locales los registros del diario, ciclo y entrenamientos, excepto las solicitudes de foto/búsqueda que la persona inicie explícitamente y que se envían a la API correspondiente.

## Alternativas consideradas

- Mantener solo la PWA: no cubre el requisito de acceso nativo a datos de salud y Bluetooth de manera fiable.
- Integrar SDK privados de cada fabricante: ampliaría la compatibilidad selectiva, pero requeriría acuerdos, dependencias y trabajo específico por marca.
- Leer pasos directamente del sensor del teléfono: no sustituye el historial agregado de Health Connect ni garantiza recuentos en segundo plano.

## Consecuencias y limitaciones

- La PWA sigue siendo necesaria como referencia mientras no se verifique la paridad de funciones en Android.
- Health Connect depende de que esté disponible, configurado y autorizado por la persona.
- BLE solo garantiza comunicación con dispositivos compatibles con el perfil estándar seleccionado; la compatibilidad de una pulsera concreta debe comprobarse en un dispositivo real.
- Las pruebas unitarias no verifican los diálogos de permisos, la conexión GATT ni el ciclo de vida en segundo plano. Estos puntos requieren teléfono y pulsera compatibles.
- No se calcula gasto calórico, intensidad ni actividad en periodos sin lecturas recibidas.

## Evidencia

- Requisitos confirmados por el usuario: reemplazo de la PWA por Android nativo, preservando la funcionalidad existente; Health Connect y pulseras BLE.
- `android/app/src/main/java/com/nutria/app/data/health/HealthConnectStepsDataSource.kt`
- `android/app/src/main/java/com/nutria/app/feature/activity/WorkoutBleService.kt`
- `android/app/src/main/java/com/nutria/app/data/remote/NutriaApiClient.kt`
