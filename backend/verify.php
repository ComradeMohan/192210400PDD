<?php
$host = "localhost";
$user = "root";
$pass = "";
$db = "univalut_db";

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    die("Connection failed");
}

$email = $_GET['email'];
$token = $_GET['token'];

$sql = "SELECT * FROM students_new WHERE email = '$email' AND email_token = '$token'";
$result = $conn->query($sql);

if ($result->num_rows > 0) {
    $update = "UPDATE students_new SET is_verified = 1 WHERE email = '$email'";
    if ($conn->query($update) === TRUE) {
        echo "✅ Email verified successfully! You can now log in.";
    } else {
        echo "❌ Something went wrong while verifying.";
    }
} else {
    echo "❌ Invalid verification link.";
}
$conn->close();
?>
