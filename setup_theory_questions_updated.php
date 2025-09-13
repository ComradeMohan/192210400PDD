<?php
/**
 * Setup script for updated theory questions database tables
 * Run this script once to create the necessary tables and sample data
 * This version matches your existing table structure
 */

// Database configuration
$host = 'localhost';
$dbname = 'univault';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "Connected to database successfully.\n";
    
    // Read and execute the SQL file
    $sqlFile = 'create_theory_questions_updated.sql';
    if (file_exists($sqlFile)) {
        $sql = file_get_contents($sqlFile);
        
        // Split SQL into individual statements
        $statements = array_filter(array_map('trim', explode(';', $sql)));
        
        foreach ($statements as $statement) {
            if (!empty($statement)) {
                try {
                    $pdo->exec($statement);
                    echo "Executed: " . substr($statement, 0, 50) . "...\n";
                } catch (PDOException $e) {
                    echo "Warning: " . $e->getMessage() . "\n";
                }
            }
        }
        
        echo "\nUpdated theory questions setup completed successfully!\n";
        echo "Features included:\n";
        echo "- Keywords field for better question categorization\n";
        echo "- Complete answer field for reference\n";
        echo "- Marks allocation for each question\n";
        echo "- Difficulty levels (easy, medium, hard)\n";
        echo "- Enhanced test results tracking\n";
        echo "\nYou can now use the updated theory questions feature in the app.\n";
        
    } else {
        echo "Error: SQL file 'create_theory_questions_updated.sql' not found.\n";
    }
    
} catch (PDOException $e) {
    echo "Database connection failed: " . $e->getMessage() . "\n";
    echo "Please make sure:\n";
    echo "1. MySQL server is running\n";
    echo "2. Database 'univault' exists\n";
    echo "3. Username and password are correct\n";
}
?>
