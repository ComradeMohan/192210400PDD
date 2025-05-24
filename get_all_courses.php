<?php
include('db.php');

// Debug: Show incoming POST data

$department_id = $_POST['department_id'];
$response = [];

if ($department_id) {
    $sql = "SELECT * FROM courses_new WHERE department_id = '$department_id'";
    $result = $conn->query($sql);

    while ($row = $result->fetch_assoc()) {
        $response[] = $row;
    }
}

// Important: Output JSON only after debug
header('Content-Type: application/json');
echo json_encode($response);
?>
