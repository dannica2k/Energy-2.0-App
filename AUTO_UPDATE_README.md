# Auto-Update Feature Documentation

## Overview

The Energy20 app includes a GitHub releases-based auto-update system that allows users to check for and install new versions directly from the app's Settings screen.

## How It Works

### 1. Version Checking
- The app queries the GitHub API for the latest release from: `https://github.com/dannica2k/Energy-2.0-App`
- Compares the current app version with the latest release version
- Supports semantic versioning (e.g., 1.0, 1.1.0, 2.0.0)

### 2. Update Flow
1. User navigates to Settings screen
2. Clicks "Check for Updates" button
3. App fetches latest release from GitHub API
4. If update available:
   - Shows dialog with version info and release notes
   - User can choose to:
     - **Download**: Downloads and installs the update
     - **Later**: Dismisses the dialog
     - **Skip This Version**: Ignores this specific version
5. If no update:
   - Shows "You're up to date!" message

### 3. Download & Installation
- Uses Android's DownloadManager for reliable downloads
- Downloads APK to device's Downloads folder
- Automatically prompts user to install when download completes
- Requires user to enable "Install from Unknown Sources" for first-time installation

## Components

### Data Models
**GitHubRelease.kt** - Models for GitHub API responses
```kotlin
data class GitHubRelease(
    val tagName: String,        // Version tag (e.g., "v1.0")
    val name: String,            // Release name
    val body: String?,           // Release notes
    val publishedAt: String,     // Publication date
    val assets: List<GitHubAsset>,
    val prerelease: Boolean      // Is this a pre-release?
)

data class GitHubAsset(
    val name: String,            // File name
    val downloadUrl: String,     // Direct download URL
    val size: Long,              // File size in bytes
    val contentType: String      // MIME type
)
```

### Network Service
**GitHubApiService.kt** - Handles GitHub API communication
- `getLatestRelease()`: Fetches the most recent release
- `getAllReleases()`: Fetches all releases (for future use)

### Update Manager
**UpdateManager.kt** - Core update logic
- Version comparison
- Update checking
- Download management
- Installation handling
- User preferences (ignored versions, last check time)

### UI Components
**SettingsFragment.kt** - User interface for updates
- Current version display
- Last check timestamp
- "Check for Updates" button
- Progress indicators
- Update dialogs

## Configuration

### Permissions (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

### FileProvider (for Android 7.0+)
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### Build Configuration
```kotlin
buildFeatures {
    buildConfig = true  // Required for BuildConfig.VERSION_NAME
}
```

## Publishing Updates

### 1. Create a New Release on GitHub

1. Go to: https://github.com/dannica2k/Energy-2.0-App/releases
2. Click "Draft a new release"
3. Fill in:
   - **Tag version**: Use semantic versioning (e.g., `v1.1.0`)
   - **Release title**: Descriptive name (e.g., "Version 1.1.0 - Bug Fixes")
   - **Description**: Release notes (what's new, bug fixes, etc.)
4. Upload the APK file:
   - Build: `gradlew.bat assembleRelease` (or assembleDebug for testing)
   - APK location: `app/build/outputs/apk/release/app-release.apk`
5. Click "Publish release"

### 2. Version Numbering

Update version in `app/build.gradle.kts`:
```kotlin
defaultConfig {
    versionCode = 2        // Increment for each release
    versionName = "1.1.0"  // Semantic version
}
```

**Version Format**: `MAJOR.MINOR.PATCH`
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes

### 3. APK Naming Convention
Recommended: `Energy20-v1.1.0.apk`

## User Experience

### First-Time Setup
Users installing from GitHub (not Play Store) must:
1. Enable "Install from Unknown Sources" in Android settings
2. Download the initial APK manually
3. Install the app

### Subsequent Updates
1. Open app → Settings
2. Tap "Check for Updates"
3. If update available, tap "Download"
4. Wait for download to complete
5. Tap notification to install
6. Confirm installation

### Update Dialog Options
- **Download**: Immediately downloads and installs
- **Later**: Closes dialog, will show again on next check
- **Skip This Version**: Won't show this version again (stored in preferences)

## Testing the Update Feature

### Local Testing
1. Build current version (e.g., v1.0)
2. Install on device
3. Create GitHub release with higher version (e.g., v1.1)
4. Upload APK to release
5. Open app → Settings → Check for Updates
6. Verify update dialog appears
7. Test download and installation

### Test Scenarios
- ✅ No update available (same version)
- ✅ Update available (newer version)
- ✅ Current version newer than release (shouldn't happen)
- ✅ Network error handling
- ✅ No APK in release
- ✅ Skip version functionality
- ✅ Download progress
- ✅ Installation flow

## Troubleshooting

### "No APK found in release"
- Ensure APK file is uploaded to GitHub release
- File must have `.apk` extension

### "Failed to fetch release: 404"
- Check repository name in `GitHubApiService.kt`
- Verify release is published (not draft)

### Installation Blocked
- User must enable "Install from Unknown Sources"
- On Android 8.0+: Per-app permission required

### Download Fails
- Check internet connection
- Verify download URL is accessible
- Check device storage space

## Security Considerations

### APK Signing
- Always sign release APKs with the same key
- Android verifies signature before installation
- Users will see warning if signature doesn't match

### HTTPS
- GitHub API uses HTTPS
- APK downloads use HTTPS
- No man-in-the-middle risk

### Permissions
- `REQUEST_INSTALL_PACKAGES`: Required for programmatic installation
- User must explicitly approve installation

## Future Enhancements

### Potential Improvements
1. **Automatic Update Checks**
   - Check on app launch (with rate limiting)
   - Background periodic checks

2. **Download Progress**
   - Show progress bar during download
   - Pause/resume capability

3. **Delta Updates**
   - Only download changed files
   - Reduce bandwidth usage

4. **Update Channels**
   - Stable vs Beta releases
   - User preference for pre-releases

5. **Rollback**
   - Keep previous version
   - Allow downgrade if needed

6. **Changelog Display**
   - Rich formatting for release notes
   - In-app changelog viewer

## API Rate Limits

GitHub API (unauthenticated):
- 60 requests per hour per IP
- Sufficient for normal usage
- Consider adding authentication token for higher limits

## Support

For issues or questions:
- GitHub Issues: https://github.com/dannica2k/Energy-2.0-App/issues
- Email: [Your support email]

## License

[Your license information]
