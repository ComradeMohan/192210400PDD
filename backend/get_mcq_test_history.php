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
$student_id = isset($_GET['student_id']) ? (int)$_GET['student_id'] : 0;
$course_id = isset($_GET['course_id']) ? (int)$_GET['course_id'] : 0;

// Validate parameters
if ($student_id <= 0) {
    echo json_encode(['error' => 'Invalid student ID']);
    exit;
}

try {
    // Build query based on parameters
    $sql = "SELECT tr.test_result_id, tr.course_id, tr.score, tr.total_questions, 
                   tr.time_taken, tr.test_date, c.course_name
            FROM mcq_test_results tr
            LEFT JOIN prepcourses c ON tr.course_id = c.course_id
            WHERE tr.student_id = :student_id";
    
    $params = [':student_id' => $student_id];
    
    if ($course_id > 0) {
        $sql .= " AND tr.course_id = :course_id";
        $params[':course_id'] = $course_id;
    }
    
    $sql .= " ORDER BY tr.test_date DESC";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    
    $test_history = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Format the response
    $response = [
        'success' => true,
        'student_id' => $student_id,
        'total_tests' => count($test_history),
        'test_history' => []
    ];
    
    foreach ($test_history as $test) {
        $percentage = $test['total_questions'] > 0 ? 
                     round(($test['score'] / $test['total_questions']) * 100, 1) : 0;
        
        $response['test_history'][] = [
            'test_result_id' => (int)$test['test_result_id'],
            'course_id' => (int)$test['course_id'],
            'course_name' => $test['course_name'] ?: 'Unknown Course',
            'score' => (int)$test['score'],
            'total_questions' => (int)$test['total_questions'],
            'percentage' => $percentage,
            'time_taken' => (int)$test['time_taken'],
            'test_date' => $test['test_date']
        ];
    }
    
    echo json_encode($response);
    
} catch(PDOException $e) {
    echo json_encode(['error' => 'Database query failed: ' . $e->getMessage()]);
}
?>
