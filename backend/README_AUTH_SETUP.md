# Energy20 Backend Authentication Setup

## Prerequisites
- PHP 7.4 or higher
- MySQL 5.7 or higher
- cURL extension enabled in PHP
- Access to Google Cloud Console

## Step 1: Google Cloud Console Setup

### 1.1 Create Google Cloud Project
1. Go to https://console.cloud.google.com
2. Click "Select a project" → "New Project"
3. Project name: "Energy20"
4. Click "Create"

### 1.2 Enable Required APIs
1. In the Google Cloud Console, go to "APIs & Services" → "Library"
2. Search for and enable these APIs:
   - **"Google+ API"** (legacy, but still used for sign-in)
   - OR search for **"People API"** (newer alternative)
3. Click "Enable" on the API you find

**Note:** As of 2024, Google Sign-In doesn't require a separate API to be enabled in most cases. The OAuth 2.0 credentials themselves are sufficient. If you encounter authentication errors later, you can enable the "People API" at that time.

### 1.3 Configure OAuth Consent Screen
1. Go to "APIs & Services" → "OAuth consent screen"
2. User Type: Select "External"
3. Click "Create"
4. Fill in required fields:
   - App name: "Energy20"
   - User support email: your email
   - Developer contact: your email
5. Scopes: Click "Add or Remove Scopes"
   - Select: `email`, `profile`, `openid`
6. Test users: Add your Google account email
7. Click "Save and Continue"

### 1.4 Create OAuth 2.0 Credentials

#### Web Client (for PHP Backend)
1. Go to "APIs & Services" → "Credentials"
2. Click "Create Credentials" → "OAuth 2.0 Client ID"
3. Application type: "Web application"
4. Name: "Energy20 Backend"
5. Authorized redirect URIs: (leave empty for now)
6. Click "Create"
7. **IMPORTANT**: Copy the "Client ID" - you'll need this for PHP

#### Android Client
1. Click "Create Credentials" → "OAuth 2.0 Client ID" again
2. Application type: "Android"
3. Name: "Energy20 Android"
4. Package name: `com.example.energy20`
5. SHA-1 certificate fingerprint: 
   - Get from Android Studio (see Step 3 below)
6. Click "Create"

## Step 2: Database Setup

### 2.1 Run Schema Migration
```bash
# Connect to MySQL
mysql -u your_username -p your_database_name

# Run the schema file
source /path/to/Energy20/database/auth_schema.sql
```

Or using phpMyAdmin:
1. Open phpMyAdmin
2. Select your database
3. Go to "Import" tab
4. Choose file: `database/auth_schema.sql`
5. Click "Go"

### 2.2 Verify Tables Created
```sql
SHOW TABLES LIKE 'users';
SHOW TABLES LIKE 'user_devices';

-- Should show both tables
```

## Step 3: Get Android SHA-1 Fingerprint

### Option A: Using Gradle (Recommended)
```bash
cd /path/to/Energy20
gradlew signingReport
```

Look for the SHA-1 under "Variant: debug" → "SHA1"

### Option B: Using keytool
```bash
# Windows
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

# Mac/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA-1 fingerprint and add it to your Android OAuth client in Google Cloud Console.

## Step 4: Configure PHP Backend

### 4.1 Create config/google_auth.php
Create the file at: `backend/config/google_auth.php`

Replace `YOUR_WEB_CLIENT_ID_HERE` with the Web Client ID from Step 1.4

### 4.2 Create API Directory Structure
```
backend/
├── config/
│   └── google_auth.php
├── api/
│   ├── auth.php
│   ├── add_device.php
│   └── get_devices.php
└── db_config.php (existing)
```

### 4.3 Update .htaccess (if using Apache)
Add to your `.htaccess`:
```apache
# Allow Authorization header
SetEnvIf Authorization "(.*)" HTTP_AUTHORIZATION=$1

# CORS headers for API
Header set Access-Control-Allow-Origin "*"
Header set Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS"
Header set Access-Control-Allow-Headers "Content-Type, Authorization"
```

## Step 5: Test Backend

### 5.1 Test Token Verification
```bash
# Get a test token from Google OAuth Playground
# https://developers.google.com/oauthplayground/

curl -X POST https://energy.webmenow.org/api/auth.php \
  -H "Content-Type: application/json" \
  -d '{"idToken":"YOUR_TEST_TOKEN_HERE"}'
```

Expected response:
```json
{
  "success": true,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "User Name",
    "google_id": "..."
  },
  "devices": [],
  "token": "..."
}
```

### 5.2 Test Add Device
```bash
curl -X POST https://energy.webmenow.org/api/add_device.php \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"device_id":"your_device_id"}'
```

## Step 6: Security Checklist

- [ ] HTTPS enabled on server
- [ ] Google Client ID configured in `google_auth.php`
- [ ] Database credentials secured (not in public directory)
- [ ] `.htaccess` configured for API access
- [ ] Test user added to OAuth consent screen
- [ ] SHA-1 fingerprint added to Android OAuth client

## Troubleshooting

### "Invalid token" error
- Verify Web Client ID matches in `google_auth.php`
- Check token hasn't expired (tokens expire after 1 hour)
- Ensure token is from the correct Google project

### "Database connection failed"
- Check `db_config.php` credentials
- Verify MySQL server is running
- Check database user has proper permissions

### "Device not found in system"
- Verify device_id exists in `daily_energy` table
- Check spelling of device_id

### CORS errors
- Verify `.htaccess` CORS headers
- Check Apache mod_headers is enabled
- For development, you may need to adjust CORS settings

## Next Steps

After backend setup is complete:
1. Configure Android app with OAuth credentials
2. Test Google Sign-In flow
3. Test device registration
4. Migrate existing devices to user accounts

## Support

For issues, check:
- PHP error logs: `/var/log/apache2/error.log` or similar
- MySQL error logs
- Browser console for CORS/network errors
