<?php
/**
 * Fix database columns for theory questions
 * Run this script to add missing columns to existing tables
 */

// Database configuration
$host = 'localhost';
$dbname = 'univault_db';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "Connected to database successfully.\n";
    
    // Add missing columns to theory_test_results table
    echo "Adding columns to theory_test_results table...\n";
    
    try {
        $pdo->exec("ALTER TABLE theory_test_results ADD COLUMN total_score INT DEFAULT 0 AFTER total_marks");
        echo "✅ Added total_score column\n";
    } catch (PDOException $e) {
        if (strpos($e->getMessage(), 'Duplicate column name') !== false) {
            echo "⚠️ total_score column already exists\n";
        } else {
            echo "❌ Error adding total_score: " . $e->getMessage() . "\n";
        }
    }
    
    try {
        $pdo->exec("ALTER TABLE theory_test_results ADD COLUMN percentage DECIMAL(5,2) DEFAULT 0.00 AFTER answered_questions");
        echo "✅ Added percentage column\n";
    } catch (PDOException $e) {
        if (strpos($e->getMessage(), 'Duplicate column name') !== false) {
            echo "⚠️ percentage column already exists\n";
        } else {
            echo "❌ Error adding percentage: " . $e->getMessage() . "\n";
        }
    }
    
    // Add missing columns to theory_test_answers table
    echo "\nAdding columns to theory_test_answers table...\n";
    
    try {
        $pdo->exec("ALTER TABLE theory_test_answers ADD COLUMN score_obtained INT DEFAULT 0 AFTER marks_allocated");
        echo "✅ Added score_obtained column\n";
    } catch (PDOException $e) {
        if (strpos($e->getMessage(), 'Duplicate column name') !== false) {
            echo "⚠️ score_obtained column already exists\n";
        } else {
            echo "❌ Error adding score_obtained: " . $e->getMessage() . "\n";
        }
    }
    
    try {
        $pdo->exec("ALTER TABLE theory_test_answers ADD COLUMN keywords_matched TEXT AFTER score_obtained");
        echo "✅ Added keywords_matched column\n";
    } catch (PDOException $e) {
        if (strpos($e->getMessage(), 'Duplicate column name') !== false) {
            echo "⚠️ keywords_matched column already exists\n";
        } else {
            echo "❌ Error adding keywords_matched: " . $e->getMessage() . "\n";
        }
    }
    
    echo "\n✅ Database columns updated successfully!\n";
    echo "You can now submit theory questions without errors.\n";
    
} catch (PDOException $e) {
    echo "❌ Database connection failed: " . $e->getMessage() . "\n";
    echo "Please make sure:\n";
    echo "1. MySQL server is running\n";
    echo "2. Database 'univault_db' exists\n";
    echo "3. Username and password are correct\n";
}
?>
