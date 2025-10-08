<?php
$host = "localhost";
$user = "root";
$pass = ""; // your MySQL password
$db = "univault_db2"; // replace with your DB

// Establish MySQLi connection (existing usage across the codebase)
$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    die(json_encode(["success" => false, "message" => "Database connection failed."]));
}

// Optional: Provide a PDO connection as well for files using PDO
try {
    $pdo = new PDO("mysql:host=$host;dbname=$db;charset=utf8", $user, $pass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
} catch (Exception $e) {
    $pdo = null; // Fallback if PDO is not needed by the caller
}

// Define BASE_URL once for the whole app (with trailing slash)
if (!defined('BASE_URL')) {
    define('BASE_URL', 'http://14.139.187.229:8081/univault/');
}

if (!function_exists('base_url')) {
    function base_url(string $path = ''): string {
        return BASE_URL . ltrim($path, '/');
    }
}
?>
