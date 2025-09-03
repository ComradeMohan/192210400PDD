<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

// Database configuration
$servername = "localhost";
$username = "your_username";
$password = "your_password";
$dbname = "univault_db";

try {
    // Create connection
    $pdo = new PDO("mysql:host=$servername;dbname=$dbname", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get JSON input
    $input = json_decode(file_get_contents('php://input'), true);
    
    // Validate required fields
    if (!isset($input['student_id']) || !isset($input['course_code']) || !isset($input['study_time_millis'])) {
        echo json_encode([
            'success' => false,
            'error' => 'Missing required fields: student_id, course_code, study_time_millis'
        ]);
        exit;
    }
    
    $student_id = (int) $input['student_id'];
    $course_code = trim($input['course_code']);
    $study_time_millis = (int) $input['study_time_millis'];
    $mode = isset($input['mode']) ? trim($input['mode']) : 'all';
    
    // Validate student_id and study_time_millis
    if ($student_id <= 0) {
        echo json_encode([
            'success' => false,
            'error' => 'Invalid student_id'
        ]);
        exit;
    }
    
    if ($study_time_millis < 0) {
        echo json_encode([
            'success' => false,
            'error' => 'Study time cannot be negative'
        ]);
        exit;
    }
    
    // Check if record exists
    $checkStmt = $pdo->prepare("
        SELECT id, total_study_time_millis 
        FROM student_study_time 
        WHERE student_id = ? AND course_code = ? AND mode = ?
    ");
    $checkStmt->execute([$student_id, $course_code, $mode]);
    $existingRecord = $checkStmt->fetch(PDO::FETCH_ASSOC);
    
    if ($existingRecord) {
        // Update existing record
        $updateStmt = $pdo->prepare("
            UPDATE student_study_time 
            SET total_study_time_millis = ?, 
                last_updated = CURRENT_TIMESTAMP 
            WHERE id = ?
        ");
        $updateStmt->execute([$study_time_millis, $existingRecord['id']]);
        
        echo json_encode([
            'success' => true,
            'message' => 'Study time updated successfully',
            'previous_time_millis' => (int) $existingRecord['total_study_time_millis'],
            'new_time_millis' => $study_time_millis
        ]);
    } else {
        // Insert new record
        $insertStmt = $pdo->prepare("
            INSERT INTO student_study_time 
            (student_id, course_code, mode, total_study_time_millis, created_at, last_updated) 
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ");
        $insertStmt->execute([$student_id, $course_code, $mode, $study_time_millis]);
        
        echo json_encode([
            'success' => true,
            'message' => 'Study time saved successfully',
            'new_time_millis' => $study_time_millis
        ]);
    }
    
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ]);
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ]);
}
?>