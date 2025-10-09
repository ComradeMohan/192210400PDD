<?php
// Database configuration file
// Update these credentials according to your database setup

$servername = "localhost";  // or your database server IP
$username = "your_username";  // your database username
$password = "your_password";  // your database password
$dbname = "univault_db";     // your database name

try {
    // Create PDO connection
    $pdo = new PDO("mysql:host=$servername;dbname=$dbname;charset=utf8mb4", $username, $password);
    
    // Set PDO attributes for better error handling and security
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    $pdo->setAttribute(PDO::ATTR_EMULATE_PREPARES, false);
    
} catch (PDOException $e) {
    // Log the error (don't expose sensitive information in production)
    error_log("Database connection failed: " . $e->getMessage());
    
    // Return generic error to client
    echo json_encode([
        'success' => false,
        'error' => 'Database connection failed. Please try again later.'
    ]);
    exit;
}
?>