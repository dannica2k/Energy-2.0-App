<?php
/**
 * Add Device Endpoint
 * Energy20 - Multi-User Authentication
 * 
 * Allows authenticated users to add devices to their account
 * Validates device exists in the system before adding
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

// Extract optional location data
$latitude = $input['latitude'] ?? null;
$longitude = $input['longitude'] ?? null;

// Validate coordinates if provided
if ($latitude !== null || $longitude !== null) {
    // Both must be provided together
    if ($latitude === null || $longitude === null) {
        http_response_code(400);
        echo json_encode(['error' => 'Both latitude and longitude must be provided together']);
        exit;
    }
    
    // Validate ranges
    if ($latitude < -90 || $latitude > 90) {
        http_response_code(400);
        echo json_encode(['error' => 'Latitude must be between -90 and 90']);
        exit;
    }
    
    if ($longitude < -180 || $longitude > 180) {
        http_response_code(400);
        echo json_encode(['error' => 'Longitude must be between -180 and 180']);
        exit;
    }
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
    
    // Verify device exists in daily_energy table
    $deviceCheckStmt = $conn->prepare(
        "SELECT DISTINCT device_id FROM daily_energy WHERE device_id = ? LIMIT 1"
    );
    
    if (!$deviceCheckStmt) {
        throw new Exception("Device check prepare failed: " . $conn->error);
    }
    
    $deviceCheckStmt->bind_param("s", $deviceId);
    $deviceCheckStmt->execute();
    $deviceCheckResult = $deviceCheckStmt->get_result();
    
    if ($deviceCheckResult->num_rows === 0) {
        $deviceCheckStmt->close();
        http_response_code(404);
        echo json_encode([
            'error' => 'Device not found in system',
            'message' => 'The device ID you entered does not exist. Please check the ID and try again.'
        ]);
        exit;
    }
    
    $deviceCheckStmt->close();
    
    // Check if user already has this device
    $existingStmt = $conn->prepare(
        "SELECT id FROM user_devices WHERE user_id = ? AND device_id = ?"
    );
    
    if (!$existingStmt) {
        throw new Exception("Existing check prepare failed: " . $conn->error);
    }
    
    $existingStmt->bind_param("is", $userId, $deviceId);
    $existingStmt->execute();
    $existingResult = $existingStmt->get_result();
    
    if ($existingResult->num_rows > 0) {
        $existingStmt->close();
        http_response_code(409);
        echo json_encode([
            'error' => 'Device already added',
            'message' => 'This device is already registered to your account.'
        ]);
        exit;
    }
    
    $existingStmt->close();
    
    // Note: We allow multiple users to access the same device
    // No ownership check needed - devices can be shared
    
    // Add device to user's account with optional location
    if ($latitude !== null && $longitude !== null) {
        $insertStmt = $conn->prepare(
            "INSERT INTO user_devices (user_id, device_id, latitude, longitude) VALUES (?, ?, ?, ?)"
        );
        
        if (!$insertStmt) {
            throw new Exception("Insert prepare failed: " . $conn->error);
        }
        
        $insertStmt->bind_param("isdd", $userId, $deviceId, $latitude, $longitude);
    } else {
        $insertStmt = $conn->prepare(
            "INSERT INTO user_devices (user_id, device_id) VALUES (?, ?)"
        );
        
        if (!$insertStmt) {
            throw new Exception("Insert prepare failed: " . $conn->error);
        }
        
        $insertStmt->bind_param("is", $userId, $deviceId);
    }
    
    if (!$insertStmt->execute()) {
        throw new Exception("Insert failed: " . $insertStmt->error);
    }
    
    $insertStmt->close();
    
    // Get device name from device_settings
    $nameStmt = $conn->prepare(
        "SELECT device_name FROM device_settings WHERE device_id = ? LIMIT 1"
    );
    
    if (!$nameStmt) {
        throw new Exception("Name query prepare failed: " . $conn->error);
    }
    
    $nameStmt->bind_param("s", $deviceId);
    $nameStmt->execute();
    $nameResult = $nameStmt->get_result();
    
    $deviceName = $nameResult->num_rows > 0 
        ? $nameResult->fetch_assoc()['device_name']
        : "Device $deviceId";
    
    $nameStmt->close();
    
    error_log("Device added: $deviceId to user: " . $user['email'] . " (ID: $userId)");
    
    // Build device response
    $deviceResponse = [
        'device_id' => $deviceId,
        'device_name' => $deviceName,
        'added_at' => date('Y-m-d H:i:s'),
        'is_active' => false,
        'timezone_id' => null,
        'latitude' => $latitude,
        'longitude' => $longitude
    ];
    
    // Return success response
    echo json_encode([
        'success' => true,
        'message' => 'Device added successfully',
        'device' => $deviceResponse
    ]);
    
} catch (Exception $e) {
    error_log("Add device error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(['error' => 'Internal server error']);
} finally {
    $conn->close();
}
?>
