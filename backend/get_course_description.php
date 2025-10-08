<?php
header("Content-Type: application/json");
include 'db.php'; // DB connection

// Check if course_code is provided
if (!isset($_GET['course_code'])) {
    echo json_encode(["error" => "course_code is required"]);
    exit;
}

$course_code = $_GET['course_code']; // keep as string

$sql = "SELECT course_code, course_name, description, created_at 
        FROM prepcourses 
        WHERE course_code = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $course_code); // "s" = string
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    $course = $result->fetch_assoc();
    echo json_encode($course, JSON_PRETTY_PRINT);
} else {
    echo json_encode(["error" => "Course not found"]);
}

$stmt->close();
$conn->close();
?>
