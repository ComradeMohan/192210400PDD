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
    
    // Check if courses table exists
    $stmt = $pdo->query("SHOW TABLES LIKE 'courses'");
    $response['checks']['courses_exists'] = $stmt->rowCount() > 0;
    
    if ($response['checks']['courses_exists']) {
        // Get table structure
        $stmt = $pdo->query("DESCRIBE courses");
        $response['checks']['courses_structure'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Get sample data
        $stmt = $pdo->query("SELECT * FROM courses LIMIT 3");
        $response['checks']['courses_sample'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Check if mcq_test_results table exists
    $stmt = $pdo->query("SHOW TABLES LIKE 'mcq_test_results'");
    $response['checks']['mcq_test_results_exists'] = $stmt->rowCount() > 0;
    
    if ($response['checks']['mcq_test_results_exists']) {
        // Get table structure
        $stmt = $pdo->query("DESCRIBE mcq_test_results");
        $response['checks']['mcq_test_results_structure'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Get sample data
        $stmt = $pdo->query("SELECT * FROM mcq_test_results LIMIT 3");
        $response['checks']['mcq_test_results_sample'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    echo json_encode($response, JSON_PRETTY_PRINT);
    
} catch(PDOException $e) {
    echo json_encode(['error' => 'Database error: ' . $e->getMessage()]);
}
?>
