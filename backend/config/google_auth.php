<?php
/**
 * Google OAuth 2.0 Authentication Configuration
 * Energy20 - Multi-User Authentication
 * 
 * This file handles Google ID token verification for the backend API.
 * Replace YOUR_WEB_CLIENT_ID_HERE with your actual Web Client ID from Google Cloud Console.
 */

// Google OAuth Web Client ID (from Google Cloud Console)
// TODO: Replace this with your actual Web Client ID
define('GOOGLE_CLIENT_ID', '242021166104-he2lbj7avj8r6ujlhc79n7i6al087idv.apps.googleusercontent.com');

/**
 * Verify Google ID Token
 * 
 * @param string $idToken The ID token from Google Sign-In
 * @return array|false User data if valid, false if invalid
 */
function verifyGoogleToken($idToken) {
    if (empty($idToken)) {
        error_log("verifyGoogleToken: Empty token provided");
        return false;
    }
    
    // Google's tokeninfo endpoint
    $url = 'https://oauth2.googleapis.com/tokeninfo?id_token=' . urlencode($idToken);
    
    // Initialize cURL
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 10);
    
    // Execute request
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);
    
    // Check for cURL errors
    if ($response === false) {
        error_log("verifyGoogleToken: cURL error - " . $curlError);
        return false;
    }
    
    // Check HTTP response code
    if ($httpCode !== 200) {
        error_log("verifyGoogleToken: Invalid HTTP code - " . $httpCode);
        error_log("verifyGoogleToken: Response - " . $response);
        return false;
    }
    
    // Parse JSON response
    $tokenInfo = json_decode($response, true);
    
    if (!$tokenInfo) {
        error_log("verifyGoogleToken: Failed to parse JSON response");
        return false;
    }
    
    // Verify the token is for our app
    if (!isset($tokenInfo['aud']) || $tokenInfo['aud'] !== GOOGLE_CLIENT_ID) {
        error_log("verifyGoogleToken: Token audience mismatch");
        error_log("verifyGoogleToken: Expected - " . GOOGLE_CLIENT_ID);
        error_log("verifyGoogleToken: Got - " . ($tokenInfo['aud'] ?? 'none'));
        return false;
    }
    
    // Verify token hasn't expired
    if (!isset($tokenInfo['exp']) || $tokenInfo['exp'] < time()) {
        error_log("verifyGoogleToken: Token expired");
        return false;
    }
    
    // Verify email is verified
    if (!isset($tokenInfo['email_verified']) || $tokenInfo['email_verified'] !== 'true') {
        error_log("verifyGoogleToken: Email not verified");
        return false;
    }
    
    // Return user data
    return [
        'google_id' => $tokenInfo['sub'],
        'email' => $tokenInfo['email'],
        'name' => $tokenInfo['name'] ?? null,
        'picture' => $tokenInfo['picture'] ?? null,
        'email_verified' => true
    ];
}

/**
 * Get Authorization header from request
 * 
 * @return string|null The authorization header value or null
 */
function getAuthorizationHeader() {
    $headers = null;
    
    // Log all server variables for debugging
    error_log("=== Authorization Header Debug ===");
    error_log("Checking \$_SERVER['Authorization']: " . (isset($_SERVER['Authorization']) ? 'EXISTS' : 'NOT SET'));
    error_log("Checking \$_SERVER['HTTP_AUTHORIZATION']: " . (isset($_SERVER['HTTP_AUTHORIZATION']) ? 'EXISTS' : 'NOT SET'));
    error_log("Checking \$_SERVER['REDIRECT_HTTP_AUTHORIZATION']: " . (isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION']) ? 'EXISTS' : 'NOT SET'));
    
    if (isset($_SERVER['Authorization'])) {
        $headers = trim($_SERVER["Authorization"]);
        error_log("Found in \$_SERVER['Authorization']: " . substr($headers, 0, 20) . "...");
    } else if (isset($_SERVER['HTTP_AUTHORIZATION'])) {
        $headers = trim($_SERVER["HTTP_AUTHORIZATION"]);
        error_log("Found in \$_SERVER['HTTP_AUTHORIZATION']: " . substr($headers, 0, 20) . "...");
    } else if (isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
        // Cloudflare/Apache often puts it here
        $headers = trim($_SERVER["REDIRECT_HTTP_AUTHORIZATION"]);
        error_log("Found in \$_SERVER['REDIRECT_HTTP_AUTHORIZATION']: " . substr($headers, 0, 20) . "...");
    } else if (function_exists('apache_request_headers')) {
        $requestHeaders = apache_request_headers();
        error_log("Using apache_request_headers()");
        $requestHeaders = array_combine(
            array_map('ucwords', array_keys($requestHeaders)), 
            array_values($requestHeaders)
        );
        
        if (isset($requestHeaders['Authorization'])) {
            $headers = trim($requestHeaders['Authorization']);
            error_log("Found in apache_request_headers: " . substr($headers, 0, 20) . "...");
        } else {
            error_log("Authorization not found in apache_request_headers");
            error_log("Available headers: " . implode(", ", array_keys($requestHeaders)));
        }
    } else {
        error_log("apache_request_headers() not available");
    }
    
    if ($headers === null) {
        error_log("WARNING: No Authorization header found!");
    }
    
    return $headers;
}

/**
 * Extract Bearer token from Authorization header
 * 
 * @return string|null The token or null
 */
function getBearerToken() {
    $headers = getAuthorizationHeader();
    
    if (!empty($headers)) {
        if (preg_match('/Bearer\s+(.*)$/i', $headers, $matches)) {
            return $matches[1];
        }
    }
    
    return null;
}

/**
 * Verify request is authenticated and return user data
 * 
 * @return array|false User data from Google if authenticated, false otherwise
 */
function requireAuthentication() {
    $token = getBearerToken();
    
    if (!$token) {
        http_response_code(401);
        echo json_encode(['error' => 'Missing authorization token']);
        exit;
    }
    
    $userData = verifyGoogleToken($token);
    
    if (!$userData) {
        http_response_code(401);
        echo json_encode(['error' => 'Invalid or expired token']);
        exit;
    }
    
    return $userData;
}
?>
