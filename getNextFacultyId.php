<?php
header("Content-Type: application/json");

$host = "localhost";
$user = "root";
$pass = "";
$db = "univalut_db";

$conn = new mysqli($host, $user, $pass, $db);

if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Connection failed"]);
    exit();
}

$sql = "SELECT login_id FROM faculty_new ORDER BY id DESC LIMIT 1";
$result = $conn->query($sql);

$nextId = "SSE001"; // default

if ($result && $result->num_rows > 0) {
    $row = $result->fetch_assoc();
    $lastId = $row['login_id']; // SSE003

    // Extract numeric part
    $num = (int)substr($lastId, 3); // 3
    $num++;
    $nextId = "SSE" . str_pad($num, 3, "0", STR_PAD_LEFT); // SSE004
}

echo json_encode(["success" => true, "next_id" => $nextId]);

$conn->close();
?>
