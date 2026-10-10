package pe.eltumi.erp;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import static pe.eltumi.erp.Store.*;

@RestController
@RequestMapping("/api")
public class InsightsController {
    final Store db;
    final Access access;
    @Value("${app.ollama-enabled}")
    boolean enabled;
    @Value("${app.ollama-url}")
    String url;
    @Value("${app.ollama-model}")
    String model;

    public InsightsController(Store d, Access a) {
        db = d;
        access = a;
    }

    @GetMapping("/cash-status")
    public Object cash() {
        access.module("sales", "read");
        return map("accounts", db.rows("SELECT id,name,kind,balance FROM cash_accounts WHERE status='ACTIVO'"),
                "sessions", db.rows("SELECT id,cash_account_id,status FROM cash_sessions WHERE status='ABIERTA'"));
    }

    @PostMapping("/insights")
    public Object insights(@RequestBody Map<String, Object> b) throws Exception {
        access.module("finance", "read");
        String question = str(b, "question");
        require(question.length() <= 1000, "La pregunta es demasiado larga");
        var cash = db.sum("SELECT COALESCE(SUM(balance),0) FROM cash_accounts");
        var incoming = db.sum(
                "SELECT COALESCE(SUM(total-paid-credit),0) FROM receivables WHERE status NOT IN ('PAGADA','ANULADA')");
        var outgoing = db.sum(
                "SELECT COALESCE(SUM(total-paid-credit),0) FROM payables WHERE status NOT IN ('PAGADA','ANULADA')");
        var projection = cash.add(incoming).subtract(outgoing);
        String facts = "Saldos actuales: S/ " + cash + ". Por cobrar: S/ " + incoming + ". Por pagar: S/ " + outgoing
                + ". Proyección simple: S/ " + projection + ".";
        String answer = facts + (projection.signum() < 0
                ? " Los compromisos superan los recursos proyectados. Revisa los vencimientos y el calendario de cobros antes de aprobar nuevas salidas."
                : " La proyección simple cubre los compromisos registrados. Revisa los vencimientos antes de programar pagos.");
        String source = "REGLAS";
        if (enabled) {
            try {
                var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
                var payload = map("model", model, "stream", false, "prompt",
                        "Eres un asistente de gestión del restaurante El Tumi. Responde en español con hasta 100 palabras. No inventes cifras ni ofrezcas asesoría legal. Solo puedes explicar los datos agregados dados, no ejecutar operaciones. Datos verificados: "
                                + facts + " Pregunta: " + question);
                var request = HttpRequest.newBuilder(URI.create(url + "/api/generate")).timeout(Duration.ofSeconds(45))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(db.json(payload))).build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200)
                    throw new IllegalStateException();
                Map<String, Object> result = db.json.readValue(response.body(),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {
                        });
                answer = str(result, "response");
                source = "OLLAMA";
            } catch (Exception e) {
                answer += " El modelo local no está disponible; se muestra el análisis por reglas.";
            }
        }
        return map("answer", answer, "source", source, "facts", facts);
    }
}
