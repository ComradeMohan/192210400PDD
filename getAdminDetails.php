<?php
// Get the admin_id from the URL
$admin_id = $_GET['admin_id'];

// Database connection
$servername = "localhost";  // Your database server
$username = "root";  // Your database username
$password = "";  // Your database password
$dbname = "univalut_db";  // Your database name

$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// SQL query to fetch admin details
$sql = "SELECT name, college FROM admins WHERE admin_id = '$admin_id'";
$result = $conn->query($sql);

// Check if admin exists
if ($result->num_rows > 0) {
    // Fetch the data
    $row = $result->fetch_assoc();
    $response = array(
        "name" => $row['name'],
        "college" => $row['college']
    );
    echo json_encode($response);
} else {
    echo json_encode(array("error" => "Admin not found"));
}

$conn->close();
?>
