# =============================================================================
# UI Multi-Stage Dockerfile - Service Virtualization Platform
# =============================================================================
#
# Stage 1 (builder): Installs dependencies and runs the webpack production build
# Stage 2 (runtime): Copies the built dist/ into nginx and configures serving
#
# Runtime environment variable:
#   BACKEND_URL  - ClusterIP URL of the backend service
#                  e.g. http://margin-app-service-virtualization-backend.margin.svc.cluster.local:8080
#
# Build:  docker build -t service-virtualization-ui:latest .
# Local:  docker build --build-arg NODE_ENV=development -t sv-ui:local .
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build the React application
# -----------------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency manifests first to leverage Docker layer cache
COPY package.json package-lock.json* ./

# Install dependencies (ci for reproducible installs)
RUN npm ci --prefer-offline --no-audit

# Copy source code
COPY . .

# Produce optimised production bundle → dist/
RUN npm run build

# -----------------------------------------------------------------------------
# Stage 2: Serve the built application with nginx
# -----------------------------------------------------------------------------
FROM nginx:1.25-alpine

# Runtime dependencies: curl for health-checks, envsubst (ships with gettext)
RUN apk add --no-cache curl gettext

# Create non-root application user
RUN addgroup -g 1000 appuser && \
    adduser -u 1000 -G appuser -s /bin/sh -D appuser

# Remove default nginx content
RUN rm -rf /usr/share/nginx/html/*

# Copy pre-built React bundle from builder stage
COPY --from=builder /app/dist/ /usr/share/nginx/html/

# Copy nginx config template (contains ${BACKEND_URL} placeholder)
COPY nginx.conf /etc/nginx/nginx.conf.template

# Pre-create all runtime directories nginx needs with correct ownership
RUN mkdir -p /var/cache/nginx/client_temp \
             /var/cache/nginx/proxy_temp \
             /var/cache/nginx/fastcgi_temp \
             /var/cache/nginx/uwsgi_temp \
             /var/cache/nginx/scgi_temp \
             /var/log/nginx \
             /var/run \
             /tmp/nginx && \
    touch /etc/nginx/nginx.conf && \
    chown -R appuser:appuser /var/cache/nginx \
                             /var/log/nginx \
                             /var/run \
                             /tmp/nginx \
                             /usr/share/nginx/html \
                             /etc/nginx/nginx.conf \
                             /etc/nginx/nginx.conf.template

USER appuser

EXPOSE 8080

ENV NGINX_WORKER_PROCESSES=auto
ENV NGINX_WORKER_CONNECTIONS=1024
ENV BACKEND_URL=http://localhost:8080
ENV NGINX_PORT=8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:${NGINX_PORT}/health || exit 1

LABEL maintainer="Service Virtualization Team"
LABEL description="Service Virtualization UI - Runtime Container"
LABEL version="1.0"

# Substitute ${BACKEND_URL} at container start then launch nginx
CMD ["sh", "-c", "envsubst '${BACKEND_URL} ${NGINX_PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf && exec nginx -g 'daemon off;'"]
