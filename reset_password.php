<?php
session_start();
$host = "localhost";
$user = "root";
$pass = "";
$db = "univalut_db";

$conn = new mysqli($host, $user, $pass, $db);
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// If form is submitted
if ($_SERVER["REQUEST_METHOD"] === "POST") {
    $token = $_POST["token"];
    $new_password = $_POST["new_password"];

    if (!$token || !$new_password) {
        $error = "Missing token or new password.";
    } else {
        $stmt = $conn->prepare("SELECT id, reset_token_expires FROM students_new WHERE reset_token = ?");
        $stmt->bind_param("s", $token);
        $stmt->execute();
        $result = $stmt->get_result();
        $user = $result->fetch_assoc();

        if (!$user) {
            $error = "Invalid or expired token.";
        } elseif (strtotime($user['reset_token_expires']) < time()) {
            $error = "Reset token has expired.";
        } else {
            // ✅ Reset password (NO hashing if you want plain text — not recommended)
            $stmt = $conn->prepare("UPDATE students_new SET password = ?, reset_token = NULL, reset_token_expires = NULL WHERE id = ?");
            $stmt->bind_param("si", $new_password, $user['id']);
            if ($stmt->execute()) {
                $success = "Password has been reset successfully!";
            } else {
                $error = "Error resetting password.";
            }
        }
    }
} else {
    $token = $_GET["token"] ?? "";
}
?>

<!DOCTYPE html>
<html>
<head>
    <title>Reset Password - UniValut</title>
</head>
<body>
    <h2>Reset Your Password</h2>

    <?php if (!empty($error)): ?>
        <p style="color:red"><?= htmlspecialchars($error) ?></p>
    <?php elseif (!empty($success)): ?>
        <p style="color:green"><?= htmlspecialchars($success) ?></p>
    <?php elseif (!empty($token)): ?>
        <form method="POST">
            <input type="hidden" name="token" value="<?= htmlspecialchars($token) ?>">
            <label>New Password:</label><br>
            <input type="password" name="new_password" required><br><br>
            <button type="submit">Reset Password</button>
        </form>
    <?php else: ?>
        <p>Invalid reset link.</p>
    <?php endif; ?>
</body>
</html>
