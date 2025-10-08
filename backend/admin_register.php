<?php
header("Content-Type: application/json");
include("db.php"); // assumes db.php connects to MySQL

$data = json_decode(file_get_contents("php://input"), true);
$name = $data['name'] ?? '';
$phone = $data['phone'] ?? '';
$email = $data['email'] ?? '';
$admin_id = $data['admin_id'] ?? '';
$password = $data['password'] ?? '';

if (!$name || !$phone || !$email || !$admin_id || !$password) {
    echo json_encode(["success" => false, "message" => "All fields are required."]);
    exit;
}

// Hash password
$hashed_password = password_hash($password, PASSWORD_BCRYPT);

// Check if admin_id or email exists
$stmt = $conn->prepare("SELECT id FROM admins WHERE admin_id = ? OR email = ?");
$stmt->bind_param("ss", $admin_id, $email);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows > 0) {
    echo json_encode(["success" => false, "message" => "Admin ID or Email already exists."]);
    exit;
}

$stmt->close();

// Insert new admin
$stmt = $conn->prepare("INSERT INTO admins (name, phone_number, email, admin_id, password) VALUES (?, ?, ?, ?, ?)");
$stmt->bind_param("sssss", $name, $phone, $email, $admin_id, $password);

if ($stmt->execute()) {
    echo json_encode(["success" => true, "message" => "Admin registered successfully."]);
} else {
    echo json_encode(["success" => false, "message" => "Registration failed."]);
}
?>
