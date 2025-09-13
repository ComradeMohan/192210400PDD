-- Create MCQ Test Results table
CREATE TABLE IF NOT EXISTS mcq_test_results (
    test_result_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    score INT NOT NULL,
    total_questions INT NOT NULL,
    time_taken INT NOT NULL, -- in seconds
    test_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_course (student_id, course_id),
    INDEX idx_test_date (test_date)
);

-- Create MCQ Test Answers table
CREATE TABLE IF NOT EXISTS mcq_test_answers (
    answer_id INT AUTO_INCREMENT PRIMARY KEY,
    test_result_id INT NOT NULL,
    question_id INT NOT NULL,
    selected_answer INT NOT NULL, -- 0, 1, 2, 3 for A, B, C, D
    is_correct TINYINT(1) NOT NULL, -- 0 or 1
    FOREIGN KEY (test_result_id) REFERENCES mcq_test_results(test_result_id) ON DELETE CASCADE,
    INDEX idx_test_result (test_result_id),
    INDEX idx_question (question_id)
);

-- Note: Courses table already exists with the following structure:
-- course_id, course_code, course_name, description, created_at, topic_count
-- No need to create or insert courses as they already exist

-- Sample MCQ questions (you already have these, but here's the structure)
CREATE TABLE IF NOT EXISTS mcq_questions (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    question_text TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_option ENUM('A', 'B', 'C', 'D') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_course (course_id)
);

-- Insert sample questions for Artificial Intelligence course (course_id = 1)
-- You can replace these with your actual 153 questions
INSERT IGNORE INTO mcq_questions (question_id, course_id, question_text, option_a, option_b, option_c, option_d, correct_option) VALUES
(1, 1, 'Which approach to AI focuses on building systems that can think and act like humans?', 'Thinking Rationally', 'Acting Humanly', 'Thinking Humanly', 'Acting Rationally', 'C'),
(2, 1, 'The "laws of thought" approach to Artificial Intelligence is most closely associated with which concept?', 'Passing the Turing Test', 'Cognitive Modeling', 'Thinking Rationally', 'Designing rational agents', 'C'),
(3, 1, 'The Turing test approach is most closely associated with which AI concept?', 'Thinking Humanly', 'Acting Humanly', 'Thinking Rationally', 'Acting Rationally', 'B'),
(4, 1, 'Which AI problem domain deals with understanding and processing human language?', 'Robotics', 'Planning', 'Knowledge Representation', 'Natural Language Processing (NLP)', 'D'),
(5, 1, 'According to the sources, which field is a foundation of Artificial Intelligence?', 'Astrology', 'Philosophy', 'Paleontology', 'Culinary Arts', 'B');

-- Note: You need to insert your 153 actual questions here
-- Make sure to use the correct course_id for each course:
-- course_id = 1: Artificial Intelligence (CSA17)
-- course_id = 2: Fundamentals of Computing (CSA57)  
-- course_id = 3: Theory of Computation (CSA13)
-- course_id = 5: Computer Network (CSA07)
-- course_id = 6: Operating Systems (CSA04)
