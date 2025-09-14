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
$test_result_id = isset($_GET['test_result_id']) ? (int)$_GET['test_result_id'] : 0;

// Debug: Log the received test_result_id
error_log("Review request for test_result_id: $test_result_id");

// Validate parameters
if ($test_result_id <= 0) {
    echo json_encode(['error' => 'Invalid test result ID', 'debug' => ['test_result_id' => $test_result_id]]);
    exit;
}

try {
    // Get test result details (without joining courses table for now)
    $sql = "SELECT * FROM mcq_test_results WHERE test_result_id = :test_result_id";
    
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':test_result_id', $test_result_id, PDO::PARAM_INT);
    $stmt->execute();
    
    $test_result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$test_result) {
        echo json_encode(['error' => 'Test result not found', 'debug' => ['test_result_id' => $test_result_id]]);
        exit;
    }
    
    // Get detailed answers
    $sql = "SELECT ta.question_id, ta.selected_answer, ta.is_correct,
                   q.question_text, q.option_a, q.option_b, q.option_c, q.option_d, q.correct_option
            FROM mcq_test_answers ta
            JOIN mcq_combined q ON ta.question_id = q.question_id
            WHERE ta.test_result_id = :test_result_id
            ORDER BY ta.question_id";
    
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':test_result_id', $test_result_id, PDO::PARAM_INT);
    $stmt->execute();
    
    $answers = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Debug: Log the number of answers found
    error_log("Found " . count($answers) . " answers for test_result_id: $test_result_id");
    
    if (empty($answers)) {
        echo json_encode(['error' => 'No answers found for this test', 'debug' => ['test_result_id' => $test_result_id]]);
        exit;
    }
    
    // Format the response
    $response = [
        'success' => true,
        'test_info' => [
            'test_result_id' => (int)$test_result['test_result_id'],
            'course_code' => $test_result['course_code'],
            'course_name' => 'Course ' . $test_result['course_code'], // Use course_code
            'score' => (int)$test_result['score'],
            'total_questions' => (int)$test_result['total_questions'],
            'percentage' => $test_result['total_questions'] > 0 ? 
                           round(($test_result['score'] / $test_result['total_questions']) * 100, 1) : 0,
            'time_taken' => (int)$test_result['time_taken'],
            'test_date' => $test_result['test_date']
        ],
        'questions' => []
    ];
    
    foreach ($answers as $answer) {
        $correct_index = getCorrectAnswerIndex($answer['correct_option']);
        
        $response['questions'][] = [
            'question_id' => (int)$answer['question_id'],
            'question_text' => $answer['question_text'],
            'options' => [
                $answer['option_a'],
                $answer['option_b'],
                $answer['option_c'],
                $answer['option_d']
            ],
            'correct_answer' => $correct_index,
            'correct_option_letter' => $answer['correct_option'],
            'selected_answer' => (int)$answer['selected_answer'],
            'selected_option_letter' => getOptionLetter($answer['selected_answer']),
            'is_correct' => (bool)$answer['is_correct']
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

// Helper function to convert index (0, 1, 2, 3) to option letter (A, B, C, D)
function getOptionLetter($index) {
    switch($index) {
        case 0: return 'A';
        case 1: return 'B';
        case 2: return 'C';
        case 3: return 'D';
        default: return 'A';
    }
}
?>
