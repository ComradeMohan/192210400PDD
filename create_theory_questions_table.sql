-- Create Theory Questions table
CREATE TABLE IF NOT EXISTS theory_questions (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    question_text TEXT NOT NULL,
    sample_answer TEXT,
    difficulty_level ENUM('EASY', 'MEDIUM', 'HARD') DEFAULT 'MEDIUM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_course (course_id),
    INDEX idx_difficulty (difficulty_level)
);

-- Create Theory Test Results table
CREATE TABLE IF NOT EXISTS theory_test_results (
    test_result_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    total_questions INT NOT NULL,
    test_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_course (student_id, course_id),
    INDEX idx_test_date (test_date)
);

-- Create Theory Test Answers table
CREATE TABLE IF NOT EXISTS theory_test_answers (
    answer_id INT AUTO_INCREMENT PRIMARY KEY,
    test_result_id INT NOT NULL,
    question_id INT NOT NULL,
    student_answer TEXT NOT NULL,
    FOREIGN KEY (test_result_id) REFERENCES theory_test_results(test_result_id) ON DELETE CASCADE,
    INDEX idx_test_result (test_result_id),
    INDEX idx_question (question_id)
);

-- Insert sample theory questions for Artificial Intelligence course (course_id = 1)
INSERT IGNORE INTO theory_questions (question_id, course_id, question_text, sample_answer, difficulty_level) VALUES
(1, 1, 'Explain the fundamental differences between Artificial Intelligence, Machine Learning, and Deep Learning. Provide examples of each.', 'AI is the broad concept of machines being able to carry out tasks in a smart way. ML is a subset of AI that focuses on algorithms that can learn from data. Deep Learning is a subset of ML that uses neural networks with multiple layers. Examples: AI - Siri, ML - Email spam detection, Deep Learning - Image recognition.', 'MEDIUM'),
(2, 1, 'Describe the four main approaches to Artificial Intelligence as outlined in the course material. Which approach do you think is most practical for real-world applications?', 'The four approaches are: 1) Acting Humanly (Turing Test), 2) Thinking Humanly (Cognitive Modeling), 3) Thinking Rationally (Laws of Thought), 4) Acting Rationally (Rational Agent). Acting Rationally is most practical as it focuses on achieving goals efficiently.', 'MEDIUM'),
(3, 1, 'What is the Turing Test and what are its limitations? Explain why some AI researchers consider it insufficient for measuring true intelligence.', 'The Turing Test is a test of a machine\'s ability to exhibit intelligent behavior equivalent to a human. Limitations include: only tests conversational ability, doesn\'t test reasoning or understanding, can be fooled by chatbots, doesn\'t measure creativity or emotional intelligence.', 'HARD'),
(4, 1, 'Explain the concept of rational agents in AI. What are the key components that make an agent rational?', 'A rational agent is one that does the right thing given what it knows. Key components: 1) Performance measure (defines success), 2) Environment (where agent operates), 3) Actuators (how agent affects environment), 4) Sensors (how agent perceives environment).', 'MEDIUM'),
(5, 1, 'Compare and contrast supervised learning, unsupervised learning, and reinforcement learning. Provide real-world examples of each.', 'Supervised learning uses labeled data to learn patterns (e.g., email spam detection). Unsupervised learning finds patterns in unlabeled data (e.g., customer segmentation). Reinforcement learning learns through trial and error with rewards/penalties (e.g., game playing AI).', 'MEDIUM'),
(6, 1, 'What is the difference between strong AI and weak AI? Which type of AI do we currently have, and what would be required to achieve strong AI?', 'Weak AI is designed for specific tasks (current state), while strong AI would have human-level intelligence across all domains. We currently have weak AI. Strong AI would require general intelligence, consciousness, and the ability to understand context across all domains.', 'HARD'),
(7, 1, 'Explain the concept of machine learning bias. How can bias be introduced into AI systems, and what are the potential consequences?', 'ML bias occurs when algorithms produce systematically prejudiced results. Sources: biased training data, algorithm design, human bias in data collection. Consequences: unfair decisions, discrimination, perpetuation of stereotypes, loss of trust in AI systems.', 'MEDIUM'),
(8, 1, 'Describe the process of natural language processing. What are the main challenges in making computers understand human language?', 'NLP involves teaching computers to understand, interpret, and generate human language. Challenges: ambiguity, context dependency, cultural differences, sarcasm/irony, evolving language, multiple meanings, and the complexity of human communication.', 'MEDIUM'),
(9, 1, 'What is computer vision and how does it work? Explain the role of deep learning in modern computer vision applications.', 'Computer vision enables machines to interpret visual information from the world. It works by processing images/videos through algorithms. Deep learning, especially CNNs, has revolutionized CV by automatically learning features from data, enabling applications like object detection, facial recognition, and medical imaging.', 'MEDIUM'),
(10, 1, 'Explain the concept of expert systems in AI. What are their advantages and limitations compared to modern AI approaches?', 'Expert systems are AI programs that mimic human expertise in specific domains using knowledge bases and inference engines. Advantages: explainable decisions, domain expertise. Limitations: limited to specific domains, require extensive knowledge engineering, can\'t learn from new data, brittle when faced with novel situations.', 'MEDIUM'),
(11, 1, 'What is the role of knowledge representation in AI? Describe different methods of representing knowledge and their trade-offs.', 'Knowledge representation is how AI systems store and organize information. Methods: 1) Logical representations (precise but limited), 2) Semantic networks (intuitive but complex), 3) Frames (structured but rigid), 4) Production rules (simple but can be incomplete). Trade-offs between expressiveness and computational efficiency.', 'HARD'),
(12, 1, 'Explain the concept of search algorithms in AI. Compare breadth-first search and depth-first search, including their advantages and disadvantages.', 'Search algorithms find solutions by exploring possible states. BFS explores all nodes at current level before going deeper - guarantees optimal solution but uses more memory. DFS goes as deep as possible first - memory efficient but may not find optimal solution and can get stuck in infinite loops.', 'MEDIUM'),
(13, 1, 'What is the difference between deterministic and non-deterministic AI algorithms? Provide examples of each type.', 'Deterministic algorithms always produce the same output for the same input (e.g., A* search, minimax). Non-deterministic algorithms may produce different outputs for the same input (e.g., genetic algorithms, simulated annealing, some neural networks with random initialization).', 'MEDIUM'),
(14, 1, 'Describe the concept of artificial neural networks. How do they mimic biological neural networks, and what are their key components?', 'ANNs are computing systems inspired by biological neural networks. They mimic by using interconnected nodes (neurons) that process information. Key components: input layer, hidden layers, output layer, weights, activation functions, and learning algorithms that adjust weights based on training data.', 'MEDIUM'),
(15, 1, 'What are the ethical implications of artificial intelligence? Discuss at least three major ethical concerns and potential solutions.', 'Major concerns: 1) Job displacement - solution: retraining programs, 2) Privacy invasion - solution: data protection laws, 3) Bias and discrimination - solution: diverse training data and algorithmic auditing, 4) Autonomous weapons - solution: international regulations, 5) Loss of human control - solution: human-in-the-loop systems.', 'HARD');
