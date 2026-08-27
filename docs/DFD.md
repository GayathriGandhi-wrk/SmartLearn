# Data Flow Diagrams (DFD)

System: **AI-Powered Student Performance Prediction & Personalized Learning System**

The diagrams below use Mermaid and render on GitHub / any Mermaid-capable viewer.
If the diagrams appear as code, open this file in GitHub, VS Code (with Markdown
Preview), or https://mermaid.live.

---

## DFD Level 0 — Context Diagram

The entire system is a single process. External entities interact with it through
login, profile data, test answers, chat messages, content management and OTP email.

```mermaid
flowchart LR
    S[<b>STUDENT</b><br/>external entity] <-->|login/register, profile,<br/>test answers, chat,<br/>practice attempts| SYS
    A[<b>ADMIN</b><br/>external entity] <-->|manage users, subjects,<br/>questions, reports| SYS
    EM[<b>EMAIL SERVICE</b><br/>external entity] <-->|OTP verify account,<br/>reset password| SYS

    subgraph SYS[<b>AI-POWERED STUDENT PERFORMANCE</b><br/>Prediction · Adaptive Tests · Recommendations ·<br/>Study Plans · Analytics · Chatbot · Gamification]
    end

    S ---|prediction + SHAP/LIME explanation,<br/>questions, recommendations,<br/>study plans, reports, notifications| SYS
```

**External entities**

| Entity | Description |
|--------|-------------|
| Student | Registers/logs in, supplies profile & academic data, takes adaptive tests, views predictions, reports and study plans, chats with the AI. |
| Admin   | Manages platform: users, subjects, question bank; views platform stats and generated reports. |
| Email Service | Receives OTP requests to verify accounts / reset passwords and delivers codes. |

**Main data flows**

| Direction | Flow |
|-----------|------|
| Student → System | credentials, profile updates, test answers, chat messages, practice submissions |
| System → Student | predictions + SHAP/LIME explanations, adaptive questions, recommendations, study plans, reports, notifications |
| Admin → System | admin credentials, content-management data, user moderation |
| System → Email | OTP verification / password-reset codes |

---

## DFD Level 1 — System Decomposition

The system decomposes into nine processes. All persistent data lives in **MySQL 8**
(D1–D14). The **Python AI service** owns prediction, explainability, recommendations,
study-planning and the chatbot (processes P3, P5, P6, P9).

```mermaid
flowchart TB
    S[<b>STUDENT</b>] <-->|credentials, profile,<br/>answers, chat| P2
    A[<b>ADMIN</b>] <-->|login, user/subject/<br/>question mgmt| P2
    P2[P2 · Auth, Profile<br/>& User Management<br/>Node + Spring] -->|JWT / OTP| EM[EMAIL SERVICE]
    P2 -->|user data| D1[(D1 users / otp)]

    S <-->|submit answers,<br/>view result| P4
    P4[P4 · Adaptive Testing<br/>Engine · Spring] --> D2[(D2 tests)]
    P4 --> D3[(D3 questions)]
    P4 --> D4[(D4 attempts)]
    P4 --> D5[(D5 test_results)]

    P4 --> P3[P3 · Prediction &<br/>Explainability · AI]
    P3 --> D6[(D6 predictions)]
    P3 <-->|feature vector,<br/>SHAP/LIME| AIS[<b>PYTHON AI</b><br/>ModelManager / Explainer]
    P3 -->|score, grade, risk,<br/>confidence| S

    P5[P5 · Weak Subject &<br/>Knowledge-Gap Detection · Spring/AI]
    P5 --> D7[(D7 weak_subjects)]
    P5 --> D8[(D8 knowledge_gap)]

    P9[P9 · Study Plans &<br/>Recommendations · AI]
    P9 --> D9[(D9 recommendations)]
    P9 --> D10[(D10 study_plans)]

    P6[P6 · AI Chatbot<br/>Gemini/OpenAI/rule]
    P6 --> D13[(D13 chat_history)]

    P7[P7 · Analytics, Reports<br/>& Question Bank · Spring]
    P7 --> D11[(D11 reports)]
    P7 --> D12[(D12 notifications)]
    P7 --> D3

    P8[P8 · Gamification<br/>XP, badges, leaderboard]
    P8 --> D14[(D14 xp / badges / streaks)]

    P7 -->|trends, PDF reports| S
    P8 -->|levels, badges, leaderboard| S
    P9 -->|resources, schedule| S
    P6 -->|answers| S
    P5 -->|priority weak subjects| S
```

**Processes**

| ID | Process | Layer |
|----|---------|-------|
| P2 | Auth, Profile & User Management — login/register, JWT, OTP email, profile uploads | Node gateway + Spring Boot |
| P3 | Performance Prediction & Explainability — 4-model ensemble, SHAP/LIME, confidence | Python AI |
| P4 | Adaptive Testing Engine — adaptive question selection, grading, auto-submit on expiry | Spring Boot |
| P5 | Weak Subject & Knowledge-Gap Detection — mastery per topic, priority ranking | Spring Boot + stored procs + AI |
| P6 | AI Chatbot — Gemini → OpenAI → rule-based fallback | Python AI |
| P7 | Analytics, Reports & Question Bank — trends, PDF reports, 3,000-MCQ bank | Spring Boot |
| P8 | Gamification + Notifications — XP, levels, badges, streaks, leaderboard | Spring Boot |
| P9 | Study Plans & Recommendations — content-based resources, day-by-day planner | Python AI |

**Data stores (MySQL 8, 20 tables)**

| Store | Content |
|-------|---------|
| D1  | users, roles, otp, login_history |
| D2  | tests (adaptive/practice/chapter/final) |
| D3  | questions (3,000 MCQs seeded) |
| D4  | test_questions, practice_questions |
| D5  | test_results (correct/incorrect/skipped, grade) |
| D6  | predictions (model, score, risk, confidence, feature importance JSON) |
| D7  | weak_subjects (weakness score, priority rank) |
| D8  | knowledge_gap (mastery, gap level, recommended hours) |
| D9  | recommendations (resource, type, reason, viewed) |
| D10 | study_plans + study_plan_tasks |
| D11 | reports (weekly/monthly/semester/prediction, PDF url) |
| D12 | notifications |
| D13 | chat_history |
| D14 | badges, student_badges, rewards (XP log) |

---

## DFD Level 2 — Adaptive Test Flow (example)

Detail of the end-to-end adaptive-testing path (P4 → P3) for the Student.

```mermaid
sequenceDiagram
    participant S as Student (Browser)
    participant N as Node Gateway :3000
    participant B as Spring Boot :8080 (P4)
    participant M as MySQL (D2–D5, D14)
    participant A as Python AI :5000 (P3)

    S->>N: POST /api/tests/start (JWT)
    N->>N: verify JWT, validate body
    N->>B: forward /api/tests/start
    B->>B: pick adaptive questions from past attempts
    B->>M: create Test + TestQuestion rows
    B-->>N: questions (no correct answers)
    N-->>S: questions + timer

    S->>N: POST /api/tests/{id}/submit (answers)
    N->>B: forward submit
    B->>B: grade answers, award XP/badges
    B->>M: save QuestionAttempt + TestResult + Prediction
    B->>A: POST /api/predict (feature vector)
    A->>A: ensemble (RF, GBM, XGB, DT) + confidence
    A-->>B: score, grade, risk, confidence
    B->>A: POST /api/explain
    A-->>B: SHAP + LIME contributions
    B-->>N: result + explanations
    N-->>S: result, weak subjects, study plan
```
