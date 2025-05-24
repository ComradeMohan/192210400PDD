<?php
include 'db.php';

$student_id = $_POST['student_id'];
$course_id = $_POST['course_id'];
$grade = $_POST['grade'];

$response = array();

if ($student_id && $course_id && $grade) {
    $sql = "INSERT INTO students_grades (student_id, course_id, grade) 
            VALUES ('$student_id', '$course_id', '$grade')
            ON DUPLICATE KEY UPDATE grade = '$grade'";

    if (mysqli_query($conn, $sql)) {
        $response['success'] = true;
    } else {
        $response['success'] = false;
        $response['error'] = mysqli_error($conn);
    }
} else {
    $response['success'] = false;
    $response['error'] = 'Missing parameters';
}

echo json_encode($response);
?>
