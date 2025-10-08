-- SQL script to create the student_study_time table
-- This table will store the total study time for each student per course and mode

CREATE TABLE IF NOT EXISTS student_study_time (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    mode ENUM('pass', 'master', 'all') DEFAULT 'all',
    total_study_time_millis BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Create unique constraint to prevent duplicate records
    UNIQUE KEY unique_student_course_mode (student_id, course_code, mode),
    
    -- Add indexes for better query performance
    INDEX idx_student_id (student_id),
    INDEX idx_course_code (course_code),
    INDEX idx_mode (mode),
    INDEX idx_last_updated (last_updated)
);

-- Optional: Create a view to get study time in human-readable format
CREATE OR REPLACE VIEW v_student_study_time AS
SELECT 
    id,
    student_id,
    course_code,
    mode,
    total_study_time_millis,
    FLOOR(total_study_time_millis / (1000 * 60 * 60)) AS hours,
    FLOOR((total_study_time_millis % (1000 * 60 * 60)) / (1000 * 60)) AS minutes,
    FLOOR((total_study_time_millis % (1000 * 60)) / 1000) AS seconds,
    CASE 
        WHEN FLOOR(total_study_time_millis / (1000 * 60 * 60)) > 0 THEN
            CONCAT(
                FLOOR(total_study_time_millis / (1000 * 60 * 60)), ' h ',
                FLOOR((total_study_time_millis % (1000 * 60 * 60)) / (1000 * 60)), ' min'
            )
        ELSE
            CONCAT(FLOOR((total_study_time_millis % (1000 * 60 * 60)) / (1000 * 60)), ' min')
    END AS formatted_time,
    created_at,
    last_updated
FROM student_study_time;

-- Sample queries for testing and analytics

-- 1. Get total study time for a specific student and course
-- SELECT * FROM v_student_study_time 
-- WHERE student_id = 1 AND course_code = 'CS101';

-- 2. Get total study time for all courses of a student
-- SELECT course_code, mode, formatted_time, last_updated 
-- FROM v_student_study_time 
-- WHERE student_id = 1 
-- ORDER BY last_updated DESC;

-- 3. Get top students by study time for a specific course
-- SELECT student_id, mode, formatted_time 
-- FROM v_student_study_time 
-- WHERE course_code = 'CS101' 
-- ORDER BY total_study_time_millis DESC 
-- LIMIT 10;

-- 4. Get average study time per course
-- SELECT 
--     course_code, 
--     mode,
--     COUNT(*) as student_count,
--     AVG(total_study_time_millis / (1000 * 60 * 60)) as avg_hours,
--     MAX(total_study_time_millis / (1000 * 60 * 60)) as max_hours
-- FROM student_study_time 
-- GROUP BY course_code, mode 
-- ORDER BY avg_hours DESC;

-- 5. Get students who studied more than X hours
-- SELECT student_id, course_code, mode, formatted_time 
-- FROM v_student_study_time 
-- WHERE total_study_time_millis > (5 * 60 * 60 * 1000)  -- More than 5 hours
-- ORDER BY total_study_time_millis DESC;