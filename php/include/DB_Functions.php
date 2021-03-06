<?php
/**
 * @package GoodHikes server-side
 * @author Chelsea
 * @copyright (C) 2016 - Team Magic
 * @license GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html
**/

class DB_Functions {

	private $conn;

	// constructor
    	function __construct() {
     
                require_once 'config.php';
		$this->conn = new mysqli(DB_HOST, DB_USER, DB_PASSWORD, DB_DATABASE);      
    }
 
    // destructor
    function __destruct() {}
 
    /**
     * Storing new user
     * returns user details
     */
    public function storeUser($name, $email, $password) {
        $uuid = uniqid('', true);
        $hash = $this->hashSSHA($password);
        $encrypted_password = $hash["encrypted"]; // encrypted password
        $salt = $hash["salt"]; // salt
 
        $stmt = $this->conn->prepare("INSERT INTO users(unique_id, name, email, encrypted_password, salt, created_at) VALUES(?, ?, ?, ?, ?, NOW())");
        $stmt->bind_param("sssss", $uuid, $name, $email, $encrypted_password, $salt);
        $result = $stmt->execute();
        $stmt->close();
 
        // check for successful store
        if ($result) {
            /*$stmt = $this->conn->prepare("SELECT * FROM users WHERE email = ?");
            $stmt->bind_param("s", $email);
            $stmt->execute();
            $user = $stmt->get_result()->fetch_assoc();
            $stmt->close(); 
            */
            $user = array ("unique_id" => $uuid, "name" => $name, "email" => $email, "password" => $encrypted_password, "salt" => $salt);
            return $user;        
	} else {
            return false;
	}
        
    }
 
    /** Verify user by email and password **/
    public function getUserByEmailAndPassword($email, $password) { 
        $stmt = $this->conn->prepare("SELECT * FROM users WHERE email = ?"); 
        $stmt->bind_param("s", $email);
 
        $user = array();
        if ($stmt->execute()) {
            $stmt->bind_result($user["unique_id"], $user["name"], $user["email"], $user["password"], $user["salt"], $user["created_at"], $user["image_str"]);
            
            while($stmt->fetch()) {
               
            // verifying user password
            $salt = $user['salt'];
            $encrypted_password = $user['password'];
            $hash = $this->checkhashSSHA($salt, $password);
                // check for password equality
                if ($encrypted_password == $hash) {
                    // user authentication details are correct
                    return $user;
                }
            }
            if ($stmt->fetch == NULL) {  return NULL;  }
           $stmt->close();
        } else {
            return NULL;
        }
    }
 
    /**
     * Check user is existed or not
     */
    public function isUserExisted($email) {
        $stmt = $this->conn->prepare("SELECT email from users WHERE email = ?"); 
        $stmt->bind_param("s", $email); 
        $stmt->execute(); 
        $stmt->store_result();
 
        if ($stmt->num_rows > 0) {
            // user existed 
            $stmt->close();
            return true;
        } else {
            // user not existed
            $stmt->close();
            return false;
        }
    }
 

    /**
     * Encrypting password
     * returns salt and encrypted password
     */
    public function hashSSHA($password) {
        $salt = sha1(rand());
        $salt = substr($salt, 0, 10);
        $encrypted = base64_encode(sha1($password . $salt, true) . $salt);
        $hash = array("salt" => $salt, "encrypted" => $encrypted);
        return $hash;
    }
 

    /**
     * Decrypting password
     * returns hash string
     */
    public function checkhashSSHA($salt, $password) { 
        $hash = base64_encode(sha1($password . $salt, true) . $salt);
        return $hash;
    }


    /** upload image string **/
    public function storeImage($uid, $image_str) {
        $stmt = $this->conn->prepare("UPDATE users SET image_str = ? WHERE unique_id = ?");
        $stmt->bind_param("ss", $image_str, $uid);
        $result = $stmt->execute();
        $stmt->close();
 
        // check for successful store
        if ($result) {
            return true;
	} else {
            return false;
	}
        
    }
 
}
 


?>
			