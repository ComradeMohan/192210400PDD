<?php
header("Content-Type: application/json");
include('db.php'); // Your database connection

$college = $_GET['college'] ?? '';

if (!empty($college)) {
    // Filter feedbacks by college
    $stmt = $conn->prepare("SELECT user_id, feedback, created_at FROM feedbacks WHERE college = ? ORDER BY created_at DESC");
    $stmt->bind_param("s", $college);
    $stmt->execute();
    $result = $stmt->get_result();
} else {
    // Get all feedbacks
    $sql = "SELECT user_id, feedback, created_at FROM feedbacks ORDER BY created_at DESC";
    $result = $conn->query($sql);
}

$feedbacks = [];

if ($result && $result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        $feedbacks[] = $row;
    }
    echo json_encode(["success" => true, "data" => $feedbacks]);
} else {
    echo json_encode(["success" => false, "data" => []]);
}

$conn->close();
?>
