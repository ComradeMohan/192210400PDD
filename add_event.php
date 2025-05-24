<?php
// Database connection
$servername = "localhost";  // XAMPP default localhost
$username = "root";         // XAMPP default username
$password = "";             // XAMPP default password (empty)
$dbname = "univalut";       // Your database name

$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

$title = $_POST['title'];
$type = $_POST['type'];
$startDate = $_POST['startDate'];
$endDate = $_POST['endDate'];
$description = $_POST['description'];

$sql = "INSERT INTO events (title, type, startDate, endDate, description) VALUES ('$title', '$type', '$startDate', '$endDate', '$description')";

if ($conn->query($sql) === TRUE) {
    echo json_encode(array("success" => true, "message" => "Event added successfully"));
} else {
    echo json_encode(array("success" => false, "message" => "Error: " . $conn->error));
}

$conn->close();
?>
