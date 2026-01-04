# Active Context

## Current Focus: Multi-User Authentication Implementation

### What We're Building
Adding Google OAuth 2.0 authentication to enable multi-user access with device ownership management.

### Recent Changes (2026-01-01)
- Planning complete for multi-user authentication system
- Decided on Google Sign-In (OAuth 2.0) direct integration
- Simplified approach: owner-only access, no sharing, no admin

### Architecture Decisions

#### Authentication Flow
```
User → Google Sign-In → Android App → PHP Backend → MySQL
                            ↓              ↓
                      ID Token      Verify Token
                            ↓              ↓
                    Store Securely   Create/Update User
                            ↓              ↓
                    Add to Requests  Check Device Access
```

#### Key Features
1. **Google Sign-In Only** - All Android users have Google accounts
2. **Owner-Only Access** - No sharing, no permission levels
3. **Device Registration** - QR code scan or manual entry
4. **Secure Token Storage** - EncryptedSharedPreferences
5. **Device Access Control** - Users only see their devices

### Implementation Phases

#### Phase 1: Database Schema ✅ (Planned)
- `users` table: Store Google user info
- `user_devices` table: Map users to devices (many-to-many)

#### Phase 2: Backend API (In Progress)
- `config/google_auth.php`: Token verification
- `api/auth.php`: Login endpoint
- `api/add_device.php`: Device registration
- Modify `daily_consumption.php`: Add auth check

#### Phase 3: Android App (Next)
- LoginActivity: Google Sign-In UI
- AuthManager: Token management
- DeviceOnboardingActivity: QR scanner + manual entry
- AuthInterceptor: Add token to requests
- Update MainActivity: Auth check
- Update Navigation Drawer: User profile

### Next Steps
1. Create backend PHP files
2. Create Android authentication components
3. Add QR code scanner
4. Test end-to-end flow
5. Update version to 2.0.0

### Technical Notes

#### Google Cloud Console Setup Required
- Create OAuth 2.0 credentials (Android + Web)
- Get SHA-1 fingerprint from Android Studio
- Configure OAuth consent screen

#### Security Measures
- HTTPS only (already configured)
- Token verification on every request
- Device access validation
- Secure token storage on device
- SQL injection prevention (prepared statements)

### User Requirements
- QR code or manual device ID entry
- Owner-only access (simplified)
- No device sharing
- Manual migration of existing devices
- No admin access needed
