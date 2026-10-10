# El Tumi ERP c

Sistema local de gestión del restaurante con **Spring Boot + MySQL + React**. Incluye código fuente, ejecutable con la interfaz integrada, 54 tablas de negocio, migraciones automáticas, datos de demostración y pruebas de los procesos principales.

## Inicio rápido en Windows con MySQL

Necesitas **Java 17 o Java 21** y **MySQL 8.0**. El ejecutable incluido no necesita Node ni Maven para funcionar.

1. Extrae completamente el ZIP. No ejecutes los archivos desde la vista comprimida.
2. Inicia MySQL. En MySQL Workbench abre y ejecuta `database/crear_base_mysql.sql` con tu usuario administrador. Este script crea la base `el_tumi` y el usuario local `tumi` sin borrar datos existentes.
3. Haz doble clic en **`iniciar.cmd`**. La primera ejecución crea `config/application.properties` a partir del ejemplo incluido.
4. Espera el mensaje `Started TumiApplication` y abre **http://localhost:8080**.

| Acceso de demostración | Valor |
|---|---|
| Correo | `admin@eltumi.local` |
| Contraseña | `ElTumi2026!` |

Si prefieres conectar con un usuario MySQL que ya tienes, copia `config/application.properties.example` como `config/application.properties` y cambia la URL, el usuario y la contraseña. No necesitas ejecutar el script de creación del usuario `tumi`; basta con disponer de una base vacía `el_tumi` y permisos sobre ella. **La contraseña de MySQL y la contraseña del administrador del ERP son diferentes.**

Para las siguientes ejecuciones solo inicia MySQL y abre `iniciar.cmd`. Las tablas se crean con Flyway y los datos de prueba se cargan una sola vez. Al cerrar la terminal se detiene el servidor; los registros permanecen en MySQL. Para detenerlo de manera ordenada usa `Ctrl+C`.

## Prueba inmediata sin configurar MySQL

Abre **`iniciar-demo.cmd`** y entra a http://localhost:8080 con las mismas credenciales. Este modo usa H2, una base local en la carpeta `data`, y sirve para recorrer las pantallas antes de configurar MySQL. Sus datos son independientes de la base MySQL y también permanecen al reiniciar. Usa solo un modo a la vez en el puerto 8080.

En Linux/macOS: `./iniciar.sh` para MySQL o `./iniciar.sh --demo` para H2. Java debe estar instalado.

## Recorrido de prueba de una venta

1. **Ventas y atención → Aperturas y cierres:** crea una apertura para «Caja del restaurante» con **S/ 300.00**, su saldo inicial de demostración.
2. **Tablero de mesas:** elige una mesa libre, selecciona «Almacén principal», agrega una línea de «Trucha frita» y guarda.
3. **Comanda cocina:** pulsa «Preparar» y luego «Terminado».
4. **Comanda salón:** pulsa «Entregar».
5. Regresa a la mesa ocupada, pulsa «Enviar a caja» y escribe `TUMI10` si quieres probar el descuento del 10 %.
6. Pulsa «Cobrar pedido», selecciona «Boleta», «Contado» y «Caja del restaurante». Confirma el cobro.
7. Consulta **Comprobantes internos**: abre el detalle con la flecha y descarga el PDF A4, ticket de 80 mm, JSON o XML borrador.
8. Pulsa la mesa pendiente de limpieza y «Marcar mesa libre».

El cobro registra la entrada de dinero, consume los ingredientes de la receta y genera los asientos contables. No admite cobrar dos veces el mismo pedido ni dejar stock negativo.

## Módulos incluidos

| Módulo | Funciones operativas |
|---|---|
| Contabilidad | Plan contable, asientos automáticos y manuales, reversas, períodos cerrados, cuentas por cobrar y pagar, tesorería, transferencias, libro mayor, balances, resultados y antigüedad de saldos. |
| Finanzas | Disponibilidad, proyección simple de caja, vencimientos, presupuestos, control del gasto, activos, depreciación mensual, servicios, obligaciones y pagos desde las cuentas por pagar. |
| Logística | Proveedores, productos, almacenes, compras, recepción parcial con evidencia, existencias, Kardex por costo promedio, mermas y órdenes sugeridas por reposición. |
| Recursos humanos | Expedientes, familiares, contratos, asistencia, ausencias, vacaciones, planillas, honorarios, beneficios, préstamos, adelantos y liquidaciones. |
| Ventas | Áreas y mesas, pedidos, cocina, salón, recetas, carta, descuentos, cotizaciones, anticipos de eventos, caja, ventas al contado/crédito y comprobantes internos. |
| Administración | Usuarios, nueve roles, permisos por módulo y acción, bloqueo de cuentas, cambio de contraseña, autenticación TOTP de dos pasos, datos del negocio y auditoría. |

Las pantallas permiten buscar, filtrar, consultar detalles y exportar. Las acciones de proceso —recibir, pagar, aprobar, depreciar, anular— se encuentran dentro de **Ver detalle** (flecha al costado del registro). La carga CSV de productos, terceros y cuentas tiene una plantilla descargable en cada pantalla.

## Alcance de esta entrega

Los comprobantes son **internos y sin validez tributaria**. El XML es un borrador de integración: esta versión no firma, transmite ni recibe CDR de SUNAT. No almacena credenciales SOL. Para emisión electrónica real se necesita implementar y validar el adaptador tributario con los datos y certificados del negocio.

Los cálculos de RRHH son parametrizados. Horas extras, asignación familiar, tardanzas, quinta categoría y bases computables se revisan e ingresan antes de aprobar. Beneficios y liquidaciones se muestran como estimaciones, con los parámetros del contrato. El documento [ALCANCE.md](docs/ALCANCE.md) detalla las simplificaciones y las integraciones externas pendientes.

## Desarrollo: React y backend separados

Para modificar el código necesitas **JDK 17/21, Maven 3.9 y Node 22.12 o superior**.

Primera terminal, desde la carpeta del proyecto:

```powershell
cd backend
mvn spring-boot:run
```

Segunda terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Abre http://localhost:5173. Vite redirige `/api` al backend en el puerto 8080; no necesitas cambiar el frontend para conectar con MySQL. El backend separado usa por defecto el usuario `tumi` y la clave del script SQL. Si usas otra conexión, configura `DB_URL`, `DB_USER` y `DB_PASSWORD` como variables de entorno de la terminal del backend, o pasa `--spring.config.additional-location=optional:file:../config/` al ejecutar el JAR desde `backend`.

No ejecutes simultáneamente `iniciar.cmd` y `mvn spring-boot:run` en el mismo puerto. El primero ya sirve el backend y React.

Para crear nuevamente el ejecutable después de hacer cambios:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

En Linux/macOS: `./scripts/build.sh`. El script compila React, lo integra en Spring Boot, ejecuta las pruebas y actualiza `runtime/el-tumi-erp.jar`.

## Docker Desktop

Con Docker Desktop iniciado y usando contenedores Linux, ejecuta desde la raíz:

```powershell
docker compose up --build -d
docker compose logs -f app
```

Luego abre http://localhost:8080. MySQL queda publicado en `localhost:3307`. No requiere instalar Java, MySQL, Maven o Node en Windows; las imágenes se descargan en la primera ejecución. Puedes copiar `.env.example` a `.env` para cambiar puertos y claves.

Para detener: `docker compose down`. Los volúmenes conservan datos y adjuntos. No agregues `-v` si quieres conservarlos. La opción Docker está preparada; consulta las verificaciones ejecutadas en `docs/VERIFICACION.md`.

## Asistente local opcional

El botón «Consultar análisis» del tablero funciona por reglas y datos reales aun sin modelo de IA. Si ya tienes Ollama, descarga el modelo con `ollama pull qwen2.5:3b` y configura `app.ollama-enabled=true` en `config/application.properties`. Reinicia el servidor. El asistente explica saldos agregados y no modifica operaciones. Si Ollama no responde, se conserva el diagnóstico por reglas. El ERP funciona completamente con esta opción desactivada.

## Estructura

| Ruta | Contenido |
|---|---|
| `backend/` | API Spring Boot, seguridad, servicios, migraciones y pruebas. |
| `frontend/` | Interfaz React, estilos, formularios y tableros. |
| `runtime/el-tumi-erp.jar` | Ejecutable con React integrado. |
| `config/` | Plantilla de configuración persistente del arranque local. |
| `database/` | Creación de base/usuario y referencia del esquema. |
| `scripts/` | Arranque y compilación. |
| `docs/` | Manual, alcance, arquitectura y verificación. |
| `samples/` | Ejemplos CSV para carga masiva. |

## Problemas frecuentes

- **`Access denied for user`:** revisa el usuario y clave de MySQL en `config/application.properties`. Si el usuario `tumi` ya existía, el SQL no cambia su contraseña.
- **`Connection refused`:** inicia MySQL y confirma el puerto; el modo local espera 3306, mientras que Docker publica 3307.
- **Puerto 8080 ocupado:** cierra otra instancia o ejecuta `powershell -File .\scripts\start.ps1 -Port 8081` y abre http://localhost:8081.
- **Pantalla sin conexión en desarrollo:** confirma que Spring Boot esté iniciado y usa la URL de Vite indicada en la terminal. Si cambias el puerto del backend, ajusta `API_PROXY_TARGET` antes de arrancar Vite.
- **Cambios de React no aparecen en `localhost:8080`:** recompila con `scripts/build.ps1`. Durante el desarrollo usa el puerto 5173.
- **No puedes enviar un pedido a caja:** primero termina y entrega todos sus platos; los devueltos sin reposición quedan fuera de la cuenta.
- **No puedes cobrar en efectivo:** abre esa cuenta de caja y comprueba el stock de todos los ingredientes.
- **Registro relacionado que no se elimina:** conserva su historial y desactívalo o usa la acción de anulación prevista.
- **Los parámetros de administrador no cambian el usuario existente:** los valores del archivo se usan solo al inicializar una base vacía. Cambia la contraseña desde «Mi cuenta» o administra el usuario desde su pantalla.

Para conservar el trabajo, respalda **MySQL y `private-files` juntos**. Para el modo H2 respalda `data` y `private-files` con el servidor detenido. Las claves incluidas son de demostración; cámbialas antes de usar datos reales o exponer el sistema.
