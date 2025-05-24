<?php
// Database connection parameters
include 'db.php'; // Include your database configuration file
// Check the connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Get the college_id from the request (GET or POST)
$college_id = isset($_GET['college_id']) ? $_GET['college_id'] : die('College ID not provided');

// SQL query to fetch grades for the given college_id
$sql = "SELECT * FROM grades WHERE college_id = $college_id";
$result = $conn->query($sql);

// Check if there are results
if ($result->num_rows > 0) {
    // Create an array to hold the results
    $grades = array();

    // Fetch data and push to the grades array
    while ($row = $result->fetch_assoc()) {
        $grades[] = $row;
    }

    // Set the response header to application/json
    header('Content-Type: application/json');

    // Output the results as JSON
    echo json_encode($grades);
} else {
    // No records found, return an empty array
    echo json_encode(array());
}

// Close the connection
$conn->close();
?>
