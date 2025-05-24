<?php
// Set header to allow cross-origin requests (optional, if needed)
header('Access-Control-Allow-Origin: *');
header('Content-Type: application/json');

// Database connection parameters
$host = "localhost";
$user = "root";
$pass = "";
$db = "univalut_db";

$conn = new mysqli($host, $user, $pass, $db);

// Check if the connection was successful
if ($conn->connect_error) {
    die(json_encode(array("success" => false, "message" => "Connection failed: " . $conn->connect_error)));
}

// Get the college_name parameter from the GET request
$college_name = $_GET['college_name'] ?? "";

// Query to fetch notifications for the specific college using placeholder
$query = "SELECT title, description, schedule_date FROM notices WHERE college= ?";

// Prepare the statement
$stmt = $conn->prepare($query);
if (!$stmt) {
    die(json_encode(array("success" => false, "message" => "Prepare failed: " . $conn->error)));
}

// Bind the parameter
$stmt->bind_param("s", $college_name);

// Execute the query
$stmt->execute();

// Get the result
$result = $stmt->get_result();

// Check if any rows were returned
if ($result->num_rows > 0) {
    $notifications = array();

    while ($row = $result->fetch_assoc()) {
        $notifications[] = array(
            "title" => $row['title'],
            "description" => $row['description'],
            "date" => $row['schedule_date'] // make sure column name is correct
        );
    }

    echo json_encode(array("success" => true, "notifications" => $notifications));
} else {
    echo json_encode(array("success" => false, "message" => "No notifications found for this college"));
}

// Close the connection
$conn->close();
?>
