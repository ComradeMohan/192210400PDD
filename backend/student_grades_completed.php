<?php
include('db.php'); // include your database connection file

header('Content-Type: application/json');

// Fetch department_id and student_id from GET parameters
$department_id = $_GET['department_id'] ?? null;
$student_id = $_GET['student_id'] ?? null;

if ($department_id && $student_id) {
    // SQL query to get completed courses
    $sql = "SELECT c.id, c.name, c.credits, sg.grade
            FROM courses_new c
            JOIN students_grades sg ON c.id = sg.course_id
            WHERE c.department_id = '$department_id' AND sg.student_id = '$student_id'";

    $result = $conn->query($sql);

    if ($result->num_rows > 0) {
        $courses = [];

        while ($row = $result->fetch_assoc()) {
            $courses[] = $row;
        }

        // Return success and the courses data
        echo json_encode([
            "success" => true,
            "courses" => $courses
        ]);
    } else {
        // No completed courses found
        echo json_encode([
            "success" => false,
            "message" => "No completed courses found"
        ]);
    }
} else {
    // Missing department_id or student_id
    echo json_encode([
        "success" => false,
        "message" => "Missing department_id or student_id"
    ]);
}
?>
