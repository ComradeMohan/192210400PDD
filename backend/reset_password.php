<?php
session_start();
include('db.php');

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
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<style>
		:root { color-scheme: light dark; }
		* { box-sizing: border-box; }
		html, body { margin: 0; padding: 0; font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; }
		body { background: #f5f7fb; color: #1f2937; }
		.container { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 16px; }
		.card { width: 100%; max-width: 420px; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 24px; box-shadow: 0 10px 30px rgba(0,0,0,0.06); }
		.card h2 { margin: 0 0 16px; font-size: 22px; line-height: 1.2; color: #111827; text-align: center; }
		.status { margin: 0 0 16px; font-size: 14px; padding: 12px; border-radius: 8px; }
		.status.error { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
		.status.success { background: #ecfdf5; color: #065f46; border: 1px solid #a7f3d0; }
		.form-group { margin-bottom: 14px; }
		label { display: block; margin-bottom: 6px; font-size: 14px; color: #374151; }
		input[type="password"] { width: 100%; padding: 12px 14px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 16px; background: #fff; color: inherit; }
		input[type="password"]:focus { outline: none; border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.2); }
		button { width: 100%; padding: 12px 16px; background: #3b82f6; color: #fff; border: 0; border-radius: 8px; font-weight: 600; font-size: 16px; cursor: pointer; }
		button:hover { background: #2563eb; }
		.footer { margin-top: 12px; font-size: 12px; color: #6b7280; text-align: center; }
		@media (max-width: 360px) {
			.card { padding: 18px; border-radius: 10px; }
			button, input[type="password"] { font-size: 15px; }
		}
	</style>
</head>
<body>
	<div class="container">
		<div class="card">
			<h2>Reset Your Password</h2>
			<?php if (!empty($error)): ?>
				<p class="status error"><?= htmlspecialchars($error) ?></p>
			<?php elseif (!empty($success)): ?>
				<p class="status success"><?= htmlspecialchars($success) ?></p>
			<?php elseif (!empty($token)): ?>
				<form method="POST">
					<input type="hidden" name="token" value="<?= htmlspecialchars($token) ?>">
					<div class="form-group">
						<label for="new_password">New Password</label>
						<input id="new_password" type="password" name="new_password" required>
					</div>
					<button type="submit">Reset Password</button>
					<div class="footer">&copy; <?= date('Y') ?> UniValut</div>
				</form>
			<?php else: ?>
				<p class="status error">Invalid reset link.</p>
			<?php endif; ?>
		</div>
	</div>
</body>
</html>
