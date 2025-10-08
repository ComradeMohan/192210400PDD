<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

// Database configuration
require 'db.php';
    // Get parameters
    $student_id = isset($_GET['student_id']) ? (int) $_GET['student_id'] : 0;
    $course_code = isset($_GET['course_code']) ? trim($_GET['course_code']) : '';
    $mode = isset($_GET['mode']) ? trim($_GET['mode']) : 'all';
    
    // Validate required fields
    if ($student_id <= 0) {
        echo json_encode([
            'success' => false,
            'error' => 'Invalid or missing student_id'
        ]);
        exit;
    }
    
    if (empty($course_code)) {
        echo json_encode([
            'success' => false,
            'error' => 'Missing course_code'
        ]);
        exit;
    }
    
    // Get study time record
    $stmt = $pdo->prepare("
        SELECT total_study_time_millis, created_at, last_updated 
        FROM student_study_time 
        WHERE student_id = ? AND course_code = ? AND mode = ?
    ");
    $stmt->execute([$student_id, $course_code, $mode]);
    $record = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($record) {
        // Convert milliseconds to hours and minutes for display
        $total_millis = (int) $record['total_study_time_millis'];
        $total_hours = $total_millis / (1000 * 60 * 60);
        $hours = floor($total_hours);
        $minutes = floor(($total_hours - $hours) * 60);
        
        echo json_encode([
            'success' => true,
            'total_study_time_millis' => $total_millis,
            'total_hours' => $hours,
            'total_minutes' => $minutes,
            'formatted_time' => $hours > 0 ? "$hours h $minutes min" : "$minutes min",
            'created_at' => $record['created_at'],
            'last_updated' => $record['last_updated']
        ]);
    } else {
        echo json_encode([
            'success' => true,
            'total_study_time_millis' => 0,
            'total_hours' => 0,
            'total_minutes' => 0,
            'formatted_time' => '0 min',
            'message' => 'No study time recorded for this course'
        ]);
    }
?>