# Technical Context

## Technologies Used

### Android Application
- **Language**: Kotlin 1.9+
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Build System**: Gradle with Kotlin DSL

### Android Libraries & Frameworks
- **AndroidX Core**: Core Android components
- **Material Design**: Material 3 components
- **Navigation Component**: Fragment navigation
- **Lifecycle Components**: ViewModel, LiveData
- **Coroutines**: Asynchronous programming
- **ViewBinding**: Type-safe view access

### Network & Data
- **HTTP Client**: Retrofit 2 (recommended) or OkHttp
- **JSON Parsing**: Gson or Moshi
- **Image Loading**: Coil (if needed)

### Charts & Visualization
- **MPAndroidChart**: Line charts, bar charts
- **Version**: 3.1.0+

### Backend (Existing)
- **Language**: PHP 7.4+
- **Database**: MySQL 5.7+
- **Web Server**: Apache/Nginx
- **Database Driver**: mysqli

### Web Technologies (Reference)
- **Frontend**: HTML5, CSS3, JavaScript
- **CSS Framework**: Tailwind CSS
- **Charts**: Chart.js 3.7.0
- **Fonts**: Google Fonts (Inter)

## Development Setup

### Required Tools
1. **Android Studio**: Hedgehog (2023.1.1) or later
2. **JDK**: Java 17 (bundled with Android Studio)
3. **Android SDK**: API 24-34
4. **Gradle**: 8.0+ (via wrapper)

### Project Structure
```
Energy20/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/energy20/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/          (to be created)
│   │   │   │   ├── network/       (to be created)
│   │   │   │   ├── repository/    (to be created)
│   │   │   │   └── ui/
│   │   │   │       ├── home/
│   │   │   │       ├── gallery/
│   │   │   │       └── slideshow/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

### Dependencies (Current)
```kotlin
// Core Android
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")

// Navigation
implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
```

### Dependencies (To Add)
```kotlin
// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Charts
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## Technical Constraints

### Network Requirements
- **Base URL**: http://energy.webmenow.ca
- **Protocol**: HTTP (consider HTTPS upgrade)
- **Data Format**: JSON for API responses
- **Authentication**: None currently (consider adding)

### Database Schema
```sql
-- daily_energy table
CREATE TABLE daily_energy (
    LocalTS DATE,
    device_id VARCHAR(50),
    circuit_id INT,
    energy_consumer_kwh DECIMAL(10,2),
    PRIMARY KEY (LocalTS, device_id, circuit_id)
);

-- device_settings table
CREATE TABLE device_settings (
    device_id VARCHAR(50),
    sensor_id INT,
    device_name VARCHAR(100),
    timezone_id VARCHAR(50),
    PRIMARY KEY (device_id, sensor_id)
);
```

### API Endpoints (To Be Created)

#### Get Daily Energy Data
```
GET /api/daily_energy.php
Parameters:
  - date_start: YYYY-MM-DD
  - date_end: YYYY-MM-DD
  - device_id: (optional) filter by device

Response:
{
  "success": true,
  "data": {
    "device_1_circuit_1": {
      "name": "Main Circuit",
      "data": {
        "2024-01-01": 12.5,
        "2024-01-02": 13.2
      }
    }
  }
}
```

#### Get Device Settings
```
GET /api/device_settings.php
Response:
{
  "success": true,
  "devices": [
    {
      "device_id": "device_1",
      "sensor_id": 1,
      "device_name": "Main Circuit",
      "timezone_id": "America/Vancouver"
    }
  ]
}
```

#### Update Device Settings
```
POST /api/device_settings.php
Body:
{
  "device_id": "device_1",
  "sensor_id": 1,
  "device_name": "Updated Name",
  "timezone_id": "America/Vancouver"
}

Response:
{
  "success": true,
  "message": "Device updated successfully"
}
```

## Development Workflow

### Build Process
1. Gradle sync
2. Build variants: debug/release
3. ProGuard for release builds
4. APK/AAB generation

### Testing Strategy
- **Unit Tests**: ViewModel logic, Repository
- **Integration Tests**: API communication
- **UI Tests**: Fragment interactions
- **Manual Testing**: Device testing on physical devices

### Version Control
- **Git**: Source control
- **Branching**: Feature branches
- **Commits**: Conventional commits format

## Performance Considerations

### Network Optimization
- Cache API responses
- Implement pagination for large datasets
- Compress JSON responses
- Use connection pooling

### UI Performance
- RecyclerView for lists
- ViewHolder pattern
- Lazy loading for charts
- Debounce user inputs

### Memory Management
- Lifecycle-aware components
- Proper resource cleanup
- Bitmap recycling (if images used)
- Avoid memory leaks

## Known Limitations

### Current Constraints
1. HTTP only (not HTTPS)
2. No authentication/authorization
3. No offline support
4. Limited error handling in PHP
5. No API versioning

### Future Improvements
1. Migrate to HTTPS
2. Implement user authentication
3. Add local database caching
4. Improve API error responses
5. Add API versioning
6. Implement push notifications
