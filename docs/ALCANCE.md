# Alcance y correspondencia con los requisitos

La entrega implementa los seis módulos y sus flujos centrales con persistencia y controles transaccionales. No equivale a un sistema tributario homologado ni a una automatización integral de todas las excepciones laborales y contables.

| Área del documento | Implementación en esta entrega | Límite concreto |
|---|---|---|
| Plan contable, diario y reportes | Catálogo jerárquico, importación, asientos de partida doble, reversas, cierre mensual, mayor, situación financiera, resultados y comprobación. | Una empresa y moneda PEN; mapeos de cuentas automáticas definidos en los servicios. No genera libros electrónicos PLE. |
| CxC y CxP | Origen manual o por proceso, pagos parciales, control de saldo, evidencia, reprogramación, notas internas sobre cuentas manuales y antigüedad. | No calcula detracciones/retenciones comerciales, castigos ni notas de crédito tributarias automáticas. |
| Tesorería y caja chica | Cuentas por medio de pago, movimientos, transferencias, apertura/cierre y arqueo. | Registro interno; no importa estados bancarios ni concilia automáticamente con un banco. Reposición mediante transferencia. |
| Flujo de caja y KPIs | Disponibilidad, compromisos registrados, vencimientos, resultado, ticket promedio, ventas diarias y clasificación por ingreso de platillos. | Proyección simple actual; no modelo semanal de predicción ni pronóstico de ventas futuras. Ranking por ingreso, no por margen unitario. |
| Presupuestos | Partidas por área/período, edición, ejecución de compras/activos/servicios y bloqueo de exceso. | Sin versionado de escenarios ni traslado automático entre partidas. |
| Activos y gastos | Fichas, documentos, deuda integrada, depreciación por período y baja. | Depreciación confirmada por usuario; mantenimientos recurrentes se registran al ocurrir, no se crean solos. |
| Logística | Catálogo, proveedores, órdenes, recepción parcial con evidencia, stock por almacén, Kardex valorizado, mermas y compra sugerida. | Costo promedio ponderado; un proveedor principal por regla; cantidades convertidas previamente a unidad base. |
| Empleados, contratos y asistencia | Expedientes, familiares, adjuntos, contratos con vigencia, asistencia manual y resumen de tardanzas/horas extra. | Sin reloj biométrico; la asistencia no convierte automáticamente todos los conceptos en dinero de planilla. |
| Vacaciones y permisos | Solicitudes, aprobación/rechazo, días y validación proporcional de vacaciones. | Acumulación basada en contrato activo; no reconstruye automáticamente saldos históricos de contratos anteriores ni compensa horas sin revisión. |
| Planillas y beneficios | Borrador, descuentos de anticipos/cuotas, aprobación, CxP, asiento, fichas PDF, beneficios y liquidación estimados. | Quinta categoría y bases variables ingresadas por responsable. Sin declaración PLAME, AFPnet, SENATI automático ni todas las particularidades de cada régimen. |
| Honorarios | Contrato, reajuste del borrador, retención parametrizada por las reglas solicitadas, adjunto, cancelación y envío a Finanzas. | La suspensión se indica en el formulario; no se valida en SUNAT. |
| Préstamos y adelantos | Límites acumulados, cronograma, desembolso, descuento y pago anticipado, PDF/no adeudo interno. | No integra firma electrónica, cobranza bancaria ni refinanciación de préstamos activos. |
| Ventas y atención | Mesas, cocina, salón, recetas preservadas, descuentos, mermas/devolución, contado/crédito, anticipos de eventos y anulación con devolución. | Un medio de pago por cobro principal; no pagos divididos en varios medios dentro de un mismo comprobante. |
| Comprobantes | Numeración transaccional, PDF A4 y 80 mm, JSON e XML borrador. | Sin firma XML, envío, CDR, bajas SUNAT ni catálogo tributario completo. Solo gestión interna. |
| Administración | Usuarios, nueve roles, permisos por módulo y acción, TOTP, bloqueo y auditoría. | La matriz no distingue cada submódulo ni separa crear/editar/eliminar. No recuperación por correo; usuarios se desactivan en vez de borrarse. |
| IA | Diagnóstico por reglas y conexión opcional a Ollama con datos agregados. | No ejecuta operaciones ni sustituye aprobaciones. El modelo no viene incluido. |

Los límites de S/ 700 y S/ 2 000, la TCEA de 6 % y los topes de préstamo/adelanto reproducen reglas del requerimiento del proyecto. No constituyen una afirmación de normativa vigente. Los parámetros laborales e impositivos deben revisarse para la empresa y período que se vaya a utilizar.

Los datos iniciales —empresa, proveedor, empleado, saldos y documentos— están identificados como demostración. No se incluyen claves reales, certificados, SOL ni información de producción.
