<?php
header('Content-Type: application/json');
error_reporting(0);
ini_set('display_errors', 0);

// DB connection
include('db.php');

// Get POST data
$departmentId = isset($_POST['department_id']) ? $_POST['department_id'] : null;

if (!$departmentId) {
    echo json_encode(["success" => false, "message" => "Department ID missing"]);
    exit();
}

// Delete query
$stmt = $conn->prepare("DELETE FROM departments_new WHERE id = ?");
$stmt->bind_param("s", $departmentId);

if ($stmt->execute()) {
    echo json_encode(["success" => true, "message" => "Department deleted"]);
} else {
    echo json_encode(["success" => false, "message" => "Failed to delete department"]);
}

$stmt->close();
$conn->close();
