<?php
$response = array();

if (isset($_POST['schedule_id'])) {
    require 'db_connect.php';

    $db = new DB_CONNECT();
    $con = $db->con;
    $con->set_charset('utf8');

    $schedule_id = $_POST['schedule_id'];

    $result = $con->query("DELETE FROM schedule WHERE id = '$schedule_id'");

    if ($result) {
        if ($con->affected_rows > 0) {
            $response["success"] = 1;
            $response["message"] = "Schedule entry successfully deleted.";
        } else {
            $response["success"] = 0;
            $response["message"] = "No entry found with the provided ID.";
        }
    } else {
        $response["success"] = 0;
        $response["message"] = "Failed to delete schedule.";
    }
} else {
    $response["success"] = 0;
    $response["message"] = "Required field(s) missing.";
}

echo json_encode($response);
?>