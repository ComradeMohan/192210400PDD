<?php
include('db.php');

// Check if course_id is provided
if (!isset($_POST['course_id']) || empty($_POST['course_id'])) {
    echo json_encode([
        "success" => false,
        "message" => "Course ID is required."
    ]);
    exit();
}

$course_id = $conn->real_escape_string($_POST['course_id']);

// Prepare and execute delete query
$sql = "DELETE FROM courses_new WHERE id = '$course_id'";

if ($conn->query($sql) === TRUE) {
    if ($conn->affected_rows > 0) {
        echo json_encode([
            "success" => true,
            "message" => "Course deleted successfully."
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "No course found with the given ID."
        ]);
    }
} else {
    echo json_encode([
        "success" => false,
        "message" => "Error deleting course: " . $conn->error
    ]);
}

$conn->close();
?>
