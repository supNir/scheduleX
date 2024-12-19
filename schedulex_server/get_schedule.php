<?php
$response = array();

if (isset($_GET['day_of_week'])) {
    require 'db_connect.php';

    $db = new DB_CONNECT();
    $con = $db->con;
    $con->set_charset('utf8');
    $day_of_week = $con->real_escape_string($_GET['day_of_week']); // Защита от SQL-инъекций

    $result = $con->query("SELECT * FROM schedule WHERE day_of_week = '$day_of_week' ORDER BY start_time ASC");

    $response["schedule"] = array();

    while ($row = $result->fetch_assoc()) {
        $entry = array();
        $entry["id"] = $row["id"];
        $entry["start_time"] = $row["start_time"];
        $entry["end_time"] = $row["end_time"];
        $entry["subject"] = $row["subject"];
        $entry["room"] = $row["room"];
        array_push($response["schedule"], $entry);
    }

    $response["success"] = 1;
} else {
    $response["success"] = 0;
    $response["message"] = "Required field(s) missing.";
}

echo json_encode($response);
?>