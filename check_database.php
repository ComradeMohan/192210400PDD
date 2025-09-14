<?php
// Database check script
header('Content-Type: application/json');

$host = 'localhost';
$dbname = 'univault_db';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "Database connection successful\n\n";
    
    // Check if courses table exists and its structure
    $tables = $pdo->query("SHOW TABLES LIKE 'courses'")->fetchAll();
    if (empty($tables)) {
        echo "ERROR: 'courses' table does not exist\n";
    } else {
        echo "✓ 'courses' table exists\n";
        
        // Get table structure
        $columns = $pdo->query("DESCRIBE courses")->fetchAll();
        echo "Table structure:\n";
        foreach ($columns as $column) {
            echo "- " . $column['Field'] . " (" . $column['Type'] . ")\n";
        }
        
        // Check for sample data
        $sample = $pdo->query("SELECT * FROM courses LIMIT 3")->fetchAll();
        echo "\nSample data:\n";
        foreach ($sample as $row) {
            echo json_encode($row) . "\n";
        }
    }
    
    // Check if mcq_test_results table exists
    $tables = $pdo->query("SHOW TABLES LIKE 'mcq_test_results'")->fetchAll();
    if (empty($tables)) {
        echo "\nERROR: 'mcq_test_results' table does not exist\n";
    } else {
        echo "\n✓ 'mcq_test_results' table exists\n";
    }
    
    // Check if mcq_test_answers table exists
    $tables = $pdo->query("SHOW TABLES LIKE 'mcq_test_answers'")->fetchAll();
    if (empty($tables)) {
        echo "ERROR: 'mcq_test_answers' table does not exist\n";
    } else {
        echo "✓ 'mcq_test_answers' table exists\n";
    }
    
} catch(PDOException $e) {
    echo "Database error: " . $e->getMessage() . "\n";
}
?>
