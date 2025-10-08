<?php
header("Content-Type: application/json");
error_reporting(E_ALL);
ini_set('display_errors', 1);

require 'vendor/autoload.php'; // Composer's autoloader
use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;
use PHPMailer\PHPMailer\SMTP;

// DB connection
include('db.php');

$expiry_time = date('Y-m-d H:i:s', time() - 3600); // 1 hour ago
$conn->query("DELETE FROM students_new WHERE verified = 0 AND created_at < '$expiry_time'");

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
        $mail->setFrom('no-reply@univault.com', 'UniVault');
        $mail->addAddress($email, $full_name);
        $mail->isHTML(true);
        $mail->Subject = 'Verify your UniVault Account';

        $verification_link = base_url('verify_email.php?token=' . $verification_token);

$mail->Body = "
<html>
<head>
  <style>
    body {
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      background-color: #f9f9f9;
      color: #333333;
      margin: 0;
      padding: 0;
    }
    .container {
      max-width: 600px;
      margin: 40px auto;
      background-color: #ffffff;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      padding: 30px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }
    h1 {
      color: #007BFF;
      font-weight: 700;
      margin-bottom: 20px;
    }
    p {
      font-size: 16px;
      line-height: 1.6;
      margin-bottom: 20px;
    }
    .button {
      display: inline-block;
      padding: 14px 28px;
      font-size: 16px;
      color: #ffffff;
      background-color: #007BFF;
      border-radius: 6px;
      text-decoration: none;
      font-weight: 600;
      box-shadow: 0 4px 12px rgba(0,123,255,0.3);
      transition: background-color 0.3s ease;
    }
    .button:hover {
      background-color: #0056b3;
    }
    .footer {
      font-size: 12px;
      color: #999999;
      margin-top: 30px;
      border-top: 1px solid #e0e0e0;
      padding-top: 15px;
    }
    a{
    color:#fff;
    }
  </style>
</head>
<body>
  <div class='container'>
    <h1>Welcome to UniVault, $full_name</h1>
    <p>Thank you for registering with UniVault. To complete your account setup and access all the features, please verify your email address.</p>
    <p>
      <a href='$verification_link' class='button'>Verify Your Email</a>
    </p>
    <p>If you did not initiate this registration, please disregard this email. No further action is required.</p>
    <div class='footer'>
      &copy; " . date('Y') . " UniVault. All rights reserved.<br>
      This is an automated message; please do not reply.
    </div>
  </div>
</body>
</html>
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