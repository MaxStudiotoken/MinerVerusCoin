const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const { URL } = require("node:url");

const envPath = path.join(__dirname, ".env");
if (fs.existsSync(envPath)) {
  for (const line of fs.readFileSync(envPath, "utf8").split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Z0-9_]+)=(.*)\s*$/);
    if (match && process.env[match[1]] === undefined) {
      process.env[match[1]] = match[2];
    }
  }
}

const port = Number(process.env.PORT || 8787);
const apiKey = process.env.FARM_API_KEY;
const allowedOrigin = process.env.ALLOWED_ORIGIN || "*";
const workers = new Map();
const events = [];

if (!apiKey) {
  console.error("FARM_API_KEY is required. Copy .env.example and set a long secret.");
  process.exit(1);
}

function addEvent(message) {
  events.unshift({ message, timestamp: Date.now() });
  events.splice(20);
}

function send(response, statusCode, payload) {
  response.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "Access-Control-Allow-Origin": allowedOrigin,
    "Vary": "Origin"
  });
  response.end(JSON.stringify(payload));
}

function readBody(request) {
  return new Promise((resolve, reject) => {
    let body = "";
    request.on("data", (chunk) => {
      body += chunk;
      if (body.length > 32_000) {
        reject(new Error("Payload too large"));
        request.destroy();
      }
    });
    request.on("end", () => {
      try {
        resolve(JSON.parse(body || "{}"));
      } catch {
        reject(new Error("Invalid JSON"));
      }
    });
  });
}

function isAuthorized(request) {
  return request.headers["x-farm-key"] === apiKey;
}

function validTelemetry(payload) {
  return typeof payload.workerId === "string" && payload.workerId.length <= 64 &&
    typeof payload.name === "string" && payload.name.length <= 64 &&
    typeof payload.hashrate === "number" && Number.isFinite(payload.hashrate) && payload.hashrate >= 0 &&
    typeof payload.acceptedShares === "number" && Number.isInteger(payload.acceptedShares) && payload.acceptedShares >= 0 &&
    typeof payload.rejectedShares === "number" && Number.isInteger(payload.rejectedShares) && payload.rejectedShares >= 0 &&
    typeof payload.isMining === "boolean" &&
    typeof payload.cpuLoadPercent === "number" && Number.isFinite(payload.cpuLoadPercent) && payload.cpuLoadPercent >= 0 && payload.cpuLoadPercent <= 100;
}

function farmStatus() {
  const now = Date.now();
  const allWorkers = [...workers.values()].map((worker) => ({
    ...worker,
    online: now - worker.lastSeenAt < 90_000
  }));
  return {
    updatedAt: now,
    workers: allWorkers,
    events
  };
}

const server = http.createServer(async (request, response) => {
  const url = new URL(request.url, `http://${request.headers.host}`);

  if (request.method === "OPTIONS") {
    response.writeHead(204, {
      "Access-Control-Allow-Origin": allowedOrigin,
      "Access-Control-Allow-Headers": "Content-Type, X-Farm-Key",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Vary": "Origin"
    });
    return response.end();
  }

  if (request.method === "GET" && url.pathname === "/health") {
    return send(response, 200, { ok: true });
  }

  if (request.method === "GET" && url.pathname === "/api/farm/status") {
    return send(response, 200, farmStatus());
  }

  if (request.method === "POST" && url.pathname === "/api/farm/telemetry") {
    if (!isAuthorized(request)) {
      return send(response, 401, { error: "Unauthorized" });
    }

    try {
      const payload = await readBody(request);
      if (!validTelemetry(payload)) {
        return send(response, 400, { error: "Invalid telemetry payload" });
      }

      const worker = {
        id: payload.workerId,
        name: payload.name,
        type: payload.type === "android" ? "Android app" : "Rig",
        active: payload.isMining,
        hashrate: payload.hashrate,
        shares: payload.acceptedShares,
        rejected: payload.rejectedShares,
        load: Math.round(payload.cpuLoadPercent),
        lastSeenAt: Date.now()
      };
      const previous = workers.get(worker.id);
      workers.set(worker.id, worker);
      if (!previous || previous.active !== worker.active) {
        addEvent(`${worker.name}: ${worker.active ? "worker activo" : "worker detenido"}`);
      }
      return send(response, 202, { accepted: true, receivedAt: worker.lastSeenAt });
    } catch (error) {
      return send(response, 400, { error: error.message });
    }
  }

  return send(response, 404, { error: "Not found" });
});

server.listen(port, () => {
  console.log(`Verus Farm API listening on http://localhost:${port}`);
  addEvent("Farm API started");
});
