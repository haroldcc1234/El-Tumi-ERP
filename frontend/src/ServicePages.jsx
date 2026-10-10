import React, { useEffect, useState } from "react";
import {
  Users,
  Plus,
  ArrowRight,
  ChefHat,
  Clock,
  Flame,
  Check,
  RotateCcw,
  UtensilsCrossed,
  ReceiptText,
} from "lucide-react";
import { api, money, title } from "./api";
import { useERP } from "./context";
import { Loading, Empty, Badge, Modal, Form, field } from "./ui";
import { useOperations } from "./DataPage";
function useBoard() {
  const { tick } = useERP();
  const [board, setBoard] = useState(null),
    [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    const load = () =>
      api("/board")
        .then((b) => {
          if (active) {
            setError("");
            setBoard(b);
          }
        })
        .catch((e) => active && setError(e.message));
    load();
    const timer = setInterval(load, 12000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [tick]);
  return { board, error };
}
export function Tables() {
  const { catalog, setDialog, lookups, refresh, toast, can, navigate } =
    useERP();
  const { board, error } = useBoard();
  const ops = useOperations();
  const [area, setArea] = useState("all"),
    [selected, setSelected] = useState(null);
  if (error) return <div className="error">{error}</div>;
  if (!board) return <Loading />;
  const current = board.orders.find((o) => o.id === selected);
  const orderEntity = catalog.find((e) => e.key === "orders");
  const tablesEntity = catalog.find((e) => e.key === "dining_tables");
  function newOrder(t) {
    ops.create(orderEntity, {
      table_id: t?.id || "",
      warehouse_id: lookups.warehouses?.[0]?.id || "",
    });
  }
  function add() {
    setDialog(
      <Modal
        title={"Agregar platos · pedido #" + current.id}
        wide
        onClose={() => setDialog(null)}
      >
        <Form
          fields={[]}
          lineType="order"
          module="sales"
          lookups={lookups}
          onClose={() => setDialog(null)}
          onSubmit={async (b) => {
            await api(`/orders/${current.id}/lines`, {
              method: "POST",
              body: b,
            });
            setDialog(null);
            toast("Platos agregados");
            refresh();
          }}
        />
      </Modal>,
    );
  }
  return (
    <div className="page enter">
      <div className="page-heading">
        <div>
          <span className="eyebrow">VENTAS / ATENCIÓN EN SALÓN</span>
          <h1>Cada mesa, una experiencia.</h1>
          <p>Desde el primer pedido hasta la cuenta.</p>
        </div>
        {can("sales", "write") && (
          <button className="button" onClick={() => newOrder()}>
            <Plus size={17} />
            Pedido sin mesa
          </button>
        )}
      </div>
      <div className="service-stats">
        {["LIBRE", "OCUPADA", "PAGAR", "LIMPIAR"].map((s) => (
          <span key={s}>
            <i className={"dot " + s.toLowerCase()} />
            {title(s)}{" "}
            <b>{board.tables.filter((t) => t.status === s).length}</b>
          </span>
        ))}
      </div>
      <div className="pos-layout">
        <section>
          <div className="tabs">
            <button
              className={area === "all" ? "active" : ""}
              onClick={() => setArea("all")}
            >
              Todos los espacios
            </button>
            {board.areas.map((a) => (
              <button
                key={a.id}
                className={area === a.id ? "active" : ""}
                onClick={() => setArea(a.id)}
              >
                {a.name}
              </button>
            ))}
          </div>
          <div className="tables-grid">
            {board.tables
              .filter((t) => area === "all" || t.area_id === area)
              .map((t) => (
                <button
                  className={`table-card ${t.status.toLowerCase()} ${selected === t.order_id ? "selected" : ""}`}
                  key={t.id}
                  onClick={() => {
                    if (t.status === "LIBRE" && can("sales", "write"))
                      newOrder(t);
                    else if (t.status === "LIMPIAR" && can("sales", "write"))
                      ops.action(tablesEntity, t, "clean");
                    else setSelected(t.order_id);
                  }}
                >
                  <div>
                    <span>{t.area_name}</span>
                    <Badge value={t.status} />
                  </div>
                  <div className="table-drawing">
                    <i />
                    <strong>{t.name}</strong>
                    <i />
                  </div>
                  <footer>
                    <span>
                      <Users size={14} />
                      {Number(t.capacity)} personas
                    </span>
                    <b>
                      {t.total
                        ? money(t.total)
                        : t.status === "LIBRE"
                          ? "Tomar pedido"
                          : t.status === "LIMPIAR"
                            ? "Limpiar mesa"
                            : "Ver pedido"}
                    </b>
                  </footer>
                </button>
              ))}
          </div>
          {board.orders.filter((o) => !o.table_id).length > 0 && (
            <section className="panel event-orders">
              <div className="panel-heading">
                <h2>Pedidos de eventos y para llevar</h2>
              </div>
              {board.orders
                .filter((o) => !o.table_id)
                .map((o) => (
                  <button
                    className="list-row"
                    key={o.id}
                    onClick={() => setSelected(o.id)}
                  >
                    Pedido #{o.id} · {o.customer || "Para llevar"}
                    <strong>{money(o.total)}</strong>
                  </button>
                ))}
            </section>
          )}
        </section>
        <aside className="panel order-panel">
          {current ? (
            <>
              <div className="panel-heading">
                <div>
                  <span className="eyebrow">PEDIDO #{current.id}</span>
                  <h2>
                    {current.table_name
                      ? "Mesa " + current.table_name
                      : current.customer || "Pedido para llevar"}
                  </h2>
                </div>
                <Badge value={current.status} />
              </div>
              <div className="order-items">
                {board.lines
                  .filter((l) => l.order_id === current.id)
                  .map((l) => (
                    <div className="order-item" key={l.id}>
                      <span className="quantity">{Number(l.quantity)}</span>
                      <div>
                        <strong>{l.dish_name}</strong>
                        {l.notes && <small>{l.notes}</small>}
                        <Badge value={l.status} />
                      </div>
                      <b>
                        {money(
                          Number(l.quantity) * Number(l.unit_price) -
                            Number(l.discount),
                        )}
                      </b>
                    </div>
                  ))}
              </div>
              <div className="order-total">
                <span>Total del pedido</span>
                <strong>{money(current.total)}</strong>
              </div>
              <div className="order-buttons">
                {current.status === "ABIERTO" && can("sales", "write") && (
                  <>
                    <button className="button secondary" onClick={add}>
                      <Plus size={16} />
                      Agregar platos
                    </button>
                    <button
                      className="button"
                      onClick={() => ops.action(orderEntity, current, "bill")}
                    >
                      <ReceiptText size={17} />
                      Enviar a caja
                    </button>
                  </>
                )}
                {current.status === "PAGAR" && can("sales", "approve") && (
                  <button
                    className="button"
                    onClick={() => ops.action(orderEntity, current, "checkout")}
                  >
                    Cobrar pedido <ArrowRight size={17} />
                  </button>
                )}
                <button
                  className="link"
                  onClick={() => ops.detail(orderEntity, current)}
                >
                  Ver detalle y acciones
                </button>
              </div>
            </>
          ) : (
            <div className="order-placeholder">
              <UtensilsCrossed size={36} />
              <h3>Selecciona una mesa</h3>
              <p>
                Consulta sus platos, añade indicaciones y acompaña el pedido.
              </p>
              <button className="link" onClick={() => navigate("cocina")}>
                Ver comandas <ArrowRight size={14} />
              </button>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}
export function Commands({ room = false }) {
  const { setDialog, refresh, toast, user } = useERP();
  const { board, error } = useBoard();
  const [busy, setBusy] = useState(0);
  if (error) return <div className="error">{error}</div>;
  if (!board) return <Loading />;
  const lines = board.lines.filter((l) =>
    room
      ? ["TERMINADO", "ENTREGADO"].includes(l.status)
      : ["INGRESADO", "PROCESADO"].includes(l.status),
  );
  const states = room ? ["TERMINADO", "ENTREGADO"] : ["INGRESADO", "PROCESADO"];
  const allowed =
    user.role === "ADMIN" ||
    (room ? ["SALON", "CAJA"].includes(user.role) : user.role === "COCINA");
  async function run(l, a, b = {}) {
    setBusy(l.id);
    try {
      await api(`/order-lines/${l.id}/${a}`, { method: "POST", body: b });
      setDialog(null);
      toast("Comanda actualizada");
      await refresh();
    } catch (e) {
      toast(e.message, "error");
    } finally {
      setBusy(0);
    }
  }
  function reason(l, a) {
    setDialog(
      <Modal
        title={
          a === "waste" ? "Registrar merma y reiniciar" : "Registrar devolución"
        }
        onClose={() => setDialog(null)}
      >
        <Form
          fields={[
            field("reason", "Motivo", "textarea", { required: true }),
            ...(a === "return"
              ? [
                  field("again", "¿Preparar nuevamente?", "select", {
                    options: ["SI", "NO"],
                  }),
                ]
              : []),
          ]}
          onClose={() => setDialog(null)}
          onSubmit={(b) =>
            run(l, a, { reason: b.reason, reprocess: b.again === "SI" })
          }
        >
          <p className="form-note">
            Esta acción registra una merma de los ingredientes de la preparación
            actual.{" "}
            {a === "waste"
              ? "La comanda vuelve a ingresado."
              : "Si no se vuelve a preparar, el platillo se retira de la cuenta."}
          </p>
        </Form>
      </Modal>,
    );
  }
  return (
    <div className="page enter">
      <div className="page-heading">
        <div>
          <span className="eyebrow">VENTAS / COMANDAS</span>
          <h1>
            {room ? "Del pase a la mesa." : "Aquí empieza el buen sabor."}
          </h1>
          <p>
            {room
              ? "Entrega los platos terminados y atiende las devoluciones."
              : "Cada preparación en su momento. Actualización cada 12 segundos."}
          </p>
        </div>
        <span className="live">
          <i />
          {lines.length} preparaciones
        </span>
      </div>
      <div className="kanban">
        {states.map((s) => (
          <section className="kanban-column" key={s}>
            <div className="kanban-header">
              <h2>
                {s === "INGRESADO"
                  ? "Por preparar"
                  : s === "PROCESADO"
                    ? "En preparación"
                    : s === "TERMINADO"
                      ? "Listo para entregar"
                      : "Entregado"}
              </h2>
              <span>{lines.filter((l) => l.status === s).length}</span>
            </div>
            {lines
              .filter((l) => l.status === s)
              .map((l) => (
                <article className="command" key={l.id}>
                  <header>
                    <strong>
                      {l.table_name
                        ? "Mesa " + l.table_name
                        : "Pedido #" + l.order_id}
                    </strong>
                    <small>#{l.order_id}</small>
                  </header>
                  <div className="command-body">
                    <b>{Number(l.quantity)}×</b>
                    <h3>{l.dish_name}</h3>
                  </div>
                  {l.notes && <p className="command-note">{l.notes}</p>}
                  <div className="command-time">
                    <Clock size={13} />
                    {String(l.created_at).slice(11, 16)}
                  </div>
                  {allowed && (
                    <footer>
                      {s === "INGRESADO" && (
                        <button
                          className="button"
                          disabled={busy === l.id}
                          onClick={() => run(l, "cook")}
                        >
                          <Flame size={15} />
                          Preparar
                        </button>
                      )}
                      {s === "PROCESADO" && (
                        <>
                          <button
                            className="button"
                            disabled={busy === l.id}
                            onClick={() => run(l, "ready")}
                          >
                            <Check size={16} />
                            Terminado
                          </button>
                          <button
                            className="link danger"
                            onClick={() => reason(l, "waste")}
                          >
                            Merma
                          </button>
                        </>
                      )}
                      {s === "TERMINADO" && (
                        <button
                          className="button"
                          disabled={busy === l.id}
                          onClick={() => run(l, "deliver")}
                        >
                          <Check size={16} />
                          Entregar
                        </button>
                      )}
                      {s === "ENTREGADO" && (
                        <button
                          className="button secondary"
                          disabled={busy === l.id}
                          onClick={() => reason(l, "return")}
                        >
                          <RotateCcw size={15} />
                          Devolución
                        </button>
                      )}
                    </footer>
                  )}
                </article>
              ))}
            {!lines.some((l) => l.status === s) && (
              <Empty
                text="Todo al día"
                detail="Las próximas comandas aparecerán aquí."
              />
            )}
          </section>
        ))}
      </div>
    </div>
  );
}
function PrivateImage({ id }) {
  const [src, setSrc] = useState("");
  useEffect(() => {
    let active = true,
      url;
    api("/files/" + id, { blob: true })
      .then((b) => {
        url = URL.createObjectURL(b);
        if (active) setSrc(url);
      })
      .catch(() => {});
    return () => {
      active = false;
      if (url) URL.revokeObjectURL(url);
    };
  }, [id]);
  return src ? <img src={src} alt="Presentación del platillo" /> : null;
}
export function Menu() {
  const { tick, catalog } = useERP();
  const [items, setItems] = useState(null),
    [category, setCategory] = useState("Todas"),
    [error, setError] = useState("");
  const ops = useOperations();
  useEffect(() => {
    api("/data/dishes?size=500&status=ACTIVO")
      .then((d) => setItems(d.items))
      .catch((e) => setError(e.message));
  }, [tick]);
  if (error) return <div className="error">{error}</div>;
  if (!items) return <Loading />;
  return (
    <div className="page enter">
      <div className="page-heading">
        <div>
          <span className="eyebrow">EL TUMI / NUESTRA CARTA</span>
          <h1>El sabor de nuestra tierra.</h1>
          <p>Platillos, recetas y precios siempre a mano.</p>
        </div>
      </div>
      <div className="tabs">
        {["Todas", ...new Set(items.map((x) => x.category))].map((c) => (
          <button
            className={category === c ? "active" : ""}
            key={c}
            onClick={() => setCategory(c)}
          >
            {c}
          </button>
        ))}
      </div>
      <div className="menu-grid">
        {items
          .filter((x) => category === "Todas" || x.category === category)
          .map((d) => (
            <button
              className="dish-card"
              key={d.id}
              onClick={() =>
                ops.detail(
                  catalog.find((e) => e.key === "dishes"),
                  d,
                )
              }
            >
              <div className="dish-image">
                {d.image_id ? (
                  <PrivateImage id={d.image_id} />
                ) : (
                  <>
                    <div className="plate-art">
                      <UtensilsCrossed size={39} />
                    </div>
                    <span>{d.category}</span>
                  </>
                )}
              </div>
              <div className="dish-info">
                <small>{d.category}</small>
                <h2>{d.name}</h2>
                <p>{d.description}</p>
                <footer>
                  <strong>{money(d.price)}</strong>
                  <span>
                    Ver receta <ArrowRight size={14} />
                  </span>
                </footer>
              </div>
            </button>
          ))}
      </div>
    </div>
  );
}
