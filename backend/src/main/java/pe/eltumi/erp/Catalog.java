package pe.eltumi.erp;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.time.*;
import static pe.eltumi.erp.Store.*;

@Component
public class Catalog {
    public record Field(String key, String label, String type, boolean required, boolean readonly, Object defaultValue,
            String ref, List<String> options, Double min) {
    }

    public final List<Map<String, Object>> entities;

    public Catalog(ObjectMapper mapper) throws Exception {
        entities = mapper.readValue(getClass().getResourceAsStream("/catalog.json"), new TypeReference<>() {
        });
    }

    public Map<String, Object> entity(String key) {
        return entities.stream().filter(x -> key.equals(x.get("key"))).findFirst()
                .orElseThrow(() -> new Problem(404, "Módulo no encontrado"));
    }

    public String module(String key) {
        return str(entity(key), "module");
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fields(String key) {
        return (List<Map<String, Object>>) entity(key).get("fields");
    }

    public Map<String, Object> clean(String key, Map<String, Object> body, boolean editing) {
        var out = new LinkedHashMap<String, Object>();
        var allowed = new HashSet<String>(List.of("lines", "version"));
        for (var f : fields(key)) {
            String k = str(f, "key");
            allowed.add(k);
            if (Boolean.TRUE.equals(f.get("readonly")))
                continue;
            Object v = body.get(k);
            if (v == null && !editing)
                v = f.get("default");
            if (v instanceof String s)
                v = s.trim();
            if (v != null && v.toString().isBlank())
                v = null;
            if (editing && !body.containsKey(k))
                continue;

            require(v != null || !Boolean.TRUE.equals(f.get("required")), str(f, "label") + " es obligatorio");
            if (v != null) {
                String t = str(f, "type");
                String s = v.toString();
                require(s.length() <= (t.equals("textarea") ? 5000 : 255), str(f, "label") + " es demasiado largo");
                switch (t) {
                    case "date" -> v = LocalDate.parse(s).toString();
                    case "datetime" -> v = LocalDateTime.parse(s).toString();
                    case "money", "number" -> {
                        var z = new java.math.BigDecimal(s);
                        require(z.signum() >= 0 && z.compareTo(new java.math.BigDecimal("9999999999")) < 0,
                                "Monto fuera de rango");
                        v = t.equals("money") ? money(z) : z.setScale(4, java.math.RoundingMode.HALF_UP);
                    }
                    case "reference" -> {
                        long id = Long.parseLong(s);
                        require(id > 0, "Referencia inválida");
                        v = id;
                    }
                    case "select" ->
                        require(((List<?>) f.get("options")).contains(s), "Opción inválida: " + str(f, "label"));
                    case "email" -> require(s.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "Correo inválido");
                    default -> {
                    }
                }
                if (k.equals("period") || k.equals("first_period"))
                    YearMonth.parse(s);
            }
            out.put(k, v);
        }
        for (String k : body.keySet())
            require(allowed.contains(k), "Campo no permitido: " + k);
        return out;
    }
}
