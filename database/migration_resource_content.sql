-- ============================================================================
-- Adds content grounding to "I have learned up to here" question generation.
--
-- Run this once on a database created from an older schema.sql. New installs
-- already get these columns from schema.sql and should skip this file.
--
-- What it adds to resource_views:
--   watched_seconds / duration_seconds
--       Where the student stopped in a video, so only the part they watched is
--       turned into questions.
--   content_text / content_source / content_seconds
--       The text the resource actually said (captions, or readable page text)
--       up to that position, kept so the questions can be written from it and so
--       a failed fetch does not have to be repeated. content_seconds records
--       which position content_text covers, so a longer watch is re-read instead
--       of silently reusing a short extract.
-- ============================================================================

ALTER TABLE resource_views
  ADD COLUMN watched_seconds  INT         NULL AFTER progress_label,
  ADD COLUMN duration_seconds INT         NULL AFTER watched_seconds,
  ADD COLUMN content_text     MEDIUMTEXT NULL AFTER duration_seconds,
  ADD COLUMN content_source   VARCHAR(20) NULL AFTER content_text,
  ADD COLUMN content_seconds  INT         NULL AFTER content_source;
