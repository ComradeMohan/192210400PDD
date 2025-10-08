<?php
include('db.php');
header('Content-Type: application/json');

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $college_id = $_POST['college_id'];
    $name = $_POST['name'];

    $stmt = $conn->prepare("SELECT id FROM departments_new WHERE college_id = ? AND name = ?");
    $stmt->bind_param("is", $college_id, $name);
    $stmt->execute();
    $stmt->bind_result($department_id);

    if ($stmt->fetch()) {
        echo json_encode(["success" => true, "department_id" => $department_id]);
    } else {
        echo json_encode(["success" => false, "message" => "Department not found"]);
    }

    $stmt->close();
    $conn->close();
}
?>
