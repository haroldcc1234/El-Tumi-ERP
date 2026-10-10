# Verificación de la entrega

Revisión ejecutada el 3 de octubre de 2026, hora de Lima. Se utilizaron datos de prueba en bases independientes; ninguna de esas bases se incluye en el paquete. Al iniciar se genera una demostración limpia.

## Procesos con MySQL 8.0.36

**16 pruebas, 0 fallos, 0 errores.** La batería ejecuta Spring Boot y sus controladores contra un servidor MySQL real. El resumen de Maven se conserva en `resultado_pruebas_mysql.txt`.

1. Autenticación, CORS, separación de roles y ocultamiento de credenciales.
2. Compra y recepción parcial, stock, cuentas por pagar, pago final y asiento balanceado.
3. Rechazo de pagos mayores a la deuda o al saldo de tesorería.
4. Mesa → cocina → salón → descuento → cobro → limpieza.
5. Merma y devolución sin descontar dos veces la misma preparación.
6. Falta de stock: reversión completa del cobro fallido.
7. Adelanto y préstamo: desembolso y aplicación una sola vez en planilla.
8. Presupuesto insuficiente y edición basada en una versión antigua.
9. Generación de PDF, XLSX, CSV, ticket 80 mm y XML bien formado.
10. Dos pagos concurrentes: solo uno consume el saldo disponible.
11. Bloqueo de asientos en un mes cerrado.
12. Cuadre de situación financiera y comprobación; mayor conciliado con caja; exportación de resultados.
13. Importación CSV atómica y rechazo de códigos duplicados.
14. Consulta de las 41 entidades y de los tableros principales.
15. Evento con adelanto, venta y anulación: devolución de cada pago exactamente una vez.
16. Reajuste de honorarios en borrador y recálculo de su neto.

## Ejecutable e interfaz integrada

- Compilación de producción de Vite completada.
- Arranque del JAR con MySQL, migración e inicialización de una base nueva completados.
- Respuestas HTTP correctas del inicio, JavaScript y CSS sin exigir autenticación para mostrar el formulario de acceso.
- Recorrido automatizado de **49 vistas** en un entorno DOM con el backend real.
- Creación de un producto desde su formulario React; consulta de la tabla resultante.
- Apertura del modal de importación CSV y disponibilidad de la plantilla.
- Navegación entre situación financiera, resultados y mayor, incluyendo selección de cuenta.

La revisión de interfaz usa un entorno DOM automatizado, no una inspección visual en Chrome. Se verificó la carga y la interacción descrita; no se certifican todas las combinaciones de pantalla ni de navegador.

## Reproducción

`cd backend` y `mvn test` ejecutan la batería con H2, sin requerir MySQL. El informe H2 final se incluye por separado. Para MySQL se debe usar una base de pruebas vacía y exclusiva, sobrescribiendo las propiedades de conexión descritas en `ARQUITECTURA.md`.

Los scripts Bash se revisaron sintácticamente y Compose se validó como YAML. No se ejecutó Docker Desktop ni PowerShell de Windows en este entorno. Ollama, la emisión SUNAT, los servicios bancarios y las declaraciones laborales no se validaron como integraciones externas; consulta `ALCANCE.md`.
