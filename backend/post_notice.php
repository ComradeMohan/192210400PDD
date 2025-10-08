<?php
// Database connection
include('db.php');

// Get POST data with sanitization
$title = isset($_POST['title']) ? trim($_POST['title']) : '';
$details = isset($_POST['details']) ? trim($_POST['details']) : '';
$college = isset($_POST['college']) ? trim($_POST['college']) : '';
$schedule_date = isset($_POST['schedule_date']) ? $_POST['schedule_date'] : null;
$schedule_time = isset($_POST['schedule_time']) ? $_POST['schedule_time'] : null;
$attachment = isset($_POST['attachment']) ? $_POST['attachment'] : null;
$is_high_priority = isset($_POST['is_high_priority']) && $_POST['is_high_priority'] === 'true' ? 1 : 0;

// Validate mandatory fields
if (empty($title) || empty($details)) {
    echo json_encode(["status" => "error", "message" => "Title and Details are mandatory!"]);
    exit;
}

// Prepare and bind parameters to avoid SQL injection
$stmt = $conn->prepare("INSERT INTO notices (title, description, college, schedule_date, schedule_time, attachment, is_high_priority) VALUES (?, ?, ?, ?, ?, ?, ?)");
$stmt->bind_param("ssssssi", $title, $details, $college, $schedule_date, $schedule_time, $attachment, $is_high_priority);

if ($stmt->execute()) {
    // Sanitize topic string (lowercase, replace spaces with underscores, remove invalid chars)
    $sanitizedCollegeTopic = strtolower($college);
    $sanitizedCollegeTopic = preg_replace('/\s+/', '_', $sanitizedCollegeTopic);
    $sanitizedCollegeTopic = preg_replace('/[^a-z0-9_]/', '', $sanitizedCollegeTopic);

    // Default topic if college matches or empty
    $topic = ($sanitizedCollegeTopic === strtolower('Saveetha School of Engineering') || empty($sanitizedCollegeTopic)) ? 'simats_users' : $sanitizedCollegeTopic;

    $payload = json_encode([
        'topic' => $topic,
        'title' => $title,
        'body' => $details
    ]);

    // Send POST request to send_notification.php
    $notifyUrl = base_url('send_notification.php'); // Adjust URL accordingly
    $ch = curl_init($notifyUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Content-Length: ' . strlen($payload)
    ]);

    $notificationResponse = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

    if ($notificationResponse === false) {
        $errorMsg = curl_error($ch);
        curl_close($ch);
        echo json_encode([
            "status" => "error",
            "message" => "Notification request failed: $errorMsg"
        ]);
        exit;
    }
    curl_close($ch);

    echo json_encode([
        "status" => "success",
        "message" => "Notice posted successfully",
        "notification_status_code" => $httpCode,
        "notification_response" => json_decode($notificationResponse, true)
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "Error posting notice: " . $stmt->error]);
}

$stmt->close();
$conn->close();
?>
