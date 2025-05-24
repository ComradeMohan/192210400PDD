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

$facultyId = $_GET['facultyId']; // Get student ID from URL

// Fetch student data by ID
$sql = "SELECT name,college FROM faculty_new WHERE login_id = '$facultyId'";
$result = $conn->query($sql);

if ($result->num_rows > 0) {
    $faculty_name = $result->fetch_assoc();
    echo json_encode(["success" => true, "college" => $faculty_name["college"],"name" => $faculty_name["name"]]);
} else {
    echo json_encode(["success" => false, "message" => "Student not found"]);
}

$conn->close();
?>
