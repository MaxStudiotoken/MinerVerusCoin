# Verus Farm

Proyecto formado por una app Android y una consola web para supervisar workers VRSC.

## Componentes

- `app/`: dashboard Android. Publica telemetria de su worker a la Farm API cuando se configura una URL HTTPS y una clave.
- `docs/`: web estatica para GitHub Pages. Muestra la granja en modo demo o conectada a la API.
- `server/`: Farm API Node.js. Recibe telemetria autenticada y expone un estado de solo lectura para la web.

## Puesta en marcha

### 1. API

En `server/`, copia `.env.example` a `.env`, cambia `FARM_API_KEY` por un secreto largo y define el dominio exacto de GitHub Pages en `ALLOWED_ORIGIN`.

```powershell
cd server
Copy-Item .env.example .env
npm start
```

La API debe desplegarse detras de HTTPS antes de conectarla con la app Android. Tiene el endpoint de salud `GET /health`.

Tambien puede ejecutarse en un contenedor:

```powershell
docker build -t verus-farm-api ./server
docker run --env-file server/.env -p 8787:8787 -v verus-farm-data:/data verus-farm-api
```

En produccion publica el contenedor mediante un proxy o plataforma que termine TLS y entregue una URL `https://`. El volumen `verus-farm-data` conserva workers y eventos entre reinicios.

### 2. Dashboard web

Actualiza `docs/config.js` con la URL HTTPS de la API y sube el proyecto a GitHub. El workflow de `.github/workflows/deploy-pages.yml` publica `docs/` al hacer push a `main`.

### 3. App Android

En la app configura la wallet, pool, nombre del worker, URL HTTPS de Farm API y la misma clave privada. La app nunca envia wallet ni datos del pool a la Farm API.

## Alcance actual

La aplicacion representa una sesion de minado y la monitoriza. Incluye un puente JNI compilado con Android NDK para `arm64-v8a`, preparado para portar el nucleo VerusHash portable.

El motor VRSC real aun requiere validar vectores de hash en ARM y conectar un cliente Stratum, con controles termicos, de bateria y consentimiento explicito del usuario.
