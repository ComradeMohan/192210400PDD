<?php
header('Content-Type: application/json');
include('db.php');

// Check connection
if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Connection failed: " . $conn->connect_error]));
}

$title = $_POST['title'];
$type = $_POST['type'];
$startDate = $_POST['start_date'];
$endDate = $_POST['end_date'];
$description = $_POST['description'];
$collegeName = $_POST['college_name'];

// Check if event already exists
$checkSql = "SELECT * FROM events_new WHERE title='$title' AND start_date='$startDate' AND end_date='$endDate' AND college_name='$collegeName'";
$result = $conn->query($checkSql);

if ($result && $result->num_rows > 0) {
    echo json_encode(["status" => "error", "message" => "Event already exists"]);
    $conn->close();
    exit;
}

// Insert into the correct table
$sql = "INSERT INTO events_new (title, type, start_date, end_date, description, college_name) VALUES ('$title', '$type', '$startDate', '$endDate', '$description', '$collegeName')";

if ($conn->query($sql) === TRUE) {
    echo json_encode(["status" => "success", "message" => "Event added successfully"]);
} else {
    echo json_encode(["status" => "error", "message" => "Error: " . $conn->error]);
}

$conn->close();
?>
