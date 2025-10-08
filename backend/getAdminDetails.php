<?php
$admin_id = $_GET['admin_id'];

$servername = "localhost";
$username = "root";
$password = "";
$dbname = "univault_db";

$conn = new mysqli($servername, $username, $password, $dbname);
if ($conn->connect_error) {
    die(json_encode(["success" => false, "message" => "DB Connection failed."]));
}

// Step 1: Fetch admin name & college
$sql = "SELECT name, college FROM admins WHERE admin_id = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $admin_id);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    $adminName = $row['name'];
    $collegeName = $row['college'];

    // Step 2: Fetch student count for the college
    $stmt2 = $conn->prepare("SELECT COUNT(*) as total_students FROM students_new WHERE college = ?");
    $stmt2->bind_param("s", $collegeName);
    $stmt2->execute();
    $result2 = $stmt2->get_result();
    $count = $result2->fetch_assoc()['total_students'];


    $stmt3 = $conn->prepare("SELECT COUNT(*) as total_faculties FROM faculty_new WHERE college = ?");
    $stmt3->bind_param("s", $collegeName);
    $stmt3->execute();
    $result3 = $stmt3->get_result();
    $count1 = $result3->fetch_assoc()['total_faculties'];

    echo json_encode([
        "success" => true,
        "name" => $adminName,
        "college" => $collegeName,
        "student_count" => $count,
        "faculty_count" => $count1
    ]);
} else {
    echo json_encode(["success" => false, "message" => "Admin not found"]);
}

$conn->close();
?>
