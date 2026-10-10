const base = import.meta.env.VITE_API_URL || "/api";
export function token() {
  return sessionStorage.getItem("tumi-token") || "";
}
export async function api(path, options = {}) {
  const headers = {
    ...(token() ? { Authorization: `Bearer ${token()}` } : {}),
  };
  if (options.body && !(options.body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
    options = { ...options, body: JSON.stringify(options.body) };
  }
  let response;
  try {
    response = await fetch(base + path, {
      ...options,
      headers: { ...headers, ...options.headers },
    });
  } catch {
    throw new Error(
      "No se pudo conectar con el servidor. Comprueba que el backend esté ejecutándose.",
    );
  }
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    if (response.status === 401 && path != "/auth/login") {
      sessionStorage.removeItem("tumi-token");
      window.dispatchEvent(new Event("session-expired"));
    }
    throw new Error(
      data.message || `No se pudo completar la operación (${response.status})`,
    );
  }
  if (options.blob) return response.blob();
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
export async function download(path, name) {
  const blob = await api(path, { blob: true });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export const money = (v) =>
  new Intl.NumberFormat("es-PE", { style: "currency", currency: "PEN" }).format(
    Number(v || 0),
  );
export const today = () =>
  new Intl.DateTimeFormat("en-CA", { timeZone: "America/Lima" }).format(
    new Date(),
  );
export const title = (s) =>
  String(s || "")
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/^./, (c) => c.toUpperCase());
