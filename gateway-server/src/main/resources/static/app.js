const versionEl = document.getElementById("version");
const errorEl = document.getElementById("error");
const routesEl = document.getElementById("routes");

const pathInput = document.getElementById("path");
const methodsInput = document.getElementById("methods");
const targetInput = document.getElementById("target");
const stripPrefixInput = document.getElementById("stripPrefix");
const rewriteInput = document.getElementById("rewrite");

document.getElementById("refresh").addEventListener("click", load);
document.getElementById("create").addEventListener("click", createRoute);

async function load() {
  clearError();
  try {
    const snapshot = await fetchJson("/admin/routes");
    versionEl.textContent = `版本：${snapshot.version || "未发布"}`;
    renderRoutes(snapshot.routes || []);
  } catch (error) {
    showError(error.message || "加载失败");
  }
}

async function createRoute() {
  clearError();
  try {
    await fetchJson("/admin/routes", {
      method: "POST",
      body: JSON.stringify({
        path: pathInput.value,
        methods: methodsInput.value.split(",").map((item) => item.trim()).filter(Boolean),
        target: targetInput.value,
        stripPrefix: Number(stripPrefixInput.value || 0),
        rewrite: rewriteInput.value || null
      })
    });
    pathInput.value = "";
    targetInput.value = "";
    rewriteInput.value = "";
    methodsInput.value = "GET";
    stripPrefixInput.value = "0";
    await load();
  } catch (error) {
    showError(error.message || "保存失败");
  }
}

async function toggleMethod(route) {
  clearError();
  const current = route.methods && route.methods.includes("GET") ? ["POST"] : ["GET"];
  try {
    await fetchJson(`/admin/routes/${route.id}`, {
      method: "PUT",
      body: JSON.stringify({ methods: current })
    });
    await load();
  } catch (error) {
    showError(error.message || "更新失败");
  }
}

async function removeRoute(route) {
  clearError();
  try {
    await fetchJson(`/admin/routes/${route.id}`, { method: "DELETE" });
    await load();
  } catch (error) {
    showError(error.message || "删除失败");
  }
}

function renderRoutes(routes) {
  routesEl.innerHTML = "";
  routes.forEach((route) => {
    const row = document.createElement("div");
    row.className = "row";
    row.innerHTML = `
      <span>${route.path}</span>
      <span>${(route.methods || ["ALL"]).join(", ")}</span>
      <span>${route.target}</span>
      <span class="actions"></span>
    `;
    const actions = row.querySelector(".actions");
    const toggle = document.createElement("button");
    toggle.textContent = "切换 Method";
    toggle.addEventListener("click", () => toggleMethod(route));
    const remove = document.createElement("button");
    remove.textContent = "删除";
    remove.addEventListener("click", () => removeRoute(route));
    actions.append(toggle, remove);
    routesEl.appendChild(row);
  });
}

function showError(message) {
  errorEl.textContent = message;
  errorEl.classList.remove("hidden");
}

function clearError() {
  errorEl.textContent = "";
  errorEl.classList.add("hidden");
}

async function fetchJson(url, options = {}) {
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });
  if (!res.ok) {
    const payload = await res.json().catch(() => ({}));
    throw new Error(payload.error || "请求失败");
  }
  if (res.status === 204) {
    return null;
  }
  return res.json();
}

load();
