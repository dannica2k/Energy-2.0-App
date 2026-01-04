<?php
/**
 * DEBUG VERSION - Get Energy Data Endpoint
 * This version shows detailed errors for debugging
 */

// Enable error display for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

echo json_encode(['step' => 1, 'message' => 'Starting...']) . "\n";

// Include required files
try {
    require_once __DIR__ . '/../db_config.php';
    echo json_encode(['step' => 2, 'message' => 'db_config.php loaded']) . "\n";
} catch (Exception $e) {
    echo json_encode(['error' => 'Failed to load db_config.php', 'details' => $e->getMessage()]) . "\n";
    exit;
}

try {
    require_once __DIR__ . '/../config/google_auth.php';
    echo json_encode(['step' => 3, 'message' => 'google_auth.php loaded']) . "\n";
} catch (Exception $e) {
    echo json_encode(['error' => 'Failed to load google_auth.php', 'details' => $e->getMessage()]) . "\n";
    exit;
}

// Require authentication
try {
    $userData = requireAuthentication();
    echo json_encode(['step' => 4, 'message' => 'Authentication successful', 'user' => $userData]) . "\n";
} catch (Exception $e) {
    echo json_encode(['error' => 'Authentication failed', 'details' => $e->getMessage()]) . "\n";
    exit;
}

// Get query parameters
$deviceId = $_GET['device_id'] ?? null;
$dateStart = $_GET['date_start'] ?? null;
$dateEnd = $_GET['date_end'] ?? null;

echo json_encode(['step' => 5, 'params' => compact('deviceId', 'dateStart', 'dateEnd')]) . "\n";

if (!$deviceId || !$dateStart || !$dateEnd) {
    echo json_encode(['error' => 'Missing required parameters']) . "\n";
    exit;
}

// Connect to database
try {
    $conn = new mysqli($servername, $username, $password, $dbname);
    
    if ($conn->connect_error) {
        throw new Exception("Connection failed: " . $conn->connect_error);
    }
    
    echo json_encode(['step' => 6, 'message' => 'Database connected']) . "\n";
} catch (Exception $e) {
    echo json_encode(['error' => 'Database connection failed', 'details' => $e->getMessage()]) . "\n";
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
        echo json_encode(['error' => 'User not found in database']) . "\n";
        exit;
    }
    
    $user = $userResult->fetch_assoc();
    $userId = $user['id'];
    $userStmt->close();
    
    echo json_encode(['step' => 7, 'message' => 'User found', 'user_id' => $userId]) . "\n";
    
    // Verify user has access to this device
    $accessStmt = $conn->prepare("SELECT id FROM user_devices WHERE user_id = ? AND device_id = ?");
    if (!$accessStmt) {
        throw new Exception("Access check prepare failed: " . $conn->error);
    }
    
    $accessStmt->bind_param("is", $userId, $deviceId);
    $accessStmt->execute();
    $accessResult = $accessStmt->get_result();
    
    if ($accessResult->num_rows === 0) {
        echo json_encode(['error' => 'User does not have access to this device']) . "\n";
        exit;
    }
    
    $accessStmt->close();
    echo json_encode(['step' => 8, 'message' => 'Access verified']) . "\n";
    
    // Fetch energy data
    $energyStmt = $conn->prepare(
        "SELECT device_id, circuit_id, LocalTS, kWh 
         FROM daily_energy 
         WHERE device_id = ? 
         AND LocalTS BETWEEN ? AND ?
         ORDER BY LocalTS ASC
         LIMIT 10"
    );
    
    if (!$energyStmt) {
        throw new Exception("Energy query prepare failed: " . $conn->error);
    }
    
    $energyStmt->bind_param("sss", $deviceId, $dateStart, $dateEnd);
    $energyStmt->execute();
    $energyResult = $energyStmt->get_result();
    
    echo json_encode(['step' => 9, 'message' => 'Query executed', 'rows' => $energyResult->num_rows]) . "\n";
    
    // Fetch first few rows
    $rows = [];
    while ($row = $energyResult->fetch_assoc()) {
        $rows[] = $row;
    }
    
    $energyStmt->close();
    
    echo json_encode(['step' => 10, 'message' => 'SUCCESS', 'sample_data' => $rows]) . "\n";
    
} catch (Exception $e) {
    echo json_encode(['error' => 'Query error', 'details' => $e->getMessage(), 'trace' => $e->getTraceAsString()]) . "\n";
} finally {
    $conn->close();
}
?>
