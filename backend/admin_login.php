<?php
header("Content-Type: application/json");
include("db.php");

// Reuse parsed input from router if available; avoid rereading php://input
if (!isset($data) || !is_array($data)) {
    $raw = file_get_contents("php://input");
    $data = $raw ? json_decode($raw, true) : null;
    if (!$data) { $data = $_POST; }
}

// Accept both keys for compatibility with different clients
$admin_id = $data['admin_id'] ?? ($data['student_number'] ?? '');
$password = $data['password'] ?? '';

if (!$admin_id || !$password) {
    echo json_encode(["success" => false, "message" => "Admin ID and password required."]);
    exit;
}

$stmt = $conn->prepare("SELECT id, name, password, college FROM admins WHERE admin_id = ?");
$stmt->bind_param("s", $admin_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 1) {
    $admin = $result->fetch_assoc();

    // WARNING: Plaintext password comparison – consider hashing in real projects
    if ($password === $admin['password']) {
        echo json_encode([
            "success" => true,
            "user_type" => "admin",
            "college" => $admin['college']
        ]);
        exit;
    } else {
        echo json_encode(["success" => false, "message" => "Invalid password."]);
        exit;
    }
} else {
    echo json_encode(["success" => false, "message" => "Admin not found."]);
    exit;
}

