<?php
$college_id = $_GET['college_id'];

include('db.php');

if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

$sql = "SELECT grade, points FROM grades WHERE college_id = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("i", $college_id);
$stmt->execute();
$result = $stmt->get_result();

$grades = array();
while ($row = $result->fetch_assoc()) {
    $grades[] = $row;
}

echo json_encode(["grades" => $grades]);
?>
