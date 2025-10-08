<?php

include('db.php');

$message = "";
$success = false;

if (isset($_GET['token'])) {
    $token = $conn->real_escape_string($_GET['token']);
    $sql = "SELECT * FROM students_new WHERE verification_token='$token' AND verified=0";
    $result = $conn->query($sql);

    if ($result->num_rows > 0) {
        $update = $conn->query("UPDATE students_new SET verified=1, verification_token=NULL WHERE verification_token='$token'");
        if ($update) {
            $message = "✅ Email verified successfully! You can now login.";
            $success = true;
        } else {
            $message = "❌ Failed to verify email. Please try again.";
        }
    } else {
        $message = "⚠️ Invalid or expired token.";
    }
} else {
    $message = "⚠️ No token provided.";
}

$conn->close();
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Email Verification</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            padding: 0;
            font-family: Arial, sans-serif;
            background-color: #f4f6f9;
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
        }

        .container {
            background-color: #fff;
            padding: 24px;
            border-radius: 12px;
            max-width: 90%;
            width: 400px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
            text-align: center;
        }

        h2 {
            margin-bottom: 16px;
            color: #333;
            font-size: 22px;
        }

        p {
            font-size: 16px;
            color: #555;
            line-height: 1.5;
        }

        .success {
            color: green;
            font-weight: bold;
        }

        .error {
            color: red;
            font-weight: bold;
        }

        @media (max-width: 480px) {
            .container {
                padding: 18px;
            }

            h2 {
                font-size: 20px;
            }

            p {
                font-size: 15px;
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <h2>Email Verification</h2>
        <p class="<?= $success ? 'success' : 'error' ?>">
            <?= htmlspecialchars($message) ?>
        </p>
    </div>
</body>
</html>
