# Etapa 1: Build de la app con Node
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
COPY vite.config.js ./
COPY . .
RUN npm install && npm run build

# Etapa 2: Servir estáticos con nginx
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY --from=builder /app/nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
