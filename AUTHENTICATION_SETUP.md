# Energy20 Authentication Setup Guide

## Overview
This guide walks you through setting up Google Sign-In authentication for the Energy20 Android app.

## Prerequisites Checklist
- [ ] Google Cloud Console account
- [ ] MySQL database access
- [ ] PHP backend server access
- [ ] Android Studio installed
- [ ] Energy20 project cloned/downloaded

---

## Part 1: Google Cloud Console Setup

### Step 1: Create Google Cloud Project
1. Go to https://console.cloud.google.com
2. Click "Select a project" → "New Project"
3. Project name: **Energy20**
4. Click "Create"
5. Wait for project creation to complete

### Step 2: Configure OAuth Consent Screen
1. In Google Cloud Console, go to **"APIs & Services"** → **"OAuth consent screen"**
2. User Type: Select **"External"**
3. Click **"Create"**
4. Fill in required fields:
   - **App name**: Energy20
   - **User support email**: your email
   - **Developer contact**: your email
5. Click **"Save and Continue"**
6. **Scopes**: Click "Add or Remove Scopes"
   - Select: `email`, `profile`, `openid`
   - Click "Update" then "Save and Continue"
7. **Test users**: Add your Google account email
8. Click **"Save and Continue"**
9. Review and click **"Back to Dashboard"**

### Step 3: Create OAuth 2.0 Credentials

#### 3A: Web Client (for PHP Backend)
1. Go to **"APIs & Services"** → **"Credentials"**
2. Click **"Create Credentials"** → **"OAuth 2.0 Client ID"**
3. Application type: **"Web application"**
4. Name: **"Energy20 Backend"**
5. Authorized redirect URIs: (leave empty for now)
6. Click **"Create"**
7. **IMPORTANT**: Copy the **"Client ID"** - you'll need this for PHP
   - Format: `XXXXXXXXX-XXXXXXXXXXXXXXXXXXXXXXXX.apps.googleusercontent.com`
8. Click "OK"

#### 3B: Android Client
1. Click **"Create Credentials"** → **"OAuth 2.0 Client ID"** again
2. Application type: **"Android"**
3. Name: **"Energy20 Android"**
4. Package name: `com.example.energy20`
5. SHA-1 certificate fingerprint: (see Step 4 below)
6. Click **"Create"**

### Step 4: Get Android SHA-1 Fingerprint

Open PowerShell in your project directory and run:

```powershell
.\gradlew signingReport
```

Look for output like this:
```
Variant: debug
Config: debug
Store: C:\Users\YourName\.android\debug.keystore
Alias: AndroidDebugKey
SHA1: AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00:AA:BB:CC:DD
```

**Copy the SHA1 value** and paste it into the Android OAuth client configuration in Step 3B.

---

## Part 2: Database Setup

### Step 1: Run SQL Schema
1. Connect to your MySQL database
2. Run the SQL file: `database/auth_schema.sql`

**Using MySQL command line:**
```bash
mysql -u your_username -p your_database_name < database/auth_schema.sql
```

**Or using phpMyAdmin:**
1. Open phpMyAdmin
2. Select your database
3. Go to "Import" tab
4. Choose file: `database/auth_schema.sql`
5. Click "Go"

### Step 2: Verify Tables
Run this SQL to verify:
```sql
SHOW TABLES LIKE 'users';
SHOW TABLES LIKE 'user_devices';
```

You should see both tables listed.

---

## Part 3: Backend Configuration

### Step 1: Update Google Client ID
1. Open `backend/config/google_auth.php`
2. Find this line:
   ```php
   define('GOOGLE_CLIENT_ID', 'YOUR_WEB_CLIENT_ID_HERE');
   ```
3. Replace `YOUR_WEB_CLIENT_ID_HERE` with your **Web Client ID** from Part 1, Step 3A
4. Save the file

### Step 2: Upload Backend Files
Upload these files to your server at `https://energy.webmenow.org/`:
- `backend/config/google_auth.php`
- `backend/api/auth.php`
- `backend/api/add_device.php`
- `backend/api/get_devices.php`

### Step 3: Test Backend
Run this command to test:
```bash
curl -X POST https://energy.webmenow.org/api/auth.php \
  -H "Content-Type: application/json" \
  -d '{"idToken":"fake_token"}'
```

**Expected response:**
```json
{"error":"Invalid or expired Google token"}
```

This confirms the backend is working! ✅

---

## Part 4: Android App Configuration

### Step 1: Update Web Client ID
1. Open `app/src/main/java/com/example/energy20/auth/LoginActivity.kt`
2. Find this line (around line 44):
   ```kotlin
   private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com"
   ```
3. Replace with your **Web Client ID** from Part 1, Step 3A
4. Save the file

### Step 2: Build and Run
1. In Android Studio, click **"Build"** → **"Rebuild Project"**
2. Wait for build to complete
3. Connect your Android device or start an emulator
4. Click **"Run"** (green play button)

---

## Part 5: Testing

### Test 1: Launch App
1. App should open to the **LoginActivity** screen
2. You should see:
   - Energy20 logo
   - "Sign in with Google" button
   - Subtitle text

### Test 2: Sign In
1. Click **"Sign in with Google"**
2. Select your Google account
3. Grant permissions when prompted
4. App should:
   - Show loading indicator
   - Authenticate with backend
   - Navigate to MainActivity
   - Show your name and email in navigation drawer

### Test 3: Logout
1. Open navigation drawer (swipe from left or tap hamburger menu)
2. Verify your name and email are displayed at the top
3. Scroll down to **"Account"** section
4. Tap **"Logout"**
5. Confirm logout
6. App should return to LoginActivity

---

## Troubleshooting

### "Sign-in failed: 10"
**Problem**: SHA-1 fingerprint not configured correctly

**Solution**:
1. Re-run `.\gradlew signingReport`
2. Copy the SHA-1 exactly
3. Update Android OAuth client in Google Cloud Console
4. Wait 5 minutes for changes to propagate
5. Try again

### "Invalid or expired Google token"
**Problem**: Web Client ID mismatch

**Solution**:
1. Verify Web Client ID in `LoginActivity.kt` matches Google Cloud Console
2. Verify Web Client ID in `backend/config/google_auth.php` matches
3. Make sure you're using the **Web Client ID**, not Android Client ID

### "Authentication failed: Network error"
**Problem**: Backend not accessible

**Solution**:
1. Verify backend files are uploaded to server
2. Test backend with curl command (see Part 3, Step 3)
3. Verify HTTPS is working on energy.webmenow.org

### App crashes on startup
**Problem**: Missing dependencies or build issues

**Solution**:
1. In Android Studio: **File** → **Invalidate Caches / Restart**
2. Clean project: **Build** → **Clean Project**
3. Rebuild: **Build** → **Rebuild Project**

---

## Security Notes

### Current Setup (Production-Ready):
1. ✅ **HTTPS enabled** on backend server (energy.webmenow.org)
2. ✅ **Cleartext HTTP disabled** in network_security_config.xml
3. **For Play Store release**, add release SHA-1 to Google Cloud Console:
   ```bash
   keytool -list -v -keystore your-release-key.keystore
   ```
4. **Restrict API keys** in Google Cloud Console
5. **Review OAuth consent screen** settings

### Current Setup Notes:
- ✅ HTTPS enforced for all connections
- ⚠️ Debug keystore SHA-1 (add release SHA-1 for production)
- ⚠️ OAuth consent screen in "Testing" mode (publish when ready)

---

## Next Steps

After authentication is working:

1. **Device Onboarding**: Implement QR code scanner for adding devices
2. **Multi-Device Support**: Filter energy data by user's devices
3. **Device Management**: Allow users to rename/remove devices
4. **Profile Settings**: Add user preferences and settings

---

## Support

### Documentation
- Backend API: `backend/API_REFERENCE.md`
- Backend Setup: `backend/README_AUTH_SETUP.md`
- Memory Bank: `memory-bank/` directory

### Common Issues
- Check Android Studio Logcat for error messages
- Check PHP error logs on server
- Verify all configuration values match

### Testing Checklist
- [ ] Backend responds to curl test
- [ ] App launches to LoginActivity
- [ ] Google Sign-In dialog appears
- [ ] Authentication succeeds
- [ ] User info displays in nav drawer
- [ ] Logout works correctly
- [ ] Re-login works after logout

---

## Configuration Summary

**What you need to configure:**

1. **Google Cloud Console**:
   - Web Client ID (for backend)
   - Android OAuth Client (with SHA-1)

2. **Backend** (`backend/config/google_auth.php`):
   - `GOOGLE_CLIENT_ID` = Your Web Client ID

3. **Android** (`LoginActivity.kt`):
   - `WEB_CLIENT_ID` = Your Web Client ID (same as backend)

4. **Database**:
   - Run `auth_schema.sql`

That's it! Once these are configured, authentication should work end-to-end.
