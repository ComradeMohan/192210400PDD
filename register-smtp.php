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

$full_name = $conn->real_escape_string($data["full_name"]);
$student_number = $conn->real_escape_string($data["student_number"]);
$email = $conn->real_escape_string($data["email"]);
$password = $conn->real_escape_string($data["password"]);
$department = $conn->real_escape_string($data["department"]);
$year_of_study = $conn->real_escape_string($data["year_of_study"]);
$college = $conn->real_escape_string($data["college"]);

// Check if already registered
$sql_check = "SELECT * FROM students_new WHERE student_number = '$student_number' OR email = '$email'";
$result = $conn->query($sql_check);
if ($result->num_rows > 0) {
    echo json_encode(["success" => false, "message" => "Student already exists"]);
    $conn->close();
    exit();
}

// Generate token
$verification_token = bin2hex(random_bytes(16));

// Insert student
$sql = "INSERT INTO students_new (full_name, student_number, email, password, department, year_of_study, college, verification_token, verified)
        VALUES ('$full_name', '$student_number', '$email', '$password', '$department', '$year_of_study', '$college', '$verification_token', 0)";

if ($conn->query($sql) === TRUE) {
    // Prepare email
    $mail = new PHPMailer(true);
    try {
        // SMTP setup
       // $mail->SMTPDebug = SMTP::DEBUG_SERVER; // Enable verbose debug output
        $mail->isSMTP();
        $mail->Host       = 'smtp.gmail.com';
        $mail->SMTPAuth   = true;
        $mail->Username   = 'mohanreddy3539@gmail.com'; // Your Gmail address
        $mail->Password   = 'unfbzihceyppbhox';                    // Your app password
        $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS; // STARTTLS encryption
        $mail->Port       = 587;                        // TLS port

        $mail->CharSet = 'UTF-8';

        // From email must be your Gmail or domain-authorized email
        $mail->setFrom('k.nobitha666@gmail.com', 'UniValut');
        $mail->addAddress($email, $full_name);

        $mail->isHTML(true);
        $mail->Subject = 'Verify your UniValut Account';

        $verification_link = "http://192.168.224.54/UniValut/verify_email.php?token=" . $verification_token;

        $mail->Body = "
            Hi $full_name,<br><br>
            Thank you for registering at UniValut.<br>
            Please verify your email by clicking the link below:<br>
            <a href='$verification_link'>$verification_link</a><br><br>
            If you did not register, please ignore this email.
        ";

        $mail->send();
        echo json_encode(["success" => true, "message" => "Registration successful"]);
    } catch (Exception $e) {
        echo json_encode(["success" => false, "message" => "Registration successful, but email could not be sent. Mailer Error: {$mail->ErrorInfo}"]);
    }
} else {
    echo json_encode(["success" => false, "message" => "Database error: " . $conn->error]);
}
exit();

$conn->close();
?>
