<?php
// faculty_register.php
header("Content-Type: application/json");

$host = "localhost";
$user = "root";
$pass = "";
$db = "univalut_db"; // Change to your database name

$conn = new mysqli($host, $user, $pass, $db);

if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Connection failed"]);
    exit();
}

$data = json_decode(file_get_contents("php://input"), true);

// Get data from the request
$name = $conn->real_escape_string($data["name"]);
$email = $conn->real_escape_string($data["email"]);
$phone_number = $conn->real_escape_string($data["phone_number"]);
$college = $conn->real_escape_string($data["college"]);
$login_id = $conn->real_escape_string($data["login_id"]);
$password = $conn->real_escape_string($data["password"]);

// Insert query
$sql = "INSERT INTO faculty_new (name, email, phone_number, college, login_id, password)
        VALUES ('$name', '$email', '$phone_number', '$college', '$login_id', '$password')";

if ($conn->query($sql) === TRUE) {
    echo json_encode(["success" => true, "message" => "Faculty registration successful"]);
} else {
    echo json_encode(["success" => false, "message" => "Error: " . $conn->error]);
}

$conn->close();
?>
