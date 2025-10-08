<?php
// Enable CORS (if you're testing from a different domain)
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

// Database connection details
include('db.php');
// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Check if the request method is POST
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    // Get raw POST data
    $data = json_decode(file_get_contents("php://input"), true);
    
    // Check if data is an array
    if (is_array($data)) {
        // Process each record
        foreach ($data as $record) {
            if (isset($record['id']) && isset($record['grade']) && isset($record['points'])) {
                // Extract data
                $id = $record['id'];
                $grade = $record['grade'];
                $points = $record['points'];
                
                // Prepare and bind
                $stmt = $conn->prepare("INSERT INTO grades (college_id, grade, points) VALUES (?, ?, ?)");
                $stmt->bind_param("isi", $id, $grade, $points);
                
                // Execute the query
                if ($stmt->execute()) {
                    $response[] = [
                        'status' => 'success',
                        'message' => 'Data added successfully!',
                        'id' => $id,
                        'grade' => $grade,
                        'points' => $points
                    ];
                } else {
                    $response[] = [
                        'status' => 'error',
                        'message' => 'Failed to insert data.'
                    ];
                }
            } else {
                $response[] = [
                    'status' => 'error',
                    'message' => 'Missing required fields in one of the records!'
                ];
            }
        }
        echo json_encode($response);
    } else {
        echo json_encode([
            'status' => 'error',
            'message' => 'Invalid data format. Expected an array.'
        ]);
    }
} else {
    echo json_encode([
        'status' => 'error',
        'message' => 'Invalid request method!'
    ]);
}

// Close connection
$conn->close();
?>
