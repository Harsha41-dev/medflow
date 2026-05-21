# MedFlow Docker Setup

This Docker setup is for reproducible local development and demo use. It runs the full MedFlow stack with PostgreSQL, the Spring Boot backend, the React frontend, and the educational ML symptom-pattern service.

## Architecture

```text
Browser
  -> React frontend container on http://localhost:3000
  -> Spring Boot backend on http://localhost:8080/api/v1
  -> ML service through Spring Boot on http://localhost:8080/api/v1/ml/symptom-pattern
Spring Boot backend
  -> FastAPI ML service on ml-service:8000
  -> PostgreSQL container on postgres:5432
```

Inside Docker Compose, the backend connects to PostgreSQL using the service name `postgres`. It must not use `localhost`, because `localhost` from inside the backend container points to the backend container itself.

The browser calls the backend through `http://localhost:8080/api/v1`. It cannot normally call `http://backend:8080/api/v1`, because `backend` is only a Docker-internal hostname.

The backend calls the ML service through `http://ml-service:8000`. The React frontend does not call the Python service directly.

## Environment

Create a local `.env` from `.env.example` and change secrets for your machine:

```text
POSTGRES_DB=medflow
POSTGRES_USER=medflow_user
POSTGRES_PASSWORD=change-me
JWT_SECRET=replace-this-with-a-long-local-docker-secret-at-least-32-chars
JWT_EXPIRATION_MS=3600000
CORS_ALLOWED_ORIGINS=http://localhost:3000
VITE_API_BASE_URL=http://localhost:8080/api/v1
ML_SERVICE_BASE_URL=http://localhost:8000
```

Do not commit real secrets. The root `.gitignore` already ignores `.env`.

## Run

```powershell
docker compose up --build
```

Frontend:

```text
http://localhost:3000
```

Backend Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Database

The PostgreSQL service uses a named volume called `postgres_data`, so data survives normal container restarts.

Flyway remains the schema owner. When the backend starts, it connects to PostgreSQL, runs migrations `V1` through `V7`, and then Hibernate validates the schema. The PostgreSQL partial unique index for scheduled appointments is preserved by the existing Flyway migration.

## ML Service

The ML service is a FastAPI application using scikit-learn. During Docker image build it generates synthetic data and trains a small Logistic Regression model. The model is loaded once when the FastAPI app starts.

This service is educational only. It returns broad symptom-pattern labels and does not diagnose disease or recommend treatment.

## Build Choice

The backend Docker image uses:

```text
mvn clean package -DskipTests
```

Tests should be run separately with `mvn test` before building or in CI. This keeps the local Docker demo build faster while still making the test step explicit.

## Useful Commands

```powershell
docker compose ps
docker compose logs backend
docker compose logs ml-service
docker compose logs postgres
docker compose down
docker compose down -v
```

Use `docker compose down -v` only when you want to remove the PostgreSQL data volume.
