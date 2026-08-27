# Architecture & Database Design

## System Overview

```
┌───────────────────────────────┐
│  Browser (Vanilla JS SPA)     │
│  frontend/                    │
│  - pages/auth, pages/student, │
│    pages/admin, js/, css/     │
└──────────────┬────────────────┘
               │ HTTP (same origin)
┌──────────────▼────────────────┐
│  Node.js Express Gateway :3000│
│  - JWT verify, rate limiting  │
│  - validation (express-       │
│    validator)                 │
│  - multer uploads             │
│  - nodemailer OTP             │
│  - proxy to Spring Boot       │
└──────────────┬────────────────┘
               │ /api/* forward
┌──────────────▼────────────────┐
│  Spring Boot REST API :8080   │
│  controllers/services/JPA     │
│  Spring Security + JWT        │
│  Cache, Scheduling, OpenAPI   │
└───────┬────────────────┬──────┘
        │ JPA            │ REST (AiServiceClient, 30s timeout)
┌───────▼────────┐  ┌────▼────────────────┐
│  MySQL 8       │  │  Python Flask :5000 │
│  24 tables     │  │  - predict/explain/ │
│  triggers/proc │  │    recommend/chat   │
└────────────────┘  │    chat             │
                    └─────────────────────┘
```

## Request flow example: adaptive test

1. Student clicks **Start Test** on `/pages/student/adaptive-test.html`.
2. Browser `POST /api/tests/start` to Node gateway.
3. Gateway verifies JWT, validates body, forwards to Spring `POST /api/tests/start`.
4. Spring picks questions (adaptive difficulty based on past attempts), creates a `Test`,
   saves `TestQuestion` rows, returns them (without correct answers).
5. Browser submits answers: `POST /api/tests/{id}/submit`.
6. Spring grades each answer, saves `QuestionAttempt` + `TestResult`, awards XP/badges,
   computes weak subjects/knowledge gaps, and persists a `Prediction`.
7. Browser shows result; `prediction.html` / `detailed-analysis.html` then call
   Spring `/api/ai/predict` + `/api/ai/explain`, which proxy to the Python service.

## Database Design

20 tables (see `database/schema.sql` for full DDL). Key entities:

### Identity & Access
- `roles` (ADMIN, STUDENT, PARENT, TEACHER)
- `users` (credentials, phone, verified, active, profile image)
- `otp` (code, purpose, expiry)
- `login_history`

### Academic
- `students` (one-to-one with `users`; code, dept, semester, batch, cgpa, level, xp, streak)

### Assessment
- `tests` (type: ADAPTIVE/PRACTICE/CHAPTER/FINAL; status; duration; deadline)
- `test_questions` (order, marks, time taken)
- `test_results` (correct/incorrect/skipped, marks, percentage, grade)
- `practice_questions` (daily practice set, bookmark, solved flags)

### AI & Personalization
- `predictions` (model, grade, score, risk, confidence, feature importance JSON)
- `weak_subjects` (weakness score, marks, attendance, priority rank)
- `knowledge_gap` (mastery, gap level, recommended hours)
- `recommendations` (type, resource, priority, reason, viewed)
- `study_plans` + `study_plan_tasks`

### Engagement
- `notifications`
- `badges` + `student_badges` (many-to-many, earned flag)
- `rewards` (XP log)
- `chat_history`
- `reports` (type, data JSON, pdf url)
- `analytics` (aggregate snapshots)

## Stored procedures (`database/triggers_procedures.sql`)

- `sp_calculate_leaderboard()` — ranks students by XP
- `sp_detect_weak_subjects(studentId)` — priority-ranked weak subjects
- `sp_detect_knowledge_gaps(studentId)` — mastery per topic
- `sp_generate_weekly_report(studentId)` — weekly report data
- `sp_update_student_level(studentId)` — level from XP
- Plus triggers to maintain streak, XP, badges and test auto-expiry.

## Spring Boot layering

```
controller → service (interface + impl) → repository → entity
                └→ DTO records (never expose entities directly)
                └→ util/SecurityUtils (current user from SecurityContext)
```

- **Security**: stateless JWT filter; `@PreAuthorize`-style role checks via custom
  `SecurityUtils.requireRole(...)`; `/auth/**` public.
- **Caching**: `@Cacheable` on question/topic lookups.
- **Scheduling**: `ScheduledTasks.expireTimedOutTests()` runs every 60 s.
- **AiServiceClient**: calls Python with `WebClient`, 30 s timeout, graceful fallback
  so the system stays up if the AI service is down.
- **MapStruct**: DTO mapping (JPA entity ↔ DTO).

## AI Service design (`ai-service/app/`)

- `preprocessing.py` — `DataPreprocessor`; canonical 12-feature vector + `map_score_to_grade`,
  `map_score_to_risk`.
- `models.py` — `ModelManager` trains Random Forest, Gradient Boosting, XGBoost and
  Decision Tree on a synthetic dataset (2,000 rows) generated at first boot; ensemble
  prediction + confidence = agreement.
- `explainability.py` — `Explainer` returns SHAP and LIME contributions per feature.
- `recommendation.py` — `RecommendationEngine` maps weak topics to curated resources;
  `StudyPlanner` builds a day-by-day schedule from gaps.
- `chatbot.py` — `ChatbotService` tries Gemini, then OpenAI, then a rule-based fallback.

## Security notes

- Passwords BCrypt-hashed (strength 10).
- JWT secret base64-decoded; 24 h validity, 7 days with "remember me".
- Node gateway re-verifies JWTs before proxying; Spring independently validates them too.
- Uploads restricted by MIME type and size (multer).
- No secrets are committed; use `.env` / `application.yml` local overrides.
