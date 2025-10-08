<?php
header("Content-Type: application/json");
include('db.php');

if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Connection failed"]);
    exit();
}

$college = $_POST['college'] ?? ''; // POST instead of GET to match Kotlin

$sql = "SELECT id, name FROM faculty_new WHERE college = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $college);
$stmt->execute();
$result = $stmt->get_result();

$facultyList = [];
while ($row = $result->fetch_assoc()) {
    $facultyList[] = [
        "id" => $row['id'],
        "faculty_name" => $row['name']
    ];
}

echo json_encode(["success" => true, "faculties" => $facultyList]);

$stmt->close();
$conn->close();
?>
