<?php
// =========================================================================
// 1. CONFIGURATION & DATABASE CONNECTION
// =========================================================================

// Database credentials - *** CHANGE THESE TO YOUR ACTUAL CREDENTIALS ***
$servername = "127.0.0.1"; // Your host, typically localhost or 127.0.0.1
$username = "root";        // Your MySQL username
$password = "";            // Your MySQL password
$dbname = "univault_db2";   // Your database name from the SQL dump

// Target table for this specific CRUD example
$table_name = 'students_new';

// Fields to display and manage (must match the table columns exactly)
$fields = [
    'id' => 'ID',
    'full_name' => 'Full Name',
    'student_number' => 'Student ID',
    'email' => 'Email',
    'department' => 'Department',
    'year_of_study' => 'Year of Study',
    'college' => 'College',
    // 'password' is omitted for security but must be handled on insert/update
];

// Fields that will be displayed as input in the 'Add'/'Edit' form
$form_fields = [
    'full_name' => ['type' => 'text', 'required' => true],
    'student_number' => ['type' => 'text', 'required' => true],
    'email' => ['type' => 'email', 'required' => true],
    'password' => ['type' => 'password', 'required' => false], // Optional: only needed for a new user/password reset
    'department' => ['type' => 'text', 'required' => true],
    'year_of_study' => ['type' => 'text', 'required' => true],
    'college' => ['type' => 'text', 'required' => true],
];

// Start PHP session
session_start();
$message = '';

// Attempt to connect to MySQL database
try {
    $conn = new mysqli($servername, $username, $password, $dbname);
    if ($conn->connect_error) {
        throw new Exception("Connection failed: " . $conn->connect_error);
    }
} catch (Exception $e) {
    die("Database Error: " . $e->getMessage());
}

// =========================================================================
// 2. AUTHENTICATION LOGIC (Login/Logout)
// =========================================================================

function check_login($conn) {
    if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true) {
        return false;
    }
    // Simple check: 'admin001' from the admins table is our logged-in user
    return isset($_SESSION['admin_id']) && $_SESSION['admin_id'] === 'admin001';
}

if (isset($_POST['login'])) {
    $admin_id = $_POST['admin_id'] ?? '';
    $password = $_POST['password'] ?? '';
    
    // Hardcoded check for the demo account 'admin001' with password 'admin'
    // NOTE: In a real app, you would verify against the database.
    if ($admin_id === 'admin001' && $password === 'admin') {
        $_SESSION['admin_logged_in'] = true;
        $_SESSION['admin_id'] = $admin_id;
        $message = "Login successful!";
    } else {
        $message = "Invalid Admin ID or Password.";
    }
}

if (isset($_GET['logout'])) {
    session_destroy();
    header("Location: admin_panel.php");
    exit();
}

$is_logged_in = check_login($conn);

// =========================================================================
// 3. CRUD LOGIC (Create, Read, Update, Delete)
// =========================================================================

if ($is_logged_in) {
    $action = $_GET['action'] ?? 'view';
    $edit_id = $_GET['edit'] ?? null;
    $delete_id = $_GET['delete'] ?? null;

    // --- CREATE/UPDATE (SAVE) ---
    if (isset($_POST['save_data'])) {
        $data = [];
        $id = $_POST['id'] ?? null;
        $is_new = empty($id);
        
        // Sanitize and validate input data
        foreach ($form_fields as $field => $config) {
            if ($field === 'password') {
                // Only set password if provided, and hash it
                if (!empty($_POST[$field])) {
                    $data[$field] = password_hash($_POST[$field], PASSWORD_DEFAULT);
                }
            } else {
                $data[$field] = $conn->real_escape_string($_POST[$field] ?? '');
            }
        }

        // Prepare the SQL statement
        if ($is_new) {
            // INSERT
            $cols = implode(", ", array_keys($data));
            $placeholders = implode(", ", array_fill(0, count($data), '?'));
            $sql = "INSERT INTO $table_name ($cols) VALUES ($placeholders)";
            
            $stmt = $conn->prepare($sql);
            if ($stmt) {
                // Determine types for bind_param dynamically
                $types = '';
                foreach ($data as $key => $value) {
                    $types .= ($key === 'id') ? 'i' : 's';
                }
                
                $stmt->bind_param($types, ...array_values($data));
                
                if ($stmt->execute()) {
                    $message = "New record created successfully!";
                } else {
                    $message = "Error creating record: " . $stmt->error;
                }
                $stmt->close();
            } else {
                $message = "Error preparing statement: " . $conn->error;
            }
        } else {
            // UPDATE
            $set_clauses = [];
            foreach (array_keys($data) as $field) {
                if ($field !== 'id') { // Don't update the ID column in the SET
                    $set_clauses[] = "$field = ?";
                }
            }
            $sql = "UPDATE $table_name SET " . implode(", ", $set_clauses) . " WHERE id = ?";
            
            $stmt = $conn->prepare($sql);
            if ($stmt) {
                $update_data = array_values($data);
                $update_data[] = $id; // Add the ID for the WHERE clause
                
                // Determine types for bind_param dynamically
                $types = '';
                foreach ($data as $key => $value) {
                    $types .= ($key === 'id') ? 'i' : 's';
                }
                $types .= 'i'; // For the final WHERE ID
                
                $stmt->bind_param($types, ...$update_data);
                
                if ($stmt->execute()) {
                    $message = "Record updated successfully!";
                } else {
                    $message = "Error updating record: " . $stmt->error;
                }
                $stmt->close();
            } else {
                $message = "Error preparing statement: " . $conn->error;
            }
        }
        $action = 'view'; // Return to view after save
    }

    // --- DELETE ---
    if ($delete_id) {
        $sql = "DELETE FROM $table_name WHERE id = ?";
        $stmt = $conn->prepare($sql);
        
        if ($stmt) {
            $stmt->bind_param("i", $delete_id);
            if ($stmt->execute()) {
                $message = "Record deleted successfully!";
            } else {
                $message = "Error deleting record: " . $stmt->error;
            }
            $stmt->close();
        } else {
            $message = "Error preparing statement: " . $conn->error;
        }
        $action = 'view';
    }

    // --- READ/VIEW (Fetch all records) ---
    if ($action === 'view') {
        $sql = "SELECT id, " . implode(", ", array_keys($fields)) . " FROM $table_name ORDER BY id DESC";
        $result = $conn->query($sql);
        $records = $result->fetch_all(MYSQLI_ASSOC);
    }

    // --- EDIT (Fetch single record for form) ---
    $edit_record = [];
    if ($action === 'edit' && $edit_id) {
        $sql = "SELECT * FROM $table_name WHERE id = ?";
        $stmt = $conn->prepare($sql);
        if ($stmt) {
            $stmt->bind_param("i", $edit_id);
            $stmt->execute();
            $result = $stmt->get_result();
            $edit_record = $result->fetch_assoc();
            $stmt->close();
        } else {
            $message = "Error preparing statement for edit: " . $conn->error;
        }
    }
}

// =========================================================================
// 4. HTML / CSS UI TEMPLATE
// =========================================================================
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Univault Admin Panel - <?php echo ucfirst($table_name); ?></title>
    <style>
        :root {
            --primary: #1e88e5; /* Blue */
            --primary-dark: #1565c0;
            --secondary: #ff9800; /* Amber */
            --success: #4caf50;
            --danger: #f44336;
            --bg-light: #f5f5f5;
            --text-dark: #333;
            --border-color: #ddd;
        }
        body {
            font-family: 'Arial', sans-serif;
            background-color: var(--bg-light);
            color: var(--text-dark);
            margin: 0;
            padding: 0;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .container {
            width: 95%;
            max-width: 1200px;
            margin: 20px auto;
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
        }
        h1 {
            color: var(--primary-dark);
            border-bottom: 2px solid var(--border-color);
            padding-bottom: 10px;
            margin-bottom: 20px;
            text-align: center;
        }
        .message {
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 5px;
            font-weight: bold;
            text-align: center;
            background-color: #e3f2fd;
            color: var(--primary-dark);
            border: 1px solid var(--primary);
        }
        .alert-success {
            background-color: #e8f5e9;
            color: var(--success);
            border-color: var(--success);
        }
        .alert-danger {
            background-color: #ffebee;
            color: var(--danger);
            border-color: var(--danger);
        }

        /* --- Buttons and Links --- */
        .btn {
            padding: 8px 15px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            text-decoration: none;
            font-weight: bold;
            display: inline-block;
            transition: background-color 0.3s;
            margin: 5px;
            font-size: 0.9em;
        }
        .btn-primary { background-color: var(--primary); color: white; }
        .btn-primary:hover { background-color: var(--primary-dark); }
        .btn-add { background-color: var(--success); color: white; }
        .btn-add:hover { background-color: #388e3c; }
        .btn-edit { background-color: var(--secondary); color: white; }
        .btn-edit:hover { background-color: #fb8c00; }
        .btn-delete { background-color: var(--danger); color: white; }
        .btn-delete:hover { background-color: #d32f2f; }
        .btn-logout { background-color: #757575; color: white; }
        .btn-logout:hover { background-color: #424242; }

        /* --- Table Styling --- */
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            border: 1px solid var(--border-color);
            padding: 10px 8px;
            text-align: left;
            font-size: 0.85em;
        }
        th {
            background-color: var(--primary);
            color: white;
            font-weight: 600;
        }
        tr:nth-child(even) {
            background-color: #f9f9f9;
        }

        /* --- Form Styling --- */
        .form-container {
            padding: 20px;
            border: 1px solid var(--border-color);
            border-radius: 8px;
            margin-top: 20px;
            background: #fcfcfc;
        }
        .form-group {
            margin-bottom: 15px;
        }
        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            color: var(--primary-dark);
        }
        .form-group input[type="text"],
        .form-group input[type="email"],
        .form-group input[type="password"] {
            width: 100%;
            padding: 10px;
            border: 1px solid var(--border-color);
            border-radius: 4px;
            box-sizing: border-box;
        }
    </style>
</head>
<body>

<div class="container">
    <h1>Univault Database Administration</h1>
    <?php if ($message): ?>
        <div class="message <?php echo strpos($message, 'successful') !== false ? 'alert-success' : 'alert-danger'; ?>">
            <?php echo $message; ?>
        </div>
    <?php endif; ?>

    <?php if (!$is_logged_in): ?>
        <h2>Admin Login</h2>
        <div class="form-container">
            <p><strong>Demo Login:</strong> Admin ID: <code>admin001</code> | Password: <code>admin</code></p>
            <form method="post" action="admin_panel.php">
                <div class="form-group">
                    <label for="admin_id">Admin ID (e.g., admin001)</label>
                    <input type="text" id="admin_id" name="admin_id" required>
                </div>
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" required>
                </div>
                <button type="submit" name="login" class="btn btn-primary">Login</button>
            </form>
        </div>
    <?php else: ?>
        <h2>Manage Students (<code style="color:var(--primary);"><?php echo $table_name; ?></code>)</h2>
        <p>Logged in as: <strong><?php echo $_SESSION['admin_id']; ?></strong> 
        <a href="?logout=true" class="btn btn-logout">Logout</a></p>

        <?php if ($action === 'view'): ?>
            <a href="?action=add" class="btn btn-add">Add New Student</a>
            
            <?php if (!empty($records)): ?>
                <table>
                    <thead>
                        <tr>
                            <?php foreach ($fields as $col => $label): ?>
                                <th><?php echo $label; ?></th>
                            <?php endforeach; ?>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php foreach ($records as $record): ?>
                            <tr>
                                <?php foreach ($fields as $col => $label): ?>
                                    <td><?php echo htmlspecialchars($record[$col] ?? 'N/A'); ?></td>
                                <?php endforeach; ?>
                                <td>
                                    <a href="?action=edit&edit=<?php echo $record['id']; ?>" class="btn btn-edit">Edit</a>
                                    <a href="?action=delete&delete=<?php echo $record['id']; ?>" class="btn btn-delete" onclick="return confirm('Are you sure you want to delete this record (ID: <?php echo $record['id']; ?>)?');">Delete</a>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            <?php else: ?>
                <p style="margin-top: 20px;">No records found in the <?php echo $table_name; ?> table.</p>
            <?php endif; ?>

        <?php elseif ($action === 'add' || $action === 'edit'): ?>
            <h2><?php echo ($action === 'add' ? 'Add New' : 'Edit'); ?> Student Record (ID: <?php echo $edit_id ?? 'New'; ?>)</h2>
            <div class="form-container">
                <form method="post" action="admin_panel.php">
                    <input type="hidden" name="id" value="<?php echo $edit_record['id'] ?? ''; ?>">
                    
                    <?php foreach ($form_fields as $field => $config): 
                        // Set the value for editing or a default for adding
                        $value = htmlspecialchars($edit_record[$field] ?? '');
                        $label = $fields[$field] ?? ucfirst(str_replace('_', ' ', $field));
                        $required = $config['required'] ? 'required' : '';
                        $placeholder = ($field === 'password') ? ($action === 'edit' ? 'Leave blank to keep current password' : 'Enter new password') : '';
                        
                        if ($field === 'password' && $action === 'edit' && $config['required']) {
                             $required = ''; // Don't strictly require password on edit unless resetting
                        }
                    ?>
                        <div class="form-group">
                            <label for="<?php echo $field; ?>"><?php echo $label; ?> <?php echo $required ? '<span style="color:var(--danger);">*</span>' : ''; ?></label>
                            <input 
                                type="<?php echo $config['type']; ?>" 
                                id="<?php echo $field; ?>" 
                                name="<?php echo $field; ?>" 
                                value="<?php echo $value; ?>"
                                placeholder="<?php echo $placeholder; ?>"
                                <?php echo $required; ?>
                            >
                        </div>
                    <?php endforeach; ?>

                    <button type="submit" name="save_data" class="btn btn-add"><?php echo ($action === 'add' ? 'Create Record' : 'Save Changes'); ?></button>
                    <a href="admin_panel.php" class="btn btn-primary">Cancel</a>
                </form>
            </div>
        <?php endif; ?>
    <?php endif; ?>
</div>

</body>
</html>