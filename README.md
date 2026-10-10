# El Tumi ERP

Sistema de gestión para un restaurante, desarrollado con Spring Boot, MySQL y React. Incluye ventas, inventario, contabilidad, finanzas, recursos humanos y administración de usuarios.

## Ejecutar en Windows

Necesitas Java 17 o 21 y MySQL 8.0.

1. Inicia MySQL y ejecuta `database/crear_base_mysql.sql` con un usuario administrador.
2. Abre `iniciar.cmd`. En el primer arranque se crea `config/application.properties`; ajusta ahí la conexión si utilizas otro usuario o contraseña de MySQL.
3. Espera el mensaje `Started TumiApplication` y entra a http://localhost:8080.

Acceso inicial: `admin@eltumi.local` / `********`. Cambia esta contraseña antes de usar datos reales.

El ejecutable está en `runtime/el-tumi-erp.jar` e incluye la interfaz. Para detenerlo, pulsa `Ctrl+C` en la terminal.

Para probar sin MySQL, abre `iniciar-demo.cmd`. Este modo guarda los datos en `data/`, separados de MySQL. En Linux o macOS usa `bash iniciar.sh` o `bash iniciar.sh --demo`.

## Desarrollo

Necesitas JDK 17 o 21, Maven 3.9 y Node.js 22.12 o superior. Ejecuta los comandos desde la carpeta `el-tumi-erp`.

Para arrancar el backend en PowerShell con el usuario creado por el script SQL:

```powershell
$env:DB_USER = 'tumi'
$env:DB_PASSWORD = 'tumi_local_2026'
cd backend
mvn spring-boot:run
```

Si tu conexión es distinta, configura `DB_URL`, `DB_USER` y `DB_PASSWORD` con tus valores.

En otra terminal, inicia la interfaz:

```powershell
cd frontend
npm ci
npm run dev
```

Abre http://localhost:5173. El backend debe estar en el puerto 8080. Detén cualquier instancia de `iniciar.cmd` antes de arrancarlo desde Maven.

Para compilar la interfaz, ejecutar las pruebas y actualizar el JAR, desde la raíz del proyecto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

En Linux o macOS: `bash scripts/build.sh`. Recompila después de cambiar el código para que el ejecutable incluya los cambios.

## Docker

Puedes copiar `.env.example` a `.env` para ajustar puertos y contraseñas. Luego ejecuta:

```sh
docker compose up --build -d
```

La aplicación queda en http://localhost:8080 y MySQL en el puerto 3306. Para detenerlos, usa `docker compose down`; los datos se conservan en los volúmenes.

## Datos y alcance

Las tablas se crean automáticamente con Flyway. Por defecto se cargan datos de demostración al iniciar una base vacía. Los adjuntos se guardan en `private-files/`; respalda esa carpeta junto con la base de datos.

Los comprobantes son internos y no se envían a SUNAT. Los cálculos de planillas y beneficios requieren revisión y configuración antes de su uso.
