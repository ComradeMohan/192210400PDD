<?php
include "db.php";

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $college_name = $_POST['college_name'];

    $query = "SELECT id FROM colleges WHERE TRIM(LOWER(name)) = TRIM(LOWER(?))";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("s", $college_name);
    $stmt->execute();
    $stmt->bind_result($college_id);

    if ($stmt->fetch()) {
        echo json_encode(["success" => true, "college_id" => $college_id]);
    } else {
        echo json_encode(["success" => false, "message" => "College not found"]);
    }

    $stmt->close();
}
?>
