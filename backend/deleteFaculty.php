<?php
header("Content-Type: application/json");
include('db.php');

if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Connection failed"]);
    exit();
}

$login_id = $_GET['login_id'] ?? '';

if (empty($login_id)) {
    echo json_encode(["success" => false, "message" => "Faculty ID missing"]);
    exit();
}

$sql = "DELETE FROM faculty_new WHERE login_id = ?";
$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $login_id);

if ($stmt->execute()) {
    if ($stmt->affected_rows > 0) {
        echo json_encode(["success" => true, "message" => "Faculty deleted successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "No faculty found with given ID"]);
    }
} else {
    echo json_encode(["success" => false, "message" => "Query failed"]);
}

$stmt->close();
$conn->close();
?>
