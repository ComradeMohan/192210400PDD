<?php
header('Content-Type: application/json');

include('db.php');
// Check if college_id is provided
if (!isset($_GET['college_id'])) {
    echo json_encode([
        "success" => false,
        "message" => "Missing college_id"
    ]);
    exit;
}

$college_id = $conn->real_escape_string($_GET['college_id']);

// Query to get departments
$sql = "SELECT id, name FROM departments_new WHERE college_id = '$college_id'";
$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
    $departments = [];

    while ($row = $result->fetch_assoc()) {
        $departments[] = [
            "id" => $row['id'],
            "name" => $row['name']
        ];
    }

    echo json_encode([
        "success" => true,
        "departments" => $departments
    ]);
} else {
    echo json_encode([
        "success" => false,
        "message" => "No departments found"
    ]);
}

$conn->close();
?>
