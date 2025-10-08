<?php
require __DIR__ . '/vendor/autoload.php';

use Google\Client;
use GuzzleHttp\Client as GuzzleClient;

function sendFcmNotification($topic, $title, $body, $serviceAccountPath)
{
    // Load service account
    $client = new Client();
    $client->setAuthConfig($serviceAccountPath);
    $client->addScope('https://www.googleapis.com/auth/firebase.messaging');

    // Fetch access token
    $accessTokenArray = $client->fetchAccessTokenWithAssertion();
    if (isset($accessTokenArray['error'])) {
        return ['success' => false, 'error' => $accessTokenArray['error_description']];
    }
    $accessToken = $accessTokenArray['access_token'];

    // Get project ID from service account file
    $projectId = json_decode(file_get_contents($serviceAccountPath), true)['project_id'];

    // Prepare FCM message payload
    $fcmMessage = [
        'message' => [
            'topic' => $topic,
            'notification' => [
                'title' => $title,
                'body' => $body
            ]
        ]
    ];

    $url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send";

    $httpClient = new GuzzleClient();

    try {
        $response = $httpClient->post($url, [
            'headers' => [
                'Authorization' => "Bearer $accessToken",
                'Content-Type' => 'application/json',
            ],
            'body' => json_encode($fcmMessage),
        ]);
        $responseBody = json_decode($response->getBody(), true);
        return ['success' => true, 'response' => $responseBody];
    } catch (Exception $e) {
        return ['success' => false, 'error' => $e->getMessage()];
    }
}

// ----------- TESTING CODE -------------
// Replace this with your service account JSON file path
$serviceAccountPath = __DIR__ . '/firebase-service-account.json';

// Send a test notification to topic "simats_user"
$result = sendFcmNotification(
    "simats_user",
    "Test Notice",
    "This is a test notification from PHP",
    $serviceAccountPath
);

if ($result['success']) {
    echo "Notification sent successfully:\n";
    print_r($result['response']);
} else {
    echo "Error sending notification: " . $result['error'] . "\n";
}
