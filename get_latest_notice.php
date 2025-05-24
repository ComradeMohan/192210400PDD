<?php
header('Content-Type: application/json');

// DB connection
$conn = new mysqli("localhost", "root", "", "univalut_db");
if ($conn->connect_error) {
    die(json_encode(["success" => false, "message" => "Connection failed."]));
}

$college = isset($_GET['college']) ? $_GET['college'] : '';

if (empty($college)) {
    echo json_encode(["success" => false, "message" => "College parameter missing"]);
    exit;
}

$sql = "SELECT title, description FROM notices WHERE college = ? ORDER BY id DESC LIMIT 1";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $college);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    echo json_encode(["success" => true, "title" => $row['title'], "description" => $row['description']]);
} else {
    echo json_encode(["success" => false, "message" => "No notices found"]);
}

$conn->close();
?>
