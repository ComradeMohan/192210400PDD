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

// Get POST data
$input = json_decode(file_get_contents('php://input'), true);

// Debug: Log the received data
error_log("Received data: " . print_r($input, true));

if (!$input) {
    echo json_encode(['error' => 'Invalid JSON data']);
    exit;
}

// Extract data
$student_id = isset($input['student_id']) ? (int)$input['student_id'] : 0;
$course_id = isset($input['course_id']) ? (int)$input['course_id'] : 0;
$score = isset($input['score']) ? (int)$input['score'] : 0;
$total_questions = isset($input['total_questions']) ? (int)$input['total_questions'] : 0;
$time_taken = isset($input['time_taken']) ? (int)$input['time_taken'] : 0;
$answers = isset($input['answers']) ? $input['answers'] : [];

// Debug: Log extracted data
error_log("Extracted data - student_id: $student_id, course_id: $course_id, score: $score, total_questions: $total_questions, time_taken: $time_taken");

// Validate required fields
if ($student_id <= 0 || $course_id <= 0 || $total_questions <= 0) {
    echo json_encode(['error' => 'Missing required fields', 'debug' => [
        'student_id' => $student_id,
        'course_id' => $course_id,
        'total_questions' => $total_questions
    ]]);
    exit;
}

try {
    // Start transaction
    $pdo->beginTransaction();
    
    // Insert test result
    $sql = "INSERT INTO mcq_test_results (student_id, course_id, score, total_questions, time_taken, test_date) 
            VALUES (:student_id, :course_id, :score, :total_questions, :time_taken, NOW())";
    
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':student_id', $student_id, PDO::PARAM_INT);
    $stmt->bindParam(':course_id', $course_id, PDO::PARAM_INT);
    $stmt->bindParam(':score', $score, PDO::PARAM_INT);
    $stmt->bindParam(':total_questions', $total_questions, PDO::PARAM_INT);
    $stmt->bindParam(':time_taken', $time_taken, PDO::PARAM_INT);
    $stmt->execute();
    
    $test_result_id = $pdo->lastInsertId();
    
    // Insert individual answers
    if (!empty($answers)) {
        $sql = "INSERT INTO mcq_test_answers (test_result_id, question_id, selected_answer, is_correct) 
                VALUES (:test_result_id, :question_id, :selected_answer, :is_correct)";
        
        $stmt = $pdo->prepare($sql);
        
        foreach ($answers as $question_id => $selected_answer) {
            // Get correct answer for this question
            $correct_sql = "SELECT correct_option FROM mcq_combined WHERE question_id = :question_id";
            $correct_stmt = $pdo->prepare($correct_sql);
            $correct_stmt->bindParam(':question_id', $question_id, PDO::PARAM_INT);
            $correct_stmt->execute();
            $correct_option = $correct_stmt->fetchColumn();
            
            // Convert correct option to index
            $correct_index = getCorrectAnswerIndex($correct_option);
            $is_correct = ($selected_answer == $correct_index) ? 1 : 0;
            
            $stmt->bindParam(':test_result_id', $test_result_id, PDO::PARAM_INT);
            $stmt->bindParam(':question_id', $question_id, PDO::PARAM_INT);
            $stmt->bindParam(':selected_answer', $selected_answer, PDO::PARAM_INT);
            $stmt->bindParam(':is_correct', $is_correct, PDO::PARAM_INT);
            $stmt->execute();
        }
    }
    
    // Commit transaction
    $pdo->commit();
    
    echo json_encode([
        'success' => true,
        'test_result_id' => $test_result_id,
        'message' => 'Test result saved successfully'
    ]);
    
} catch(PDOException $e) {
    // Rollback transaction on error
    $pdo->rollback();
    echo json_encode(['error' => 'Failed to save test result: ' . $e->getMessage()]);
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
