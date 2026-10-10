package pe.eltumi.erp;

import org.springframework.stereotype.Service;
import java.util.*;
import java.math.*;
import java.time.*;
import static pe.eltumi.erp.Store.*;

@Service
public class Sales {
    final Store db;
    final Ledger ledger;
    final Inventory inventory;

    public Sales(Store d, Ledger l, Inventory i) {
        db = d;
        ledger = l;
        inventory = i;
    }

    public long createOrder(Map<String, Object> b, Map<String, Object> clean) {
        long table = id(clean, "table_id");
        if (table > 0) {
            var t = db.lock("dining_tables", table);
            require(str(t, "status").equals("LIBRE"), "La mesa no está libre");
            db.update("dining_tables", table, map("status", "OCUPADA"));
        }
        clean.put("status", "ABIERTO");
        clean.put("total", 0);
        clean.put("discount", 0);
        clean.put("rental", 0);
        clean.put("quote_id", null);
        long id = db.insert("orders", clean);
        addLines(id, lines(b));
        return id;
    }

    public void addLines(long order, List<Map<String, Object>> lines) {
        var o = db.lock("orders", order);
        require(str(o, "status").equals("ABIERTO"), "El pedido ya está en caja");
        for (var line : lines) {
            long dish = id(line, "dish_id");
            var d = db.get("dishes", dish);
            require(str(d, "status").equals("ACTIVO"), "Platillo inactivo");
            var qty = dec(line, "quantity");
            positive(qty);
            require(qty.scale() <= 0 || qty.stripTrailingZeros().scale() <= 0, "La cantidad de platos debe ser entera");
            db.insert("order_lines",
                    map("order_id", order, "dish_id", dish, "quantity", qty, "unit_price", dec(d, "price"), "tax_rate",
                            str(d, "tax_type").equals("GRAVADO") ? dec(d, "tax_rate") : 0, "discount", 0, "status",
                            "INGRESADO", "notes", str(line, "notes"), "recipe_snapshot", inventory.recipe(dish)));
        }
        totals(order);
    }

    public void totals(long id) {
        var o = db.get("orders", id);
        BigDecimal total = dec(o, "rental"), discount = BigDecimal.ZERO;
        for (var l : db.rows("SELECT * FROM order_lines WHERE order_id=? AND status<>'CANCELADO'", id)) {
            var sub = money(dec(l, "unit_price").multiply(dec(l, "quantity")));
            total = total.add(sub.subtract(dec(l, "discount")));
            discount = discount.add(dec(l, "discount"));
        }
        db.update("orders", id, map("total", money(total), "discount", money(discount)));
    }

    public void line(long id, String action, Map<String, Object> b) {
        var l = db.one("SELECT * FROM order_lines WHERE id=? FOR UPDATE", id);
        var o = db.lock("orders", id(l, "order_id"));
        require(str(o, "status").equals("ABIERTO"), "El pedido ya está en caja o cerrado");
        String state = str(l, "status"), next;
        switch (action) {
            case "cook" -> {
                require(state.equals("INGRESADO"), "Estado inválido");
                next = "PROCESADO";
            }
            case "ready" -> {
                require(state.equals("PROCESADO"), "Estado inválido");
                next = "TERMINADO";
            }
            case "deliver" -> {
                require(state.equals("TERMINADO"), "Estado inválido");
                next = "ENTREGADO";
            }
            case "waste" -> {
                require(List.of("PROCESADO", "TERMINADO").contains(state),
                        "Solo se registra merma durante la preparación");
                require(!str(b, "reason").isBlank(), "Indica el motivo de la merma");
                inventory.consume(l, id(o, "warehouse_id"), "MERMA", "MERMA:" + id + ":" + UUID.randomUUID());
                next = "INGRESADO";
            }
            case "return" -> {
                require(state.equals("ENTREGADO"), "Solo se devuelve un plato entregado");
                require(!str(b, "reason").isBlank(), "Indica el motivo de devolución");
                inventory.consume(l, id(o, "warehouse_id"), "MERMA", "DEVOLUCION:" + id + ":" + UUID.randomUUID());
                next = Boolean.TRUE.equals(b.get("reprocess")) ? "INGRESADO" : "CANCELADO";
            }
            default -> throw new Problem(422, "Acción inválida");
        }
        db.exec("UPDATE order_lines SET status=? WHERE id=?", next, id);
        totals(id(l, "order_id"));
    }

    public void bill(long id, Map<String, Object> b) {
        var order = db.lock("orders", id);
        require(str(order, "status").equals("ABIERTO"), "El pedido ya fue enviado a caja");
        require(db.count(
                "SELECT COUNT(*) FROM order_lines WHERE order_id=? AND status NOT IN ('ENTREGADO','CANCELADO')",
                id) == 0, "Entrega todos los platos antes de solicitar el pago");
        String code = str(b, "discount_code").toUpperCase();
        if (!code.isBlank()) {
            require(id(order, "quote_id") == 0, "Las cotizaciones conservan el descuento acordado");
            var d = db.one("SELECT * FROM discounts WHERE code=? AND status='ACTIVO' AND start_date<=? AND end_date>=?",
                    code, today(), today());
            for (var l : db.rows("SELECT * FROM order_lines WHERE order_id=? AND status<>'CANCELADO'", id)) {
                var amount = id(d, "dish_id") == 0 || id(d, "dish_id") == id(l, "dish_id") ? money(dec(l, "quantity")
                        .multiply(dec(l, "unit_price")).multiply(dec(d, "percent")).divide(new BigDecimal("100")))
                        : BigDecimal.ZERO;
                db.exec("UPDATE order_lines SET discount=? WHERE id=?", amount, id(l, "id"));
            }
            db.update("orders", id, map("discount_code", code));
        }
        totals(id);
        require(dec(db.get("orders", id), "total").signum() > 0, "El pedido está vacío o tiene total cero");
        db.update("orders", id, map("status", "PAGAR"));
        if (id(order, "table_id") > 0)
            db.update("dining_tables", id(order, "table_id"), map("status", "PAGAR"));
    }

    public Map<String, Object> checkout(long id, Map<String, Object> b) {
        var o = db.lock("orders", id);
        require(str(o, "status").equals("PAGAR"), "El pedido no está pendiente de pago");
        var company = db.get("company", 1);
        var total = dec(o, "total");
        String type = str(b, "document_type"), doc = str(b, "document"), customer = str(b, "customer");
        require(List.of("BOLETA", "FACTURA").contains(type), "Tipo de comprobante inválido");
        if (type.equals("FACTURA"))
            require(doc.matches("\\d{11}") && !customer.isBlank(), "Factura: ingresa RUC de 11 dígitos y razón social");
        else if (total.compareTo(dec(company, "id_threshold")) >= 0 || !doc.isBlank())
            require(doc.matches("\\d{8}"), "Ingresa DNI de 8 dígitos");
        if (!doc.isBlank())
            require(!customer.isBlank(), "Ingresa el nombre del cliente");
        String payment = str(b, "payment_type");
        require(List.of("CONTADO", "CREDITO").contains(payment), "Condición de pago inválida");
        BigDecimal advance = BigDecimal.ZERO;
        if (id(o, "quote_id") > 0)
            advance = dec(db.get("quotes", id(o, "quote_id")), "advance");
        require(advance.compareTo(total) <= 0, "El adelanto supera el total del pedido");
        BigDecimal due = total.subtract(advance);
        BigDecimal paid = payment.equals("CONTADO") ? due : money(dec(b, "amount"));
        require(paid.signum() >= 0 && paid.compareTo(due) <= 0, "Abono inválido");
        long cash = id(b, "cash_account_id");
        if (paid.signum() > 0) {
            var account = db.get("cash_accounts", cash);
            if (total.compareTo(dec(company, "digital_threshold")) >= 0)
                require(!str(account, "kind").equals("EFECTIVO"),
                        "Para este importe se requiere un método de pago digital");
            if (str(account, "kind").equals("EFECTIVO"))
                require(db.count("SELECT COUNT(*) FROM cash_sessions WHERE cash_account_id=? AND status='ABIERTA'",
                        cash) > 0, "Abre la caja antes de cobrar en efectivo");
        }
        String series = type.equals("FACTURA") ? "F001" : "B001";
        var seq = db.one("SELECT * FROM document_sequences WHERE series=? FOR UPDATE", series);
        long number = id(seq, "next_value");
        db.exec("UPDATE document_sequences SET next_value=next_value+1 WHERE series=?", series);
        List<Map<String, Object>> snapshots = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO, tax = BigDecimal.ZERO;
        for (var l : db.rows(
                "SELECT l.*,d.name,d.code,d.tax_type FROM order_lines l JOIN dishes d ON d.id=l.dish_id WHERE order_id=? AND l.status<>'CANCELADO'",
                id)) {
            var amount = money(dec(l, "quantity").multiply(dec(l, "unit_price")).subtract(dec(l, "discount")));
            var base = amount.divide(BigDecimal.ONE.add(dec(l, "tax_rate").divide(new BigDecimal("100"))), 2,
                    RoundingMode.HALF_UP);
            var igv = amount.subtract(base);
            l.put("base", base);
            l.put("tax", igv);
            l.put("total", amount);
            snapshots.add(l);
            subtotal = subtotal.add(base);
            tax = tax.add(igv);
            inventory.consume(l, id(o, "warehouse_id"), "SALIDA", "VENTA:" + id);
            db.exec("UPDATE order_lines SET consumed=TRUE WHERE id=?", id(l, "id"));
        }
        if (dec(o, "rental").signum() > 0) {
            var amount = dec(o, "rental");
            var base = amount.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
            var igv = amount.subtract(base);
            subtotal = subtotal.add(base);
            tax = tax.add(igv);
            snapshots.add(map("code", "ALQUILER", "name", "Alquiler de local", "quantity", 1, "unit_price", amount,
                    "tax_rate", 18, "tax_type", "GRAVADO", "discount", 0, "base", base, "tax", igv, "total", amount));
        }
        long invoice = db.insert("invoices",
                map("number", series + "-" + String.format("%08d", number), "series", series, "correlative", number,
                        "order_id", id, "customer", customer.isBlank() ? "Consumidor final" : customer, "document", doc,
                        "document_type", type, "payment_type", payment, "date", today(), "subtotal", subtotal, "tax",
                        tax, "total", total, "status", "EMITIDO", "cash_account_id", cash > 0 ? cash : null, "paid_now",
                        paid, "address", str(b, "address"), "company_snapshot", db.json(company), "lines_snapshot",
                        db.json(snapshots)));
        if (paid.signum() > 0)
            ledger.cash(cash, paid, "INGRESO", "Venta " + series + "-" + number, "invoices:" + invoice, "");
        BigDecimal remaining = due.subtract(paid);
        if (remaining.signum() > 0) {
            require(!doc.isBlank() && !customer.isBlank(), "El crédito requiere cliente identificado");
            LocalDate limit = LocalDate.parse(str(b, "due_date"));
            require(!limit.isBefore(today()), "El vencimiento no puede ser anterior a la emisión");
            ledger.debt("receivables", customer, doc, remaining, limit, "invoices", invoice,
                    "Venta " + series + "-" + number);
        }
        var entries = new ArrayList<Map<String, Object>>();
        if (paid.signum() > 0)
            entries.add(ledger.entry(ledger.cashCode(cash), paid, BigDecimal.ZERO));
        entries.add(ledger.entry("121", remaining, BigDecimal.ZERO));
        entries.add(ledger.entry("461", advance, BigDecimal.ZERO));
        entries.add(ledger.entry("701", BigDecimal.ZERO, subtotal));
        entries.add(ledger.entry("4011", BigDecimal.ZERO, tax));
        ledger.journal("Venta " + series + "-" + number, "invoices:" + invoice, today(), entries);
        db.update("orders", id, map("status", "PAGADO"));
        if (id(o, "quote_id") > 0)
            db.update("quotes", id(o, "quote_id"), map("status", "PAGADA"));
        if (id(o, "table_id") > 0)
            db.update("dining_tables", id(o, "table_id"), map("status", "LIMPIAR"));
        return db.get("invoices", invoice);
    }

    public long quote(Map<String, Object> b, Map<String, Object> clean) {
        require(dec(clean, "discount").compareTo(new BigDecimal("100")) < 0, "Descuento inválido");
        long id = db.insert("quotes", clean);
        BigDecimal total = dec(clean, "rental");
        for (var line : lines(b)) {
            var d = db.get("dishes", id(line, "dish_id"));
            var qty = dec(line, "quantity");
            positive(qty);
            var price = dec(d, "price");
            db.insert("quote_lines", map("quote_id", id, "dish_id", d.get("id"), "quantity", qty, "unit_price", price,
                    "tax_rate", str(d, "tax_type").equals("GRAVADO") ? dec(d, "tax_rate") : 0));
            total = total.add(money(qty.multiply(price)
                    .multiply(BigDecimal.ONE.subtract(dec(clean, "discount").divide(new BigDecimal("100"))))));
        }
        db.update("quotes", id, map("total", money(total)));
        return id;
    }

    public long convert(long id, Map<String, Object> b) {
        var q = db.lock("quotes", id);
        require(str(q, "status").equals("ACEPTADA"), "La cotización debe estar aceptada");
        long warehouse = id(b, "warehouse_id");
        db.get("warehouses", warehouse);
        long order = db.insert("orders",
                map("table_id", null, "customer", str(q, "customer"), "quote_id", id, "warehouse_id", warehouse, "date",
                        today(), "status", "ABIERTO", "total", 0, "discount", 0, "rental", q.get("rental"), "notes",
                        "Evento " + str(q, "date")));
        for (var l : db.rows("SELECT * FROM quote_lines WHERE quote_id=?", id)) {
            db.insert("order_lines",
                    map("order_id", order, "dish_id", l.get("dish_id"), "quantity", l.get("quantity"), "unit_price",
                            l.get("unit_price"), "tax_rate", l.get("tax_rate"), "discount",
                            money(dec(l, "quantity").multiply(dec(l, "unit_price")).multiply(dec(q, "discount"))
                                    .divide(new BigDecimal("100"))),
                            "status", "INGRESADO", "notes", "Evento", "recipe_snapshot",
                            inventory.recipe(id(l, "dish_id"))));
        }
        totals(order);
        db.update("quotes", id, map("status", "CONVERTIDA", "order_id", order));
        return order;
    }

    public void deposit(long id, Map<String, Object> b) {
        var q = db.lock("quotes", id);
        require(str(q, "status").equals("ACEPTADA"), "Registra el adelanto después de aceptar y antes de convertir");
        var amount = money(dec(b, "amount"));
        positive(amount);
        var next = dec(q, "advance").add(amount);
        require(next.compareTo(dec(q, "total")) <= 0, "El adelanto supera el total");
        long account = id(b, "cash_account_id");
        ledger.cash(account, amount, "INGRESO", "Adelanto de evento #" + id, "quotes:" + id, str(b, "file_id"));
        ledger.simple(ledger.cashCode(account), "461", amount, "Adelanto de cliente", "quotes:" + id);
        db.update("quotes", id, map("advance", next));
    }

    public void cancel(long id, String reason) {
        require(!reason.isBlank(), "Indica el motivo");
        var o = db.lock("orders", id);
        require(List.of("ABIERTO", "PAGAR").contains(str(o, "status")), "El pedido ya está cerrado");
        for (var line : db
                .rows("SELECT * FROM order_lines WHERE order_id=? AND status NOT IN ('INGRESADO','CANCELADO')", id))
            inventory.consume(line, id(o, "warehouse_id"), "MERMA", "CANCELACION:" + id);
        db.exec("UPDATE order_lines SET status='CANCELADO' WHERE order_id=?", id);
        db.update("orders", id, map("status", "ANULADO", "notes", reason));
        if (id(o, "quote_id") > 0)
            refundQuote(id(o, "quote_id"), reason);
        if (id(o, "table_id") > 0)
            db.update("dining_tables", id(o, "table_id"), map("status", "LIMPIAR"));
    }

    public void voidInvoice(long id, String reason) {
        require(reason.length() >= 5, "Detalla el motivo de la anulación");
        var invoice = db.lock("invoices", id);
        require(str(invoice, "status").equals("EMITIDO"), "El comprobante ya fue anulado");
        require(db.count(
                "SELECT COUNT(*) FROM receivables WHERE source='invoices' AND source_id=? AND (paid>0 OR credit>0)",
                id) == 0, "El comprobante tiene cobros o notas aplicadas; regularízalos primero");
        var order = db.get("orders", id(invoice, "order_id"));
        var paid = dec(invoice, "paid_now");
        if (paid.signum() > 0)
            ledger.cash(id(invoice, "cash_account_id"), paid, "EGRESO", "Devolución " + str(invoice, "number"),
                    "ANULACION:" + id, "");
        db.exec("UPDATE receivables SET status='ANULADA',version=version+1 WHERE source='invoices' AND source_id=?",
                id);
        var original = db.one("SELECT * FROM journals WHERE reference=?", "invoices:" + id);
        var reversal = db.rows(
                "SELECT account_id,credit AS debit,debit AS credit FROM journal_lines WHERE journal_id=?",
                id(original, "id"));
        ledger.journal("Anulación interna " + str(invoice, "number"), "ANULACION:" + id, today(), reversal);
        if (id(order, "quote_id") > 0)
            refundQuote(id(order, "quote_id"), reason);
        db.update("invoices", id, map("status", "ANULADO", "cancel_reason", reason));
        db.update("orders", id(invoice, "order_id"), map("status", "ANULADO"));
    }

    public void cancelQuote(long id, String reason) {
        var q = db.lock("quotes", id);
        require(List.of("COTIZAR", "ACEPTADA").contains(str(q, "status")),
                "Cancela el pedido o comprobante asociado a esta cotización");
        require(!reason.isBlank(), "Indica el motivo");
        refundQuote(id, reason);
    }

    void refundQuote(long id, String reason) {
        var q = db.lock("quotes", id);
        require(!str(q, "status").equals("ANULADA"), "Cotización ya anulada");
        BigDecimal refunded = BigDecimal.ZERO;
        for (var payment : db.rows("SELECT * FROM treasury WHERE reference=? AND kind='INGRESO' ORDER BY id",
                "quotes:" + id)) {
            long cash = id(payment, "cash_account_id");
            var amount = dec(payment, "amount");
            ledger.cash(cash, amount, "EGRESO", "Devolución adelanto de evento #" + id, "ANULACION-EVENTO:" + id, "");
            ledger.simple("461", ledger.cashCode(cash), amount, "Devolución de adelanto de cliente",
                    "ANULACION-EVENTO:" + id);
            refunded = refunded.add(amount);
        }
        require(refunded.compareTo(dec(q, "advance")) == 0,
                "Los anticipos no concilian; revisa la tesorería del evento");
        db.update("quotes", id,
                map("advance", 0, "status", "ANULADA", "notes", str(q, "notes") + " | Anulación: " + reason));
    }
}
