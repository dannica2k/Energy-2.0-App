<?php
/**
 * Get Devices Endpoint
 * Energy20 - Multi-User Authentication
 * 
 * Returns list of devices owned by the authenticated user
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Only allow GET requests
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed. Use GET.']);
    exit;
}

// Include required files
require_once __DIR__ . '/../db_config.php';
require_once __DIR__ . '/../config/google_auth.php';

// Require authentication
$userData = requireAuthentication();

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
    $userStmt = $conn->prepare("SELECT id, email, name FROM users WHERE google_id = ?");
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
    
    // Get user's devices with additional info
    // Note: GROUP BY ensures only one row per device (device_settings has multiple rows per device for circuits)
    $devicesStmt = $conn->prepare(
        "SELECT 
            ud.device_id, 
            COALESCE(MAX(ds.device_name), CONCAT('Device ', ud.device_id)) as device_name,
            MAX(ds.timezone_id) as timezone_id,
            ud.added_at,
            (SELECT COUNT(*) FROM daily_energy de WHERE de.device_id = ud.device_id) as data_count,
            (SELECT MAX(LocalTS) FROM daily_energy de WHERE de.device_id = ud.device_id) as last_data_date
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
            'timezone_id' => $row['timezone_id'],
            'added_at' => $row['added_at'],
            'data_count' => (int)$row['data_count'],
            'last_data_date' => $row['last_data_date']
        ];
    }
    
    $devicesStmt->close();
    
    // Return success response
    echo json_encode([
        'success' => true,
        'user' => [
            'id' => (int)$user['id'],
            'email' => $user['email'],
            'name' => $user['name']
        ],
        'devices' => $devices,
        'device_count' => count($devices)
    ]);
    
} catch (Exception $e) {
    error_log("Get devices error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(['error' => 'Internal server error']);
} finally {
    $conn->close();
}
?>
