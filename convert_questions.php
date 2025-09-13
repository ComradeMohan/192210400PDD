<?php
// Helper script to convert your existing questions to the proper format
// This will help you format your 153 questions correctly

echo "MCQ Questions Converter\n";
echo "======================\n\n";

// Example of how to format your questions
echo "Here's the format you should use for each question:\n\n";

echo "// Question 1\n";
echo "[\n";
echo "    'course_id' => 1, // Use 1 for AI, 2 for Computing, 3 for Theory, 5 for Network, 6 for OS\n";
echo "    'question_text' => 'Your question text here?',\n";
echo "    'option_a' => 'Option A text',\n";
echo "    'option_b' => 'Option B text',\n";
echo "    'option_c' => 'Option C text',\n";
echo "    'option_d' => 'Option D text',\n";
echo "    'correct_option' => 'A' // Use A, B, C, or D\n";
echo "],\n\n";

echo "Course ID Reference:\n";
echo "1 = Artificial Intelligence (CSA17)\n";
echo "2 = Fundamentals of Computing (CSA57)\n";
echo "3 = Theory of Computation (CSA13)\n";
echo "5 = Computer Network (CSA07)\n";
echo "6 = Operating Systems (CSA04)\n\n";

echo "Steps to add your 153 questions:\n";
echo "1. Open insert_mcq_questions.php\n";
echo "2. Replace the sample questions array with your 153 questions\n";
echo "3. Make sure each question follows the format above\n";
echo "4. Use the correct course_id for each question\n";
echo "5. Run the script to insert all questions\n\n";

echo "Example for different courses:\n\n";

echo "// AI Question\n";
echo "[\n";
echo "    'course_id' => 1,\n";
echo "    'question_text' => 'What is machine learning?',\n";
echo "    'option_a' => 'A type of database',\n";
echo "    'option_b' => 'A programming language',\n";
echo "    'option_c' => 'A subset of AI',\n";
echo "    'option_d' => 'A type of computer',\n";
echo "    'correct_option' => 'C'\n";
echo "],\n\n";

echo "// Computing Question\n";
echo "[\n";
echo "    'course_id' => 2,\n";
echo "    'question_text' => 'What is a linked list?',\n";
echo "    'option_a' => 'A type of array',\n";
echo "    'option_b' => 'A linear data structure',\n";
echo "    'option_c' => 'A sorting algorithm',\n";
echo "    'option_d' => 'A database table',\n";
echo "    'correct_option' => 'B'\n";
echo "],\n\n";

echo "After formatting all 153 questions, run: php insert_mcq_questions.php\n";
?>
