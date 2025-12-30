# How to Create Your First GitHub Release

## Step 1: Locate the APK File

The debug APK has been built and is located at:
```
C:\Users\Daniel\AndroidStudioProjects\Energy20\app\build\outputs\apk\debug\app-debug.apk
```

**Rename it to:** `Energy20-v1.0.0.apk` (for clarity)

## Step 2: Go to GitHub Releases

1. Open your browser and go to:
   ```
   https://github.com/dannica2k/Energy-2.0-App/releases
   ```

2. Click the **"Draft a new release"** button (or "Create a new release")

## Step 3: Fill in Release Information

### Tag Version
- Enter: `v1.0.0`
- Click "Create new tag: v1.0.0 on publish"

### Release Title
- Enter: `Version 1.0.0 - Initial Release with Auto-Update`

### Description (Release Notes)
Copy and paste this:

```markdown
# Energy20 v1.0.0 - Initial Release

## 🎉 Features

### Energy Monitoring
- **Daily Energy Charts**: View energy consumption with interactive line charts
- **Multi-Device Support**: Track multiple devices and circuits simultaneously
- **Date Range Selection**: 6 quick date options (Today, Yesterday, Last 7/30/90 days, Custom)
- **Occupancy Tracking**: See occupied vs total days based on configurable threshold
- **Device Statistics**: Total consumption, average per day, and occupied days

### Settings & Configuration
- **Occupancy Threshold**: Configure minimum kWh for a day to be considered "occupied"
- **Persistent Settings**: All preferences saved locally

### Auto-Update System ⭐ NEW
- **Check for Updates**: Tap button in Settings to check for new versions
- **Automatic Download**: Downloads APK directly from GitHub releases
- **Easy Installation**: Prompts to install when download completes
- **Version Tracking**: Shows current version and last check time
- **Skip Versions**: Option to ignore specific updates

## 📱 Installation

1. Download `Energy20-v1.0.0.apk` from the Assets section below
2. Enable "Install from Unknown Sources" in your Android settings
3. Open the downloaded APK and install
4. Grant necessary permissions when prompted

## 🔄 Future Updates

After installing this version, you can check for updates directly in the app:
1. Open Energy20
2. Go to Settings (drawer menu)
3. Tap "Check for Updates"
4. Download and install new versions with one tap!

## 🛠️ Technical Details

- **Minimum Android Version**: Android 7.0 (API 24)
- **Target Android Version**: Android 14 (API 36)
- **Architecture**: MVVM with Repository pattern
- **Networking**: OkHttp + Gson
- **Charts**: MPAndroidChart
- **Backend**: PHP/MySQL (energy.webmenow.ca)

## 📝 Known Issues

- None reported yet

## 🙏 Feedback

Found a bug or have a suggestion? Please open an issue on GitHub!

---

**Full Changelog**: Initial release
```

## Step 4: Upload the APK

1. Scroll down to the **"Attach binaries"** section
2. Click or drag the renamed APK file: `Energy20-v1.0.0.apk`
3. Wait for the upload to complete (you'll see a checkmark)

## Step 5: Publish the Release

1. **IMPORTANT**: Make sure "Set as the latest release" is checked
2. Leave "Set as a pre-release" UNCHECKED (unless you want to mark it as beta)
3. Click the green **"Publish release"** button

## Step 6: Verify the Release

After publishing:
1. You should see your release at: https://github.com/dannica2k/Energy-2.0-App/releases
2. The APK should be listed under "Assets"
3. You can download it to verify

## Step 7: Test the Auto-Update Feature

Now let's test if the update system works:

### Option A: Test with a Second Release (Recommended)

1. Change version in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 2
   versionName = "1.1.0"
   ```

2. Build new APK: `gradlew.bat assembleDebug`

3. Create another release (v1.1.0) with the new APK

4. Install v1.0.0 on your device

5. Open app → Settings → "Check for Updates"

6. Should show v1.1.0 is available!

### Option B: Quick Test (Install Current Version)

1. Install the v1.0.0 APK on your Android device

2. Open the app

3. Go to Settings

4. Tap "Check for Updates"

5. Should say "You're up to date!" (since you're on the latest)

## Troubleshooting

### "Install from Unknown Sources" Required
- Go to Settings → Security → Enable "Unknown Sources"
- On Android 8.0+: Settings → Apps → Special Access → Install unknown apps → Enable for your browser/file manager

### APK Won't Install
- Make sure you downloaded the complete file
- Check that you have enough storage space
- Try redownloading the APK

### Update Check Shows Error
- Check your internet connection
- Verify the release is published (not draft)
- Make sure the APK is uploaded to the release

## Next Steps

After creating this release:

1. **Share the link**: Send the release URL to users
2. **Test on device**: Install and verify everything works
3. **Monitor feedback**: Watch for any issues
4. **Plan updates**: When you add features, create new releases

## Future: Creating a Release Keystore

When you're ready for production (Play Store or wider distribution):

1. I can help you create a proper release keystore
2. Build signed release APKs
3. These will be more secure and official

For now, the debug APK is perfect for testing the auto-update feature!

---

**Need Help?** Just ask if you run into any issues!
