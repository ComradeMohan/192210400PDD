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

$college = $_GET['college'] ?? '';

$sql = "SELECT name FROM faculty_new WHERE college = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $college);
$stmt->execute();
$result = $stmt->get_result();

$facultyList = [];
while ($row = $result->fetch_assoc()) {
    $facultyList[] = $row['name'];
}

echo json_encode(["success" => true, "faculty" => $facultyList]);

$stmt->close();
$conn->close();
?>
