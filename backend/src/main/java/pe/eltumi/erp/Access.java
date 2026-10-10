package pe.eltumi.erp;

import org.springframework.stereotype.Component;
import static pe.eltumi.erp.Store.*;

@Component
public class Access {
    private final Store db;
    private final Catalog cat;

    public Access(Store d, Catalog c) {
        db = d;
        cat = c;
    }

    public boolean allowed(String module, String action) {
        if (Security.user().role().equals("ADMIN"))
            return true;
        require(java.util.List.of("read", "write", "approve", "export").contains(action), "Permiso inválido");
        return db.count("SELECT COUNT(*) FROM permissions WHERE role=? AND module=? AND can_" + action + "=TRUE",
                Security.user().role(), module) > 0;
    }

    public void module(String module, String action) {
        if (!allowed(module, action))
            throw new Problem(403, "Tu rol no tiene permiso para esta operación");
    }

    public void entity(String entity, String action) {
        module(cat.module(entity), action);
    }

    public void audit(String action, String entity, long id, String detail) {
        db.insert("audit", map("username", Security.user().email(), "action", action, "entity", entity, "entity_id", id,
                "detail", detail));
    }
}
