<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Database configuration
$host = 'localhost';
$dbname = 'univault_db';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch(PDOException $e) {
    echo json_encode(['error' => 'Database connection failed: ' . $e->getMessage()]);
    exit;
}

// Test parameters
$student_id = 192210400;
$course_code = 'CSA17'; // Test with a specific course code

echo "<h2>Testing Combined Test History</h2>";
echo "<p>Student ID: $student_id</p>";
echo "<p>Course Code: $course_code</p>";

// Test MCQ results
echo "<h3>MCQ Test Results:</h3>";
$mcq_sql = "SELECT * FROM mcq_test_results WHERE student_id = :student_id AND course_code = :course_code";
$mcq_stmt = $pdo->prepare($mcq_sql);
$mcq_stmt->bindParam(':student_id', $student_id, PDO::PARAM_INT);
$mcq_stmt->bindParam(':course_code', $course_code, PDO::PARAM_STR);
$mcq_stmt->execute();
$mcq_results = $mcq_stmt->fetchAll(PDO::FETCH_ASSOC);

echo "<pre>";
print_r($mcq_results);
echo "</pre>";

// Test Theory results
echo "<h3>Theory Test Results:</h3>";
$theory_sql = "SELECT * FROM theory_test_results WHERE student_id = :student_id AND course_code = :course_code";
$theory_stmt = $pdo->prepare($theory_sql);
$theory_stmt->bindParam(':student_id', $student_id, PDO::PARAM_INT);
$theory_stmt->bindParam(':course_code', $course_code, PDO::PARAM_STR);
$theory_stmt->execute();
$theory_results = $theory_stmt->fetchAll(PDO::FETCH_ASSOC);

echo "<pre>";
print_r($theory_results);
echo "</pre>";

// Test combined query
echo "<h3>Combined Query Test:</h3>";
$combined_sql = "SELECT 
                    'MCQ' as test_type,
                    mtr.test_result_id as id,
                    mtr.score as total_score,
                    mtr.total_questions,
                    ROUND((mtr.score / mtr.total_questions) * 100, 2) as percentage,
                    mtr.time_taken,
                    mtr.test_date,
                    mtr.course_code
                FROM mcq_test_results mtr 
                WHERE mtr.student_id = :student_id 
                AND mtr.course_code = :course_code
                
                UNION ALL
                
                SELECT 
                    'Theory' as test_type,
                    ttr.test_result_id as id,
                    ttr.total_score as total_score,
                    ttr.total_questions,
                    ttr.percentage,
                    NULL as time_taken,
                    ttr.test_date,
                    ttr.course_code
                FROM theory_test_results ttr 
                WHERE ttr.student_id = :student_id 
                AND ttr.course_code = :course_code
                
                ORDER BY test_date DESC";

$combined_stmt = $pdo->prepare($combined_sql);
$combined_stmt->bindParam(':student_id', $student_id, PDO::PARAM_INT);
$combined_stmt->bindParam(':course_code', $course_code, PDO::PARAM_STR);
$combined_stmt->execute();
$combined_results = $combined_stmt->fetchAll(PDO::FETCH_ASSOC);

echo "<pre>";
print_r($combined_results);
echo "</pre>";

// Show all course codes for this student
echo "<h3>All Course Codes for Student $student_id:</h3>";

// MCQ course codes
$mcq_codes_sql = "SELECT DISTINCT course_code FROM mcq_test_results WHERE student_id = :student_id";
$mcq_codes_stmt = $pdo->prepare($mcq_codes_sql);
$mcq_codes_stmt->bindParam(':student_id', $student_id, PDO::PARAM_INT);
$mcq_codes_stmt->execute();
$mcq_codes = $mcq_codes_stmt->fetchAll(PDO::FETCH_COLUMN);

echo "<p>MCQ Course Codes: " . implode(', ', $mcq_codes) . "</p>";

// Theory course codes
$theory_codes_sql = "SELECT DISTINCT course_code FROM theory_test_results WHERE student_id = :student_id";
$theory_codes_stmt = $pdo->prepare($theory_codes_sql);
$theory_codes_stmt->bindParam(':student_id', $student_id, PDO::PARAM_INT);
$theory_codes_stmt->execute();
$theory_codes = $theory_codes_stmt->fetchAll(PDO::FETCH_COLUMN);

echo "<p>Theory Course Codes: " . implode(', ', $theory_codes) . "</p>";
?>
