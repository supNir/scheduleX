<?php
$response = array();

if (isset($_POST['day_of_week']) && isset($_POST['start_time']) && isset($_POST['end_time']) && isset($_POST['subject']) && isset($_POST['room'])) {
    require 'db_connect.php';

    $db = new DB_CONNECT();
    $con = $db->con;
    $con->set_charset('utf8');
    $day_of_week = $_POST['day_of_week'];
    $start_time = $_POST['start_time'];
    $end_time = $_POST['end_time'];
    $subject = $_POST['subject'];
    $room = $_POST['room'];

    $result = $con->query("INSERT INTO schedule (day_of_week, start_time, end_time, subject, room) VALUES ('$day_of_week', '$start_time', '$end_time', '$subject', '$room')");

    if ($result) {
        $response["success"] = 1;
        $response["message"] = "Schedule entry successfully added.";
    } else {
        $response["success"] = 0;
        $response["message"] = "Failed to add schedule.";
    }
} else {
    $response["success"] = 0;
    $response["message"] = "Required field(s) missing.";
}

echo json_encode($response);
?>