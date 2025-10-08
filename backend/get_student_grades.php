<?php
include 'db.php'; // your database connection

$response = array();

if (isset($_POST['student_id'])) {
    $student_id = $_POST['student_id'];

    $query = "SELECT grade FROM student_grades WHERE student_id = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("s", $student_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $grades = array();

    while ($row = $result->fetch_assoc()) {
        $grades[] = $row['grade'];
    }

    $response['success'] = true;
    $response['grades'] = $grades;
} else {
    $response['success'] = false;
    $response['message'] = "Missing student_id";
}

echo json_encode($response);
?>
