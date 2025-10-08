<?php
// =========================================================================
// COMPREHENSIVE ADMIN PANEL SYSTEM
// =========================================================================

session_start();

// --- DATABASE CONFIGURATION AND CONNECTION ---
// Set your connection variables directly here.
$host = "localhost";
$user = "root";
$pass = ""; // Your MySQL password
$db = "univault_db2"; // <-- Confirmed DB name from your schema

$pdo = null; // Initialize PDO object

try {
    // Establish PDO connection
    $pdo = new PDO("mysql:host=$host;dbname=$db;charset=utf8", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    // Connection successful, $pdo is now available globally
} catch(PDOException $e) {
    // If connection fails, stop execution and show error
    die("<h1>Database Connection Failed</h1><p>Error: " . $e->getMessage() . "</p><p>Please verify your credentials and database status.</p>");
}

// Initialize message variable
$message = '';
$message_type = '';

// =========================================================================
// AUTHENTICATION FUNCTIONS
// =========================================================================

function isLoggedIn() {
    return isset($_SESSION['admin_logged_in']) && $_SESSION['admin_logged_in'] === true;
}

function requireLogin() {
    if (!isLoggedIn()) {
        header('Location: ?page=login');
        exit();
    }
}

// =========================================================================
// LOGIN HANDLER (Checks the 'admins' table using $pdo)
// =========================================================================

if (isset($_POST['login'])) {
    global $pdo; 

    $admin_id = $_POST['admin_id'] ?? '';
    $password_input = $_POST['password'] ?? '';
    
    try {
        $stmt = $pdo->prepare("SELECT admin_id, name, college, password FROM admins WHERE admin_id = ?");
        $stmt->execute([$admin_id]);
        $admin = $stmt->fetch(PDO::FETCH_ASSOC);

        // NOTE: Comparing directly against plaintext password as per your schema structure
        if ($admin && $admin['password'] === $password_input) { 
            $_SESSION['admin_logged_in'] = true;
            $_SESSION['admin_id'] = $admin['admin_id'];
            $_SESSION['admin_name'] = $admin['name'];
            $_SESSION['admin_college'] = $admin['college'];
            $message = "Login successful! Welcome, " . $admin['name'];
            $message_type = 'success';
        } else {
            $message = "Invalid Admin ID or Password.";
            $message_type = 'error';
        }
    } catch (Exception $e) {
        $message = "Login Error: " . $e->getMessage();
        $message_type = 'error';
    }
}

// Logout handler
if (isset($_GET['logout'])) {
    session_destroy();
    header('Location: ?page=login');
    exit();
}

// =========================================================================
// PASSWORD/PROFILE CHANGE HANDLER
// =========================================================================

if (isset($_POST['change_password']) && isLoggedIn()) {
    global $pdo;

    $current_password = $_POST['current_password'] ?? '';
    $new_password = $_POST['new_password'] ?? '';
    $confirm_password = $_POST['confirm_password'] ?? '';
    $admin_id = $_SESSION['admin_id'];
    
    if ($new_password === $confirm_password) {
        try {
            // Check current password
            $stmt = $pdo->prepare("SELECT password FROM admins WHERE admin_id = ?");
            $stmt->execute([$admin_id]);
            $admin = $stmt->fetch(PDO::FETCH_ASSOC);

            if ($admin && $admin['password'] === $current_password) {
                // Update password (using plain text as per your schema structure)
                $stmt = $pdo->prepare("UPDATE admins SET password = ? WHERE admin_id = ?");
                if ($stmt->execute([$new_password, $admin_id])) {
                    $message = "Password changed successfully!";
                    $message_type = 'success';
                }
            } else {
                $message = "Current password is incorrect.";
                $message_type = 'error';
            }
        } catch (Exception $e) {
             $message = "Error changing password: " . $e->getMessage();
             $message_type = 'error';
        }
    } else {
        $message = "New passwords do not match.";
        $message_type = 'error';
    }
}


// =========================================================================
// CRUD OPERATIONS AND SQL EXECUTION
// =========================================================================

// Generic CRUD handler
if (isLoggedIn() && isset($_POST['action'])) {
    global $pdo;

    $action = $_POST['action'];
    $table = $_POST['table'] ?? '';
    
    try {
        switch ($action) {
            case 'add':
                // Dynamically build INSERT query
                $post_data = $_POST;
                unset($post_data['action'], $post_data['table']);
                
                $fields = array_keys($post_data);
                $placeholders = str_repeat('?,', count($fields) - 1) . '?';
                $field_names = implode(',', $fields);
                $values = array_values($post_data);
                
                $sql = "INSERT INTO $table ($field_names) VALUES ($placeholders)";
                $stmt = $pdo->prepare($sql);
                $stmt->execute($values);
                
                $message = "Record added successfully to $table!";
                $message_type = 'success';
                break;
                
            case 'update':
                $id = $_POST['id'];
                $id_field = $_POST['id_field'] ?? 'id';
                
                $post_data = $_POST;
                unset($post_data['action'], $post_data['table'], $post_data['id'], $post_data['id_field']);
                
                $fields = array_keys($post_data);
                $set_clause = implode(' = ?, ', $fields) . ' = ?';
                
                $values = array_values($post_data);
                $values[] = $id; // ID for the WHERE clause
                
                $sql = "UPDATE $table SET $set_clause WHERE $id_field = ?";
                $stmt = $pdo->prepare($sql);
                $stmt->execute($values);
                
                $message = "Record updated successfully in $table!";
                $message_type = 'success';
                break;
                
            case 'delete':
                $id = $_POST['id'];
                $id_field = $_POST['id_field'] ?? 'id';
                
                $sql = "DELETE FROM $table WHERE $id_field = ?";
                $stmt = $pdo->prepare($sql);
                $stmt->execute([$id]);
                
                $message = "Record deleted successfully from $table!";
                $message_type = 'success';
                break;
        }
        // Redirect to prevent form resubmission
        header("Location: ?page=" . $_GET['page'] . "&message=" . urlencode($message) . "&type=" . $message_type);
        exit();
    } catch (Exception $e) {
        $message = "Error: " . $e->getMessage();
        $message_type = 'error';
    }
}

// SQL Query Executor Handler
if (isLoggedIn() && isset($_POST['execute_sql'])) {
    global $pdo;

    $query = trim($_POST['sql_query'] ?? '');
    
    if (empty($query)) {
        $message = "Query cannot be empty.";
        $message_type = 'error';
    } else {
        try {
            $query_type = strtoupper(substr(ltrim($query), 0, strpos(ltrim($query) . ' ', ' ')));

            $stmt = $pdo->prepare($query);
            $stmt->execute();
            
            if ($query_type === 'SELECT') {
                $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
                $_SESSION['sql_results'] = $results;
                $message = "Query executed successfully. Found " . count($results) . " rows.";
            } else {
                $message = "Query executed successfully. Rows affected: " . $stmt->rowCount();
                unset($_SESSION['sql_results']);
            }
            $message_type = 'success';

        } catch (Exception $e) {
            $message = "SQL Error: " . $e->getMessage();
            $message_type = 'error';
            unset($_SESSION['sql_results']);
        }
    }
    // Set message in session/URL for dashboard display
    $_SESSION['last_query'] = $_POST['sql_query'];
    header("Location: ?page=dashboard&message=" . urlencode($message) . "&type=" . $message_type);
    exit();
}

// --- FILE EDITOR HANDLER ---
if (isLoggedIn() && isset($_POST['save_file'])) {
    $filename = basename($_POST['filename'] ?? '');
    $content = $_POST['file_content'] ?? '';
    $filepath = __DIR__ . DIRECTORY_SEPARATOR . $filename;

    if (empty($filename) || $filename === '..' || !file_exists($filepath)) {
        $message = "Invalid file specified or file does not exist in the current directory.";
        $message_type = 'error';
    } else {
        // Prevent editing sensitive files beyond this one, for safety
        if ($filename !== 'admin_panel.php' && $filename !== 'db.php') {
             $message = "Editing restricted to admin_panel.php and db.php for security reasons.";
             $message_type = 'error';
        } else {
             if (file_put_contents($filepath, $content) !== false) {
                 $message = "File '$filename' saved successfully.";
                 $message_type = 'success';
             } else {
                 $message = "Error writing to file '$filename'. Check file permissions.";
                 $message_type = 'error';
             }
        }
    }
    header("Location: ?page=file_editor&filename=" . urlencode($filename) . "&message=" . urlencode($message) . "&type=" . $message_type);
    exit();
}

// --- FILE UPLOAD HANDLER ---
if (isLoggedIn() && isset($_POST['upload_file'])) {
    if (isset($_FILES['new_file']) && $_FILES['new_file']['error'] === UPLOAD_ERR_OK) {
        $file_tmp_path = $_FILES['new_file']['tmp_name'];
        $file_name = basename($_FILES['new_file']['name']);
        $dest_path = __DIR__ . DIRECTORY_SEPARATOR . $file_name;

        // Basic validation: Prevent overwriting crucial files like admin_panel.php itself
        if ($file_name === 'admin_panel.php' || $file_name === 'db.php') {
            $message = "Uploading files with that name is restricted.";
            $message_type = 'error';
        } 
        // Prevent path traversal
        else if (strpos($file_name, '..') !== false || strpos($file_name, '/') !== false) {
            $message = "Invalid characters in filename.";
            $message_type = 'error';
        }
        else {
            if (move_uploaded_file($file_tmp_path, $dest_path)) {
                $message = "File '{$file_name}' uploaded successfully to the folder.";
                $message_type = 'success';
            } else {
                $message = "Error moving uploaded file. Check folder permissions.";
                $message_type = 'error';
            }
        }
    } else {
        $message = "No file selected or upload error occurred. Error code: " . ($_FILES['new_file']['error'] ?? 'N/A');
        $message_type = 'error';
    }
    header("Location: ?page=file_editor&message=" . urlencode($message) . "&type=" . $message_type);
    exit();
}

// --- FILE DELETE HANDLER (NEW) ---
if (isLoggedIn() && isset($_POST['delete_file'])) {
    $filename = basename($_POST['filename'] ?? '');
    $filepath = __DIR__ . DIRECTORY_SEPARATOR . $filename;

    if (empty($filename) || $filename === '..' || $filename === '.' || !file_exists($filepath)) {
        $message = "Invalid file specified or file does not exist.";
        $message_type = 'error';
    } else {
        // Restriction for critical files
        if ($filename === 'admin_panel.php' || $filename === 'db.php') {
            $message = "Deletion of critical system files ({$filename}) is prohibited.";
            $message_type = 'error';
        } else {
            if (unlink($filepath)) {
                $message = "File '$filename' deleted successfully.";
                $message_type = 'success';
            } else {
                $message = "Error deleting file '$filename'. Check file permissions.";
                $message_type = 'error';
            }
        }
    }
    header("Location: ?page=file_editor&message=" . urlencode($message) . "&type=" . $message_type);
    exit();
}


// Check for URL messages after CRUD actions
if (isset($_GET['message'])) {
    $message = htmlspecialchars($_GET['message']);
    $message_type = htmlspecialchars($_GET['type']);
}

// Get current page
$page = $_GET['page'] ?? 'dashboard';

?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Univault Admin Panel</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
        }
        
        .container {
            max-width: 1400px;
            margin: 0 auto;
            padding: 20px;
        }
        
        .header {
            background: white;
            padding: 20px;
            border-radius: 10px;
            margin-bottom: 20px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .nav {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
        }
        
        .nav a {
            padding: 10px 15px;
            background: #667eea;
            color: white;
            text-decoration: none;
            border-radius: 5px;
            font-size: 14px;
        }
        
        .nav a:hover, .nav a.active {
            background: #764ba2;
        }
        
        .content {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        
        .message {
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 5px;
            font-weight: bold;
        }
        
        .message.success {
            background: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        
        .message.error {
            background: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        
        th, td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #ddd;
            font-size: 14px;
            word-break: break-word;
        }
        
        th {
            background-color: #f8f9fa;
            font-weight: 600;
        }
        
        .btn {
            padding: 8px 12px;
            background: #667eea;
            color: white;
            text-decoration: none;
            border-radius: 4px;
            border: none;
            cursor: pointer;
            font-size: 12px;
            margin: 2px;
            display: inline-block;
        }
        
        .btn:hover {
            background: #764ba2;
        }
        
        .btn.btn-danger {
            background: #dc3545;
        }
        
        .btn.btn-danger:hover {
            background: #c82333;
        }
        
        .form-group {
            margin-bottom: 15px;
        }
        
        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
        }
        
        .form-group input, .form-group select, .form-group textarea {
            width: 100%;
            padding: 8px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
        }
        
        .form-row {
            display: flex;
            gap: 15px;
        }
        
        .form-row .form-group {
            flex: 1;
        }
        
        .login-form {
            max-width: 400px;
            margin: 100px auto;
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        
        .stat-card {
            background: linear-gradient(135deg, #667eea, #764ba2);
            color: white;
            padding: 20px;
            border-radius: 10px;
            text-align: center;
        }
        
        .stat-number {
            font-size: 2em;
            font-weight: bold;
            margin-bottom: 10px;
        }
        
        .modal {
            display: none;
            position: fixed;
            z-index: 1000;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0,0,0,0.5);
        }
        
        .modal-content {
            background-color: white;
            margin: 10% auto;
            padding: 20px;
            border-radius: 10px;
            width: 80%;
            max-width: 600px; /* Increased max-width for forms */
        }
        
        .close {
            color: #aaa;
            float: right;
            font-size: 28px;
            font-weight: bold;
            cursor: pointer;
        }
        
        .close:hover {
            color: black;
        }

        .table-container {
            overflow-x: auto;
        }

        .action-buttons {
            white-space: nowrap;
        }

        @media (max-width: 768px) {
            .header {
                flex-direction: column;
                gap: 15px;
            }
            
            .nav {
                justify-content: center;
            }
            
            .form-row {
                flex-direction: column;
            }
            
            table {
                font-size: 10px;
            }
            
            th, td {
                padding: 6px;
            }
        }
    </style>
</head>
<body>

<?php if (!isLoggedIn()): ?>
    <!-- Login Form -->
    <div class="login-form">
        <h2 style="text-align: center; margin-bottom: 30px; color: #667eea;">Univault Admin Login</h2>
        
        <div class="message error">
            <p><strong>Database Login Required:</strong> Use credentials from the `admins` table (e.g., ID: `admin001`, Password: `admin`)</p>
        </div>
        
        <?php if ($message): ?>
            <div class="message <?php echo $message_type; ?>">
                <?php echo htmlspecialchars($message); ?>
            </div>
        <?php endif; ?>
        
        <form method="POST">
            <div class="form-group">
                <label for="admin_id">Admin ID:</label>
                <input type="text" id="admin_id" name="admin_id" required>
            </div>
            <div class="form-group">
                <label for="password">Password:</label>
                <input type="password" id="password" name="password" required>
            </div>
            <button type="submit" name="login" class="btn" style="width: 100%; padding: 12px;">Login</button>
        </form>
    </div>

<?php else: ?>
    <!-- Admin Panel -->
    <div class="container">
        <div class="header">
            <div>
                <h1 style="color: #667eea;">Univault Admin Panel</h1>
                <p>Logged In As: <strong><?php echo htmlspecialchars($_SESSION['admin_name']); ?></strong> | 
                    College Scope: <strong><?php echo htmlspecialchars($_SESSION['admin_college']); ?></strong></p>
            </div>
            <div class="nav">
                <a href="?page=dashboard" class="<?php echo $page === 'dashboard' ? 'active' : ''; ?>">Dashboard</a>
                <a href="?page=students" class="<?php echo $page === 'students' ? 'active' : ''; ?>">Students</a>
                <a href="?page=faculty" class="<?php echo $page === 'faculty' ? 'active' : ''; ?>">Faculty</a>
                <a href="?page=courses" class="<?php echo $page === 'courses' ? 'active' : ''; ?>">Courses</a>
                <a href="?page=departments" class="<?php echo $page === 'departments' ? 'active' : ''; ?>">Departments</a>
                <a href="?page=colleges" class="<?php echo $page === 'colleges' ? 'active' : ''; ?>">Colleges</a>
                <a href="?page=notices" class="<?php echo $page === 'notices' ? 'active' : ''; ?>">Notices</a>
                <a href="?page=events" class="<?php echo $page === 'events' ? 'active' : ''; ?>">Events</a>
                <a href="?page=feedbacks" class="<?php echo $page === 'feedbacks' ? 'active' : ''; ?>">Feedbacks</a>
                <a href="?page=prepcourses" class="<?php echo $page === 'prepcourses' ? 'active' : ''; ?>">Prep Courses</a>
                <a href="?page=topics" class="<?php echo $page === 'topics' ? 'active' : ''; ?>">Topics</a>
                <a href="?page=mcq_questions" class="<?php echo $page === 'mcq_questions' ? 'active' : ''; ?>">MCQ Qs</a>
                <a href="?page=theory_questions" class="<?php echo $page === 'theory_questions' ? 'active' : ''; ?>">Theory Qs</a>
                <a href="?page=mcq_results" class="<?php echo $page === 'mcq_results' ? 'active' : ''; ?>">MCQ Results</a>
                <a href="?page=theory_results" class="<?php echo $page === 'theory_results' ? 'active' : ''; ?>">Theory Results</a>
                <a href="?page=grades" class="<?php echo $page === 'grades' ? 'active' : ''; ?>">Grades</a>
                <a href="?page=admins" class="<?php echo $page === 'admins' ? 'active' : ''; ?>">Admins</a>
                <a href="?page=profile" class="<?php echo $page === 'profile' ? 'active' : ''; ?>">Profile</a>
                <a href="?page=file_editor" class="<?php echo $page === 'file_editor' ? 'active' : ''; ?>">File Editor</a>
                <a href="?logout=true" style="background: #dc3545;">Logout</a>
            </div>
        </div>
        
        <div class="content">
            <?php if ($message): ?>
                <div class="message <?php echo $message_type; ?>">
                    <?php echo htmlspecialchars($message); ?>
                </div>
            <?php endif; ?>

            <?php
            // Page content based on current page
            switch ($page) {
                case 'dashboard':
                    include_dashboard($pdo);
                    break;
                case 'students':
                    include_students($pdo);
                    break;
                case 'faculty':
                    include_faculty($pdo);
                    break;
                case 'courses':
                    include_courses($pdo);
                    break;
                case 'colleges':
                    include_colleges($pdo);
                    break;
                case 'departments':
                    include_departments($pdo);
                    break;
                case 'notices':
                    include_notices($pdo);
                    break;
                case 'events':
                    include_events($pdo);
                    break;
                case 'feedbacks':
                    include_feedbacks($pdo);
                    break;
                case 'prepcourses':
                    include_prepcourses($pdo);
                    break;
                case 'topics':
                    include_topics($pdo);
                    break;
                case 'mcq_questions':
                    include_mcq_questions($pdo);
                    break;
                case 'theory_questions':
                    include_theory_questions($pdo);
                    break;
                case 'mcq_results':
                    include_mcq_results($pdo);
                    break;
                case 'theory_results':
                    include_theory_results($pdo);
                    break;
                case 'grades': 
                    include_grades($pdo);
                    break;
                case 'admins': 
                    include_admins($pdo);
                    break;
                case 'profile':
                    include_profile($pdo);
                    break;
                case 'file_editor':
                    include_file_editor();
                    break;
                default:
                    include_dashboard($pdo);
            }
            ?>
        </div>
    </div>
<?php endif; ?>

<script>
function openModal(modalId) {
    document.getElementById(modalId).style.display = 'block';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

function confirmDelete(form) {
    if (confirm('Are you sure you want to delete this record? This action cannot be undone.')) {
        form.submit();
    }
    return false; // Prevent default submit if confirm is canceled
}

function populateEditForm(data, formId) {
    const form = document.getElementById(formId);
    if (!form) {
        console.error('Form not found:', formId);
        return;
    }
    
    // Set form hidden fields
    form.querySelector('input[name="id"]').value = data.id || data.course_id || data.topic_id || data.question_id || data.test_result_id; 
    
    // Ensure the ID field name is correctly set for generic update
    const idFieldElement = form.querySelector('input[name="id_field"]');
    if (idFieldElement) {
        // Determine the correct primary key name
        const pk = data.id ? 'id' : (data.course_id ? 'course_id' : (data.topic_id ? 'topic_id' : (data.question_id ? 'question_id' : (data.test_result_id ? 'test_result_id' : 'id'))));
        idFieldElement.value = pk;
    }

    for (const [key, value] of Object.entries(data)) {
        const field = form.querySelector(`[name="${key}"]`);
        if (field) {
            if (field.tagName === 'SELECT') {
                // Set value for SELECT elements
                field.value = value;
            } else if (key === 'password' || key === 'current_password' || key === 'new_password' || key === 'confirm_password') {
                // Never pre-fill password fields with values
                field.value = '';
            } else {
                field.value = value;
            }
        }
    }
    openModal(formId.replace('Form', 'Modal'));
}

// Close modal when clicking outside
window.onclick = function(event) {
    if (event.target.classList.contains('modal')) {
        event.target.style.display = 'none';
    }
}
</script>

</body>
</html>

<?php
// =========================================================================
// PAGE FUNCTION DEFINITIONS
// =========================================================================

function include_dashboard($pdo) {
    ?>
    <h2>System Dashboard Overview</h2>
    
    <div class="stats-grid">
        <?php
        $tables = [
            'students_new' => 'Students',
            'faculty_new' => 'Faculty',
            'departments_new' => 'Departments',
            'courses_new' => 'Courses',
            'prepcourses' => 'Prep Courses',
            'mcq_combined' => 'MCQ Questions',
            'theory_questions' => 'Theory Questions',
            'notices' => 'Notices',
            'events_new' => 'Events'
        ];
        
        foreach ($tables as $table => $label) {
            try {
                $stmt = $pdo->query("SELECT COUNT(*) as count FROM $table");
                $count = $stmt->fetchColumn();
                ?>
                <div class="stat-card">
                    <div class="stat-number"><?php echo $count; ?></div>
                    <div><?php echo $label; ?></div>
                </div>
                <?php
            } catch (Exception $e) {
                // Handle cases where a table might not exist or connection fails
                echo "<div class=\"stat-card\" style=\"background: #e74c3c;\">
                        <div class=\"stat-number\">ERROR</div>
                        <div>$label (Table Missing)</div>
                      </div>";
            }
        }
        ?>
    </div>
    
    <h3>SQL Query Executor</h3>
    <p>Run custom SQL commands directly against the <code>univault_db</code> database. Use caution when using INSERT, UPDATE, or DELETE commands.</p>
    <div style="background: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 20px;">
        <form method="POST" action="?page=dashboard">
            <div class="form-group">
                <label for="sql_query">SQL Query:</label>
                <textarea id="sql_query" name="sql_query" rows="5" required><?php echo htmlspecialchars($_SESSION['last_query'] ?? 'SELECT * FROM students_new LIMIT 5'); ?></textarea>
            </div>
            <button type="submit" name="execute_sql" class="btn" style="background: #007bff;">Execute Query</button>
        </form>
        <?php 
        // Display SQL results if available in session
        if (isset($_SESSION['sql_results']) && !empty($_SESSION['sql_results'])): 
            $results = $_SESSION['sql_results'];
            // Clear session to prevent display on next page load unless new query is run
            $_SESSION['last_query'] = $_POST['sql_query'];
            unset($_SESSION['sql_results']); 
        ?>
            <h4 style="margin-top: 15px; color: #1565c0;">Query Results (<?php echo count($results); ?> rows)</h4>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <?php foreach (array_keys($results[0]) as $col): ?>
                                <th><?php echo htmlspecialchars($col); ?></th>
                            <?php endforeach; ?>
                        </tr>
                    </thead>
                    <tbody>
                        <?php foreach ($results as $row): ?>
                            <tr>
                                <?php foreach ($row as $data): ?>
                                    <td><?php echo htmlspecialchars(substr($data ?? 'NULL', 0, 100)); ?></td>
                                <?php endforeach; ?>
                            </tr>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
        <?php elseif (isset($_SESSION['last_query'])): ?>
             <?php unset($_SESSION['last_query']); // Clear after showing if no results ?>
        <?php endif; ?>

    </div>
    
    <h3>Recent Activities</h3>
    <div style="background: #f8f9fa; padding: 15px; border-radius: 5px;">
        <p>Welcome to the Univault Admin Panel. Use the navigation menu to manage different aspects of the system.</p>
    </div>
    <?php
}

// --- FILE EDITOR MANAGEMENT (UPDATED with File List and Delete Functionality) ---
function include_file_editor() {
    $selected_file = $_GET['filename'] ?? 'admin_panel.php';
    $filepath = __DIR__ . DIRECTORY_SEPARATOR . basename($selected_file); // Sanitize the path
    $file_content = '';
    $current_dir_files = [];
    $is_file_editable = false;

    // Get list of files in the current directory (for the table)
    $files = scandir(__DIR__);
    foreach ($files as $file) {
        if (!is_dir($file) && !in_array($file, ['.', '..'])) {
            $current_dir_files[] = $file;
        }
    }
    sort($current_dir_files); // Sort files alphabetically

    if (file_exists($filepath) && !is_dir($filepath)) {
        if (is_readable($filepath)) {
            $file_content = htmlspecialchars(file_get_contents($filepath));
            $is_file_editable = (basename($selected_file) === 'admin_panel.php' || basename($selected_file) === 'db.php');
        } else {
            $GLOBALS['message'] = "Error: File is not readable.";
            $GLOBALS['message_type'] = 'error';
            $selected_file = 'admin_panel.php'; // Fallback
        }
    } else {
        $GLOBALS['message'] = "File not found or invalid path.";
        $GLOBALS['message_type'] = 'error';
        $selected_file = 'admin_panel.php'; // Fallback
    }

    ?>
    <h2>Developer File Management</h2>
    <p>Use this tool to manage files in the current directory (<code><?php echo basename(__DIR__); ?>/</code>). Editing is restricted to system files for security.</p>
    
    <div class="form-row" style="margin-bottom: 20px;">
        <!-- File Upload Utility -->
        <div class="form-group" style="flex: none; width: 300px; padding: 15px; border: 1px solid #ddd; border-radius: 8px;">
            <h3>Upload New File</h3>
            <form method="POST" enctype="multipart/form-data" action="?page=file_editor">
                <input type="hidden" name="upload_file" value="1">
                <label for="new_file">Select File:</label>
                <input type="file" id="new_file" name="new_file" required style="margin-bottom: 10px;">
                <button type="submit" class="btn" style="width: 100%;">Upload to Current Folder</button>
            </form>
        </div>
    </div>

    <!-- File List Table -->
    <h3>Current Directory Files (<?php echo count($current_dir_files); ?> Files)</h3>
    <div class="table-container" style="margin-top: 20px;">
        <table>
            <thead>
                <tr>
                    <th>Filename</th>
                    <th>Size</th>
                    <th>Last Modified</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($current_dir_files as $file): 
                    $fpath = __DIR__ . DIRECTORY_SEPARATOR . $file;
                    $is_critical = in_array($file, ['admin_panel.php', 'db.php']);
                    $is_currently_editing = (basename($selected_file) === $file);
                ?>
                <tr>
                    <td>
                        <a href="?page=file_editor&filename=<?php echo urlencode($file); ?>" style="color: #667eea; font-weight: bold;"><?php echo htmlspecialchars($file); ?></a>
                        <?php if ($is_critical): ?>
                            <span style="color: #dc3545; font-size: 0.8em;"> (Critical)</span>
                        <?php endif; ?>
                    </td>
                    <td><?php echo file_exists($fpath) ? round(filesize($fpath) / 1024, 1) . ' KB' : 'N/A'; ?></td>
                    <td><?php echo file_exists($fpath) ? date('Y-m-d H:i:s', filemtime($fpath)) : 'N/A'; ?></td>
                    <td class="action-buttons">
                        
                        <?php if ($is_currently_editing): ?>
                            <span style="color: #667eea; font-size: 0.9em;">(Editing)</span>
                        <?php else: ?>
                            <a href="?page=file_editor&filename=<?php echo urlencode($file); ?>" class="btn" style="background: #3498db;">Load to Edit</a>
                        <?php endif; ?>

                        <?php if (!$is_critical): ?>
                            <form method="POST" style="display: inline;" onsubmit="return confirmDelete(this);">
                                <input type="hidden" name="delete_file" value="1">
                                <input type="hidden" name="filename" value="<?php echo htmlspecialchars($file); ?>">
                                <button type="submit" class="btn btn-danger">Delete</button>
                            </form>
                        <?php endif; ?>
                    </td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>

    <!-- Code Editor Form -->
    <?php if (file_exists($filepath)): ?>
        <h3 style="margin-top: 30px;">Editing: <code><?php echo htmlspecialchars(basename($filepath)); ?></code></h3>
        <form method="POST" action="?page=file_editor">
            <input type="hidden" name="filename" value="<?php echo htmlspecialchars(basename($filepath)); ?>">
            <input type="hidden" name="save_file" value="1">
            <div class="form-group">
                <textarea name="file_content" rows="25" style="font-family: monospace; font-size: 13px;"><?php echo $file_content; ?></textarea>
            </div>
            
            <?php if ($is_file_editable): ?>
                <button type="submit" class="btn" style="background: #4caf50;">Save Changes</button>
            <?php else: ?>
                <button type="button" class="btn" style="background: #999; cursor: not-allowed;" disabled>Saving Disabled</button>
                <span style="color: #dc3545; margin-left: 10px; font-size: 0.9em;">(Only admin_panel.php and db.php can be saved)</span>
            <?php endif; ?>

            <a href="?page=file_editor" class="btn">Cancel Editing</a>
        </form>
    <?php endif; ?>
    <?php
}

// --- PROFILE MANAGEMENT ---
function include_profile($pdo) {
    requireLogin();
    $admin_id = $_SESSION['admin_id'];

    // Fetch current admin details
    $stmt = $pdo->prepare("SELECT admin_id, name, phone_number, email, college FROM admins WHERE admin_id = ?");
    $stmt->execute([$admin_id]);
    $admin = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$admin) {
        echo "<p class='message error'>Error: Admin record not found.</p>";
        return;
    }

    // Handle Profile Update
    if (isset($_POST['update_profile'])) {
        $name = $_POST['name'] ?? '';
        $phone_number = $_POST['phone_number'] ?? '';
        $email = $_POST['email'] ?? '';
        $college = $_POST['college'] ?? '';

        try {
            $stmt = $pdo->prepare("UPDATE admins SET name = ?, phone_number = ?, email = ?, college = ? WHERE admin_id = ?");
            if ($stmt->execute([$name, $phone_number, $email, $college, $admin_id])) {
                // Update session variables
                $_SESSION['admin_name'] = $name;
                $_SESSION['admin_college'] = $college;
                // Redirect to display success message
                header("Location: ?page=profile&message=" . urlencode("Profile updated successfully!") . "&type=success");
                exit();
            }
        } catch (Exception $e) {
            $GLOBALS['message'] = "Profile update failed: " . $e->getMessage();
            $GLOBALS['message_type'] = 'error';
        }
    }

    ?>
    <h2>Admin Profile: <?php echo htmlspecialchars($admin['name']); ?></h2>
    
    <div style="max-width: 600px;">
        
        <!-- Profile Details Form -->
        <h3>Update Details</h3>
        <form method="POST">
            <input type="hidden" name="update_profile" value="1">
            <div class="form-row">
                <div class="form-group"><label>Admin ID:</label><input type="text" value="<?php echo htmlspecialchars($admin['admin_id']); ?>" readonly></div>
                <div class="form-group"><label>Full Name:</label><input type="text" name="name" value="<?php echo htmlspecialchars($admin['name']); ?>" required></div>
            </div>
            <div class="form-row">
                <div class="form-group"><label>Email:</label><input type="email" name="email" value="<?php echo htmlspecialchars($admin['email']); ?>" required></div>
                <div class="form-group"><label>Phone Number:</label><input type="text" name="phone_number" value="<?php echo htmlspecialchars($admin['phone_number']); ?>"></div>
            </div>
            <div class="form-group"><label>College:</label><input type="text" name="college" value="<?php echo htmlspecialchars($admin['college']); ?>" required></div>
            <button type="submit" class="btn">Save Profile</button>
        </form>

        <h3 style="margin-top: 30px;">Change Password</h3>
        <form method="POST" onsubmit="return confirm('Are you sure you want to change your password?')">
            <input type="hidden" name="change_password" value="1">
            <div class="form-group">
                <label for="current_password">Current Password:</label>
                <input type="password" id="current_password" name="current_password" required>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label for="new_password">New Password:</label>
                    <input type="password" id="new_password" name="new_password" required>
                </div>
                <div class="form-group">
                    <label for="confirm_password">Confirm New Password:</label>
                    <input type="password" id="confirm_password" name="confirm_password" required>
                </div>
            </div>
            <button type="submit" class="btn btn-danger">Change Password</button>
        </form>
    </div>
    <?php
}

// --- ADMINS MANAGEMENT ---
function include_admins($pdo) {
    $table_name = 'admins';
    $primary_key = 'id';
    $display_fields = [
        'id' => 'ID', 'admin_id' => 'Admin ID', 'name' => 'Name', 'email' => 'Email', 
        'phone_number' => 'Phone', 'college' => 'College', 'created_at' => 'Created'
    ];
    $form_fields = [
        'admin_id' => 'text', 'name' => 'text', 'email' => 'email', 'phone_number' => 'text', 
        'college' => 'text', 'password' => 'password'
    ];
    
    // Fetch all admins
    $stmt = $pdo->query("SELECT id, admin_id, name, phone_number, email, college, created_at FROM $table_name ORDER BY id DESC");
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'Admin');
}

// --- STUDENTS MANAGEMENT ---
function include_students($pdo) {
    $table_name = 'students_new';
    $primary_key = 'id';
    $display_fields = [
        'id' => 'ID',
        'full_name' => 'Full Name',
        'student_number' => 'Student ID',
        'email' => 'Email',
        'department' => 'Dept.',
        'year_of_study' => 'Year',
        'college' => 'College',
        'created_at' => 'Created At',
        'verification_token' => 'Verification Token',
        'verified' => 'Verified (0/1)',
        'reset_token_expires' => 'Token Expires'
    ];
    $form_fields = [
        'full_name' => 'text', 'student_number' => 'text', 'email' => 'email', 'password' => 'password', 
        'department' => 'text', 'year_of_study' => 'text', 'college' => 'text', 'verified' => 'number',
        'verification_token' => 'text', 'reset_token' => 'text'
    ];
    
    $query = "SELECT id, full_name, student_number, email, department, year_of_study, college, created_at, verification_token, verified, reset_token_expires 
              FROM $table_name ORDER BY id DESC";
    $stmt = $pdo->query($query);
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    ?>
    <h2>Students Management (<?php echo $table_name; ?>)</h2>
    <button onclick="openModal('addStudentModal')" class="btn">Add New Student</button>
    
    <div class="table-container">
        <table>
            <thead>
                <tr>
                    <?php foreach ($display_fields as $key => $label): ?>
                        <th><?php echo $label; ?></th>
                    <?php endforeach; ?>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($records as $row): ?>
                <tr data-record='<?php echo json_encode($row); ?>'>
                    <?php foreach ($display_fields as $key => $label): ?>
                        <td><?php echo htmlspecialchars($row[$key] ?? 'NULL'); ?></td>
                    <?php endforeach; ?>
                    <td class="action-buttons">
                        <button onclick="populateEditForm(<?php echo htmlspecialchars(json_encode($row)); ?>, 'editStudentForm')" class="btn">Edit</button>
                        <form method="POST" style="display: inline;" onsubmit="return confirmDelete(this);">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="table" value="<?php echo $table_name; ?>">
                            <input type="hidden" name="id_field" value="<?php echo $primary_key; ?>">
                            <input type="hidden" name="id" value="<?php echo $row[$primary_key]; ?>">
                            <button type="submit" class="btn btn-danger">Delete</button>
                        </form>
                    </td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>

    <!-- Add Student Modal -->
    <div id="addStudentModal" class="modal">
        <div class="modal-content">
            <span class="close" onclick="closeModal('addStudentModal')">&times;</span>
            <h3>Add New Student</h3>
            <form id="addStudentForm" method="POST">
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="table" value="<?php echo $table_name; ?>">
                <div class="form-row">
                    <div class="form-group"><label>Full Name:</label><input type="text" name="full_name" required></div>
                    <div class="form-group"><label>Student ID:</label><input type="text" name="student_number" required></div>
                </div>
                <div class="form-row">
                    <div class="form-group"><label>Email:</label><input type="email" name="email" required></div>
                    <div class="form-group"><label>Password:</label><input type="password" name="password" required></div>
                </div>
                <div class="form-row">
                    <div class="form-group"><label>Department:</label><input type="text" name="department" required></div>
                    <div class="form-group"><label>Year of Study:</label><input type="text" name="year_of_study" required></div>
                </div>
                <div class="form-group"><label>College:</label><input type="text" name="college" required></div>
                <button type="submit" class="btn">Add Student</button>
            </form>
        </div>
    </div>

    <!-- Edit Student Modal -->
    <div id="editStudentModal" class="modal">
        <div class="modal-content">
            <span class="close" onclick="closeModal('editStudentModal')">&times;</span>
            <h3>Edit Student Record</h3>
            <form id="editStudentForm" method="POST">
                <input type="hidden" name="action" value="update">
                <input type="hidden" name="table" value="<?php echo $table_name; ?>">
                <input type="hidden" name="id_field" value="<?php echo $primary_key; ?>">
                <input type="hidden" name="id" value="">
                <div class="form-row">
                    <div class="form-group"><label>Full Name:</label><input type="text" name="full_name" required></div>
                    <div class="form-group"><label>Student ID:</label><input type="text" name="student_number" required></div>
                </div>
                <div class="form-row">
                    <div class="form-group"><label>Email:</label><input type="email" name="email" required></div>
                    <div class="form-group"><label>New Password (Optional):</label><input type="password" name="password"></div>
                </div>
                <div class="form-row">
                    <div class="form-group"><label>Department:</label><input type="text" name="department" required></div>
                    <div class="form-group"><label>Year of Study:</label><input type="text" name="year_of_study" required></div>
                </div>
                <div class="form-group"><label>College:</label><input type="text" name="college" required></div>
                
                <h4>Administrative Fields:</h4>
                <div class="form-row">
                    <div class="form-group"><label>Verified (0=No, 1=Yes):</label><input type="number" name="verified" required min="0" max="1"></div>
                    <div class="form-group"><label>Verification Token:</label><input type="text" name="verification_token"></div>
                </div>
                <div class="form-row">
                    <div class="form-group"><label>Reset Token:</label><input type="text" name="reset_token"></div>
                    <div class="form-group"><label>Token Expires (Optional):</label><input type="text" name="reset_token_expires"></div>
                </div>
                <button type="submit" class="btn">Update Student</button>
            </form>
        </div>
    </div>
    <?php
}

// --- FACULTY MANAGEMENT ---
function include_faculty($pdo) {
    $table_name = 'faculty_new';
    $primary_key = 'id';
    $display_fields = [
        'id' => 'ID', 'name' => 'Name', 'login_id' => 'Login ID', 'email' => 'Email', 
        'phone_number' => 'Phone', 'college' => 'College'
    ];
    $form_fields = [
        'name' => 'text', 'login_id' => 'text', 'email' => 'email', 'password' => 'password',
        'phone_number' => 'text', 'college' => 'text'
    ];
    
    $stmt = $pdo->query("SELECT * FROM $table_name ORDER BY id DESC");
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'Faculty');
}

// --- COURSES MANAGEMENT (Foreign Key Lookup) ---
function include_courses($pdo) {
    $table_name = 'courses_new';
    $primary_key = 'id';
    $display_fields = [
        'id' => 'ID',
        'course_name_display' => 'Course/Code',
        'department_name' => 'Department/College',
        'credits' => 'Credits'
    ];

    $query = "
        SELECT 
            c.*, 
            d.name as department_name, 
            col.name as college_name
        FROM courses_new c
        LEFT JOIN departments_new d ON c.department_id = d.id
        LEFT JOIN colleges col ON d.college_id = col.id
        ORDER BY c.id DESC
    ";
    $stmt = $pdo->query($query);
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Get departments for select box
    $departments = $pdo->query("SELECT d.id, d.name, c.name as college_name FROM departments_new d LEFT JOIN colleges c ON d.college_id = c.id ORDER BY d.name");
    $department_options = $departments->fetchAll(PDO::FETCH_ASSOC);
    ?>
    <h2>Courses Management (<?php echo $table_name; ?>)</h2>
    <button onclick="openModal('addCourseModal')" class="btn">Add New Course</button>
    
    <div class="table-container">
        <table>
            <thead>
                <tr>
                    <?php foreach ($display_fields as $key => $label): ?>
                        <th><?php echo $label; ?></th>
                    <?php endforeach; ?>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($records as $row): ?>
                <tr data-record='<?php echo json_encode($row); ?>'>
                    <td><?php echo $row['id']; ?></td>
                    <td><?php echo htmlspecialchars($row['name']); ?></td>
                    <td><?php echo htmlspecialchars($row['department_name'] . ' (' . ($row['college_name'] ?? 'N/A') . ')'); ?></td>
                    <td><?php echo htmlspecialchars($row['credits']); ?></td>
                    <td class="action-buttons">
                        <button onclick="populateEditForm(<?php echo htmlspecialchars(json_encode($row)); ?>, 'editCourseForm')" class="btn">Edit</button>
                        <form method="POST" style="display: inline;" onsubmit="return confirmDelete(this);">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="table" value="<?php echo $table_name; ?>">
                            <input type="hidden" name="id_field" value="<?php echo $primary_key; ?>">
                            <input type="hidden" name="id" value="<?php echo $row[$primary_key]; ?>">
                            <button type="submit" class="btn btn-danger">Delete</button>
                        </form>
                    </td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>

    <!-- Add/Edit Course Modals (Simplified - uses 'name' for combined info) -->
    <?php include_course_modals($table_name, $primary_key, $department_options); ?>

    <?php
}

function include_course_modals($table_name, $primary_key, $department_options) {
    ?>
    <!-- Add Course Modal -->
    <div id="addCourseModal" class="modal">
        <div class="modal-content">
            <span class="close" onclick="closeModal('addCourseModal')">&times;</span>
            <h3>Add New Course</h3>
            <form method="POST">
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="table" value="<?php echo $table_name; ?>">
                <div class="form-group">
                    <label>Department:</label>
                    <select name="department_id" required>
                        <option value="">Select Department</option>
                        <?php foreach ($department_options as $dept): ?>
                            <option value="<?php echo $dept['id']; ?>"><?php echo htmlspecialchars("{$dept['name']} ({$dept['college_name']})"); ?></option>
                        <?php endforeach; ?>
                    </select>
                </div>
                <div class="form-group"><label>Course Name (e.g., CSA05 - Database Management Systems):</label><input type="text" name="name" required></div>
                <div class="form-group"><label>Credits:</label><input type="number" name="credits" required min="1"></div>
                <button type="submit" class="btn">Add Course</button>
            </form>
        </div>
    </div>

    <!-- Edit Course Modal -->
    <div id="editCourseModal" class="modal">
        <div class="modal-content">
            <span class="close" onclick="closeModal('editCourseModal')">&times;</span>
            <h3>Edit Course</h3>
            <form id="editCourseForm" method="POST">
                <input type="hidden" name="action" value="update">
                <input type="hidden" name="table" value="<?php echo $table_name; ?>">
                <input type="hidden" name="id_field" value="<?php echo $primary_key; ?>">
                <input type="hidden" name="id" value="">
                <div class="form-group">
                    <label>Department:</label>
                    <select name="department_id" required>
                        <option value="">Select Department</option>
                        <?php foreach ($department_options as $dept): ?>
                            <option value="<?php echo $dept['id']; ?>"><?php echo htmlspecialchars("{$dept['name']} ({$dept['college_name']})"); ?></option>
                        <?php endforeach; ?>
                    </select>
                </div>
                <div class="form-group"><label>Course Name (e.g., CSA05 - Database Management Systems):</label><input type="text" name="name" required></div>
                <div class="form-group"><label>Credits:</label><input type="number" name="credits" required min="1"></div>
                <button type="submit" class="btn">Update Course</button>
            </form>
        </div>
    </div>
    <?php
}

// --- DEPARTMENTS MANAGEMENT (Foreign Key Lookup) ---
function include_departments($pdo) {
    $table_name = 'departments_new';
    $primary_key = 'id';
    $display_fields = [
        'id' => 'ID',
        'college_name' => 'College',
        'name' => 'Department Name'
    ];
    $form_fields = [
        'college_id' => 'select', 
        'name' => 'text'
    ];
    
    $query = "SELECT d.id, d.name, d.college_id, c.name as college_name 
              FROM departments_new d 
              LEFT JOIN colleges c ON d.college_id = c.id 
              ORDER BY d.id DESC";
    $stmt = $pdo->query($query);
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Get colleges for select box
    $colleges = $pdo->query("SELECT id, name FROM colleges ORDER BY name");
    $college_options = $colleges->fetchAll(PDO::FETCH_ASSOC);

    // Pass $college_options as $fk_data for the 'college_id' field
    $fk_data = [
        'college_id' => ['options' => $college_options, 'key_field' => 'id', 'display_field' => 'name']
    ];

    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'Department', $fk_data);
}

// --- COLLEGES MANAGEMENT ---
function include_colleges($pdo) {
    $table_name = 'colleges';
    $primary_key = 'id';
    $display_fields = ['id' => 'ID', 'name' => 'College Name'];
    $form_fields = ['name' => 'text'];
    
    $stmt = $pdo->query("SELECT * FROM $table_name ORDER BY id DESC");
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'College');
}

// --- NOTICES MANAGEMENT ---
function include_notices($pdo) {
    $table_name = 'notices';
    $primary_key = 'id';
    $display_fields = [
        'id' => 'ID', 'title' => 'Title', 'description' => 'Description', 
        'college' => 'College', 'schedule_date' => 'Date', 'schedule_time' => 'Time', 
        'is_high_priority' => 'Priority'
    ];
    $form_fields = [
        'title' => 'text', 'description' => 'textarea', 'college' => 'text', 
        'schedule_date' => 'date', 'schedule_time' => 'time', 'is_high_priority' => 'number'
    ];
    
    $stmt = $pdo->query("SELECT * FROM $table_name ORDER BY id DESC");
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'Notice');
}

// --- EVENTS MANAGEMENT ---
function include_events($pdo) {
    $table_name = 'events_new';
    $primary_key = 'id';
    $display_fields = [
        'id' => 'ID', 'college_name' => 'College', 'title' => 'Title', 
        'type' => 'Type', 'description' => 'Description', 'start_date' => 'Start Date', 
        'end_date' => 'End Date'
    ];
    $form_fields = [
        'college_name' => 'text', 'title' => 'text', 'type' => 'text', 
        'description' => 'textarea', 'start_date' => 'date', 'end_date' => 'date'
    ];
    
    $stmt = $pdo->query("SELECT * FROM $table_name ORDER BY id DESC");
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'Event');
}

// --- FEEDBACKS MANAGEMENT ---
function include_feedbacks($pdo) {
    $table_name = 'feedbacks';
    $primary_key = 'id';
    $display_fields = [
        'id' => 'ID', 'user_id' => 'User ID', 'feedback' => 'Feedback', 
        'college' => 'College', 'created_at' => 'Submitted At'
    ];
    // No add/edit form for feedback, only view/delete
    $form_fields = [];
    
    $stmt = $pdo->query("SELECT * FROM $table_name ORDER BY id DESC");
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'Feedback');
}

// --- PREP COURSES MANAGEMENT ---
function include_prepcourses($pdo) {
    $table_name = 'prepcourses';
    $primary_key = 'course_id';
    $display_fields = [
        'course_id' => 'ID', 'course_code' => 'Code', 'course_name' => 'Name', 
        'description' => 'Description', 'topic_count' => 'Topics', 'created_at' => 'Created'
    ];
    $form_fields = [
        'course_code' => 'text', 'course_name' => 'text', 'description' => 'textarea', 'topic_count' => 'number'
    ];
    
    $stmt = $pdo->query("SELECT * FROM $table_name ORDER BY course_id DESC");
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'Prep Course');
}

// --- TOPICS MANAGEMENT (Foreign Key Lookup & Content Preview Fix) ---
function include_topics($pdo) {
    $table_name = 'topics';
    $primary_key = 'topic_id';
    $display_fields = [
        'topic_id' => 'ID',
        'course_name' => 'Prep Course',
        'topic_name' => 'Topic Name',
        'content' => 'Content Preview', // Added content preview
        'difficulty' => 'Difficulty'
    ];
    $form_fields = [
        'course_code' => 'select', 
        'topic_name' => 'text', 
        'content' => 'textarea', // Must be textarea for HTML input
        'difficulty' => 'select'
    ];

    $query = "
        SELECT t.*, p.course_name 
        FROM topics t 
        LEFT JOIN prepcourses p ON t.course_code = p.course_code 
        ORDER BY t.topic_id DESC
    ";
    $stmt = $pdo->query($query);
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Get prep courses for select box
    $prepcourses = $pdo->query("SELECT course_code, course_name FROM prepcourses ORDER BY course_name");
    $prepcourse_options = $prepcourses->fetchAll(PDO::FETCH_ASSOC);
    
    // Hardcoded options for difficulty
    $difficulty_options = ['easy', 'medium', 'hard'];

    // Combine foreign key data for generic function
    $fk_data = [
        'course_code' => ['options' => $prepcourse_options, 'key_field' => 'course_code', 'display_field' => 'course_name'],
        'difficulty' => ['options' => $difficulty_options, 'is_simple_array' => true]
    ];
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'Topic', $fk_data);
}

// --- MCQ QUESTIONS MANAGEMENT ---
function include_mcq_questions($pdo) {
    $table_name = 'mcq_combined';
    $primary_key = 'question_id';
    $display_fields = [
        'question_id' => 'ID', 'course_name' => 'Course', 'question_text' => 'Question', 
        'option_a' => 'A', 'option_b' => 'B', 'option_c' => 'C', 'option_d' => 'D', 
        'correct_option' => 'Correct'
    ];
    $form_fields = [
        'course_id' => 'select', 'question_text' => 'textarea', 'option_a' => 'textarea', 
        'option_b' => 'textarea', 'option_c' => 'textarea', 'option_d' => 'textarea', 
        'correct_option' => 'select'
    ];

    $query = "
        SELECT mcq.*, p.course_name 
        FROM mcq_combined mcq 
        LEFT JOIN prepcourses p ON mcq.course_id = p.course_code 
        ORDER BY mcq.question_id DESC
    ";
    $stmt = $pdo->query($query);
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $prepcourses = $pdo->query("SELECT course_code, course_name FROM prepcourses ORDER BY course_name");
    $prepcourse_options = $prepcourses->fetchAll(PDO::FETCH_ASSOC);
    $correct_options = ['A', 'B', 'C', 'D'];

    $fk_data = [
        'course_id' => ['options' => $prepcourse_options, 'key_field' => 'course_code', 'display_field' => 'course_name'],
        'correct_option' => ['options' => $correct_options, 'is_simple_array' => true]
    ];
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'MCQ Question', $fk_data);
}

// --- THEORY QUESTIONS MANAGEMENT ---
function include_theory_questions($pdo) {
    $table_name = 'theory_questions';
    $primary_key = 'question_id';
    $display_fields = [
        'question_id' => 'ID', 'course_id' => 'Course', 'question_text' => 'Question', 
        'keywords' => 'Keywords', 'complete_answer' => 'Answer', 'difficulty_level' => 'Difficulty', 
        'marks' => 'Marks'
    ];
    $form_fields = [
        'course_id' => 'select', 'question_text' => 'textarea', 'keywords' => 'textarea', 
        'complete_answer' => 'textarea', 'difficulty_level' => 'select', 'marks' => 'number'
    ];

    $query = "SELECT * FROM theory_questions ORDER BY question_id DESC";
    $stmt = $pdo->query($query);
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $prepcourses = $pdo->query("SELECT course_code, course_name FROM prepcourses ORDER BY course_name");
    $prepcourse_options = $prepcourses->fetchAll(PDO::FETCH_ASSOC);
    $difficulty_options = ['easy', 'medium', 'hard'];

    $fk_data = [
        'course_id' => ['options' => $prepcourse_options, 'key_field' => 'course_code', 'display_field' => 'course_name'],
        'difficulty_level' => ['options' => $difficulty_options, 'is_simple_array' => true]
    ];
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'Theory Question', $fk_data);
}

// --- MCQ TEST RESULTS MANAGEMENT (View & Delete Only) ---
function include_mcq_results($pdo) {
    $table_name = 'mcq_test_results';
    $primary_key = 'test_result_id';
    $display_fields = [
        'test_result_id' => 'Test ID', 
        'student_id' => 'Student Num',
        'student_name' => 'Student Name', 
        'course_code' => 'Course', 
        'score' => 'Score', 
        'total_questions' => 'Total Qs', 
        'time_taken' => 'Time (s)', 
        'test_date' => 'Date'
    ];
    $form_fields = []; 
    
    // FIX: Join on student_id (from mcq_test_results) = student_number (from students_new)
    $query = "
        SELECT 
            m.*, s.full_name as student_name
        FROM mcq_test_results m
        LEFT JOIN students_new s ON m.student_id = s.student_number 
        ORDER BY m.test_result_id DESC
    ";
    $stmt = $pdo->query($query);
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'MCQ Test Result');
}

// --- THEORY TEST RESULTS MANAGEMENT (View & Delete Only) ---
function include_theory_results($pdo) {
    $table_name = 'theory_test_results';
    $primary_key = 'test_result_id';
    $display_fields = [
        'test_result_id' => 'Test ID', 
        'student_id' => 'Student Num',
        'student_name' => 'Student Name', 
        'course_code' => 'Course', 
        'total_marks' => 'Total Marks', 
        'total_score' => 'Score', 
        'percentage' => 'Percent', 
        'test_date' => 'Date'
    ];
    $form_fields = []; 
    
    // FIX: Join on student_id (from theory_test_results) = student_number (from students_new)
    $query = "
        SELECT 
            t.*, s.full_name as student_name
        FROM theory_test_results t
        LEFT JOIN students_new s ON t.student_id = s.student_number
        ORDER BY t.test_result_id DESC
    ";
    $stmt = $pdo->query($query);
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'Theory Test Result');
}

// --- GRADES MANAGEMENT ---
function include_grades($pdo) {
    $table_name = 'students_grades';
    $primary_key = 'id';
    $display_fields = [
        'id' => 'ID', 
        'student_id' => 'Student ID (Num)', 
        'student_name' => 'Student Name',
        'course_name' => 'Course Name',
        'grade' => 'Grade', 
        'submitted_at' => 'Submitted At'
    ];
    
    // Form fields for adding/editing grades
    $form_fields = [
        'student_id' => 'text', // VARCHAR in DB
        'course_id' => 'number', // INT in DB
        'grade' => 'text'
    ];
    
    // Complex Query to join tables for readable display
    $query = "
        SELECT 
            sg.id, sg.student_id, sg.course_id, sg.grade, sg.submitted_at,
            s.full_name as student_name,
            c.name as course_name
        FROM students_grades sg
        LEFT JOIN students_new s ON sg.student_id = s.student_number
        LEFT JOIN courses_new c ON sg.course_id = c.id
        ORDER BY sg.id DESC
    ";
    $stmt = $pdo->query($query);
    $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, 'Student Grade');
}


// =========================================================================
// GENERIC CRUD FUNCTION (Used for multiple simple tables)
// =========================================================================

function include_generic_crud($table_name, $primary_key, $display_fields, $form_fields, $records, $pdo, $label, $fk_data = []) {
    $add_modal_id = 'add' . str_replace(' ', '', $label) . 'Modal';
    $edit_modal_id = 'edit' . str_replace(' ', '', $label) . 'Modal';
    $edit_form_id = 'edit' . str_replace(' ', '', $label) . 'Form';
    ?>
    <h2><?php echo $label; ?> Management (<?php echo $table_name; ?>)</h2>
    
    <?php if (!empty($form_fields)): ?>
        <button onclick="openModal('<?php echo $add_modal_id; ?>')" class="btn">Add New <?php echo $label; ?></button>
    <?php endif; ?>
    
    <div class="table-container">
        <table>
            <thead>
                <tr>
                    <?php foreach ($display_fields as $key => $disp_label): ?>
                        <th><?php echo $disp_label; ?></th>
                    <?php endforeach; ?>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($records as $row): ?>
                <tr data-record='<?php echo json_encode($row); ?>'>
                    <?php foreach ($display_fields as $key => $disp_label): ?>
                        <td>
                            <?php 
                            $value = $row[$key] ?? 'NULL';
                            
                            // Special handling for HTML content: Remove tags and truncate for Topics content preview
                            if ($key === 'content' && $table_name === 'topics') {
                                // FIX: Ensure line breaks and raw entities are handled for a clean preview
                                $text_content = html_entity_decode($value);
                                $text_content = strip_tags($text_content);
                                $text_content = str_replace(['\r', '\n', '\t'], ' ', $text_content); // Remove newline chars from dump artifact
                                $text_content = preg_replace('/\s+/', ' ', $text_content); // Remove excessive whitespace
                                $content_preview = substr($text_content, 0, 150);
                                echo htmlspecialchars($content_preview) . (strlen($text_content) > 150 ? '...' : '');
                            } else {
                                echo htmlspecialchars($value);
                            }
                            ?>
                        </td>
                    <?php endforeach; ?>
                    <td class="action-buttons">
                        <?php if (!empty($form_fields)): ?>
                            <button onclick="populateEditForm(<?php echo htmlspecialchars(json_encode($row)); ?>, '<?php echo $edit_form_id; ?>')" class="btn">Edit</button>
                        <?php endif; ?>
                        <form method="POST" style="display: inline;" onsubmit="return confirmDelete(this);">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="table" value="<?php echo $table_name; ?>">
                            <input type="hidden" name="id_field" value="<?php echo $primary_key; ?>">
                            <input type="hidden" name="id" value="<?php echo $row[$primary_key]; ?>">
                            <button type="submit" class="btn btn-danger">Delete</button>
                        </form>
                    </td>
                </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>

    <!-- Add Modal -->
    <?php if (!empty($form_fields)): ?>
    <div id="<?php echo $add_modal_id; ?>" class="modal">
        <div class="modal-content">
            <span class="close" onclick="closeModal('<?php echo $add_modal_id; ?>')">&times;</span>
            <h3>Add New <?php echo $label; ?></h3>
            <form method="POST">
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="table" value="<?php echo $table_name; ?>">
                <?php foreach ($form_fields as $field => $type): 
                    $disp_label = $display_fields[$field] ?? ucfirst(str_replace('_', ' ', $field));
                    ?>
                    <div class="form-group">
                        <label><?php echo $disp_label; ?>:</label>
                        <?php if (isset($fk_data[$field])): 
                            $fk = $fk_data[$field];
                            $key_field = $fk['key_field'] ?? 'id';
                            $display_field = $fk['display_field'] ?? 'name';
                            ?>
                            <select name="<?php echo $field; ?>" required>
                                <option value="">Select <?php echo $disp_label; ?></option>
                                <?php if (isset($fk['is_simple_array'])): ?>
                                    <?php foreach ($fk['options'] as $opt): ?>
                                        <option value="<?php echo htmlspecialchars($opt); ?>"><?php echo htmlspecialchars($opt); ?></option>
                                    <?php endforeach; ?>
                                <?php else: ?>
                                    <?php foreach ($fk['options'] as $opt): ?>
                                        <option value="<?php echo $opt[$key_field]; ?>"><?php echo htmlspecialchars($opt[$display_field] ?? $opt[$key_field]); ?></option>
                                    <?php endforeach; ?>
                                <?php endif; ?>
                            </select>
                        <?php elseif ($type === 'textarea'): ?>
                            <textarea name="<?php echo $field; ?>" required></textarea>
                        <?php else: ?>
                            <input type="<?php echo $type; ?>" name="<?php echo $field; ?>" required>
                        <?php endif; ?>
                    </div>
                <?php endforeach; ?>
                <button type="submit" class="btn">Add <?php echo $label; ?></button>
            </form>
        </div>
    </div>
    <?php endif; ?>

    <!-- Edit Modal -->
    <?php if (!empty($form_fields)): ?>
    <div id="<?php echo $edit_modal_id; ?>" class="modal">
        <div class="modal-content">
            <span class="close" onclick="closeModal('<?php echo $edit_modal_id; ?>')">&times;</span>
            <h3>Edit <?php echo $label; ?></h3>
            <form id="<?php echo $edit_form_id; ?>" method="POST">
                <input type="hidden" name="action" value="update">
                <input type="hidden" name="table" value="<?php echo $table_name; ?>">
                <input type="hidden" name="id_field" value="<?php echo $primary_key; ?>">
                <input type="hidden" name="id" value=""> <!-- Hidden ID field for primary key -->
                
                <?php foreach ($form_fields as $field => $type): 
                    $disp_label = $display_fields[$field] ?? ucfirst(str_replace('_', ' ', $field));
                    // Skip 'id' field in the list of editable fields
                    if ($field === $primary_key) continue; 
                    ?>
                    <div class="form-group">
                        <label><?php echo $disp_label; ?>:</label>
                        <?php if (isset($fk_data[$field])): 
                            $fk = $fk_data[$field];
                            $key_field = $fk['key_field'] ?? 'id';
                            $display_field = $fk['display_field'] ?? 'name';
                            ?>
                            <select name="<?php echo $field; ?>" required>
                                <option value="">Select <?php echo $disp_label; ?></option>
                                <?php if (isset($fk['is_simple_array'])): ?>
                                    <?php foreach ($fk['options'] as $opt): ?>
                                        <option value="<?php echo htmlspecialchars($opt); ?>"><?php echo htmlspecialchars($opt); ?></option>
                                    <?php endforeach; ?>
                                <?php else: ?>
                                    <?php foreach ($fk['options'] as $opt): ?>
                                        <option value="<?php echo $opt[$key_field]; ?>"><?php echo htmlspecialchars($opt[$display_field] ?? $opt[$key_field]); ?></option>
                                    <?php endforeach; ?>
                                <?php endif; ?>
                            </select>
                        <?php elseif ($type === 'textarea'): ?>
                            <textarea name="<?php echo $field; ?>" required></textarea>
                        <?php else: ?>
                            <input type="<?php echo $type; ?>" name="<?php echo $field; ?>" <?php echo ($type === 'password' ? '' : 'required'); ?>>
                            <?php if ($type === 'password'): ?><small>Leave blank to keep existing password.</small><?php endif; ?>
                        <?php endif; ?>
                    </div>
                <?php endforeach; ?>
                <button type="submit" class="btn">Update <?php echo $label; ?></button>
            </form>
        </div>
    </div>
    <?php endif; ?>
    <?php
}  
?>
