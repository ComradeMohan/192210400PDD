<?php
include('db.php');

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $department_id = $_POST['department_id'];
    $name = $_POST['name'];
    $credits = $_POST['credits'];

    $sql = "INSERT INTO courses_new (department_id, name, credits) VALUES ('$department_id', '$name', '$credits')";

    if ($conn->query($sql) === TRUE) {
        echo "New course added successfully";
    } else {
        echo "Error: " . $sql . "<br>" . $conn->error;
    }

    $conn->close();
}
?>