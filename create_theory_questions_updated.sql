-- Create Theory Questions table (updated structure to match your existing table)
CREATE TABLE IF NOT EXISTS theory_questions (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    question_text TEXT NOT NULL,
    keywords TEXT,
    complete_answer TEXT,
    difficulty_level ENUM('easy', 'medium', 'hard') DEFAULT 'easy',
    marks INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_course (course_id),
    INDEX idx_difficulty (difficulty_level)
);

-- Create Theory Test Results table (updated structure)
CREATE TABLE IF NOT EXISTS theory_test_results (
    test_result_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    total_questions INT NOT NULL,
    total_marks INT DEFAULT 0,
    total_score INT DEFAULT 0,
    answered_questions INT DEFAULT 0,
    percentage DECIMAL(5,2) DEFAULT 0.00,
    test_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_course (student_id, course_id),
    INDEX idx_test_date (test_date)
);

-- Create Theory Test Answers table (updated structure)
CREATE TABLE IF NOT EXISTS theory_test_answers (
    answer_id INT AUTO_INCREMENT PRIMARY KEY,
    test_result_id INT NOT NULL,
    question_id INT NOT NULL,
    student_answer TEXT NOT NULL,
    marks_allocated INT DEFAULT 0,
    score_obtained INT DEFAULT 0,
    keywords_matched TEXT,
    FOREIGN KEY (test_result_id) REFERENCES theory_test_results(test_result_id) ON DELETE CASCADE,
    INDEX idx_test_result (test_result_id),
    INDEX idx_question (question_id)
);

-- Insert sample theory questions for Artificial Intelligence course (course_id = 1)
-- Based on your existing data structure
INSERT IGNORE INTO theory_questions (question_id, course_id, question_text, keywords, complete_answer, difficulty_level, marks) VALUES
(2, 1, 'What are the four primary approaches to defining Artificial Intelligence?', 'four approaches, Artificial Intelligence', 'The four primary approaches are **Thinking Humanly** (cognitive modeling), **Acting Humanly** (Turing Test), **Thinking Rationally** (laws of thought), and **Acting Rationally** (rational agent approach).', 'easy', 4),
(3, 1, 'Which approach to AI involves the cognitive modeling of human thought processes?', 'cognitive modeling, human thought, AI approach', 'The cognitive modeling approach, which focuses on understanding how humans think and replicating those processes in machines.', 'easy', 1),
(4, 1, 'Explain the "laws of thought" approach to AI.', 'laws of thought, AI approach', 'The "**laws of thought**" approach to AI is about creating systems that can think logically and rationally, following the same principles that govern human reasoning.', 'easy', 1),
(5, 1, 'What is the difference between strong AI and weak AI?', 'strong AI, weak AI, artificial intelligence', 'Weak AI is designed for specific tasks and cannot think beyond its programming, while strong AI would have human-level intelligence and consciousness across all domains.', 'medium', 3),
(6, 1, 'Explain the concept of machine learning and its types.', 'machine learning, supervised learning, unsupervised learning', 'Machine learning is a subset of AI that enables computers to learn from data. Types include supervised learning (with labeled data), unsupervised learning (finding patterns), and reinforcement learning (learning through rewards).', 'medium', 4),
(7, 1, 'What is the Turing Test and what are its limitations?', 'Turing Test, AI evaluation, limitations', 'The Turing Test evaluates if a machine can exhibit intelligent behavior equivalent to a human. Limitations include only testing conversational ability, not true understanding or reasoning capabilities.', 'medium', 3),
(8, 1, 'Describe the role of neural networks in artificial intelligence.', 'neural networks, artificial intelligence, deep learning', 'Neural networks are computing systems inspired by biological neural networks. They process information through interconnected nodes and are fundamental to deep learning and pattern recognition.', 'medium', 4),
(9, 1, 'What are the ethical implications of artificial intelligence?', 'AI ethics, artificial intelligence, ethical concerns', 'AI ethics concerns include job displacement, privacy invasion, bias in algorithms, autonomous weapons, and the need for human oversight in AI decision-making processes.', 'hard', 5),
(10, 1, 'Explain the concept of natural language processing in AI.', 'natural language processing, NLP, AI', 'Natural Language Processing (NLP) is a field of AI that focuses on enabling computers to understand, interpret, and generate human language in a valuable way.', 'medium', 3),
(11, 1, 'What is computer vision and how does it work?', 'computer vision, image processing, AI', 'Computer vision is a field of AI that trains computers to interpret and understand visual information from the world, using algorithms to process images and videos.', 'medium', 3),
(12, 1, 'Describe the concept of expert systems in AI.', 'expert systems, knowledge-based systems, AI', 'Expert systems are AI programs that mimic human expertise in specific domains by using knowledge bases and inference engines to solve complex problems.', 'medium', 3),
(13, 1, 'What is the difference between deterministic and non-deterministic AI algorithms?', 'deterministic, non-deterministic, AI algorithms', 'Deterministic algorithms always produce the same output for the same input, while non-deterministic algorithms may produce different outputs due to randomness or probabilistic elements.', 'hard', 4),
(14, 1, 'Explain the concept of search algorithms in AI.', 'search algorithms, AI, problem solving', 'Search algorithms in AI are methods for finding solutions by exploring possible states. They include breadth-first search, depth-first search, and heuristic search methods like A*.', 'medium', 4),
(15, 1, 'What are the challenges in achieving artificial general intelligence?', 'AGI, artificial general intelligence, challenges', 'Challenges include understanding consciousness, achieving common sense reasoning, handling uncertainty, and creating systems that can learn and adapt across diverse domains like humans do.', 'hard', 5),
(16, 1, 'Describe the concept of knowledge representation in AI systems.', 'knowledge representation, AI, information storage', 'Knowledge representation is how AI systems store and organize information to enable reasoning and problem-solving, using methods like logical representations, semantic networks, and frames.', 'hard', 4);
