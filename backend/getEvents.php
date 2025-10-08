<?php
header('Content-Type: application/json');
include 'db.php';

$response = [];

// Check if the college_name parameter is set
if (isset($_GET['college_name'])) {
    $college_name = mysqli_real_escape_string($conn, $_GET['college_name']);

    // SQL query to fetch events based on college name
    $sql = "SELECT * FROM events_new WHERE college_name = '$college_name'";
    $result = mysqli_query($conn, $sql);

    if ($result) {
        $events = [];
        $currentDate = date('Y-m-d'); // Get today's date

        // Loop through the result set and filter out events with an end date earlier than today
        while ($row = mysqli_fetch_assoc($result)) {
            $eventEndDate = $row['end_date'];

            // Check if the event end date is after today's date
            if ($eventEndDate >= $currentDate) {
                $events[] = $row;
            }
        }

        // Check if any events are found
        if (count($events) > 0) {
            $response['status'] = 'success';
            $response['message'] = count($events) . ' event(s) found';
            $response['data'] = $events;
        } else {
            $response['status'] = 'success';
            $response['message'] = 'No future events found';
            $response['data'] = [];
        }
    } else {
        $response['status'] = 'error';
        $response['message'] = 'Query failed: ' . mysqli_error($conn);
        $response['data'] = [];
    }
} else {
    $response['status'] = 'error';
    $response['message'] = 'Missing parameter: college_name';
    $response['data'] = [];
}

// Return the JSON response
echo json_encode($response);
mysqli_close($conn);
?>
