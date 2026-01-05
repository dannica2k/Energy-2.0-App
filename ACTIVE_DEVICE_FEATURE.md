# Active Device Feature Implementation

## Overview
This feature allows users to designate one of their devices as "active" for display on the home screen. Only one device can be active at a time per user.

## Database Changes

### Migration File
**File:** `database/add_active_device.sql`

Adds `is_active` column to `user_devices` table:
- Column: `is_active TINYINT(1) DEFAULT 0`
- Index: `idx_user_active (user_id, is_active)`
- Auto-sets first device as active for existing users

**To apply:**
```bash
mysql -u username -p database_name < database/add_active_device.sql
```

## Backend Changes

### New API Endpoint
**File:** `backend/api/set_active_device.php`

**Endpoint:** `POST /api/set_active_device.php`

**Request:**
```json
{
  "device_id": "D072F19EF0C8"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Active device updated successfully",
  "device_id": "D072F19EF0C8",
  "devices": [...]
}
```

**Behavior:**
- Sets all user's devices to `is_active = 0`
- Sets specified device to `is_active = 1`
- Returns updated device list
- Uses transaction for atomicity

### Updated Endpoint
**File:** `backend/api/get_devices.php`

Now includes `is_active` field in device list:
- Added `ud.is_active` to SELECT
- Added `ud.is_active` to GROUP BY
- Orders by `is_active DESC` (active device first)
- Returns `is_active` as boolean in response

## Android App Changes

### Data Model
**File:** `app/src/main/java/com/example/energy20/data/User.kt`

**Updated `UserDevice`:**
```kotlin
data class UserDevice(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("device_name")
    val deviceName: String,
    @SerializedName("added_at")
    val addedAt: String,
    @SerializedName("is_active")
    val isActive: Boolean = false,  // NEW FIELD
    // ... other fields
)
```

**New Response Model:**
```kotlin
data class SetActiveDeviceResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("device_id")
    val deviceId: String,
    val devices: List<UserDevice>
)
```

### API Service
**File:** `app/src/main/java/com/example/energy20/api/AuthApiService.kt`

**New Method:**
```kotlin
suspend fun setActiveDevice(deviceId: String): Result<SetActiveDeviceResponse>
```

### UI - Device Card Layout
**File:** `app/src/main/res/layout/item_device.xml`

**Added:**
1. **Active Status Badge** - Green "ACTIVE" label (visible when `isActive = true`)
2. **Set Active Button** - Star icon button (visible when `isActive = false`)

**Layout Structure:**
```
Device Card
├── Device Info (left)
│   ├── Device ID
│   ├── Device Name
│   ├── Added Date
│   └── Active Badge (conditional)
└── Action Buttons (right)
    ├── Set Active Button (star icon)
    └── Delete Button (trash icon)
```

### UI - Adapter
**File:** `app/src/main/java/com/example/energy20/ui/settings/DeviceListAdapter.kt`

**Changes:**
- Added `onSetActiveClick` callback parameter
- Shows/hides active badge based on `device.isActive`
- Shows/hides set active button (inverse of badge)
- Handles set active button clicks

### UI - Settings Fragment
**File:** `app/src/main/java/com/example/energy20/ui/settings/SettingsFragment.kt`

**New Method:**
```kotlin
private fun setActiveDevice(device: UserDevice) {
    // Calls API
    // Updates local storage
    // Reloads device list
    // Shows success message
}
```

**Updated:**
- `DeviceListAdapter` initialization with both callbacks
- Device list now shows active status visually

## User Experience

### Visual Indicators
1. **Active Device:**
   - Shows green "ACTIVE" badge
   - No star button (already active)
   - Listed first in device list

2. **Inactive Devices:**
   - No badge
   - Shows star button to set as active
   - Listed after active device

### User Flow
1. User navigates to Settings
2. Sees list of devices with active status
3. Taps star icon on desired device
4. Device becomes active (badge appears)
5. Previous active device becomes inactive
6. Success message confirms change

## Testing Checklist

### Database
- [ ] Run migration SQL successfully
- [ ] Verify `is_active` column exists
- [ ] Verify index created
- [ ] Check first device set as active for existing users

### Backend
- [ ] Test `set_active_device.php` endpoint
- [ ] Verify only one device active per user
- [ ] Test `get_devices.php` returns `is_active` field
- [ ] Verify devices ordered by active status

### Android App
- [ ] Build app successfully
- [ ] Active badge shows on correct device
- [ ] Star button shows on inactive devices
- [ ] Tapping star sets device as active
- [ ] UI updates immediately after change
- [ ] Success message displays
- [ ] Device list reorders (active first)

## Future Enhancements

### Home Screen Integration
The active device feature is designed to support filtering on the home screen:

```kotlin
// In HomeFragment or HomeViewModel
val activeDevice = authManager.getUserDevices().find { it.isActive }
if (activeDevice != null) {
    // Load data only for active device
    loadEnergyData(activeDevice.deviceId)
}
```

### Additional Features
- Auto-activate first device when added
- Confirmation dialog before changing active device
- Show active device name in home screen title
- Filter charts by active device only

## Notes

- Only one device can be active at a time per user
- Setting a device as active automatically deactivates others
- Active status persists across app restarts
- Backend uses transactions to ensure consistency
- Device list automatically reorders with active device first

## Files Modified

### Backend
- `database/add_active_device.sql` (new)
- `backend/api/set_active_device.php` (new)
- `backend/api/get_devices.php` (updated)

### Android
- `app/src/main/java/com/example/energy20/data/User.kt`
- `app/src/main/java/com/example/energy20/api/AuthApiService.kt`
- `app/src/main/res/layout/item_device.xml`
- `app/src/main/java/com/example/energy20/ui/settings/DeviceListAdapter.kt`
- `app/src/main/java/com/example/energy20/ui/settings/SettingsFragment.kt`
