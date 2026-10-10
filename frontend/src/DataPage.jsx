import React, { useEffect, useState } from "react";
import {
  Plus,
  Search,
  Download,
  ChevronLeft,
  ChevronRight,
  ArrowUpRight,
  Pencil,
  Trash2,
  Paperclip,
  FileText,
  RefreshCw,
  ArrowLeftRight,
} from "lucide-react";
import { api, money, title, download, today } from "./api";
import CsvImport from "./CsvImport";
import { useERP } from "./context";
import {
  Modal,
  Form,
  Field,
  Empty,
  Loading,
  Badge,
  field,
  FileField,
} from "./ui";
const F = field;
const account = F("cash_account_id", "Cuenta de tesorería", "reference", {
  ref: "cash_accounts",
  required: true,
});
const reason = F("reason", "Motivo / referencia", "textarea", {
  required: true,
});
const amount = F("amount", "Monto", "money", { required: true });
const file = F("file_id", "Documento adjunto", "file");
export const labels = {
  pay: "Pagar aquí",
  credit: "Aplicar nota interna",
  reschedule: "Reprogramar",
  send: "Enviar orden",
  receive: "Recibir productos",
  cancel: "Cancelar",
  approve: "Aprobar y registrar",
  reject: "Rechazar",
  settle: "Pagar préstamo total",
  depreciate: "Depreciar mes",
  retire: "Dar de baja",
  clean: "Marcar mesa libre",
  recipe: "Editar receta",
  bill: "Enviar a caja",
  checkout: "Cobrar pedido",
  accept: "Aceptar",
  convert: "Convertir a pedido",
  deposit: "Registrar adelanto",
  void: "Anular y devolver",
  close: "Cerrar caja",
  reverse: "Reversar asiento",
  suggest: "Generar compra sugerida",
};
const noCreate = ["stocks", "receipts", "invoices", "audit", "company"];
export function canEdit(e, r) {
  return (
    (!e.readonly || (e.key === "fees" && r.status === "BORRADOR")) &&
    !["assets"].includes(e.key) &&
    (e.key !== "purchases" || r.status === "BORRADOR") &&
    (e.key !== "dining_tables" || r.status === "LIBRE")
  );
}
export function actionsFor(entity, r) {
  return (entity.actions || []).filter((a) => {
    const s = r.status;
    switch (a) {
      case "send":
        return s === "BORRADOR";
      case "receive":
        return ["ENVIADO", "PARCIAL"].includes(s);
      case "cancel":
        return [
          "BORRADOR",
          "ENVIADO",
          "ABIERTO",
          "PAGAR",
          "COTIZAR",
          "ACEPTADA",
        ].includes(s);
      case "approve":
        return ["BORRADOR", "PENDIENTE"].includes(s);
      case "reject":
        return ["PENDIENTE", "COTIZAR"].includes(s);
      case "pay":
      case "credit":
      case "reschedule":
        return !["PAGADA", "ANULADA"].includes(s);
      case "settle":
        return s === "ACTIVO";
      case "retire":
      case "depreciate":
        return s === "ACTIVO";
      case "clean":
        return s === "LIMPIAR";
      case "bill":
        return s === "ABIERTO";
      case "checkout":
        return s === "PAGAR";
      case "accept":
        return s === "COTIZAR";
      case "convert":
      case "deposit":
        return s === "ACEPTADA";
      case "void":
        return s === "EMITIDO";
      case "close":
        return s === "ABIERTA";
      case "reverse":
        return (
          s === "REGISTRADO" && String(r.reference || "").startsWith("MANUAL")
        );
      default:
        return true;
    }
  });
}
export function useOperations() {
  const ctx = useERP();
  const { lookups, setDialog, refresh, toast, catalog } = ctx;
  const success = async (message = "Operación completada") => {
    setDialog(null);
    toast(message);
    await refresh();
  };
  const run = async (p, b) => {
    await api(p, { method: "POST", body: b });
    await success();
  };
  async function create(entity, preset = {}) {
    const e =
      typeof entity === "string"
        ? catalog.find((e) => e.key === entity)
        : entity;
    let fields = e.fields.filter(
        (f) => !f.readonly && (e.key !== "orders" || f.key !== "quote_id"),
      ),
      lineType = {
        purchases: "purchase",
        orders: "order",
        quotes: "quote",
        journals: "journal",
      }[e.key],
      url = "/data/" + e.key;
    let initial = { ...preset };
    if (e.key === "stock_moves") {
      fields = e.fields.filter((f) => !f.readonly && f.key !== "reference");
      url = "/stock-moves";
    }
    if (e.key === "journals") {
      fields = [
        F("date", "Fecha", "date", { required: true }),
        F("description", "Descripción", "text", { required: true }),
        F("reference", "Referencia"),
      ];
      url = "/journals";
    }
    if (e.key === "cash_sessions") {
      try {
        const a = await api("/cash-status");
        const first = a.accounts.find(
          (x) =>
            x.kind === "EFECTIVO" &&
            !a.sessions.some((s) => s.cash_account_id === x.id),
        );
        if (first)
          initial = {
            cash_account_id: first.id,
            opening: first.balance,
            ...initial,
          };
      } catch {}
    }
    setDialog(
      <Modal
        title={"Nuevo registro · " + e.title}
        wide={!!lineType}
        onClose={() => setDialog(null)}
      >
        <Form
          fields={fields}
          initial={initial}
          module={e.module}
          lineType={lineType}
          initialLines={[]}
          lookups={lookups}
          onClose={() => setDialog(null)}
          onSubmit={(b) => run(url, b)}
        >
          {e.description && <p className="form-note">{e.description}</p>}
          {["benefits", "settlements", "payrolls", "fees"].includes(e.key) && (
            <p className="form-note">
              Revisa los parámetros del contrato y los conceptos con RRHH antes
              de aprobar. Los cálculos de beneficios y liquidación son
              estimaciones parametrizadas.
            </p>
          )}
        </Form>
      </Modal>,
    );
  }
  function edit(e, r) {
    const fields = e.fields.filter((f) => !f.readonly);
    setDialog(
      <Modal title={"Editar · " + e.title} onClose={() => setDialog(null)}>
        <Form
          fields={fields}
          initial={r}
          lookups={lookups}
          module={e.module}
          onClose={() => setDialog(null)}
          onSubmit={async (b) => {
            await api(`/data/${e.key}/${r.id}`, {
              method: "PUT",
              body: { ...b, version: r.version },
            });
            await success("Registro actualizado");
          }}
        />
      </Modal>,
    );
  }
  function remove(e, r) {
    setDialog(
      <Modal title="Eliminar registro" onClose={() => setDialog(null)}>
        <Form
          fields={[]}
          lookups={lookups}
          onClose={() => setDialog(null)}
          submitLabel="Eliminar registro"
          onSubmit={async () => {
            await api(`/data/${e.key}/${r.id}`, { method: "DELETE" });
            await success("Registro eliminado");
          }}
        >
          <p>
            Se eliminará el registro #{r.id} de {e.title}. Los registros con
            movimientos asociados se conservarán.
          </p>
        </Form>
      </Modal>,
    );
  }
  async function action(e, r, a) {
    try {
      let fields = [],
        initial = {},
        lineType,
        initialLines = [];
      if (["pay", "deposit"].includes(a)) {
        fields = [amount, account, file];
        initial.amount =
          a === "pay"
            ? Number(r.total) - Number(r.paid || 0) - Number(r.credit || 0)
            : 0;
      }
      if (a === "credit") fields = [amount, reason];
      if (a === "reschedule")
        fields = [
          F("due_date", "Nuevo vencimiento", "date", { required: true }),
          reason,
        ];
      if (["cancel", "retire", "void"].includes(a)) fields = [reason];
      if (a === "depreciate")
        fields = [F("period", "Período AAAA-MM", "text", { required: true })];
      if (a === "settle") fields = [account];
      if (a === "close") {
        fields = [
          F("counted", "Efectivo contado", "money", { required: true }),
        ];
        const cash = await api("/cash-status");
        initial.counted =
          cash.accounts.find((c) => c.id === r.cash_account_id)?.balance || 0;
      }
      if (a === "convert")
        fields = [
          F("warehouse_id", "Almacén de insumos", "reference", {
            ref: "warehouses",
            required: true,
          }),
        ];
      if (a === "bill")
        fields = [F("discount_code", "Código de descuento (opcional)")];
      if (a === "checkout") {
        fields = [
          F("document_type", "Comprobante", "select", {
            options: ["BOLETA", "FACTURA"],
          }),
          F("payment_type", "Condición", "select", {
            options: ["CONTADO", "CREDITO"],
          }),
          F("customer", "Nombre / razón social"),
          F("document", "DNI / RUC"),
          F("address", "Dirección"),
          account,
          F("amount", "Abono inicial si es a crédito", "money"),
          F("due_date", "Vencimiento si es a crédito", "date"),
        ];
        initial.customer = r.customer || "";
        initial.amount = 0;
      }
      if (a === "receive") {
        const d = await api(`/data/purchases/${r.id}`);
        fields = [
          F("reference", "Documento del proveedor", "text", { required: true }),
          { ...file, required: true },
        ];
        lineType = "receive";
        initialLines = d.lines
          .filter((l) => Number(l.quantity) > Number(l.received))
          .map((l) => ({
            line_id: l.id,
            label:
              lookups.products?.find((p) => p.id === l.product_id)?.label ||
              "#" + l.product_id,
            quantity: Number(l.quantity) - Number(l.received),
            remaining: Number(l.quantity) - Number(l.received),
            unit_cost: l.unit_cost,
          }));
      }
      if (a === "recipe") {
        const d = await api("/data/dishes/" + r.id);
        lineType = "recipe";
        initialLines = d.lines;
      }
      setDialog(
        <Modal
          title={
            a === "pay" && e.key === "receivables"
              ? "Registrar cobro"
              : labels[a]
          }
          wide={!!lineType}
          onClose={() => setDialog(null)}
        >
          <Form
            fields={fields}
            initial={initial}
            module={e.module}
            lineType={lineType}
            initialLines={initialLines}
            lookups={lookups}
            submitLabel={a === "checkout" ? "Confirmar cobro" : labels[a]}
            onClose={() => setDialog(null)}
            onSubmit={(b) => run(`/actions/${e.key}/${r.id}/${a}`, b)}
          >
            <div className="operation-summary">
              <span>
                {e.title} · #{r.id}
              </span>
              <strong>{r.name || r.party || r.code || r.number || ""}</strong>
              {(r.total !== undefined ||
                r.net !== undefined ||
                r.amount !== undefined) && (
                <b>{money(r.net ?? r.total ?? r.amount)}</b>
              )}
            </div>
            {a === "checkout" && (
              <p className="form-note">
                Al contado se cobra el saldo completo. En crédito se registra el
                abono y el saldo pasa a cuentas por cobrar. Los comprobantes son
                internos, sin validez tributaria.
              </p>
            )}
            {a === "recipe" && (
              <p className="form-note">
                Indica las cantidades por una porción usando la unidad base de
                cada insumo. Los pedidos existentes conservan su receta
                original.
              </p>
            )}
            {a === "close" && (
              <p className="form-note">
                La diferencia del arqueo queda registrada. Cerrar no modifica el
                saldo contable; cualquier regularización debe registrarse por
                separado.
              </p>
            )}
          </Form>
        </Modal>,
      );
    } catch (error) {
      toast(error.message, "error");
    }
  }
  async function detail(e, r) {
    try {
      const d = await api(`/data/${e.key}/${r.id}`);
      setDialog(
        <Detail
          entity={e}
          row={d}
          close={() => setDialog(null)}
          operations={{ action, edit, remove }}
        />,
      );
    } catch (e) {
      toast(e.message, "error");
    }
  }
  function transfer() {
    setDialog(
      <Modal title="Transferir entre cuentas" onClose={() => setDialog(null)}>
        <Form
          fields={[
            F("from_id", "Cuenta origen", "reference", {
              ref: "cash_accounts",
              required: true,
            }),
            F("to_id", "Cuenta destino", "reference", {
              ref: "cash_accounts",
              required: true,
            }),
            amount,
          ]}
          module="accounting"
          lookups={lookups}
          onClose={() => setDialog(null)}
          onSubmit={(b) => run("/transfers", b)}
        />
      </Modal>,
    );
  }
  return { create, edit, remove, action, detail, transfer, success };
}
function Value({ f, row, lookups }) {
  const v = row[f.key];
  if (v === null || v === undefined || v === "")
    return <span className="muted">—</span>;
  if (f.key === "status") return <Badge value={v} />;
  if (f.type === "reference")
    return (
      lookups[f.ref]?.find((x) => Number(x.id) === Number(v))?.label || "#" + v
    );
  if (f.type === "money")
    return /rate|percent|factor|discount/.test(f.key)
      ? Number(v).toLocaleString("es-PE")
      : money(v);
  if (f.type === "file")
    return (
      <span className="file-indicator">
        <Paperclip size={14} />
        Adjunto
      </span>
    );
  if (f.type === "number") return Number(v).toLocaleString("es-PE");
  return <span>{String(v)}</span>;
}
function Detail({ entity: e, row: r, close, operations }) {
  const { lookups, can, toast } = useERP();
  const [attached, setAttached] = useState(r.file_id || "");
  async function dl(path, name) {
    try {
      await download(path, name);
    } catch (e) {
      toast(e.message, "error");
    }
  }
  return (
    <Modal title={`${e.title} · #${r.id}`} wide onClose={close}>
      <div className="modal-body">
        <div className="detail-tools">
          {can(e.module, "write") && canEdit(e, r) && (
            <button
              className="button secondary small"
              onClick={() => operations.edit(e, r)}
            >
              <Pencil size={14} />
              Editar
            </button>
          )}
          {can(e.module, "export") && (
            <button
              className="button secondary small"
              onClick={() =>
                dl(`/documents/${e.key}/${r.id}.pdf`, `${e.key}-${r.id}.pdf`)
              }
            >
              <FileText size={15} />
              PDF
            </button>
          )}
          {e.key === "invoices" && can(e.module, "export") && (
            <>
              <button
                className="button secondary small"
                onClick={() =>
                  dl(
                    `/documents/invoices/${r.id}.pdf?paper=80mm`,
                    r.number + "-80mm.pdf",
                  )
                }
              >
                Ticket 80 mm
              </button>
              <button
                className="button secondary small"
                onClick={() =>
                  dl(`/invoices/${r.id}/draft.xml`, r.number + "-BORRADOR.xml")
                }
              >
                XML borrador
              </button>
              <button
                className="button secondary small"
                onClick={() =>
                  dl(`/invoices/${r.id}/payload`, r.number + ".json")
                }
              >
                JSON
              </button>
            </>
          )}
          {actionsFor(e, r).map(
            (a) =>
              can(
                e.module,
                ["recipe", "clean", "bill", "send", "convert"].includes(a)
                  ? "write"
                  : "approve",
              ) && (
                <button
                  key={a}
                  className="button small"
                  onClick={() => operations.action(e, r, a)}
                >
                  {a === "pay" && e.key === "receivables"
                    ? "Registrar cobro"
                    : labels[a]}
                </button>
              ),
          )}
        </div>
        <div className="detail-grid">
          {e.fields
            .filter((f) => f.type !== "password")
            .map((f) => (
              <div key={f.key}>
                <small>{f.label}</small>
                {f.type === "file" && r[f.key] ? (
                  <button
                    className="link"
                    onClick={() => dl("/files/" + r[f.key], "adjunto")}
                  >
                    <Download size={14} />
                    Descargar
                  </button>
                ) : (
                  <strong>
                    <Value f={f} row={r} lookups={lookups} />
                  </strong>
                )}
              </div>
            ))}
        </div>
        {r.lines?.length > 0 && (
          <div className="detail-lines">
            <h3>Detalle</h3>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    {Object.keys(r.lines[0])
                      .filter(
                        (k) =>
                          ![
                            "recipe_snapshot",
                            "created_at",
                            "consumed",
                            "notes",
                          ].includes(k),
                      )
                      .map((k) => (
                        <th key={k}>{title(k)}</th>
                      ))}
                  </tr>
                </thead>
                <tbody>
                  {r.lines.map((l, i) => (
                    <tr key={i}>
                      {Object.entries(l)
                        .filter(
                          ([k]) =>
                            ![
                              "recipe_snapshot",
                              "created_at",
                              "consumed",
                              "notes",
                            ].includes(k),
                        )
                        .map(([k, v]) => (
                          <td key={k}>
                            {k === "product_id"
                              ? lookups.products?.find((p) => p.id === v)
                                  ?.label || v
                              : k === "dish_id"
                                ? lookups.dishes?.find((p) => p.id === v)
                                    ?.label || v
                                : k === "account_id"
                                  ? lookups.accounts?.find((p) => p.id === v)
                                      ?.label || v
                                  : String(v ?? "—")}
                          </td>
                        ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
        {r.payments?.length > 0 && (
          <section>
            <h3>Pagos registrados</h3>
            {r.payments.map((p) => (
              <div className="list-row" key={p.id}>
                <span>
                  {p.date} ·{" "}
                  {
                    lookups.cash_accounts?.find(
                      (c) => c.id === p.cash_account_id,
                    )?.label
                  }
                </span>
                <strong>{money(p.amount)}</strong>
              </div>
            ))}
          </section>
        )}
        {can(e.module, "write") &&
          e.fields.some((f) => f.key === "file_id") && (
            <section className="attach-section">
              <h3>Actualizar documento de respaldo</h3>
              <FileField
                module={e.module}
                value={attached}
                onChange={async (v) => {
                  try {
                    await api(`/attachments/${e.key}/${r.id}`, {
                      method: "POST",
                      body: { file_id: v },
                    });
                    setAttached(v);
                    toast("Documento adjuntado");
                  } catch (e) {
                    toast(e.message, "error");
                  }
                }}
              />
            </section>
          )}
      </div>
      <footer>
        <small>
          Registro #{r.id} · Creado{" "}
          {String(r.created_at || "")
            .slice(0, 16)
            .replace("T", " ")}
        </small>
        <button className="button secondary" onClick={close}>
          Cerrar detalle
        </button>
      </footer>
    </Modal>
  );
}
export default function DataPage({ entity: e }) {
  const { lookups, can, tick, toast, setDialog } = useERP();
  const ops = useOperations();
  const [data, setData] = useState(null),
    [q, setQ] = useState(""),
    [status, setStatus] = useState(""),
    [from, setFrom] = useState(""),
    [to, setTo] = useState(""),
    [page, setPage] = useState(0),
    [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    setError("");
    const t = setTimeout(
      () =>
        api(
          `/data/${e.key}?${new URLSearchParams({ q, status, from, to, page, size: 20 })}`,
        )
          .then((r) => {
            if (active) setData(r);
          })
          .catch((e) => {
            if (active) setError(e.message);
          }),
      120,
    );
    return () => {
      active = false;
      clearTimeout(t);
    };
  }, [e.key, q, status, from, to, page, tick]);
  useEffect(() => {
    setPage(0);
    setData(null);
    setQ("");
    setStatus("");
  }, [e.key]);
  const cols = e.fields.filter(
    (f) => !["file", "textarea", "password"].includes(f.type),
  );
  let chosen = cols.slice(0, 6);
  const state = cols.find((f) => f.key === "status");
  if (state && !chosen.includes(state)) chosen.push(state);
  if (["payables", "receivables"].includes(e.key))
    chosen = e.fields.filter((f) =>
      ["code", "party", "due_date", "total", "paid", "status"].includes(f.key),
    );
  const statusField = e.fields.find((f) => f.key === "status");
  async function exp(format) {
    try {
      await download(
        `/exports/${e.key}.${format}?${new URLSearchParams({ q, status, from, to })}`,
        `${e.key}.${format}`,
      );
    } catch (e) {
      toast(e.message, "error");
    }
  }
  return (
    <div className="page enter">
      <div className="page-heading">
        <div>
          <span className="eyebrow">GESTIÓN / {title(e.module)}</span>
          <h1>{e.title}</h1>
          <p>
            {e.description ||
              "Consulta, registra y da seguimiento a tus operaciones."}
          </p>
        </div>
        <div className="heading-actions">
          {["products", "partners", "accounts"].includes(e.key) &&
            can(e.module, "write") && (
              <button
                className="button secondary"
                onClick={() =>
                  setDialog(
                    <CsvImport entity={e} onClose={() => setDialog(null)} />,
                  )
                }
              >
                Importar CSV
              </button>
            )}
          {e.key === "treasury" && can(e.module, "approve") && (
            <button className="button secondary" onClick={ops.transfer}>
              <ArrowLeftRight size={16} />
              Transferir
            </button>
          )}
          {can(e.module, "write") && !noCreate.includes(e.key) && (
            <button className="button" onClick={() => ops.create(e)}>
              <Plus size={18} />
              Nuevo registro
            </button>
          )}
          {e.key === "company" && can("admin", "write") && data?.items?.[0] && (
            <button
              className="button"
              onClick={() => ops.edit(e, data.items[0])}
            >
              <Pencil size={16} />
              Editar negocio
            </button>
          )}
        </div>
      </div>
      {["invoices", "benefits", "settlements"].includes(e.key) && (
        <p className="notice">
          {e.key === "invoices"
            ? "Los comprobantes generados son internos. Los XML son borradores sin firma ni aceptación de SUNAT."
            : "Los importes son estimaciones según los parámetros del contrato. Revisa las bases y los períodos computables antes de aprobar."}
        </p>
      )}
      <section className="panel">
        <div className="table-toolbar">
          <div className="search-box">
            <Search size={16} />
            <input
              aria-label="Buscar registros"
              placeholder="Buscar por nombre o código…"
              value={q}
              onChange={(ev) => {
                setQ(ev.target.value);
                setPage(0);
              }}
            />
          </div>
          {statusField && (
            <select
              aria-label="Filtrar estado"
              value={status}
              onChange={(ev) => {
                setStatus(ev.target.value);
                setPage(0);
              }}
            >
              <option value="">Todos los estados</option>
              {statusField.options.map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
          )}
          <div className="date-filter">
            <input
              aria-label="Desde"
              type="date"
              value={from}
              onChange={(e) => {
                setFrom(e.target.value);
                setPage(0);
              }}
            />
            <span>—</span>
            <input
              aria-label="Hasta"
              type="date"
              value={to}
              onChange={(e) => {
                setTo(e.target.value);
                setPage(0);
              }}
            />
          </div>
          {can(e.module, "export") && (
            <div className="export-group">
              {["xlsx", "pdf", "csv"].map((x) => (
                <button
                  key={x}
                  className="icon-text"
                  onClick={() => exp(x)}
                  title={"Exportar " + x}
                >
                  <Download size={14} />
                  {x.toUpperCase()}
                </button>
              ))}
            </div>
          )}
        </div>
        {error ? (
          <div className="error">{error}</div>
        ) : !data ? (
          <Loading />
        ) : !data.items.length ? (
          <Empty detail="Usa Nuevo registro para empezar o ajusta los filtros." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  {chosen.map((f) => (
                    <th key={f.key}>{f.label}</th>
                  ))}
                  <th className="right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((r) => (
                  <tr key={r.id}>
                    <td className="muted">{String(r.id).padStart(3, "0")}</td>
                    {chosen.map((f) => (
                      <td key={f.key}>
                        <Value f={f} row={r} lookups={lookups} />
                      </td>
                    ))}
                    <td>
                      <div className="row-actions">
                        <button
                          className="icon"
                          title="Ver detalle"
                          aria-label={`Ver registro ${r.id}`}
                          onClick={() => ops.detail(e, r)}
                        >
                          <ArrowUpRight size={17} />
                        </button>
                        {can(e.module, "write") && canEdit(e, r) && (
                          <button
                            className="icon"
                            title="Editar"
                            onClick={() => ops.edit(e, r)}
                          >
                            <Pencil size={15} />
                          </button>
                        )}
                        {can(e.module, "write") &&
                          (!e.readonly ||
                            ([
                              "payrolls",
                              "fees",
                              "benefits",
                              "settlements",
                            ].includes(e.key) &&
                              r.status === "BORRADOR")) &&
                          ![
                            "company",
                            "cash_accounts",
                            "users",
                            "assets",
                            "purchases",
                            "periods",
                          ].includes(e.key) && (
                            <button
                              className="icon danger"
                              aria-label={`Eliminar registro ${r.id}`}
                              onClick={() => ops.remove(e, r)}
                            >
                              <Trash2 size={15} />
                            </button>
                          )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="table-footer">
          <span>
            {data?.total || 0} registros · Página {page + 1}
          </span>
          <div>
            <button
              className="icon"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
              aria-label="Página anterior"
            >
              <ChevronLeft size={18} />
            </button>
            <button
              className="icon"
              disabled={!data || (page + 1) * 20 >= data.total}
              onClick={() => setPage(page + 1)}
              aria-label="Página siguiente"
            >
              <ChevronRight size={18} />
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}
