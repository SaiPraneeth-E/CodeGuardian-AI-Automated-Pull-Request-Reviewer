# =============================================================
# Stage 1: Build the React dashboard
# =============================================================
FROM node:20-alpine AS build

WORKDIR /app

# Copy dependency descriptors first for layer caching
COPY package.json ./
RUN npm install

# Copy source and build production bundle
COPY . .
RUN npm run build

# =============================================================
# Stage 2: Serve with Nginx
# =============================================================
FROM nginx:alpine AS runtime

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config with reverse proxy rules
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built React assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
