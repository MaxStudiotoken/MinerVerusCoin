FROM node:24-alpine

WORKDIR /app
COPY package.json ./
COPY server.js ./

ENV NODE_ENV=production
ENV FARM_DATA_FILE=/data/farm-state.json
EXPOSE 8787
VOLUME ["/data"]

CMD ["node", "server.js"]
