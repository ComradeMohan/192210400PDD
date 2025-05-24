<?php
header("Content-Type: application/json");

$data = json_decode(file_get_contents("php://input"), true);
$student_number = $data["student_number"] ?? '';
$password = $data["password"] ?? '';

if (strpos($student_number, "19") === 0) {
    include("student_login.php");
} else if (stripos($student_number, "sse") === 0) {
    include("faculty_login.php");
} else if (stripos($student_number, "admin") === 0) {
    include("admin_login.php");
} else {
    echo json_encode(["success" => false, "message" => "Invalid user type"]);
}
?>
<?php
header("Content-Type: application/json");

$data = json_decode(file_get_contents("php://input"), true);
$student_number = $data["student_number"] ?? '';
$password = $data["password"] ?? '';

if (strpos($student_number, "19") === 0) {
    include("student_login.php");
} else if (stripos($student_number, "SSE") === 0) {
    include("faculty_login.php");
} else if (stripos($student_number, "admin") === 0) {
    include("admin_login.php");
} else {
    echo json_encode(["success" => false, "message" => "Invalid user type"]);
}
?>
