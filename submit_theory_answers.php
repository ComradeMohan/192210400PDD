<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

// Database configuration
$host = 'localhost';
$dbname = 'univault';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed: ' . $e->getMessage()
    ]);
    exit;
}

// Get JSON input
$input = json_decode(file_get_contents('php://input'), true);

if (!$input) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid JSON input'
    ]);
    exit;
}

// Validate required fields
$requiredFields = ['student_id', 'course_id', 'answers'];
foreach ($requiredFields as $field) {
    if (!isset($input[$field])) {
        echo json_encode([
            'success' => false,
            'message' => "Missing required field: $field"
        ]);
        exit;
    }
}

$studentId = (int)$input['student_id'];
$courseId = (int)$input['course_id'];
$answers = $input['answers'];

if (!is_array($answers) || empty($answers)) {
    echo json_encode([
        'success' => false,
        'message' => 'Answers must be a non-empty array'
    ]);
    exit;
}

try {
    // Start transaction
    $pdo->beginTransaction();
    
    // Insert test result
    $stmt = $pdo->prepare("
        INSERT INTO theory_test_results (student_id, course_id, total_questions) 
        VALUES (:student_id, :course_id, :total_questions)
    ");
    $stmt->execute([
        'student_id' => $studentId,
        'course_id' => $courseId,
        'total_questions' => count($answers)
    ]);
    
    $testResultId = $pdo->lastInsertId();
    
    // Insert answers
    $stmt = $pdo->prepare("
        INSERT INTO theory_test_answers (test_result_id, question_id, student_answer) 
        VALUES (:test_result_id, :question_id, :student_answer)
    ");
    
    foreach ($answers as $answer) {
        if (!isset($answer['question_id']) || !isset($answer['answer'])) {
            throw new Exception('Invalid answer format');
        }
        
        $stmt->execute([
            'test_result_id' => $testResultId,
            'question_id' => (int)$answer['question_id'],
            'student_answer' => trim($answer['answer'])
        ]);
    }
    
    // Commit transaction
    $pdo->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Theory answers submitted successfully',
        'test_result_id' => $testResultId,
        'total_questions' => count($answers),
        'submission_date' => date('Y-m-d H:i:s')
    ]);
    
} catch (PDOException $e) {
    // Rollback transaction on error
    $pdo->rollBack();
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
} catch (Exception $e) {
    // Rollback transaction on error
    $pdo->rollBack();
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>
