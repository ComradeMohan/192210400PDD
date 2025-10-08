<?php
header("Content-Type: application/json");

include('db.php');

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
