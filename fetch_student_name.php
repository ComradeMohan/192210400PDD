<?php
header("Content-Type: application/json");

$host = "localhost";
$user = "root";
$pass = "";
$db = "univalut_db"; // Your database name

$conn = new mysqli($host, $user, $pass, $db);

if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Connection failed"]);
    exit();
}

$student_id = $_GET['studentID']; // Get student ID from URL

// Fetch student data by ID
$sql = "SELECT full_name,college,department FROM students_new WHERE student_number = '$student_id'";
$result = $conn->query($sql);

if ($result->num_rows > 0) {
    $student = $result->fetch_assoc();
    echo json_encode(["success" => true, "name" => $student["full_name"], "college" => $student["college"], "dept" => $student["department"]]);    
} else {
    echo json_encode(["success" => false, "message" => "Student not found"]);
}

$conn->close();
?>
