FROM node:22-alpine as builder
RUN apk add --no-cache git python3 make g++
WORKDIR /app
ADD . .
RUN yarn install
RUN yarn run build

FROM node:22-alpine as production
WORKDIR /app
COPY --from=builder /app/continuum-workbench/lib .
CMD [ "backend/main.js" ]