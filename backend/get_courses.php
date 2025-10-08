<?php

include('db.php'); // Make sure $conn is defined here

// Optionally filter by department_id
$department_id = isset($_GET['department_id']) ? $_GET['department_id'] : null;

if ($department_id) {
    $stmt = $conn->prepare("SELECT id, department_id, name, credits FROM courses_new WHERE department_id = ?");
    $stmt->bind_param("s", $department_id);
} else {
    $stmt = $conn->prepare("SELECT id, department_id, name, credits FROM courses_new");
}

$stmt->execute();
$result = $stmt->get_result();

$courses = [];
while ($row = $result->fetch_assoc()) {
    $courses[] = $row;
}

if (count($courses) > 0) {
    echo json_encode([
        "success" => true,
        "data" => $courses
    ]);
} else {
    echo json_encode([
        "success" => false,
        "message" => "No courses found"
    ]);
}

$stmt->close();
$conn->close();
