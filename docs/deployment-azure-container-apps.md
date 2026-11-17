# Preparación del backend para Azure Container Apps

La búsqueda de ingredientes ya no necesita servidor. El backend se conserva únicamente para `POST /api/analyze-meal`, que envía la imagen a OpenAI. La imagen de contenedor se prepara aquí, pero **no se ha desplegado ningún recurso**.

## Estado de seguridad: no publicar aún

El endpoint fotográfico solo tiene límite de solicitudes por dirección IP; todavía no requiere autenticación. Publicarlo con ingreso externo permitiría que terceros consumieran la clave y el presupuesto de OpenAI. No despliegues la aplicación públicamente hasta implementar autenticación adecuada para clientes móviles y controles de cuota por usuario. Un token estático incluido en una APK no es un secreto y no basta como protección.

Las alertas de presupuesto de Azure sirven para avisar, no para garantizar un tope de gasto. Azure Container Apps Consumption puede escalar a cero, pero no implica coste cero: también pueden cobrarse el registro de imágenes, el entorno, los registros, el tráfico y el uso que exceda las franquicias vigentes. Comprueba los precios y límites de la región y suscripción antes de crear recursos.

## Imagen de contenedor

El `Dockerfile` publica el proyecto con el SDK .NET 10 y ejecuta la imagen ASP.NET Runtime .NET 10 como usuario no root. La aplicación escucha en el puerto `8080`. `.dockerignore` excluye el repositorio Git, Android, documentación, salidas de compilación y archivos locales.

Desde la raíz del proyecto, con Docker instalado y su motor en ejecución:

```powershell
docker build -t nutria:local .
docker run --rm -p 8080:8080 -e OPENAI_API_KEY="CLAVE_LOCAL" nutria:local
```

La clave de ejemplo debe ser una variable de entorno local, no un valor que se escriba en el Dockerfile, en la imagen o en el repositorio. La aplicación web debería responder en `http://localhost:8080`; el análisis de fotos solo podrá funcionar con una clave válida.

## Despliegue futuro (después de añadir autenticación)

1. Instala y autentica Azure CLI y la extensión `containerapp`; selecciona explícitamente la suscripción correcta.
2. Revisa las opciones de la suscripción y la región, el coste del entorno, del registro de contenedores y de Log Analytics. Configura presupuesto y alertas antes de crear recursos.
3. Publica la imagen en un registro privado y crea Azure Container Apps con ingreso HTTPS solo cuando la autenticación y las cuotas estén implementadas.
4. Configura `OPENAI_API_KEY` como secreto de Container Apps y referencia ese secreto desde la variable de entorno; no pases la clave como argumento de compilación ni la guardes en el repositorio.
5. Configura mínimo de réplicas `0`, límites de CPU/memoria y registros con retención acotada. Verifica desde el portal las reglas de escala y los costes estimados.
6. Despliega primero en un entorno de prueba, comprueba los límites de uso y la eliminación de recursos; no uses `az containerapp up` en una suscripción con gastos no autorizados.

La autenticación, las cuotas por identidad, el despliegue y la configuración de recursos de Azure quedan fuera de esta preparación y requieren una decisión/validación adicional.

Referencia de Microsoft: [Deploy Azure Container Apps with the `az containerapp up` command](https://learn.microsoft.com/azure/container-apps/containerapp-up).
