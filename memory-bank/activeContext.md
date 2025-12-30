# Active Context

## Current Focus
**FIRST VERSION COMPLETE**: Energy consumption graph display implemented using HTML parsing approach.

## Recent Changes
1. Created Memory Bank structure with core documentation files
2. Documented project requirements and goals
3. Established technical architecture and patterns
4. **PLAN UPDATED**: User requested to use existing PHP endpoints without modification
5. Analyzed PHP code - discovered `$daily_data_json` already contains needed data
6. **IMPLEMENTATION COMPLETE**: Built first version with energy consumption chart
   - Added all required dependencies (OkHttp, Gson, Jsoup, MPAndroidChart, Coroutines)
   - Created network security config for HTTP access
   - Implemented HTML parsing to extract JSON from PHP pages
   - Built complete data flow: Network → Repository → ViewModel → UI
   - Created interactive line chart with MPAndroidChart

## Current State

### Project Setup
- Android Studio project created with Navigation Drawer template
- Base structure includes:
  - MainActivity with navigation drawer
  - Three fragments (Home, Gallery, Slideshow)
  - ViewModels for each fragment
  - Material Design components

### Existing Backend
- PHP web application at http://energy.webmenow.ca
- `daily_consumption.php` provides energy data visualization
- MySQL database with energy consumption data
- Tables: `daily_energy`, `device_settings`

### What Exists
✅ Android project structure
✅ Navigation drawer UI
✅ Fragment placeholders
✅ PHP backend with database access
✅ Web-based energy dashboard

### What Has Been Built
✅ API endpoints exist (JSON embedded in HTML pages)
✅ Network layer (OkHttp client)
✅ HTML parsing to extract JSON data (Jsoup + Regex)
✅ Data models (DeviceInfo, DeviceEnergyData)
✅ Repository pattern implementation
✅ Energy dashboard UI in HomeFragment
✅ Chart integration (MPAndroidChart with 6-color palette)
✅ Basic error handling and loading states

### What Needs to Be Built (Future)
❌ Device management UI
❌ Date range selection controls
❌ Data refresh functionality
❌ Offline caching
❌ Enhanced error handling

## Next Steps

### Immediate Priorities (REVISED)
1. **Add Dependencies**: Update `build.gradle.kts` with required libraries
   - OkHttp for HTTP requests
   - Gson for JSON parsing
   - Jsoup for HTML parsing (to extract JSON from script tags)
   - MPAndroidChart for visualization
   - Coroutines for async operations

2. **Analyze Existing PHP Pages**: Identify JSON data locations
   - ✅ `daily_consumption.php` has `$daily_data_json` in JavaScript
   - Need to check for other pages with device settings data
   - Document the exact location of JSON in HTML

3. **Build Network Layer**: Android networking infrastructure
   - OkHttp client for fetching HTML pages
   - HTML parser to extract JSON from `<script>` tags
   - Regex or Jsoup to find `const dailyData = {...}` pattern

4. **Define Data Models**: Kotlin data classes matching existing JSON
   - EnergyData model (matches `$daily_data_by_device` structure)
   - DeviceInfo model (name + data map)
   - Parse existing JSON format without changes

5. **Implement Repository**: Data access layer
   - Fetch HTML page
   - Extract JSON from script tags
   - Parse JSON to Kotlin objects
   - Error handling

### Phase 1: Energy Dashboard (Priority)
- Update HomeFragment to display energy data
- Implement date range selection
- Add chart visualization
- Show consumption breakdown table
- Handle loading/error states

### Phase 2: Device Management
- Repurpose GalleryFragment for device settings
- List all devices
- Edit device configuration
- Save changes to backend

### Phase 3: Polish & Testing
- Improve error handling
- Add loading indicators
- Implement data caching
- Test on physical devices
- Handle edge cases

## Active Decisions

### API Design - REVISED
**Decision**: Use existing PHP pages without modification, extract JSON from HTML
**Rationale**: 
- User preference to avoid backend changes initially
- PHP already generates JSON for JavaScript charts
- Can optimize later by adding dedicated API endpoints
- Faster initial development

**Implementation Approach**:
1. Fetch HTML page: `http://energy.webmenow.ca/daily_consumption.php?date_start=X&date_end=Y`
2. Parse HTML to find: `const dailyData = {...};`
3. Extract JSON string between `=` and `;`
4. Parse JSON with Gson
5. Map to Kotlin data classes

**Future Optimization**: Add `?api=true` parameter to return pure JSON

### Chart Library
**Decision**: Use MPAndroidChart
**Rationale**:
- Most popular Android charting library
- Supports line charts with multiple datasets
- Good documentation and examples
- Active maintenance

### Architecture Pattern
**Decision**: MVVM with Repository pattern
**Rationale**:
- Recommended by Android team
- Separates concerns clearly
- Testable components
- Lifecycle-aware

## Current Challenges

### Challenge 1: HTTP vs HTTPS
- Backend currently uses HTTP
- Android 9+ requires HTTPS by default
- **Solution**: Add network security config to allow HTTP for development
- **Future**: Migrate backend to HTTPS

### Challenge 2: No Authentication
- API endpoints will be public
- No user authentication system
- **Current**: Acceptable for MVP
- **Future**: Add authentication layer

### Challenge 3: Date Handling
- PHP uses LocalTS (local timestamp)
- Need to handle timezone conversions
- **Solution**: Use device timezone for display, send UTC to API

## Notes & Observations

### From PHP Code Analysis
1. Default date range: Last 7 days
2. Data grouped by device_id + circuit_id (e.g., "device_1_1")
3. Device names from `device_settings` table, fallback to generated names
4. Chart uses 6-color palette
5. Summary table shows total, average, and day count
6. **JSON Location**: Embedded in `<script>` tag as `const dailyData = <?= $daily_data_json ?>;`
7. **JSON Structure**: 
   ```json
   {
     "device_1_1": {
       "name": "Main Circuit",
       "data": {
         "2024-01-01": 12.5,
         "2024-01-02": 13.2
       }
     }
   }
   ```

### UI Patterns from Web Version
- Clean, modern design with Tailwind CSS
- Mobile-responsive layout
- Chart above data table
- Date range controls prominent
- Export functionality available

### Key Insights
- Energy data is daily aggregated (not real-time)
- Multiple devices/circuits can be monitored
- Timezone awareness is important
- Data visualization is primary feature
- Configuration is secondary feature

## Questions to Resolve
1. Should we implement offline caching? (Future consideration)
2. Do we need push notifications for alerts? (Future feature)
3. Should we support multiple users? (Not in MVP)
4. Export functionality needed in mobile app? (Nice to have)
