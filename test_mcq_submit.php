<?php
// Test script to debug MCQ submission
header('Content-Type: application/json');

// Test data
$testData = [
    'student_id' => 1,
    'course_code' => 'TEST001',
    'score' => 5,
    'total_questions' => 15,
    'time_taken' => 300,
    'answers' => [
        '1' => 0,
        '2' => 1,
        '3' => 2
    ]
];

echo "Test data being sent:\n";
echo json_encode($testData, JSON_PRETTY_PRINT) . "\n\n";

// Test the actual save_mcq_result.php
$url = 'http://localhost/univault/save_mcq_result.php';
$options = [
    'http' => [
        'header' => "Content-type: application/json\r\n",
        'method' => 'POST',
        'content' => json_encode($testData)
    ]
];

$context = stream_context_create($options);
$result = file_get_contents($url, false, $context);

echo "Response from save_mcq_result.php:\n";
echo $result . "\n";
?>
