<?php
header("Content-Type: application/json");

$host = "localhost";
$user = "root";
$pass = "";
$db = "univalut_db";

$conn = new mysqli($host, $user, $pass, $db);

if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Connection failed"]);
    exit();
}

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $userId = isset($_POST['faculty_id']) ? $_POST['faculty_id'] :
         (isset($_POST['student_number']) ? $_POST['student_number'] :
         (isset($_POST['admin_id']) ? $_POST['admin_id'] : null));

    $oldPassword = $_POST['old_password'];
    $newPassword = $_POST['new_password'];
    $userType = $_POST['user_type'];

    // Map user_type to table name and ID column
    $userMap = [
        'faculty' => ['table' => 'faculty_new', 'id_column' => 'login_id'],
        'student' => ['table' => 'students_new', 'id_column' => 'student_number'],
        'admin'   => ['table' => 'admins', 'id_column' => 'admin_id']
    ];

    if (!isset($userMap[$userType])) {
        echo json_encode(["success" => false, "message" => "Invalid user type"]);
        exit;
    }

    $tableName = $userMap[$userType]['table'];
    $idColumn = $userMap[$userType]['id_column'];

    $query = "SELECT * FROM $tableName WHERE $idColumn = ?";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("s", $userId);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($result && $result->num_rows > 0) {
        $user = $result->fetch_assoc();
        if ($user['password'] === $oldPassword) {
            $updateQuery = "UPDATE $tableName SET password = ? WHERE $idColumn = ?";
            $updateStmt = $conn->prepare($updateQuery);
            $updateStmt->bind_param("ss", $newPassword, $userId);

            if ($updateStmt->execute()) {
                $response['success'] = true;
                $response['message'] = "Password updated successfully";
            } else {
                $response['success'] = false;
                $response['message'] = "Failed to update password";
            }
        } else {
            $response['success'] = false;
            $response['message'] = "Old password is incorrect";
        }
    } 
    else {
        $response['success'] = false;
        $response['message'] = "User not found";
    }

    echo json_encode($response);
}
?>
