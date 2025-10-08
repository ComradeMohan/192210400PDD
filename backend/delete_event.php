<?php
header('Content-Type: application/json');
include 'db.php';

$response = [];

// Read raw POST JSON body
$data = json_decode(file_get_contents("php://input"), true);

// Check if title and college_name exist in parsed JSON
if (isset($data['title']) && isset($data['college_name'])) {
    $title = mysqli_real_escape_string($conn, $data['title']);
    $college_name = mysqli_real_escape_string($conn, $data['college_name']);

    $sql = "DELETE FROM events_new WHERE title = '$title' AND college_name = '$college_name'";
    $result = mysqli_query($conn, $sql);

    if ($result && mysqli_affected_rows($conn) > 0) {
        $response['status'] = 'success';
        $response['message'] = 'Event deleted successfully';
    } else {
        $response['status'] = 'error';
        $response['message'] = 'Event not found or already deleted';
    }
} else {
    $response['status'] = 'error';
    $response['message'] = 'Missing title or college_name';
}

echo json_encode($response);
mysqli_close($conn);
?>
