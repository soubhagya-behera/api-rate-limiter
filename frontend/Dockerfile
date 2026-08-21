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
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
