<?php
class DB_CONNECT {
    public $con;
    function __construct() {
        $this->con = $this->connect();
    }

    function __destruct() {
        $this->close();
    }

    function connect() {
        require 'db_config.php';
        $con = mysqli_connect(DB_SERVER, DB_USER, DB_PASSWORD) or die(mysqli_error($con));
        $db = mysqli_select_db($con, DB_DATABASE) or die(mysqli_error($con));
        return $con;
    }

    function close() {
        mysqli_close($this->con);
    }
}
?>
