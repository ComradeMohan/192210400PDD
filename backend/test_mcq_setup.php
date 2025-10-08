<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Database configuration
$host = 'localhost';
$dbname = 'univault_db';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    $response = ['success' => true, 'checks' => []];
    
    // Check if mcq_combined table exists
    $stmt = $pdo->query("SHOW TABLES LIKE 'mcq_combined'");
    $response['checks']['mcq_combined_exists'] = $stmt->rowCount() > 0;
    
    // Check if mcq_test_results table exists
    $stmt = $pdo->query("SHOW TABLES LIKE 'mcq_test_results'");
    $response['checks']['mcq_test_results_exists'] = $stmt->rowCount() > 0;
    
    // Check if mcq_test_answers table exists
    $stmt = $pdo->query("SHOW TABLES LIKE 'mcq_test_answers'");
    $response['checks']['mcq_test_answers_exists'] = $stmt->rowCount() > 0;
    
    // Count questions in mcq_combined
    if ($response['checks']['mcq_combined_exists']) {
        $stmt = $pdo->query("SELECT COUNT(*) as count FROM mcq_combined");
        $response['checks']['mcq_combined_count'] = $stmt->fetch()['count'];
    }
    
    // Test a simple query
    if ($response['checks']['mcq_combined_exists']) {
        $stmt = $pdo->query("SELECT question_id, question_text FROM mcq_combined LIMIT 1");
        $response['checks']['sample_question'] = $stmt->fetch(PDO::FETCH_ASSOC);
    }
    
    echo json_encode($response, JSON_PRETTY_PRINT);
    
} catch(PDOException $e) {
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
