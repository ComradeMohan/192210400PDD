<?php
header("Content-Type: application/json");

// --- CONFIGURE YOUR DATABASE HERE ---
include('db.php'); // database connection ($conn)

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $userId = $_POST['user_id'] ?? '';
    $feedback = $_POST['feedback'] ?? '';
    $college = $_POST['college'] ?? '';

    if (empty($userId) || empty($feedback) || empty($college)) {
        echo json_encode(["success" => false, "message" => "Missing user ID, feedback, or college"]);
        exit;
    }

    // Insert feedback into database
    $stmt = $conn->prepare("INSERT INTO feedbacks (user_id, feedback, college) VALUES (?, ?, ?)");
    $stmt->bind_param("sss", $userId, $feedback, $college);
    
    if ($stmt->execute()) {
        // Fetch admin FCM tokens for the specific college
        $adminStmt = $conn->prepare("SELECT user_id, fcm_token FROM user_fcm_tokens WHERE user_id LIKE 'admin%' AND college = ?");
        $adminStmt->bind_param("s", $college);
        $adminStmt->execute();
        $result = $adminStmt->get_result();
        
        $adminTokens = [];
        $adminCount = 0;
        
        while ($row = $result->fetch_assoc()) {
            $adminTokens[] = [
                'user_id' => $row['user_id'],
                'fcm_token' => $row['fcm_token'],
                'college' => $college
            ];
            $adminCount++;
        }
        $adminStmt->close();
        
        // Create filename based on college name (sanitized)
        $sanitizedCollegeName = strtolower($college);
        $sanitizedCollegeName = preg_replace('/\s+/', '_', $sanitizedCollegeName);
        $sanitizedCollegeName = preg_replace('/[^a-z0-9_]/', '', $sanitizedCollegeName);
        
        $filename = "admin_tokens_{$sanitizedCollegeName}.json";
        $filepath = __DIR__ . "/tokens/" . $filename;
        
        // Create tokens directory if it doesn't exist
        $tokensDir = __DIR__ . "/tokens/";
        if (!is_dir($tokensDir)) {
            mkdir($tokensDir, 0755, true);
        }
        
        // Prepare data to store in file
        $fileData = [
            'college' => $college,
            'feedback_timestamp' => date('Y-m-d H:i:s'),
            'feedback_by' => $userId,
            'admin_count' => $adminCount,
            'admin_tokens' => $adminTokens
        ];
        
        // Write to file
        $fileWriteSuccess = file_put_contents($filepath, json_encode($fileData, JSON_PRETTY_PRINT));

        // 🔔 SEND TOPIC NOTIFICATION TO ADMINS (via send_notification.php)
        $topic = $sanitizedCollegeName . '_admins';  // e.g., saveetha_school_of_engineering_admins

        $notificationPayload = [
            "topic" => $topic,
            "title" => "New Feedback Received",
            "body" => "Feedback submitted by $userId in $college"
        ];

        $ch = curl_init(base_url('send_notification.php'));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($notificationPayload));
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'Content-Type: application/json'
        ]);

        $notificationResponse = curl_exec($ch);
        $notificationHttpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        $notificationData = json_decode($notificationResponse, true);

        // ✅ Final Response
        echo json_encode([
            "success" => true,
            "message" => "Feedback submitted successfully",
            "admin_info" => [
                "admins_found" => $adminCount,
                "college" => $college,
                "tokens_file" => $filename,
                "file_path" => $filepath,
                "admin_users" => array_column($adminTokens, 'user_id')
            ],
            "fcm_info" => [
                "sent_to_topic" => $topic,
                "http_status" => $notificationHttpCode,
                "fcm_response" => $notificationData
            ]
        ]);
        
    } else {
        echo json_encode(["success" => false, "message" => "Failed to submit feedback: " . $stmt->error]);
    }

    $stmt->close();
    $conn->close();

} else {
    echo json_encode(["success" => false, "message" => "Invalid request method"]);
}
?>
