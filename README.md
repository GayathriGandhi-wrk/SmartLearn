# AI-Powered Student Performance Prediction & Personalized Learning System

A full-stack intelligent platform that predicts student academic performance using machine
learning, detects weak areas and knowledge gaps, and generates personalized study plans,
recommendations, adaptive tests and explainable analyses.

## Tech Stack
                              
| Layer | Technology |
|-------|-----------|
| Frontend | HTML5, CSS3, Vanilla JavaScript, Bootstrap 5, Chart.js |
| API Gateway | Node.js + Express (proxy, validation, uploads, OTP email) |
| Backend | Spring Boot 3.2 (Java 17), Spring Security, JWT, Spring Data JPA |
| AI Service | Python Flask, scikit-learn, XGBoost, SHAP, LIME |
| Database | MySQL 8 |
| Docs/API | Springdoc OpenAPI (Swagger UI) |

## Architecture

```
Browser (frontend/)
    │  HTTP
    ▼
Node.js Gateway (:3000)  ── proxy ──►  Spring Boot API (:8080)  ──►  MySQL 8
    │  auth / validation / uploads          │ (JWT secured)
    ▼                                       ▼
  (SPA static files)               Python AI Service (:5000)
                                   ├─ /api/predict   (Random Forest, GBM, XGBoost ensemble)
                                   ├─ /api/explain   (SHAP + LIME)
                                   ├─ /api/recommend (content-based)
                                   ├─ /api/study-plans (schedules weak topics)
                                   └─ /api/chat      (Gemini/OpenAI/rule-based)
```

## Features

1. **User authentication & profile management**
   - Register / login / logout with JWT
   - OTP-based email verification & password reset
   - Student profile with photo upload, academic details, password change

2. **Performance prediction (AI/ML)**
   - Score, grade and failure-risk prediction using a 4-model ensemble
   - SHAP and LIME feature contribution explanations
   - Confidence scores per prediction

3. **Adaptive testing engine**
   - Start timed tests; questions adapt to ability in real-time
   - Per-subject scoring, knowledge-gap detection, auto-submit on expiry

4. **Question bank (3,000 MCQs seeded)**
   - 1,000 beginner / 1,000 intermediate / 1,000 advanced
   - Filtering, daily practice sets, bookmarking, attempt tracking

5. **Weak-subject & knowledge-gap detection**
   - Mastery levels per topic, priority-ranked weak subjects

6. **Personalized recommendations**
   - Videos, books, articles and practice resources targeted at weak topics

7. **AI study planner**
   - Weekly / daily / exam plans generated from detected gaps

8. **Analytics & reporting**
   - Weekly/monthly trends, subject radar, difficulty distribution
   - Generated performance reports (weekly/monthly/semester/prediction)

9. **Gamification**
   - XP points, levels, badges, streaks, leaderboard

10. **AI chatbot assistant** with learning-material support

11. **Admin dashboard**
    - Platform stats, user enable/disable, subject & question management

12. **Notifications** for tests, results and recommendations

## Repository Layout

```
AI-Student-Performance-System/
├── database/        MySQL schema, triggers/procedures, seed data
├── frontend/        HTML/CSS/JS SPA (served by Node gateway)
├── node-server/     Express API gateway (:3000)
├── spring-service/  Spring Boot REST backend (:8080)
├── ai-service/      Python Flask AI service (:5000)
├── docs/            Installation, API and deployment guides
├── scripts/         Question-bank generator
└── reports/ uploads/  Generated reports & uploaded files
```

## Getting Started

See [docs/INSTALLATION.md](docs/INSTALLATION.md) for the full setup guide.

Quick start (3 terminals + MySQL):

```bash
# 1. Python AI service (:5000)
cd ai-service
pip install -r requirements.txt
python run.py

# 2. Spring Boot backend (:8080)
cd spring-service
mvn spring-boot:run

# 3. Node.js gateway + frontend (:3000)
cd node-server
npm install
npm start
```

Open http://localhost:3000

**Demo accounts** (seeded):
- Admin: `admin@studentai.com` / `Admin@123`
- Student: `student1@studentai.com` / `Student@123`

## Documentation Index

- [Installation Guide](docs/INSTALLATION.md)
- [Architecture & Database Design](docs/ARCHITECTURE.md)
- [API Reference](docs/API.md)
- [Deployment Guide](docs/DEPLOYMENT.md)

## License

Academic mini-project.
