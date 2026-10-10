import React, { useEffect, useState } from "react";
import {
  ArrowUpRight,
  Wallet,
  ReceiptText,
  TrendingUp,
  UtensilsCrossed,
  PackageCheck,
  ArrowRight,
  CalendarDays,
  Sparkles,
  AlertCircle,
  Users,
} from "lucide-react";
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import { api, money, today } from "./api";
import { useERP } from "./context";
import { Loading, Empty, Badge } from "./ui";
export default function Dashboard() {
  const { tick, navigate, can, toast } = useERP();
  const [data, setData] = useState(null),
    [from, setFrom] = useState(today().slice(0, 7) + "-01"),
    [to, setTo] = useState(today()),
    [error, setError] = useState(""),
    [insight, setInsight] = useState(""),
    [busy, setBusy] = useState(false);
  useEffect(() => {
    let active = true;
    api("/dashboard?" + new URLSearchParams({ from, to }))
      .then((r) => active && setData(r))
      .catch((e) => active && setError(e.message));
    return () => {
      active = false;
    };
  }, [tick, from, to]);
  if (error) return <div className="error">{error}</div>;
  if (!data) return <Loading />;
  const cards = [];
  if (data.sales !== undefined) {
    cards.push({
      label: "Ventas del período",
      value: money(data.sales),
      caption: `${data.tickets} comprobantes registrados`,
      icon: ReceiptText,
      color: "orange",
    });
    cards.push({
      label: "Ticket promedio",
      value: money(data.tickets ? Number(data.sales) / data.tickets : 0),
      caption: "Por comprobante emitido",
      icon: TrendingUp,
      color: "green",
    });
  }
  if (data.profit !== undefined)
    cards.push({
      label: "Resultado contable",
      value: money(data.profit),
      caption: "Ingresos menos costos y gastos",
      icon: Wallet,
      color: "blue",
    });
  if (data.openOrders !== undefined)
    cards.push({
      label: "Pedidos en atención",
      value: data.openOrders,
      caption: "En salón o pendientes de pago",
      icon: UtensilsCrossed,
      color: "purple",
    });
  if (!cards.length && data.staff !== undefined)
    cards.push({
      label: "Equipo activo",
      value: data.staff,
      caption: "Expedientes de colaboradores",
      icon: Users,
      color: "green",
    });
  if (
    data.payables !== undefined &&
    !cards.some((c) => c.label === "Resultado contable")
  )
    cards.push({
      label: "Por pagar",
      value: money(data.payables),
      caption: "Saldo de obligaciones",
      icon: Wallet,
      color: "orange",
    });
  const daily = data.daily?.length
    ? data.daily
    : [
        { date: from, total: 0 },
        { date: to, total: 0 },
      ];
  return (
    <div className="page enter">
      <div className="page-heading">
        <div>
          <span className="eyebrow">EL TUMI / VISIÓN GENERAL</span>
          <h1>Tu restaurante, de un vistazo.</h1>
          <p>Cada detalle cuenta. Aquí comienza tu jornada.</p>
        </div>
        <div className="period">
          <CalendarDays size={17} />
          <input
            aria-label="Inicio del período"
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
          <span>—</span>
          <input
            aria-label="Fin del período"
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        </div>
      </div>
      <div className="metrics">
        {cards.map((c) => (
          <section className="metric" key={c.label}>
            <div>
              <span>{c.label}</span>
              <span className={"metric-icon " + c.color}>
                <c.icon size={19} />
              </span>
            </div>
            <strong>{c.value}</strong>
            <small>{c.caption}</small>
          </section>
        ))}
      </div>
      <div className="dashboard-grid">
        <div className="main-column">
          {data.daily && (
            <section className="panel chart-panel">
              <div className="panel-heading">
                <div>
                  <h2>El ritmo de tus ventas</h2>
                  <p>Evolución diaria · soles</p>
                </div>
                <span className="chart-key">
                  <i />
                  Ventas
                </span>
              </div>
              <div className="chart">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart
                    data={daily}
                    margin={{ top: 18, right: 14, left: 0, bottom: 0 }}
                  >
                    <defs>
                      <linearGradient
                        id="salesFill"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="0%"
                          stopColor="#d86b3c"
                          stopOpacity={0.22}
                        />
                        <stop
                          offset="100%"
                          stopColor="#d86b3c"
                          stopOpacity={0}
                        />
                      </linearGradient>
                    </defs>
                    <CartesianGrid
                      vertical={false}
                      stroke="#eceee8"
                      strokeDasharray="3 5"
                    />
                    <XAxis
                      dataKey="date"
                      tickFormatter={(x) => x.slice(5).replace("-", "/")}
                      tickLine={false}
                      axisLine={false}
                      fontSize={11}
                      dy={10}
                    />
                    <YAxis
                      tickLine={false}
                      axisLine={false}
                      fontSize={11}
                      width={53}
                      tickFormatter={(x) => "S/ " + x}
                    />
                    <Tooltip
                      formatter={(v) => [money(v), "Ventas"]}
                      contentStyle={{
                        borderRadius: 12,
                        border: "1px solid #eee",
                      }}
                    />
                    <Area
                      type="monotone"
                      dataKey="total"
                      stroke="#cf6336"
                      strokeWidth={3}
                      fill="url(#salesFill)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
              {!data.daily.length && (
                <p className="chart-empty">
                  Las ventas aparecerán después del primer cobro.
                </p>
              )}
            </section>
          )}
          {data.cash && (
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <h2>Disponibilidad y compromisos</h2>
                  <p>Saldos actuales del negocio</p>
                </div>
                <button className="link" onClick={() => navigate("treasury")}>
                  Tesorería <ArrowUpRight size={15} />
                </button>
              </div>
              <div className="cash-grid">
                {data.cash.map((c) => (
                  <div key={c.id}>
                    <span>{c.name}</span>
                    <strong>{money(c.balance)}</strong>
                    <small>{c.kind.toLowerCase()}</small>
                  </div>
                ))}
              </div>
              <div className="flow-strip">
                <div>
                  <span>Por cobrar</span>
                  <b>{money(data.receivables)}</b>
                </div>
                <div>
                  <span>Por pagar</span>
                  <b>{money(data.payables)}</b>
                </div>
                <div>
                  <span>Saldo proyectado simple</span>
                  <b
                    className={
                      data.cash.reduce((a, c) => a + Number(c.balance), 0) +
                        Number(data.receivables) -
                        Number(data.payables) <
                      0
                        ? "negative"
                        : ""
                    }
                  >
                    {money(
                      data.cash.reduce((a, c) => a + Number(c.balance), 0) +
                        Number(data.receivables) -
                        Number(data.payables),
                    )}
                  </b>
                </div>
              </div>
              <p className="footnote">
                Proyección simple = saldos actuales + cuentas pendientes por
                cobrar − cuentas pendientes por pagar.
              </p>
            </section>
          )}
          {data.budgets && (
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <h2>Presupuesto bajo control</h2>
                  <p>Ejecución de las partidas asignadas</p>
                </div>
                <button className="link" onClick={() => navigate("budgets")}>
                  Ver partidas <ArrowRight size={15} />
                </button>
              </div>
              {data.budgets.length ? (
                data.budgets.slice(0, 5).map((b) => (
                  <div className="budget-row" key={b.id}>
                    <div>
                      <strong>{b.area}</strong>
                      <span>{b.category}</span>
                    </div>
                    <div className="budget-track">
                      <i
                        style={{
                          width:
                            Math.min(
                              100,
                              (Number(b.executed) / Number(b.amount || 1)) *
                                100,
                            ) + "%",
                        }}
                      />
                    </div>
                    <div>
                      <b>{money(b.executed)}</b>
                      <small>de {money(b.amount)}</small>
                    </div>
                  </div>
                ))
              ) : (
                <Empty />
              )}
            </section>
          )}
          {data.attendance && (
            <section className="panel">
              <div className="panel-heading">
                <h2>Asistencia del equipo</h2>
                <button className="link" onClick={() => navigate("attendance")}>
                  Ver registros <ArrowRight size={15} />
                </button>
              </div>
              {data.attendance.map((a, i) => (
                <div className="list-row" key={i}>
                  <strong>
                    {a.name} {a.last_name}
                  </strong>
                  <span>
                    {a.records} registros · {Number(a.late_minutes || 0)} min de
                    tardanza · {Number(a.extra_hours || 0)} h extra
                  </span>
                </div>
              ))}
            </section>
          )}
        </div>
        <aside className="right-column">
          {can("sales", "read") && (
            <section className="service-card">
              <div className="service-symbol">
                <UtensilsCrossed size={32} />
              </div>
              <span className="eyebrow">EL SERVICIO CONTINÚA</span>
              <h2>
                Todo listo para
                <br />
                la siguiente mesa.
              </h2>
              <p>Gestiona los pedidos y acompaña cada plato hasta el salón.</p>
              <button onClick={() => navigate("mesas")}>
                Abrir tablero de mesas <ArrowRight size={17} />
              </button>
            </section>
          )}
          {data.stockAlerts && (
            <section className="panel">
              <div className="panel-heading">
                <h2>Atención al almacén</h2>
                <span className="number-badge">{data.stockAlerts.length}</span>
              </div>
              {data.stockAlerts.length ? (
                data.stockAlerts.slice(0, 5).map((a) => (
                  <div className="alert-row" key={a.id}>
                    <div className="alert-icon">
                      <PackageCheck size={17} />
                    </div>
                    <div>
                      <strong>{a.name}</strong>
                      <small>{a.warehouse}</small>
                    </div>
                    <b
                      className={
                        Number(a.quantity) <= Number(a.minimum) + 5
                          ? "negative"
                          : ""
                      }
                    >
                      {Number(a.quantity)} <small>{a.unit.toLowerCase()}</small>
                    </b>
                  </div>
                ))
              ) : (
                <p className="muted padded">
                  No hay alertas de reabastecimiento.
                </p>
              )}
              <button
                className="panel-link"
                onClick={() => navigate("stock_rules")}
              >
                Revisar reabastecimiento <ArrowRight size={15} />
              </button>
            </section>
          )}
          {data.topDishes && (
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <h2>Favoritos del período</h2>
                  <p>Ordenados por ingreso de ventas</p>
                </div>
              </div>
              {data.topDishes.length ? (
                data.topDishes.map((d, i) => (
                  <div className="ranking-row" key={d.name}>
                    <span>0{i + 1}</span>
                    <div>
                      <strong>{d.name}</strong>
                      <small>{Number(d.quantity)} platos vendidos</small>
                    </div>
                    <b>{money(d.revenue)}</b>
                  </div>
                ))
              ) : (
                <Empty
                  text="La carta espera su primer favorito"
                  detail="Se ordenarán después de registrar ventas."
                />
              )}
            </section>
          )}
          {data.due?.length > 0 && (
            <section className="panel">
              <div className="panel-heading">
                <h2>Pagos próximos</h2>
                <AlertCircle size={18} />
              </div>
              {data.due.slice(0, 4).map((d) => (
                <button
                  className="due-row"
                  key={d.id}
                  onClick={() => navigate("payables")}
                >
                  <div>
                    <strong>{d.party}</strong>
                    <small>Vence {d.due_date}</small>
                  </div>
                  <b>
                    {money(Number(d.total) - Number(d.paid) - Number(d.credit))}
                  </b>
                </button>
              ))}
            </section>
          )}
          {can("finance", "read") && (
            <section className="insight-card">
              <Sparkles size={21} />
              <h3>Una mirada a tus números</h3>
              <p>
                {insight ||
                  "Revisa un diagnóstico a partir de tus saldos y obligaciones actuales."}
              </p>
              <button
                className="link"
                disabled={busy}
                onClick={async () => {
                  setBusy(true);
                  try {
                    const r = await api("/insights", {
                      method: "POST",
                      body: {
                        question:
                          "Resume el estado de caja y los pagos pendientes.",
                      },
                    });
                    setInsight(r.answer);
                  } catch (e) {
                    toast(e.message, "error");
                  } finally {
                    setBusy(false);
                  }
                }}
              >
                {busy ? "Analizando…" : "Consultar análisis"}{" "}
                <ArrowUpRight size={14} />
              </button>
            </section>
          )}
        </aside>
      </div>
    </div>
  );
}
