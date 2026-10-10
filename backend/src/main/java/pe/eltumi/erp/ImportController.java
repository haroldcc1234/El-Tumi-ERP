package pe.eltumi.erp;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static pe.eltumi.erp.Store.*;

@RestController
@RequestMapping("/api/imports")
public class ImportController {
    final ErpService erp;
    final Access access;
    final Catalog catalog;

    public ImportController(ErpService e, Access a, Catalog c) {
        erp = e;
        access = a;
        catalog = c;
    }

    @PostMapping("/{entity}")
    @Transactional
    public Object upload(@PathVariable String entity, @RequestParam MultipartFile file) throws Exception {
        require(List.of("accounts", "products", "partners").contains(entity),
                "La importación admite plan contable, productos y terceros");
        access.entity(entity, "write");
        require(file.getSize() <= 2 * 1024 * 1024, "El CSV no debe superar 2 MB");
        String data = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\ufeff", "");
        var rows = parse(data);
        require(rows.size() > 1 && rows.size() <= 501, "Carga de 1 a 500 registros");
        var keys = rows.get(0);
        Set<String> allowed = new HashSet<>();
        for (var f : catalog.fields(entity))
            if (!Boolean.TRUE.equals(f.get("readonly")))
                allowed.add(str(f, "key"));
        require(new HashSet<>(keys).size() == keys.size() && allowed.containsAll(keys),
                "Encabezados inválidos. Usa los nombres técnicos de la plantilla CSV");
        int created = 0;
        for (int i = 1; i < rows.size(); i++) {
            var line = rows.get(i);
            if (line.stream().allMatch(String::isBlank))
                continue;
            require(line.size() == keys.size(), "Cantidad de columnas inválida en fila " + (i + 1));
            var body = new LinkedHashMap<String, Object>();
            for (int j = 0; j < keys.size(); j++)
                if (!line.get(j).isBlank())
                    body.put(keys.get(j), line.get(j));
            try {
                erp.create(entity, body);
            } catch (Problem e) {
                throw new Problem(e.status, "Fila " + (i + 1) + ": " + e.getMessage());
            }
            created++;
        }
        return map("created", created);
    }

    static List<List<String>> parse(String input) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                if (quote && i + 1 < input.length() && input.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else
                    quote = !quote;
            } else if (c == ',' && !quote) {
                row.add(cell.toString().trim());
                cell.setLength(0);
            } else if ((c == '\n' || c == '\r') && !quote) {
                if (c == '\r' && i + 1 < input.length() && input.charAt(i + 1) == '\n')
                    i++;
                row.add(cell.toString().trim());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else
                cell.append(c);
        }
        require(!quote, "El CSV contiene comillas sin cerrar");
        if (!row.isEmpty() || !cell.isEmpty()) {
            row.add(cell.toString().trim());
            rows.add(row);
        }
        return rows;
    }
}
