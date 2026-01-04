<?php
/**
 * Debug Authentication Endpoint
 * Energy20 - Diagnostic Tool
 * 
 * Returns detailed information about the authentication request
 * to help diagnose authorization header issues
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Include required files
require_once __DIR__ . '/../config/google_auth.php';

$debug = [];

// 1. Check for Authorization header in various places
$debug['server_vars'] = [
    'Authorization' => $_SERVER['Authorization'] ?? 'NOT SET',
    'HTTP_AUTHORIZATION' => $_SERVER['HTTP_AUTHORIZATION'] ?? 'NOT SET',
];

// 2. Try apache_request_headers if available
if (function_exists('apache_request_headers')) {
    $headers = apache_request_headers();
    $debug['apache_headers'] = $headers;
    $debug['apache_authorization'] = $headers['Authorization'] ?? 'NOT SET';
} else {
    $debug['apache_headers'] = 'apache_request_headers() not available';
}

// 3. Check all $_SERVER keys that might contain auth info
$authKeys = [];
foreach ($_SERVER as $key => $value) {
    if (stripos($key, 'auth') !== false || stripos($key, 'bearer') !== false) {
        $authKeys[$key] = $value;
    }
}
$debug['server_auth_keys'] = $authKeys;

// 4. Try to get the authorization header using our function
$authHeader = getAuthorizationHeader();
$debug['getAuthorizationHeader_result'] = $authHeader ?? 'NULL';

// 5. Try to extract bearer token
$bearerToken = getBearerToken();
$debug['getBearerToken_result'] = $bearerToken ? substr($bearerToken, 0, 50) . '... (length: ' . strlen($bearerToken) . ')' : 'NULL';

// 6. If we have a token, try to verify it
if ($bearerToken) {
    $debug['token_verification'] = 'Attempting verification...';
    
    $userData = verifyGoogleToken($bearerToken);
    
    if ($userData) {
        $debug['token_verification'] = 'SUCCESS';
        $debug['user_data'] = [
            'google_id' => $userData['google_id'],
            'email' => $userData['email'],
            'name' => $userData['name'],
            'email_verified' => $userData['email_verified']
        ];
    } else {
        $debug['token_verification'] = 'FAILED';
        $debug['verification_note'] = 'Token verification failed. Check if token is expired or GOOGLE_CLIENT_ID is correct.';
    }
} else {
    $debug['token_verification'] = 'SKIPPED - No token found';
}

// 7. Server info
$debug['server_info'] = [
    'php_version' => phpversion(),
    'server_software' => $_SERVER['SERVER_SOFTWARE'] ?? 'UNKNOWN',
    'request_method' => $_SERVER['REQUEST_METHOD'],
    'request_uri' => $_SERVER['REQUEST_URI'] ?? 'UNKNOWN',
];

// 8. Google Client ID (masked for security)
$clientId = GOOGLE_CLIENT_ID;
$debug['google_client_id'] = substr($clientId, 0, 20) . '...' . substr($clientId, -20);

// Return debug information
echo json_encode([
    'success' => true,
    'message' => 'Debug information collected',
    'debug' => $debug,
    'instructions' => [
        'If Authorization header is NOT SET' => 'Server is not passing the header. Add .htaccess rewrite rule.',
        'If token verification FAILED' => 'Token might be expired or GOOGLE_CLIENT_ID mismatch.',
        'If token verification SUCCESS' => 'Authentication is working! The issue is elsewhere.'
    ]
], JSON_PRETTY_PRINT);
?>
