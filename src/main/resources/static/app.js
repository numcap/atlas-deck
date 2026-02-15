const state = {
  apps: [],
  deployments: [],
  active: [],
  editingId: null
};

const statusText = document.getElementById("status-text");
const appList = document.getElementById("app-list");
const deploymentList = document.getElementById("deployment-list");
const activeList = document.getElementById("active-list");
const appForm = document.getElementById("app-form");
const formTitle = document.getElementById("form-title");
const submitButton = document.getElementById("submit-app");
const resetButton = document.getElementById("reset-form");
const toastRoot = document.getElementById("toast-root");

const counters = {
  apps: document.getElementById("count-apps"),
  deployments: document.getElementById("count-deployments"),
  active: document.getElementById("count-active")
};

function setStatus(text) {
  statusText.textContent = text;
}

function showToast(message, tone = "info") {
  const toast = document.createElement("div");
  toast.className = `toast ${tone === "error" ? "error" : ""}`;
  toast.textContent = message;
  toastRoot.appendChild(toast);
  setTimeout(() => toast.remove(), 3600);
}

async function request(url, options = {}) {
  const opts = {
    headers: {
      "Content-Type": "application/json"
    },
    ...options
  };

  if (opts.body === undefined) {
    delete opts.body;
  }

  const res = await fetch(url, opts);
  const raw = await res.text();
  let data = null;
  if (raw) {
    try {
      data = JSON.parse(raw);
    } catch {
      data = raw;
    }
  }

  if (!res.ok) {
    const message = typeof data === "string" && data.length ? data : res.statusText;
    throw new Error(message || "Request failed");
  }

  return data;
}

function formatDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function shortId(value) {
  if (!value) return "-";
  return value.toString().slice(0, 8);
}

function formatEnv(env) {
  if (!env || typeof env !== "object" || Array.isArray(env)) {
    return "-";
  }
  const keys = Object.keys(env);
  if (!keys.length) return "-";
  return JSON.stringify(env);
}

function setEditing(app) {
  state.editingId = app.id;
  formTitle.textContent = `Update Application`; 
  submitButton.textContent = "Update Application";
  appForm.elements.name.value = app.name || "";
  appForm.elements.image.value = app.image || "";
  appForm.elements.desiredReplicas.value = app.desiredReplicas ?? "";
  appForm.elements.containerPort.value = app.containerPort ?? "";
  appForm.elements.resources_cpu.value = app.resources_cpu || "";
  appForm.elements.resources_ram.value = app.resources_ram || "";
  appForm.elements.serviceEnabled.checked = Boolean(app.serviceEnabled);
  appForm.elements.env.value = app.env && Object.keys(app.env || {}).length
    ? JSON.stringify(app.env, null, 2)
    : "";
}

function resetForm() {
  state.editingId = null;
  formTitle.textContent = "Create Application";
  submitButton.textContent = "Create Application";
  appForm.reset();
}

function buildPayload() {
  const payload = {};
  const name = appForm.elements.name.value.trim();
  const image = appForm.elements.image.value.trim();
  const desiredReplicas = appForm.elements.desiredReplicas.value;
  const containerPort = appForm.elements.containerPort.value;
  const resourcesCpu = appForm.elements.resources_cpu.value.trim();
  const resourcesRam = appForm.elements.resources_ram.value.trim();
  const envRaw = appForm.elements.env.value.trim();

  if (!name || !image) {
    throw new Error("Name and image are required.");
  }

  payload.name = name;
  payload.image = image;
  payload.serviceEnabled = appForm.elements.serviceEnabled.checked;

  if (desiredReplicas !== "") payload.desiredReplicas = Number(desiredReplicas);
  if (containerPort !== "") payload.containerPort = Number(containerPort);
  if (resourcesCpu) payload.resources_cpu = resourcesCpu;
  if (resourcesRam) payload.resources_ram = resourcesRam;

  if (envRaw) {
    try {
      payload.env = JSON.parse(envRaw);
    } catch (err) {
      throw new Error("Env JSON must be valid JSON.");
    }
  }

  return payload;
}

function renderApps() {
  counters.apps.textContent = state.apps.length;
  if (!state.apps.length) {
    appList.innerHTML = "<div class=\"app-card\"><strong>No applications yet.</strong><span>Create one to get started.</span></div>";
    return;
  }

  appList.innerHTML = state.apps
    .map((app, index) => {
      const delay = `${index * 0.05}s`;
      return `
        <article class="app-card" data-app-id="${app.id}" style="animation-delay:${delay}">
          <div class="title-row">
            <div>
              <h3>${app.name}</h3>
              <span class="pill">${shortId(app.id)}</span>
            </div>
            <button class="btn ghost" data-action="edit">Edit</button>
          </div>
          <div class="meta-grid">
            <span><strong>Image:</strong> ${app.image || "-"}</span>
            <span><strong>Replicas:</strong> ${app.desiredReplicas ?? "-"}</span>
            <span><strong>Port:</strong> ${app.containerPort ?? "-"}</span>
            <span><strong>Service:</strong> ${app.serviceEnabled ? "Enabled" : "Off"}</span>
            <span><strong>CPU:</strong> ${app.resources_cpu || "-"}</span>
            <span><strong>RAM:</strong> ${app.resources_ram || "-"}</span>
            <span><strong>Env:</strong> ${formatEnv(app.env)}</span>
            <span><strong>Updated:</strong> ${formatDate(app.updatedAt)}</span>
          </div>
          <div class="app-actions">
            <div class="action-row">
              <button class="btn primary" data-action="deploy">Deploy</button>
              <button class="btn ghost" data-action="restart">Restart</button>
              <button class="btn ghost" data-action="stop">Stop</button>
            </div>
            <div class="action-row">
              <span class="label">Replicas</span>
              <input class="replicas-input" type="number" min="0" value="${app.desiredReplicas ?? 1}">
              <button class="btn ghost" data-action="start">Start</button>
            </div>
            <div class="action-row">
              <span class="label">Service</span>
              <select class="service-type">
                <option value="ClusterIP">ClusterIP</option>
                <option value="NodePort">NodePort</option>
                <option value="LoadBalancer">LoadBalancer</option>
              </select>
              <button class="btn ghost" data-action="expose">Expose</button>
            </div>
            <div class="action-row">
              <button class="btn danger" data-action="delete-deploy">Delete Deployment</button>
              <button class="btn danger" data-action="delete-app">Delete App</button>
            </div>
          </div>
        </article>
      `;
    })
    .join("");

  bindAppActions();
}

function renderDeployments() {
  counters.deployments.textContent = state.deployments.length;
  if (!state.deployments.length) {
    deploymentList.innerHTML = "<div class=\"deploy-card\"><strong>No deployments found.</strong><span>Deploy an application to see details.</span></div>";
    return;
  }

  deploymentList.innerHTML = state.deployments
    .map((deployment, index) => {
      const delay = `${index * 0.05}s`;
      return `
        <article class="deploy-card" style="animation-delay:${delay}">
          <div class="title-row">
            <h3>${deployment.name}</h3>
            <span class="pill">${deployment.deploymentExists ? "Exists" : "Missing"}</span>
          </div>
          <div class="meta-grid">
            <span><strong>Image:</strong> ${deployment.image || "-"}</span>
            <span><strong>Desired:</strong> ${deployment.desiredReplicas}</span>
            <span><strong>Spec:</strong> ${deployment.specReplicas}</span>
            <span><strong>Ready:</strong> ${deployment.readyReplicas}</span>
            <span><strong>Available:</strong> ${deployment.availableReplicas}</span>
            <span><strong>Updated:</strong> ${deployment.updatedReplicas}</span>
          </div>
        </article>
      `;
    })
    .join("");
}

function renderActive() {
  counters.active.textContent = state.active.length;
  if (!state.active.length) {
    activeList.innerHTML = "<div class=\"deploy-card\"><strong>No active deployments.</strong><span>Start replicas to see live state.</span></div>";
    return;
  }

  activeList.innerHTML = state.active
    .map((deployment, index) => {
      const delay = `${index * 0.05}s`;
      return `
        <article class="deploy-card" style="animation-delay:${delay}">
          <div class="title-row">
            <h3>${deployment.name}</h3>
            <span class="pill">Active</span>
          </div>
          <div class="meta-grid">
            <span><strong>Image:</strong> ${deployment.image || "-"}</span>
            <span><strong>Desired:</strong> ${deployment.desiredReplicas}</span>
            <span><strong>Spec:</strong> ${deployment.specReplicas}</span>
            <span><strong>Ready:</strong> ${deployment.readyReplicas}</span>
            <span><strong>Available:</strong> ${deployment.availableReplicas}</span>
            <span><strong>Updated:</strong> ${deployment.updatedReplicas}</span>
          </div>
        </article>
      `;
    })
    .join("");
}

function bindAppActions() {
  document.querySelectorAll(".app-card").forEach((card) => {
    const appId = card.dataset.appId;
    const replicasInput = card.querySelector(".replicas-input");
    const serviceSelect = card.querySelector(".service-type");

    card.querySelectorAll("[data-action]").forEach((button) => {
      button.addEventListener("click", async () => {
        const action = button.dataset.action;
        const app = state.apps.find((item) => item.id === appId);

        try {
          if (action === "edit") {
            setEditing(app);
            setStatus(`Editing ${app.name}.`);
            return;
          }

          if (action === "delete-app") {
            await request(`/api/application/${appId}`, { method: "DELETE" });
            showToast(`Deleted application ${app.name}.`);
            await refreshAll();
            return;
          }

          if (action === "deploy") {
            await request(`/api/deployment/${appId}`, { method: "POST" });
            showToast(`Deployment created for ${app.name}.`);
            await refreshDeployments();
            return;
          }

          if (action === "restart") {
            await request(`/api/deployment/${appId}/restart`, { method: "POST" });
            showToast(`Restart triggered for ${app.name}.`);
            await refreshDeployments();
            return;
          }

          if (action === "stop") {
            await request(`/api/deployment/${appId}/stop`, { method: "POST" });
            showToast(`Stopped deployment for ${app.name}.`);
            await refreshDeployments();
            return;
          }

          if (action === "start") {
            const replicas = Number(replicasInput.value || 0);
            await request(`/api/deployment/${appId}/start?replicas=${encodeURIComponent(replicas)}`, {
              method: "POST"
            });
            showToast(`Scaled ${app.name} to ${replicas}.`);
            await refreshDeployments();
            return;
          }

          if (action === "expose") {
            const type = serviceSelect.value;
            await request(`/api/deployment/${appId}/service?type=${encodeURIComponent(type)}`, {
              method: "POST"
            });
            showToast(`Service exposed for ${app.name} (${type}).`);
            await refreshDeployments();
            return;
          }

          if (action === "delete-deploy") {
            await request(`/api/deployment/${appId}`, { method: "DELETE" });
            showToast(`Deployment deleted for ${app.name}.`);
            await refreshDeployments();
            return;
          }
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });
  });
}

async function refreshApps() {
  try {
    setStatus("Loading applications...");
    state.apps = await request("/api/application");
    renderApps();
    setStatus("Applications loaded.");
  } catch (err) {
    showToast(err.message, "error");
    setStatus("Failed to load applications.");
  }
}

async function refreshDeployments() {
  try {
    setStatus("Loading deployments...");
    state.deployments = await request("/api/deployment");
    renderDeployments();
    setStatus("Deployments loaded.");
  } catch (err) {
    showToast(err.message, "error");
    setStatus("Failed to load deployments.");
  }
}

async function refreshActive() {
  try {
    setStatus("Loading active deployments...");
    state.active = await request("/api/deployment/active");
    renderActive();
    setStatus("Active deployments loaded.");
  } catch (err) {
    showToast(err.message, "error");
    setStatus("Failed to load active deployments.");
  }
}

async function refreshAll() {
  await Promise.all([refreshApps(), refreshDeployments(), refreshActive()]);
}

appForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const payload = buildPayload();
    if (state.editingId) {
      await request(`/api/application/update/${state.editingId}`, {
        method: "POST",
        body: JSON.stringify(payload)
      });
      showToast("Application updated.");
    } else {
      await request("/api/application", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      showToast("Application created.");
    }
    resetForm();
    await refreshAll();
  } catch (err) {
    showToast(err.message, "error");
  }
});

resetButton.addEventListener("click", () => {
  resetForm();
  setStatus("Form reset.");
});

document.getElementById("refresh-all").addEventListener("click", refreshAll);
document.getElementById("refresh-apps").addEventListener("click", refreshApps);
document.getElementById("refresh-deployments").addEventListener("click", refreshDeployments);
document.getElementById("refresh-active").addEventListener("click", refreshActive);
document.getElementById("deploy-all").addEventListener("click", async () => {
  try {
    await request("/api/deployment/all", { method: "POST" });
    showToast("Deploying all applications.");
    await refreshDeployments();
  } catch (err) {
    showToast(err.message, "error");
  }
});

refreshAll();
