<?php
/**
 * Test script for theory questions API
 * This script tests the get_theory_questions_updated.php API
 */

echo "Testing Theory Questions API\n";
echo "============================\n\n";

// Test URL
$url = "http://localhost/univault/get_theory_questions_updated.php?course_id=1&limit=5";

echo "Testing URL: $url\n\n";

// Test 1: Basic request
echo "Test 1: Basic request (course_id=1, limit=5)\n";
$response = file_get_contents($url);
$data = json_decode($response, true);

if ($data && $data['success']) {
    echo "✅ Success: " . $data['message'] . "\n";
    echo "Total questions: " . $data['total_questions'] . "\n";
    echo "Course ID: " . $data['course_id'] . "\n\n";
    
    // Display first question
    if (!empty($data['questions'])) {
        $firstQuestion = $data['questions'][0];
        echo "Sample Question:\n";
        echo "ID: " . $firstQuestion['question_id'] . "\n";
        echo "Text: " . substr($firstQuestion['question_text'], 0, 100) . "...\n";
        echo "Keywords: " . $firstQuestion['keywords'] . "\n";
        echo "Difficulty: " . $firstQuestion['difficulty_level'] . "\n";
        echo "Marks: " . $firstQuestion['marks'] . "\n\n";
    }
} else {
    echo "❌ Error: " . ($data['message'] ?? 'Unknown error') . "\n\n";
}

// Test 2: Request with difficulty filter
echo "Test 2: Request with difficulty filter (easy)\n";
$url2 = "http://localhost/univault/get_theory_questions_updated.php?course_id=1&limit=3&difficulty=easy";
$response2 = file_get_contents($url2);
$data2 = json_decode($response2, true);

if ($data2 && $data2['success']) {
    echo "✅ Success: " . $data2['message'] . "\n";
    echo "Total questions: " . $data2['total_questions'] . "\n";
    
    // Check if all questions are easy
    $allEasy = true;
    foreach ($data2['questions'] as $question) {
        if ($question['difficulty_level'] !== 'easy') {
            $allEasy = false;
            break;
        }
    }
    echo "All questions are easy: " . ($allEasy ? "✅ Yes" : "❌ No") . "\n\n";
} else {
    echo "❌ Error: " . ($data2['message'] ?? 'Unknown error') . "\n\n";
}

// Test 3: Request with include_answer parameter
echo "Test 3: Request with include_answer=true\n";
$url3 = "http://localhost/univault/get_theory_questions_updated.php?course_id=1&limit=1&include_answer=true";
$response3 = file_get_contents($url3);
$data3 = json_decode($response3, true);

if ($data3 && $data3['success']) {
    echo "✅ Success: " . $data3['message'] . "\n";
    echo "Include answer: " . ($data3['include_answer'] ? "Yes" : "No") . "\n";
    
    if (!empty($data3['questions']) && isset($data3['questions'][0]['complete_answer'])) {
        echo "Complete answer included: ✅ Yes\n";
        echo "Answer preview: " . substr($data3['questions'][0]['complete_answer'], 0, 100) . "...\n\n";
    } else {
        echo "Complete answer included: ❌ No\n\n";
    }
} else {
    echo "❌ Error: " . ($data3['message'] ?? 'Unknown error') . "\n\n";
}

echo "API Testing Complete!\n";
?>
