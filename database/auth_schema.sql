-- Energy20 Multi-User Authentication Schema
-- Created: 2026-01-01
-- Purpose: Add user authentication and device ownership

-- Users table - stores Google OAuth user information
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    google_id VARCHAR(255) UNIQUE NOT NULL COMMENT 'Google OAuth user ID',
    email VARCHAR(255) NOT NULL COMMENT 'User email from Google',
    name VARCHAR(255) COMMENT 'User display name from Google',
    profile_picture_url VARCHAR(500) COMMENT 'Google profile picture URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL COMMENT 'Last successful login timestamp',
    INDEX idx_google_id (google_id),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stores authenticated user information from Google OAuth';

-- User-Device mapping - defines which users own which devices
CREATE TABLE IF NOT EXISTS user_devices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT 'Reference to users table',
    device_id VARCHAR(50) NOT NULL COMMENT 'Energy monitoring device ID',
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'When device was added to account',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_device (user_id, device_id) COMMENT 'Prevent duplicate device assignments',
    INDEX idx_user_id (user_id),
    INDEX idx_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Maps users to their owned energy monitoring devices';

-- Verification: Check if tables were created successfully
SELECT 
    TABLE_NAME, 
    TABLE_ROWS, 
    CREATE_TIME 
FROM 
    information_schema.TABLES 
WHERE 
    TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME IN ('users', 'user_devices');
