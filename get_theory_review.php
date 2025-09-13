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
$dbname = 'univault_db';
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

// Get parameters
$testResultId = isset($_GET['test_result_id']) ? (int)$_GET['test_result_id'] : 0;

if ($testResultId <= 0) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid test result ID'
    ]);
    exit;
}

try {
    // Get theory test answers with question details
    $sql = "
        SELECT 
            ta.answer_id,
            ta.question_id,
            ta.student_answer,
            ta.marks_allocated,
            ta.score_obtained,
            ta.keywords_matched,
            tq.question_text,
            tq.keywords,
            tq.complete_answer,
            tq.difficulty_level,
            tq.marks
        FROM theory_test_answers ta
        JOIN theory_questions tq ON ta.question_id = tq.question_id
        WHERE ta.test_result_id = :test_result_id
        ORDER BY ta.answer_id ASC
    ";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute(['test_result_id' => $testResultId]);
    $answers = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    if (empty($answers)) {
        echo json_encode([
            'success' => false,
            'message' => 'No review data found for this test'
        ]);
        exit;
    }
    
    // Format the response
    $formattedAnswers = [];
    foreach ($answers as $answer) {
        $formattedAnswers[] = [
            'answer_id' => (int)$answer['answer_id'],
            'question_id' => (int)$answer['question_id'],
            'question_text' => $answer['question_text'],
            'student_answer' => $answer['student_answer'],
            'marks_allocated' => (int)$answer['marks_allocated'],
            'score_obtained' => (int)$answer['score_obtained'],
            'keywords_matched' => $answer['keywords_matched'],
            'keywords' => $answer['keywords'],
            'complete_answer' => $answer['complete_answer'],
            'difficulty_level' => $answer['difficulty_level'],
            'marks' => (int)$answer['marks']
        ];
    }
    
    // Get test result summary
    $summarySql = "
        SELECT 
            total_questions,
            total_marks,
            total_score,
            percentage,
            test_date
        FROM theory_test_results 
        WHERE test_result_id = :test_result_id
    ";
    
    $stmt = $pdo->prepare($summarySql);
    $stmt->execute(['test_result_id' => $testResultId]);
    $summary = $stmt->fetch(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'message' => 'Theory review data retrieved successfully',
        'test_result_id' => $testResultId,
        'answers' => $formattedAnswers,
        'summary' => [
            'total_questions' => (int)$summary['total_questions'],
            'total_marks' => (int)$summary['total_marks'],
            'total_score' => (int)$summary['total_score'],
            'percentage' => round((float)$summary['percentage'], 2),
            'test_date' => $summary['test_date']
        ]
    ]);
    
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>
