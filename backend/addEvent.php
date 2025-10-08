<?php
header('Content-Type: application/json');
include 'db.php';

// Read raw JSON input
$data = json_decode(file_get_contents('php://input'), true);

// Extract fields
$title = $data['title'] ?? '';
$type = $data['type'] ?? '';
$description = $data['description'] ?? '';
$start_date = $data['start_date'] ?? '';
$end_date = $data['end_date'] ?? '';
$college_name = $data['college_name'] ?? '';

// Validate inputs
if ($title && $type && $start_date && $end_date && $college_name) {

    // Step 1: Check for duplicate event
    $checkSql = "SELECT id FROM events_new WHERE title = ? AND start_date = ? AND end_date = ? AND college_name = ?";
    $stmt = $conn->prepare($checkSql);
    $stmt->bind_param("ssss", $title, $start_date, $end_date, $college_name);
    $stmt->execute();
    $stmt->store_result();

    if ($stmt->num_rows > 0) {
        $stmt->close();
        echo json_encode([
            "status" => "error",
            "message" => "Event exists"
        ]);
        exit();
    }
    $stmt->close();

    // Step 2: Insert the new event
    $sql = "INSERT INTO events_new (title, type, description, start_date, end_date, college_name)
            VALUES (?, ?, ?, ?, ?, ?)";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("ssssss", $title, $type, $description, $start_date, $end_date, $college_name);

    if ($stmt->execute()) {
        // Step 3: Sanitize topic
        $sanitizedCollegeTopic = strtolower($college_name);
        $sanitizedCollegeTopic = preg_replace('/\s+/', '_', $sanitizedCollegeTopic);       // Replace spaces
        $sanitizedCollegeTopic = preg_replace('/[^a-z0-9_]/', '', $sanitizedCollegeTopic);  // Remove special chars

           $topic = ($sanitizedCollegeTopic === strtolower('Saveetha School of Engineering') || empty($sanitizedCollegeTopic)) ? 'simats_users' : $sanitizedCollegeTopic;

        // Step 4: Build FCM payload
        $payload = json_encode([
            'topic' => $topic,
            'title' => "New Event: $title",
            'body' => $description ?: "$type event scheduled from $start_date to $end_date"
        ]);

        // Step 5: Send FCM notification
        $notifyUrl = base_url('send_notification.php');
        $ch = curl_init($notifyUrl);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Content-Type: application/json',
            'Content-Length: ' . strlen($payload)
        ]);
        $notifyResponse = curl_exec($ch);
        $notifyCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        // Step 6: Final success response
        echo json_encode([
            "status" => "success",
            "message" => "Event added successfully",
            "notification_status_code" => $notifyCode,
            "notification_response" => json_decode($notifyResponse, true)
        ]);
    } else {
        echo json_encode(["status" => "error", "message" => $stmt->error]);
    }

    $stmt->close();

} else {
    echo json_encode(["status" => "error", "message" => "Missing required fields"]);
}

$conn->close();
?>
