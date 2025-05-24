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
$facultyId = $conn->real_escape_string($data["faculty_id"]);

$sql = "SELECT college FROM faculty_new WHERE login_id = '$facultyId'";
$result = $conn->query($sql);

if ($result && $row = $result->fetch_assoc()) {
    echo json_encode(["success" => true, "college" => $row["college"]]);
} else {
    echo json_encode(["success" => false, "message" => "Faculty not found"]);
}

$conn->close();
?>
