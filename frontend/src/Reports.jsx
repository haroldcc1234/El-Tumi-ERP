import React, { useEffect, useState } from "react";
import { Download, ArrowUpRight } from "lucide-react";
import { api, download, money, today, title } from "./api";
import { useERP } from "./context";
import { Loading, Empty } from "./ui";

const tabs = [
  ["trial", "Comprobación"],
  ["balance", "Situación financiera"],
  ["income", "Resultados"],
  ["ledger", "Libro mayor"],
  ["receivables", "Antigüedad por cobrar"],
  ["payables", "Antigüedad por pagar"],
];

export default function Reports() {
  const { tick, lookups, toast, can, navigate } = useERP();
  const [view, setView] = useState("trial");
  const [from, setFrom] = useState(today().slice(0, 7) + "-01");
  const [to, setTo] = useState(today());
  const [accountId, setAccountId] = useState("");
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const aging = ["receivables", "payables"].includes(view);
  const params = new URLSearchParams({
    from,
    to,
    accountId: accountId || 0,
  }).toString();

  useEffect(() => {
    let active = true;
    setData(null);
    setError("");
    if (view === "ledger" && !accountId)
      return () => {
        active = false;
      };
    api(
      aging ? `/reports/aging/${view}` : `/financial-reports/${view}?${params}`,
    )
      .then((result) => {
        if (!active) return;
        if (aging) {
          setData({
            title:
              view === "payables"
                ? "Antigüedad por pagar"
                : "Antigüedad por cobrar",
            rows: result,
            summary: {
              "Saldo pendiente actual": result.reduce(
                (s, r) => s + Number(r.balance),
                0,
              ),
            },
            columns: [
              { key: "code", label: "Documento" },
              { key: "party", label: "Cliente / proveedor" },
              { key: "due_date", label: "Vencimiento" },
              { key: "balance", label: "Saldo", numeric: true },
              { key: "overdue_days", label: "Días vencidos" },
              { key: "bucket", label: "Antigüedad" },
            ],
          });
        } else setData(result);
      })
      .catch((e) => active && setError(e.message));
    return () => {
      active = false;
    };
  }, [view, params, tick, aging, accountId]);

  async function exportReport(format) {
    try {
      await download(
        `/financial-reports/${view}/export?${params}&format=${format}`,
        `el-tumi-${view}.${format}`,
      );
    } catch (e) {
      toast(e.message, "error");
    }
  }

  return (
    <div className="page enter">
      <div className="page-heading">
        <div>
          <span className="eyebrow">CONTABILIDAD / REPORTES</span>
          <h1>Los números, en orden.</h1>
          <p>Consulta tus saldos y sigue el movimiento de cada cuenta.</p>
        </div>
        {!aging && (
          <div className="period">
            {view !== "balance" && (
              <>
                <input
                  type="date"
                  aria-label="Fecha inicial"
                  value={from}
                  onChange={(e) => setFrom(e.target.value)}
                />
                <span>—</span>
              </>
            )}
            <input
              type="date"
              aria-label="Fecha final"
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          </div>
        )}
      </div>
      <div className="tabs">
        {tabs.map(([key, label]) => (
          <button
            className={key === view ? "active" : ""}
            key={key}
            onClick={() => setView(key)}
          >
            {label}
          </button>
        ))}
      </div>
      {view === "ledger" && (
        <label className="report-account">
          Cuenta contable
          <select
            value={accountId}
            onChange={(e) => setAccountId(e.target.value)}
          >
            <option value="">Selecciona una cuenta…</option>
            {(lookups.accounts || []).map((a) => (
              <option value={a.id} key={a.id}>
                {a.label}
              </option>
            ))}
          </select>
        </label>
      )}
      {view === "balance" && (
        <p className="notice">
          Saldos acumulados a la fecha de corte. El patrimonio incluye el
          resultado acumulado de las cuentas de ingresos, costos y gastos.
        </p>
      )}
      {aging && (
        <p className="footnote">
          Antigüedad calculada a la fecha de hoy sobre el saldo pendiente de
          cada documento.
        </p>
      )}
      {error ? (
        <div className="error" role="alert">
          {error}
        </div>
      ) : view === "ledger" && !accountId ? (
        <Empty
          text="Elige una cuenta contable"
          detail="Verás su saldo inicial, sus asientos y el saldo después de cada movimiento."
        />
      ) : !data ? (
        <Loading />
      ) : (
        <>
          <div className="metrics">
            {Object.entries(data.summary).map(([label, value]) => (
              <section className="metric" key={label}>
                <span>{label}</span>
                <strong>{money(value)}</strong>
              </section>
            ))}
          </div>
          <section className="panel">
            <div className="panel-heading">
              <h2>{data.title}</h2>
              {!aging && can("accounting", "export") && (
                <div className="export-group">
                  {["xlsx", "pdf", "csv"].map((format) => (
                    <button
                      key={format}
                      className="icon-text"
                      onClick={() => exportReport(format)}
                    >
                      <Download size={14} />
                      {format.toUpperCase()}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    {data.columns.map((c) => (
                      <th key={c.key}>{c.label}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {data.rows.map((row, n) => (
                    <tr key={n}>
                      {data.columns.map((c) => (
                        <td key={c.key}>
                          {c.numeric
                            ? money(row[c.key])
                            : c.key === "type"
                              ? title(row[c.key])
                              : String(row[c.key] ?? "—")}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {!data.rows.length && (
              <Empty
                text="No hay movimientos para esta selección"
                detail="Ajusta las fechas o registra una operación."
              />
            )}
            <div className="table-footer">
              <span>
                {data.rows.length} registros · Reporte interno en soles
              </span>
              <button
                className="link"
                onClick={() => navigate(aging ? view : "journals")}
              >
                Ver movimientos <ArrowUpRight size={14} />
              </button>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
