<?php
header("Content-Type: application/json");

include('db.php');
$college = $_GET['college'] ?? '';

$sql = "SELECT name, login_id FROM faculty_new WHERE college = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $college);
$stmt->execute();
$result = $stmt->get_result();

$facultyList = [];
while ($row = $result->fetch_assoc()) {
    $facultyList[] = [
        "name" => $row['name'],
        "login_id" => $row['login_id']
    ];
}

echo json_encode(["success" => true, "faculty" => $facultyList]);

$stmt->close();
$conn->close();
?>
