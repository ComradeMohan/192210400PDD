<?php
include('db.php');

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $college_id = $_POST['college_id'];
    $name = $_POST['name'];

    $sql = "INSERT INTO departments_new (college_id, name) VALUES ('$college_id', '$name')";

    if ($conn->query($sql) === TRUE) {
        echo "New department added successfully";
    } else {
        echo "Error: " . $sql . "<br>" . $conn->error;
    }

    $conn->close();
}
?>
