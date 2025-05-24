<?php
$college = $_GET['college'];
$course = $_GET['course'];

$dir = "uploads/$college/$course/";
$response = [];

if (is_dir($dir)) {
    $files = array_diff(scandir($dir), ['.', '..']);
    foreach ($files as $file) {
        $filePath = $dir . $file;
        $response[] = [
            "name" => $file,
            "url" => "http://192.168.224.54/UniValut/uploads/$college/$course/$file"
,
            "date" => date("m/d/Y", filemtime($filePath))
        ];
    }
    echo json_encode(["success" => true, "files" => $response]);
} else {
    echo json_encode(["success" => false, "message" => "Directory not found"]);
}
?>
