-- SQL script to modify mcq_test_results table to use course_code instead of course_id

-- Step 1: Add course_code column to mcq_test_results table
ALTER TABLE mcq_test_results 
ADD COLUMN course_code VARCHAR(50) AFTER course_id;

-- Step 2: Update existing records with course_code values
-- You'll need to populate this based on your courses table
-- Example: UPDATE mcq_test_results SET course_code = 'CSA57' WHERE course_id = 1;

-- Step 3: Make course_code NOT NULL after populating data
ALTER TABLE mcq_test_results 
MODIFY COLUMN course_code VARCHAR(50) NOT NULL;

-- Step 4: Add index on course_code for better performance
ALTER TABLE mcq_test_results 
ADD INDEX idx_course_code (course_code);

-- Step 5: (Optional) Drop the old course_id column if you want to completely replace it
-- ALTER TABLE mcq_test_results DROP COLUMN course_id;

-- Alternative: If you want to keep both columns, you can just add the course_code column
-- and use it for new records while keeping course_id for backward compatibility

-- Step 6: Update mcq_test_answers table if it references course information
-- (This table might not need changes if it only references test_result_id)

-- Example of how to populate course_code for existing records:
-- UPDATE mcq_test_results r 
-- JOIN courses c ON r.course_id = c.course_id 
-- SET r.course_code = c.course_code;
