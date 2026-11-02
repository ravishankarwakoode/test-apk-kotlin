# APK Analyzer Pro – Project Documentation

## 1. Project Overview
APK Analyzer Pro is a Kotlin-based application designed to inspect Android APK files and generate a detailed report. The project includes:

- a desktop Swing UI
- backend API support using Ktor
- APK metadata extraction
- HTML report generation
- APK upload analysis support through backend endpoints

This app is intended for static analysis of APK files. It reads the APK archive and Android manifest files without installing or executing the APK.

## 2. Objectives
The main objectives of the project are:

- Analyze APK package details
- Read manifest metadata
- Extract app permissions and components
- Generate a report for application review
- Provide API-based integration for backend usage

## 3. Key Features
### Desktop Features
- Select APK with file chooser
- Validate file type
- View summary cards
- Analyze manifest information
- Show detailed report in UI
- Save HTML report beside the APK

### Backend Features
- Run Ktor server
- Upload APK using multipart form
- Return JSON analysis result
- Expose health check endpoint

### Analysis Features
- Package name
- Version name and version code
- Min SDK and Target SDK
- App label and app icon values
- Permissions list
- Activities, services, receivers, providers
- Native libraries
- META-INF files
- SHA-256 hash
- File size

## 4. Technical Stack
- Kotlin
- Gradle
- Swing (Desktop UI)
- Ktor (Backend API)
- Java XML parser for AndroidManifest.xml
- ZIP file handling for APK archive analysis

## 5. Project Structure
- `src/main/kotlin/com/example/apkreport/Main.kt` – desktop UI and core APK analysis logic
- `src/main/kotlin/com/example/apkreport/Backend.kt` – Ktor backend and upload analysis endpoint
- `build.gradle.kts` – project dependencies and build settings
- `README.md` – quick project overview and usage
- `docs/` – project documentation files

## 6. How to Run
### Desktop UI
```bash
gradle run
```
Then click Browse and select an APK file.

### Backend API
```bash
gradle run --args="--backend"
```

Health check:
```bash
http://localhost:8080/health
```

Upload endpoint:
```bash
POST http://localhost:8080/api/analyze
```
Use a multipart request with a file field named `file`.

## 7. Example API Response
```json
{
  "fileName": "example.apk",
  "fileSizeBytes": 123456,
  "sha256": "...",
  "packageName": "com.example.app",
  "versionName": "1.0.0",
  "versionCode": "1",
  "minSdk": "24",
  "targetSdk": "34",
  "appLabel": "Example App",
  "appIcon": "Unknown",
  "permissions": [
    "android.permission.INTERNET"
  ],
  "activities": [
    "com.example.app.MainActivity"
  ],
  "services": [],
  "receivers": [],
  "providers": [],
  "nativeLibraries": [],
  "metaInfFiles": []
}
```

## 8. Output
The application creates an HTML report generated in the same folder as the APK file. This report contains the complete APK metadata and analysis result.

## 9. Limitations
This project performs static analysis of the APK archive and manifest. It does not:

- install the app
- execute the APK
- emulate a device
- scan runtime behavior

## 10. Future Enhancements
Possible improvements for future versions include:

- database storage of reports
- APK history tracking
- advanced security checks
- PDF export
- user authentication for API access
- dark mode UI
- web dashboard integration

## 11. Conclusion
APK Analyzer Pro offers a practical solution for static APK inspection. It can be used for application review, metadata extraction, security checking, and API-based integration in Kotlin projects.
