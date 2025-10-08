<?php

require 'vendor/autoload.php';

use Google\Auth\Credentials\ServiceAccountCredentials;

// Path to your service account JSON file
$serviceAccountPath = __DIR__ . '/tester-c330a-27a7195c45a8.json'; // <-- replace with actual path

// Set the required scope for FCM
$scopes = ['https://www.googleapis.com/auth/firebase.messaging'];

// Create service account credentials
$credentials = new ServiceAccountCredentials($scopes, $serviceAccountPath);

// Fetch the access token
$tokenArray = $credentials->fetchAuthToken();
if (!isset($tokenArray['access_token'])) {
    die("Failed to get access token");
}

$accessToken = $tokenArray['access_token'];

// Your Firebase project ID
$projectId = 'tester-c330a'; // <-- replace with actual project ID

// Target device token
$deviceToken = 'e0_1ZWZGRUaQVe2uepcCeh:APA91bGVNi-j-9JymwVTr15StRj1QsjDs2Lwg4_62G4UnIPO1Vq_k7c3KkBkmVd5iRqByrQNr9lJxTdPWvlkILc4hae4jOHD7BPl7hpWTM8p07lRq5aOqHs';

// Build notification message
$message = [
    'message' => [
        'token' => $deviceToken,
        'notification' => [
            'title' => '🚀 Notification Test',
            'body' => 'This is a test from PHP using HTTP v1',
        ],
        'data' => [
            'key1' => 'value1',
            'key2' => 'value2',
        ],
    ],
];

// FCM HTTP v1 endpoint
$url = "https://fcm.googleapis.com/v1/projects/{$projectId}/messages:send";

// Prepare headers
$headers = [
    'Authorization: Bearer ' . $accessToken,
    'Content-Type: application/json',
];

// Send request using cURL
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($message));

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

if (curl_errno($ch)) {
    echo 'Curl error: ' . curl_error($ch);
} else {
    echo "HTTP Status: $httpCode\n";
    echo "Response:\n$response\n";
}

curl_close($ch);
