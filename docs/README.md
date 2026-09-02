# Verus Farm Console

Web estatica para GitHub Pages. Sirve como panel de control de la granja: no ejecuta mineria en el navegador.

## Publicar en GitHub Pages

1. Crea un repositorio GitHub y sube este proyecto.
2. En Settings > Pages, selecciona GitHub Actions como fuente de despliegue.
3. El workflow incluido publicara automaticamente el contenido de `docs/` cuando se haga push a `main`.

## Conexion de la granja real

La API esta en `server/` y expone `GET /api/farm/status` y `POST /api/farm/telemetry`. Para ejecutarla localmente, define las variables de `.env.example` y usa `npm start` dentro de `server/`.

Para que la pagina publicada en GitHub Pages lea la granja, configura `VERUS_FARM_API_URL` en `docs/config.js`. Tambien se puede pasar temporalmente con `?api=https://tu-api.example.com`. Ejemplo: `https://usuario.github.io/repo/?api=https://api.example.com`.

Nunca se deben exponer wallets, claves de pool ni `FARM_API_KEY` en esta web estatica. Los equipos envian la telemetria al backend por HTTPS usando la clave privada.
