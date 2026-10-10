import React, { useEffect, useState } from "react";
import {
  Save,
  ShieldCheck,
  KeyRound,
  LockKeyhole,
  FileSpreadsheet,
  BookOpen,
} from "lucide-react";
import { api, money, today, title } from "./api";
import { useERP } from "./context";
import { Loading, Modal, Form, field } from "./ui";
export function Permissions() {
  const { tick, toast, can } = useERP();
  const [rows, setRows] = useState(null),
    [role, setRole] = useState("CONTADOR"),
    [error, setError] = useState("");
  useEffect(() => {
    api("/permissions")
      .then(setRows)
      .catch((e) => setError(e.message));
  }, [tick]);
  async function save(r) {
    try {
      await api("/permissions/" + r.id, {
        method: "PUT",
        body: Object.fromEntries(
          ["can_read", "can_write", "can_approve", "can_export"].map((k) => [
            k,
            Boolean(r[k]),
          ]),
        ),
      });
      toast("Permisos actualizados");
    } catch (e) {
      toast(e.message, "error");
    }
  }
  if (error) return <div className="error">{error}</div>;
  if (!rows) return <Loading />;
  return (
    <div className="page enter">
      <div className="page-heading">
        <div>
          <span className="eyebrow">ADMINISTRACIÓN / ACCESO</span>
          <h1>El acceso adecuado para cada rol.</h1>
          <p>Los permisos se validan en el servidor en cada operación.</p>
        </div>
      </div>
      <div className="tabs">
        {[...new Set(rows.map((r) => r.role))].map((r) => (
          <button
            className={r === role ? "active" : ""}
            key={r}
            onClick={() => setRole(r)}
          >
            {title(r)}
          </button>
        ))}
      </div>
      <section className="panel">
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Módulo</th>
                <th>Ver</th>
                <th>Registrar / editar</th>
                <th>Aprobar / anular</th>
                <th>Exportar</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {rows
                .filter((r) => r.role === role)
                .map((r) => (
                  <tr key={r.id}>
                    <td>
                      {
                        {
                          accounting: "Contabilidad",
                          finance: "Finanzas",
                          logistics: "Logística",
                          hr: "Recursos humanos",
                          sales: "Ventas",
                          admin: "Administración",
                        }[r.module]
                      }
                    </td>
                    {["can_read", "can_write", "can_approve", "can_export"].map(
                      (k) => (
                        <td key={k}>
                          <input
                            type="checkbox"
                            aria-label={`${r.module} ${k}`}
                            checked={Boolean(r[k])}
                            disabled={!can("admin", "write")}
                            onChange={(e) =>
                              setRows(
                                rows.map((x) =>
                                  x.id === r.id
                                    ? { ...x, [k]: e.target.checked }
                                    : x,
                                ),
                              )
                            }
                          />
                        </td>
                      ),
                    )}
                    <td>
                      <button
                        className="button secondary small"
                        disabled={!can("admin", "write")}
                        onClick={() => save(r)}
                      >
                        <Save size={14} />
                        Guardar
                      </button>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </section>
      <p className="footnote">
        El administrador conserva acceso completo. Las comandas restringen
        además las acciones a cocina y salón según el rol.
      </p>
    </div>
  );
}
export function Profile() {
  const { user, setDialog, toast, logout, refresh } = useERP();
  const [secret, setSecret] = useState("");
  function form(title, fields, submit) {
    setDialog(
      <Modal title={title} onClose={() => setDialog(null)}>
        <Form
          fields={fields}
          module="admin"
          onClose={() => setDialog(null)}
          onSubmit={submit}
        />
      </Modal>,
    );
  }
  return (
    <div className="page enter">
      <div className="page-heading">
        <div>
          <span className="eyebrow">MI CUENTA / SEGURIDAD</span>
          <h1>Tu acceso a El Tumi.</h1>
          <p>
            {user.name} · {user.email}
          </p>
        </div>
      </div>
      <div className="profile-grid">
        <section className="panel padded">
          <KeyRound size={26} />
          <h2>Contraseña</h2>
          <p>
            Actualiza tu contraseña. Se cerrarán las sesiones activas de tu
            usuario.
          </p>
          <button
            className="button secondary"
            onClick={() =>
              form(
                "Cambiar contraseña",
                [
                  field("current", "Contraseña actual", "password", {
                    required: true,
                  }),
                  field(
                    "password",
                    "Nueva contraseña (10–72 caracteres)",
                    "password",
                    { required: true },
                  ),
                ],
                async (b) => {
                  await api("/auth/password", { method: "POST", body: b });
                  setDialog(null);
                  logout();
                },
              )
            }
          >
            Cambiar contraseña
          </button>
        </section>
        <section className="panel padded">
          <ShieldCheck size={26} />
          <h2>Autenticación de dos pasos</h2>
          <p>
            Protege tu acceso con códigos de seis dígitos desde una aplicación
            de autenticación.
          </p>
          {secret && (
            <div className="totp-secret">
              <small>
                Clave de configuración manual · TOTP / SHA1 / 30 s / 6 dígitos
              </small>
              <code>{secret}</code>
              <p>Guarda esta clave en tu autenticador antes de confirmar.</p>
            </div>
          )}
          <div className="heading-actions">
            {!user.totp_enabled && !secret && (
              <button
                className="button secondary"
                onClick={() =>
                  form(
                    "Configurar autenticador",
                    [
                      field("password", "Confirma tu contraseña", "password", {
                        required: true,
                      }),
                    ],
                    async (b) => {
                      const r = await api("/auth/totp/setup", {
                        method: "POST",
                        body: b,
                      });
                      setSecret(r.secret);
                      setDialog(null);
                    },
                  )
                }
              >
                Configurar 2FA
              </button>
            )}
            {secret && (
              <button
                className="button"
                onClick={() =>
                  form(
                    "Activar autenticador",
                    [
                      field("otp", "Código de seis dígitos", "text", {
                        required: true,
                      }),
                    ],
                    async (b) => {
                      await api("/auth/totp/enable", {
                        method: "POST",
                        body: b,
                      });
                      setSecret("");
                      setDialog(null);
                      toast("Autenticador activado");
                      refresh(true);
                    },
                  )
                }
              >
                Confirmar código
              </button>
            )}
            {user.totp_enabled && (
              <button
                className="button secondary"
                onClick={() =>
                  form(
                    "Desactivar autenticador",
                    [
                      field("password", "Contraseña", "password", {
                        required: true,
                      }),
                      field("otp", "Código del autenticador", "text", {
                        required: true,
                      }),
                    ],
                    async (b) => {
                      await api("/auth/totp/disable", {
                        method: "POST",
                        body: b,
                      });
                      setDialog(null);
                      toast("Autenticador desactivado");
                      refresh(true);
                    },
                  )
                }
              >
                Desactivar 2FA
              </button>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
