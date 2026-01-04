<?php
/**
 * Get Energy Data Endpoint
 * Energy20 - Multi-User Authentication
 * 
 * Returns daily energy consumption data for a device
 * Requires authentication and device ownership
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

// Get query parameters
$deviceId = $_GET['device_id'] ?? null;
$dateStart = $_GET['date_start'] ?? null;
$dateEnd = $_GET['date_end'] ?? null;

if (!$deviceId || !$dateStart || !$dateEnd) {
    http_response_code(400);
    echo json_encode(['error' => 'Missing required parameters: device_id, date_start, date_end']);
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
    // Get user ID
    $userStmt = $conn->prepare("SELECT id FROM users WHERE google_id = ?");
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
    
    // Verify user has access to this device
    $accessStmt = $conn->prepare(
        "SELECT id FROM user_devices WHERE user_id = ? AND device_id = ?"
    );
    
    if (!$accessStmt) {
        throw new Exception("Access check prepare failed: " . $conn->error);
    }
    
    $accessStmt->bind_param("is", $userId, $deviceId);
    $accessStmt->execute();
    $accessResult = $accessStmt->get_result();
    
    if ($accessResult->num_rows === 0) {
        $accessStmt->close();
        http_response_code(403);
        echo json_encode([
            'error' => 'Access denied',
            'message' => 'You do not have access to this device. Add it in Settings first.'
        ]);
        exit;
    }
    
    $accessStmt->close();
    
    // Fetch energy data
    // Note: Using < DATE_ADD to ensure end date is fully included (handles midnight timestamps)
    $energyStmt = $conn->prepare(
        "SELECT device_id, circuit_id, LocalTS, energy_consumer_kwh 
         FROM daily_energy 
         WHERE device_id = ? 
         AND LocalTS >= ? 
         AND LocalTS < DATE_ADD(?, INTERVAL 1 DAY)
         ORDER BY LocalTS ASC"
    );
    
    if (!$energyStmt) {
        throw new Exception("Energy query prepare failed: " . $conn->error);
    }
    
    $energyStmt->bind_param("sss", $deviceId, $dateStart, $dateEnd);
    $energyStmt->execute();
    $energyResult = $energyStmt->get_result();
    
    // Organize data by device_circuit key
    $energyData = [];
    
    while ($row = $energyResult->fetch_assoc()) {
        $key = $row['device_id'] . '_' . $row['circuit_id'];
        
        if (!isset($energyData[$key])) {
            // Get circuit name from device_settings
            $nameStmt = $conn->prepare(
                "SELECT device_name FROM device_settings 
                 WHERE device_id = ? AND sensor_id = ? LIMIT 1"
            );
            $nameStmt->bind_param("si", $row['device_id'], $row['circuit_id']);
            $nameStmt->execute();
            $nameResult = $nameStmt->get_result();
            
            $circuitName = $nameResult->num_rows > 0 
                ? $nameResult->fetch_assoc()['device_name']
                : "Circuit " . $row['circuit_id'];
            
            $nameStmt->close();
            
            $energyData[$key] = [
                'name' => $circuitName,
                'data' => []
            ];
        }
        
        $energyData[$key]['data'][$row['LocalTS']] = (float)$row['energy_consumer_kwh'];
    }
    
    $energyStmt->close();
    
    // Return data
    echo json_encode([
        'success' => true,
        'device_id' => $deviceId,
        'date_start' => $dateStart,
        'date_end' => $dateEnd,
        'data' => $energyData
    ], JSON_PRETTY_PRINT);
    
} catch (Exception $e) {
    error_log("Get energy data error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(['error' => 'Internal server error']);
} finally {
    $conn->close();
}
?>
