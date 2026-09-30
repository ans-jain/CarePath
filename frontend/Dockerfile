# Stage 1: Build static distribution
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency manifests
COPY package.json package-lock.json ./
RUN npm ci

# Accept optional build arguments for API URLs
ARG VITE_BACKEND_URL
ARG VITE_ML_URL
ENV VITE_BACKEND_URL=${VITE_BACKEND_URL}
ENV VITE_ML_URL=${VITE_ML_URL}

# Copy full frontend source and build
COPY . .
RUN npm run build

# Stage 2: Hardened Alpine NGINX runtime
FROM nginx:1.27-alpine

LABEL maintainer="CarePath Platform Engineering"
LABEL description="CarePath React Frontend Application"

# Remove default nginx distribution files
RUN rm -rf /usr/share/nginx/html/*

# Copy built static assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom reverse proxy & SPA configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# Healthcheck probe
HEALTHCHECK --interval=20s --timeout=5s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
