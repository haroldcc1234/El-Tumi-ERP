import React, { useState } from "react";
import { Download, Upload } from "lucide-react";
import { api } from "./api";
import { Modal } from "./ui";
import { useERP } from "./context";

export default function CsvImport({ entity, onClose }) {
  const { refresh, toast } = useERP();
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const fields = entity.fields.filter((f) => !f.readonly);

  function template() {
    const text = "\ufeff" + fields.map((f) => f.key).join(",") + "\r\n";
    const url = URL.createObjectURL(
      new Blob([text], { type: "text/csv;charset=utf-8" }),
    );
    const link = document.createElement("a");
    link.href = url;
    link.download = `plantilla_${entity.key}.csv`;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    if (!file) return setError("Selecciona un archivo CSV.");
    setBusy(true);
    try {
      const body = new FormData();
      body.append("file", file);
      const result = await api(`/imports/${entity.key}`, {
        method: "POST",
        body,
      });
      onClose();
      toast(`${result.created} registros importados correctamente.`);
      await refresh();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title={`Importar CSV · ${entity.title}`} onClose={onClose} wide>
      <form onSubmit={submit}>
        <div className="modal-body">
          <p>
            Descarga la plantilla y agrega hasta 500 filas. Guárdala como CSV
            UTF-8 separado por comas. Las columnas obligatorias deben
            completarse en todas las filas.
          </p>
          <button type="button" className="button secondary" onClick={template}>
            <Download size={16} />
            Descargar plantilla
          </button>
          <p className="form-note">
            Si una fila contiene un error, se cancela toda la importación para
            que puedas corregirla y volver a cargar el archivo. Los códigos
            existentes no se reemplazan.
          </p>
          {error && (
            <div className="error" role="alert">
              {error}
            </div>
          )}
          <label>
            Archivo CSV (máximo 2 MB)
            <input
              type="file"
              accept=".csv,text/csv"
              required
              disabled={busy}
              onChange={(e) => setFile(e.target.files?.[0] || null)}
            />
          </label>
          <details style={{ marginTop: 20 }}>
            <summary>Campos y valores admitidos</summary>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Columna</th>
                    <th>Descripción</th>
                    <th>Valores</th>
                  </tr>
                </thead>
                <tbody>
                  {fields.map((f) => (
                    <tr key={f.key}>
                      <td>
                        <code>{f.key}</code>
                        {f.required ? " *" : ""}
                      </td>
                      <td>{f.label}</td>
                      <td>
                        {f.options?.join(", ") ||
                          (f.ref
                            ? "ID del registro relacionado"
                            : f.type === "file"
                              ? "Opcional; adjunta desde el detalle después de importar"
                              : f.type === "date"
                                ? "AAAA-MM-DD"
                                : "Texto o número, según el campo")}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </details>
        </div>
        <footer>
          <button
            type="button"
            className="button secondary"
            disabled={busy}
            onClick={onClose}
          >
            Cancelar
          </button>
          <button className="button" disabled={busy}>
            <Upload size={16} />
            {busy ? "Importando…" : "Importar registros"}
          </button>
        </footer>
      </form>
    </Modal>
  );
}
