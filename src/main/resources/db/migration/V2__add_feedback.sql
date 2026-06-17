-- Add accepted boolean column to track developer feedback for LLM-generated comments
ALTER TABLE comments ADD COLUMN accepted BOOLEAN;
