<?php
header("Content-Type: application/json");

$host = "localhost";
$user = "root";
$pass = "";
$db = "univalut_db"; // Same DB used in your registration script

$conn = new mysqli($host, $user, $pass, $db);

// Check connection
if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Connection failed"]);
    exit();
}

// Get data (either from GET or JSON body)
$data = json_decode(file_get_contents("php://input"), true);

$student_number = $_GET['student_number'] ?? ($data['student_number'] ?? null);
$email = $_GET['email'] ?? ($data['email'] ?? null);

// Ensure at least one identifier is provided
if (!$student_number && !$email) {
    echo json_encode(["success" => false, "message" => "student_number or email is required"]);
    $conn->close();
    exit();
}

// Build dynamic WHERE clause
$whereClause = '';
$params = [];
$types = '';

if ($student_number) {
    $whereClause .= "student_number = ?";
    $params[] = $student_number;
    $types .= 's';
}

if ($email) {
    if ($whereClause) $whereClause .= " OR ";
    $whereClause .= "email = ?";
    $params[] = $email;
    $types .= 's';
}

$sql = "SELECT full_name, student_number, email, department, year_of_study, college FROM students_new WHERE $whereClause";
$stmt = $conn->prepare($sql);
$stmt->bind_param($types, ...$params);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    echo json_encode(["success" => true, "data" => $result->fetch_assoc()]);
} else {
    echo json_encode(["success" => false, "message" => "Student not found"]);
}

$stmt->close();
$conn->close();
?>
