<?php
$host = "localhost";
$user = "root";
$pass = ""; // your MySQL password
$db = "univalut_db"; // replace with your DB

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    die(json_encode(["success" => false, "message" => "Database connection failed."]));
}
?>
