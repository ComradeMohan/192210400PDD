<?php
include('db.php');

$data = json_decode(file_get_contents("php://input"), true);
$email = $data['email'] ?? '';
$user_id = $data['user_id'] ?? ''; // Google ID
$name = $data['name'] ?? '';

$email = $conn->real_escape_string($email);

// Look for the student in DB by email
$sql = "SELECT student_number, college FROM students_new WHERE email = '$email' LIMIT 1";
$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
    $row = $result->fetch_assoc();
    $student_number = $row['student_number'];
    $college = $row['college'];

    // Optional: update name or ID if needed
    // $conn->query("UPDATE students_new SET full_name = '$name' WHERE email = '$email'");

    echo json_encode([
        "success" => true,
        "student_number" => $student_number,
        "college" => $college
    ]);
} else {
    echo json_encode(["success" => false, "message" => "Student email not found"]);
}

$conn->close();
?>
