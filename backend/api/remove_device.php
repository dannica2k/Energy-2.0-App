<?php
/**
 * Remove Device Endpoint
 * Energy20 - Multi-User Authentication
 * 
 * Allows authenticated users to remove devices from their account
 * Validates user owns the device before removal
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Allow POST or DELETE methods
if ($_SERVER['REQUEST_METHOD'] !== 'POST' && $_SERVER['REQUEST_METHOD'] !== 'DELETE') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed. Use POST or DELETE.']);
    exit;
}

// Include required files
require_once __DIR__ . '/../db_config.php';
require_once __DIR__ . '/../config/google_auth.php';

// Require authentication
$userData = requireAuthentication();

// Get request body
$input = json_decode(file_get_contents('php://input'), true);

if (!$input) {
    http_response_code(400);
    echo json_encode(['error' => 'Invalid JSON in request body']);
    exit;
}

// Extract device ID
$deviceId = $input['device_id'] ?? null;

if (!$deviceId) {
    http_response_code(400);
    echo json_encode(['error' => 'Missing device_id in request']);
    exit;
}

// Sanitize device ID
$deviceId = trim($deviceId);

if (empty($deviceId)) {
    http_response_code(400);
    echo json_encode(['error' => 'device_id cannot be empty']);
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
    // Get user ID from database
    $userStmt = $conn->prepare("SELECT id, email FROM users WHERE google_id = ?");
    if (!$userStmt) {
        throw new Exception("User query prepare failed: " . $conn->error);
    }
    
    $userStmt->bind_param("s", $userData['google_id']);
    $userStmt->execute();
    $userResult = $userStmt->get_result();
    
    if ($userResult->num_rows === 0) {
        http_response_code(404);
        echo json_encode(['error' => 'User not found']);
        exit;
    }
    
    $user = $userResult->fetch_assoc();
    $userId = $user['id'];
    $userStmt->close();
    
    // Check if user owns this device
    $checkStmt = $conn->prepare(
        "SELECT id FROM user_devices WHERE user_id = ? AND device_id = ?"
    );
    
    if (!$checkStmt) {
        throw new Exception("Check query prepare failed: " . $conn->error);
    }
    
    $checkStmt->bind_param("is", $userId, $deviceId);
    $checkStmt->execute();
    $checkResult = $checkStmt->get_result();
    
    if ($checkResult->num_rows === 0) {
        $checkStmt->close();
        http_response_code(404);
        echo json_encode([
            'error' => 'Device not found',
            'message' => 'This device is not registered to your account.'
        ]);
        exit;
    }
    
    $checkStmt->close();
    
    // Remove device from user's account
    $deleteStmt = $conn->prepare(
        "DELETE FROM user_devices WHERE user_id = ? AND device_id = ?"
    );
    
    if (!$deleteStmt) {
        throw new Exception("Delete prepare failed: " . $conn->error);
    }
    
    $deleteStmt->bind_param("is", $userId, $deviceId);
    
    if (!$deleteStmt->execute()) {
        throw new Exception("Delete failed: " . $deleteStmt->error);
    }
    
    $deleteStmt->close();
    
    error_log("Device removed: $deviceId from user: " . $user['email'] . " (ID: $userId)");
    
    // Get updated device list
    $devicesStmt = $conn->prepare(
        "SELECT 
            ud.device_id, 
            COALESCE(MAX(ds.device_name), CONCAT('Device ', ud.device_id)) as device_name,
            ud.added_at
         FROM user_devices ud
         LEFT JOIN device_settings ds ON ud.device_id = ds.device_id
         WHERE ud.user_id = ?
         GROUP BY ud.device_id, ud.added_at
         ORDER BY ud.added_at DESC"
    );
    
    if (!$devicesStmt) {
        throw new Exception("Devices query prepare failed: " . $conn->error);
    }
    
    $devicesStmt->bind_param("i", $userId);
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
    echo json_encode([
        'success' => true,
        'message' => 'Device removed successfully',
        'device_id' => $deviceId,
        'devices' => $devices,
        'device_count' => count($devices)
    ]);
    
} catch (Exception $e) {
    error_log("Remove device error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(['error' => 'Internal server error']);
} finally {
    $conn->close();
}
?>
