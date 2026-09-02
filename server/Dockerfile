FROM node:24-alpine

WORKDIR /app
COPY package.json ./
COPY server.js ./

ENV NODE_ENV=production
EXPOSE 8787

CMD ["node", "server.js"]
