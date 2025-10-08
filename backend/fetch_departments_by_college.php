<?php
include('db.php');

if ($_SERVER["REQUEST_METHOD"] == "GET") {
    if (isset($_GET['college_id'])) {
        $college_id = $_GET['college_id'];

        // Query to fetch departments by college_id
        $sql = "SELECT * FROM departments_new WHERE college_id = '$college_id'";
        $result = $conn->query($sql);

        if ($result->num_rows > 0) {
            // Create an array to store departments
            $departments = array();
            
            while ($row = $result->fetch_assoc()) {
                $departments[] = $row;
            }

            // Send departments as a JSON response
            echo json_encode($departments);
        } else {
            echo json_encode(["message" => "No departments found for this college"]);
        }
    } else {
        echo json_encode(["message" => "College ID not provided"]);
    }

    $conn->close();
}
?>
