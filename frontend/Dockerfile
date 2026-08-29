# ---- Build stage: compile the React/Vite production bundle ----
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies from the lockfile for a reproducible build.
COPY package.json package-lock.json ./
RUN npm ci

# Copy sources (node_modules/dist are excluded via .dockerignore).
COPY . .

# API base URL is injected at build time. Empty (default) = relative URLs,
# so the app calls its own origin and nginx proxies /api to the backend.
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ---- Runtime stage: serve the static build with nginx ----
FROM nginx:1.27-alpine
# Default upstream is the Docker Compose backend service; override BACKEND_UPSTREAM
# in production to point at the hosted backend origin. The entrypoint renders
# /etc/nginx/templates/*.template into /etc/nginx/conf.d/ via envsubst.
ENV BACKEND_UPSTREAM=backend:8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
