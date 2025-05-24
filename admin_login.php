<?php
header("Content-Type: application/json");
include("db.php");

$data = json_decode(file_get_contents("php://input"), true);
$admin_id = $data['student_number'] ?? ''; // same field used in main login
$password = $data['password'] ?? '';

if (!$admin_id || !$password) {
    echo json_encode(["success" => false, "message" => "Admin ID and password required."]);
    exit;
}

$stmt = $conn->prepare("SELECT id, name, password FROM admins WHERE admin_id = ?");
$stmt->bind_param("s", $admin_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 1) {
    $admin = $result->fetch_assoc();
    // Direct comparison (no hashing)
    if ($password === $admin['password']) {
        echo json_encode([
            "success" => true,
            "user_type" => "admin"
        ]);
    } else {
        echo json_encode(["success" => false, "message" => "Invalid password."]);
    }
} else {
    echo json_encode(["success" => false, "message" => "Admin not found."]);
}
?>
