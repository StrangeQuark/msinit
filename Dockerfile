FROM node:20-alpine

WORKDIR /nodeservice

COPY package.json ./
COPY server.js ./

ENV PORT=3000
EXPOSE 3000

RUN npm install

CMD ["node", "server.js"]