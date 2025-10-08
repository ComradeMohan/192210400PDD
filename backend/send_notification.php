<?php
require __DIR__ . '/vendor/autoload.php';

use Google\Client;
use GuzzleHttp\Client as GuzzleClient;

header('Content-Type: application/json');

// Path to Firebase service account key
$serviceAccountPath = __DIR__ . '/testing-5cd2f-firebase-adminsdk-fbsvc-45cdfbbf18.json';

// Read JSON POST input
$input = json_decode(file_get_contents('php://input'), true);

if (!$input || !isset($input['topic'], $input['title'], $input['body'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Please provide topic, title, and body in JSON POST data']);
    exit;
}

$topic = $input['topic'];
$title = $input['title'];
$body = $input['body'];

// Function to send FCM notification
function sendFcmNotification($topic, $title, $body, $serviceAccountPath)
{
    $client = new Client();
    $client->setAuthConfig($serviceAccountPath);
    $client->addScope('https://www.googleapis.com/auth/firebase.messaging');

    $accessTokenArray = $client->fetchAccessTokenWithAssertion();
    if (isset($accessTokenArray['error'])) {
        return ['success' => false, 'error' => $accessTokenArray['error_description']];
    }
    $accessToken = $accessTokenArray['access_token'];

    $projectId = json_decode(file_get_contents($serviceAccountPath), true)['project_id'];

    $fcmMessage = [
  'message' => [
    'topic' => $topic,
    'notification' => [
        'title' => $title,
        'body' => $body
    ],
    'data' => [
        'click_action' => 'OPEN_SPLASH_ACTIVITY',
        'title' => $title,
        'body' => $body,
        'screen' => 'SplashActivity'
    ],
    'android' => [
        'priority' => 'HIGH',
        'notification' => [
            'sound' => 'default',
            'click_action' => 'OPEN_SPLASH_ACTIVITY'
        ],
    ],
    'apns' => [
        'headers' => [
            'apns-priority' => '10',
        ],
        'payload' => [
            'aps' => [
                'sound' => 'default',
                'category' => 'FLUTTER_NOTIFICATION_CLICK'
            ],
        ],
    ],
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
        return [
            'success' => true,
            'message_id' => $responseBody['name'] ?? null
        ];
    } catch (Exception $e) {
        return ['success' => false, 'error' => $e->getMessage()];
    }
}

$result = sendFcmNotification($topic, $title, $body, $serviceAccountPath);

if ($result['success']) {
    echo json_encode([
        'status' => 'success',
        'topic' => $topic,
        'message_id' => $result['message_id'],
        'info' => 'Notification sent successfully (FCM message ID returned, actual delivery count not available here).'
    ]);
} else {
    http_response_code(500);
    echo json_encode(['status' => 'error', 'message' => $result['error']]);
}
