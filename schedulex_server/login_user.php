<?php
$response = array();

if (isset($_POST['username']) && isset($_POST['password'])) {
    require 'db_connect.php';

    $db = new DB_CONNECT();
    $con = $db->con;

    $username = $_POST['username'];
    $password = $_POST['password'];

    $result = $con->query("SELECT * FROM users WHERE username = '$username'");

    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        if (password_verify($password, $row['password'])) {
            $response["success"] = 1;
            $response["message"] = "Login successful.";
            $response["user"] = array(
                "id" => $row["id"],
                "username" => $row["username"],
                "role" => $row["role"]
            );
        } else {
            $response["success"] = 0;
            $response["message"] = "Invalid password.";
        }
    } else {
        $response["success"] = 0;
        $response["message"] = "User not found.";
    }
} else {
    $response["success"] = 0;
    $response["message"] = "Required fields are missing.";
}

echo json_encode($response);
?>