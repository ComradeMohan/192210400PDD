<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');
include('db.php');
if ($conn->connect_error) {
    die(json_encode(array("status" => "error", "message" => "Connection failed")));
}

$title = $_POST['title'] ?? "";

if (empty($title)) {
    echo json_encode(array("status" => "error", "message" => "Title is required"));
    exit;
}

$stmt = $conn->prepare("DELETE FROM notices WHERE title = ?");
$stmt->bind_param("s", $title);

if ($stmt->execute()) {
    echo json_encode(array("status" => "success", "message" => "Notice deleted successfully"));
} else {
    echo json_encode(array("status" => "error", "message" => "Failed to delete notice"));
}

$stmt->close();
$conn->close();
?>
