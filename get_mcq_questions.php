<?php
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

// Get parameters
$course_id = isset($_GET['course_id']) ? (int)$_GET['course_id'] : 1;
$limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 15;

// Validate parameters
if ($course_id <= 0) {
    echo json_encode(['error' => 'Invalid course ID']);
    exit;
}

if ($limit <= 0 || $limit > 50) {
    $limit = 15; // Default to 15 questions
}



try {
    // Fetch random questions for the specified course
    $sql = "SELECT question_id, question_text, option_a, option_b, option_c, option_d, correct_option 
            FROM mcq_combined 
            WHERE course_id = :course_id 
            ORDER BY RAND() 
            LIMIT :limit";
    
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':course_id', $course_id, PDO::PARAM_INT);
    $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
    $stmt->execute();
    
    $questions = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    if (empty($questions)) {
        echo json_encode(['error' => 'No questions found for this course']);
        exit;
    }
    
    // Format the response
    $response = [
        'success' => true,
        'course_id' => $course_id,
        'total_questions' => count($questions),
        'questions' => []
    ];
    
    foreach ($questions as $question) {
        $response['questions'][] = [
            'question_id' => (int)$question['question_id'],
            'question_text' => $question['question_text'],
            'options' => [
                $question['option_a'],
                $question['option_b'],
                $question['option_c'],
                $question['option_d']
            ],
            'correct_answer' => getCorrectAnswerIndex($question['correct_option'])
        ];
    }
    
    echo json_encode($response);
    
} catch(PDOException $e) {
    echo json_encode(['error' => 'Database query failed: ' . $e->getMessage()]);
}

// Helper function to convert correct_option (A, B, C, D) to index (0, 1, 2, 3)
function getCorrectAnswerIndex($correctOption) {
    switch(strtoupper($correctOption)) {
        case 'A': return 0;
        case 'B': return 1;
        case 'C': return 2;
        case 'D': return 3;
        default: return 0;
    }
}
?>
