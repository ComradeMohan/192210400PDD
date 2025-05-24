<?php
header("Content-Type: application/json");
error_reporting(E_ALL);
ini_set('display_errors', 1);

require 'vendor/autoload.php'; // Composer's autoloader
use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;
use PHPMailer\PHPMailer\SMTP;

// DB connection
$host = "localhost";
$user = "root";
$pass = "";
$db = "univalut_db";

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    echo json_encode(["success" => false, "message" => "Connection failed"]);
    exit();
}

// Read and sanitize input
$data = json_decode(file_get_contents("php://input"), true);

// Fallback if no JSON body (e.g., from browser)
if (!$data) {
    $data = $_GET;
}

$student_number = isset($data["student_number"]) ? $conn->real_escape_string($data["student_number"]) : null;




if (!$student_number) {
    echo json_encode(["success" => false, "message" => "Student number is required."]);
    $conn->close();
    exit();
}

// Fetch user by student_number
$sql = "SELECT id, email, full_name FROM students_new WHERE student_number = '$student_number' LIMIT 1";
$result = $conn->query($sql);
$user = $result->fetch_assoc();

$genericMsg = 'If the account exists, a reset email has been sent.';

if (!$user) {
    echo json_encode(["success" => true, "message" => $genericMsg]);
    $conn->close();
    exit();
}

// Generate secure token and expiry
$reset_token = bin2hex(random_bytes(32));
$expires = date('Y-m-d H:i:s', strtotime('+30 minutes'));

// Store token and expiry
$sql = "UPDATE students_new SET reset_token = '$reset_token', reset_token_expires = '$expires' WHERE id = {$user['id']}";
$conn->query($sql);

// Prepare reset email
$mail = new PHPMailer(true);
try {
    // SMTP setup
    $mail->isSMTP();
    $mail->Host       = 'smtp.gmail.com';
    $mail->SMTPAuth   = true;
    $mail->Username   = 'mohanreddy3539@gmail.com'; // Your Gmail address
    $mail->Password   = 'unfbzihceyppbhox'; // Your app password
    $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
    $mail->Port       = 587;
    $mail->CharSet    = 'UTF-8';

    $mail->setFrom('k.nobitha666@gmail.com', 'UniValut');
    $mail->addAddress($user['email'], $user['full_name']);

    $mail->isHTML(true);
    $mail->Subject = 'UniValut Password Reset Request';

    $reset_link = "http://192.168.224.54/UniValut/reset_password.php?token=" . $reset_token;

    $mail->Body = "
        Hi {$user['full_name']},<br><br>
        We received a request to reset your UniValut password.<br>
        Please click the link below to reset your password:<br>
        <a href='$reset_link'>$reset_link</a><br><br>
        This link will expire in 30 minutes.<br>
        If you did not request this, please ignore this email.
    ";

    $mail->send();
    echo json_encode(["success" => true, "message" => $genericMsg]);
} catch (Exception $e) {
    echo json_encode(["success" => false, "message" => "Reset email could not be sent. Mailer Error: {$mail->ErrorInfo}"]);
}

$conn->close();
exit();
?>
