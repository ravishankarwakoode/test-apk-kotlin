# test-apk-kotlin

A Kotlin desktop APK analyzer with a cleaner Swing-based user interface.

## Features

- Browse and select an APK file from the desktop UI
- Inspect manifest metadata
- Get package name, version name, version code, SDK values
- List permissions, activities, services, receivers, providers
- Detect native libraries and META-INF entries
- Calculate SHA-256
- Generate and save a full HTML report file beside the APK

## Run

```bash
gradle run
```

Then click the Browse button and choose an APK file.

## Build

```bash
gradle build
```

## Project output

The app opens as a desktop window with:
- a file selection panel
- a summary card area
- a detailed HTML report viewer

This is a static APK analyzer that reads the APK archive and manifest, without executing the app itself.
