-- SQL script to create the student_study_time table
-- This table stores the total study time for each student per course and mode

CREATE TABLE student_study_time (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_code VARCHAR(50) NOT NULL,
    mode ENUM('pass', 'master', 'all') NOT NULL DEFAULT 'all',
    total_study_time_millis BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Create unique constraint to prevent duplicate records for same student, course, and mode
    UNIQUE KEY unique_student_course_mode (student_id, course_code, mode),
    
    -- Add indexes for better query performance
    INDEX idx_student_id (student_id),
    INDEX idx_course_code (course_code),
    INDEX idx_mode (mode),
    INDEX idx_last_updated (last_updated),
    INDEX idx_student_course (student_id, course_code)
);

-- Optional: Create a view to display study time in human-readable format
CREATE OR REPLACE VIEW v_student_study_time_readable AS
SELECT 
    id,
    student_id,
    course_code,
    mode,
    total_study_time_millis,
    -- Convert milliseconds to hours, minutes, seconds
    FLOOR(total_study_time_millis / (1000 * 60 * 60)) AS hours,
    FLOOR((total_study_time_millis % (1000 * 60 * 60)) / (1000 * 60)) AS minutes,
    FLOOR((total_study_time_millis % (1000 * 60)) / 1000) AS seconds,
    -- Create formatted time string
    CASE 
        WHEN FLOOR(total_study_time_millis / (1000 * 60 * 60)) > 0 THEN
            CONCAT(
                FLOOR(total_study_time_millis / (1000 * 60 * 60)), ' h ',
                FLOOR((total_study_time_millis % (1000 * 60 * 60)) / (1000 * 60)), ' min'
            )
        WHEN FLOOR((total_study_time_millis % (1000 * 60 * 60)) / (1000 * 60)) > 0 THEN
            CONCAT(FLOOR((total_study_time_millis % (1000 * 60 * 60)) / (1000 * 60)), ' min')
        ELSE
            CONCAT(FLOOR((total_study_time_millis % (1000 * 60)) / 1000), ' sec')
    END AS formatted_time,
    created_at,
    last_updated
FROM student_study_time;

-- Sample data insertion (for testing purposes)
-- INSERT INTO student_study_time (student_id, course_code, mode, total_study_time_millis) 
-- VALUES 
-- (1, 'CS101', 'pass', 3600000),    -- 1 hour
-- (1, 'CS101', 'master', 7200000),  -- 2 hours
-- (2, 'MATH201', 'all', 5400000);   -- 1.5 hours

-- Useful queries for analytics and reporting:

-- 1. Get total study time for a specific student and course
-- SELECT * FROM v_student_study_time_readable 
-- WHERE student_id = 1 AND course_code = 'CS101';

-- 2. Get all study times for a student across all courses
-- SELECT course_code, mode, formatted_time, last_updated 
-- FROM v_student_study_time_readable 
-- WHERE student_id = 1 
-- ORDER BY last_updated DESC;

-- 3. Get top students by study time for a specific course
-- SELECT student_id, mode, formatted_time, total_study_time_millis
-- FROM v_student_study_time_readable 
-- WHERE course_code = 'CS101' 
-- ORDER BY total_study_time_millis DESC 
-- LIMIT 10;

-- 4. Get average study time per course and mode
-- SELECT 
--     course_code, 
--     mode,
--     COUNT(*) as student_count,
--     ROUND(AVG(total_study_time_millis / (1000 * 60 * 60)), 2) as avg_hours,
--     ROUND(MAX(total_study_time_millis / (1000 * 60 * 60)), 2) as max_hours,
--     ROUND(MIN(total_study_time_millis / (1000 * 60 * 60)), 2) as min_hours
-- FROM student_study_time 
-- GROUP BY course_code, mode 
-- ORDER BY avg_hours DESC;

-- 5. Get students who studied more than a specific duration (e.g., 5 hours)
-- SELECT student_id, course_code, mode, formatted_time 
-- FROM v_student_study_time_readable 
-- WHERE total_study_time_millis > (5 * 60 * 60 * 1000)  -- More than 5 hours
-- ORDER BY total_study_time_millis DESC;

-- 6. Get total study time across all courses for each student
-- SELECT 
--     student_id,
--     COUNT(DISTINCT course_code) as courses_studied,
--     SUM(total_study_time_millis) as total_millis,
--     ROUND(SUM(total_study_time_millis) / (1000 * 60 * 60), 2) as total_hours
-- FROM student_study_time 
-- GROUP BY student_id 
-- ORDER BY total_hours DESC;