# System Patterns

## Architecture Overview

### High-Level Architecture
```
┌─────────────────┐
│  Android App    │
│   (Kotlin)      │
└────────┬────────┘
         │ HTTP/REST
         ▼
┌─────────────────┐
│   PHP Backend   │
│  (Web Server)   │
└────────┬────────┘
         │ SQL
         ▼
┌─────────────────┐
│ MySQL Database  │
│  (Energy Data)  │
└─────────────────┘
```

### Component Structure

#### Android Application
- **MainActivity**: Navigation drawer host
- **Fragments**: 
  - HomeFragment: Energy consumption dashboard
  - GalleryFragment: (To be repurposed for device management)
  - SlideshowFragment: (To be repurposed or removed)
- **ViewModels**: MVVM pattern for data management
- **Network Layer**: HTTP client for API communication
- **Data Models**: Kotlin data classes for energy data

#### Backend (Existing PHP)
- **daily_consumption.php**: Main endpoint for energy data
- **Database Connection**: mysqli-based connection
- **Query Pattern**: Prepared statements for security
- **Response Format**: JSON for API, HTML for web

## Key Technical Decisions

### 1. API Communication Strategy
**Decision**: Create REST API endpoints from existing PHP
**Rationale**: 
- Leverage existing database logic
- Minimal backend changes required
- Separate concerns (web UI vs mobile API)

**Implementation**:
- Add API mode parameter to existing PHP files
- Return JSON when API mode enabled
- Maintain backward compatibility with web interface

### 2. Data Architecture
**Pattern**: Repository Pattern
**Structure**:
```
ViewModel → Repository → RemoteDataSource → API
                      ↓
                  LocalCache (optional)
```

**Benefits**:
- Separation of concerns
- Testability
- Future offline support capability

### 3. UI Architecture
**Pattern**: MVVM (Model-View-ViewModel)
**Components**:
- **View**: Fragments with data binding
- **ViewModel**: LiveData/StateFlow for reactive updates
- **Model**: Data classes and repository

### 4. Chart Library Selection
**Options Considered**:
- MPAndroidChart (most popular)
- PhilJay Charts
- Custom Canvas drawing

**Recommendation**: MPAndroidChart
- Mature, well-documented
- Supports line charts with multiple datasets
- Customizable styling
- Active maintenance

## Design Patterns in Use

### 1. Navigation Component
- Single Activity architecture
- Fragment-based navigation
- Navigation drawer for main menu
- Type-safe argument passing

### 2. Dependency Injection (Future)
- Consider Hilt/Dagger for DI
- Improves testability
- Manages lifecycle-aware components

### 3. Coroutines for Async Operations
- Network calls on background threads
- Main thread UI updates
- Structured concurrency

### 4. LiveData/StateFlow for Reactive UI
- Lifecycle-aware data observation
- Automatic UI updates on data changes
- Memory leak prevention

## Data Flow Patterns

### Energy Data Retrieval
```
User Action (Date Selection)
    ↓
ViewModel.loadEnergyData()
    ↓
Repository.getEnergyData(startDate, endDate)
    ↓
API.fetchDailyEnergy(params)
    ↓
Parse JSON Response
    ↓
Update LiveData/StateFlow
    ↓
Fragment Observes & Updates UI
```

### Device Configuration Update
```
User Edits Settings
    ↓
ViewModel.updateDeviceSettings()
    ↓
Repository.saveDeviceSettings(device)
    ↓
API.updateDevice(deviceData)
    ↓
Receive Confirmation
    ↓
Update UI State
    ↓
Show Success/Error Message
```

## Database Schema Patterns

### daily_energy Table
- **Primary Key**: Composite (LocalTS, device_id, circuit_id)
- **Indexes**: LocalTS for date range queries
- **Data Type**: DECIMAL for energy values (precision)

### device_settings Table
- **Primary Key**: Composite (device_id, sensor_id)
- **Foreign Key**: References devices
- **Nullable Fields**: device_name (defaults to generated name)

## Error Handling Strategy

### Network Errors
- Retry logic with exponential backoff
- User-friendly error messages
- Offline state indication

### Data Validation
- Client-side validation before API calls
- Server-side validation in PHP
- Graceful degradation for missing data

### State Management
- Loading states
- Error states
- Empty states
- Success states

## Security Considerations

### API Security
- HTTPS for all communications
- Input validation and sanitization
- Prepared statements (SQL injection prevention)
- Rate limiting (future consideration)

### Data Privacy
- No sensitive data caching
- Secure credential storage (if authentication added)
- Minimal data retention on device
