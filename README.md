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
