<?php
header("Content-Type: application/json");

include('db.php');

// Get college name from request
$college = isset($_GET['college']) ? $_GET['college'] : '';

if (empty($college)) {
    echo json_encode(["success" => false, "message" => "College name is required"]);
    exit();
}

// Map college name to prefix
$prefix = match (strtolower(trim($college))) {
    'panimalar engineering college' => 'PEC',
    'saveetha school of engineering' => 'SSE',
    default => strtoupper(implode('', array_map(fn($word) => ucfirst($word[0]), explode(' ', trim($college))))) // extract first letter of each word
};

$sql = "SELECT login_id FROM faculty_new WHERE login_id LIKE '$prefix%' ORDER BY id DESC LIMIT 1";
$result = $conn->query($sql);

$nextId = $prefix . "001"; // default starting ID

if ($result && $result->num_rows > 0) {
    $row = $result->fetch_assoc();
    $lastId = $row['login_id'];

    $num = (int)substr($lastId, strlen($prefix)); // get number part
    $num++;
    $nextId = $prefix . str_pad($num, 3, "0", STR_PAD_LEFT);
}

echo json_encode(["success" => true, "next_id" => $nextId]);

$conn->close();
?>
