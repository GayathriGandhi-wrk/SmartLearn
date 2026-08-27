# Installation Guide

This guide walks through setting up the full system on Windows (or any OS with the listed
prerequisites).

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java JDK | 17+ | Spring Boot backend |
| Maven | 3.8+ | Spring Boot build |
| Node.js | 18+ | Express gateway |
| Python | 3.10+ | Flask AI service |
| MySQL | 8.x | Database |
| Git | any | (optional) version control |

## Step 1: Database setup

1. Start MySQL and log in as root:

```bash
mysql -u root -p
```

2. Create the database and load the SQL files **in this order**:

```sql
CREATE DATABASE IF NOT EXISTS student_performance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE student_performance_db;
```

```bash
mysql -u root -p student_performance_db < database/schema.sql
mysql -u root -p student_performance_db < database/triggers_procedures.sql
mysql -u root -p student_performance_db < database/seed_base.sql
```

3. Verify:

```sql
USE student_performance_db;
SELECT * FROM users;              -- expect admin + student accounts
```

## Step 2: Python AI service (:5000)

```bash
cd ai-service
python -m venv venv
venv\Scripts\activate            # Windows  (use `source venv/bin/activate` on Linux/Mac)
pip install -r requirements.txt
```

Configure optional AI providers in a `.env` file (copy from `.env.example`):

```env
GEMINI_API_KEY=your_key        # optional
OPENAI_API_KEY=your_key        # optional
```

Without API keys the chatbot falls back to a built-in rule-based engine, so the service
still works fully.

Start it:

```bash
python run.py
```

Smoke test:

```bash
curl http://localhost:5000/api/health
# {"status":"ok",...}
```

## Step 3: Spring Boot backend (:8080)

1. Configure the database connection in `spring-service/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/student_performance?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: root
    password: root
```

2. (Optional) Configure SMTP for real OTP emails in the same file under
   `spring.mail.*`. If left unset, OTP emails are simulated and the code is returned
   in the API response.

3. Build & run:

```bash
cd spring-service
mvn clean package -DskipTests
mvn spring-boot:run
```

Spring Boot auto-creates missing tables via Hibernate `ddl-auto: update`, so the schema
does not need to be created manually if you prefer.

Verify:

```bash
curl http://localhost:8080/api/ai/ping
```

Swagger UI: http://localhost:8080/swagger-ui.html

## Step 4: Node.js gateway + frontend (:3000)

```bash
cd node-server
npm install
npm start
```

Verify: open http://localhost:3000 — you should see the landing page.

## Step 5: Run the tests

```bash
# Spring Boot
cd spring-service
mvn test

# AI service
cd ai-service
pytest

# Node gateway
cd node-server
npm test
```

## Logging in

- Admin: `admin@studentai.com` / `Admin@123`
- Student: `student1@studentai.com` / `Student@123` (up to `student20@studentai.com`)

All seeded passwords are `Admin@123` (admin) and `Student@123` (students).

## Common issues

| Problem | Fix |
|---------|-----|
| `Public Key Retrieval is not allowed` | add `allowPublicKeyRetrieval=true` to JDBC URL |
| Port 8080/3000/5000 already in use | change the port in `application.yml`, `node-server/config/index.js` or `ai-service/app/__init__.py` |
| CORS errors | frontend calls are proxied through Node (same origin). If calling Spring directly, check `app.cors.allowed-origins`. |
| MySQL root password | update in `application.yml`; a `.env` override is not supported for Spring |
