# API Reference

All API calls are made through the Node.js gateway on port **3000**.
Everything under `/api/*` is proxied to the Spring Boot backend.

Base URL: `http://localhost:3000/api`

Response envelope (all endpoints):

```json
{ "success": true, "message": "Success", "data": { ... } }
```

Auth: send `Authorization: Bearer <jwt>` for protected endpoints.

---

## Auth `/api/auth`

All auth endpoints are proxied by the Node gateway (port 3000) to Spring Boot (port 8080).

| Method | Path | Auth | Body | Description |
|--------|------|------|------|-------------|
| POST | `/register` | - | `fullName, email, phone, password, department, semester, cgpa?` | Register student |
| POST | `/login` | - | `email, password, rememberMe?` | Login, returns JWT + user fields |
| GET | `/me` | ✓ | - | Current authenticated user profile |
| POST | `/logout` | ✓ | - | Invalidate session |
| POST | `/send-otp` | - | `email, purpose` (REGISTRATION / PASSWORD_RESET) | Generate + email OTP |
| POST | `/verify-otp` | - | `email, otpCode, purpose` | Verify OTP |
| POST | `/forgot-password` | - | `email` | Send password-reset OTP |
| POST | `/reset-password` | - | `email, otpCode, newPassword` | Reset password |
| PUT | `/change-password` | ✓ | `oldPassword, newPassword` | Change current password |

### Login response shape

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "<jwt>",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "userId": 1,
    "email": "student1@studentai.com",
    "fullName": "Aarav Sharma",
    "role": "STUDENT",
    "studentId": 1,
    "verified": true,
    "message": "Login successful"
  }
}
```

### OTP response shape

`/send-otp` and `/forgot-password` return the OTP metadata. When the Node gateway
cannot deliver email (SMTP not configured) it echoes the code back as `devOtp`:

```json
{ "success": true, "message": "OTP sent to your email",
  "data": { "devOtp": "482913" } }
```

---

## Student `/api/students`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/me` | ✓ | Current student profile |
| PUT | `/me` | ✓ | Update profile (`fullName, phone, address, dateOfBirth, gender, department, semester, cgpa, enrollmentYear`) |
| PUT | `/me/academic` | ✓ | Update academic details (`department, semester, batch, cgpa, enrollmentYear`) |
| POST | `/me/photo` | ✓ | Multipart profile photo |
| PUT | `/me/password` | ✓ | `oldPassword, newPassword` |
| DELETE | `/me` | ✓ | Delete account |

---

## Subjects `/api/subjects`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` | ✓ | List subjects (with topic/question counts) |
| GET | `/semester/{semester}` | ✓ | Subjects for a semester |
| GET | `/{subjectId}/topics` | ✓ | Topics of a subject |
| GET | `/performance` | ✓ | Subject performance + recent attempts |
| POST | `/marks` | ✓ | Submit marks/attendance for prediction features |

---

## Questions `/api/questions`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` | ✓ | Question bank (filters: `difficulty, subjectId, topicId, page, size`) |
| GET | `/counts` | ✓ | Counts by difficulty |
| GET | `/{questionId}` | ✓ | Single question |
| POST | `/` | ✓ | Create question |
| PUT | `/{questionId}` | ✓ | Update question |
| DELETE | `/{questionId}` | ✓ | Delete question |
| GET | `/practice/daily` | ✓ | Daily practice set |
| POST | `/{questionId}/bookmark` | ✓ | Toggle bookmark |
| GET | `/bookmarks` | ✓ | Bookmarked questions |
| POST | `/{questionId}/attempt` | ✓ | `selectedAnswer, timeTakenSec` |

---

## Tests `/api/tests`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/start` | ✓ | `numberOfQuestions, durationMinutes, testType, title?` |
| GET | `/active` | ✓ | Current active test |
| POST | `/{testId}/submit` | ✓ | `{ answers: [{ testQuestionId, selectedAnswer, timeTakenSec }] }` |
| GET | `/{testId}/result` | ✓ | Detailed result (correct answer, explanation per question) |
| GET | `/history` | ✓ | Test history |

---

## AI `/api/ai`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/predict` | ✓ | Run ML prediction (grade, score, risk, confidence) |
| GET | `/prediction/latest` | ✓ | Latest stored prediction |
| GET | `/weak-subjects` | ✓ | Priority-ranked weak subjects |
| GET | `/knowledge-gaps` | ✓ | Topic-level knowledge gaps |
| GET | `/explain?method=shap\|lime` | ✓ | Feature contributions |
| GET | `/ping` | ✓ | AI service health |

---

## Study Planner `/api/study-plans`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/generate` | ✓ | Generate a plan. Body: `{ planType, totalHoursPerDay?, availableTime?, subjects: [{ subjectId, allTopics?, topics? }] }` |
| GET | `/` | ✓ | List plans |
| GET | `/{id}` | ✓ | Get a plan (owner only) |
| PUT | `/{id}` | ✓ | Regenerate a plan with new inputs |
| DELETE | `/{id}` | ✓ | Delete a plan (owner only) |
| PUT | `/sessions/{sessionId}/complete` | ✓ | Mark session completed |
| PUT | `/sessions/{sessionId}/skip` | ✓ | Mark session skipped |
| GET | `/{planId}/report` | ✓ | Plan report (owner only) |

---

## Recommendations `/api/recommendations`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` | ✓ | List recommendations |
| POST | `/generate` | ✓ | Generate from weak topics |
| POST | `/{id}/viewed` | ✓ | Mark viewed |
| GET | `/unread-count` | ✓ | Unread count |

---

## Analytics `/api/analytics`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/overview` | ✓ | Trends, subject accuracy, difficulty distribution, stats |
| GET | `/gamification` | ✓ | Level, XP, streak, badges, leaderboard |
| GET | `/leaderboard` | ✓ | Ranked leaderboard |

---

## Reports `/api/reports`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/generate` | ✓ | `reportType` (WEEKLY/MONTHLY/SEMESTER/PREDICTION), `title?` |
| GET | `/` | ✓ | List reports |
| GET | `/{id}/download?format=pdf\|docx` | ✓ | Download a report file (owner only) |

---

## Chatbot `/api/chatbot`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/chat` | ✓ | `{ message, provider? }` → `{ reply, intent, confidence }` |

---

## Notifications `/api/notifications`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` | ✓ | List notifications |
| POST | `/{id}/read` | ✓ | Mark read |
| POST | `/read-all` | ✓ | Mark all read |
| GET | `/unread-count` | ✓ | Unread count |

---

## Admin `/api/admin`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/dashboard` | ADMIN | Platform stats, risk distribution, recent tests |
| GET | `/users` | ADMIN | All users |
| PUT | `/users/{userId}/toggle` | ADMIN | Enable/disable user |
| POST | `/questions/import` | ADMIN | Bulk question import |

---

## Uploads `/api/upload`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/profile-photo` | ✓ | Multipart `file` (image) |
| POST | `/document` | ✓ | Multipart `file` |

---

## Python AI service (direct, port 5000)

Not exposed to the browser; called internally by Spring. For manual testing:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/health` | Service health |
| POST | `/api/predict` | `{ student_id, features: {...} }` |
| POST | `/api/explain/shap` | SHAP contributions |
| POST | `/api/explain/lime` | LIME contributions |
| POST | `/api/recommend` | `{ weak_subjects, knowledge_gaps }` |
| POST | `/api/planner` | `{ knowledge_gaps, total_hours }` |
| POST | `/api/chat` | `{ message, provider? }` |

### Prediction feature vector (12 features)

```
attendance_percentage, study_hours_per_week, assignments_submitted,
internal_marks, previous_exam_score, participation_score, sleep_hours,
stress_level, cgpa, class_attendance, social_media_hours, revision_hours
```

## Error codes

| Status | Meaning |
|--------|---------|
| 400 | Validation / business-rule error |
| 401 | Missing or invalid token |
| 403 | Insufficient role |
| 404 | Resource not found |
| 409 | Duplicate resource (e.g. email already registered) |
| 502 | Upstream (AI) service unreachable |
