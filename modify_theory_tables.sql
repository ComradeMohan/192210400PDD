-- Modify Theory Tables to Use course_code Instead of course_id
-- This script updates the theory tables to match the MCQ table structure

-- 1. Add course_code column to theory_test_results table
ALTER TABLE theory_test_results 
ADD COLUMN course_code VARCHAR(50) AFTER course_id;

-- 2. Update existing records with course_code values
-- This assumes you have a prepcourses table with course_id and course_code mapping
UPDATE theory_test_results ttr
JOIN prepcourses pc ON ttr.course_id = pc.course_id
SET ttr.course_code = pc.course_code;

-- 3. Make course_code NOT NULL after updating existing records
ALTER TABLE theory_test_results 
MODIFY COLUMN course_code VARCHAR(50) NOT NULL;

-- 4. Add index for course_code for better performance
ALTER TABLE theory_test_results 
ADD INDEX idx_course_code (course_code);

-- 5. Add index for student_id and course_code combination
ALTER TABLE theory_test_results 
ADD INDEX idx_student_course_code (student_id, course_code);

-- 6. Optional: You can drop the course_id column if you want to fully migrate
-- ALTER TABLE theory_test_results DROP COLUMN course_id;

-- 7. Optional: Drop the old index if you dropped course_id
-- ALTER TABLE theory_test_results DROP INDEX idx_student_course;

-- Note: The theory_questions table still uses course_id because it references
-- the prepcourses table directly. The theory_test_results table now uses
-- course_code to match the MCQ structure.

-- Verify the changes
SELECT 
    'theory_test_results' as table_name,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'univault_db' 
AND TABLE_NAME = 'theory_test_results'
AND COLUMN_NAME IN ('course_id', 'course_code')
ORDER BY ORDINAL_POSITION;
