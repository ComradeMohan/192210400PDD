<?php
header('Content-Type: application/json');
include 'db.php';

// Read raw JSON input
$data = json_decode(file_get_contents('php://input'), true);

// Extract fields
$title = $data['title'] ?? '';
$type = $data['type'] ?? '';
$description = $data['description'] ?? '';
$start_date = $data['start_date'] ?? '';
$end_date = $data['end_date'] ?? '';
$college_name = $data['college_name'] ?? '';

// Validate inputs
if ($title && $type && $start_date && $end_date && $college_name) {
    $sql = "INSERT INTO events_new (title, type, description, start_date, end_date, college_name)
            VALUES ('$title', '$type', '$description', '$start_date', '$end_date', '$college_name')";

    if (mysqli_query($conn, $sql)) {
        echo json_encode(["status" => "success", "message" => "Event added successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => mysqli_error($conn)]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Missing required fields"]);
}

mysqli_close($conn);

?>
