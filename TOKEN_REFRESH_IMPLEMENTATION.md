# Automatic Token Refresh Implementation

## Overview
This document describes the automatic token refresh mechanism implemented to handle Google ID token expiration in the Energy20 Android app.

## Problem
Google ID tokens expire after **1 hour**. When users leave the app idle for more than an hour, subsequent API requests fail with **401 Unauthorized** errors because the backend rejects expired tokens.

## Solution
Implemented automatic silent token refresh using Google Sign-In's built-in capabilities. When a 401 error is detected, the app automatically attempts to refresh the token in the background without user interaction.

## Architecture

### 1. AuthManager Enhancements
**File:** `app/src/main/java/com/example/energy20/auth/AuthManager.kt`

**New Features:**
- **Token Expiry Tracking**: Stores token expiration timestamp (current time + 1 hour)
- **Silent Token Refresh**: Uses `GoogleSignInClient.silentSignIn()` to get fresh tokens
- **Proactive Refresh Check**: `needsTokenRefresh()` method checks if token expires in < 5 minutes

**Key Methods:**
```kotlin
suspend fun refreshTokenSilently(): String?
fun needsTokenRefresh(): Boolean
fun getAuthToken(): String? // Enhanced with expiry checking
```

**How It Works:**
1. When token is saved, expiry time is calculated (current time + 3600 seconds)
2. `getAuthToken()` checks if token is expired before returning it
3. `refreshTokenSilently()` uses Google Sign-In to get a fresh token without user interaction
4. New token is stored with updated expiry time

### 2. AuthInterceptor Enhancements
**File:** `app/src/main/java/com/example/energy20/auth/AuthInterceptor.kt`

**New Features:**
- **401 Detection**: Monitors all API responses for 401 Unauthorized errors
- **Automatic Retry**: Attempts silent token refresh and retries the failed request
- **Fallback Handling**: If refresh fails, broadcasts token expiration event

**Flow:**
```
API Request → 401 Response → Close Response → Attempt Silent Refresh
    ↓                                                    ↓
Success ← Retry Request ← New Token          Refresh Failed
                                                         ↓
                                              Clear Auth Data → Broadcast Event
```

**Key Code:**
```kotlin
if (response.code == 401 && token != null) {
    response.close()
    val newToken = runBlocking { authManager.refreshTokenSilently() }
    if (newToken != null) {
        // Retry with new token
        response = chain.proceed(newRequest)
    } else {
        // Trigger re-authentication
        authManager.clearAuthData()
        context.sendBroadcast(Intent(ACTION_TOKEN_EXPIRED))
    }
}
```

### 3. MainActivity Enhancements
**File:** `app/src/main/java/com/example/energy20/MainActivity.kt`

**New Features:**
- **BroadcastReceiver**: Listens for token expiration events
- **User-Friendly Dialog**: Shows "Session Expired" message when refresh fails
- **Automatic Redirect**: Navigates to LoginActivity for re-authentication

**Flow:**
```
Token Expiration Broadcast → Show Dialog → User Clicks "Sign In" → Navigate to Login
```

## User Experience

### Scenario 1: Token Expires While App is Active
1. User is viewing energy data
2. Token expires (after 1 hour)
3. Next API request gets 401
4. **App automatically refreshes token in background**
5. Request is retried with new token
6. **User sees no interruption** - data loads normally

### Scenario 2: Token Refresh Fails
1. User is viewing energy data
2. Token expires and refresh fails (rare - e.g., network issue, Google account removed)
3. **Dialog appears**: "Session Expired - Please sign in again"
4. User clicks "Sign In"
5. Redirected to LoginActivity
6. **One-tap sign-in** (Google remembers the account)

## Technical Details

### Dependencies Added
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
```
This library provides the `.await()` extension for Google Play Services Tasks, enabling coroutine-based async operations.

### Token Lifecycle
```
Login → Token (expires in 1 hour) → Stored with expiry timestamp
                                              ↓
                                    Used for API requests
                                              ↓
                                    401 Error after 1 hour
                                              ↓
                                    Silent Refresh (automatic)
                                              ↓
                                    New Token (expires in 1 hour)
```

### Security Considerations
- Tokens stored in **EncryptedSharedPreferences** (AES256-GCM encryption)
- Silent refresh only works if user hasn't revoked app permissions
- If refresh fails, all auth data is cleared immediately
- User must re-authenticate if Google account is removed from device

## Testing

### Manual Testing Steps
1. **Install the updated APK** on your device
2. **Sign in** with Google account
3. **Use the app normally** - verify data loads
4. **Wait 1 hour** (or modify expiry time for faster testing)
5. **Interact with the app** (pull to refresh, change date range)
6. **Verify**: Data loads without showing login screen
7. **Check logs**: Should see "Silent token refresh successful"

### Testing Token Refresh Failure
1. Sign in to the app
2. Go to device Settings → Accounts → Remove Google account
3. Return to app and trigger API request
4. **Verify**: "Session Expired" dialog appears
5. Click "Sign In"
6. **Verify**: Redirected to login screen

### Logcat Monitoring
Key log tags to watch:
- `AuthManager`: Token refresh attempts and results
- `AuthInterceptor`: 401 detection and retry logic
- `MainActivity`: Token expiration broadcast handling

Example logs:
```
AuthManager: Token expires in: 55 minutes
AuthInterceptor: Response code: 401 for https://energy.webmenow.org/api/...
AuthInterceptor: Received 401 Unauthorized - attempting token refresh
AuthManager: Attempting silent token refresh...
AuthManager: Silent token refresh successful
AuthManager: New token saved. Expires at: Sat Jan 04 16:15:00 PST 2026
AuthInterceptor: Token refresh successful - retrying request
AuthInterceptor: Retry response code: 200
```

## Benefits

✅ **Seamless User Experience**: No interruptions during normal usage  
✅ **Automatic Recovery**: Handles token expiration transparently  
✅ **Graceful Degradation**: Shows friendly message if refresh fails  
✅ **Industry Standard**: Uses Google's recommended approach  
✅ **Secure**: Maintains encryption and proper token lifecycle  
✅ **Minimal User Interaction**: Only requires re-login in rare failure cases  

## Future Enhancements

### Proactive Refresh (Optional)
Could add proactive token refresh before expiration:
```kotlin
// In ViewModel or Repository, before making requests
if (authManager.needsTokenRefresh()) {
    authManager.refreshTokenSilently()
}
```

### Refresh Token Support (Advanced)
For longer sessions without re-authentication, could implement OAuth refresh tokens:
- Requires backend changes to issue refresh tokens
- Refresh tokens can last weeks/months
- More complex but provides better UX for long-term usage

## Troubleshooting

### Issue: Still getting 401 errors
**Cause**: Token refresh might be failing silently  
**Solution**: Check logcat for "Silent token refresh failed" messages

### Issue: "Session Expired" dialog appears immediately
**Cause**: Google account removed or app permissions revoked  
**Solution**: User needs to sign in again - this is expected behavior

### Issue: App crashes on 401
**Cause**: BroadcastReceiver not registered properly  
**Solution**: Verify MainActivity registers receiver in onCreate()

## Version History
- **v2.0.0** (2026-01-04): Initial implementation of automatic token refresh
