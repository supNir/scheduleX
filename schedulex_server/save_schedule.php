<?php
header('Content-Type: application/json');

// Подключение к базе данных через db_connect.php
require 'db_connect.php';

$db = new DB_CONNECT();
$con = $db->con;
$con->set_charset('utf8');

// Получение JSON данных из запроса
$data = json_decode(file_get_contents('php://input'), true);

if (!$data) {
    echo json_encode(["success" => false, "message" => "Неверный формат данных"]);
    exit;
}

$response = [];

foreach ($data as $change) {
    $action = $change['action'];
    $day = $con->real_escape_string($change['day']);
    $start_time = $con->real_escape_string($change['start_time']);
    $end_time = $con->real_escape_string($change['end_time']);
    $subject = $con->real_escape_string($change['subject']);
    $room = $con->real_escape_string($change['room']);

    if ($action === 'add') {
        $sql = "INSERT INTO schedule (day_of_week, start_time, end_time, subject, room) VALUES ('$day', '$start_time', '$end_time', '$subject', '$room')";
    } elseif ($action === 'edit') {
        $id = intval($change['id']);
        $sql = "UPDATE schedule SET day_of_week='$day', start_time='$start_time', end_time='$end_time', subject='$subject', room='$room' WHERE id=$id";
    } elseif ($action === 'delete') {
        $id = intval($change['id']);
        $sql = "DELETE FROM schedule WHERE id=$id";
    } else {
        $response[] = ["success" => false, "message" => "Неизвестное действие", "data" => $change];
        continue;
    }

    if ($con->query($sql) === TRUE) {
        $response[] = ["success" => true, "action" => $action, "data" => $change];
    } else {
        $response[] = ["success" => false, "message" => $con->error, "data" => $change];
    }
}

$con->close();

// Возвращаем результат
echo json_encode($response);
