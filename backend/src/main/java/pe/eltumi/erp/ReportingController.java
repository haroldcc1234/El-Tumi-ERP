package pe.eltumi.erp;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import static pe.eltumi.erp.Store.*;

/** Internal accounting statements built from posted double-entry journals. */
@RestController
@RequestMapping("/api/financial-reports")
public class ReportingController {
    private final Store db;
    private final Access access;
    private final ExportController exports;

    public ReportingController(Store db, Access access, ExportController exports) {
        this.db = db;
        this.access = access;
        this.exports = exports;
    }

    @GetMapping("/{kind}")
    public Map<String, Object> report(@PathVariable String kind,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") long accountId) {
        access.module("accounting", "read");
        LocalDate start = from == null || from.isBlank() ? today().withDayOfMonth(1) : LocalDate.parse(from);
        LocalDate end = to == null || to.isBlank() ? today() : LocalDate.parse(to);
        require(kind.equals("balance") || !end.isBefore(start), "La fecha final debe ser posterior o igual al inicio");
        var result = map("from", start, "to", end, "summary", map());
        if (kind.equals("ledger")) {
            var account = db.get("accounts", accountId);
            BigDecimal opening = db.sum(
                    "SELECT COALESCE(SUM(l.debit-l.credit),0) FROM journal_lines l JOIN journals j ON j.id=l.journal_id WHERE l.account_id=? AND j.date<?",
                    accountId, start);
            var rows = db.rows(
                    "SELECT j.date,j.id AS journal,j.description,j.reference,l.debit,l.credit FROM journal_lines l JOIN journals j ON j.id=l.journal_id WHERE l.account_id=? AND j.date BETWEEN ? AND ? ORDER BY j.date,j.id,l.id",
                    accountId, start, end);
            BigDecimal running = opening;
            for (var row : rows) {
                running = running.add(dec(row, "debit")).subtract(dec(row, "credit"));
                row.put("balance", money(running));
            }
            result.putAll(map("title", "Libro mayor · " + str(account, "code") + " " + str(account, "name"),
                    "columns",
                    columns("date", "Fecha", "journal", "Asiento", "description", "Descripción", "reference",
                            "Referencia", "debit", "Debe", "credit", "Haber", "balance", "Saldo"),
                    "rows", rows, "summary",
                    map("Saldo inicial (debe − haber)", opening, "Saldo final (debe − haber)", running)));
            return result;
        }
        require(List.of("trial", "balance", "income").contains(kind), "Reporte desconocido");
        LocalDate effectiveStart = kind.equals("balance") ? LocalDate.of(1900, 1, 1) : start;
        var rows = db.rows(
                "SELECT a.code,a.name,a.type,COALESCE(SUM(x.debit),0) debit,COALESCE(SUM(x.credit),0) credit,COALESCE(SUM(x.debit-x.credit),0) balance FROM accounts a LEFT JOIN (SELECT l.* FROM journal_lines l JOIN journals j ON j.id=l.journal_id WHERE j.date BETWEEN ? AND ?) x ON x.account_id=a.id GROUP BY a.id,a.code,a.name,a.type ORDER BY a.code",
                effectiveStart, end);
        if (kind.equals("trial")) {
            BigDecimal debit = BigDecimal.ZERO, credit = BigDecimal.ZERO;
            for (var r : rows) {
                debit = debit.add(dec(r, "debit"));
                credit = credit.add(dec(r, "credit"));
            }
            result.putAll(map("title", "Balance de comprobación", "columns",
                    columns("code", "Código", "name", "Cuenta", "type", "Tipo", "debit", "Debe", "credit", "Haber",
                            "balance", "Saldo"),
                    "rows", rows,
                    "summary", map("Total debe", debit, "Total haber", credit, "Diferencia", debit.subtract(credit))));
            return result;
        }
        BigDecimal assets = BigDecimal.ZERO, liabilities = BigDecimal.ZERO, equity = BigDecimal.ZERO;
        BigDecimal income = BigDecimal.ZERO, expenses = BigDecimal.ZERO, cost = BigDecimal.ZERO;
        List<Map<String, Object>> selected = new ArrayList<>();
        for (var row : rows) {
            String type = str(row, "type");
            BigDecimal balance = dec(row, "balance");
            switch (type) {
                case "ACTIVO" -> assets = assets.add(balance);
                case "PASIVO" -> liabilities = liabilities.subtract(balance);
                case "PATRIMONIO" -> equity = equity.subtract(balance);
                case "INGRESO" -> income = income.subtract(balance);
                case "GASTO" -> expenses = expenses.add(balance);
                case "COSTO" -> cost = cost.add(balance);
            }
            boolean selectedType = kind.equals("balance") ? List.of("ACTIVO", "PASIVO", "PATRIMONIO").contains(type)
                    : List.of("INGRESO", "GASTO", "COSTO").contains(type);
            if (selectedType)
                selected.add(map("type", type, "code", row.get("code"), "name", row.get("name"), "amount",
                        List.of("PASIVO", "PATRIMONIO", "INGRESO").contains(type) ? balance.negate() : balance));
        }
        BigDecimal profit = income.subtract(expenses).subtract(cost);
        if (kind.equals("balance")) {
            selected.add(map("type", "PATRIMONIO", "code", "—", "name", "Resultado acumulado de cuentas de gestión",
                    "amount", profit));
            result.putAll(map("title", "Estado de situación financiera interno", "summary",
                    map("Activos", assets, "Pasivos", liabilities, "Patrimonio y resultado acumulado",
                            equity.add(profit), "Diferencia de cuadre",
                            assets.subtract(liabilities).subtract(equity).subtract(profit))));
        } else {
            result.putAll(map("title", "Estado de resultados interno", "summary",
                    map("Ingresos", income, "Costos", cost, "Gastos", expenses, "Resultado del período", profit)));
        }
        result.put("rows", selected);
        result.put("columns", columns("type", "Grupo", "code", "Código", "name", "Cuenta", "amount", "Importe"));
        return result;
    }

    private List<Map<String, Object>> columns(String... pairs) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2)
            list.add(map("key", pairs[i], "label", pairs[i + 1], "numeric",
                    List.of("debit", "credit", "balance", "amount").contains(pairs[i])));
        return list;
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/{kind}/export")
    public ResponseEntity<byte[]> export(@PathVariable String kind,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") long accountId) throws Exception {
        access.module("accounting", "export");
        var report = report(kind, from, to, accountId);
        var cols = (List<Map<String, Object>>) report.get("columns");
        var rows = (List<Map<String, Object>>) report.get("rows");
        var summary = (Map<String, Object>) report.get("summary");
        List<String> headings = cols.stream().map(c -> str(c, "label")).toList();
        List<List<String>> values = new ArrayList<>();
        for (var row : rows)
            values.add(cols.stream().map(c -> str(row, str(c, "key"))).toList());
        summary.forEach((label, value) -> {
            var line = new ArrayList<>(Collections.nCopies(cols.size(), ""));
            line.set(0, label);
            line.set(cols.size() - 1, value.toString());
            values.add(line);
        });
        String heading = str(report, "title") + " | "
                + (kind.equals("balance") ? "Al " + report.get("to") : report.get("from") + " / " + report.get("to"));
        if (format.equals("pdf"))
            return exports.response(exports.tablePdf(heading, headings, values), "application/pdf",
                    "el-tumi-" + kind + ".pdf");
        if (format.equals("csv")) {
            StringBuilder csv = new StringBuilder("\ufeff");
            csv.append(exports.cell(heading)).append("\r\n");
            values.add(0, headings);
            for (var row : values)
                csv.append(row.stream().map(exports::cell).collect(java.util.stream.Collectors.joining(",")))
                        .append("\r\n");
            return exports.response(csv.toString().getBytes(StandardCharsets.UTF_8), "text/csv",
                    "el-tumi-" + kind + ".csv");
        }
        require(format.equals("xlsx"), "Formato inválido");
        try (var book = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = book.createSheet("Reporte");
            sheet.createRow(0).createCell(0).setCellValue(heading);
            var h = sheet.createRow(1);
            for (int c = 0; c < headings.size(); c++)
                h.createCell(c).setCellValue(headings.get(c));
            for (int n = 0; n < values.size(); n++) {
                var row = sheet.createRow(n + 2);
                for (int c = 0; c < cols.size(); c++) {
                    var cell = row.createCell(c);
                    String value = values.get(n).get(c);
                    if (Boolean.TRUE.equals(cols.get(c).get("numeric")) && !value.isBlank())
                        cell.setCellValue(new BigDecimal(value).doubleValue());
                    else
                        cell.setCellValue(value);
                }
            }
            sheet.createFreezePane(0, 2);
            for (int c = 0; c < cols.size(); c++)
                sheet.setColumnWidth(c,
                        str(cols.get(c), "key").equals("name") || str(cols.get(c), "key").equals("description") ? 14000
                                : 6500);
            book.write(out);
            return exports.response(out.toByteArray(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "el-tumi-" + kind + ".xlsx");
        }
    }
}
