FROM node:20-alpine

WORKDIR /reactservice

COPY public ./public
COPY src ./src
COPY package.json ./

ENV PORT=3001
EXPOSE 3001

RUN npm install

CMD ["npm", "start"]