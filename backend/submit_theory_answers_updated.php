<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

/**
 * Calculate score based on keyword matching
 */
function calculateKeywordScore($studentAnswer, $keywords, $maxMarks) {
    if (empty($keywords) || empty($studentAnswer)) {
        return 0;
    }
    
    // Convert to lowercase for case-insensitive matching
    $answer = strtolower($studentAnswer);
    $keywordList = array_map('trim', explode(',', strtolower($keywords)));
    
    $matchedKeywords = 0;
    $totalKeywords = count($keywordList);
    
    // Check for keyword matches
    foreach ($keywordList as $keyword) {
        if (strpos($answer, $keyword) !== false) {
            $matchedKeywords++;
        }
    }
    
    // Calculate score based on keyword match percentage
    $matchPercentage = $totalKeywords > 0 ? ($matchedKeywords / $totalKeywords) : 0;
    
    // Base score on match percentage with some minimum for partial answers
    if ($matchPercentage >= 0.8) {
        return $maxMarks; // Full marks for 80%+ keyword match
    } elseif ($matchPercentage >= 0.6) {
        return round($maxMarks * 0.8); // 80% of marks for 60-79% match
    } elseif ($matchPercentage >= 0.4) {
        return round($maxMarks * 0.6); // 60% of marks for 40-59% match
    } elseif ($matchPercentage >= 0.2) {
        return round($maxMarks * 0.4); // 40% of marks for 20-39% match
    } elseif (strlen($studentAnswer) > 20) {
        return round($maxMarks * 0.2); // 20% of marks for substantial answer
    } else {
        return 0; // No marks for very short or no relevant content
    }
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

// Get JSON input
$input = json_decode(file_get_contents('php://input'), true);

if (!$input) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid JSON input'
    ]);
    exit;
}

// Validate required fields
$requiredFields = ['student_id', 'course_code', 'answers'];
foreach ($requiredFields as $field) {
    if (!isset($input[$field])) {
        echo json_encode([
            'success' => false,
            'message' => "Missing required field: $field"
        ]);
        exit;
    }
}

$studentId = (int)$input['student_id'];
$courseCode = $input['course_code'];
$answers = $input['answers'];

if (!is_array($answers) || empty($answers)) {
    echo json_encode([
        'success' => false,
        'message' => 'Answers must be a non-empty array'
    ]);
    exit;
}

try {
    // Validate course_code exists in prepcourses table
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
    
    // Start transaction
    $pdo->beginTransaction();
    
    // Calculate total marks and scores
    $totalMarks = 0;
    $totalScore = 0;
    $answeredQuestions = 0;
    $questionScores = [];
    
    // Get marks and calculate scores for each question
    foreach ($answers as $answer) {
        if (isset($answer['question_id']) && !empty(trim($answer['answer']))) {
            $stmt = $pdo->prepare("SELECT marks, keywords FROM theory_questions WHERE question_id = :question_id");
            $stmt->execute(['question_id' => (int)$answer['question_id']]);
            $question = $stmt->fetch(PDO::FETCH_ASSOC);
            
            if ($question) {
                $maxMarks = (int)$question['marks'];
                $keywords = $question['keywords'];
                $studentAnswer = trim($answer['answer']);
                
                // Calculate score based on keyword matching
                $score = calculateKeywordScore($studentAnswer, $keywords, $maxMarks);
                
                $totalMarks += $maxMarks;
                $totalScore += $score;
                $answeredQuestions++;
                
                $questionScores[] = [
                    'question_id' => (int)$answer['question_id'],
                    'max_marks' => $maxMarks,
                    'score' => $score,
                    'keywords' => $keywords
                ];
            }
        }
    }
    
    // Calculate percentage
    $percentage = $totalMarks > 0 ? round(($totalScore / $totalMarks) * 100, 2) : 0;
    
    // Insert test result with calculated marks and scores
    $stmt = $pdo->prepare("
        INSERT INTO theory_test_results (student_id, course_code, total_questions, total_marks, total_score, answered_questions, percentage) 
        VALUES (:student_id, :course_code, :total_questions, :total_marks, :total_score, :answered_questions, :percentage)
    ");
    $stmt->execute([
        'student_id' => $studentId,
        'course_code' => $courseCode,
        'total_questions' => count($answers),
        'total_marks' => $totalMarks,
        'total_score' => $totalScore,
        'answered_questions' => $answeredQuestions,
        'percentage' => $percentage
    ]);
    
    $testResultId = $pdo->lastInsertId();
    
    // Insert answers with detailed scoring
    $stmt = $pdo->prepare("
        INSERT INTO theory_test_answers (test_result_id, question_id, student_answer, marks_allocated, score_obtained, keywords_matched) 
        VALUES (:test_result_id, :question_id, :student_answer, :marks_allocated, :score_obtained, :keywords_matched)
    ");
    
    foreach ($answers as $answer) {
        if (!isset($answer['question_id']) || !isset($answer['answer'])) {
            throw new Exception('Invalid answer format');
        }
        
        // Find the score for this question
        $questionScore = null;
        foreach ($questionScores as $score) {
            if ($score['question_id'] == (int)$answer['question_id']) {
                $questionScore = $score;
                break;
            }
        }
        
        if ($questionScore) {
            $stmt->execute([
                'test_result_id' => $testResultId,
                'question_id' => (int)$answer['question_id'],
                'student_answer' => trim($answer['answer']),
                'marks_allocated' => $questionScore['max_marks'],
                'score_obtained' => $questionScore['score'],
                'keywords_matched' => $questionScore['keywords']
            ]);
        }
    }
    
    // Commit transaction
    $pdo->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Theory answers submitted successfully',
        'test_result_id' => $testResultId,
        'total_questions' => count($answers),
        'answered_questions' => $answeredQuestions,
        'total_marks' => $totalMarks,
        'total_score' => $totalScore,
        'percentage' => $percentage,
        'course_id' => $courseId,
        'course_code' => $courseCode,
        'question_scores' => $questionScores,
        'submission_date' => date('Y-m-d H:i:s')
    ]);
    
} catch (PDOException $e) {
    // Rollback transaction on error
    $pdo->rollBack();
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
} catch (Exception $e) {
    // Rollback transaction on error
    $pdo->rollBack();
    echo json_encode([
        'success' => false,
        'message' => 'Error: ' . $e->getMessage()
    ]);
}
?>
