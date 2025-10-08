<?php
header('Content-Type: application/json');
include('db.php');

$data = json_decode(file_get_contents('php://input'), true);

if (!$data || !isset($data['user_id']) || !isset($data['fcm_token']) || !isset($data['college'])) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'Invalid input']);
    exit;
}

$user_id = $data['user_id'];
$fcm_token = $data['fcm_token'];
$college = $data['college'];

$stmt = $conn->prepare(
    "INSERT INTO user_fcm_tokens (user_id, fcm_token, college) 
     VALUES (?, ?, ?) 
     ON DUPLICATE KEY UPDATE fcm_token = ?, college = ?, updated_at = CURRENT_TIMESTAMP"
);
$stmt->bind_param("sssss", $user_id, $fcm_token, $college, $fcm_token, $college);

if ($stmt->execute()) {
    echo json_encode(['success' => true, 'message' => 'FCM token saved']);
} else {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Failed to save token',
        'error' => $stmt->error
    ]);
}

$stmt->close();
$conn->close();
?>
