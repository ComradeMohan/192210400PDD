<?php
include('db.php');

$student_id = $_POST['student_id'] ?? '';
$course_id = $_POST['course_id'] ?? '';

$response = array();

if ($student_id && $course_id) {
    $sql = "DELETE FROM students_grades WHERE student_id = '$student_id' AND course_id = '$course_id'";

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
