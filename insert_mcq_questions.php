<?php
// Script to insert MCQ questions into database
// You can modify this to insert your 153 questions

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Database configuration
$host = 'localhost';
$dbname = 'univault_db'; // Replace with your database name
$username = 'root'; // Replace with your database username
$password = ''; // Replace with your database password

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch(PDOException $e) {
    echo json_encode(['error' => 'Database connection failed: ' . $e->getMessage()]);
    exit;
}

// Sample questions for Artificial Intelligence (course_id = 1)
// Replace this array with your actual 153 questions
$questions = [
    // Question 1
    [
        'course_id' => 1,
        'question_text' => 'Which approach to AI focuses on building systems that can think and act like humans?',
        'option_a' => 'Thinking Rationally',
        'option_b' => 'Acting Humanly',
        'option_c' => 'Thinking Humanly',
        'option_d' => 'Acting Rationally',
        'correct_option' => 'C'
    ],
    // Question 2
    [
        'course_id' => 1,
        'question_text' => 'The "laws of thought" approach to Artificial Intelligence is most closely associated with which concept?',
        'option_a' => 'Passing the Turing Test',
        'option_b' => 'Cognitive Modeling',
        'option_c' => 'Thinking Rationally',
        'option_d' => 'Designing rational agents',
        'correct_option' => 'C'
    ],
    // Question 3
    [
        'course_id' => 1,
        'question_text' => 'The Turing test approach is most closely associated with which AI concept?',
        'option_a' => 'Thinking Humanly',
        'option_b' => 'Acting Humanly',
        'option_c' => 'Thinking Rationally',
        'option_d' => 'Acting Rationally',
        'correct_option' => 'B'
    ],
    // Question 4
    [
        'course_id' => 1,
        'question_text' => 'Which AI problem domain deals with understanding and processing human language?',
        'option_a' => 'Robotics',
        'option_b' => 'Planning',
        'option_c' => 'Knowledge Representation',
        'option_d' => 'Natural Language Processing (NLP)',
        'correct_option' => 'D'
    ],
    // Question 5
    [
        'course_id' => 1,
        'question_text' => 'According to the sources, which field is a foundation of Artificial Intelligence?',
        'option_a' => 'Astrology',
        'option_b' => 'Philosophy',
        'option_c' => 'Paleontology',
        'option_d' => 'Culinary Arts',
        'correct_option' => 'B'
    ],
    // Add your remaining 148 questions here...
    // Follow the same format for each question
];

try {
    $pdo->beginTransaction();
    
    $sql = "INSERT INTO mcq_questions (course_id, question_text, option_a, option_b, option_c, option_d, correct_option) 
            VALUES (:course_id, :question_text, :option_a, :option_b, :option_c, :option_d, :correct_option)";
    
    $stmt = $pdo->prepare($sql);
    
    $inserted_count = 0;
    foreach ($questions as $question) {
        $stmt->bindParam(':course_id', $question['course_id'], PDO::PARAM_INT);
        $stmt->bindParam(':question_text', $question['question_text']);
        $stmt->bindParam(':option_a', $question['option_a']);
        $stmt->bindParam(':option_b', $question['option_b']);
        $stmt->bindParam(':option_c', $question['option_c']);
        $stmt->bindParam(':option_d', $question['option_d']);
        $stmt->bindParam(':correct_option', $question['correct_option']);
        
        if ($stmt->execute()) {
            $inserted_count++;
        }
    }
    
    $pdo->commit();
    
    echo json_encode([
        'success' => true,
        'message' => "Successfully inserted $inserted_count questions",
        'total_questions' => count($questions)
    ]);
    
} catch(PDOException $e) {
    $pdo->rollback();
    echo json_encode(['error' => 'Failed to insert questions: ' . $e->getMessage()]);
}
?>
