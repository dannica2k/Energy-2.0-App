-- Add Location Fields to User Devices
-- Energy20 - Device Location Feature
-- Created: 2026-01-04
-- Purpose: Add latitude and longitude to user_devices table for per-device weather data

-- Add latitude and longitude columns to user_devices table
ALTER TABLE user_devices 
ADD COLUMN latitude DECIMAL(10, 8) NULL COMMENT 'Device location latitude (-90 to 90)',
ADD COLUMN longitude DECIMAL(11, 8) NULL COMMENT 'Device location longitude (-180 to 180)';

-- Add index for location queries (optional, for future geo-based features)
CREATE INDEX idx_location ON user_devices(latitude, longitude);

-- Verification: Check if columns were added successfully
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM 
    information_schema.COLUMNS 
WHERE 
    TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'user_devices'
    AND COLUMN_NAME IN ('latitude', 'longitude');

-- Show sample data structure
DESCRIBE user_devices;
