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
$courseCode = isset($_GET['course_code']) ? $_GET['course_code'] : '';
$limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 15;
$difficulty = isset($_GET['difficulty']) ? $_GET['difficulty'] : null;
$includeAnswer = isset($_GET['include_answer']) ? filter_var($_GET['include_answer'], FILTER_VALIDATE_BOOLEAN) : false;

// Validate course_code parameter
if (empty($courseCode)) {
    echo json_encode([
        'success' => false,
        'message' => 'Missing required parameter: course_code'
    ]);
    exit;
}

try {
    // First, get the course_id from course_code
    $course_sql = "SELECT course_id FROM prepcourses WHERE course_code = :course_code";
    $course_stmt = $pdo->prepare($course_sql);
    $course_stmt->bindParam(':course_code', $courseCode, PDO::PARAM_STR);
    $course_stmt->execute();
    $course_result = $course_stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$course_result) {
        echo json_encode([
            'success' => false,
            'message' => 'Course not found for course_code: ' . $courseCode
        ]);
        exit;
    }
    
    $courseId = (int)$course_result['course_id'];
    
    // Build the query based on your table structure
    $sql = "SELECT question_id, course_id, question_text, keywords, complete_answer, difficulty_level, marks, created_at 
            FROM theory_questions 
            WHERE course_id = :course_id";
    
    $params = ['course_id' => $courseId];
    
    // Add difficulty filter if specified
    if ($difficulty && in_array(strtolower($difficulty), ['easy', 'medium', 'hard'])) {
        $sql .= " AND difficulty_level = :difficulty";
        $params['difficulty'] = strtolower($difficulty);
    }
    
    // Add randomization and limit
    $sql .= " ORDER BY RAND() LIMIT :limit";
    
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':course_id', $courseId, PDO::PARAM_INT);
    $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
    
    if ($difficulty && in_array(strtolower($difficulty), ['easy', 'medium', 'hard'])) {
        $stmt->bindParam(':difficulty', strtolower($difficulty), PDO::PARAM_STR);
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
    
    // Process questions to format them properly
    $formattedQuestions = [];
    foreach ($questions as $question) {
        $formattedQuestion = [
            'question_id' => (int)$question['question_id'],
            'course_id' => (int)$question['course_id'],
            'question_text' => $question['question_text'],
            'keywords' => $question['keywords'],
            'difficulty_level' => $question['difficulty_level'],
            'marks' => (int)$question['marks'],
            'created_at' => $question['created_at']
        ];
        
        // Include complete answer only if requested (for review purposes)
        if ($includeAnswer) {
            $formattedQuestion['complete_answer'] = $question['complete_answer'];
        }
        
        $formattedQuestions[] = $formattedQuestion;
    }
    
    // Return success response
    echo json_encode([
        'success' => true,
        'message' => 'Theory questions retrieved successfully',
        'questions' => $formattedQuestions,
        'total_questions' => count($formattedQuestions),
        'course_id' => $courseId,
        'course_code' => $courseCode,
        'include_answer' => $includeAnswer
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
