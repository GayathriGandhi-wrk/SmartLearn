# Deployment Guide

## Option A: Run everything locally (dev)

See [INSTALLATION.md](INSTALLATION.md). TL;DR:

```
ai-service   : python run.py          → :5000
spring-service: mvn spring-boot:run   → :8080
node-server  : npm start              → :3000 (frontend + gateway)
```

## Option B: Docker (recommended for production)

Create `docker-compose.yml` at the project root:

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: student_performance_db
      MYSQL_ROOT_PASSWORD: MySQL@123
    ports: ["3306:3306"]
    volumes:
      - ./database/schema.sql:/docker-entrypoint-initdb.d/01_schema.sql:ro
      - ./database/triggers_procedures.sql:/docker-entrypoint-initdb.d/02_triggers.sql:ro
      - ./database/seed_base.sql:/docker-entrypoint-initdb.d/03_seed.sql:ro
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 10

  ai-service:
    build: ./ai-service
    ports: ["5000:5000"]
    environment:
      GEMINI_API_KEY: ${GEMINI_API_KEY:-}
      OPENAI_API_KEY: ${OPENAI_API_KEY:-}
    depends_on:
      - mysql

  backend:
    build: ./spring-service
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/student_performance?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: change_me
      AI_SERVICE_BASE_URL: http://ai-service:5000
    depends_on:
      mysql:
        condition: service_healthy
      ai-service:
        condition: service_started

  gateway:
    build: ./node-server
    ports: ["3000:3000"]
    environment:
      PORT: 3000
      SPRING_API_BASE: http://backend:8080/api
      JWT_SECRET: ${JWT_SECRET:-change_me_in_production}
    depends_on:
      - backend

volumes:
  mysql-data:
```

Each service needs a `Dockerfile`:

**ai-service/Dockerfile**

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 5000
CMD ["python", "run.py"]
```

**spring-service/Dockerfile**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**node-server/Dockerfile**

```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev
COPY . .
EXPOSE 3000
CMD ["node", "server.js"]
```

Run:

```bash
docker compose up -d --build
docker compose ps
```

The MySQL entrypoint scripts load schema + seeds automatically on first boot.

## Option C: Production hardening

1. **Reverse proxy** (nginx) in front of the gateway:

```nginx
server {
  listen 80;
  server_name Smart Learn.example.com;
  location / {
    proxy_pass http://localhost:3000;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }
}
```

2. **HTTPS** — obtain a certificate (Let's Encrypt / certbot) and forward 80→443.

3. **Secrets** — change `JWT_SECRET` and DB password; never commit `.env`.
   Generate a strong JWT secret:

```bash
node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"
```

4. **MySQL** — run as a dedicated user, not root; enable `require_secure_transport`.

5. **Backups** — nightly dump:

```bash
mysqldump -u root -p student_performance | gzip > backups/$(date +%F).sql.gz
```

6. **Process management** — use `pm2` for the Node gateway and `systemd` units
   (or the Docker setup above) for Spring and Python.

7. **AI service scale-out** — the AI service is stateless (models train at boot).
   Run multiple replicas behind a load balancer if needed.

## Monitoring

- Spring Actuator: add `spring-boot-starter-actuator` and expose `/actuator/health`.
- Swagger UI: http://host:8080/swagger-ui.html
- Node gateway: structured JSON logs via `node-server/utils/logger.js`.

## Environment variables summary

| Variable | Default | Used by |
|----------|---------|---------|
| `PORT` | 3000 | Node gateway |
| `SPRING_API_BASE` | http://localhost:8080/api | Node gateway |
| `JWT_SECRET` | dev secret | Node gateway (verify) |
| `SPRING_DATASOURCE_URL` | local MySQL | Spring Boot |
| `SPRING_DATASOURCE_USERNAME/PASSWORD` | root/root | Spring Boot |
| `AI_SERVICE_BASE_URL` | http://localhost:5000 | Spring Boot |
| `GEMINI_API_KEY` | - | AI chatbot |
| `OPENAI_API_KEY` | - | AI chatbot |
