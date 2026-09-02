const STORAGE_KEY = "verus-farm-console-v1";
const MARKET_URL = "https://api.coingecko.com/api/v3/simple/price?ids=verus-coin&vs_currencies=usd&include_24hr_change=true";
const apiBaseUrl = (
  new URLSearchParams(window.location.search).get("api") || window.VERUS_FARM_API_URL || ""
).trim().replace(/\/$/, "");

const defaultState = {
  price: null,
  priceChange: null,
  logs: [
    { text: "Consola de granja iniciada en modo demo", time: "ahora" },
    { text: "Los datos locales no se envian a un pool", time: "ahora" },
    { text: "Esperando telemetria verificada de la Farm API", time: "ahora" }
  ],
  workers: [
    { id: 1, name: "android-demo", type: "Datos simulados", active: false, hashrate: 0, shares: 0, rejected: 0, load: 0 }
  ]
};

const state = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null") || defaultState;
const $ = (selector) => document.querySelector(selector);
const formatUsd = (value) => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", minimumFractionDigits: value < 1 ? 4 : 2 }).format(value);
const formatHashrate = (value) => value >= 1000 ? `${(value / 1000).toFixed(2)} MH/s` : `${value.toFixed(2)} kH/s`;

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function addLog(text) {
  state.logs.unshift({ text, time: "ahora" });
  state.logs = state.logs.slice(0, 6);
  persist();
}

function renderWorkers() {
  $("#workers").innerHTML = state.workers.map((worker) => `
    <article class="worker" style="--worker-glow:${worker.active ? "#3165d4" : "#75859c"}">
      <div class="worker-top">
        <div><div class="worker-name">${worker.name}</div><span class="status ${worker.active ? "" : "offline"}">${worker.active ? "DEMO ACTIVA" : "DETENIDO"}</span></div>
        <button class="worker-toggle" data-worker="${worker.id}" ${apiBaseUrl ? "disabled" : ""}>${worker.active ? "DETENER DEMO" : "INICIAR DEMO"}</button>
      </div>
      <div class="worker-stat"><div><strong>${formatHashrate(worker.hashrate)}</strong><span>hash rate actual</span></div><span>${worker.type}</span></div>
      <div class="worker-footer"><span>${worker.shares} eventos demo</span><span>${worker.rejected} rechazados</span><span>${worker.load}% carga</span></div>
    </article>`).join("");

  document.querySelectorAll("[data-worker]").forEach((button) => {
    button.addEventListener("click", () => toggleWorker(Number(button.dataset.worker)));
  });
}

function renderMetrics() {
  const workers = state.workers;
  const activeWorkers = workers.filter((worker) => worker.active);
  const totalHashrate = activeWorkers.reduce((sum, worker) => sum + worker.hashrate, 0);
  const shares = workers.reduce((sum, worker) => sum + worker.shares, 0);
  const rejected = workers.reduce((sum, worker) => sum + worker.rejected, 0);
  const dailyVrsc = totalHashrate * 0.0105;
  const dailyUsd = dailyVrsc * (state.price || 0);

  $("#total-hashrate").textContent = formatHashrate(totalHashrate);
  $("#hashrate-detail").textContent = `${activeWorkers.length} workers activos`;
  $("#daily-vrsc").textContent = dailyVrsc.toFixed(4);
  $("#daily-usd").textContent = formatUsd(dailyUsd);
  $("#total-shares").textContent = shares;
  $("#rejected-shares").textContent = `${rejected} rechazadas`;

  if (state.price !== null) {
    $("#vrsc-price").textContent = formatUsd(state.price);
    const change = state.priceChange || 0;
    const label = $("#price-change");
    label.textContent = `${change >= 0 ? "+" : ""}${change.toFixed(2)}% en 24h`;
    label.className = change >= 0 ? "positive" : "negative";
  }
}

function renderActivity() {
  $("#activity").innerHTML = state.logs.map((log, index) => `<div class="activity-item"><i style="background:${index === 0 ? "#4aa658" : "#6d9cff"}"></i><span>${log.text}</span><time>${log.time}</time></div>`).join("");
}

function renderChart() {
  const activeHash = state.workers.filter((worker) => worker.active).reduce((sum, worker) => sum + worker.hashrate, 0);
  const bars = Array.from({ length: 30 }, (_, index) => Math.max(7, Math.min(94, 52 + Math.sin(index * 0.47) * 17 + (index / 30) * 20 + activeHash * 0.7)));
  $("#chart").innerHTML = bars.map((height, index) => `<span class="bar" style="height:${height}%; animation-delay:${index * 18}ms"></span>`).join("");
}

function render() {
  renderWorkers();
  renderMetrics();
  renderActivity();
  renderChart();
}

async function syncFarmStatus() {
  if (!apiBaseUrl) return;
  try {
    const response = await fetch(`${apiBaseUrl}/api/farm/status`);
    if (!response.ok) throw new Error("Farm API unavailable");
    const remoteState = await response.json();
    state.workers = remoteState.workers.map((worker) => ({ ...worker, active: worker.active && worker.online }));
    state.logs = remoteState.events.map((event) => ({
      text: event.message,
      time: new Date(event.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
    }));
    $("#connection-label").textContent = "Granja conectada";
    $("#start-all").disabled = true;
    $("#start-all").textContent = "Control desde la app";
    render();
  } catch {
    $("#connection-label").textContent = "API no disponible";
  }
}

function toggleWorker(id) {
  const worker = state.workers.find((item) => item.id === id);
  if (!worker) return;
  worker.active = !worker.active;
  worker.hashrate = worker.active ? Math.max(worker.hashrate, 4.2) : 0;
  worker.load = worker.active ? Math.max(worker.load, 60) : 0;
  addLog(`${worker.name}: ${worker.active ? "demo iniciada" : "demo detenida"}`);
  persist();
  render();
}

async function refreshMarket() {
  const button = $("#refresh-market");
  button.textContent = "Actualizando...";
  button.disabled = true;
  try {
    const response = await fetch(MARKET_URL);
    if (!response.ok) throw new Error("market unavailable");
    const asset = (await response.json())["verus-coin"];
    state.price = asset.usd;
    state.priceChange = asset.usd_24h_change;
    addLog("Precio VRSC actualizado desde CoinGecko");
  } catch {
    addLog("No se pudo actualizar el mercado. Se mantiene el ultimo valor.");
  } finally {
    button.textContent = "Actualizar mercado";
    button.disabled = false;
    persist();
    render();
  }
}

$("#start-all").addEventListener("click", () => {
  state.workers.forEach((worker) => { if (!worker.active) { worker.active = true; worker.hashrate = 4.2; worker.load = 55; } });
  addLog("Todos los workers demo fueron activados; no se enviaron hashes al pool");
  persist();
  render();
});

$("#refresh-market").addEventListener("click", refreshMarket);
$("#add-worker").addEventListener("click", () => {
  const number = state.workers.length + 1;
  state.workers.push({ id: Date.now(), name: `rig-demo-${number}`, type: "Demo rig", active: false, hashrate: 0, shares: 0, rejected: 0, load: 0 });
  addLog(`Worker rig-demo-${number} agregado en modo demo`);
  persist();
  render();
});

render();
refreshMarket();
syncFarmStatus();
if (apiBaseUrl) window.setInterval(syncFarmStatus, 15_000);
