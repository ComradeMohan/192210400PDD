<?php
header("Content-Type: application/json");

$host = "localhost";
$user = "root";
$pass = "";
$db = "univalut_db";

$conn = new mysqli($host, $user, $pass, $db);

if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Connection failed"]);
    exit();
}

$data = json_decode(file_get_contents("php://input"), true);

// Get the course data from the request
$course_code = $conn->real_escape_string($data["course_code"]);
$subject_name = $conn->real_escape_string($data["subject_name"]);
$faculty_name = $conn->real_escape_string($data["faculty"]);
$strength = intval($data["strength"]);
$college = $conn->real_escape_string($data["college"]);

// Prepare SQL to insert the course
$sql = "INSERT INTO courses (course_code, subject_name, faculty_name, strength, college)
        VALUES ('$course_code', '$subject_name', '$faculty_name', $strength, '$college')";

// Execute the query
if ($conn->query($sql) === TRUE) {
    echo json_encode(["success" => true, "message" => "Course added successfully"]);
} else {
    echo json_encode(["success" => false, "message" => "Error: " . $conn->error]);
}

$conn->close();
?>
