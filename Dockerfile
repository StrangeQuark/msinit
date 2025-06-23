FROM node:20-alpine

WORKDIR /nodeservice

COPY package.json ./
COPY server.js ./
COPY launch_script.py ./

ENV PORT=3000
EXPOSE 3000

RUN npm install --production

CMD ["node", "server.js"]