const versionEl = document.getElementById("version");
const errorEl = document.getElementById("error");
const routesEl = document.getElementById("routes");
const groupFilterEl = document.getElementById("groupFilter");
const keywordFilterEl = document.getElementById("keywordFilter");
const testPathEl = document.getElementById("testPath");
const testApiKeyEl = document.getElementById("testApiKey");
const testResultEl = document.getElementById("testResult");
const metricsEl = document.getElementById("metrics");

const groupInput = document.getElementById("group");
const pathInput = document.getElementById("path");
const methodsInput = document.getElementById("methods");
const targetInput = document.getElementById("target");
const stripPrefixInput = document.getElementById("stripPrefix");
const rewriteInput = document.getElementById("rewrite");
const authTypeInput = document.getElementById("authType");
const apiKeyInput = document.getElementById("apiKey");
const rateLimitInput = document.getElementById("rateLimitQps");
const timeoutInput = document.getElementById("timeoutMs");
const enabledInput = document.getElementById("enabled");

document.getElementById("refresh").addEventListener("click", load);
document.getElementById("create").addEventListener("click", createRoute);
groupFilterEl.addEventListener("input", load);
keywordFilterEl.addEventListener("input", load);
document.getElementById("testRequest").addEventListener("click", sendTestRequest);

async function load() {
  clearError();
  try {
    const snapshot = await fetchJson("/admin/routes");
    versionEl.textContent = `版本：${snapshot.version || "未发布"}`;
    renderRoutes(snapshot.routes || []);
    await loadMetrics();
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
        group: groupInput.value || null,
        path: pathInput.value,
        methods: methodsInput.value.split(",").map((item) => item.trim()).filter(Boolean),
        target: targetInput.value,
        stripPrefix: Number(stripPrefixInput.value || 0),
        rewrite: rewriteInput.value || null,
        authType: authTypeInput.value || null,
        apiKey: apiKeyInput.value || null,
        rateLimitQps: Number(rateLimitInput.value || 0),
        timeoutMs: Number(timeoutInput.value || 0),
        enabled: enabledInput.checked
      })
    });
    groupInput.value = "";
    pathInput.value = "";
    targetInput.value = "";
    rewriteInput.value = "";
    methodsInput.value = "GET";
    stripPrefixInput.value = "0";
    authTypeInput.value = "";
    apiKeyInput.value = "";
    rateLimitInput.value = "0";
    timeoutInput.value = "3000";
    enabledInput.checked = true;
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
  const filter = (groupFilterEl.value || "").trim().toLowerCase();
  const keyword = (keywordFilterEl.value || "").trim().toLowerCase();
  routes
    .filter((route) => !filter || (route.group || "").toLowerCase().includes(filter))
    .filter((route) => !keyword || (route.path || "").toLowerCase().includes(keyword))
    .forEach((route) => {
    const row = document.createElement("div");
    row.className = "row";
    const authLabel = route.authType ? `${route.authType}${route.apiKey ? "(已配置)" : ""}` : "none";
    const rateLabel = route.rateLimitQps ? `${route.rateLimitQps}/s` : "不限流";
    row.innerHTML = `
      <span>${route.path}</span>
      <span>${route.group || "-"}</span>
      <span>${(route.methods || ["ALL"]).join(", ")}</span>
      <span>${route.target}</span>
      <span>${authLabel} · ${rateLabel}</span>
      <span>${route.enabled === false ? "禁用" : "启用"} · ${route.timeoutMs || 3000}ms</span>
      <span class="actions"></span>
    `;
    const actions = row.querySelector(".actions");
    const test = document.createElement("button");
    test.textContent = "测试";
    test.addEventListener("click", () => prefillTest(route));
    const toggleEnabled = document.createElement("button");
    toggleEnabled.textContent = route.enabled === false ? "启用" : "禁用";
    toggleEnabled.addEventListener("click", () => toggleEnable(route));
    const toggle = document.createElement("button");
    toggle.textContent = "切换 Method";
    toggle.addEventListener("click", () => toggleMethod(route));
    const remove = document.createElement("button");
    remove.textContent = "删除";
    remove.addEventListener("click", () => removeRoute(route));
    actions.append(test, toggleEnabled, toggle, remove);
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

async function loadMetrics() {
  try {
    const metrics = await fetchJson("/admin/routes/metrics");
    renderMetrics(metrics || []);
  } catch (error) {
    metricsEl.textContent = error.message || "无法加载统计";
  }
}

function renderMetrics(metrics) {
  if (!metrics.length) {
    metricsEl.textContent = "暂无统计数据";
    return;
  }
  metricsEl.innerHTML = "";
  metrics.forEach((metric) => {
    const item = document.createElement("div");
    item.className = "metric-item";
    item.innerHTML = `
      <span class="metric-id">${metric.routeId}</span>
      <span>命中：${metric.hits}</span>
      <span>最近：${metric.lastHit || "-"}</span>
    `;
    metricsEl.appendChild(item);
  });
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

function prefillTest(route) {
  let samplePath = route.path || "/api/account/1001";
  if (samplePath.includes("**")) {
    samplePath = samplePath.replace("**", "1001");
  }
  if (samplePath.includes("*")) {
    samplePath = samplePath.replace("*", "1001");
  }
  samplePath = samplePath.replaceAll("{id}", "1001");
  testPathEl.value = samplePath.startsWith("/") ? samplePath : `/${samplePath}`;
  testApiKeyEl.value = route.apiKey || testApiKeyEl.value;
  testResultEl.textContent = "已填充测试路径，可点击发送测试请求";
}

async function sendTestRequest() {
  clearError();
  testResultEl.textContent = "请求中...";
  try {
    const res = await fetch(testPathEl.value, {
      headers: {
        "X-API-Key": testApiKeyEl.value
      }
    });
    const text = await res.text();
    testResultEl.textContent = `Status: ${res.status}\n${text}`;
  } catch (error) {
    testResultEl.textContent = error.message || "请求失败";
  }
}

async function toggleEnable(route) {
  clearError();
  try {
    await fetchJson(`/admin/routes/${route.id}`, {
      method: "PUT",
      body: JSON.stringify({ enabled: !(route.enabled === false) })
    });
    await load();
  } catch (error) {
    showError(error.message || "更新失败");
  }
}
