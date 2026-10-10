package pe.eltumi.erp;

import org.springframework.stereotype.Service;
import java.util.*;
import java.math.*;
import java.time.*;
import static pe.eltumi.erp.Store.*;

@Service
public class Ledger {
    final Store db;

    public Ledger(Store db) {
        this.db = db;
    }

    public void period(LocalDate date) {
        require(db.count("SELECT COUNT(*) FROM periods WHERE period=?", YearMonth.from(date).toString()) == 0,
                "El período contable está cerrado");
    }

    public long account(String code) {
        return id(db.one("SELECT id FROM accounts WHERE code=? AND status='ACTIVO' AND postable='SI'", code), "id");
    }

    public Map<String, Object> entry(String code, BigDecimal debit, BigDecimal credit) {
        return map("account_id", account(code), "debit", money(debit), "credit", money(credit));
    }

    public long journal(String description, String ref, LocalDate date, List<Map<String, Object>> entries) {
        period(date);
        BigDecimal debit = BigDecimal.ZERO, credit = BigDecimal.ZERO;
        int count = 0;
        for (var e : entries) {
            var a = db.get("accounts", id(e, "account_id"));
            require(str(a, "status").equals("ACTIVO") && str(a, "postable").equals("SI"),
                    "Cuenta contable no habilitada");
            var d = money(dec(e, "debit"));
            var c = money(dec(e, "credit"));
            require(d.signum() >= 0 && c.signum() >= 0 && (d.signum() == 0 || c.signum() == 0),
                    "Cada línea debe tener solo debe o haber");
            debit = debit.add(d);
            credit = credit.add(c);
            if (d.add(c).signum() > 0)
                count++;
        }
        require(debit.compareTo(credit) == 0 && count >= 2,
                "El asiento debe cuadrar: debe = haber, con al menos dos líneas");
        long id = db.insert("journals", map("date", date, "description", description, "reference", ref, "status",
                "REGISTRADO", "total", debit));
        for (var e : entries)
            if (dec(e, "debit").add(dec(e, "credit")).signum() > 0)
                db.insert("journal_lines", map("journal_id", id, "account_id", e.get("account_id"), "debit",
                        money(dec(e, "debit")), "credit", money(dec(e, "credit"))));
        return id;
    }

    public void simple(String debit, String credit, BigDecimal amount, String text, String ref) {
        simple(debit, credit, amount, text, ref, today());
    }

    public void simple(String debit, String credit, BigDecimal amount, String text, String ref, LocalDate date) {
        amount = money(amount);
        if (amount.signum() == 0)
            return;
        journal(text, ref, date,
                List.of(entry(debit, amount, BigDecimal.ZERO), entry(credit, BigDecimal.ZERO, amount)));
    }

    public long cash(long account, BigDecimal amount, String kind, String description, String ref, String file) {
        return cash(account, amount, kind, description, ref, file, today());
    }

    public long cash(long account, BigDecimal amount, String kind, String description, String ref, String file,
            LocalDate date) {
        period(date);
        amount = money(amount);
        positive(amount);
        var a = db.lock("cash_accounts", account);
        require(str(a, "status").equals("ACTIVO"), "Cuenta de tesorería inactiva");
        var next = dec(a, "balance").add(kind.equals("INGRESO") ? amount : amount.negate());
        require(next.signum() >= 0, "Saldo insuficiente en la cuenta de tesorería");
        db.update("cash_accounts", account, map("balance", next));
        return db.insert("treasury", map("cash_account_id", account, "kind", kind, "amount", amount, "date", date,
                "description", description, "reference", ref, "file_id", file));
    }

    public String cashCode(long id) {
        return str(db.get("cash_accounts", id), "kind").equals("EFECTIVO") ? "101" : "104";
    }

    public long debt(String side, String party, String document, BigDecimal amount, LocalDate due, String source,
            long sourceId, String notes) {
        positive(amount);
        return db.insert(side,
                map("code",
                        (side.equals("payables") ? "CP-" : "CC-")
                                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                        "party", party, "document", document, "date", today(), "due_date", due, "total", money(amount),
                        "paid", 0, "credit", 0, "status", "PENDIENTE", "source", source, "source_id", sourceId, "notes",
                        notes));
    }

    public void budget(long id, BigDecimal amount, LocalDate date) {
        if (id == 0)
            return;
        var b = db.lock("budgets", id);
        require(!date.isBefore(LocalDate.parse(str(b, "start_date")))
                && !date.isAfter(LocalDate.parse(str(b, "end_date"))),
                "La fecha está fuera de la vigencia del presupuesto");
        var used = dec(b, "executed").add(amount);
        require(used.compareTo(dec(b, "amount")) <= 0, "La operación supera el presupuesto disponible");
        require(used.signum() >= 0, "Ejecución presupuestal inválida");
        db.update("budgets", id, map("executed", used));
    }

    public void pay(String side, long id, Map<String, Object> b) {
        var debt = db.lock(side, id);
        require(!List.of("PAGADA", "ANULADA").contains(str(debt, "status")), "Cuenta ya pagada o anulada");
        var amount = money(dec(b, "amount"));
        positive(amount);
        var balance = dec(debt, "total").subtract(dec(debt, "paid")).subtract(dec(debt, "credit"));
        require(amount.compareTo(balance) <= 0, "El pago supera el saldo pendiente");
        long account = id(b, "cash_account_id");
        boolean incoming = side.equals("receivables");
        long treasury = cash(account, amount, incoming ? "INGRESO" : "EGRESO", "Pago " + str(debt, "code"),
                side + ":" + id, str(b, "file_id"));
        BigDecimal paid = dec(debt, "paid").add(amount);
        boolean full = paid.add(dec(debt, "credit")).compareTo(dec(debt, "total")) == 0;
        db.update(side, id, map("paid", paid, "status", full ? "PAGADA" : "PARCIAL"));
        db.insert("payments", map("side", side, "debt_id", id, "amount", amount, "cash_account_id", account,
                "treasury_id", treasury, "date", today(), "file_id", str(b, "file_id")));
        simple(incoming ? cashCode(account) : "421", incoming ? "121" : cashCode(account), amount,
                "Pago " + str(debt, "code"), side + ":" + id);
        if (full && !incoming) {
            String source = str(debt, "source");
            if (source.equals("loans"))
                db.update("loans", id(debt, "source_id"), map("status", "ACTIVO"));
            if (source.equals("advances"))
                db.update("advances", id(debt, "source_id"), map("status", "ENTREGADO"));
            if (source.equals("receipts")) {
                long purchase = id(db.get("receipts", id(debt, "source_id")), "purchase_id");
                var order = db.get("purchases", purchase);
                if (str(order, "status").equals("ENTREGADO") && db.count(
                        "SELECT COUNT(*) FROM payables p JOIN receipts r ON p.source='receipts' AND p.source_id=r.id WHERE r.purchase_id=? AND p.status<>'PAGADA'",
                        purchase) == 0)
                    db.update("purchases", purchase, map("status", "PAGADO"));
            }
        }
    }

    public Map<String, Object> balances() {
        return map("accounts", db.rows(
                "SELECT a.id,a.code,a.name,a.type,COALESCE(SUM(l.debit),0) debit,COALESCE(SUM(l.credit),0) credit,COALESCE(SUM(l.debit-l.credit),0) balance FROM accounts a LEFT JOIN journal_lines l ON a.id=l.account_id GROUP BY a.id,a.code,a.name,a.type ORDER BY a.code"));
    }
}
