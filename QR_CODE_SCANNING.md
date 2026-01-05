# QR Code Scanning Feature

## Overview
Version 2.5.0 introduces QR code scanning functionality for easy device registration. Users can now scan QR codes on their energy monitoring devices to automatically fill in the device ID.

## QR Code Format

### Recommended Format (JSON)
```json
{
  "device_id": "D072F19EF0C8",
  "type": "energy.webmenow.org"
}
```

**Required Fields:**
- `device_id`: 12-character hexadecimal device identifier (e.g., "D072F19EF0C8")
- `type`: Must be exactly `"energy.webmenow.org"` for validation

### Backward Compatible Format (Plain Text)
```
D072F19EF0C8
```

Plain text QR codes containing just the 12-character hex device ID are also supported for backward compatibility.

## Implementation Details

### Components Created

1. **QrCodeParser.kt** - Utility for parsing and validating QR codes
   - Validates QR code type matches "energy.webmenow.org"
   - Validates device ID format (12 hex characters)
   - Supports both JSON and plain text formats

2. **QrScannerActivity.kt** - Full-screen camera scanner
   - Uses CameraX for camera preview
   - Uses ML Kit for barcode detection
   - Real-time QR code scanning
   - Haptic feedback on successful scan
   - Flashlight toggle support
   - Auto-close on successful scan

3. **activity_qr_scanner.xml** - Scanner UI layout
   - Full-screen camera preview
   - Corner markers for scanning guide
   - Close and flashlight buttons
   - Instruction text with color feedback

### User Flow

1. User navigates to Device Management
2. Clicks "Scan QR" button next to Device ID field
3. App requests camera permission (if not granted)
4. Scanner activity launches with camera preview
5. User points camera at QR code on device
6. ML Kit detects and validates QR code
7. Device vibrates on successful scan
8. Scanner closes and device ID auto-fills
9. User can optionally add location coordinates
10. User clicks "Add Device" to complete registration

### Permissions

**Required:**
- `CAMERA` - For scanning QR codes
- `VIBRATE` - For haptic feedback

**Features:**
- `android.hardware.camera` (optional) - Camera hardware
- `android.hardware.camera.autofocus` (optional) - Autofocus support

### Error Handling

The scanner validates QR codes and provides clear error messages:

- **Invalid type**: "Invalid QR code: Expected type 'energy.webmenow.org', got '...'"
- **Missing device_id**: "QR code missing device_id field"
- **Invalid format**: "Invalid device ID format: Expected 12 hexadecimal characters"
- **Empty QR code**: "QR code is empty"
- **Invalid JSON**: Falls back to plain text validation

Errors are displayed in red text for 2 seconds, then the scanner resets to allow retry.

## QR Code Generation

To generate QR codes for your devices, use any QR code generator with the following JSON:

```json
{
  "device_id": "YOUR_DEVICE_ID_HERE",
  "type": "energy.webmenow.org"
}
```

**Recommendations:**
- Use error correction level M or H for better scanning reliability
- Ensure QR code is at least 2cm x 2cm when printed
- Use high contrast (black on white background)
- Test scanning from various distances and angles

## Testing

To test the QR code scanning feature:

1. Generate a test QR code with the JSON format above
2. Open the app and navigate to Device Management
3. Click "Scan QR" button
4. Grant camera permission when prompted
5. Point camera at the QR code
6. Verify device ID auto-fills correctly
7. Complete device registration

## Future Enhancements

Potential future additions to QR codes:
- `timezone` - Auto-select device timezone
- `location` - Auto-fill latitude/longitude
- `device_name` - Suggested device name
- `firmware_version` - Device firmware information

Currently, the parser only extracts `device_id` and validates `type`. Additional fields can be added to the `ScannedDeviceData` class as needed.

## Technical Notes

- **ML Kit Barcode Scanning**: Version 17.2.0
- **CameraX**: Version 1.3.1
- **Minimum SDK**: 24 (Android 7.0)
- **Scanner orientation**: Portrait only
- **Supported barcode types**: QR_CODE (TYPE_TEXT and TYPE_URL)

## Version History

- **2.5.0** (2026-01-04): Initial QR code scanning implementation
  - JSON format with type validation
  - Backward compatibility with plain text
  - Camera permission handling
  - Haptic feedback
  - Flashlight support
