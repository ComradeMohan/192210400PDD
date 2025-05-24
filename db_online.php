<?php
$host = "sql12.freesqldatabase.com";     // remote host
$user = "sql12779691";                   // your DB user
$pass = "dQLvpAjifX";                         // your DB password
$db   = "sql12779691";                   // your DB name
$port = 3306;                           // port number (optional, default is 3306)

$conn = new mysqli($host, $user, $pass, $db, $port);
if ($conn->connect_error) {
    die(json_encode(["success" => false, "message" => "Database connection failed."]));
}
?>
