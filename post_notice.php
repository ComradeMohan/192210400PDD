<?php
// Database connection
$servername = "localhost";
$username = "root"; // Default XAMPP MySQL username
$password = ""; // Default XAMPP MySQL password (empty)
$dbname = "univalut_db";

// Create connection
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Get POST data
$title = isset($_POST['title']) ? $_POST['title'] : '';
$details = isset($_POST['details']) ? $_POST['details'] : '';
$college = isset($_POST['college']) ? $_POST['college'] : '';
$schedule_date = isset($_POST['schedule_date']) ? $_POST['schedule_date'] : null;
$schedule_time = isset($_POST['schedule_time']) ? $_POST['schedule_time'] : null;
$attachment = isset($_POST['attachment']) ? $_POST['attachment'] : null;
$is_high_priority = isset($_POST['is_high_priority']) ? $_POST['is_high_priority'] : false;

// Validate mandatory fields
if (empty($title) || empty($details)) {
    echo json_encode(["status" => "error", "message" => "Title and Details are mandatory!"]);
    exit;
}

// Insert into the database
$sql = "INSERT INTO notices (title, description, college, schedule_date, schedule_time, attachment, is_high_priority)
        VALUES ('$title', '$details', '$college', '$schedule_date', '$schedule_time', '$attachment', '$is_high_priority')";

if ($conn->query($sql) === TRUE) {
    echo json_encode(["status" => "success", "message" => "Notice posted successfully"]);
} else {
    echo json_encode(["status" => "error", "message" => "Error posting notice: " . $conn->error]);
}

$conn->close();
?>
