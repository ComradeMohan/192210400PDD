<?php
// Set the correct content type
header('Content-Type: application/json');

// Check if required POST parameters are set
if (isset($_POST['college']) && isset($_POST['course']) && isset($_POST['file'])) {
    $college = $_POST['college'];
    $course = $_POST['course'];
    $fileName = $_POST['file'];

    // Construct the file path
    $baseDir = "uploads/$college/$course/";
    $filePath = $baseDir . $fileName;

    // Check if file exists
    if (file_exists($filePath)) {
        if (unlink($filePath)) {
            // Optional: delete from DB (if applicable)
            // $conn = new mysqli("localhost", "root", "", "your_db");
            // $stmt = $conn->prepare("DELETE FROM materials WHERE filename = ? AND course = ? AND college = ?");
            // $stmt->bind_param("sss", $fileName, $course, $college);
            // $stmt->execute();
            // $stmt->close();
            // $conn->close();

            echo json_encode(["success" => true, "message" => "File deleted"]);
        } else {
            echo json_encode(["success" => false, "message" => "Unable to delete file"]);
        }
    } else {
        echo json_encode(["success" => false, "message" => "File does not exist"]);
    }
} else {
    echo json_encode(["success" => false, "message" => "Invalid request"]);
}
?>
