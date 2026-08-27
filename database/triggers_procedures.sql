-- ============================================================================
-- AI-Powered Student Performance Prediction & Personalized Learning System
-- TRIGGERS & STORED PROCEDURES - MySQL 8
-- File: database/triggers_procedures.sql
-- ============================================================================

USE student_performance_db;

DELIMITER $$

-- ============================================================================
-- TRIGGER 1: Update students.xp_points after a reward is earned
-- ============================================================================
CREATE TRIGGER trg_rewards_update_xp
AFTER INSERT ON rewards
FOR EACH ROW
BEGIN
  UPDATE students
     SET xp_points = xp_points + NEW.xp_earned,
         level = LEAST(10, FLOOR((xp_points + NEW.xp_earned) / 100) + 1)
   WHERE student_id = NEW.student_id;
END$$

-- ============================================================================
-- TRIGGER 2: Automatically create test_result when a test is submitted
-- ============================================================================
CREATE TRIGGER trg_tests_update_result
AFTER UPDATE ON tests
FOR EACH ROW
BEGIN
  IF NEW.status = 'COMPLETED' AND OLD.status <> 'COMPLETED' AND NEW.score IS NOT NULL THEN
    INSERT INTO test_results
      (test_id, student_id, total_marks, obtained_marks, percentage, grade,
       accuracy_rate, speed_score, difficulty_avg, summary_json)
    VALUES
      (NEW.test_id, NEW.student_id, NEW.total_marks, NEW.score, NEW.percentage,
       CASE
         WHEN NEW.percentage >= 90 THEN 'A+'
         WHEN NEW.percentage >= 80 THEN 'A'
         WHEN NEW.percentage >= 70 THEN 'B+'
         WHEN NEW.percentage >= 60 THEN 'B'
         WHEN NEW.percentage >= 50 THEN 'C'
         WHEN NEW.percentage >= 40 THEN 'D'
         ELSE 'F'
       END,
       IF(NEW.total_questions > 0, (NEW.correct_count / NEW.total_questions) * 100, 0),
       NULL, NULL, NULL);
  END IF;
END$$

-- ============================================================================
-- TRIGGER 3: Log login activity helper (called from app / procedure)
-- ============================================================================

-- ============================================================================
-- STORED PROCEDURE 1: sp_LoginAttempt - record login history
-- ============================================================================
CREATE PROCEDURE sp_LoginAttempt(
  IN p_user_id BIGINT,
  IN p_ip_address VARCHAR(45),
  IN p_user_agent VARCHAR(255),
  IN p_device VARCHAR(100),
  IN p_status ENUM('SUCCESS','FAILED')
)
BEGIN
  INSERT INTO login_history (user_id, ip_address, user_agent, device, status)
  VALUES (p_user_id, p_ip_address, p_user_agent, p_device, p_status);
END$$

-- ============================================================================
-- STORED PROCEDURE 2: sp_GenerateWeeklyReport - aggregate weekly performance
-- ============================================================================
CREATE PROCEDURE sp_GenerateWeeklyReport(IN p_student_id BIGINT)
BEGIN
  SELECT
    COUNT(*)                                                AS tests_taken,
    COALESCE(ROUND(AVG(tr.percentage), 2), 0)               AS avg_percentage,
    COALESCE(SUM(tr.obtained_marks), 0)                     AS total_marks_obtained,
    COALESCE(SUM(t.correct_count), 0)                       AS total_correct,
    COALESCE(SUM(t.incorrect_count), 0)                     AS total_incorrect,
    COALESCE(SUM(t.skipped_count), 0)                       AS total_skipped,
    COALESCE((SELECT AVG(ga.mastery_level)
                FROM knowledge_gap ga
               WHERE ga.student_id = p_student_id
                 AND ga.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)), 0) AS avg_mastery
  FROM test_results tr
  JOIN tests t ON t.test_id = tr.test_id
  WHERE tr.student_id = p_student_id
    AND tr.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY);
END$$

-- ============================================================================
-- STORED PROCEDURE 3: sp_GetLeaderboard - ranked students by XP
-- ============================================================================
CREATE PROCEDURE sp_GetLeaderboard(IN p_limit INT)
BEGIN
  SELECT
    s.student_id,
    u.full_name,
    s.student_code,
    s.department,
    s.semester,
    s.xp_points,
    s.level,
    s.streak_days,
    DENSE_RANK() OVER (ORDER BY s.xp_points DESC) AS rank_no
  FROM students s
  JOIN users u ON u.user_id = s.user_id
  ORDER BY s.xp_points DESC
  LIMIT p_limit;
END$$

-- ============================================================================
-- STORED PROCEDURE 4: sp_ExpireOldOtps - clean up expired OTPs
-- ============================================================================
CREATE PROCEDURE sp_ExpireOldOtps()
BEGIN
  UPDATE otp SET is_used = 1 WHERE expires_at < NOW() AND is_used = 0;
  DELETE FROM otp WHERE expires_at < DATE_SUB(NOW(), INTERVAL 7 DAY);
END$$

DELIMITER ;
