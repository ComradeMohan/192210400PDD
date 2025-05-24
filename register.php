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

$data = json_decode(file_get_contents("php://input"), true);

$full_name = $conn->real_escape_string($data["full_name"]);
$student_number = $conn->real_escape_string($data["student_number"]);
$email = $conn->real_escape_string($data["email"]);
$password = $conn->real_escape_string($data["password"]);
$department = $conn->real_escape_string($data["department"]);
$year_of_study = $conn->real_escape_string($data["year_of_study"]);
$college = $conn->real_escape_string($data["college"]);

// Hash the password before storing it

// Check if the student already exists in the database
$sql_check = "SELECT * FROM students_new WHERE student_number = '$student_number' OR email = '$email'";
$result = $conn->query($sql_check);

if ($result->num_rows > 0) {
    echo json_encode(["success" => false, "message" => "Student already exists"]);
    $conn->close();
    exit();
}

// Insert new student record into the database
$sql = "INSERT INTO students_new (full_name, student_number, email, password, department, year_of_study, college)
        VALUES ('$full_name', '$student_number', '$email', '$password', '$department', '$year_of_study', '$college')";

if ($conn->query($sql) === TRUE) {
    echo json_encode(["success" => true, "message" => "Registration successful"]);
} else {
    echo json_encode(["success" => false, "message" => "Error: " . $conn->error]);
}

$conn->close();
?>
