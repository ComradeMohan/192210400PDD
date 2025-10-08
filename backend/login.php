<?php
header("Content-Type: application/json");

$data = json_decode(file_get_contents("php://input"), true);
$student_number = $data["student_number"] ?? '';
$password = $data["password"] ?? '';

if (preg_match('/^[0-9]/', $student_number)) {
    include("student_login.php");
} elseif (stripos($student_number, "admin") === 0) {
    include("admin_login.php");
} elseif (preg_match('/^[a-zA-Z]+[0-9]+$/', $student_number)) {
    // Starts with letters (1 or more) + digits (1 or more) → faculty login
    include("faculty_login.php");
} else {
    echo json_encode(["success" => false, "message" => "Invalid user type"]);
}
?>