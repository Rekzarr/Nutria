# nutria

Primera versión de una aplicación web progresiva (PWA) adaptable a móviles para registrar nutrición y agua. Se puede añadir a la pantalla de inicio desde un navegador móvil compatible; la interfaz y la API ASP.NET Core se sirven desde el mismo origen. Para que el análisis fotográfico funcione en producción, despliega la aplicación bajo HTTPS.

## Funciones

- Navegación en cinco pestañas independientes: resumen, comida, recetas, foto y calendario mensual.
- Calcula calorías, proteínas, carbohidratos y grasas según gramos, onzas o raciones.
- Muestra el agua junto a los macronutrientes del resumen, con accesos rápidos para registrar 150 o 250 ml.
- Organiza recetas en Comidas y Cenas para la generación aleatoria; al editar el calendario, cualquier receta puede asignarse a cualquier turno.
- Calcula calorías y macronutrientes de recetas sumando los ingredientes. Busca en Open Food Facts con nombres/localización en español y pulsa un resultado para importar sus valores por 100 g; también puedes introducirlos manualmente. La búsqueda requiere que el servidor de Nutria esté ejecutándose. Para medidas distintas de gramos, indica el peso equivalente. Revisa los datos del envase.
- Usa la pestaña Calendario en formato mensual visual. En móvil, toca un día para consultar o editar sus comidas; también puedes generar menús aleatorios semanales (de lunes a domingo) o mensuales con tus recetas. Los menús y cambios se guardan en el dispositivo.
- Registra comidas y agua, permite borrar comidas individualmente y deshacer la última toma de agua, muestra el resumen diario y permite personalizar objetivos.
- Incluye un historial de comidas agrupado por día que muestra los últimos 30 días.
- Permite elegir entre cuatro colores de aplicación; la preferencia se guarda en el dispositivo.
- Ofrece recetas nuevas con ingredientes, preparación, tiempo y complejidad; puedes filtrar por tipo (comidas, cenas o ambas), tiempo y dificultad exacta (o elegir cualquier dificultad), y actualizar las sugerencias. Todas se mantienen en un máximo de 550 kcal, y las recetas guardadas conservan sus datos de preparación.
- Incluye un seguimiento menstrual privado en el dispositivo: muestra un círculo de progreso y una fase aproximada (menstruación, folicular, ventana ovulatoria estimada o lútea), estima la próxima fecha y cuenta los días de posible retraso si no se registra el nuevo inicio. Permite corregir fechas olvidadas, ajustar la duración habitual del ciclo y de la regla, y anotar a diario flujo, dolor, ánimo y notas. Son estimaciones de calendario, no confirman ovulación, fertilidad o embarazo ni sustituyen orientación profesional.
- Persiste los datos localmente en el navegador (`localStorage`); no hay cuenta ni sincronización entre dispositivos.
- Analiza fotos de platos con OpenAI y añade la estimación al diario. En móviles, el botón de foto ofrece la cámara trasera en navegadores compatibles y también permite subir una imagen existente. Requiere configurar una clave API en el servidor.

## Cliente Android nativo (migración en curso)

La migración añade una aplicación Android nativa y mantiene temporalmente la PWA y el servidor actuales. La API sigue siendo el límite para el análisis de fotos y la búsqueda de ingredientes; la clave de OpenAI permanece en el servidor.

La pantalla Actividad puede leer los pasos de hoy desde Health Connect. La aplicación solicita permiso de lectura solo cuando la persona pulsa el botón para conectarlo y vuelve a comprobar los datos al regresar a primer plano. Si Health Connect no está disponible, no hay registros o falla la lectura, la pantalla informa ese estado en lugar de inventar un total o mostrar cero como si fuera un dato real.

La integración es de solo lectura: no solicita permiso para escribir en Health Connect, no guarda el total de pasos en la base de datos o preferencias y no lo envía a Nutria ni a otro servidor. El total leído se conserva únicamente en el estado temporal de la pantalla.

La tarjeta independiente de pulsera Bluetooth permite buscar y seleccionar una banda compatible con el servicio estándar de frecuencia cardiaca BLE (`0x180D`) y su característica de medición (`0x2A37`). Conectar la banda no inicia un entrenamiento: la persona debe pulsar «Iniciar entrenamiento». Solo se muestran y guardan las lecturas que la banda notifica, con su hora; no se calculan calorías ni minutos de ejercicio. La pantalla distingue permisos, Bluetooth apagado, incompatibilidad, fallos y pérdida de conexión. Al empezar un entrenamiento se mantiene un aviso del sistema con una acción para detenerlo.

Android solicita los permisos de dispositivos cercanos cuando se necesita buscar o conectar una pulsera. En Android 11 y versiones anteriores, la plataforma también exige permiso de ubicación durante el uso para buscar dispositivos Bluetooth; Nutria no calcula ni guarda la ubicación. Los entrenamientos y las lecturas reales se guardan en la base cifrada privada de Nutria. No se envían a Health Connect ni al servicio de Nutria. Las lecturas antiguas y los intervalos sin conexión se indican como tales; no se rellenan con valores estimados.

**Validación del dispositivo pendiente:** las pruebas automatizadas no sustituyen una comprobación de Health Connect ni de BLE con un teléfono y una banda compatibles. En este entorno no hay dispositivo conectado y el emulador no puede arrancar porque falta el controlador de hipervisor. Por ello, no se verificaron las ventanas de permisos, la conexión GATT, las notificaciones persistentes ni la recuperación de ciclo de vida en un dispositivo. Los nombres de pantallas y pasos de permiso pueden variar según Android; no se presentan aquí como instrucciones verificadas.

## Aplicación nativa Android (migración en curso)

La PWA descrita arriba se conserva y sigue siendo la referencia funcional mientras se comprueba la paridad. La aplicación Compose de `android/` se está implementando por partes; todavía no sustituye a la PWA ni ofrece todas sus capacidades.

La pantalla «Inicio» muestra ahora las calorías y los macronutrientes acumulados de las comidas de hoy frente a sus objetivos; las calorías restantes nunca se muestran como un valor negativo. La pantalla «Foto» permite seleccionar una imagen JPG, PNG o WebP de hasta 10 MiB, pedir un análisis explícito al backend y guardar su estimación orientativa como comida en el diario local. Para conectarla, configura en «Ajustes» la URL HTTPS del servidor Nutria que expone `/api/analyze-meal`; no incluyas claves ni credenciales. La clave de OpenAI permanece en el servidor; la foto solo se envía al elegir «Analizar y añadir al diario». La porción actual de «Comida» permite registrar alimentos manualmente en gramos, onzas (1 oz = 28,3495 g) o porciones con su peso, revisar una vista previa de nutrientes, consultar los últimos 30 días y quitar comidas del día actual. También permite anotar +150 ml o +250 ml de agua y deshacer la última toma. El historial de días anteriores es de solo lectura. Estos registros se guardan en la base local cifrada de la aplicación; no se importan automáticamente los datos del navegador y no se envían al backend desde este flujo. Los nutrientes se guardan con precisión de milésimas. Se implementó una migración explícita destinada a conservar comidas del esquema anterior; su prueba SQL instrumentada se compiló pero aún no se ejecutó en un dispositivo.

La pantalla nativa «Ciclo» permite registrar, corregir y finalizar períodos; valida fechas y solapamientos, permite configurar promedios y muestra fases/fechas como estimaciones con sus advertencias. El registro privado de hoy incluye flujo, dolor, ánimo y una nota opcional de hasta 160 caracteres; al volver a abrir la pantalla se cargan los valores ya guardados para poder editarlos. Períodos y notas se guardan únicamente en la base Room cifrada de la aplicación, no se incluyen en notificaciones ni se envían por red. El acceso a la pantalla no verifica ni diagnostica condiciones médicas.

**Verificación de host (14-11-2026):** `.\gradlew.bat clean test build assembleDebugAndroidTest --no-daemon --no-parallel --max-workers=1` terminó correctamente con 61 pruebas JVM aprobadas, compilaciones debug/release, lint sin errores y APK instrumentado empaquetado. Las pruebas instrumentadas no se ejecutaron: no hay un dispositivo conectado y el emulador no puede arrancar porque falta el controlador de hipervisor. Por tanto, la conexión real de Health Connect/BLE, la ejecución de interfaz en Android y la paridad completa siguen sin verificarse.

## Ejecutar la aplicación

Requiere el SDK de .NET 10 y una clave API de OpenAI. Para desarrollo local, guarda la clave fuera del repositorio con User Secrets:

```powershell
dotnet user-secrets set "OPENAI_API_KEY" "tu-clave"
dotnet run --project Nutria.csproj
```

Ejecuta estos comandos desde la carpeta del proyecto. No compartas ni confirmes la clave en el código, `appsettings.json` o el navegador; para producción, usa la configuración de secretos del entorno de despliegue. Abre la URL local que indique `dotnet run`. En un móvil, usa «Añadir a pantalla de inicio» en el menú del navegador para instalar la PWA. En la pestaña «Foto», el selector abre la cámara trasera en dispositivos compatibles y permite escoger una foto si no está disponible. La especificación OpenAPI está disponible en `/openapi/v1.json` durante el desarrollo. Opcionalmente, `OPENAI_MODEL` cambia el modelo (por defecto `gpt-4.1-mini`).

El cliente envía la foto como `multipart/form-data` a `POST /api/analyze-meal`. El servidor acepta imágenes JPG, PNG y WebP de hasta 10 MB, limita los análisis por IP y nunca expone la clave al navegador. Las estimaciones son aproximadas y deben comprobarse; no sustituyen el consejo de un profesional sanitario. La imagen se envía a OpenAI para analizarla. No expongas el backend públicamente sin añadir autenticación y controles de uso.

## Estructura

- `wwwroot/index.html`: estructura accesible y formularios.
- `wwwroot/styles.css`: interfaz adaptable a escritorio y móvil.
- `wwwroot/app.js`: cálculos, almacenamiento local y cliente de la API.
- `Endpoints/MealAnalysisEndpoints.cs`: endpoint HTTP de análisis.
- `Services/OpenAiMealAnalysisService.cs`: integración con Responses API.
- `Endpoints/OpenFoodFactsEndpoints.cs` y `Services/OpenFoodFactsService.cs`: búsqueda de ingredientes y nutrientes.

Los datos del diario se guardan bajo la clave `nutria.v1` del navegador. La clave API nunca debe guardarse en el frontend ni confirmarse en el repositorio.
