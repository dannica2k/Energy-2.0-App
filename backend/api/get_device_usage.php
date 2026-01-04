<?php
/**
 * Get Device Usage Endpoint
 * Energy20 - Multi-User Authentication
 * 
 * Returns device settings and energy summary
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

if (!$deviceId) {
    http_response_code(400);
    echo json_encode(['error' => 'Missing required parameter: device_id']);
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
    
    // Get device settings (first circuit for main device info)
    $settingsStmt = $conn->prepare(
        "SELECT device_id, sensor_id, device_name, timezone_id 
         FROM device_settings 
         WHERE device_id = ?
         ORDER BY sensor_id ASC
         LIMIT 1"
    );
    
    if (!$settingsStmt) {
        throw new Exception("Settings query prepare failed: " . $conn->error);
    }
    
    $settingsStmt->bind_param("s", $deviceId);
    $settingsStmt->execute();
    $settingsResult = $settingsStmt->get_result();
    
    if ($settingsResult->num_rows === 0) {
        http_response_code(404);
        echo json_encode(['error' => 'Device settings not found']);
        exit;
    }
    
    $mainSettings = $settingsResult->fetch_assoc();
    $settingsStmt->close();
    
    // Get all circuits for this device
    $circuitsStmt = $conn->prepare(
        "SELECT device_id, sensor_id, device_name, burden
         FROM device_settings 
         WHERE device_id = ?
         ORDER BY sensor_id ASC"
    );
    
    if (!$circuitsStmt) {
        throw new Exception("Circuits query prepare failed: " . $conn->error);
    }
    
    $circuitsStmt->bind_param("s", $deviceId);
    $circuitsStmt->execute();
    $circuitsResult = $circuitsStmt->get_result();
    
    $devices = [];
    while ($row = $circuitsResult->fetch_assoc()) {
        $devices[] = [
            'device_id' => $row['device_id'],
            'sensor_id' => (int)$row['sensor_id'],
            'device_name' => $row['device_name'],
            'burden' => (int)$row['burden']
        ];
    }
    
    $circuitsStmt->close();
    
    // Get energy summary (from start date to today)
    $summaryStmt = $conn->prepare(
        "SELECT 
            MIN(LocalTS) as start_date,
            SUM(CASE WHEN LocalTS = CURDATE() THEN energy_consumer_kwh ELSE 0 END) as today_kwh,
            SUM(energy_consumer_kwh) as total_kwh,
            DATEDIFF(CURDATE(), MIN(LocalTS)) as days_since_start
         FROM daily_energy 
         WHERE device_id = ?"
    );
    
    if (!$summaryStmt) {
        throw new Exception("Summary query prepare failed: " . $conn->error);
    }
    
    $summaryStmt->bind_param("s", $deviceId);
    $summaryStmt->execute();
    $summaryResult = $summaryStmt->get_result();
    
    $summary = $summaryResult->fetch_assoc();
    $summaryStmt->close();
    
    // Calculate derived values
    $daysSinceStart = (int)($summary['days_since_start'] ?? 0);
    $totalKwh = (float)($summary['total_kwh'] ?? 0);
    $todayKwh = (float)($summary['today_kwh'] ?? 0);
    
    // Default values for cost calculation (can be made configurable later)
    $costPerKwh = 0.12;
    $dailyKwhAllowance = 10.0;
    
    $cumulativeAllowance = $daysSinceStart * $dailyKwhAllowance;
    $billableKwh = max(0, $totalKwh - $cumulativeAllowance);
    $cost = $billableKwh * $costPerKwh;
    
    // Return data in the format expected by Android app
    echo json_encode([
        'device_settings' => [
            'device_id' => $deviceId,
            'timezone_id' => $mainSettings['timezone_id'] ?? 'UTC',
            'cost_per_kwh' => $costPerKwh,
            'daily_kwh_allowance' => $dailyKwhAllowance,
            'new_firmware_version' => '1.0.0',
            'firmware_update_required' => false
        ],
        'energy_summary' => [
            'start_date' => $summary['start_date'] ?? date('Y-m-d'),
            'today_kwh' => $todayKwh,
            'total_kwh' => $totalKwh,
            'days_since_start' => $daysSinceStart,
            'cumulative_allowance' => $cumulativeAllowance,
            'billable_kwh' => $billableKwh,
            'cost' => $cost
        ],
        'devices' => $devices
    ], JSON_PRETTY_PRINT);
    
} catch (Exception $e) {
    error_log("Get device usage error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(['error' => 'Internal server error']);
} finally {
    $conn->close();
}
?>
