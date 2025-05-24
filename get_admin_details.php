<?php
// Database connection
$servername = "localhost";
$username = "root"; // change to your database username
$password = ""; // change to your database password
$dbname = "univalut_db"; // change to your database name

// Create connection
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Get the admin ID from the GET request
$adminId = $_GET['admin_id']; // assuming the admin_id is passed as a GET parameter

// Check if admin_id is provided
if (empty($adminId)) {
    echo json_encode(["error" => "Admin ID is required"]);
    exit();
}

// Query to fetch admin details by ID
$sql = "SELECT * FROM admins WHERE admin_id = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $adminId); // bind adminId to the query

$stmt->execute();
$result = $stmt->get_result();

// Check if the admin exists
if ($result->num_rows > 0) {
    // Fetch the admin details
    $admin = $result->fetch_assoc();
    
    // Set default values for missing fields if they are NULL
    
    // Return the admin details as JSON
    echo json_encode([
        "id" => $admin["id"],
        "name" => $admin["name"],
        "email" => $admin["email"],
        "phone_number" => $admin["phone_number"],
        "employee_id" => $adminId,
        "created_at" => $admin["created_at"],
        "college" => $admin["college"]
    ]);
} else {
    echo json_encode(["error" => "Admin not found"]);
}

// Close the database connection
$stmt->close();
$conn->close();
?>
