-- Add missing columns to existing theory_test_results table
ALTER TABLE theory_test_results 
ADD COLUMN IF NOT EXISTS total_score INT DEFAULT 0 AFTER total_marks,
ADD COLUMN IF NOT EXISTS percentage DECIMAL(5,2) DEFAULT 0.00 AFTER answered_questions;

-- Add missing columns to existing theory_test_answers table
ALTER TABLE theory_test_answers 
ADD COLUMN IF NOT EXISTS score_obtained INT DEFAULT 0 AFTER marks_allocated,
ADD COLUMN IF NOT EXISTS keywords_matched TEXT AFTER score_obtained;

-- Verify the changes
DESCRIBE theory_test_results;
DESCRIBE theory_test_answers;
