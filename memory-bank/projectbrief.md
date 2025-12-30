# Energy Monitoring Application - Project Brief

## Project Overview
Building an Android application to display energy consumption data from monitoring devices and provide device configuration management capabilities.

## Core Objectives
1. Display energy consumption data from database
2. Provide device configuration update functionality
3. Create mobile-friendly interface for energy monitoring

## Starting Point
- Web application exists at http://energy.webmenow.ca
- PHP-based backend with MySQL database
- Current implementation: `daily_consumption.php` displays daily energy consumption with charts and tables
- Android Studio project initialized with Navigation Drawer template

## Key Requirements
1. **Data Display**
   - Show energy consumption metrics
   - Support date range filtering
   - Visualize data with charts
   - Display consumption breakdown by device/circuit

2. **Device Configuration**
   - Update device settings
   - Manage sensor configurations
   - Handle timezone settings

3. **Platform**
   - Android native application
   - Kotlin programming language
   - Material Design UI components

## Database Schema (Inferred)
- `daily_energy` table: LocalTS, device_id, circuit_id, energy_consumer_kwh
- `device_settings` table: device_id, sensor_id, device_name, timezone_id

## Technical Stack
- **Frontend**: Android (Kotlin)
- **Backend**: PHP/MySQL (existing)
- **UI Framework**: Material Design, Navigation Drawer
- **Data Visualization**: Chart library (TBD for Android)
- **Network**: HTTP/REST API communication

## Success Criteria
- Successfully fetch and display energy consumption data
- Provide intuitive date range selection
- Enable device configuration updates
- Maintain responsive, mobile-optimized UI
- Handle offline scenarios gracefully
