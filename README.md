# nutria

Primera versión de una aplicación web progresiva (PWA) adaptable a móviles para registrar nutrición y agua. Se puede añadir a la pantalla de inicio desde un navegador móvil compatible; la interfaz y la API ASP.NET Core se sirven desde el mismo origen. Para que el análisis fotográfico funcione en producción, despliega la aplicación bajo HTTPS.

## Funciones

- Navegación en cinco pestañas independientes: resumen, comida, recetas, foto y calendario mensual.
- Calcula calorías, proteínas, carbohidratos y grasas según gramos, onzas o raciones.
- Muestra el agua junto a los macronutrientes del resumen, con accesos rápidos para registrar 150 o 250 ml.
- Organiza recetas en Comidas y Cenas para la generación aleatoria; al editar el calendario, cualquier receta puede asignarse a cualquier turno.
- Calcula calorías y macronutrientes de recetas sumando los ingredientes. La PWA busca directamente en Open Food Facts con nombres/localización en español y pulsa un resultado para importar sus valores por 100 g; también puedes introducirlos manualmente. Esta consulta no pasa por el backend. La PWA conserva por ahora el endpoint antiguo `/cgi/search.pl`, que puede devolver HTTP 503; Android nativo ya usa Search-a-licious. Para medidas distintas de gramos, indica el peso equivalente. Revisa los datos del envase.
- Usa la pestaña Calendario en formato mensual visual. En móvil, toca un día para consultar o editar sus comidas; también puedes generar menús aleatorios semanales (de lunes a domingo) o mensuales con tus recetas. Los menús y cambios se guardan en el dispositivo.
- Registra comidas y agua, permite borrar comidas individualmente y deshacer la última toma de agua, muestra el resumen diario y permite personalizar objetivos.
- Incluye un historial de comidas agrupado por día que muestra los últimos 30 días.
- Permite elegir entre cuatro colores de aplicación; la preferencia se guarda en el dispositivo.
- Ofrece recetas nuevas con ingredientes, preparación, tiempo y complejidad; puedes filtrar por tipo (comidas, cenas o ambas), tiempo y dificultad exacta (o elegir cualquier dificultad), y actualizar las sugerencias. Todas se mantienen en un máximo de 550 kcal, y las recetas guardadas conservan sus datos de preparación.
- Incluye un seguimiento menstrual privado en el dispositivo: muestra un círculo de progreso y una fase aproximada (menstruación, folicular, ventana ovulatoria estimada o lútea), estima la próxima fecha y cuenta los días de posible retraso si no se registra el nuevo inicio. Permite corregir fechas olvidadas, ajustar la duración habitual del ciclo y de la regla, y anotar a diario flujo, dolor, ánimo y notas. Son estimaciones de calendario, no confirman ovulación, fertilidad o embarazo ni sustituyen orientación profesional.
- Persiste los datos localmente en el navegador (`localStorage`); no hay cuenta ni sincronización entre dispositivos.
- Analiza fotos de platos con OpenAI y añade la estimación al diario. En móviles, el botón de foto ofrece la cámara trasera en navegadores compatibles y también permite subir una imagen existente. Requiere configurar una clave API en el servidor.

## Cliente Android nativo (migración en curso)

La migración añade una aplicación Android nativa y mantiene temporalmente la PWA y el servidor actuales. La API solo es necesaria para el análisis de fotos; la clave de OpenAI permanece en el servidor. La búsqueda de ingredientes se conecta directamente desde la PWA y Android a Open Food Facts.

La pantalla Actividad incluye un selector semanal para consultar hoy y hasta 29 días anteriores. Los días futuros y los que queden fuera de esa ventana no se pueden seleccionar. Al elegir un día, Nutria lee sus pasos directamente de Health Connect y muestra los entrenamientos BLE que Nutria guardó localmente en esa fecha. La aplicación solicita permiso de lectura de pasos solo cuando la persona lo elige y vuelve a comprobarlos al cambiar de día, regresar a primer plano o pulsar «Actualizar». Si Health Connect no está disponible, no hay registros o falla la lectura, la pantalla informa ese estado en lugar de inventar un total o mostrar cero como si fuera un dato real.

La integración de pasos es de solo lectura: no solicita permiso para escribir en Health Connect, no guarda los totales en la base de datos o preferencias y no los envía a Nutria ni a otro servidor. El total consultado se conserva únicamente en el estado temporal de la pantalla.

La tarjeta independiente de pulsera Bluetooth permite buscar y seleccionar una banda compatible con el servicio estándar de frecuencia cardiaca BLE (`0x180D`) y su característica de medición (`0x2A37`). Conectar la banda no inicia un entrenamiento: la persona debe pulsar «Iniciar entrenamiento». Solo se muestran y guardan las lecturas que la banda notifica, con su hora; no se calculan calorías ni minutos de ejercicio. La pantalla distingue permisos, Bluetooth apagado, incompatibilidad, fallos y pérdida de conexión. Al empezar un entrenamiento se mantiene un aviso del sistema con una acción para detenerlo.

Android solicita los permisos de dispositivos cercanos cuando se necesita buscar o conectar una pulsera. En Android 11 y versiones anteriores, la plataforma también exige permiso de ubicación durante el uso para buscar dispositivos Bluetooth; Nutria no calcula ni guarda la ubicación. Los entrenamientos y las lecturas reales se guardan en la base cifrada privada de Nutria. No se envían a Health Connect ni al servicio de Nutria. Las lecturas antiguas y los intervalos sin conexión se indican como tales; no se rellenan con valores estimados.

**Validación de hardware:** en el OPPO CPH2197 con Android 13 se verificó una lectura real de pasos desde Health Connect, incluido el permiso de solo lectura y la acción «Actualizar». La conexión a una pulsera BLE compatible sigue pendiente: no se han verificado en hardware la conexión GATT, las notificaciones persistentes ni las interrupciones del entrenamiento. El AVD disponible tampoco puede arrancar porque falta el controlador de hipervisor.

## Aplicación nativa Android (migración en curso)

La PWA descrita arriba se conserva y sigue siendo la referencia funcional mientras se comprueba la paridad. La aplicación Compose de `android/` se está implementando por partes; todavía no sustituye a la PWA ni ofrece todas sus capacidades.

La pantalla «Inicio» muestra calorías, macronutrientes y agua de hoy frente a sus objetivos, con barras de progreso visualmente limitadas al 100 %, la racha de días consecutivos con comidas o agua, el número de comidas y una lista breve de sus nombres/calorías. Las calorías restantes nunca se muestran como un valor negativo. En «Comida», la búsqueda envía el nombre directamente a Open Food Facts y permite copiar sus nutrientes por 100 g a la entrada manual; no necesita URL de Nutria. Confirma siempre los valores con el envase. La pantalla «Foto» permite seleccionar una imagen JPG, PNG o WebP de hasta 10 MiB, pedir un análisis explícito al backend y guardar su estimación orientativa como comida en el diario local. Para conectarla, configura en «Ajustes» la URL HTTPS del servidor Nutria que expone `/api/analyze-meal`; no incluyas claves ni credenciales. La clave de OpenAI permanece en el servidor; la foto solo se envía al elegir «Analizar y añadir al diario». La porción actual de «Comida» permite registrar alimentos manualmente en gramos, onzas (1 oz = 28,3495 g) o porciones con su peso, revisar una vista previa de nutrientes, consultar los últimos 30 días y quitar comidas del día actual. También permite anotar +150 ml o +250 ml de agua, deshacer la última toma y, desde «Inicio», borrar conjuntamente las comidas y el agua de hoy tras una confirmación. Esa acción no afecta recetas, planes ni registros de otros días. El historial de días anteriores es de solo lectura. Estos registros se guardan en la base local cifrada de la aplicación; no se importan automáticamente los datos del navegador y no se envían al backend desde este flujo. Los nutrientes se guardan con precisión de milésimas. Se implementó una migración explícita destinada a conservar comidas del esquema anterior; su prueba SQL instrumentada se compiló pero aún no se ejecutó en un dispositivo.

La pantalla nativa «Recetas» permite descubrir recetas sugeridas sin conexión, filtrar por categoría, tiempo máximo y complejidad, consultar ingredientes y preparación, y guardar sugerencias en el libro local. Las sugerencias conservan el máximo de 550 kcal estimadas por ración y las ya guardadas no se repiten. Los filtros se guardan en DataStore. También permite crear, editar y eliminar recetas locales con ingredientes, peso comestible, nutrientes por 100 g, raciones, categoría, tiempo, dificultad y pasos o notas de preparación, que se escriben una por línea y se conservan en orden. La opción «Importar receta desde archivo» acepta JSON `nutria.recipe.v1` de hasta 128 KiB, valida sus campos y evita duplicados por nombre; la importación no requiere red ni permisos de almacenamiento. Los nutrientes totales y por ración se calculan a partir de las cantidades; la búsqueda existente puede rellenar el ingrediente seleccionado y añadir una ración al diario. Las recetas se guardan solo en Room cifrada mediante una migración no destructiva, en unidades de milésimas; la búsqueda de ingredientes consulta Search-a-licious directamente y los datos de las recetas no se envían al servidor.

La pantalla nativa «Plan» muestra el calendario mensual, permite cambiar de mes, activar el desayuno y editar las recetas de cada día. También genera menús para el mes abierto o para una semana de lunes a domingo, comprueba que haya recetas de comida y cena y pide confirmación antes de sustituir un plan existente. Las asignaciones se guardan junto a las recetas en Room cifrada; eliminar una receta elimina sus asignaciones, pero editarla las conserva. El ajuste del desayuno se guarda aparte en DataStore. Los datos de la PWA no se importan automáticamente.

La pantalla nativa «Ciclo» permite registrar, corregir y finalizar períodos; valida fechas y solapamientos, permite configurar promedios y muestra fases/fechas como estimaciones con sus advertencias. El registro privado de hoy incluye flujo, dolor, ánimo y una nota opcional de hasta 160 caracteres; al volver a abrir la pantalla se cargan los valores ya guardados para poder editarlos. Períodos y notas se guardan únicamente en la base Room cifrada de la aplicación, no se incluyen en notificaciones ni se envían por red. El acceso a la pantalla no verifica ni diagnostica condiciones médicas.

**Verificación de host (14-11-2026):** `.\gradlew.bat clean test build assembleDebugAndroidTest --no-daemon --no-parallel --max-workers=1` terminó correctamente con 61 pruebas JVM aprobadas, compilaciones debug/release, lint sin errores y APK instrumentado empaquetado. Las pruebas instrumentadas no se ejecutaron: no hay un dispositivo conectado y el emulador no puede arrancar porque falta el controlador de hipervisor. Por tanto, la conexión real de Health Connect/BLE, la ejecución de interfaz en Android y la paridad completa siguen sin verificarse.

**Verificación de host (15-11-2026):** `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest` y `assembleDebug` terminaron correctamente para el calendario. Se generó la migración Room 8 y se compiló la prueba instrumentada de persistencia, reemplazo de intervalos y borrado en cascada. La prueba instrumentada no se ejecutó por falta de un dispositivo/emulador conectado.

**Verificación de host (15-11-2026):** las pruebas unitarias comprueban que las 11 recomendaciones tienen identificadores únicos, ingredientes y preparación, no superan 550 kcal estimadas por ración y dejan de mostrarse cuando se guardan. `lintDebug`, `assembleDebugAndroidTest` y `assembleDebug` pasan para esta función. La prueba instrumentada de preferencias está compilada, pero no se ejecuta sin dispositivo/emulador.

**Verificación de host (15-11-2026):** `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest` y `assembleDebug` pasan con el resumen diario y la racha. Se añadió una prueba instrumentada que comprueba una racha que combina comidas y agua y que se detiene en el primer día vacío; está compilada, pero no ejecutada sin dispositivo/emulador.

**Verificación de host (15-11-2026):** la acción de borrado diario se valida con una prueba instrumentada que cubre cancelar, confirmar y conservar intactos los registros de ayer. La operación usa una transacción Room existente; en la ejecución de host se compilaron las pruebas instrumentadas y el APK, pero no se ejecutaron en un dispositivo. La ejecución posterior en el OPPO se documenta a continuación.

**Verificación en dispositivo (15-11-2026):** `connectedDebugAndroidTest` pasó en un OPPO CPH2197 con Android 13: 30 pruebas instrumentadas, 0 fallos. La ejecución cubre el lanzamiento nativo, navegación semanal, pantallas Compose, almacenamiento cifrado, migraciones y persistencia de las funciones. En una comprobación manual, Nutria leyó pasos de hoy desde Health Connect y «Actualizar» volvió a consultarlos; al seleccionar fechas previas sin registros, mostró el estado «sin datos». No se registra el total personal. La conexión e interrupciones de una pulsera BLE siguen pendientes de validación.

**Verificación (17-11-2026):** el importador `nutria.recipe.v1` pasó 81 pruebas JVM junto con `lintDebug`, `assembleDebugAndroidTest` y `assembleDebug`. La APK se instaló en el OPPO CPH2197 y el JSON de la receta se copió a Descargas. La importación dentro de Nutria queda pendiente de selección manual: Enlace Móvil no expuso una ventana controlable para completar ese paso.

**Verificación (17-11-2026):** la búsqueda Android se migró a Search-a-licious `POST /search`. La prueba en el endpoint oficial devolvió HTTP 200 y productos en `hits`; `testDebugUnitTest`, `lintDebug`, `assembleDebugAndroidTest` y `assembleDebug` pasaron. Esta APK con el arreglo todavía no está instalada en el OPPO.

## Ejecutar la aplicación

Requiere el SDK de .NET 10 y una clave API de OpenAI para analizar fotos. La búsqueda de ingredientes llama directamente a Open Food Facts y funciona sin esta clave ni backend. Para desarrollo local, guarda la clave fuera del repositorio con User Secrets:

```powershell
dotnet user-secrets set "OPENAI_API_KEY" "tu-clave"
dotnet run --project Nutria.csproj
```

Ejecuta estos comandos desde la carpeta del proyecto. No compartas ni confirmes la clave en el código, `appsettings.json` o el navegador; para producción, usa la configuración de secretos del entorno de despliegue. Abre la URL local que indique `dotnet run`. En un móvil, usa «Añadir a pantalla de inicio» en el menú del navegador para instalar la PWA. En la pestaña «Foto», el selector abre la cámara trasera en dispositivos compatibles y permite escoger una foto si no está disponible. La especificación OpenAPI está disponible en `/openapi/v1.json` durante el desarrollo. Opcionalmente, `OPENAI_MODEL` cambia el modelo (por defecto `gpt-4.1-mini`).

El cliente envía la foto como `multipart/form-data` a `POST /api/analyze-meal`. El servidor acepta imágenes JPG, PNG y WebP de hasta 10 MB, limita los análisis por IP y nunca expone la clave al navegador. Las estimaciones son aproximadas y deben comprobarse; no sustituyen el consejo de un profesional sanitario. La imagen se envía a OpenAI para analizarla. No expongas el backend públicamente sin añadir autenticación y controles de uso. Consulta [docs/deployment-azure-container-apps.md](./docs/deployment-azure-container-apps.md) antes de desplegar.

## Estructura

- `wwwroot/index.html`: estructura accesible y formularios.
- `wwwroot/styles.css`: interfaz adaptable a escritorio y móvil.
- `wwwroot/app.js`: cálculos, almacenamiento local y cliente de la API.
- `Endpoints/MealAnalysisEndpoints.cs`: endpoint HTTP de análisis.
- `Services/OpenAiMealAnalysisService.cs`: integración con Responses API.
- `android/app/src/main/java/com/nutria/app/data/remote/OpenFoodFactsClient.kt`: cliente nativo de búsqueda directa en Open Food Facts.

Los datos del diario se guardan bajo la clave `nutria.v1` del navegador. La clave API nunca debe guardarse en el frontend ni confirmarse en el repositorio.
