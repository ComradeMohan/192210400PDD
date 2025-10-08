<?php

// DB connection
include('db.php');

$course_id = $_GET['course_id'];

if (!$course_id) {
    echo json_encode(["success" => false, "message" => "Missing course_id"]);
    exit;
}

$query = "DELETE FROM courses WHERE course_code = ?";
$stmt = $conn->prepare($query);
$stmt->bind_param("s", $course_id);

if ($stmt->execute()) {
    echo json_encode(["success" => true]);
} else {
    echo json_encode(["success" => false, "message" => "Failed to delete course"]);
}
?>
