package pe.eltumi.erp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import java.math.*;
import java.util.concurrent.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.junit.jupiter.api.Assertions.*;
import static pe.eltumi.erp.Store.*;

@SpringBootTest(properties = { "app.files=./target/test-files" })
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    Store db;
    @Autowired
    ObjectMapper json;
    String token;
    static long invoiceId;

    @BeforeEach
    void login() throws Exception {
        var result = mvc
                .perform(post("/api/auth/login").contentType("application/json").content(
                        json.writeValueAsString(map("email", "admin@eltumi.local", "password", "ElTumi2026!"))))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        token = str(parse(result.getResponse().getContentAsString()), "token");
    }

    Map<String, Object> parse(String text) throws Exception {
        return json.readValue(text, new TypeReference<>() {
        });
    }

    Map<String, Object> postData(String path, Map<String, Object> body, int expected) throws Exception {
        var response = mvc.perform(post("/api" + path).header("Authorization", "Bearer " + token)
                .contentType("application/json").content(json.writeValueAsString(body))).andReturn().getResponse();
        assertEquals(expected, response.getStatus(), path + " " + response.getContentAsString());
        return response.getContentAsString().isBlank() ? Map.of() : parse(response.getContentAsString());
    }

    Map<String, Object> getData(String path) throws Exception {
        var response = mvc.perform(get("/api" + path).header("Authorization", "Bearer " + token)).andReturn()
                .getResponse();
        assertEquals(200, response.getStatus(), response.getContentAsString());
        return parse(response.getContentAsString());
    }

    String evidence() throws Exception {
        var response = mvc.perform(multipart("/api/files")
                .file(new MockMultipartFile("file", "evidencia.pdf", "application/pdf",
                        "%PDF-1.4\n% Test evidence".getBytes()))
                .param("module", "logistics").header("Authorization", "Bearer " + token)).andReturn().getResponse();
        assertEquals(200, response.getStatus(), response.getContentAsString());
        return str(parse(response.getContentAsString()), "id");
    }

    @Test
    @Order(1)
    void authenticationCorsAndRoleBoundary() throws Exception {
        assertEquals(401, mvc.perform(get("/api/data/users")).andReturn().getResponse().getStatus());
        var cors = mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")).andReturn().getResponse();
        assertEquals("http://localhost:5173", cors.getHeader("Access-Control-Allow-Origin"));
        var user = postData("/data/users", map("name", "Cocina QA", "email", "cocina.qa@eltumi.local", "password",
                "Cocina2026!", "role", "COCINA", "status", "ACTIVO"), 200);
        var login = postData("/auth/login", map("email", "cocina.qa@eltumi.local", "password", "Cocina2026!"), 200);
        String admin = token;
        token = str(login, "token");
        postData("/data/partners",
                map("kind", "CLIENTE", "name", "Denegado", "document", "99999999", "status", "ACTIVO"), 403);
        assertEquals(403, mvc.perform(get("/api/data/users").header("Authorization", "Bearer " + token)).andReturn()
                .getResponse().getStatus());
        token = admin;
        var detail = getData("/data/users/" + id(user, "id"));
        assertFalse(detail.containsKey("password_hash"));
        assertFalse(detail.containsKey("totp_secret"));
    }

    @Test
    @Order(2)
    void partialPurchaseCreatesStockDebtAndBalancedJournalExactlyOnce() throws Exception {
        var product = postData("/data/products", map("code", "QA-INS", "name", "Insumo de prueba", "category", "INSUMO",
                "unit", "KG", "tax_type", "GRAVADO", "tax_rate", 18, "status", "ACTIVO"), 200);
        long p = id(product, "id");
        var purchase = postData("/data/purchases",
                map("code", "QA-OC", "partner_id", 1, "warehouse_id", 1, "date", today().toString(), "due_date",
                        today().plusDays(7).toString(), "lines",
                        List.of(map("product_id", p, "quantity", 10, "unit_cost", 5))),
                200);
        long order = id(purchase, "id");
        assertEquals(0, new BigDecimal(purchase.get("total").toString()).compareTo(new BigDecimal("59")));
        postData("/actions/purchases/" + order + "/send", map(), 200);
        long line = id(db.one("SELECT id FROM purchase_lines WHERE purchase_id=?", order), "id");
        String file = evidence();
        var body = map("file_id", file, "reference", "F-QA", "lines",
                List.of(map("line_id", line, "quantity", 4, "unit_cost", 5)));
        postData("/actions/purchases/" + order + "/receive", body, 200);
        assertEquals("PARCIAL", db.get("purchases", order).get("status"));
        assertEquals(0, db.sum("SELECT quantity FROM stocks WHERE product_id=? AND warehouse_id=1", p)
                .compareTo(new BigDecimal("4")));
        long before = db.count("SELECT COUNT(*) FROM receipts");
        body.put("lines", List.of(map("line_id", line, "quantity", 7, "unit_cost", 5)));
        postData("/actions/purchases/" + order + "/receive", body, 422);
        assertEquals(before, db.count("SELECT COUNT(*) FROM receipts"));
        body.put("lines", List.of(map("line_id", line, "quantity", 6, "unit_cost", 5)));
        postData("/actions/purchases/" + order + "/receive", body, 200);
        assertEquals("ENTREGADO", db.get("purchases", order).get("status"));
        postData("/actions/purchases/" + order + "/receive", body, 422);
        for (var debt : db.rows(
                "SELECT p.* FROM payables p JOIN receipts r ON p.source='receipts' AND p.source_id=r.id WHERE r.purchase_id=?",
                order))
            postData("/actions/payables/" + id(debt, "id") + "/pay",
                    map("amount", debt.get("total"), "cash_account_id", 3), 200);
        assertEquals("PAGADO", db.get("purchases", order).get("status"));
        assertEquals(0, db.sum("SELECT COALESCE(SUM(debit-credit),0) FROM journal_lines").compareTo(BigDecimal.ZERO));
    }

    @Test
    @Order(3)
    void paymentCannotExceedDebtOrTreasuryBalance() throws Exception {
        var debt = postData("/data/payables", map("code", "QA-PAY", "party", "Proveedor QA", "date", today().toString(),
                "due_date", today().toString(), "total", 100), 200);
        long id = id(debt, "id");
        postData("/actions/payables/" + id + "/pay", map("amount", 101, "cash_account_id", 3), 422);
        postData("/actions/payables/" + id + "/pay", map("amount", 30, "cash_account_id", 3), 200);
        assertEquals("PARCIAL", db.get("payables", id).get("status"));
        postData("/actions/payables/" + id + "/pay", map("amount", 70, "cash_account_id", 3), 200);
        postData("/actions/payables/" + id + "/pay", map("amount", 1, "cash_account_id", 3), 422);
        var b = postData("/data/payables", map("code", "QA-NOCASH", "party", "Proveedor QA", "date", today().toString(),
                "due_date", today().toString(), "total", 20), 200);
        postData("/actions/payables/" + id(b, "id") + "/pay", map("amount", 20, "cash_account_id", 5), 422);
        assertEquals(0, dec(db.get("payables", id(b, "id")), "paid").signum());
    }

    @Test
    @Order(4)
    void restaurantFromTableToKitchenCashAndCleaning() throws Exception {
        postData("/data/cash_sessions", map("cash_account_id", 1, "opening", db.get("cash_accounts", 1).get("balance")),
                200);
        var before = db.sum("SELECT quantity FROM stocks WHERE product_id=1 AND warehouse_id=1");
        var order = postData("/data/orders", map("table_id", 1, "warehouse_id", 1, "date", today().toString(), "lines",
                List.of(map("dish_id", 1, "quantity", 1, "notes", "Sin ensalada"))), 200);
        long id = id(order, "id");
        long line = id(db.one("SELECT id FROM order_lines WHERE order_id=?", id), "id");
        postData("/actions/orders/" + id + "/bill", map(), 422);
        postData("/order-lines/" + line + "/cook", map(), 200);
        postData("/order-lines/" + line + "/ready", map(), 200);
        postData("/order-lines/" + line + "/deliver", map(), 200);
        postData("/actions/orders/" + id + "/bill", map("discount_code", "TUMI10"), 200);
        assertEquals(0, dec(db.get("orders", id), "total").compareTo(new BigDecimal("28.80")));
        var checkout = map("document_type", "BOLETA", "payment_type", "CONTADO", "cash_account_id", 1, "customer",
                "Cliente QA");
        var invoice = postData("/actions/orders/" + id + "/checkout", checkout, 200);
        invoiceId = id(invoice, "id");
        postData("/actions/orders/" + id + "/checkout", checkout, 422);
        assertEquals("LIMPIAR", db.get("dining_tables", 1).get("status"));
        assertEquals(0, db.sum("SELECT quantity FROM stocks WHERE product_id=1 AND warehouse_id=1")
                .compareTo(before.subtract(new BigDecimal("0.3"))));
        postData("/actions/dining_tables/1/clean", map(), 200);
        assertEquals("LIBRE", db.get("dining_tables", 1).get("status"));
    }

    @Test
    @Order(5)
    void wasteAndReturnDoNotDoubleDeductStock() throws Exception {
        var before = db.sum("SELECT quantity FROM stocks WHERE product_id=1 AND warehouse_id=1");
        var order = postData("/data/orders", map("table_id", 2, "warehouse_id", 1, "date", today().toString(), "lines",
                List.of(map("dish_id", 1, "quantity", 1))), 200);
        long id = id(order, "id"), line = id(db.one("SELECT id FROM order_lines WHERE order_id=?", id), "id");
        postData("/order-lines/" + line + "/cook", map(), 200);
        postData("/order-lines/" + line + "/waste", map("reason", "Preparación quemada"), 200);
        postData("/order-lines/" + line + "/waste", map("reason", "Duplicada"), 422);
        postData("/order-lines/" + line + "/cook", map(), 200);
        postData("/order-lines/" + line + "/ready", map(), 200);
        postData("/order-lines/" + line + "/deliver", map(), 200);
        postData("/order-lines/" + line + "/return", map("reason", "Cliente devuelve", "reprocess", false), 200);
        assertEquals(0, db.sum("SELECT quantity FROM stocks WHERE product_id=1 AND warehouse_id=1")
                .compareTo(before.subtract(new BigDecimal("0.6"))));
        postData("/actions/orders/" + id + "/cancel", map("reason", "Pedido devuelto"), 200);
        assertEquals(0, db.sum("SELECT quantity FROM stocks WHERE product_id=1 AND warehouse_id=1")
                .compareTo(before.subtract(new BigDecimal("0.6"))));
    }

    @Test
    @Order(6)
    void insufficientStockRollsBackCheckout() throws Exception {
        var order = postData("/data/orders", map("table_id", 3, "warehouse_id", 1, "date", today().toString(), "lines",
                List.of(map("dish_id", 2, "quantity", 1000))), 200);
        long id = id(order, "id"), line = id(db.one("SELECT id FROM order_lines WHERE order_id=?", id), "id");
        for (String step : List.of("cook", "ready", "deliver"))
            postData("/order-lines/" + line + "/" + step, map(), 200);
        postData("/actions/orders/" + id + "/bill", map(), 200);
        long count = db.count("SELECT COUNT(*) FROM invoices");
        var before = db.sum("SELECT COALESCE(SUM(balance),0) FROM cash_accounts");
        postData("/actions/orders/" + id + "/checkout", map("document_type", "FACTURA", "document", "20000000003",
                "customer", "Empresa QA", "payment_type", "CONTADO", "cash_account_id", 3), 422);
        assertEquals(count, db.count("SELECT COUNT(*) FROM invoices"));
        assertEquals(before, db.sum("SELECT COALESCE(SUM(balance),0) FROM cash_accounts"));
        assertEquals("PAGAR", db.get("orders", id).get("status"));
    }

    @Test
    @Order(7)
    void employeeAdvanceAndLoanFlowIntoPayrollOnce() throws Exception {
        postData("/data/advances", map("employee_id", 1, "date", today().toString(), "period",
                today().toString().substring(0, 7), "amount", 721), 422);
        var advance = postData("/data/advances", map("employee_id", 1, "date", today().toString(), "period",
                today().toString().substring(0, 7), "amount", 100), 200);
        var payable = db.one("SELECT * FROM payables WHERE source='advances' AND source_id=?", id(advance, "id"));
        postData("/actions/payables/" + id(payable, "id") + "/pay", map("amount", 100, "cash_account_id", 3), 200);
        var loan = postData("/data/loans", map("employee_id", 1, "date", today().toString(), "amount", 300, "months", 3,
                "first_period", today().toString().substring(0, 7)), 200);
        var lp = db.one("SELECT * FROM payables WHERE source='loans' AND source_id=?", id(loan, "id"));
        postData("/actions/payables/" + id(lp, "id") + "/pay", map("amount", 300, "cash_account_id", 3), 200);
        var payroll = postData("/data/payrolls", map("employee_id", 1, "period", today().toString().substring(0, 7)),
                200);
        assertEquals(0, dec(payroll, "advances").compareTo(new BigDecimal("100")));
        assertTrue(dec(payroll, "loans").signum() > 0);
        long id = id(payroll, "id");
        postData("/actions/payrolls/" + id + "/approve", map(), 200);
        postData("/actions/payrolls/" + id + "/approve", map(), 422);
        assertEquals(1, db.count("SELECT COUNT(*) FROM payables WHERE source='payrolls' AND source_id=?", id));
        assertEquals("DESCONTADO", db.get("advances", id(advance, "id")).get("status"));
        assertEquals(0, db.sum("SELECT COALESCE(SUM(debit-credit),0) FROM journal_lines").compareTo(BigDecimal.ZERO));
    }

    @Test
    @Order(8)
    void budgetAndOptimisticLockProtectCommittedValues() throws Exception {
        var b = postData("/data/budgets", map("code", "QA-PRE", "area", "QA", "category", "Pruebas", "start_date",
                today().toString(), "end_date", today().plusDays(10).toString(), "amount", 100), 200);
        var body = map("code", "QA-SRV", "name", "Servicio QA", "partner_id", 1, "date", today().toString(), "due_date",
                today().toString(), "amount", 101, "budget_id", id(b, "id"));
        postData("/data/services", body, 422);
        assertEquals(0, db.count("SELECT COUNT(*) FROM services WHERE code='QA-SRV'"));
        body.put("amount", 90);
        postData("/data/services", body, 200);
        assertEquals(0, dec(db.get("budgets", id(b, "id")), "executed").compareTo(new BigDecimal("90")));
        var response = mvc
                .perform(put("/api/data/budgets/" + id(b, "id")).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(json.writeValueAsString(map("amount", 200, "version", b.get("version")))))
                .andReturn().getResponse();
        assertEquals(422, response.getStatus());
    }

    @Test
    @Order(9)
    void exportsAreRealFilesAndXmlIsWellFormed() throws Exception {
        for (String format : List.of("pdf", "xlsx", "csv")) {
            var res = mvc.perform(get("/api/exports/products." + format).header("Authorization", "Bearer " + token))
                    .andReturn().getResponse();
            assertEquals(200, res.getStatus(), res.getContentAsString());
            assertTrue(res.getContentAsByteArray().length > 50);
            if (format.equals("xlsx"))
                assertEquals('P', res.getContentAsByteArray()[0]);
            if (format.equals("pdf"))
                assertEquals('%', res.getContentAsByteArray()[0]);
        }
        for (String paper : List.of("a4", "80mm")) {
            var res = mvc.perform(get("/api/documents/invoices/" + invoiceId + ".pdf?paper=" + paper)
                    .header("Authorization", "Bearer " + token)).andReturn().getResponse();
            assertEquals(200, res.getStatus());
            assertEquals('%', res.getContentAsByteArray()[0]);
        }
        var res = mvc
                .perform(get("/api/invoices/" + invoiceId + "/draft.xml").header("Authorization", "Bearer " + token))
                .andReturn().getResponse();
        assertEquals(200, res.getStatus());
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        assertNotNull(
                factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(res.getContentAsByteArray())));
        assertTrue(res.getContentAsString().contains("BORRADOR"));
    }

    @Test
    @Order(10)
    void parallelPaymentsCannotOverpay() throws Exception {
        var debt = postData("/data/payables", map("code", "QA-RACE", "party", "QA concurrencia", "date",
                today().toString(), "due_date", today().toString(), "total", 10), 200);
        long id = id(debt, "id");
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> task = () -> mvc
                    .perform(post("/api/actions/payables/" + id + "/pay").header("Authorization", "Bearer " + token)
                            .contentType("application/json")
                            .content(json.writeValueAsString(map("amount", 10, "cash_account_id", 3))))
                    .andReturn().getResponse().getStatus();
            var results = pool.invokeAll(List.of(task, task));
            var status = List.of(results.get(0).get(), results.get(1).get());
            assertTrue(status.contains(200));
            assertTrue(status.contains(422));
            assertEquals(0, dec(db.get("payables", id), "paid").compareTo(new BigDecimal("10")));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Order(11)
    void closedAccountingMonthRejectsJournal() throws Exception {
        String period = today().minusMonths(1).toString().substring(0, 7);
        postData("/data/periods", map("period", period), 200);
        var body = map("date", period + "-01", "description", "Asiento bloqueado", "lines", List
                .of(map("account_id", 1, "debit", 10, "credit", 0), map("account_id", 13, "debit", 0, "credit", 10)));
        postData("/journals", body, 422);
    }

    @Test
    @Order(12)
    void statementsAndLedgerReconcileWithPostedOperations() throws Exception {
        var balance = getData("/financial-reports/balance");
        var summary = (Map<String, Object>) balance.get("summary");
        assertEquals(0, dec(summary, "Diferencia de cuadre").compareTo(BigDecimal.ZERO));
        var trial = getData("/financial-reports/trial");
        assertEquals(0, dec((Map<String, Object>) trial.get("summary"), "Diferencia").compareTo(BigDecimal.ZERO));
        var ledger = getData("/financial-reports/ledger?accountId=1");
        assertNotNull(ledger.get("rows"));
        var before = db.sum("SELECT COALESCE(SUM(balance),0) FROM cash_accounts WHERE kind='EFECTIVO'");
        assertEquals(0,
                dec((Map<String, Object>) ledger.get("summary"), "Saldo final (debe − haber)").compareTo(before));
        for (String format : List.of("pdf", "xlsx", "csv")) {
            var result = mvc.perform(get("/api/financial-reports/income/export?format=" + format)
                    .header("Authorization", "Bearer " + token)).andReturn().getResponse();
            assertEquals(200, result.getStatus(), result.getContentAsString());
            assertTrue(result.getContentAsByteArray().length > 100);
        }
    }

    @Test
    @Order(13)
    void csvImportsAreAtomicAndRejectDuplicateCodes() throws Exception {
        String header = "code,name,category,unit,tax_type,tax_rate,status\n";
        String invalid = header
                + "CSV-ROLLBACK,Insumo CSV,INSUMO,KG,GRAVADO,18,ACTIVO\nCSV-INVALID,,INSUMO,KG,GRAVADO,18,ACTIVO\n";
        var result = mvc.perform(multipart("/api/imports/products")
                .file(new MockMultipartFile("file", "carga.csv", "text/csv",
                        invalid.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + token)).andReturn().getResponse();
        assertEquals(422, result.getStatus(), result.getContentAsString());
        assertEquals(0, db.count("SELECT COUNT(*) FROM products WHERE code='CSV-ROLLBACK'"));
        String valid = header + "CSV-OK,Insumo CSV,INSUMO,KG,GRAVADO,18,ACTIVO\n";
        for (int expected : List.of(200, 409)) {
            var imported = mvc.perform(multipart("/api/imports/products")
                    .file(new MockMultipartFile("file", "carga.csv", "text/csv",
                            valid.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    .header("Authorization", "Bearer " + token)).andReturn().getResponse();
            assertEquals(expected, imported.getStatus(), imported.getContentAsString());
        }
        assertEquals(1, db.count("SELECT COUNT(*) FROM products WHERE code='CSV-OK'"));
    }

    @Test
    @Order(14)
    void everyModuleAndDashboardCanReadRealStoredData() throws Exception {
        var response = mvc.perform(get("/api/catalog").header("Authorization", "Bearer " + token)).andReturn()
                .getResponse();
        assertEquals(200, response.getStatus());
        var catalog = json.readValue(response.getContentAsString(), new TypeReference<List<Map<String, Object>>>() {
        });
        assertEquals(41, catalog.size());
        for (var entity : catalog)
            assertNotNull(getData("/data/" + str(entity, "key") + "?size=5").get("items"));
        for (String path : List.of("/dashboard", "/lookups", "/board", "/cash-status"))
            assertFalse(getData(path).isEmpty());
    }

    @Test
    @Order(15)
    void eventDepositSaleAndCancellationRefundEachPaymentOnce() throws Exception {
        var initial = dec(db.get("cash_accounts", 3), "balance");
        var quote = postData("/data/quotes", map("customer", "Evento QA", "document", "00000000", "date",
                today().toString(), "rental", 20, "discount", 10, "lines", List.of(map("dish_id", 1, "quantity", 1))),
                200);
        long quoteId = id(quote, "id");
        postData("/actions/quotes/" + quoteId + "/accept", map(), 200);
        postData("/actions/quotes/" + quoteId + "/deposit", map("amount", 10, "cash_account_id", 3), 200);
        long order = id(postData("/actions/quotes/" + quoteId + "/convert", map("warehouse_id", 1), 200), "order_id");
        long line = id(db.one("SELECT id FROM order_lines WHERE order_id=?", order), "id");
        for (String step : List.of("cook", "ready", "deliver"))
            postData("/order-lines/" + line + "/" + step, map(), 200);
        postData("/actions/orders/" + order + "/bill", map(), 200);
        long invoice = id(postData("/actions/orders/" + order + "/checkout", map("document_type", "BOLETA",
                "payment_type", "CONTADO", "customer", "Evento QA", "cash_account_id", 3), 200), "id");
        assertEquals("PAGADA", db.get("quotes", quoteId).get("status"));
        postData("/actions/invoices/" + invoice + "/void", map("reason", "Anulación del evento de prueba"), 200);
        assertEquals(initial, dec(db.get("cash_accounts", 3), "balance"));
        assertEquals("ANULADA", db.get("quotes", quoteId).get("status"));
        postData("/actions/invoices/" + invoice + "/void", map("reason", "Duplicada"), 422);
        assertEquals(initial, dec(db.get("cash_accounts", 3), "balance"));
        assertEquals(0, db.sum("SELECT COALESCE(SUM(debit-credit),0) FROM journal_lines").compareTo(BigDecimal.ZERO));
    }

    @Test
    @Order(16)
    void feeDraftCanBeAdjustedBeforeApproval() throws Exception {
        var fee = postData("/data/fees", map("code", "HON-QA", "party", "Servicio QA", "document", "00000002",
                "start_date", today().toString(), "end_date", today().toString(), "amount", 1000, "waiver", "NO"), 200);
        var response = mvc
                .perform(put("/api/data/fees/" + id(fee, "id")).header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(json.writeValueAsString(map("amount", 1800, "version", fee.get("version")))))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus(), response.getContentAsString());
        var changed = parse(response.getContentAsString());
        assertEquals(0, dec(changed, "net").compareTo(new BigDecimal("1656")));
        assertEquals(0, dec(changed, "withheld").compareTo(new BigDecimal("144")));
    }
}
