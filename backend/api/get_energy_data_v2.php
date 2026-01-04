<?php
/**
 * Get Energy Data Endpoint - Simplified Version
 * Energy20 - Multi-User Authentication
 */

// Start output buffering to prevent header issues
ob_start();

// Set headers
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    ob_end_flush();
    exit;
}

// Only allow GET
if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
    ob_end_flush();
    exit;
}

try {
    // Include required files
    require_once __DIR__ . '/../db_config.php';
    require_once __DIR__ . '/../config/google_auth.php';
    
    // Require authentication
    $userData = requireAuthentication();
    
    // Get parameters
    $deviceId = $_GET['device_id'] ?? null;
    $dateStart = $_GET['date_start'] ?? null;
    $dateEnd = $_GET['date_end'] ?? null;
    
    if (!$deviceId || !$dateStart || !$dateEnd) {
        http_response_code(400);
        echo json_encode(['error' => 'Missing parameters']);
        ob_end_flush();
        exit;
    }
    
    // Connect to database
    $conn = new mysqli($servername, $username, $password, $dbname);
    
    if ($conn->connect_error) {
        throw new Exception("DB connection failed");
    }
    
    // Get user ID
    $googleId = $conn->real_escape_string($userData['google_id']);
    $userQuery = "SELECT id FROM users WHERE google_id = '$googleId'";
    $userResult = $conn->query($userQuery);
    
    if (!$userResult || $userResult->num_rows === 0) {
        http_response_code(404);
        echo json_encode(['error' => 'User not found']);
        $conn->close();
        ob_end_flush();
        exit;
    }
    
    $user = $userResult->fetch_assoc();
    $userId = $user['id'];
    
    // Check device access
    $deviceIdEsc = $conn->real_escape_string($deviceId);
    $accessQuery = "SELECT id FROM user_devices WHERE user_id = $userId AND device_id = '$deviceIdEsc'";
    $accessResult = $conn->query($accessQuery);
    
    if (!$accessResult || $accessResult->num_rows === 0) {
        http_response_code(403);
        echo json_encode(['error' => 'Access denied']);
        $conn->close();
        ob_end_flush();
        exit;
    }
    
    // Get energy data
    $dateStartEsc = $conn->real_escape_string($dateStart);
    $dateEndEsc = $conn->real_escape_string($dateEnd);
    
    $energyQuery = "SELECT device_id, circuit_id, LocalTS, kWh 
                    FROM daily_energy 
                    WHERE device_id = '$deviceIdEsc' 
                    AND LocalTS BETWEEN '$dateStartEsc' AND '$dateEndEsc'
                    ORDER BY LocalTS ASC";
    
    $energyResult = $conn->query($energyQuery);
    
    if (!$energyResult) {
        throw new Exception("Energy query failed: " . $conn->error);
    }
    
    // Organize data
    $energyData = [];
    
    while ($row = $energyResult->fetch_assoc()) {
        $key = $row['device_id'] . '_' . $row['circuit_id'];
        
        if (!isset($energyData[$key])) {
            // Get circuit name
            $circuitId = (int)$row['circuit_id'];
            $nameQuery = "SELECT device_name FROM device_settings 
                         WHERE device_id = '$deviceIdEsc' AND circuit_id = $circuitId LIMIT 1";
            $nameResult = $conn->query($nameQuery);
            
            $circuitName = ($nameResult && $nameResult->num_rows > 0) 
                ? $nameResult->fetch_assoc()['device_name']
                : "Circuit " . $row['circuit_id'];
            
            $energyData[$key] = [
                'name' => $circuitName,
                'data' => []
            ];
        }
        
        $energyData[$key]['data'][$row['LocalTS']] = (float)$row['kWh'];
    }
    
    $conn->close();
    
    // Return success
    echo json_encode([
        'success' => true,
        'device_id' => $deviceId,
        'date_start' => $dateStart,
        'date_end' => $dateEnd,
        'data' => $energyData
    ], JSON_PRETTY_PRINT);
    
} catch (Exception $e) {
    error_log("get_energy_data_v2 error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        'error' => 'Internal server error',
        'details' => $e->getMessage()
    ]);
}

ob_end_flush();
?>
