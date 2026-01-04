<?php
/**
 * Authentication Endpoint
 * Energy20 - Multi-User Authentication
 * 
 * Handles user login via Google OAuth 2.0
 * Creates new users or updates existing users
 * Returns user info and their associated devices
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Only allow POST requests
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed. Use POST.']);
    exit;
}

// Include required files
require_once __DIR__ . '/../db_config.php';
require_once __DIR__ . '/../config/google_auth.php';


// Get request body
$input = json_decode(file_get_contents('php://input'), true);

if (!$input) {
    http_response_code(400);
    echo json_encode(['error' => 'Invalid JSON in request body']);
    exit;
}

// Extract ID token
$idToken = $input['idToken'] ?? null;

if (!$idToken) {
    http_response_code(400);
    echo json_encode(['error' => 'Missing idToken in request']);
    exit;
}

// Verify Google token
$userData = verifyGoogleToken($idToken);

if (!$userData) {
    http_response_code(401);
    echo json_encode(['error' => 'Invalid or expired Google token']);
    exit;
}

// Connect to database
$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    error_log("Database connection failed: " . $conn->connect_error);
    http_response_code(500);
    echo json_encode(['error' => 'Database connection failed']);
    exit;
}

try {
    // Check if user exists
    $stmt = $conn->prepare("SELECT id, email, name, profile_picture_url FROM users WHERE google_id = ?");
    if (!$stmt) {
        throw new Exception("Prepare failed: " . $conn->error);
    }
    
    $stmt->bind_param("s", $userData['google_id']);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows > 0) {
        // Existing user - update last login
        $user = $result->fetch_assoc();
        
        $updateStmt = $conn->prepare("UPDATE users SET last_login = NOW(), name = ?, profile_picture_url = ? WHERE id = ?");
        if (!$updateStmt) {
            throw new Exception("Update prepare failed: " . $conn->error);
        }
        
        $updateStmt->bind_param("ssi", $userData['name'], $userData['picture'], $user['id']);
        $updateStmt->execute();
        $updateStmt->close();
        
        error_log("User logged in: " . $user['email'] . " (ID: " . $user['id'] . ")");
    } else {
        // New user - create account
        $insertStmt = $conn->prepare(
            "INSERT INTO users (google_id, email, name, profile_picture_url, last_login) 
             VALUES (?, ?, ?, ?, NOW())"
        );
        
        if (!$insertStmt) {
            throw new Exception("Insert prepare failed: " . $conn->error);
        }
        
        $insertStmt->bind_param(
            "ssss",
            $userData['google_id'],
            $userData['email'],
            $userData['name'],
            $userData['picture']
        );
        
        if (!$insertStmt->execute()) {
            throw new Exception("Insert failed: " . $insertStmt->error);
        }
        
        $user = [
            'id' => $conn->insert_id,
            'email' => $userData['email'],
            'name' => $userData['name'],
            'profile_picture_url' => $userData['picture']
        ];
        
        $insertStmt->close();
        
        error_log("New user created: " . $user['email'] . " (ID: " . $user['id'] . ")");
    }
    
    $stmt->close();
    
    // Get user's devices
    $devicesStmt = $conn->prepare(
        "SELECT 
            ud.device_id, 
            COALESCE(ds.device_name, CONCAT('Device ', ud.device_id)) as device_name,
            ud.added_at
         FROM user_devices ud
         LEFT JOIN device_settings ds ON ud.device_id = ds.device_id
         WHERE ud.user_id = ?
         ORDER BY ud.added_at DESC"
    );
    
    if (!$devicesStmt) {
        throw new Exception("Devices query prepare failed: " . $conn->error);
    }
    
    $devicesStmt->bind_param("i", $user['id']);
    $devicesStmt->execute();
    $devicesResult = $devicesStmt->get_result();
    
    $devices = [];
    while ($row = $devicesResult->fetch_assoc()) {
        $devices[] = [
            'device_id' => $row['device_id'],
            'device_name' => $row['device_name'],
            'added_at' => $row['added_at']
        ];
    }
    
    $devicesStmt->close();
    
    // Return success response
    $response = [
        'success' => true,
        'user' => [
            'id' => (int)$user['id'],
            'email' => $user['email'],
            'name' => $user['name'],
            'profile_picture_url' => $user['profile_picture_url'],
            'google_id' => $userData['google_id']
        ],
        'devices' => $devices,
        'token' => $idToken, // Client will use this for subsequent requests
        'has_devices' => count($devices) > 0
    ];
    
    echo json_encode($response);
    
} catch (Exception $e) {
    error_log("Auth error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(['error' => 'Internal server error']);
} finally {
    $conn->close();
}
?>
