import React, { useEffect, useRef, useState } from "react";
import {
  X,
  Plus,
  Trash2,
  Upload,
  CheckCircle2,
  Inbox,
  LoaderCircle,
  ArrowUpRight,
  Download,
} from "lucide-react";
import { api, title, money, today, download } from "./api";
export function TumiMark() {
  return (
    <svg viewBox="0 0 48 52" fill="none" aria-hidden="true">
      <path
        d="M5 23a19 19 0 0 1 38 0H5Z"
        stroke="currentColor"
        strokeWidth="2.5"
      />
      <path
        d="M12 22a12 12 0 0 1 24 0M24 4v10M12 9l6 8M36 9l-6 8M19 24h10v13l9 8a23 23 0 0 1-28 0l9-8V24Z"
        stroke="currentColor"
        strokeWidth="2.5"
      />
      <circle cx="24" cy="29" r="2" fill="currentColor" />
    </svg>
  );
}
export function Badge({ value }) {
  const good = [
    "ACTIVO",
    "PAGADA",
    "PAGADO",
    "ENTREGADO",
    "ENTREGADA",
    "LIBRE",
    "APROBADO",
    "APROBADA",
    "EMITIDO",
    "TERMINADO",
  ];
  const bad = [
    "ANULADO",
    "ANULADA",
    "RECHAZADA",
    "INACTIVO",
    "BLOQUEADO",
    "CANCELADO",
    "FALTA",
    "BAJA",
  ];
  return (
    <span
      className={`badge ${good.includes(value) ? "green" : bad.includes(value) ? "red" : "amber"}`}
    >
      {title(value) || "—"}
    </span>
  );
}
export function Empty({
  text = "Todavía no hay registros.",
  detail = "Los nuevos registros aparecerán aquí.",
}) {
  return (
    <div className="empty">
      <Inbox size={30} />
      <strong>{text}</strong>
      <p>{detail}</p>
    </div>
  );
}
export function Loading() {
  return (
    <div className="loading">
      <LoaderCircle className="spin" /> Cargando información…
    </div>
  );
}
export function Modal({ title: heading, children, onClose, wide = false }) {
  const panel = useRef(null);
  useEffect(() => {
    const prior = document.activeElement;
    const body = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    panel.current?.focus();
    const key = (e) => {
      if (e.key === "Escape") onClose();
      if (e.key === "Tab") {
        const nodes = panel.current.querySelectorAll(
          "button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),a[href]",
        );
        if (!nodes.length) return;
        const first = nodes[0],
          last = nodes[nodes.length - 1];
        if (
          e.shiftKey &&
          (document.activeElement === first ||
            document.activeElement === panel.current)
        ) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };
    document.addEventListener("keydown", key);
    return () => {
      document.body.style.overflow = body;
      document.removeEventListener("keydown", key);
      prior?.focus();
    };
  }, []);
  return (
    <div
      className="overlay"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <section
        className={`modal ${wide ? "wide" : ""}`}
        role="dialog"
        aria-modal="true"
        aria-label={heading}
        tabIndex={-1}
        ref={panel}
      >
        <header>
          <div>
            <span className="eyebrow">EL TUMI / GESTIÓN</span>
            <h2>{heading}</h2>
          </div>
          <button className="icon" aria-label="Cerrar" onClick={onClose}>
            <X />
          </button>
        </header>
        {children}
      </section>
    </div>
  );
}
export function FileField({ value, onChange, module = "admin", label }) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  return (
    <div className="file-field">
      <label className="file-button">
        <Upload size={16} />
        {busy
          ? "Subiendo…"
          : value
            ? "Reemplazar archivo"
            : "Adjuntar PDF o imagen"}
        <input
          type="file"
          accept="application/pdf,image/png,image/jpeg"
          disabled={busy}
          aria-label={label || "Adjuntar archivo"}
          onChange={async (e) => {
            const file = e.target.files?.[0];
            if (!file) return;
            setBusy(true);
            setError("");
            try {
              const body = new FormData();
              body.append("file", file);
              body.append("module", module);
              const r = await api("/files", { method: "POST", body });
              onChange(r.id);
            } catch (e) {
              setError(e.message);
            } finally {
              setBusy(false);
              e.target.value = "";
            }
          }}
        />
      </label>
      {value && (
        <button
          type="button"
          className="link"
          onClick={() =>
            download("/files/" + value, "documento").catch((e) =>
              setError(e.message),
            )
          }
        >
          <CheckCircle2 size={14} />
          Ver archivo
        </button>
      )}
      {error && <small className="error-text">{error}</small>}
    </div>
  );
}
export function Field({ field: f, value, onChange, lookups = {}, module }) {
  const props = {
    id: "f-" + f.key,
    name: f.key,
    value: value ?? "",
    required: f.required,
    onChange: (e) => onChange(e.target.value),
    disabled: f.disabled,
  };
  let input;
  if (f.type === "reference")
    input = (
      <select {...props}>
        <option value="">Seleccionar…</option>
        {(lookups[f.ref] || []).map((o) => (
          <option key={o.id} value={o.id}>
            {o.label}
          </option>
        ))}
      </select>
    );
  else if (f.type === "select")
    input = (
      <select {...props}>
        {(f.options || []).map((o) => (
          <option key={o} value={o}>
            {title(o)}
          </option>
        ))}
      </select>
    );
  else if (f.type === "textarea") input = <textarea {...props} rows={3} />;
  else if (f.type === "file")
    input = (
      <FileField
        value={value}
        onChange={onChange}
        module={module}
        label={f.label}
      />
    );
  else
    input = (
      <input
        {...props}
        type={
          f.type === "datetime"
            ? "datetime-local"
            : ["number", "money"].includes(f.type)
              ? "number"
              : f.type === "email"
                ? "email"
                : f.type === "password"
                  ? "password"
                  : f.type === "date"
                    ? "date"
                    : "text"
        }
        min={f.min ?? (["money", "number"].includes(f.type) ? 0 : undefined)}
        step={
          f.type === "money"
            ? "0.01"
            : f.type === "number"
              ? "0.0001"
              : undefined
        }
        maxLength={f.type === "password" ? 72 : 255}
        placeholder={f.placeholder || ""}
        autoComplete={f.type === "password" ? "new-password" : undefined}
      />
    );
  return (
    <div className={`field ${f.type === "textarea" ? "full" : ""}`}>
      <label htmlFor={"f-" + f.key}>
        {f.label}
        {f.required && <span> *</span>}
      </label>
      {input}
    </div>
  );
}
export function LinesEditor({ type, lines, setLines, lookups, original = [] }) {
  const ref =
    type === "journal"
      ? "accounts"
      : ["purchase", "recipe"].includes(type)
        ? "products"
        : "dishes";
  const key =
    type === "journal"
      ? "account_id"
      : ["purchase", "recipe"].includes(type)
        ? "product_id"
        : "dish_id";
  const set = (i, k, v) =>
    setLines(lines.map((l, n) => (n === i ? { ...l, [k]: v } : l)));
  return (
    <div className="lines-editor">
      <div className="section-line">
        <h3>
          {type === "recipe"
            ? "Ingredientes y preparación"
            : type === "journal"
              ? "Detalle del asiento"
              : "Detalle de la operación"}
        </h3>
        {type !== "receive" && (
          <button
            type="button"
            className="button secondary small"
            onClick={() =>
              setLines([
                ...lines,
                {
                  [key]: "",
                  quantity: 1,
                  unit_cost: 0,
                  debit: 0,
                  credit: 0,
                  notes: "",
                  process: "",
                },
              ])
            }
          >
            <Plus size={15} />
            Agregar línea
          </button>
        )}
      </div>
      {lines.length === 0 && (
        <p className="muted">Agrega el primer elemento de este registro.</p>
      )}
      {lines.map((l, i) => (
        <div className="line-row" key={i}>
          <div className="line-main">
            {type === "receive" ? (
              <>
                <strong>{l.label}</strong>
                <small>Pendiente: {l.remaining}</small>
              </>
            ) : (
              <label>
                {type === "journal"
                  ? "Cuenta"
                  : type === "recipe"
                    ? "Insumo"
                    : "Producto"}
                <select
                  value={l[key] || ""}
                  onChange={(e) => set(i, key, e.target.value)}
                  required
                >
                  <option value="">Seleccionar…</option>
                  {(lookups[ref] || []).map((o) => (
                    <option key={o.id} value={o.id}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </label>
            )}
          </div>
          {type === "journal" ? (
            <>
              <label>
                Debe
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={l.debit ?? 0}
                  onChange={(e) => set(i, "debit", e.target.value)}
                />
              </label>
              <label>
                Haber
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={l.credit ?? 0}
                  onChange={(e) => set(i, "credit", e.target.value)}
                />
              </label>
            </>
          ) : (
            <label>
              Cantidad
              <input
                aria-label={`Cantidad línea ${i + 1}`}
                type="number"
                min={
                  ["order", "quote"].includes(type)
                    ? 1
                    : type === "receive"
                      ? 0
                      : 0.0001
                }
                step={["order", "quote"].includes(type) ? 1 : 0.0001}
                required
                value={l.quantity ?? 1}
                onChange={(e) => set(i, "quantity", e.target.value)}
              />
            </label>
          )}
          {["purchase", "receive"].includes(type) && (
            <label>
              Costo sin IGV
              <input
                aria-label={`Costo línea ${i + 1}`}
                type="number"
                min="0"
                step="0.0001"
                required
                value={l.unit_cost ?? 0}
                onChange={(e) => set(i, "unit_cost", e.target.value)}
              />
            </label>
          )}
          {["recipe", "order"].includes(type) && (
            <label className="line-note">
              {type === "recipe" ? "Proceso / unidad base" : "Indicaciones"}
              <input
                value={l[type === "recipe" ? "process" : "notes"] || ""}
                placeholder={
                  type === "recipe"
                    ? "Usar la unidad base del insumo"
                    : "Sin cebolla, bien cocido…"
                }
                onChange={(e) =>
                  set(
                    i,
                    type === "recipe" ? "process" : "notes",
                    e.target.value,
                  )
                }
              />
            </label>
          )}
          {type !== "receive" && (
            <button
              className="icon danger"
              type="button"
              aria-label={`Quitar línea ${i + 1}`}
              onClick={() => setLines(lines.filter((_, n) => n !== i))}
            >
              <Trash2 size={16} />
            </button>
          )}
        </div>
      ))}
      {type === "journal" && (
        <div className="subtotal">
          Debe: {money(lines.reduce((a, l) => a + Number(l.debit || 0), 0))} ·
          Haber: {money(lines.reduce((a, l) => a + Number(l.credit || 0), 0))}
        </div>
      )}
    </div>
  );
}
export function Form({
  fields,
  initial = {},
  onSubmit,
  onClose,
  lookups,
  module,
  lineType,
  initialLines = [],
  submitLabel = "Guardar registro",
  children,
}) {
  const [data, setData] = useState(() =>
    Object.fromEntries(
      fields.map((f) => [
        f.key,
        initial[f.key] ??
        f.default ??
        (f.type === "date"
          ? today()
          : f.key === "period" || f.key === "first_period"
            ? today().slice(0, 7)
            : f.type === "select"
              ? f.options[0]
              : ""),
      ]),
    ),
  ),
    [lines, setLines] = useState(initialLines),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      if (lineType && lines.length === 0)
        throw new Error("Agrega por lo menos una línea.");
      const body = { ...data };
      if (lineType)
        body.lines =
          lineType === "receive"
            ? lines
              .filter((l) => Number(l.quantity) > 0)
              .map((l) => ({
                line_id: l.line_id,
                quantity: l.quantity,
                unit_cost: l.unit_cost,
              }))
            : lines;
      await onSubmit(body);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit}>
      <div className="modal-body">
        {children}
        {error && (
          <div className="error" role="alert">
            {error}
          </div>
        )}
        <div className="form-grid">
          {fields.map((f) => (
            <Field
              key={f.key}
              field={f}
              value={data[f.key]}
              onChange={(v) => setData({ ...data, [f.key]: v })}
              lookups={lookups}
              module={module}
            />
          ))}
        </div>
        {lineType && (
          <LinesEditor
            type={lineType}
            lines={lines}
            setLines={setLines}
            lookups={lookups}
          />
        )}
      </div>
      <footer>
        <button
          type="button"
          className="button secondary"
          onClick={onClose}
          disabled={busy}
        >
          Cancelar
        </button>
        <button className="button" disabled={busy}>
          {busy ? (
            <LoaderCircle size={17} className="spin" />
          ) : (
            <CheckCircle2 size={17} />
          )}{" "}
          {busy ? "Guardando…" : submitLabel}
        </button>
      </footer>
    </form>
  );
}
export const field = (key, label, type = "text", extra = {}) => ({
  key,
  label,
  type,
  ...extra,
});
