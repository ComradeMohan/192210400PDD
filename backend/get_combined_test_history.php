<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Use centralized DB connection
include 'db.php';

// Get parameters
$student_id = isset($_GET['student_id']) ? (int)$_GET['student_id'] : 0;
$course_code = isset($_GET['course_id']) ? $_GET['course_id'] : ''; // Parameter name is course_id but contains course_code

// Debug logging
error_log("get_combined_test_history.php - student_id: $student_id, course_code: $course_code");

// Validate parameters
if ($student_id <= 0 || empty($course_code)) {
    echo json_encode(['error' => 'Missing required parameters: student_id and course_id']);
    exit;
}

try {
    // Query to get combined test history (MCQ + Theory) for the student and course
    $sql = "SELECT 
                'MCQ' as test_type,
                mtr.test_result_id as id,
                mtr.score as total_score,
                mtr.total_questions,
                ROUND((mtr.score / mtr.total_questions) * 100, 2) as percentage,
                mtr.time_taken,
                mtr.test_date,
                mtr.course_code
            FROM mcq_test_results mtr 
            WHERE mtr.student_id = :student_id 
            AND mtr.course_code = :course_code
            
            UNION ALL
            
            SELECT 
                'Theory' as test_type,
                ttr.test_result_id as id,
                ttr.total_score as total_score,
                ttr.total_questions,
                ttr.percentage,
                NULL as time_taken,
                ttr.test_date,
                ttr.course_code
            FROM theory_test_results ttr 
            WHERE ttr.student_id = :student_id 
            AND ttr.course_code = :course_code
            
            ORDER BY test_date DESC";
    
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':student_id', $student_id, PDO::PARAM_INT);
    $stmt->bindParam(':course_code', $course_code, PDO::PARAM_STR);
    $stmt->execute();
    
    $test_history = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Debug logging
    error_log("get_combined_test_history.php - Found " . count($test_history) . " test records");
    
    // Calculate statistics
    $total_tests = count($test_history);
    $total_score = 0;
    $best_score = 0;
    
    foreach ($test_history as $test) {
        $total_score += $test['percentage'];
        if ($test['percentage'] > $best_score) {
            $best_score = $test['percentage'];
        }
    }
    
    $average_score = $total_tests > 0 ? round($total_score / $total_tests, 2) : 0;
    
    echo json_encode([
        'success' => true,
        'test_history' => $test_history,
        'statistics' => [
            'total_tests' => $total_tests,
            'average_score' => $average_score,
            'best_score' => $best_score
        ]
    ]);
    
} catch(PDOException $e) {
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>