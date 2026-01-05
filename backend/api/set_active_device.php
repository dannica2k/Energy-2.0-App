<?php
/**
 * Set Active Device API Endpoint
 * 
 * Sets a device as the active device for the authenticated user.
 * Only one device can be active at a time per user.
 * 
 * Method: POST
 * Authentication: Required (Bearer token)
 * 
 * Request Body:
 * {
 *   "device_id": "D072F19EF0C8"
 * }
 * 
 * Success Response (200):
 * {
 *   "success": true,
 *   "message": "Active device updated successfully",
 *   "device_id": "D072F19EF0C8",
 *   "devices": [...] // Updated list of all user devices
 * }
 */

// Enable error display for debugging
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// Log start of request
error_log("=== SET ACTIVE DEVICE REQUEST START ===");
error_log("Method: " . $_SERVER['REQUEST_METHOD']);
error_log("Request URI: " . $_SERVER['REQUEST_URI']);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Only allow POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed', 'message' => 'Use POST method']);
    exit();
}

error_log("Loading dependencies...");
require_once __DIR__ . '/../db_config.php';
require_once __DIR__ . '/../config/google_auth.php';
error_log("Dependencies loaded");

// Require authentication
error_log("Authenticating user...");
$userData = requireAuthentication();
$googleId = $userData['google_id'];
error_log("User authenticated: " . $googleId);

try {
    
    // Get request body
    $rawInput = file_get_contents('php://input');
    error_log("Raw input: " . $rawInput);
    
    $input = json_decode($rawInput, true);
    error_log("Decoded input: " . print_r($input, true));
    
    if (!isset($input['device_id']) || empty($input['device_id'])) {
        error_log("ERROR: device_id missing or empty");
        http_response_code(400);
        echo json_encode(['error' => 'Bad request', 'message' => 'device_id is required']);
        exit();
    }
    
    $deviceId = trim($input['device_id']);
    error_log("Device ID to set active: " . $deviceId);
    
    // Connect to database
    error_log("Connecting to database...");
    $conn = new mysqli($servername, $username, $password, $dbname);
    
    if ($conn->connect_error) {
        error_log("Database connection failed: " . $conn->connect_error);
        http_response_code(500);
        echo json_encode(['error' => 'Database connection failed', 'message' => $conn->connect_error]);
        exit();
    }
    error_log("Database connected");
    
    // Get user ID from google_id
    $stmt = $conn->prepare("SELECT id FROM users WHERE google_id = ?");
    $stmt->bind_param("s", $googleId);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows === 0) {
        http_response_code(404);
        echo json_encode(['error' => 'Not found', 'message' => 'User not found']);
        exit();
    }
    
    $user = $result->fetch_assoc();
    $userId = $user['id'];
    $stmt->close();
    
    // Verify the device belongs to this user
    $stmt = $conn->prepare("SELECT id FROM user_devices WHERE user_id = ? AND device_id = ?");
    $stmt->bind_param("is", $userId, $deviceId);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows === 0) {
        http_response_code(404);
        echo json_encode(['error' => 'Not found', 'message' => 'Device not found in your account']);
        exit();
    }
    $stmt->close();
    
    // Start transaction
    $conn->begin_transaction();
    
    try {
        // Set all user's devices to inactive
        $stmt = $conn->prepare("UPDATE user_devices SET is_active = 0 WHERE user_id = ?");
        $stmt->bind_param("i", $userId);
        $stmt->execute();
        $stmt->close();
        
        // Set the specified device as active
        $stmt = $conn->prepare("UPDATE user_devices SET is_active = 1 WHERE user_id = ? AND device_id = ?");
        $stmt->bind_param("is", $userId, $deviceId);
        $stmt->execute();
        $stmt->close();
        
        // Commit transaction
        $conn->commit();
        
        // Get updated list of devices with device info including location
        // Optimized query - removed slow correlated subqueries for data_count and last_data_date
        $stmt = $conn->prepare("
            SELECT 
                ud.device_id,
                COALESCE(MAX(ds.device_name), CONCAT('Device ', ud.device_id)) as device_name,
                ud.added_at,
                ud.is_active,
                MAX(ds.timezone_id) as timezone_id,
                ud.latitude,
                ud.longitude
            FROM user_devices ud
            LEFT JOIN device_settings ds ON ud.device_id = ds.device_id
            WHERE ud.user_id = ?
            GROUP BY ud.device_id, ud.added_at, ud.is_active, ud.latitude, ud.longitude
            ORDER BY ud.is_active DESC, ud.added_at DESC
        ");
        $stmt->bind_param("i", $userId);
        $stmt->execute();
        $result = $stmt->get_result();
        
        $devices = [];
        while ($row = $result->fetch_assoc()) {
            $devices[] = [
                'device_id' => $row['device_id'],
                'device_name' => $row['device_name'],
                'added_at' => $row['added_at'],
                'is_active' => (bool)$row['is_active'],
                'timezone_id' => $row['timezone_id'],
                'latitude' => $row['latitude'] !== null ? (float)$row['latitude'] : null,
                'longitude' => $row['longitude'] !== null ? (float)$row['longitude'] : null
            ];
        }
        $stmt->close();
        
        // Success response
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => 'Active device updated successfully',
            'device_id' => $deviceId,
            'devices' => $devices
        ]);
        
    } catch (Exception $e) {
        // Rollback on error
        $conn->rollback();
        throw $e;
    }
    
    $conn->close();
    
} catch (Exception $e) {
    error_log("Set active device error: " . $e->getMessage());
    error_log("Stack trace: " . $e->getTraceAsString());
    http_response_code(500);
    echo json_encode([
        'error' => 'Internal server error', 
        'message' => $e->getMessage(),
        'file' => $e->getFile(),
        'line' => $e->getLine(),
        'trace' => $e->getTraceAsString()
    ]);
}
?>
