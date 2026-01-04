# Energy20 API Reference - Authentication Endpoints

## Base URL
```
https://energy.webmenow.org/api
```

## Authentication
All authenticated endpoints require a Bearer token in the Authorization header:
```
Authorization: Bearer <GOOGLE_ID_TOKEN>
```

---

## Endpoints

### 1. POST /api/auth.php
**Purpose:** Authenticate user with Google Sign-In

**Authentication:** None required (this IS the login endpoint)

**Request Body:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6..."
}
```

**Success Response (200):**
```json
{
  "success": true,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "profile_picture_url": "https://...",
    "google_id": "1234567890"
  },
  "devices": [
    {
      "device_id": "device_001",
      "device_name": "Home Energy Monitor",
      "added_at": "2026-01-01 12:00:00"
    }
  ],
  "token": "eyJhbGciOiJSUzI1NiIsImtpZCI6...",
  "has_devices": true
}
```

**Error Responses:**
- `400`: Missing idToken
- `401`: Invalid or expired Google token
- `500`: Database error

---

### 2. POST /api/add_device.php
**Purpose:** Add a device to user's account

**Authentication:** Required (Bearer token)

**Request Body:**
```json
{
  "device_id": "device_001"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Device added successfully",
  "device": {
    "device_id": "device_001",
    "device_name": "Home Energy Monitor",
    "added_at": "2026-01-01 12:00:00"
  }
}
```

**Error Responses:**
- `400`: Missing device_id
- `401`: Missing or invalid token
- `404`: Device not found in system
- `409`: Device already added to account OR device owned by another user
- `500`: Database error

---

### 3. GET /api/get_devices.php
**Purpose:** Get list of user's devices

**Authentication:** Required (Bearer token)

**Request:** No body required

**Success Response (200):**
```json
{
  "success": true,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe"
  },
  "devices": [
    {
      "device_id": "device_001",
      "device_name": "Home Energy Monitor",
      "timezone_id": "UTC",
      "added_at": "2026-01-01 12:00:00",
      "data_count": 365,
      "last_data_date": "2026-01-01"
    }
  ],
  "device_count": 1
}
```

**Error Responses:**
- `401`: Missing or invalid token
- `404`: User not found
- `500`: Database error

---

## Testing with cURL

### 1. Test Authentication
```bash
curl -X POST https://energy.webmenow.org/api/auth.php \
  -H "Content-Type: application/json" \
  -d '{"idToken":"YOUR_GOOGLE_ID_TOKEN"}'
```

### 2. Test Add Device
```bash
curl -X POST https://energy.webmenow.org/api/add_device.php \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_GOOGLE_ID_TOKEN" \
  -d '{"device_id":"device_001"}'
```

### 3. Test Get Devices
```bash
curl -X GET https://energy.webmenow.org/api/get_devices.php \
  -H "Authorization: Bearer YOUR_GOOGLE_ID_TOKEN"
```

---

## Error Handling

All endpoints return errors in this format:
```json
{
  "error": "Error type",
  "message": "Detailed error message (optional)"
}
```

Common HTTP status codes:
- `200`: Success
- `400`: Bad Request (missing or invalid parameters)
- `401`: Unauthorized (missing or invalid token)
- `404`: Not Found (resource doesn't exist)
- `405`: Method Not Allowed (wrong HTTP method)
- `409`: Conflict (duplicate resource)
- `500`: Internal Server Error

---

## Security Notes

1. **HTTPS Only**: All production requests must use HTTPS
2. **Token Expiration**: Google ID tokens expire after 1 hour
3. **Token Verification**: Every request verifies the token with Google
4. **SQL Injection**: All queries use prepared statements
5. **Device Ownership**: Users can only access their own devices
6. **One Owner Per Device**: Each device can only be owned by one user

---

## Database Schema

### users table
```sql
id INT PRIMARY KEY
google_id VARCHAR(255) UNIQUE
email VARCHAR(255)
name VARCHAR(255)
profile_picture_url VARCHAR(500)
created_at TIMESTAMP
last_login TIMESTAMP
```

### user_devices table
```sql
id INT PRIMARY KEY
user_id INT (FK to users.id)
device_id VARCHAR(50)
added_at TIMESTAMP
UNIQUE(user_id, device_id)
```

---

## Next Steps

After backend is set up:
1. Configure Google Cloud Console OAuth credentials
2. Update `GOOGLE_CLIENT_ID` in `config/google_auth.php`
3. Run database migration (`database/auth_schema.sql`)
4. Test endpoints with cURL
5. Integrate with Android app
