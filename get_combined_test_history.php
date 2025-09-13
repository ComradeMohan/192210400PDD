<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

// Database configuration
$host = 'localhost';
$dbname = 'univault_db';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed: ' . $e->getMessage()
    ]);
    exit;
}

// Get parameters
$studentId = isset($_GET['student_id']) ? (int)$_GET['student_id'] : 1;
$courseId = isset($_GET['course_id']) ? (int)$_GET['course_id'] : 1;

try {
    $combinedHistory = [];
    
    // Get MCQ Test History
    $mcqSql = "
        SELECT 
            test_result_id as id,
            'MCQ' as test_type,
            score as total_score,
            total_questions,
            time_taken,
            test_date,
            (score / total_questions * 100) as percentage
        FROM mcq_test_results 
        WHERE student_id = :student_id AND course_id = :course_id
        ORDER BY test_date DESC
    ";
    
    $stmt = $pdo->prepare($mcqSql);
    $stmt->execute(['student_id' => $studentId, 'course_id' => $courseId]);
    $mcqResults = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($mcqResults as $result) {
        $combinedHistory[] = [
            'id' => $result['id'],
            'test_type' => $result['test_type'],
            'total_score' => (int)$result['total_score'],
            'total_questions' => (int)$result['total_questions'],
            'percentage' => round((float)$result['percentage'], 2),
            'test_date' => $result['test_date'],
            'time_taken' => (int)$result['time_taken'],
            'time_taken_formatted' => formatTime($result['time_taken'])
        ];
    }
    
    // Get Theory Test History
    $theorySql = "
        SELECT 
            test_result_id as id,
            'Theory' as test_type,
            total_score,
            total_questions,
            test_date,
            percentage
        FROM theory_test_results 
        WHERE student_id = :student_id AND course_id = :course_id
        ORDER BY test_date DESC
    ";
    
    $stmt = $pdo->prepare($theorySql);
    $stmt->execute(['student_id' => $studentId, 'course_id' => $courseId]);
    $theoryResults = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($theoryResults as $result) {
        $combinedHistory[] = [
            'id' => $result['id'],
            'test_type' => $result['test_type'],
            'total_score' => (int)$result['total_score'],
            'total_questions' => (int)$result['total_questions'],
            'percentage' => round((float)$result['percentage'], 2),
            'test_date' => $result['test_date'],
            'time_taken' => null, // Theory tests don't have time limit
            'time_taken_formatted' => 'No Limit'
        ];
    }
    
    // Sort combined history by date (newest first)
    usort($combinedHistory, function($a, $b) {
        return strtotime($b['test_date']) - strtotime($a['test_date']);
    });
    
    // Calculate statistics
    $totalTests = count($combinedHistory);
    $mcqTests = count($mcqResults);
    $theoryTests = count($theoryResults);
    
    $totalPercentage = 0;
    $bestScore = 0;
    $averageScore = 0;
    
    if ($totalTests > 0) {
        foreach ($combinedHistory as $test) {
            $totalPercentage += $test['percentage'];
            if ($test['percentage'] > $bestScore) {
                $bestScore = $test['percentage'];
            }
        }
        $averageScore = round($totalPercentage / $totalTests, 2);
    }
    
    echo json_encode([
        'success' => true,
        'message' => 'Combined test history retrieved successfully',
        'test_history' => $combinedHistory,
        'statistics' => [
            'total_tests' => $totalTests,
            'mcq_tests' => $mcqTests,
            'theory_tests' => $theoryTests,
            'average_score' => $averageScore,
            'best_score' => $bestScore
        ],
        'student_id' => $studentId,
        'course_id' => $courseId
    ]);
    
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}

/**
 * Format time in seconds to readable format
 */
function formatTime($seconds) {
    if ($seconds < 60) {
        return $seconds . 's';
    } elseif ($seconds < 3600) {
        $minutes = floor($seconds / 60);
        $remainingSeconds = $seconds % 60;
        return $minutes . 'm ' . $remainingSeconds . 's';
    } else {
        $hours = floor($seconds / 3600);
        $minutes = floor(($seconds % 3600) / 60);
        return $hours . 'h ' . $minutes . 'm';
    }
}
?>
