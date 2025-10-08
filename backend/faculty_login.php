<?php
header("Content-Type: application/json");
include('db.php');

// Reuse parsed input from router if available
if (!isset($data) || !is_array($data)) {
    $raw = file_get_contents("php://input");
    $data = $raw ? json_decode($raw, true) : null;
    if (!$data) { $data = $_POST; }
}

// Accept both login_id and student_number for compatibility
$login_id = isset($data['login_id']) ? $data['login_id'] : ($data['student_number'] ?? '');
$password = $data['password'] ?? '';

if (!$login_id || !$password) {
    echo json_encode(["success" => false, "message" => "Login ID and password required."]);
    exit();
}

$login_id = $conn->real_escape_string($login_id);
$password = $conn->real_escape_string($password);

$sql = "SELECT college FROM faculty_new WHERE login_id = ? AND password = ? LIMIT 1";
$stmt = $conn->prepare($sql);
$stmt->bind_param("ss", $login_id, $password);
$stmt->execute();
$result = $stmt->get_result();

if ($result && $result->num_rows > 0) {
    $row = $result->fetch_assoc();
    echo json_encode([
        "success" => true,
        "user_type" => "faculty",
        "college" => $row['college']
    ]);
} else {
    echo json_encode(["success" => false, "message" => "Invalid faculty credentials"]);
}

$stmt->close();
$conn->close();
