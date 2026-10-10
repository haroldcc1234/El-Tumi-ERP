# Manual de operaciones

## Navegación y archivos

Los módulos están en el menú lateral. La flecha de cada fila abre el detalle completo con sus botones de proceso. Los formularios muestran errores de validación; una operación que falla no debe corregirse modificando saldos directamente en MySQL. Corrige el dato del formulario y reintenta.

PDF, PNG y JPEG de hasta 10 MB se guardan como evidencias privadas. Para cambiar un respaldo después del registro, abre el detalle y utiliza «Actualizar documento de respaldo». Las fotos de platillos y el logo se cargan desde el formulario de edición correspondiente.

## Compras, recepción y pagos

1. Registra el proveedor en «Clientes y proveedores» y los insumos en «Insumos y productos». El costo de compra por línea se ingresa **sin IGV** y cada insumo tiene su unidad base.
2. Crea una orden de compra con proveedor, almacén, fechas y cantidades. Puedes asociarla a un presupuesto. Agrega todas las líneas antes de guardarla.
3. Abre el detalle y pulsa «Enviar orden».
4. Pulsa «Recibir productos». Adjunta la evidencia, indica el documento del proveedor y ajusta las cantidades realmente recibidas. Para omitir una línea en esa recepción, usa cantidad cero.
5. La recepción actualiza el Kardex, el costo promedio, las existencias y el presupuesto, y genera una cuenta por pagar por la cantidad recibida. Las siguientes recepciones solo pueden registrar el saldo pendiente.
6. En «Cuentas por pagar», abre cada documento, pulsa «Pagar aquí», elige la cuenta de origen e indica el monto. Puedes hacer pagos parciales; no se permite superar el saldo ni el dinero disponible.
7. Una compra completamente recibida pasa a «Pagado» cuando todas las cuentas de sus recepciones están pagadas.

El código, nombre y unidad base de un insumo deben ser coherentes con sus recetas. Las salidas se valorizan con costo promedio ponderado. Unidades de compra distintas requieren convertir previamente las cantidades a la unidad base del producto.

## Kardex y reabastecimiento

«Kardex → Nuevo registro» admite ingreso, salida, merma y ajustes de entrada/salida. Usa cantidades positivas y deja que el tipo determine el sentido. La fecha puede ser hoy o hasta cuatro días anteriores. Un período contable cerrado bloquea los asientos que le correspondan.

Las alertas de stock se muestran en el tablero cuando el saldo llega a mínimo + 5 o al máximo. En el detalle de una regla, «Generar compra sugerida» crea una orden borrador para completar el máximo; revisa precio, cantidad y proveedor antes de enviarla.

## Pedidos, cocina y caja

Cada pedido conserva el precio y la receta que tenía el platillo al agregarlo. Editar una receta no altera pedidos existentes. Las comandas se actualizan cada 12 segundos y los usuarios de cocina y salón operan con sus roles.

La merma descuenta los ingredientes de esa preparación y reinicia su comanda. La devolución descuenta la preparación devuelta; puedes volver a prepararla o retirarla del pedido. El cobro consume únicamente las preparaciones finales que siguen en la cuenta. Anular un comprobante devuelve el dinero permitido por el flujo y revierte el ingreso contable; no repone alimentos ya preparados.

«Enviar a caja» requiere platos entregados o cancelados. El descuento se aplica por código sobre todos los platos o sobre el platillo configurado. En una cotización se conserva el descuento originalmente acordado.

La condición contado cobra todo el saldo. La condición crédito permite un abono y crea una cuenta por cobrar por el resto, con cliente identificado y vencimiento. Los límites iniciales de identificación y pago digital provienen del requerimiento y son editables en «Datos del negocio».

El efectivo requiere una caja abierta. Tarjeta, banco y Yape se registran como cuentas de tesorería; registrar un cobro no procesa una transacción bancaria externa. El cierre guarda el efectivo esperado, el contado y la diferencia. Las diferencias se regularizan mediante un movimiento autorizado, sin reescribir el saldo del cierre.

## Eventos

Crea la cotización con platillos, cantidades, descuento y alquiler. Acéptala y registra sus adelantos antes de convertirla en pedido. Al convertir, selecciona el almacén; aparecerá en el tablero como un pedido sin mesa. Cocina, entrega y cobra el evento como cualquier pedido. Al cobrar solo se exige el saldo después de los adelantos.

Una cotización sin convertir puede anularse con motivo y sus anticipos se devuelven a las cuentas que los recibieron. Si ya existe un pedido, anula desde ese pedido; si ya está cobrado, desde el comprobante. Se necesita saldo suficiente para devolver el dinero. La operación es transaccional: si falla una devolución, no se procesa una parte de la anulación.

Comprobantes a crédito con cobros posteriores requieren conciliar esos cobros antes de anular: el sistema bloquea la anulación directa para evitar devolver importes equivocados.

## Finanzas

Define partidas con fecha de vigencia, área y monto. Compras recibidas, activos y servicios asociados consumen la partida. El sistema rechaza una operación que exceda su saldo. La edición tampoco puede bajar un presupuesto por debajo de lo ejecutado.

Registrar un activo genera su deuda y asiento. «Depreciar mes» contabiliza una cuota proporcional a costo menos residual / vida útil; no se repite un mismo mes. «Dar de baja» retira el activo y su depreciación acumulada. La depreciación se confirma mes a mes, no mediante un proceso automático en segundo plano.

Los servicios y obligaciones crean cuentas por pagar, desde las que Finanzas confirma los desembolsos. El tablero muestra saldos, vencimientos y proyección simple de compromisos. Esta proyección no estima ventas que aún no fueron registradas.

## Recursos humanos

1. Crea el expediente del empleado y sus familiares por separado, vinculados al mismo empleado.
2. Registra un contrato vigente con salario, régimen y parámetros de vacaciones, gratificación, CTS, pensión y aporte del empleador. No se admiten contratos activos superpuestos.
3. Registra asistencia y solicitudes de ausencia. Para vacaciones, al aprobar se valida el saldo proporcional según el contrato activo y las vacaciones aprobadas.
4. Crea una planilla por empleado y mes. Completa las sumas y descuentos variables. El sistema incorpora adelantos ya entregados y cuotas de préstamos activos pendientes hasta el período elegido.
5. Revisa el borrador y apruébalo. Se genera una cuenta por pagar por el neto y el asiento correspondiente; los descuentos quedan aplicados una sola vez.
6. Confirma el pago neto desde Cuentas por pagar. Las retenciones y aportes quedan contablemente reconocidos; registra sus obligaciones de pago a terceros según la liquidación revisada por el área responsable.

Los préstamos usan la TCEA fija del 6 % indicada en el requerimiento y un cronograma mensual. Se valida que las cuotas acumuladas no superen el 45 % del sueldo. Los adelantos del mismo mes no pueden superar el 40 %. Ambos se activan para descuento después de que Finanzas desembolsa su cuenta por pagar completa. El pago anticipado de un préstamo recibe el capital pendiente y cancela las cuotas futuras.

Los honorarios pueden reajustarse en borrador desde «Editar», recalculando retención y neto. Adjunta el recibo y aprueba desde dos días antes del vencimiento. Después de aprobar, se conserva el documento y su trazabilidad.

Beneficios y liquidaciones requieren ingresar los días y meses computables revisados. No se determinan automáticamente todos los supuestos legales de cada régimen. Una liquidación aprobada inactiva al empleado y finaliza sus contratos; no se aprueba con préstamos pendientes. Puedes descargar la ficha PDF desde cada detalle, incluida la constancia interna de no adeudo de un préstamo pagado.

## Contabilidad y reportes

El plan contable contiene las cuentas usadas por los asientos automáticos. Se puede ampliar e importar, pero no cambiar el código o tipo de una cuenta con movimientos. Los códigos del catálogo base se usan como mapeo contable de los procesos; modificar ese mapeo requiere ajustar el servicio correspondiente.

Los asientos manuales deben tener al menos dos líneas y cuadrar. Una reversa genera otro asiento; no elimina el original. El cierre mensual bloquea nuevos asientos de ese mes. Solo pueden cerrarse meses anteriores al actual.

En «Reportes contables» encontrarás comprobación, situación financiera, resultados, libro mayor por cuenta y antigüedad. El libro mayor muestra saldo inicial, movimientos y saldo final. La situación financiera acumula todos los asientos hasta el corte e incorpora al patrimonio el resultado acumulado aún reflejado en cuentas de gestión. Los reportes son internos, en PEN.

## Usuarios y roles

El administrador inicial puede crear usuarios con los roles CONTADOR, FINANZAS, ALMACEN, RRHH, CAJA, SALON, COCINA, MARKETING y ADMIN. Los permisos de ver, registrar/editar, aprobar/anular y exportar se revisan en el servidor. Cocina y salón además restringen sus acciones de comanda por rol.

Para retirar acceso, cambia el estado a inactivo o bloqueado. No se eliminan los usuarios para conservar referencias y auditoría. «Mi cuenta» permite cambiar contraseña y configurar un autenticador TOTP mediante clave manual. Conserva esa clave en tu autenticador; no se incluye un servicio de recuperación por correo.
