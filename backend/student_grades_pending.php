<?php
include('db.php');

header('Content-Type: application/json');

$method = $_SERVER["REQUEST_METHOD"];
$department_id = null;
$student_id = null;

if ($method === "POST") {
    $department_id = $_POST['department_id'] ?? null;
    $student_id = $_POST['student_id'] ?? null;
} else if ($method === "GET") {
    $department_id = $_GET['department_id'] ?? null;
    $student_id = $_GET['student_id'] ?? null;
}

if ($department_id !== null && $student_id !== null) {
    // Step 1: Fetch all courses for the department
    $sql = "SELECT * FROM courses_new WHERE department_id = '$department_id'";
    $result = $conn->query($sql);

    if ($result->num_rows > 0) {
        $courses = [];

        // Step 2: Iterate over each course
        while ($row = $result->fetch_assoc()) {
            $course_id = $row['id'];

            // Step 3: Check if the student has already completed this course
            $check_sql = "SELECT 1 FROM students_grades WHERE student_id = '$student_id' AND course_id = '$course_id' LIMIT 1";
            $check_result = $conn->query($check_sql);

            // Step 4: If course is not completed by the student, add it to the list
            if ($check_result && $check_result->num_rows == 0) {
                $row['status'] = 'pending'; // Status is pending as the student has not completed it
                $courses[] = $row; // Add course to pending list
            }
        }

        // Step 5: Return the result as JSON
        echo json_encode([
            "success" => true,
            "courses" => $courses
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "No courses found"
        ]);
    }

    $conn->close();
} else {
    echo json_encode([
        "success" => false,
        "message" => "Missing department_id or student_id"
    ]);
}
?>
