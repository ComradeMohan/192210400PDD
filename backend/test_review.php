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
    
    // Check if tables exist
    $tables = ['mcq_test_results', 'mcq_test_answers', 'mcq_combined', 'courses'];
    foreach ($tables as $table) {
        $stmt = $pdo->query("SHOW TABLES LIKE '$table'");
        $response['checks'][$table . '_exists'] = $stmt->rowCount() > 0;
    }
    
    // Check if there are any test results
    if ($response['checks']['mcq_test_results_exists']) {
        $stmt = $pdo->query("SELECT COUNT(*) as count FROM mcq_test_results");
        $response['checks']['test_results_count'] = $stmt->fetch()['count'];
        
        // Get the latest test result
        if ($response['checks']['test_results_count'] > 0) {
            $stmt = $pdo->query("SELECT * FROM mcq_test_results ORDER BY test_result_id DESC LIMIT 1");
            $response['checks']['latest_test_result'] = $stmt->fetch(PDO::FETCH_ASSOC);
        }
    }
    
    // Check if there are any test answers
    if ($response['checks']['mcq_test_answers_exists']) {
        $stmt = $pdo->query("SELECT COUNT(*) as count FROM mcq_test_answers");
        $response['checks']['test_answers_count'] = $stmt->fetch()['count'];
    }
    
    // Check if there are questions
    if ($response['checks']['mcq_combined_exists']) {
        $stmt = $pdo->query("SELECT COUNT(*) as count FROM mcq_combined");
        $response['checks']['questions_count'] = $stmt->fetch()['count'];
    }
    
    echo json_encode($response, JSON_PRETTY_PRINT);
    
} catch(PDOException $e) {
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
