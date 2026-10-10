import React, { useState, useEffect, useCallback } from "react";
import {
  LayoutDashboard,
  UtensilsCrossed,
  ChefHat,
  HandPlatter,
  BookOpen,
  Wallet,
  Boxes,
  Users,
  Settings2,
  Search,
  ChevronDown,
  ChevronRight,
  LogOut,
  Menu as MenuIcon,
  X,
  ArrowRight,
  CheckCircle2,
  AlertCircle,
  ShieldCheck,
  LockKeyhole,
  ArrowUpRight,
} from "lucide-react";
import { api, token, title } from "./api";
import { ERPContext } from "./context";
import { TumiMark, Loading } from "./ui";
import Dashboard from "./Dashboard";
import DataPage from "./DataPage";
import { Tables, Commands, Menu } from "./ServicePages";
import { Permissions, Profile } from "./AdminPages";
import Reports from "./Reports";
const groups = [
  { id: "sales", label: "Ventas y atención", icon: UtensilsCrossed },
  { id: "logistics", label: "Logística", icon: Boxes },
  { id: "accounting", label: "Contabilidad", icon: BookOpen },
  { id: "finance", label: "Finanzas", icon: Wallet },
  { id: "hr", label: "Recursos humanos", icon: Users },
  { id: "admin", label: "Administración", icon: Settings2 },
];
function Login({ onLogin }) {
  const [email, setEmail] = useState("admin@eltumi.local"),
    [password, setPassword] = useState(""),
    [otp, setOtp] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  return (
    <div className="login">
      <div className="login-story">
        <a className="brand" href="#">
          <TumiMark />
          <div>
            EL TUMI<small>RESTAURANTE & GESTIÓN</small>
          </div>
        </a>
        <div className="login-art">
          <div className="sun-ring ring-one" />
          <div className="sun-ring ring-two" />
          <div className="sun-ring ring-three" />
          <div className="large-mark">
            <TumiMark />
          </div>
          <div className="mountain m1" />
          <div className="mountain m2" />
          <div className="mountain m3" />
        </div>
        <div className="login-copy">
          <span className="eyebrow">RAÍCES QUE NOS UNEN</span>
          <h1>
            Tradición en la mesa.
            <br />
            Claridad en tu negocio.
          </h1>
          <p>Un solo lugar para cuidar cada parte de El Tumi.</p>
        </div>
        <footer>
          INGENIO, JUNÍN · PERÚ{" "}
          <span>DESDE LA COCINA HASTA LOS RESULTADOS</span>
        </footer>
      </div>
      <main className="login-main">
        <div className="login-form">
          <span className="eyebrow">BIENVENIDO A EL TUMI</span>
          <h2>Comienza tu jornada.</h2>
          <p>Ingresa a tu espacio de trabajo.</p>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setError("");
              try {
                const r = await api("/auth/login", {
                  method: "POST",
                  body: { email, password, otp },
                });
                sessionStorage.setItem("tumi-token", r.token);
                onLogin(r);
              } catch (e) {
                setError(e.message);
              } finally {
                setBusy(false);
              }
            }}
          >
            {error && (
              <div className="error" role="alert">
                {error}
              </div>
            )}
            <label>
              Correo electrónico
              <input
                type="email"
                value={email}
                required
                autoComplete="username"
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>
            <label>
              Contraseña
              <input
                type="password"
                value={password}
                required
                autoComplete="current-password"
                placeholder="Ingresa tu contraseña"
                onChange={(e) => setPassword(e.target.value)}
              />
            </label>
            <details>
              <summary>Uso un código de autenticación</summary>
              <input
                aria-label="Código 2FA"
                inputMode="numeric"
                maxLength="6"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                placeholder="000000"
                autoComplete="one-time-code"
              />
            </details>
            <button className="button login-submit" disabled={busy}>
              {busy ? "Ingresando…" : "Ingresar al sistema"}
              <ArrowRight size={18} />
            </button>
          </form>
          <p className="login-security">
            <ShieldCheck size={15} />
            Acceso personal · Operaciones registradas
          </p>
        </div>
        <footer>
          El Tumi ERP <span>Gestión que acompaña tu crecimiento.</span>
        </footer>
      </main>
    </div>
  );
}
export default function App() {
  const [session, setSession] = useState(null),
    [catalog, setCatalog] = useState([]),
    [lookups, setLookups] = useState({}),
    [loading, setLoading] = useState(!!token()),
    [bootError, setBootError] = useState(""),
    [route, setRoute] = useState(location.hash.slice(1) || "dashboard"),
    [expanded, setExpanded] = useState(""),
    [mobile, setMobile] = useState(false),
    [search, setSearch] = useState(""),
    [tick, setTick] = useState(0),
    [dialog, setDialog] = useState(null),
    [toasts, setToasts] = useState([]);
  const toast = useCallback((message, type = "success") => {
    const id = crypto.randomUUID();
    setToasts((t) => [...t.slice(-2), { id, message, type }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 5500);
  }, []);
  function navigate(r) {
    location.hash = r;
    setRoute(r);
    setMobile(false);
    setSearch("");
    const e = catalog.find((x) => x.key === r);
    if (e) setExpanded(e.module);
  }
  const logout = useCallback(async () => {
    try {
      if (token()) await api("/auth/logout", { method: "POST", body: {} });
    } catch {}
    sessionStorage.removeItem("tumi-token");
    setSession(null);
    setCatalog([]);
    setDialog(null);
  }, []);
  async function boot() {
    try {
      setBootError("");
      const [me, cat, look] = await Promise.all([
        api("/auth/me"),
        api("/catalog"),
        api("/lookups"),
      ]);
      setSession(me);
      setCatalog(cat);
      setLookups(look);
    } catch (e) {
      if (token()) setBootError(e.message);
      else setSession(null);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    if (token()) boot();
    const end = () => {
      setSession(null);
      setDialog(null);
      setLoading(false);
    };
    const hash = () => setRoute(location.hash.slice(1) || "dashboard");
    window.addEventListener("session-expired", end);
    window.addEventListener("hashchange", hash);
    return () => {
      window.removeEventListener("session-expired", end);
      window.removeEventListener("hashchange", hash);
    };
  }, []);
  async function refresh(me = false) {
    setTick((t) => t + 1);
    try {
      setLookups(await api("/lookups"));
      if (me) await boot();
    } catch (e) {
      toast(e.message, "error");
    }
  }
  const can = (module, action = "read") =>
    session?.user.role === "ADMIN" ||
    Boolean(
      session?.permissions.find((p) => p.module === module)?.["can_" + action],
    );
  if (loading)
    return (
      <div className="boot">
        <TumiMark />
        <Loading />
      </div>
    );
  if (bootError)
    return (
      <div className="boot">
        <p className="error">{bootError}</p>
        <button className="button" onClick={boot}>
          Reintentar conexión
        </button>
      </div>
    );
  if (!session)
    return (
      <Login
        onLogin={async () => {
          setLoading(true);
          await boot();
          navigate("dashboard");
        }}
      />
    );
  const entity = catalog.find((e) => e.key === route);
  let content;
  if (route === "profile") content = <Profile />;
  else if (route === "mesas" && can("sales")) content = <Tables />;
  else if (route === "cocina" && can("sales")) content = <Commands />;
  else if (route === "salon" && can("sales")) content = <Commands room />;
  else if (route === "carta" && can("sales")) content = <Menu />;
  else if (route === "reports" && can("accounting")) content = <Reports />;
  else if (route === "permissions" && can("admin")) content = <Permissions />;
  else if (entity) content = <DataPage key={entity.key} entity={entity} />;
  else content = <Dashboard />;
  const special = [
    { id: "dashboard", name: "Vista general", icon: LayoutDashboard },
    { id: "mesas", name: "Tablero de mesas", icon: UtensilsCrossed },
    { id: "cocina", name: "Comanda cocina", icon: ChefHat },
    { id: "salon", name: "Comanda salón", icon: HandPlatter },
    { id: "carta", name: "Nuestra carta", icon: BookOpen },
  ];
  const currentName =
    entity?.title ||
    special.find((s) => s.id === route)?.name ||
    {
      reports: "Reportes contables",
      permissions: "Roles y permisos",
      profile: "Mi cuenta",
    }[route] ||
    "Vista general";
  return (
    <ERPContext.Provider
      value={{
        user: session.user,
        catalog,
        lookups,
        can,
        toast,
        navigate,
        tick,
        refresh,
        setDialog,
        logout,
      }}
    >
      <div className="app-shell">
        {mobile && (
          <div className="sidebar-backdrop" onClick={() => setMobile(false)} />
        )}
        <aside className={"sidebar " + (mobile ? "open" : "")}>
          <button className="brand" onClick={() => navigate("dashboard")}>
            <TumiMark />
            <div>
              EL TUMI<small>GESTIÓN DEL RESTAURANTE</small>
            </div>
          </button>
          <div className="location-label">
            <span className="location-dot" />
            Ingenio, Junín <span>PERÚ</span>
          </div>
          <nav>
            <span className="nav-label">TU JORNADA</span>
            {special
              .filter((s) => s.id === "dashboard" || can("sales"))
              .map((s) => (
                <button
                  key={s.id}
                  className={"nav-item " + (route === s.id ? "active" : "")}
                  onClick={() => navigate(s.id)}
                >
                  <s.icon size={18} />
                  {s.name}
                  {route === s.id && <i />}
                </button>
              ))}
            <span className="nav-label modules-label">TU NEGOCIO</span>
            {groups
              .filter((g) => can(g.id))
              .map((g) => (
                <div className="nav-group" key={g.id}>
                  <button
                    className={
                      "nav-item " +
                      (entity?.module === g.id ? "group-active" : "")
                    }
                    onClick={() => setExpanded(expanded === g.id ? "" : g.id)}
                  >
                    <g.icon size={17} />
                    {g.label}
                    {expanded === g.id ? (
                      <ChevronDown size={14} />
                    ) : (
                      <ChevronRight size={14} />
                    )}
                  </button>
                  {expanded === g.id && (
                    <div className="subnav">
                      {catalog
                        .filter((e) => e.module === g.id)
                        .map((e) => (
                          <button
                            key={e.key}
                            className={route === e.key ? "active" : ""}
                            onClick={() => navigate(e.key)}
                          >
                            {e.title}
                          </button>
                        ))}
                      {g.id === "accounting" && (
                        <button
                          className={route === "reports" ? "active" : ""}
                          onClick={() => navigate("reports")}
                        >
                          Reportes y antigüedad
                        </button>
                      )}
                      {g.id === "admin" && (
                        <button
                          className={route === "permissions" ? "active" : ""}
                          onClick={() => navigate("permissions")}
                        >
                          Roles y permisos
                        </button>
                      )}
                    </div>
                  )}
                </div>
              ))}
          </nav>
          <div className="sidebar-bottom">
            <div className="support-note">
              <span className="tiny-dot" />
              Cada operación cuenta.
              <small>Construyendo un mejor servicio.</small>
            </div>
            <button
              className="user-profile"
              onClick={() => navigate("profile")}
            >
              <span className="avatar">{session.user.name.slice(0, 1)}</span>
              <div>
                <strong>{session.user.name.split(" ")[0]}</strong>
                <small>{title(session.user.role)}</small>
              </div>
              <Settings2 size={17} />
            </button>
            <button className="logout" onClick={logout}>
              <LogOut size={15} />
              Cerrar sesión
            </button>
          </div>
        </aside>
        <main className="main-shell">
          <header className="topbar">
            <div className="breadcrumb">
              <button
                className="icon mobile-only"
                onClick={() => setMobile(true)}
                aria-label="Abrir menú"
              >
                <MenuIcon size={21} />
              </button>
              <span>Espacio de trabajo</span>
              <ChevronRight size={13} />
              <strong>{currentName}</strong>
            </div>
            <div className="topbar-right">
              <div className="nav-search">
                <Search size={16} />
                <input
                  placeholder="Ir a un módulo…"
                  aria-label="Buscar módulo"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
                {search && (
                  <div className="search-results">
                    {catalog
                      .filter((e) =>
                        e.title.toLowerCase().includes(search.toLowerCase()),
                      )
                      .slice(0, 10)
                      .map((e) => (
                        <button key={e.key} onClick={() => navigate(e.key)}>
                          {e.title}
                          <ArrowUpRight size={14} />
                        </button>
                      ))}
                    {!catalog.some((e) =>
                      e.title.toLowerCase().includes(search.toLowerCase()),
                    ) && <p>Sin resultados.</p>}
                  </div>
                )}
              </div>
              <span className="top-separator" />
              <button
                className="top-avatar"
                title="Mi cuenta"
                onClick={() => navigate("profile")}
              >
                {session.user.name.slice(0, 1)}
              </button>
            </div>
          </header>
          {content}
          <footer className="app-footer">
            <span>
              EL TUMI <i />
              Restaurante & gestión
            </span>
            <span>Hecho para cuidar cada detalle.</span>
          </footer>
        </main>
      </div>
      {dialog}
      <div className="toast-stack" aria-live="polite">
        {toasts.map((t) => (
          <div className={"toast " + t.type} key={t.id}>
            {t.type === "error" ? (
              <AlertCircle size={19} />
            ) : (
              <CheckCircle2 size={19} />
            )}
            <span>{t.message}</span>
            <button
              onClick={() => setToasts((ts) => ts.filter((x) => x.id !== t.id))}
              aria-label="Cerrar notificación"
            >
              <X size={15} />
            </button>
          </div>
        ))}
      </div>
    </ERPContext.Provider>
  );
}
