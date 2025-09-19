# Stage 1: Build
FROM node:20-alpine AS build

WORKDIR /nodeservice

COPY package*.json ./
RUN npm install

COPY server.js ./
COPY launch_script.* ./

# Stage 2: Deploy
FROM node:20-alpine AS runtime

WORKDIR /nodeservice

ENV PORT=3000
EXPOSE 3000

COPY package*.json ./
RUN npm install --production

COPY --from=build /nodeservice/server.js ./
COPY --from=build /nodeservice/launch_script.* ./

CMD ["node", "server.js"]
