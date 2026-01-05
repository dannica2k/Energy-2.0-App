-- Add is_active column to user_devices table
-- This allows users to designate one device as their "active" device for the home screen

-- Add the column (defaults to 0/false)
ALTER TABLE user_devices 
ADD COLUMN is_active TINYINT(1) DEFAULT 0 
COMMENT 'Whether this is the active device for home screen display';

-- Add index for faster queries
ALTER TABLE user_devices 
ADD INDEX idx_user_active (user_id, is_active);

-- Set the first device for each user as active (if they have devices)
UPDATE user_devices ud1
SET is_active = 1
WHERE id IN (
    SELECT * FROM (
        SELECT MIN(id) 
        FROM user_devices ud2 
        GROUP BY user_id
    ) AS first_devices
);

-- Verification: Show active devices per user
SELECT 
    u.email,
    ud.device_id,
    ud.is_active,
    ud.added_at
FROM 
    users u
    LEFT JOIN user_devices ud ON u.id = ud.user_id
ORDER BY 
    u.id, ud.is_active DESC, ud.added_at;
