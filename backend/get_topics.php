<?php
header("Content-Type: application/json");
include 'db.php'; // DB connection

// Check if course_code is provided
if (!isset($_GET['course_code'])) {
    echo json_encode(["error" => "course_code is required"]);
    exit;
}

$course_code = $_GET['course_code']; // string (like CSA57)
$mode = isset($_GET['mode']) ? strtolower($_GET['mode']) : "all"; // default all

// Base query
$sql = "SELECT topic_id, course_code, topic_name, content, difficulty
        FROM topics
        WHERE course_code = ?";

// Mode filtering
if ($mode === "pass") {
    $sql .= " AND difficulty IN ('easy', 'medium')";
} elseif ($mode === "master") {
    $sql .= " AND difficulty = 'hard'";
}
// if $mode = all → no extra filter

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $course_code); // "s" for string
$stmt->execute();
$result = $stmt->get_result();

$topics = [];
while ($row = $result->fetch_assoc()) {
    $topics[] = $row;
}

if (count($topics) > 0) {
    echo json_encode($topics, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode(["error" => "No topics found for this course"]);
}

$stmt->close();
$conn->close();
?>
