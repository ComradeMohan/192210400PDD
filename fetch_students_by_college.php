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

// Get college from query parameter
if (!isset($_GET["college"])) {
    echo json_encode(["success" => false, "message" => "College name is required"]);
    $conn->close();
    exit();
}

$college = $conn->real_escape_string($_GET["college"]);

// Fetch students by college name
$sql = "SELECT full_name, student_number FROM students_new WHERE college = '$college'";
$result = $conn->query($sql);

if ($result->num_rows > 0) {
    $students = [];
    while ($row = $result->fetch_assoc()) {
        $students[] = [
            "full_name" => $row["full_name"],
            "student_number" => $row["student_number"]
        ];
    }
    echo json_encode(["success" => true, "students" => $students]);
} else {
    echo json_encode(["success" => false, "message" => "No students found for the given college"]);
}

$conn->close();
?>
