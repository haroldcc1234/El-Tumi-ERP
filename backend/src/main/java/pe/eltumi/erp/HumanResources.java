package pe.eltumi.erp;

import org.springframework.stereotype.Service;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static pe.eltumi.erp.Store.*;

@Service
public class HumanResources {
    final Store db;
    final Ledger ledger;

    public HumanResources(Store d, Ledger l) {
        db = d;
        ledger = l;
    }

    public Map<String, Object> contract(long employee) {
        require(str(db.get("employees", employee), "status").equals("ACTIVO"), "Empleado inactivo");
        return db.one(
                "SELECT * FROM contracts WHERE employee_id=? AND status='ACTIVO' AND start_date<=? AND end_date>=? ORDER BY start_date DESC LIMIT 1",
                employee, today(), today());
    }

    public String employeeName(long employee) {
        var e = db.get("employees", employee);
        return str(e, "name") + " " + str(e, "last_name");
    }

    public long create(String entity, Map<String, Object> b) {
        long employee = id(b, "employee_id");
        var contract = entity.equals("fees") ? Map.<String, Object>of() : contract(employee);
        var salary = dec(contract, "salary");
        switch (entity) {
            case "leaves" -> {
                LocalDate start = LocalDate.parse(str(b, "start_date")), end = LocalDate.parse(str(b, "end_date"));
                require(!end.isBefore(start), "Rango de fechas inválido");
                require(db.count(
                        "SELECT COUNT(*) FROM leaves WHERE employee_id=? AND status<>'RECHAZADA' AND start_date<=? AND end_date>=?",
                        employee, end, start) == 0, "La ausencia coincide con otra solicitud");
                b.put("days", ChronoUnit.DAYS.between(start, end) + 1);
            }
            case "payrolls" -> {
                YearMonth.parse(str(b, "period"));
                b.put("salary", salary);
                calculatePayroll(b, contract);
            }
            case "fees" -> recalculateFee(b);
            case "loans" -> {
                var amount = dec(b, "amount");
                positive(amount);
                int months = dec(b, "months").intValueExact();
                require(months >= 1 && months <= 120, "Las cuotas deben ser entre 1 y 120");
                double rate = Math.pow(1.06, 1.0 / 12) - 1;
                BigDecimal installment = money(
                        BigDecimal.valueOf(amount.doubleValue() * rate / (1 - Math.pow(1 + rate, -months))));
                BigDecimal previous = db.sum(
                        "SELECT COALESCE(SUM(installment),0) FROM loans WHERE employee_id=? AND status IN ('ACTIVO','PENDIENTE')",
                        employee);
                require(installment.add(previous).compareTo(salary.multiply(new BigDecimal("0.45"))) <= 0,
                        "Las cuotas acumuladas superan el 45% del sueldo");
                YearMonth.parse(str(b, "first_period"));
                b.put("annual_rate", 6);
                b.put("installment", installment);
                b.put("balance", amount);
            }
            case "advances" -> {
                positive(dec(b, "amount"));
                var total = db.sum("SELECT COALESCE(SUM(amount),0) FROM advances WHERE employee_id=? AND period=?",
                        employee, str(b, "period")).add(dec(b, "amount"));
                require(total.compareTo(salary.multiply(new BigDecimal("0.4"))) <= 0,
                        "Los adelantos del mes superan el 40% del sueldo");
                require(db.count("SELECT COUNT(*) FROM payrolls WHERE employee_id=? AND period=?", employee,
                        str(b, "period")) == 0,
                        "Ya existe una planilla del mes; registra el adelanto en un período posterior");
            }
            case "benefits" -> {
                BigDecimal amount;
                String kind = str(b, "kind");
                var months = dec(b, "months");
                require(months.compareTo(new BigDecimal("6")) <= 0, "Máximo seis meses computables");
                if (kind.equals("CTS"))
                    amount = salary.multiply(dec(contract, "cts_factor")).multiply(months).divide(new BigDecimal("12"),
                            2, RoundingMode.HALF_UP);
                else if (kind.equals("GRATIFICACION"))
                    amount = salary.multiply(dec(contract, "bonus_factor")).multiply(months).divide(new BigDecimal("6"),
                            2, RoundingMode.HALF_UP);
                else
                    amount = salary.multiply(dec(b, "days")).divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
                b.put("amount", money(amount));
                require(db.count("SELECT COUNT(*) FROM benefits WHERE employee_id=? AND period=? AND kind=?", employee,
                        str(b, "period"), kind) == 0, "Ya existe ese beneficio para el período");
            }
            case "settlements" -> {
                require(dec(b, "cts_months").compareTo(new BigDecimal("6")) <= 0
                        && dec(b, "bonus_months").compareTo(new BigDecimal("6")) <= 0, "Revisa los meses computables");
                var amount = salary.multiply(dec(b, "vacation_days"))
                        .divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP)
                        .add(salary.multiply(dec(contract, "cts_factor")).multiply(dec(b, "cts_months"))
                                .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP))
                        .add(salary.multiply(dec(contract, "bonus_factor")).multiply(dec(b, "bonus_months"))
                                .divide(new BigDecimal("6"), 2, RoundingMode.HALF_UP))
                        .add(dec(b, "other"));
                b.put("amount", money(amount));
            }
            default -> throw new Problem(422, "Operación RRHH inválida");
        }
        long id = db.insert(entity, b);
        if (entity.equals("loans")) {
            var outstanding = dec(b, "amount");
            double r = Math.pow(1.06, 1.0 / 12) - 1;
            int months = dec(b, "months").intValueExact();
            YearMonth period = YearMonth.parse(str(b, "first_period"));
            for (int i = 0; i < months; i++) {
                var interest = money(outstanding.multiply(BigDecimal.valueOf(r)));
                var principal = i == months - 1 ? outstanding : dec(b, "installment").subtract(interest);
                db.insert("installments", map("loan_id", id, "period", period.plusMonths(i).toString(), "amount",
                        principal.add(interest), "principal", principal, "interest", interest, "status", "PENDIENTE"));
                outstanding = outstanding.subtract(principal);
            }
            payable(entity, id, employee, dec(b, "amount"), "141");
        }
        if (entity.equals("advances"))
            payable(entity, id, employee, dec(b, "amount"), "141");
        return id;
    }

    void payable(String entity, long id, long employee, BigDecimal amount, String debit) {
        ledger.debt("payables", employeeName(employee), str(db.get("employees", employee), "document"), amount, today(),
                entity, id, entity + " #" + id);
        ledger.simple(debit, "421", amount, "Registro " + entity, entity + ":" + id);
    }

    void calculatePayroll(Map<String, Object> b, Map<String, Object> contract) {
        long employee = id(b, "employee_id");
        String period = str(b, "period");
        var advance = db.sum(
                "SELECT COALESCE(SUM(amount),0) FROM advances WHERE employee_id=? AND period=? AND status='ENTREGADO'",
                employee, period);
        var loan = db.sum(
                "SELECT COALESCE(SUM(i.amount),0) FROM installments i JOIN loans l ON l.id=i.loan_id WHERE l.employee_id=? AND l.status='ACTIVO' AND i.status='PENDIENTE' AND i.period<=?",
                employee, period);
        BigDecimal gross = dec(b, "salary").add(dec(b, "overtime")).add(dec(b, "family_allowance"))
                .add(dec(b, "other_bonus")).subtract(dec(b, "lateness"));
        var pension = money(gross.multiply(dec(contract, "pension_rate")).divide(new BigDecimal("100")));
        var net = money(gross.subtract(pension).subtract(dec(b, "income_tax")).subtract(advance).subtract(loan));
        require(net.signum() > 0, "La planilla no puede tener un neto negativo o cero");
        b.put("pension", pension);
        b.put("advances", advance);
        b.put("loans", loan);
        b.put("net", net);
        b.put("employer_health", money(gross.multiply(dec(contract, "health_rate")).divide(new BigDecimal("100"))));
    }

    public void approve(String entity, long id) {
        var b = db.lock(entity, id);
        String status = str(b, "status");
        require(List.of("BORRADOR", "PENDIENTE").contains(status), "El registro ya fue procesado");
        long employee = id(b, "employee_id");
        if (entity.equals("leaves")) {
            if (str(b, "kind").equals("VACACIONES")) {
                var c = contract(employee);
                long days = Math.max(0, ChronoUnit.DAYS.between(LocalDate.parse(str(c, "start_date")), today()));
                BigDecimal accrued = dec(c, "vacation_days").multiply(BigDecimal.valueOf(days))
                        .divide(new BigDecimal("365"), 2, RoundingMode.DOWN);
                var used = db.sum(
                        "SELECT COALESCE(SUM(days),0) FROM leaves WHERE employee_id=? AND kind='VACACIONES' AND status='APROBADA'",
                        employee);
                require(accrued.subtract(used).compareTo(dec(b, "days")) >= 0,
                        "Saldo de vacaciones insuficiente. Acumulado proporcional: " + accrued);
            }
            db.update(entity, id, map("status", "APROBADA"));
            return;
        }
        if (entity.equals("payrolls")) {
            var recalculated = new LinkedHashMap<>(b);
            calculatePayroll(recalculated, contract(employee));
            require(dec(recalculated, "net").compareTo(dec(b, "net")) == 0,
                    "Los descuentos cambiaron. Elimina el borrador y vuelve a generar la planilla");
            BigDecimal principal = BigDecimal.ZERO, interest = BigDecimal.ZERO;
            for (var i : db.rows(
                    "SELECT i.* FROM installments i JOIN loans l ON l.id=i.loan_id WHERE l.employee_id=? AND l.status='ACTIVO' AND i.status='PENDIENTE' AND i.period<=?",
                    employee, str(b, "period"))) {
                principal = principal.add(dec(i, "principal"));
                interest = interest.add(dec(i, "interest"));
                db.exec("UPDATE installments SET status='DESCONTADA',payroll_id=? WHERE id=?", id, id(i, "id"));
                var loan = db.lock("loans", id(i, "loan_id"));
                var next = dec(loan, "balance").subtract(dec(i, "principal"));
                db.update("loans", id(i, "loan_id"),
                        map("balance", next, "status", next.signum() == 0 ? "PAGADO" : "ACTIVO"));
            }
            db.exec("UPDATE advances SET status='DESCONTADO',version=version+1 WHERE employee_id=? AND period=? AND status='ENTREGADO'",
                    employee, str(b, "period"));
            var gross = dec(b, "net").add(dec(b, "pension")).add(dec(b, "income_tax")).add(dec(b, "advances"))
                    .add(dec(b, "loans"));
            ledger.debt("payables", employeeName(employee), "", dec(b, "net"), today(), entity, id,
                    "Planilla " + str(b, "period"));
            ledger.journal("Planilla " + employeeName(employee) + " " + str(b, "period"), entity + ":" + id, today(),
                    List.of(ledger.entry("621", gross, BigDecimal.ZERO),
                            ledger.entry("627", dec(b, "employer_health"), BigDecimal.ZERO),
                            ledger.entry("421", BigDecimal.ZERO, dec(b, "net")),
                            ledger.entry("403", BigDecimal.ZERO, dec(b, "pension").add(dec(b, "employer_health"))),
                            ledger.entry("4017", BigDecimal.ZERO, dec(b, "income_tax")),
                            ledger.entry("141", BigDecimal.ZERO, dec(b, "advances").add(principal)),
                            ledger.entry("759", BigDecimal.ZERO, interest)));
        } else if (entity.equals("fees")) {
            require(!str(b, "file_id").isBlank(), "Adjunta el recibo de honorarios");
            require(!today().isBefore(LocalDate.parse(str(b, "end_date")).minusDays(2)),
                    "El recibo se envía desde dos días antes del vencimiento");
            ledger.debt("payables", str(b, "party"), str(b, "document"), dec(b, "net"), today(), entity, id,
                    "Honorarios");
            ledger.journal("Honorarios " + str(b, "party"), entity + ":" + id, today(),
                    List.of(ledger.entry("631", dec(b, "amount"), BigDecimal.ZERO),
                            ledger.entry("421", BigDecimal.ZERO, dec(b, "net")),
                            ledger.entry("4017", BigDecimal.ZERO, dec(b, "withheld"))));
        } else {
            payable(entity, id, employee, dec(b, "amount"), "621");
            if (entity.equals("settlements")) {
                require(db.count("SELECT COUNT(*) FROM loans WHERE employee_id=? AND status<>'PAGADO'", employee) == 0,
                        "Liquida los préstamos pendientes antes del cese");
                db.update("employees", employee, map("status", "INACTIVO"));
                db.exec("UPDATE contracts SET status='FINALIZADO',version=version+1 WHERE employee_id=?", employee);
            }
        }
        db.update(entity, id, map("status", List.of("fees", "benefits").contains(entity) ? "APROBADO" : "APROBADA"));
    }

    public void recalculateFee(Map<String, Object> b) {
        require(!LocalDate.parse(str(b, "end_date")).isBefore(LocalDate.parse(str(b, "start_date"))),
                "Fechas inválidas");
        var amount = dec(b, "amount");
        positive(amount);
        var tax = amount.compareTo(new BigDecimal("1500")) > 0 && str(b, "waiver").equals("NO")
                ? money(amount.multiply(new BigDecimal("0.08")))
                : BigDecimal.ZERO;
        b.put("withheld", tax);
        b.put("net", amount.subtract(tax));
    }

    public void settle(long id, long cash) {
        var loan = db.lock("loans", id);
        require(str(loan, "status").equals("ACTIVO"), "Solo se liquidan préstamos activos");
        var amount = dec(loan, "balance");
        positive(amount);
        ledger.cash(cash, amount, "INGRESO", "Cancelación anticipada de préstamo #" + id, "loans:" + id, "");
        ledger.simple(ledger.cashCode(cash), "141", amount, "Pago total préstamo", "loans:" + id);
        db.exec("UPDATE installments SET status='CANCELADA' WHERE loan_id=? AND status='PENDIENTE'", id);
        db.update("loans", id, map("status", "PAGADO", "balance", 0));
    }
}
