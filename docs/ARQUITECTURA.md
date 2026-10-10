# Arquitectura técnica

- **Backend:** Spring Boot 3.5.16, Java 17 como nivel de compilación, Spring MVC, Security y JDBC (`JdbcTemplate`). Java 21 puede ejecutar el JAR.
- **Persistencia:** MySQL 8.0, Flyway y 54 tablas de negocio; Flyway agrega su tabla de historial. H2 se usa en las pruebas y el modo demo independiente.
- **Interfaz:** React 19.2, Vite 7.1.12, iconos Lucide y gráficos Recharts. Navegación mediante hash para poder servir el frontend directamente desde Spring Boot.
- **Documentos:** OpenPDF para PDF y Apache POI para XLSX; CSV UTF-8.
- **Autenticación:** tokens aleatorios de sesión de 8 horas. Solo se guarda su hash SHA-256; las contraseñas usan BCrypt. No usa JWT. Las autorizaciones se comprueban en cada operación.
- **Tiempo:** días operativos de America/Lima; el arranque de la aplicación usa esa zona y las conexiones MySQL fijan `-05:00`.

## Servicios

| Clase | Responsabilidad |
|---|---|
| `ErpService` | Validación de campos, cambios autorizados y límites de transacción de las operaciones. |
| `Inventory` | Compras, recepción, costo promedio, stock y consumo de recetas. |
| `Sales` | Pedidos, comandas, cotizaciones, anticipos, cobros y anulaciones. |
| `Ledger` | Partida doble, tesorería, pagos, deuda y presupuesto. |
| `HumanResources` | Contratos vigentes, planillas, préstamos, anticipos, honorarios y beneficios. |
| `ReportingController` | Estados contables, mayor y exportaciones de reportes. |
| `Catalog` | Lista cerrada de entidades/campos y metadatos de formularios; `catalog.json` sincroniza API e interfaz. |
| `Store` | Acceso JDBC y utilidades de valores. Las tablas/columnas de entradas genéricas se resuelven a través del catálogo. |
| `Access`, `Security`, `AuthController` | Autorización, sesiones, contraseñas, roles y TOTP. |
| `Bootstrap` | Datos base e inicialización de demostración, ejecutada una vez y en transacción. |

## Consistencia

Los cambios financieros y de inventario se realizan dentro de transacciones de Spring. Las mutaciones toman un bloqueo sobre la fila de empresa y sobre saldos cuando corresponde; esto evita pagar o cobrar concurrentemente más de lo permitido. La edición usa una versión de registro para rechazar escrituras basadas en datos antiguos. El bloqueo de empresa serializa las operaciones y es una elección para una instalación pequeña; requiere rediseño antes de escalar a múltiples locales con carga elevada.

El stock se consume al cobrar, de acuerdo con el requerimiento, y en el momento de registrar mermas/devoluciones. No se reserva al tomar el pedido; si varios pedidos exceden la disponibilidad, el último cobro falla y conserva la operación sin cambios parciales. Los pedidos guardan una copia de su receta y los comprobantes guardan copias de sus líneas y datos de empresa.

## API principal

| Método y ruta | Uso |
|---|---|
| `POST /api/auth/login` | Inicia sesión; devuelve `token`, usuario y permisos. |
| `GET /api/auth/me` | Consulta usuario autenticado. |
| `GET /api/catalog`, `GET /api/lookups` | Metadatos autorizados y referencias de formularios. |
| `GET/POST /api/data/{entity}` | Consulta paginada / creación validada. |
| `GET/PUT/DELETE /api/data/{entity}/{id}` | Detalle, edición con `version`, eliminación permitida. |
| `POST /api/actions/{entity}/{id}/{action}` | Transiciones y operaciones de proceso. |
| `POST /api/order-lines/{id}/{action}` | Preparar, terminar, entregar, merma y devolver. |
| `POST /api/orders/{id}/lines` | Agrega platos a un pedido abierto. |
| `POST /api/journals`, `/api/stock-moves`, `/api/transfers` | Asientos, movimientos y transferencias manuales. |
| `GET /api/board`, `/api/dashboard` | Tablero de atención y métricas según permisos. |
| `GET /api/financial-reports/{kind}` | `trial`, `balance`, `income` o `ledger`; filtros `from`, `to`, `accountId`. |
| `GET /api/financial-reports/{kind}/export` | `format=pdf`, `xlsx` o `csv`. |
| `GET /api/exports/{entity}.{format}` | Exportación de registros filtrados. |
| `GET /api/documents/{entity}/{id}.pdf` | Documento individual; en comprobantes acepta `paper=80mm`. |
| `POST /api/imports/{entity}` | Multipart CSV de `products`, `partners` o `accounts`. |
| `POST /api/files`, `GET /api/files/{id}` | Adjuntos privados por módulo. |
| `POST /api/insights` | Diagnóstico por reglas o modelo local. |
| `GET /actuator/health` | Comprobación de salud pública sin datos sensibles. |

Las rutas protegidas esperan `Authorization: Bearer <token>`. No se debe guardar un token ni una clave de administrador dentro del código frontend. La interfaz mantiene la sesión en `sessionStorage`.

## Pruebas

En `backend`, ejecuta `mvn test`. La batería utiliza H2 por defecto. Para probar en MySQL crea una base **exclusiva y vacía para pruebas**, porque el recorrido registra compras, ventas y planillas. Puedes sobrescribir `spring.datasource.url`, `spring.datasource.username` y `spring.datasource.password` como propiedades de Maven. Nunca dirijas las pruebas a una base de uso real.
