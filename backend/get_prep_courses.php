<?php
header("Content-Type: application/json");
include 'db.php';

$sql = "SELECT course_code, course_name FROM prepcourses";
$result = $conn->query($sql);

$response = ["success" => false, "courses" => []];

if ($result->num_rows > 0) {
    $response["success"] = true;
    while ($row = $result->fetch_assoc()) {
        $response["courses"][] = [
            "course_code" => $row["course_code"],
            "subject_name" => $row["course_name"],
            "strength" => rand(2,5) // just demo, remove if not needed
        ];
    }
}

echo json_encode($response, JSON_PRETTY_PRINT);
$conn->close();
?>
