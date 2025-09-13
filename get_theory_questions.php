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
$dbname = 'univault';
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
$courseId = isset($_GET['course_id']) ? (int)$_GET['course_id'] : 1;
$limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 15;
$difficulty = isset($_GET['difficulty']) ? $_GET['difficulty'] : null;

try {
    // Build the query
    $sql = "SELECT question_id, course_id, question_text, sample_answer, difficulty_level 
            FROM theory_questions 
            WHERE course_id = :course_id";
    
    $params = ['course_id' => $courseId];
    
    // Add difficulty filter if specified
    if ($difficulty && in_array(strtoupper($difficulty), ['EASY', 'MEDIUM', 'HARD'])) {
        $sql .= " AND difficulty_level = :difficulty";
        $params['difficulty'] = strtoupper($difficulty);
    }
    
    // Add randomization and limit
    $sql .= " ORDER BY RAND() LIMIT :limit";
    
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':course_id', $courseId, PDO::PARAM_INT);
    $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
    
    if ($difficulty && in_array(strtoupper($difficulty), ['EASY', 'MEDIUM', 'HARD'])) {
        $stmt->bindParam(':difficulty', strtoupper($difficulty), PDO::PARAM_STR);
    }
    
    $stmt->execute();
    $questions = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    if (empty($questions)) {
        echo json_encode([
            'success' => false,
            'message' => 'No theory questions found for this course'
        ]);
        exit;
    }
    
    // Return success response
    echo json_encode([
        'success' => true,
        'message' => 'Theory questions retrieved successfully',
        'questions' => $questions,
        'total_questions' => count($questions),
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
?>
