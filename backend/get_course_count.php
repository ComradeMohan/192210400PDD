<?php
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");

require 'db.php'; // this defines $conn

try {
    $result = $conn->query("SELECT COUNT(*) AS total_courses FROM prepcourses");
    $row = $result->fetch_assoc();

    echo json_encode([
        "success" => true,
        "total_courses" => (int)$row['total_courses']
    ]);
} catch (Exception $e) {
    echo json_encode([
        "success" => false,
        "error" => $e->getMessage()
    ]);
}
