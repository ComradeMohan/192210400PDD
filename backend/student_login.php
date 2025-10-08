<?php
include('db.php');

$data = json_decode(file_get_contents("php://input"), true);
$student_number = $data['student_number'] ?? '';
$password = $data['password'] ?? '';

$student_number = $conn->real_escape_string($student_number);
$password = $conn->real_escape_string($password);

$sql = "SELECT * FROM students_new WHERE student_number = '$student_number' AND password = '$password' LIMIT 1";
$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
     $row = $result->fetch_assoc();
     if($row['verified'] == 1){
        echo json_encode([
            "success" => true,
            "user_type" => "student",
            "college" => $row['college']  // <-- send college here
        ]);
     }
    else{
    echo json_encode(["success" => false, "message" => "check ur college mail box to verify your mail "]);
    }
} else {
    echo json_encode(["success" => false, "message" => "Invalid student credentials"]);
}

$conn->close();
?>