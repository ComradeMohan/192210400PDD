<?php
include('db.php');

if ($_SERVER["REQUEST_METHOD"] == "GET") {
    if (isset($_GET['department_id'])) {
        $department_id = $_GET['department_id'];

        // Query to fetch courses by department_id
        $sql = "SELECT * FROM courses_new WHERE department_id = '$department_id'";
        $result = $conn->query($sql);

        if ($result->num_rows > 0) {
            // Create an array to store courses
            $courses = array();
            
            while ($row = $result->fetch_assoc()) {
                $courses[] = $row;
            }

            // Send courses as a JSON response
            echo json_encode($courses);
        } else {
            echo json_encode(["message" => "No courses found for this department"]);
        }
    } else {
        echo json_encode(["message" => "Department ID not provided"]);
    }

    $conn->close();
}
?>
