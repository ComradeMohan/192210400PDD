<?php
include('db.php');

$method = $_SERVER["REQUEST_METHOD"];
$department_id = null;

if ($method === "POST") {
    $department_id = $_POST['department_id'];
} else if ($method === "GET") {
    $department_id = $_GET['department_id'];
}

if ($department_id !== null) {
    $sql = "SELECT * FROM courses_new WHERE department_id = '$department_id'";
    $result = $conn->query($sql);

    if ($result->num_rows > 0) {
        $courses = array();
        while ($row = $result->fetch_assoc()) {
            $courses[] = $row;
        }
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
        "message" => "Missing department_id"
    ]);
}
?>
