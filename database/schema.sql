-- ============================================================================
-- AI-Powered Student Performance Prediction & Personalized Learning System
-- DATABASE SCHEMA - MySQL 8
-- File: database/schema.sql
-- ============================================================================

CREATE DATABASE IF NOT EXISTS student_performance_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci; 

USE student_performance_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS login_history;
DROP TABLE IF EXISTS otp;
DROP TABLE IF EXISTS chat_history;
DROP TABLE IF EXISTS rewards;
DROP TABLE IF EXISTS badges;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS practice_questions;
DROP TABLE IF EXISTS study_plan_sessions;
DROP TABLE IF EXISTS study_plans;
DROP TABLE IF EXISTS recommendations;
DROP TABLE IF EXISTS knowledge_gap;
DROP TABLE IF EXISTS weak_subjects;
DROP TABLE IF EXISTS predictions;
DROP TABLE IF EXISTS test_results;
DROP TABLE IF EXISTS question_attempts;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS topics;
DROP TABLE IF EXISTS test_questions;
DROP TABLE IF EXISTS tests;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 1. ROLES
-- ============================================================================
CREATE TABLE roles (
  role_id          BIGINT       NOT NULL AUTO_INCREMENT,
  role_name        VARCHAR(50)  NOT NULL,
  description      VARCHAR(255) NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (role_id),
  UNIQUE KEY uq_roles_name (role_name)
) ENGINE=InnoDB;

-- ============================================================================
-- 2. USERS
-- ============================================================================
CREATE TABLE users (
  user_id          BIGINT       NOT NULL AUTO_INCREMENT,
  role_id          BIGINT       NOT NULL,
  email            VARCHAR(150) NOT NULL,
  password_hash    VARCHAR(255) NOT NULL,
  phone            VARCHAR(20)  NULL,
  full_name        VARCHAR(120) NOT NULL,
  is_active        TINYINT(1)   NOT NULL DEFAULT 1,
  is_verified      TINYINT(1)   NOT NULL DEFAULT 0,
  profile_image    VARCHAR(255) NULL,
  remember_token  VARCHAR(255) NULL,
  last_login_at    DATETIME     NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uq_users_email (email),
  KEY idx_users_role (role_id),
  KEY idx_users_phone (phone),
  CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (role_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 3. STUDENTS
-- ============================================================================
CREATE TABLE students (
  student_id       BIGINT       NOT NULL AUTO_INCREMENT,
  user_id          BIGINT       NOT NULL,
  student_code     VARCHAR(30)  NOT NULL,
  department       VARCHAR(100) NOT NULL,
  semester         INT          NOT NULL DEFAULT 1,
  batch            VARCHAR(30)  NULL,
  cgpa             DECIMAL(4,2) NULL,
  enrollment_year  INT          NULL,
  date_of_birth    DATE         NULL,
  gender           ENUM('MALE','FEMALE','OTHER') NULL,
  address          VARCHAR(255) NULL,
  level            INT          NOT NULL DEFAULT 1,
  xp_points        INT          NOT NULL DEFAULT 0,
  streak_days      INT          NOT NULL DEFAULT 0,
  profile_photo    VARCHAR(255) NULL,
  PRIMARY KEY (student_id),
  UNIQUE KEY uq_students_code (student_code),
  UNIQUE KEY uq_students_user (user_id),
  KEY idx_students_department (department),
  KEY idx_students_semester (semester),
  KEY idx_students_cgpa (cgpa),
  CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 4. CATEGORIES
-- ============================================================================
CREATE TABLE categories (
  category_id      BIGINT       NOT NULL AUTO_INCREMENT,
  category_name    VARCHAR(150) NOT NULL,
  description      TEXT         NULL,
  is_active        TINYINT(1)   NOT NULL DEFAULT 1,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (category_id),
  UNIQUE KEY uq_categories_name (category_name),
  KEY idx_categories_created (created_at)
) ENGINE=InnoDB;

-- ============================================================================
-- 5. SUBJECTS
-- ============================================================================
CREATE TABLE subjects (
  subject_id       BIGINT       NOT NULL AUTO_INCREMENT,
  category_id      BIGINT       NOT NULL,
  subject_code     VARCHAR(30)  NOT NULL,
  subject_name     VARCHAR(150) NOT NULL,
  department       VARCHAR(100) NULL,
  credit_hours     DECIMAL(3,1) NOT NULL DEFAULT 3.0,
  description      TEXT         NULL,
  is_active        TINYINT(1)   NOT NULL DEFAULT 1,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (subject_id),
  UNIQUE KEY uq_subjects_code (subject_code),
  UNIQUE KEY uq_subjects_name (subject_name),
  KEY idx_subjects_department (department),
  KEY idx_subjects_category (category_id),
  KEY idx_subjects_created (created_at),
  CONSTRAINT fk_subjects_category FOREIGN KEY (category_id) REFERENCES categories (category_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 5A. TOPICS
-- Each subject groups 5 important topics. Difficulty is set at topic level so
-- the adaptive engine can ramp BEGINNER -> INTERMEDIATE -> ADVANCED per topic.
-- ============================================================================
CREATE TABLE topics (
  topic_id         BIGINT       NOT NULL AUTO_INCREMENT,
  subject_id       BIGINT       NOT NULL,
  topic_name       VARCHAR(150) NOT NULL,
  difficulty_level VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
  description      TEXT         NULL,
  estimated_hours  INT          NOT NULL DEFAULT 2,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (topic_id),
  UNIQUE KEY uq_topics_subject_name (subject_id, topic_name),
  KEY idx_topics_subject (subject_id),
  KEY idx_topics_difficulty (difficulty_level),
  KEY idx_topics_created (created_at),
  CONSTRAINT fk_topics_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 5B. QUESTIONS
-- The core question bank. Difficulty ENUM drives adaptive testing; marks follow
-- the rule BEGINNER=1, INTERMEDIATE=2, ADVANCED=3. question_type supports future
-- expansion (e.g. MULTIPLE_SELECT, TRUE_FALSE) without schema redesign.
-- ============================================================================
CREATE TABLE questions (
  question_id     BIGINT       NOT NULL AUTO_INCREMENT,
  topic_id        BIGINT       NOT NULL,
  question_text   TEXT         NOT NULL,
  option_a        VARCHAR(500) NOT NULL,
  option_b        VARCHAR(500) NOT NULL,
  option_c        VARCHAR(500) NOT NULL,
  option_d        VARCHAR(500) NOT NULL,
  correct_answer  ENUM('A','B','C','D') NOT NULL,
  explanation     TEXT         NULL,
  difficulty      ENUM('BEGINNER','INTERMEDIATE','ADVANCED') NOT NULL DEFAULT 'BEGINNER',
  marks           DECIMAL(4,2) NOT NULL DEFAULT 1.00,
  question_type   VARCHAR(30)  NOT NULL DEFAULT 'MCQ',
  is_active       TINYINT(1)   NOT NULL DEFAULT 1,
  source          ENUM('SEED','AI') NOT NULL DEFAULT 'SEED',
  content_hash    CHAR(64)     NULL,
  source_resource VARCHAR(255) NULL,
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (question_id),
  KEY idx_questions_topic (topic_id),
  KEY idx_questions_difficulty (difficulty),
  KEY idx_questions_topic_difficulty (topic_id, difficulty),
  KEY idx_questions_type (question_type),
  KEY idx_questions_created (created_at),
  KEY idx_questions_source (source),
  KEY idx_questions_hash (content_hash),
  CONSTRAINT fk_questions_topic FOREIGN KEY (topic_id) REFERENCES topics (topic_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_questions_marks CHECK (marks IN (1.00, 2.00, 3.00))
) ENGINE=InnoDB;

-- ============================================================================
-- 6. TESTS
-- ============================================================================
CREATE TABLE tests (
  test_id          BIGINT       NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  test_type        VARCHAR(30)  NOT NULL DEFAULT 'ADAPTIVE',
  title            VARCHAR(150) NOT NULL,
  total_questions  INT          NOT NULL,
  total_marks      DECIMAL(6,2) NOT NULL,
  duration_minutes INT          NOT NULL DEFAULT 15,
  status           ENUM('IN_PROGRESS','COMPLETED','EXPIRED','CANCELLED') NOT NULL DEFAULT 'IN_PROGRESS',
  started_at       DATETIME     NULL,
  submitted_at     DATETIME     NULL,
  score            DECIMAL(6,2) NULL,
  percentage       DECIMAL(5,2) NULL,
  correct_count    INT          NULL,
  incorrect_count  INT          NULL,
  skipped_count    INT          NULL,
  difficulty_level VARCHAR(20)  NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (test_id),
  KEY idx_tests_student (student_id),
  KEY idx_tests_status (status),
  CONSTRAINT fk_tests_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 7. TEST QUESTIONS
-- ============================================================================
CREATE TABLE test_questions (
  test_question_id BIGINT       NOT NULL AUTO_INCREMENT,
  test_id          BIGINT       NOT NULL,
  question_id      BIGINT       NOT NULL,
  order_no         INT          NOT NULL,
  selected_answer  ENUM('A','B','C','D') NULL,
  is_correct       TINYINT(1)   NULL,
  marks_obtained   DECIMAL(4,2) NULL,
  time_taken_sec   INT          NULL,
  is_answered      TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (test_question_id),
  KEY idx_tq_test (test_id),
  KEY idx_tq_question (question_id),
  CONSTRAINT fk_tq_test FOREIGN KEY (test_id) REFERENCES tests (test_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_tq_question FOREIGN KEY (question_id) REFERENCES questions (question_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 7A. QUESTION ATTEMPTS
-- Per-question analytics stream (student x question). Drives weak-topic
-- detection, adaptive difficulty selection and performance prediction.
-- ============================================================================
CREATE TABLE question_attempts (
  attempt_id       BIGINT       NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  question_id      BIGINT       NOT NULL,
  selected_answer  ENUM('A','B','C','D') NULL,
  is_correct       TINYINT(1)   NULL,
  time_taken_sec   INT          NULL,
  confidence_score DECIMAL(5,2) NULL,
  attempted_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (attempt_id),
  KEY idx_attempts_student (student_id),
  KEY idx_attempts_question (question_id),
  KEY idx_attempts_correct (is_correct),
  KEY idx_attempts_created (attempted_at),
  CONSTRAINT fk_attempts_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_attempts_question FOREIGN KEY (question_id) REFERENCES questions (question_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 8. TEST RESULTS
-- ============================================================================
CREATE TABLE test_results (
  result_id        BIGINT       NOT NULL AUTO_INCREMENT,
  test_id          BIGINT       NOT NULL,
  student_id       BIGINT       NOT NULL,
  total_marks      DECIMAL(6,2) NOT NULL,
  obtained_marks   DECIMAL(6,2) NOT NULL,
  percentage       DECIMAL(5,2) NOT NULL,
  grade            VARCHAR(5)   NOT NULL,
  rank_in_class    INT          NULL,
  accuracy_rate    DECIMAL(5,2) NULL,
  speed_score      DECIMAL(5,2) NULL,
  difficulty_avg   DECIMAL(5,2) NULL,
  summary_json     JSON         NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (result_id),
  UNIQUE KEY uq_results_test (test_id),
  KEY idx_results_student (student_id),
  CONSTRAINT fk_results_test FOREIGN KEY (test_id) REFERENCES tests (test_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_results_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 9. PREDICTIONS
-- ============================================================================
CREATE TABLE predictions (
  prediction_id    BIGINT       NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  model_name       VARCHAR(50)  NOT NULL,
  predicted_grade  VARCHAR(5)   NOT NULL,
  predicted_score  DECIMAL(5,2) NOT NULL,
  risk_level       ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  confidence_score DECIMAL(5,2) NOT NULL,
  feature_importance JSON       NULL,
  explanation      TEXT         NULL,
  input_features   JSON         NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (prediction_id),
  KEY idx_predictions_student (student_id),
  KEY idx_predictions_risk (risk_level),
  CONSTRAINT fk_predictions_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 10. WEAK SUBJECTS
-- ============================================================================
CREATE TABLE weak_subjects (
  weak_subject_id  BIGINT       NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  subject_id       BIGINT       NOT NULL,
  weakness_score   DECIMAL(5,2) NOT NULL,
  average_marks    DECIMAL(5,2) NULL,
  attendance       DECIMAL(5,2) NULL,
  assignment_score DECIMAL(5,2) NULL,
  internal_marks   DECIMAL(5,2) NULL,
  priority_rank    INT          NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (weak_subject_id),
  UNIQUE KEY uq_weak_student (student_id, subject_id),
  KEY idx_weak_student (student_id),
  KEY idx_weak_subject (subject_id),
  CONSTRAINT fk_weak_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_weak_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id)
) ENGINE=InnoDB;

-- ============================================================================
-- 11. KNOWLEDGE GAP
-- ============================================================================
CREATE TABLE knowledge_gap (
  gap_id           BIGINT       NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  topic_id         BIGINT       NOT NULL,
  mastery_level    DECIMAL(5,2) NOT NULL,
  gap_level        ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  attempted_questions INT       NOT NULL DEFAULT 0,
  correct_questions   INT       NOT NULL DEFAULT 0,
  recommended_hours   INT       NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (gap_id),
  UNIQUE KEY uq_gap_student (student_id, topic_id),
  KEY idx_gap_student (student_id),
  KEY idx_gap_topic (topic_id),
  CONSTRAINT fk_gap_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_gap_topic FOREIGN KEY (topic_id) REFERENCES topics (topic_id)
) ENGINE=InnoDB;

-- ============================================================================
-- 12. RECOMMENDATIONS
-- ============================================================================
CREATE TABLE recommendations (
  recommendation_id BIGINT      NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  type             VARCHAR(30)  NOT NULL,
  title            VARCHAR(200) NOT NULL,
  description      TEXT         NULL,
  resource_type    ENUM('VIDEO','ARTICLE','QUESTION','MATERIAL','REVISION','LINK') NULL,
  resource_url     VARCHAR(500) NULL,
  priority         INT          NOT NULL DEFAULT 3,
  is_viewed        TINYINT(1)   NOT NULL DEFAULT 0,
  reason           VARCHAR(255) NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (recommendation_id),
  KEY idx_reco_student (student_id),
  CONSTRAINT fk_reco_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 13. STUDY PLANS
-- ============================================================================
CREATE TABLE study_plans (
  plan_id          BIGINT       NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  title            VARCHAR(200) NOT NULL,
  subject_name     VARCHAR(150) NULL,
  topic_name       VARCHAR(150) NULL,
  plan_type        ENUM('DAILY','WEEKLY','MONTHLY','EXAM') NOT NULL DEFAULT 'DAILY',
  start_date       DATE         NOT NULL,
  end_date         DATE         NOT NULL,
  total_hours      INT          NOT NULL DEFAULT 4,
  status           ENUM('ACTIVE','COMPLETED','PAUSED') NOT NULL DEFAULT 'ACTIVE',
  is_ai_generated  TINYINT(1)   NOT NULL DEFAULT 1,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (plan_id),
  KEY idx_plans_student (student_id),
  KEY idx_plans_type (plan_type),
  CONSTRAINT fk_plans_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 13b. STUDY PLAN SESSIONS
-- ============================================================================
CREATE TABLE study_plan_sessions (
  session_id       BIGINT       NOT NULL AUTO_INCREMENT,
  plan_id          BIGINT       NOT NULL,
  subject_id       BIGINT       NULL,
  topic_id         BIGINT       NULL,
  session_date     DATE         NOT NULL,
  day              VARCHAR(20)  NOT NULL,
  start_time       TIME         NOT NULL,
  end_time         TIME         NOT NULL,
  duration_minutes INT          NOT NULL,
  difficulty       VARCHAR(20)  NULL,
  priority         VARCHAR(20)  NULL,
  activity         VARCHAR(200) NULL,
  status           ENUM('PENDING','COMPLETED','SKIPPED') NOT NULL DEFAULT 'PENDING',
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (session_id),
  KEY idx_sessions_plan (plan_id),
  KEY idx_sessions_topic (topic_id),
  CONSTRAINT fk_sessions_plan FOREIGN KEY (plan_id) REFERENCES study_plans (plan_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_sessions_subject FOREIGN KEY (subject_id) REFERENCES subjects (subject_id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_sessions_topic FOREIGN KEY (topic_id) REFERENCES topics (topic_id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 15. PRACTICE QUESTIONS
-- ============================================================================
CREATE TABLE practice_questions (
  practice_id      BIGINT       NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  question_id      BIGINT       NOT NULL,
  is_bookmarked    TINYINT(1)   NOT NULL DEFAULT 0,
  is_solved        TINYINT(1)   NOT NULL DEFAULT 0,
  last_practiced_at DATETIME    NULL,
  attempt_count    INT          NOT NULL DEFAULT 0,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (practice_id),
  UNIQUE KEY uq_practice_student_question (student_id, question_id),
  KEY idx_practice_student (student_id),
  KEY idx_practice_question (question_id),
  KEY idx_practice_bookmarked (is_bookmarked),
  CONSTRAINT fk_practice_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_practice_question FOREIGN KEY (question_id) REFERENCES questions (question_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 16. NOTIFICATIONS
-- ============================================================================
CREATE TABLE notifications (
  notification_id  BIGINT       NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  title            VARCHAR(150) NOT NULL,
  message          TEXT         NOT NULL,
  type             VARCHAR(30)  NOT NULL DEFAULT 'INFO',
  is_read          TINYINT(1)   NOT NULL DEFAULT 0,
  link_url         VARCHAR(500) NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (notification_id),
  KEY idx_notifications_student (student_id),
  KEY idx_notifications_read (is_read),
  CONSTRAINT fk_notifications_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 17. BADGES
-- ============================================================================
CREATE TABLE badges (
  badge_id         BIGINT       NOT NULL AUTO_INCREMENT,
  badge_name       VARCHAR(80)  NOT NULL,
  badge_icon       VARCHAR(50)  NULL,
  badge_description VARCHAR(255) NULL,
  threshold        INT          NOT NULL DEFAULT 0,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (badge_id),
  UNIQUE KEY uq_badges_name (badge_name)
) ENGINE=InnoDB;

-- ============================================================================
-- 18. REWARDS (student-badge mapping + xp ledger)
-- ============================================================================
CREATE TABLE rewards (
  reward_id        BIGINT       NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  badge_id         BIGINT       NULL,
  reward_type      VARCHAR(30)  NOT NULL DEFAULT 'BADGE',
  xp_earned        INT          NOT NULL DEFAULT 0,
  description      VARCHAR(255) NULL,
  earned_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (reward_id),
  KEY idx_rewards_student (student_id),
  KEY idx_rewards_badge (badge_id),
  UNIQUE KEY uq_rewards_student_badge (student_id, badge_id),
  CONSTRAINT fk_rewards_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_rewards_badge FOREIGN KEY (badge_id) REFERENCES badges (badge_id)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 19. CHAT HISTORY
-- ============================================================================
CREATE TABLE chat_history (
  chat_id          BIGINT       NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  user_message     TEXT         NOT NULL,
  bot_response     TEXT         NOT NULL,
  intent           VARCHAR(50)  NULL,
  confidence       DECIMAL(5,2) NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (chat_id),
  KEY idx_chat_student (student_id),
  KEY idx_chat_created (created_at),
  CONSTRAINT fk_chat_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 20. OTP
-- ============================================================================
CREATE TABLE otp (
  otp_id           BIGINT       NOT NULL AUTO_INCREMENT,
  email            VARCHAR(150) NOT NULL,
  otp_code         VARCHAR(8)   NOT NULL,
  purpose          VARCHAR(30)  NOT NULL DEFAULT 'EMAIL_VERIFICATION',
  is_used          TINYINT(1)   NOT NULL DEFAULT 0,
  expires_at       DATETIME     NOT NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (otp_id),
  KEY idx_otp_email (email),
  KEY idx_otp_expiry (expires_at),
  KEY idx_otp_code (otp_code)
) ENGINE=InnoDB;

-- ============================================================================
-- 21. LOGIN HISTORY
-- ============================================================================
CREATE TABLE login_history (
  login_id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id          BIGINT       NOT NULL,
  ip_address       VARCHAR(45)  NULL,
  user_agent       VARCHAR(255) NULL,
  device           VARCHAR(100) NULL,
  status           ENUM('SUCCESS','FAILED') NOT NULL DEFAULT 'SUCCESS',
  login_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (login_id),
  KEY idx_login_user (user_id),
  KEY idx_login_time (login_at),
  CONSTRAINT fk_login_user FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 22. ANALYTICS (aggregated event stream)
-- ============================================================================
CREATE TABLE analytics (
  analytics_id     BIGINT       NOT NULL AUTO_INCREMENT,
  student_id       BIGINT       NOT NULL,
  event_type       VARCHAR(50)  NOT NULL,
  event_value      DECIMAL(10,2) NULL,
  event_meta       JSON         NULL,
  event_date       DATE         NOT NULL,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (analytics_id),
  KEY idx_analytics_student (student_id),
  KEY idx_analytics_type (event_type),
  KEY idx_analytics_date (event_date),
  CONSTRAINT fk_analytics_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================================
-- 23. TOPIC RESOURCE VIEWS
-- Records which tutorial/doc/video a student opened, and how far they got.
-- resource_key is a stable hash of (student, topic, url) so re-opening the same
-- link updates the existing row (last_viewed_at) instead of creating duplicates.
-- That row is the input for "I have learned up to here" question generation.
--
-- watched_seconds / duration_seconds are the playback position of a video, so
-- only the part of it the student actually watched is turned into questions.
-- content_text caches what the resource said (transcript or page text) up to
-- that point, and content_seconds records which position that text covers, so a
-- longer watch is re-read rather than quietly reusing a short extract.
-- ============================================================================
CREATE TABLE resource_views (
  view_id           BIGINT        NOT NULL AUTO_INCREMENT,
  student_id        BIGINT        NOT NULL,
  topic_id          BIGINT        NOT NULL,
  resource_type     ENUM('VIDEO','DOC','SEARCH') NOT NULL DEFAULT 'VIDEO',
  resource_title    VARCHAR(255)  NOT NULL,
  resource_url      VARCHAR(500)  NOT NULL,
  resource_key      CHAR(64)      NOT NULL,
  progress_label    VARCHAR(120)  NULL,
  watched_seconds   INT           NULL,
  duration_seconds  INT           NULL,
  content_text      MEDIUMTEXT    NULL,
  content_source    VARCHAR(20)   NULL,
  content_seconds   INT           NULL,
  first_viewed_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_viewed_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  view_count        INT           NOT NULL DEFAULT 1,
  PRIMARY KEY (view_id),
  UNIQUE KEY uq_resource_view_key (resource_key),
  KEY idx_resource_view_student (student_id),
  KEY idx_resource_view_topic (topic_id),
  KEY idx_resource_view_last (last_viewed_at),
  CONSTRAINT fk_resource_view_student FOREIGN KEY (student_id) REFERENCES students (student_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_resource_view_topic FOREIGN KEY (topic_id) REFERENCES topics (topic_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

