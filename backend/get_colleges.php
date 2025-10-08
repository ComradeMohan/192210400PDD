<?php
header('Content-Type: application/json');
include('db.php');

if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Connection failed"]);
    exit;
}

$result = $conn->query("SELECT id, name FROM colleges");

$colleges = [];
while ($row = $result->fetch_assoc()) {
    $colleges[] = $row;
}

echo json_encode(["success" => true, "colleges" => $colleges]);
$conn->close();
?>
