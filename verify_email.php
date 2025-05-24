<?php
$host = "localhost";
$user = "root";
$pass = "";
$db = "univalut_db";

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    die("Connection failed");
}

if (isset($_GET['token'])) {
    $token = $conn->real_escape_string($_GET['token']);
    $sql = "SELECT * FROM students_new WHERE verification_token='$token' AND verified=0";
    $result = $conn->query($sql);

    if ($result->num_rows > 0) {
        $update = $conn->query("UPDATE students_new SET verified=1, verification_token=NULL WHERE verification_token='$token'");
        if ($update) {
            echo "Email verified successfully! You can now login.";
        } else {
            echo "Failed to verify email.";
        }
    } else {
        echo "Invalid or expired token.";
    }
} else {
    echo "No token provided.";
}

$conn->close();
?>
