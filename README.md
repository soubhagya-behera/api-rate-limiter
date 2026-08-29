# API Rate Limiter

A Redis-backed API rate limiting service demonstrating a fixed-window rate limiter
implemented with a Lua script, a Spring Boot backend (Java 17), and a React + Vite
dashboard.

- **Backend:** Spring Boot (WebMVC, Data Redis, Actuator, springdoc-openapi), Java 17
- **Frontend:** React 19 + Vite
- **Rate limiter:** Redis Lua script (`backend/src/main/resources/scripts/rate-limit.lua`),
  per-client-IP fixed window (default: 5 requests / 60 seconds)

## Run with Docker

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose v2)

### Start

From the project root:

```bash
docker compose up --build
```

This builds and starts all three services:

| Service  | Description                                    |
| -------- | ---------------------------------------------- |
| `redis`  | Redis with persistent volume (`redis-data`)    |
| `backend`| Spring Boot app (multi-stage Maven/JRE build)  |
| `frontend`| nginx serving the Vite production build       |

The backend waits for the Redis healthcheck (`redis-cli ping`) to pass before starting.
The frontend proxies `/api/*` to the backend over the internal Docker network.

### Access

| URL                                              | Purpose                     |
| ------------------------------------------------ | --------------------------- |
| http://localhost:5173                            | React dashboard             |
| http://localhost:8080                            | Backend API                 |
| http://localhost:8080/swagger-ui/index.html      | Swagger UI                  |
| http://localhost:8080/actuator/health            | Backend health endpoint     |

Quick check of the rate limiter:

```bash
curl.exe http://localhost:8080/api/test
```

Call it repeatedly: the first 5 requests within the 60-second window return
`HTTP 200`, further requests return `HTTP 429` with a `Retry-After` header.
`GET /api/rate-limit/status` is read-only and does not consume quota.

### Stop

```bash
docker compose down
```

Add `-v` only if you also want to delete the Redis data volume:

```bash
docker compose down -v   # WARNING: removes persisted rate-limit state
```

### Rebuild

After changing backend or frontend code:

```bash
docker compose up --build
```

## Run locally without Docker

Local development with Maven/npm remains fully supported.

### Backend

Requires Java 17 and a local Redis on `localhost:6379`.

```bash
cd backend
mvn spring-boot:run        # or: mvn test / mvn package
```

Redis connection and rate-limit settings can be overridden via environment
variables without code changes:
`SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`,
`RATE_LIMIT_MAX_REQUESTS`, `RATE_LIMIT_WINDOW_SECONDS`, `RATE_LIMIT_KEY_PREFIX`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Set `VITE_API_BASE_URL` (see `frontend/.env.example`) to point at another backend;
it defaults to `http://localhost:8080`.

## Deployment

The app deploys as three services on any Docker-compatible host (e.g. Render):

1. **Frontend** — nginx serving the static Vite build (`frontend/Dockerfile`)
2. **Backend** — Spring Boot API (`backend/Dockerfile`)
3. **Redis** — managed Redis instance

No code changes are required for deployment; everything is configured through
environment variables listed below.

### Backend environment variables

| Variable | Purpose | Local/Docker default |
| --- | --- | --- |
| `PORT` | HTTP port Spring Boot listens on | `8080` |
| `SPRING_DATA_REDIS_HOST` | Redis hostname | `localhost` (Docker Compose: service name `redis`) |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `RATE_LIMIT_MAX_REQUESTS` | Requests allowed per window | `5` |
| `RATE_LIMIT_WINDOW_SECONDS` | Fixed-window length in seconds | `60` |
| `RATE_LIMIT_KEY_PREFIX` | Redis key prefix | `rate-limit:ip` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins allowed to call `/api/**` | `http://localhost:5173` |

Notes:

- Set `CORS_ALLOWED_ORIGINS` to your production frontend origin (no trailing
  slash), e.g. `https://your-frontend.example.com`. Multiple origins are
  comma-separated. Avoid `*`: the API is read-only GET but the limiter is
  per-IP, so browsers should talk to their own origin's `/api` proxy where
  possible.
- If your managed Redis requires TLS, add `SPRING_DATA_REDIS_SSL_ENABLED=true`
  (native Spring property, no code/config-file change needed).

### Frontend environment variables

| Variable | When it applies | Effect if unset |
| --- | --- | --- |
| `VITE_API_BASE_URL` | **Build time** of the React bundle | Empty in the Docker build → relative URLs, so the dashboard calls its own origin and nginx proxies `/api/*` to the backend via `BACKEND_UPSTREAM` |
| `BACKEND_UPSTREAM` | **Runtime** of the nginx container | `backend:8080` (the Docker Compose backend service) |

- In production you normally do **not** need `VITE_API_BASE_URL`: keep the
  default relative URLs and set `BACKEND_UPSTREAM` to the backend's internal
  host:port so nginx proxies API calls over the platform network.
- Only set `VITE_API_BASE_URL` if the dashboard must call a *different public*
  API origin directly (then that origin must be listed in
  `CORS_ALLOWED_ORIGINS`). It must not contain secrets — everything baked into
  a frontend bundle is public.
- The frontend has no build-time dependency on `http://localhost:8080`; that
  value is only the fallback when developing outside Docker without an `.env`.

### Secrets

Never commit real credentials or production URLs. Configure all sensitive
values (Redis credentials, tokens, etc.) through your hosting provider's
environment-variable/secret settings, not in this repository.

### Render blueprint

A minimal [`render.yaml`](render.yaml) is provided for Render's Blueprint
deploy. During creation, Render prompts for the values marked `sync: false`
(Redis internal hostname, allowed CORS origin). You can also create the three
services by hand using the environment variables above; nothing in the repo is
Render-specific.
